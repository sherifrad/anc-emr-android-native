package com.sherifradwan.ancnative.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatDmyTest {
    @Test
    fun nullGivesNull() {
        assertNull(formatDmy(null))
    }

    @Test
    fun blankGivesNull() {
        assertNull(formatDmy(""))
        assertNull(formatDmy("   "))
    }

    @Test
    fun validIsoIsDayMonthYearZeroPadded() {
        assertEquals("07/03/2026", formatDmy("2026-03-07"))
        assertEquals("29/02/2024", formatDmy("2024-02-29"))
    }

    @Test
    fun surroundingWhitespaceIsIgnoredForValidDates() {
        assertEquals("05/01/2026", formatDmy("  2026-01-05 "))
    }

    @Test
    fun malformedReturnsTrimmedRaw() {
        assertEquals("not a date", formatDmy("  not a date "))
        assertEquals("2026-1-5", formatDmy("2026-1-5"))
    }

    @Test
    fun outOfRangeDateReturnsTrimmedRaw() {
        assertEquals("2026-02-30", formatDmy(" 2026-02-30"))
    }

    @Test
    fun defaultLocaleDoesNotChangeDigitsOrOrder() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            assertEquals("07/03/2026", formatDmy("2026-03-07"))
            Locale.setDefault(Locale.forLanguageTag("hi-IN-u-nu-deva"))
            assertEquals("07/03/2026", formatDmy("2026-03-07"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
