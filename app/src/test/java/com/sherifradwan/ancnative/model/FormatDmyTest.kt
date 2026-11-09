package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
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

    private val hostileLocales = listOf(
        "th-TH-u-ca-buddhist-nu-thai",
        "ar-SA-u-ca-islamic-umalqura-nu-arab",
        "hi-IN-u-nu-deva",
        "fa-IR-u-ca-persian-nu-arabext",
    )

    private inline fun withDefaultLocale(tag: String, block: () -> Unit) {
        val default = Locale.getDefault()
        val format = Locale.getDefault(Locale.Category.FORMAT)
        try {
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            Locale.setDefault(Locale.Category.FORMAT, locale)
            block()
        } finally {
            Locale.setDefault(default)
            Locale.setDefault(Locale.Category.FORMAT, format)
        }
    }

    @Test
    fun formattingIgnoresTheDefaultLocale() {
        for (tag in hostileLocales) {
            withDefaultLocale(tag) {
                assertEquals(tag, "07/03/2026", formatDmy("2026-03-07"))
                assertEquals(tag, "07/03/2026", formatDmy(LocalDate.of(2026, 3, 7)))
            }
        }
    }

    // The formatter is a top-level val built when IsoDatesKt loads. A fresh copy of the class loaded under a
    // hostile default locale fails if the formatter captures that locale (Thai digits and Buddhist year 2569
    // with localizedBy or a default-locale String.format), which the in-process test above cannot see.
    @Test
    fun formatterBuiltUnderAHostileDefaultLocaleStillPrintsAsciiGregorian() {
        for (tag in hostileLocales) {
            withDefaultLocale(tag) {
                val facade = FreshIsoDatesLoader().loadClass(ISO_DATES_FACADE)
                assertNotSame("the facade must be a fresh copy so its formatter is rebuilt", ISO_DATES_CLASS, facade)
                val format = facade.getMethod("formatDmy", String::class.java)
                assertEquals(tag, "07/03/2026", format.invoke(null, "2026-03-07"))
            }
        }
    }

    private class FreshIsoDatesLoader : ClassLoader(FormatDmyTest::class.java.classLoader) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name != ISO_DATES_FACADE) return super.loadClass(name, resolve)
            val bytes = parent.getResourceAsStream(name.replace('.', '/') + ".class")!!.use { it.readBytes() }
            return defineClass(name, bytes, 0, bytes.size)
        }
    }

    private companion object {
        const val ISO_DATES_FACADE = "com.sherifradwan.ancnative.model.IsoDatesKt"
        val ISO_DATES_CLASS: Class<*> = Class.forName(ISO_DATES_FACADE)
    }
}
