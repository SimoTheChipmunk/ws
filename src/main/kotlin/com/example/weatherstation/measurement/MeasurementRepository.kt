package com.example.weatherstation.measurement

import com.example.weatherstation.domain.HourlyObservation
import com.example.weatherstation.domain.MeasurementType
import com.example.weatherstation.domain.TimeWindow
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Repository
class MeasurementRepository(
    private val jdbcClient: JdbcClient,
    private val named: NamedParameterJdbcTemplate,
) {

    @Transactional
    fun upsertAll(
        locationId: Long,
        source: String,
        observations: List<HourlyObservation>,
    ): Int {
        if (observations.isEmpty()) {
            return 0
        }

        observations.chunked(BATCH_SIZE).forEach { chunk ->
            val batch: Array<SqlParameterSource> = Array(chunk.size) { index ->
                val observation = chunk[index]

                MapSqlParameterSource()
                    .addValue("locationId", locationId)
                    .addValue(
                        "observedAt",
                        observation.observedAt.atOffset(ZoneOffset.UTC),
                    )
                    .addValue(
                        "temperature",
                        observation.temperatureC,
                        Types.DOUBLE,
                    )
                    .addValue(
                        "humidity",
                        observation.humidityPct,
                        Types.DOUBLE,
                    )
                    .addValue(
                        "wind",
                        observation.windKmh,
                        Types.DOUBLE,
                    )
                    .addValue("source", source)
            }

            named.batchUpdate(UPSERT_SQL, batch)
        }

        return observations.size
    }

    fun findRaw(
        locationId: Long,
        window: TimeWindow,
        limit: Int,
        offset: Int,
    ): List<StoredObservation> {
        val sql = """
            SELECT observed_at, temperature_c, humidity_pct, wind_kmh, source
            FROM measurement
            WHERE location_id = :locationId
              AND observed_at >= :start
              AND observed_at < :end
            ORDER BY observed_at
            LIMIT :limit
            OFFSET :offset
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("locationId", locationId)
            .param("start", window.startInclusive.atOffset(ZoneOffset.UTC))
            .param("end", window.endExclusive.atOffset(ZoneOffset.UTC))
            .param("limit", limit)
            .param("offset", offset)
            .query(::toStoredObservation)
            .list()
    }

    fun aggregate(
        locationId: Long,
        metric: MeasurementType,
        granularity: Granularity,
        window: TimeWindow,
    ): List<AggregationBucket> {
        // The column name comes exclusively from the MeasurementType enum.
        val column = metric.column

        val sql = """
            SELECT
                date_trunc(
                    CAST(:unit AS text),
                    m.observed_at AT TIME ZONE l.timezone
                ) AS bucket,
                MIN(m.$column) AS min_v,
                AVG(m.$column) AS avg_v,
                MAX(m.$column) AS max_v,
                COUNT(m.$column) AS samples
            FROM measurement m
            JOIN location l ON l.id = m.location_id
            WHERE m.location_id = :locationId
              AND m.observed_at >= :start
              AND m.observed_at < :end
            GROUP BY bucket
            ORDER BY bucket
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("unit", granularity.sqlUnit)
            .param("locationId", locationId)
            .param("start", window.startInclusive.atOffset(ZoneOffset.UTC))
            .param("end", window.endExclusive.atOffset(ZoneOffset.UTC))
            .query(::toAggregationBucket)
            .list()
    }

    fun overall(
        locationId: Long,
        metric: MeasurementType,
        window: TimeWindow,
    ): OverallStats {
        // The column name comes exclusively from the MeasurementType enum.
        val column = metric.column

        val sql = """
            SELECT
                MIN(m.$column) AS min_v,
                AVG(m.$column) AS avg_v,
                MAX(m.$column) AS max_v,
                COUNT(m.$column) AS samples
            FROM measurement m
            WHERE m.location_id = :locationId
              AND m.observed_at >= :start
              AND m.observed_at < :end
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("locationId", locationId)
            .param("start", window.startInclusive.atOffset(ZoneOffset.UTC))
            .param("end", window.endExclusive.atOffset(ZoneOffset.UTC))
            .query(::toOverallStats)
            .single()
    }

    fun rankDays(
        locationId: Long,
        metric: MeasurementType,
        statistic: Statistic,
        direction: SortDirection,
        limit: Int,
        window: TimeWindow,
    ): List<DayValue> {
        // These SQL fragments come exclusively from enums, never request text.
        val column = metric.column
        val stat = statistic.sql
        val dir = direction.sql

        val sql = """
            WITH daily AS (
                SELECT
                    (m.observed_at AT TIME ZONE l.timezone)::date AS day,
                    $stat(m.$column) AS value
                FROM measurement m
                JOIN location l ON l.id = m.location_id
                WHERE m.location_id = :locationId
                  AND m.observed_at >= :start
                  AND m.observed_at < :end
                  AND m.$column IS NOT NULL
                GROUP BY day
            )
            SELECT day, value
            FROM daily
            ORDER BY value $dir, day
            LIMIT :limit
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("locationId", locationId)
            .param("start", window.startInclusive.atOffset(ZoneOffset.UTC))
            .param("end", window.endExclusive.atOffset(ZoneOffset.UTC))
            .param("limit", limit)
            .query(::toDayValue)
            .list()
    }

    private fun toStoredObservation(
        rs: ResultSet,
        rowNum: Int,
    ): StoredObservation =
        StoredObservation(
            observedAt = rs.getObject(
                "observed_at",
                OffsetDateTime::class.java,
            ).toInstant(),
            temperatureC = rs.getNullableDouble("temperature_c"),
            humidityPct = rs.getNullableDouble("humidity_pct"),
            windKmh = rs.getNullableDouble("wind_kmh"),
            source = rs.getString("source"),
        )

    private fun toAggregationBucket(
        rs: ResultSet,
        rowNum: Int,
    ): AggregationBucket =
        AggregationBucket(
            start = rs.getObject(
                "bucket",
                LocalDateTime::class.java,
            ).toLocalDate(),
            min = rs.getNullableDouble("min_v"),
            avg = rs.getNullableDouble("avg_v"),
            max = rs.getNullableDouble("max_v"),
            samples = rs.getLong("samples"),
        )

    private fun toOverallStats(
        rs: ResultSet,
        rowNum: Int,
    ): OverallStats =
        OverallStats(
            min = rs.getNullableDouble("min_v"),
            avg = rs.getNullableDouble("avg_v"),
            max = rs.getNullableDouble("max_v"),
            samples = rs.getLong("samples"),
        )

    private fun toDayValue(
        rs: ResultSet,
        rowNum: Int,
    ): DayValue =
        DayValue(
            date = rs.getDate("day").toLocalDate(),
            value = rs.getDouble("value"),
        )

    /**
     * ResultSet.getDouble() returns 0.0 for SQL NULL.
     * wasNull() is therefore necessary to preserve nullable measurements.
     */
    private fun ResultSet.getNullableDouble(column: String): Double? {
        val value = getDouble(column)
        return if (wasNull()) null else value
    }

    private companion object {
        const val BATCH_SIZE = 1_000

        const val UPSERT_SQL = """
            INSERT INTO measurement (
                location_id,
                observed_at,
                temperature_c,
                humidity_pct,
                wind_kmh,
                source
            )
            VALUES (
                :locationId,
                :observedAt,
                :temperature,
                :humidity,
                :wind,
                :source
            )
            ON CONFLICT (location_id, observed_at)
            DO UPDATE SET
                temperature_c = EXCLUDED.temperature_c,
                humidity_pct = EXCLUDED.humidity_pct,
                wind_kmh = EXCLUDED.wind_kmh,
                source = EXCLUDED.source,
                ingested_at = now()
        """
    }
}
