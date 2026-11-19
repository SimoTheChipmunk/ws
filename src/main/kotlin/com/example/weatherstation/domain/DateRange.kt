package com.example.weatherstation.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DateRange(val from: LocalDate, val to: LocalDate) {
    init {
        if (to.isBefore(from)) throw InvalidRequestException("'to' ($to) must not be before 'from' ($from)")
    }

    val days: Long get() = ChronoUnit.DAYS.between(from, to) + 1
}

data class TimeWindow(val startInclusive: Instant, val endExclusive: Instant)

context(zone: ZoneId)
fun DateRange.toWindow(): TimeWindow =
    TimeWindow(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant())
