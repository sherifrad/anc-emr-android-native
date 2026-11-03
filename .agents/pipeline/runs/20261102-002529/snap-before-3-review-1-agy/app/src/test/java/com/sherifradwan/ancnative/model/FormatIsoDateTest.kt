package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatIsoDateTest {
    @Test
    fun ordinaryDateIsZeroPaddedIso() {
        assertEquals("2026-03-07", formatIsoDate(LocalDate.of(2026, 3, 7)))
    }

    @Test
    fun leapDayIsFormatted() {
        assertEquals("2024-02-29", formatIsoDate(LocalDate.of(2024, 2, 29)))
    }

    @Test
    fun yearBoundariesAreFormatted() {
        assertEquals("2025-12-31", formatIsoDate(LocalDate.of(2025, 12, 31)))
        assertEquals("2026-01-01", formatIsoDate(LocalDate.of(2026, 1, 1)))
    }
}
