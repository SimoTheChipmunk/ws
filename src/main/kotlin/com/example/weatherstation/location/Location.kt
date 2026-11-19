package com.example.weatherstation.location

import java.time.ZoneId

data class Location(val id: Long, val name: String, val latitude: Double, val longitude: Double, val timezone: String) {
    val zoneId: ZoneId get() = ZoneId.of(timezone)
}
