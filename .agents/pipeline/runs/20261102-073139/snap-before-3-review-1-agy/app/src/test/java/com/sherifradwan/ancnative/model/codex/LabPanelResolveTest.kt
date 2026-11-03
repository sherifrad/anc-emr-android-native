package com.sherifradwan.ancnative.model.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabPanelResolveTest {
    private val hb = LabCodexItem(id = "a", testName = "Hemoglobin", category = "Hematology", unit = "g/dL", referenceRange = "11-14")
    private val glucose = LabCodexItem(id = "b", testName = "Glucose", category = "Biochemistry")
    private val tsh = LabCodexItem(id = "c", testName = "TSH", category = "Biochemistry")
    private val items = listOf(hb, glucose, tsh)

    @Test
    fun everyDefaultPanelResolvesWithNothingMissing() {
        val defaultItems = ClinicalCodexRepository.defaultLabCodexItems()
        for (panel in ClinicalCodexRepository.defaultLabCategoryGroups()) {
            val resolution = resolveLabPanel(panel, defaultItems)
            assertTrue(panel.name + " missing " + resolution.missing, resolution.missing.isEmpty())
            assertEquals(panel.name, panel.testNames.size, resolution.tests.size)
        }
    }

    @Test
    fun testsFollowThePanelOrder() {
        val panel = LabCategoryGroup(name = "P", testNames = listOf("TSH", "Hemoglobin", "Glucose"))

        assertEquals(listOf(tsh, hb, glucose), resolveLabPanel(panel, items).tests)
    }

    @Test
    fun matchingIgnoresCaseAndSurroundingSpaces() {
        val panel = LabCategoryGroup(name = "P", testNames = listOf("  hemoglobin ", "GLUCOSE"))

        val resolution = resolveLabPanel(panel, items)

        assertEquals(listOf(hb, glucose), resolution.tests)
        assertTrue(resolution.missing.isEmpty())
    }

    @Test
    fun unknownNamesGoToMissingInOrder() {
        val panel = LabCategoryGroup(name = "P", testNames = listOf("Ferritin", "TSH", "Zinc"))

        val resolution = resolveLabPanel(panel, items)

        assertEquals(listOf(tsh), resolution.tests)
        assertEquals(listOf("Ferritin", "Zinc"), resolution.missing)
    }

    @Test
    fun sameTestIsNotReturnedTwice() {
        val panel = LabCategoryGroup(name = "P", testNames = listOf("TSH", "tsh", "Glucose", " TSH "))

        assertEquals(listOf(tsh, glucose), resolveLabPanel(panel, items).tests)
    }

    @Test
    fun renamedTestStillResolvesInItsPanel() {
        val panel = LabCategoryGroup(name = "P", testNames = listOf("Hemoglobin", "Glucose"))
        val edited = applyLabItemEdit(items, listOf(panel), hb.copy(testName = "Hb"))!!

        val resolution = resolveLabPanel(edited.groups[0], edited.items)

        assertEquals(listOf("Hb", "Glucose"), resolution.tests.map { it.testName })
        assertTrue(resolution.missing.isEmpty())
    }
}
