package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsoDatesTest {
    @Test
    fun lenientParseTrimsAndAcceptsIso() {
        assertEquals(LocalDate.of(2026, 1, 5), parseIsoDateOrNull(" 2026-01-05 "))
        assertEquals(LocalDate.of(2026, 1, 5), parseIsoDateOrNull("2026-01-05"))
    }

    @Test
    fun lenientParseRejectsNonIso() {
        assertNull(parseIsoDateOrNull("2026-1-5"))
        assertNull(parseIsoDateOrNull(""))
        assertNull(parseIsoDateOrNull(null))
        assertNull(parseIsoDateOrNull("abc"))
    }

    @Test
    fun canonicalParseAcceptsOnlyExactIso() {
        assertEquals(LocalDate.of(2026, 1, 5), parseCanonicalIsoDateOrNull("2026-01-05"))
        assertNull(parseCanonicalIsoDateOrNull(" 2026-01-05"))
        assertNull(parseCanonicalIsoDateOrNull("2026-02-30"))
        assertNull(parseCanonicalIsoDateOrNull("2026-1-05"))
    }

    @Test
    fun eddIsLmpPlusFullTerm() {
        assertEquals(280L, FULL_TERM_DAYS)
        assertEquals(LocalDate.of(2026, 10, 8), eddFromLmp(LocalDate.of(2026, 1, 1)))
    }
}
