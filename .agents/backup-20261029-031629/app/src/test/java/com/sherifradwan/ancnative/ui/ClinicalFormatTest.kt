package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec-derived tests (P6, P7, P8) for ClinicalFormat.kt pure functions.
 * Table-driven: every example from the spec plus three boundary cases each.
 * Red phase per MACA lane contract: the functions do not exist yet; that is expected.
 */
class ClinicalFormatTest {

    private data class RhCase(val input: String?, val expect: Boolean) {
        override fun toString() = "input=${input?.let { "\"$it\"" } ?: "null"}"
    }

    // ---- P6 isRhNegative ----
    private val RH_TRUE = listOf(
        // Spec examples
        RhCase("A-", true),
        RhCase("O-", true),
        RhCase("O −", true),          // U+2212 minus
        RhCase("O neg", true),
        RhCase("AB Negative", true),
        RhCase("B Rh -ve", true),
        RhCase("O Rh(-)", true),
        RhCase("O minus", true),
        // My boundary cases
        RhCase("o-", true),           // case-insensitive (spec already treats "high"/"High" equivalently in P7)
        RhCase("  O-  ", true),       // surrounding whitespace must not hide a negative status
        RhCase("AB Rh -ve", true),    // same pattern family as "B Rh -ve"
    )

    private val RH_FALSE = listOf(
        // Spec examples
        RhCase("A+", false),
        RhCase("O pos", false),
        RhCase("A+, antibody screen negative", false),  // 'negative' here refers to the screen, not the group
        RhCase("", false),
        RhCase(null, false),
        RhCase("pending", false),
        // My boundary cases
        RhCase("B Rh +ve", false),
        RhCase("O+", false),
        RhCase("Rh positive", false),
    )

    @Test
    fun rhNegative_trueCases() {
        for (c in RH_TRUE) assertEquals("isRhNegative(${c})", true, isRhNegative(c.input))
    }

    @Test
    fun rhNegative_falseCases() {
        for (c in RH_FALSE) assertEquals("isRhNegative(${c})", false, isRhNegative(c.input))
    }

    // ---- P7 isHighRisk ----
    private data class RiskCase(val input: String?, val expect: Boolean) {
        override fun toString() = "input=${input?.let { "\"$it\"" } ?: "null"}"
    }

    private val RISK_TRUE = listOf(
        // Spec examples
        RiskCase("High", true),
        RiskCase("High Risk", true),
        RiskCase("high", true),
        // My boundary cases
        RiskCase("HIGH RISK", true),   // all caps still emphasise
        RiskCase("  High  ", true),    // padding must not hide risk
        RiskCase("high risk", true),   // lowercase compound
    )

    private val RISK_FALSE = listOf(
        // Spec examples
        RiskCase("Not high risk", false),
        RiskCase("Low", false),
        RiskCase("Moderate", false),
        RiskCase(null, false),
        RiskCase("", false),
        // My boundary cases
        RiskCase("Not High", false),   // negation prefix
        RiskCase("not high", false),   // lowercase negation prefix
        RiskCase("Low risk", false),
    )

    @Test
    fun highRisk_trueCases() {
        for (c in RISK_TRUE) assertEquals("isHighRisk(${c})", true, isHighRisk(c.input))
    }

    @Test
    fun highRisk_falseCases() {
        for (c in RISK_FALSE) assertEquals("isHighRisk(${c})", false, isHighRisk(c.input))
    }

    // ---- P8 formatAgeYears ----
    private data class AgeCase(val input: String?, val expect: String?) {
        override fun toString() = "input=${input?.let { "\"$it\"" } ?: "null"} expect=$expect"
    }

    private val AGE_SPEC = listOf(
        AgeCase("29", "29"),
        AgeCase("29 years", "29"),
        AgeCase("29 Years", "29"),
        AgeCase("29 yrs.", "29"),
        AgeCase("29 yr", "29"),
        AgeCase("  29 ", "29"),
        AgeCase("Age 29", "29"),
        AgeCase(null, null),
        AgeCase("", null),
        AgeCase("yrs", null),
        AgeCase("unknown", null),
    )

    private val AGE_BOUNDARY = listOf(
        AgeCase("0", "0"),        // digits only rule: "0" is digits
        AgeCase("Age 33", "33"),  // label variant with different value
        AgeCase("   ", null),     // whitespace-only behaves like blank
    )

    @Test
    fun formatAgeYears_specExamples() {
        for (c in AGE_SPEC) assertEquals("formatAgeYears(${c})", c.expect, formatAgeYears(c.input))
    }

    @Test
    fun formatAgeYears_boundaryCases() {
        for (c in AGE_BOUNDARY) assertEquals("formatAgeYears(${c})", c.expect, formatAgeYears(c.input))
    }
}
