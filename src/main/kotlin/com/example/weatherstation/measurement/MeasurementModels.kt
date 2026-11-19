package com.example.weatherstation.measurement

import java.time.Instant
import java.time.LocalDate

enum class Granularity(internal val sqlUnit: String) { DAY("day"), WEEK("week"), MONTH("month") }
enum class Statistic(internal val sql: String) { MIN("MIN"), AVG("AVG"), MAX("MAX") }
enum class SortDirection(internal val sql: String) { HIGHEST("DESC"), LOWEST("ASC") }

data class StoredObservation(
    val observedAt: Instant,
    val temperatureC: Double?,
    val humidityPct: Double?,
    val windKmh: Double?,
    val source: String
)

data class AggregationBucket(
    val start: LocalDate,
    val min: Double?,
    val avg: Double?,
    val max: Double?,
    val samples: Long
)

data class OverallStats(val min: Double?, val avg: Double?, val max: Double?, val samples: Long)
data class DayValue(val date: LocalDate, val value: Double)
