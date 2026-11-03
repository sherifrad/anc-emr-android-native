package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Q78 (SPEC_FIX_R3.md round 3k): ONE rule for a visit weight as displayed,
 * [formatWeightKg]. null/blank -> null; otherwise the trimmed text, with " kg"
 * appended only when it does not already end in "kg" (case-insensitive, ignoring
 * trailing spaces) or in "kilograms"/"kilogram". Never "74.5 kg kg", never a
 * bare "74.5".
 */
class FormatWeightKgTest {

    @Test
    fun bareNumberGetsKgAppended() {
        assertEquals("74.5 kg", formatWeightKg("74.5"))
    }

    @Test
    fun valueAlreadyCarryingKgIsUnchanged() {
        assertEquals("74.5 kg", formatWeightKg("74.5 kg"))
        assertEquals("74.5KG", formatWeightKg("74.5KG"))
        assertEquals("63 kg", formatWeightKg(" 63 kg "))
    }

    @Test
    fun kilogramsWordingIsAlreadyUnitBearing() {
        // The spec: "63 kilograms kg" is NOT acceptable - text ending with
        // "kilograms"/"kilogram" counts as already unit-bearing.
        assertEquals("63 kilograms", formatWeightKg("63 kilograms"))
        assertEquals("63 kilogram", formatWeightKg("63 kilogram"))
    }

    @Test
    fun missingWeightIsNull() {
        assertNull(formatWeightKg(null))
        assertNull(formatWeightKg(""))
        assertNull(formatWeightKg("   "))
    }
}
