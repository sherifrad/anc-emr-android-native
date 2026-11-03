package com.sherifradwan.ancnative.model.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabItemEditTest {
    private val hb = LabCodexItem(id = "a", testName = "Hemoglobin", category = "Hematology", unit = "g/dL", referenceRange = "11-14")
    private val glucose = LabCodexItem(id = "b", testName = "Glucose", category = "Biochemistry", unit = "mg/dL", referenceRange = "70-99")
    private val urine = LabCodexItem(id = "c", testName = "Urine protein", category = "Urinalysis")
    private val items = listOf(hb, glucose, urine)

    private val anemia = LabCategoryGroup(id = "g1", name = "Anemia", testNames = listOf("hemoglobin", "Ferritin"))
    private val booking = LabCategoryGroup(id = "g2", name = "Booking", testNames = listOf("Glucose", "HEMOGLOBIN", "Urine protein"))
    private val diabetes = LabCategoryGroup(id = "g3", name = "Diabetes", testNames = listOf("Glucose"))
    private val groups = listOf(anemia, booking, diabetes)

    @Test
    fun renameUpdatesItemAndGroupMembershipIgnoringCase() {
        val result = applyLabItemEdit(items, groups, hb.copy(testName = "Hb"))!!

        assertEquals("Hb", result.items[0].testName)
        assertEquals(listOf("Hb", "Ferritin"), result.groups[0].testNames)
        assertEquals(listOf("Glucose", "Hb", "Urine protein"), result.groups[1].testNames)
    }

    @Test
    fun renameLeavesOtherGroupsUntouched() {
        val result = applyLabItemEdit(items, groups, hb.copy(testName = "Hb"))!!

        assertEquals(diabetes, result.groups[2])
        assertEquals(groups.map { it.id }, result.groups.map { it.id })
    }

    @Test
    fun unitAndRangeChangeKeepsIdAndPosition() {
        val result = applyLabItemEdit(items, groups, glucose.copy(unit = "mmol/L", referenceRange = "3.9-5.5"))!!

        assertEquals(listOf("a", "b", "c"), result.items.map { it.id })
        assertEquals("mmol/L", result.items[1].unit)
        assertEquals("3.9-5.5", result.items[1].referenceRange)
        assertEquals(groups, result.groups)
    }

    @Test
    fun blankNameReturnsNull() {
        assertNull(applyLabItemEdit(items, groups, hb.copy(testName = "   ")))
    }

    @Test
    fun unknownIdReturnsNull() {
        assertNull(applyLabItemEdit(items, groups, hb.copy(id = "missing", testName = "Hb")))
    }

    @Test
    fun nameUsedByAnotherItemReturnsNull() {
        assertNull(applyLabItemEdit(items, groups, hb.copy(testName = "glucose")))
    }

    @Test
    fun sameNameDifferentCaseOnSameItemIsAllowed() {
        val result = applyLabItemEdit(items, groups, hb.copy(testName = "HEMOGLOBIN"))
        assertNotNull(result)
        assertEquals("HEMOGLOBIN", result!!.items[0].testName)
        assertEquals(listOf("HEMOGLOBIN", "Ferritin"), result.groups[0].testNames)
    }

    @Test
    fun valuesAreTrimmed() {
        val edited = hb.copy(testName = "  Hb  ", category = " Hematology ", unit = " g/L ", referenceRange = " 110-140 ", defaultResult = " 12 ")
        val saved = applyLabItemEdit(items, groups, edited)!!.items[0]

        assertEquals("Hb", saved.testName)
        assertEquals("Hematology", saved.category)
        assertEquals("g/L", saved.unit)
        assertEquals("110-140", saved.referenceRange)
        assertEquals("12", saved.defaultResult)
    }

    @Test
    fun editedItemIsMarkedCustom() {
        assertTrue(!hb.isCustom)
        assertTrue(applyLabItemEdit(items, groups, hb.copy(unit = "g/L"))!!.items[0].isCustom)
    }
}
