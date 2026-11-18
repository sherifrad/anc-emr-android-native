package com.sherifradwan.ancnative.ui.report

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintCrashFixTest {

    private class FakeActivity : Activity()

    private class FakeContextWrapper(private var base: Context? = null) : ContextWrapper(base) {
        override fun getBaseContext(): Context? = base
        fun setBase(newBase: Context?) {
            base = newBase
        }
    }

    @Test
    fun printJobName_withSpaces_replacesWithSingleUnderscoreAndTrims() {
        val result = PatientPrintManager.printJobName("Jane Doe", "CASE 101")
        assertEquals("Jane_Doe_CASE_101", result)

        val multipleSpaces = PatientPrintManager.printJobName("   Jane    Doe   ", "   CASE   101   ")
        assertEquals("Jane_Doe_CASE_101", multipleSpaces)
    }

    @Test
    fun printJobName_withArabicName_preservesArabicLetters() {
        val result = PatientPrintManager.printJobName("فاطمة أحمد", "CASE-102")
        assertEquals("فاطمة_أحمد_CASE-102", result)

        val fullArabic = PatientPrintManager.printJobName("مريم", "حالة-42")
        assertEquals("مريم_حالة-42", fullArabic)
    }

    @Test
    fun printJobName_withSlashesAndColons_sanitizesToUnderscore() {
        val result = PatientPrintManager.printJobName("Clinic/Ward:A/Bed:12", "CASE:99/2026")
        assertEquals("Clinic_Ward_A_Bed_12_CASE_99_2026", result)

        val consecutive = PatientPrintManager.printJobName("Patient//Name::ID", "CASE///001")
        assertEquals("Patient_Name_ID_CASE_001", consecutive)
    }

    @Test
    fun printJobName_emptyOrDisallowedOnly_returnsFallback() {
        val emptyResult = PatientPrintManager.printJobName("", "")
        assertEquals("Patient_Report", emptyResult)

        val spacesResult = PatientPrintManager.printJobName("     ", "   ")
        assertEquals("Patient_Report", spacesResult)

        val disallowedOnly = PatientPrintManager.printJobName("///:::", ":::///")
        assertEquals("Patient_Report", disallowedOnly)
    }

    @Test
    fun printJobName_twoHundredCharacterName_capsAtSixtyNeverEmptyOnlyAllowedChars() {
        val longDisplayName = "A".repeat(200)
        val result = PatientPrintManager.printJobName(longDisplayName, "CASE-01")

        assertTrue("Result must not exceed 60 chars", result.length <= 60)
        assertEquals(60, result.length)
        assertTrue("Result must never be empty", result.isNotEmpty())
        assertTrue(
            "Result must only contain letters, digits, underscores, or dashes",
            result.all { Character.isLetterOrDigit(it) || it == '-' || it == '_' }
        )
        assertFalse("Result must not end with underscore", result.endsWith('_'))

        // Test with 200 chars having separators near the 60 char boundary
        val mixedLong = ("Name/With:Special_And-Chars " + "X".repeat(50)).repeat(4)
        val mixedResult = PatientPrintManager.printJobName(mixedLong, "ID/123")

        assertTrue("Mixed result must not exceed 60 chars", mixedResult.length <= 60)
        assertTrue("Mixed result must never be empty", mixedResult.isNotEmpty())
        assertTrue(
            "Mixed result must only contain letters, digits, underscores, or dashes",
            mixedResult.all { Character.isLetterOrDigit(it) || it == '-' || it == '_' }
        )
        assertFalse("Mixed result must not end with underscore", mixedResult.endsWith('_'))
    }

    @Test
    fun findActivity_throughTwoNestedContextWrappers_returnsActivity() {
        val activity = FakeActivity()
        val innerWrapper = FakeContextWrapper(activity)
        val outerWrapper = FakeContextWrapper(innerWrapper)

        val found = outerWrapper.findActivity()
        assertNotNull(found)
        assertSame(activity, found)
    }

    @Test
    fun findActivity_plainContext_returnsNull() {
        val plainFakeWrapper = FakeContextWrapper(null)
        assertNull(plainFakeWrapper.findActivity())

        val plainContextWrapper = ContextWrapper(null)
        assertNull(plainContextWrapper.findActivity())
    }

    @Test
    fun findActivity_selfReferencingWrapper_doesNotLoopAndReturnsNull() {
        val cycleWrapper = FakeContextWrapper(null)
        cycleWrapper.setBase(cycleWrapper)

        assertNull(cycleWrapper.findActivity())

        // Also verify multi-node cycle termination
        val nodeA = FakeContextWrapper(null)
        val nodeB = FakeContextWrapper(nodeA)
        nodeA.setBase(nodeB)

        assertNull(nodeB.findActivity())
    }
}
