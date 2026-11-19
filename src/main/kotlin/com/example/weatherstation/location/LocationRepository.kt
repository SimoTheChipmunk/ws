package com.example.weatherstation.location

import com.example.weatherstation.domain.DuplicateLocationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class LocationRepository(
    private val jdbcClient: JdbcClient,
) {

    fun insert(
        name: String,
        latitude: Double,
        longitude: Double,
        timezone: String,
    ): Location {
        val sql = """
            INSERT INTO location (name, latitude, longitude, timezone)
            VALUES (:name, :latitude, :longitude, :timezone)
            RETURNING id, name, latitude, longitude, timezone
        """.trimIndent()

        return try {
            jdbcClient.sql(sql)
                .param("name", name)
                .param("latitude", latitude)
                .param("longitude", longitude)
                .param("timezone", timezone)
                .query(::toLocation)
                .single()
        } catch (ex: DuplicateKeyException) {
            throw DuplicateLocationException(name)
        }
    }

    fun findById(id: Long): Location? {
        val sql = """
            SELECT id, name, latitude, longitude, timezone
            FROM location
            WHERE id = :id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("id", id)
            .query(::toLocation)
            .optional()
            .orElse(null)
    }

    fun findAll(): List<Location> {
        val sql = """
            SELECT id, name, latitude, longitude, timezone
            FROM location
            ORDER BY id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query(::toLocation)
            .list()
    }

    private fun toLocation(
        resultSet: ResultSet,
        rowNum: Int,
    ): Location =
        Location(
            id = resultSet.getLong("id"),
            name = resultSet.getString("name"),
            latitude = resultSet.getDouble("latitude"),
            longitude = resultSet.getDouble("longitude"),
            timezone = resultSet.getString("timezone"),
        )
}
