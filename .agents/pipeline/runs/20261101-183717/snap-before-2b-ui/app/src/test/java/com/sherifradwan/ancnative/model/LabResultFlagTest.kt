package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LabResultFlagTest {
    @Test
    fun rangeBelowLowerEndIsLow() {
        assertEquals(LabResultFlag.LOW, labResultFlag("10.9", "11.0 - 15.0"))
    }

    @Test
    fun rangeInsideIsNormal() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("13", "11.0 - 15.0"))
    }

    @Test
    fun rangeEndsAreInclusive() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("11.0", "11.0 - 15.0"))
        assertEquals(LabResultFlag.NORMAL, labResultFlag("15", "11.0 - 15.0"))
    }

    @Test
    fun rangeAboveUpperEndIsHigh() {
        assertEquals(LabResultFlag.HIGH, labResultFlag("15.1", "11.0 - 15.0"))
    }

    @Test
    fun enDashAndSpacingVariantsAreAccepted() {
        assertEquals(LabResultFlag.HIGH, labResultFlag("16", "11–15"))
        assertEquals(LabResultFlag.LOW, labResultFlag("10", "11 – 15"))
        assertEquals(LabResultFlag.NORMAL, labResultFlag("12", "11-15"))
        assertEquals(LabResultFlag.NORMAL, labResultFlag("12", "  11   -   15  "))
    }

    @Test
    fun inputsAreTrimmed() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("  12 ", " 11 - 15 "))
    }

    @Test
    fun lessThanRange() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("4.9", "< 5"))
        assertEquals(LabResultFlag.HIGH, labResultFlag("5", "< 5"))
        assertEquals(LabResultFlag.HIGH, labResultFlag("6", "<5"))
    }

    @Test
    fun lessThanOrEqualRange() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("5", "<= 5"))
        assertEquals(LabResultFlag.HIGH, labResultFlag("5.1", "<= 5"))
    }

    @Test
    fun greaterThanRange() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("5.1", "> 5"))
        assertEquals(LabResultFlag.LOW, labResultFlag("5", "> 5"))
        assertEquals(LabResultFlag.LOW, labResultFlag("4", ">5"))
    }

    @Test
    fun greaterThanOrEqualRange() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("5", ">= 5"))
        assertEquals(LabResultFlag.LOW, labResultFlag("4.9", ">= 5"))
    }

    @Test
    fun decimalsAreCompared() {
        assertEquals(LabResultFlag.NORMAL, labResultFlag("0.45", "0.4 - 4.0"))
        assertEquals(LabResultFlag.LOW, labResultFlag("0.39", "0.4 - 4.0"))
        assertEquals(LabResultFlag.HIGH, labResultFlag("4.01", "0.4 - 4.0"))
    }

    @Test
    fun textResultIsUnknown() {
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("Positive", "11 - 15"))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("5.0 g/dL", "11 - 15"))
    }

    @Test
    fun emptyResultIsUnknown() {
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("", "11 - 15"))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("   ", "11 - 15"))
    }

    @Test
    fun resultWithComparatorIsUnknown() {
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("<5", "11 - 15"))
    }

    @Test
    fun emptyOrJunkRangeIsUnknown() {
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("12", ""))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("12", "   "))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("12", "normal"))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("12", "11 - "))
        assertEquals(LabResultFlag.UNKNOWN, labResultFlag("12", "< abc"))
    }
}
