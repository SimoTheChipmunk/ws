package com.example.weatherstation.domain

import kotlin.time.Instant

enum class MeasurementType(val unit: String, internal val column: String) {
    TEMPERATURE("°C", "temperature_c"),
    HUMIDITY("%", "humidity_pct"),
    WIND("km/h", "wind_kmh"),
}

data class HourlyObservation(
    val observedAt: Instant,
    val temperatureC: Double?,
    val humidityPct: Double?,
    val windKmh: Double?,
)
