package com.example.weatherstation.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateRangeTest {

    @Test
    fun `to before from throws InvalidRequestException`() {
        val from = LocalDate.parse("2026-01-02")
        val to = LocalDate.parse("2026-01-01")

        assertThrows(InvalidRequestException::class.java) {
            DateRange(from = from, to = to)
        }
    }

    @Test
    fun `days is inclusive and same day returns one`() {
        val sameDayRange = DateRange(
            from = LocalDate.parse("2026-01-01"),
            to = LocalDate.parse("2026-01-01"),
        )

        assertEquals(1L, sameDayRange.days)

        val multipleDaysRange = DateRange(
            from = LocalDate.parse("2026-01-01"),
            to = LocalDate.parse("2026-01-03"),
        )

        assertEquals(3L, multipleDaysRange.days)
    }

    @Test
    fun `toWindow converts Bucharest local dates to UTC instants`() {
        val zone = ZoneId.of("Europe/Bucharest")

        val range = DateRange(
            from = LocalDate.parse("2026-01-01"),
            to = LocalDate.parse("2026-01-01"),
        )

        val window = context(zone) {
            range.toWindow()
        }

        assertEquals(
            Instant.parse("2025-12-31T22:00:00Z"),
            window.startInclusive,
        )

        assertEquals(
            Instant.parse("2026-01-01T22:00:00Z"),
            window.endExclusive,
        )
    }

    @Test
    fun `toWindow produces a 23 hour window on Bucharest DST transition day`() {
        val zone = ZoneId.of("Europe/Bucharest")

        val range = DateRange(
            from = LocalDate.parse("2026-03-29"),
            to = LocalDate.parse("2026-03-29"),
        )

        val window = context(zone) {
            range.toWindow()
        }

        assertEquals(
            Instant.parse("2026-03-28T22:00:00Z"),
            window.startInclusive,
        )

        assertEquals(
            Instant.parse("2026-03-29T21:00:00Z"),
            window.endExclusive,
        )

        val duration = Duration.between(
            window.startInclusive,
            window.endExclusive,
        )

        assertEquals(Duration.ofHours(23), duration)
    }
}
