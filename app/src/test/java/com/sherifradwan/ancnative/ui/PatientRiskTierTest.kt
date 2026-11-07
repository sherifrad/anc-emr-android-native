package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PatientRiskTierTest {

    @Test
    fun parseRiskTier_categorizesHighRiskCorrectly() {
        assertEquals(PatientRiskTier.HIGH, parseRiskTier("High"))
        assertEquals(PatientRiskTier.HIGH, parseRiskTier("high"))
        assertEquals(PatientRiskTier.HIGH, parseRiskTier("High Risk"))
        assertEquals(PatientRiskTier.HIGH, parseRiskTier("HIGH"))
    }

    @Test
    fun parseRiskTier_categorizesMediumRiskCorrectly() {
        assertEquals(PatientRiskTier.MEDIUM, parseRiskTier("Medium"))
        assertEquals(PatientRiskTier.MEDIUM, parseRiskTier("medium"))
        assertEquals(PatientRiskTier.MEDIUM, parseRiskTier("Moderate"))
        assertEquals(PatientRiskTier.MEDIUM, parseRiskTier("moderate risk"))
    }

    @Test
    fun parseRiskTier_categorizesLowRiskCorrectly() {
        assertEquals(PatientRiskTier.LOW, parseRiskTier("Low"))
        assertEquals(PatientRiskTier.LOW, parseRiskTier("low"))
        assertEquals(PatientRiskTier.LOW, parseRiskTier("Normal"))
        assertEquals(PatientRiskTier.LOW, parseRiskTier("low risk"))
    }

    @Test
    fun parseRiskTier_categorizesUnrecordedOrBlankCorrectly() {
        assertEquals(PatientRiskTier.UNRECORDED, parseRiskTier(null))
        assertEquals(PatientRiskTier.UNRECORDED, parseRiskTier(""))
        assertEquals(PatientRiskTier.UNRECORDED, parseRiskTier("   "))
        assertEquals(PatientRiskTier.UNRECORDED, parseRiskTier("unspecified"))
    }
}
