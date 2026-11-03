package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 2 tests (Q5 age table, Q6 formatBloodGroup/isRhPositive tables) from
 * .agents/pipeline/SPEC_FIX_R2.md, written from the spec only, red phase.
 *
 * Expected today: compile failure for EXACTLY the two new names
 * isRhPositive / formatBloodGroup (formatAgeYears already exists; the Q5 table
 * refines its behaviour and must fail at runtime, not compile time).
 *
 * SAFE assumptions:
 *  - isRhPositive is the "mirror of isRhNegative": the same normalisation with the
 *    sense flipped, so "A+, antibody screen negative" (P6: isRhNegative false) is
 *    treated as positive; only blank/null/"pending"/actual-negative groups are false.
 *  - formatBloodGroup never alters the value wording - it only trims and appends
 *    " (Rh negative)" for Rh-negative groups; returned strings contain no emoji.
 */
class ClinicalFormatR2Test {

    // ---- Q5 formatAgeYears (fixes R2-4; supersedes P8 extraction) ----
    // Whole trimmed text must match: optional "Age" + optional ":", an integer,
    // optional unit y|yr|yrs|year|years with optional ".", case-insensitive,
    // AND 10 <= n <= 70. Returns the number only.
    private data class AgeCase(val input: String?, val expect: String?) {
        override fun toString() = "input=${input?.let { "\"$it\"" } ?: "null"} expect=$expect"
    }

    private val AGE_ACCEPT = listOf(
        // Spec examples (all -> "29")
        AgeCase("29", "29"),
        AgeCase("29 years", "29"),
        AgeCase("29 Years", "29"),
        AgeCase("29 yrs.", "29"),
        AgeCase("29 yr", "29"),
        AgeCase("29y", "29"),
        AgeCase("  29 ", "29"),
        AgeCase("Age 29", "29"),
        AgeCase("Age: 29", "29"),
        // Own boundary cases at the 10..70 gates
        AgeCase("10", "10"),
        AgeCase("70", "70"),
        AgeCase("Age 70 yrs", "70"),
    )

    private val AGE_REJECT = listOf(
        // Spec examples (all -> null)
        AgeCase(null, null),
        AgeCase("", null),
        AgeCase("yrs", null),
        AgeCase("unknown", null),
        AgeCase("1990-05-01", null),
        AgeCase("290", null),
        AgeCase("9", null),
        AgeCase("71", null),
        AgeCase("29 years 3 months", null),
        AgeCase("about 30", null),
    )

    @Test
    fun formatAgeYears_r2_acceptsWholeTextMatches() {
        for (c in AGE_ACCEPT) assertEquals("formatAgeYears(${c})", c.expect, formatAgeYears(c.input))
    }

    @Test
    fun formatAgeYears_r2_rejectsEverythingElse() {
        for (c in AGE_REJECT) assertEquals("formatAgeYears(${c})", c.expect, formatAgeYears(c.input))
    }

    // ---- Q6 isRhPositive (mirror of isRhNegative) ----
    private data class RhCase(val input: String?, val expect: Boolean) {
        override fun toString() = "input=${input?.let { "\"$it\"" } ?: "null"}"
    }

    private val POSITIVE_TRUE = listOf(
        // Spec examples
        RhCase("A+", true),
        RhCase("O pos", true),
        RhCase("B Positive", true),
        RhCase("AB +", true),
        // Own boundary cases
        RhCase("B+", true),                           // same family as "A+"
        RhCase("A+, antibody screen negative", true), // P6 says this is NOT negative; mirror -> positive
        RhCase("b positive", true),                   // mirror keeps isRhNegative's case tolerance
    )

    private val POSITIVE_FALSE = listOf(
        // Spec examples
        RhCase("O-", false),
        RhCase("O neg", false),
        RhCase("pending", false),
        RhCase("", false),
        RhCase(null, false),
        // Own boundary cases
        RhCase("O −", false),      // U+2212 minus: isRhNegative true (P6) -> not positive
        RhCase("O Rh(-)", false),  // whole -ve pattern family from P6
        RhCase("AB Negative", false),
    )

    @Test
    fun isRhPositive_trueCases() {
        for (c in POSITIVE_TRUE) assertEquals("isRhPositive(${c})", c.expect, isRhPositive(c.input))
    }

    @Test
    fun isRhPositive_falseCases() {
        for (c in POSITIVE_FALSE) assertEquals("isRhPositive(${c})", c.expect, isRhPositive(c.input))
    }

    /**
     * Mirror integrity: for every typed group where P6 defines isRhNegative, the two
     * helpers must agree ("pending" is the documented dual-negative exception and is
     * not in this table; blank/null already covered above).
     */
    @Test
    fun isRhPositive_mirrors_isRhNegative() {
        for (g in listOf("A-", "O-", "O −", "O neg", "AB Negative", "B Rh -ve", "O Rh(-)", "O minus")) {
            assertFalse("isRhPositive($g) must mirror isRhNegative = true", isRhPositive(g))
        }
        for (g in listOf("A+", "O pos", "A+, antibody screen negative", "B Rh +ve", "Rh positive")) {
            assertTrue("isRhPositive($g) must mirror isRhNegative = false", isRhPositive(g))
        }
    }

    // ---- Q6 formatBloodGroup (D1, D3) ----
    @Test
    fun formatBloodGroup_r2_specExamples() {
        assertEquals("O- (Rh negative)", formatBloodGroup("O-"))
        assertEquals("AB Negative (Rh negative)", formatBloodGroup("AB Negative"))
        assertEquals("A+", formatBloodGroup("A+"))
        assertEquals("B+", formatBloodGroup("  B+ "))
        assertEquals("pending", formatBloodGroup("pending"))
        assertNull(formatBloodGroup(""))
        assertNull(formatBloodGroup(null))
    }

    @Test
    fun formatBloodGroup_ownBoundaries() {
        assertEquals("O pos", formatBloodGroup("O pos"))   // not Rh-negative -> trimmed value only
        assertEquals("AB +", formatBloodGroup("AB +"))     // keeps the typed wording of the value
        assertNull(formatBloodGroup("   "))                // whitespace-only behaves like blank
    }

    @Test
    fun formatBloodGroup_neverAddsEmoji() {
        val inputs = listOf("O-", "AB Negative", "A+", "  B+ ", "pending", "O pos", "AB +")
        for (i in inputs) {
            val result = formatBloodGroup(i) ?: continue
            val hasForbidden = result.any { ch ->
                val c = ch.code
                c in 0x1F000..0x1FAFF || c in 0x2600..0x27BF || c in 0x2B00..0x2BFF || c == 0xFE0F
            }
            assertFalse("formatBloodGroup($i) must not add emoji or glyph code points", hasForbidden)
        }
    }
}
