package com.sherifradwan.ancnative.model.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalCodexModelsTest {

    @Test
    fun testLabCodexItemModelAndDisplayLabel() {
        val labItem = LabCodexItem(
            id = "test-lab-1",
            testName = "Serum Ferritin",
            category = "Biochemistry",
            unit = "ng/mL",
            referenceRange = "15 - 150",
            defaultResult = "Normal",
            isCustom = true,
        )
        assertEquals("Serum Ferritin", labItem.testName)
        assertEquals("Serum Ferritin", labItem.displayLabel)
        assertEquals("Biochemistry", labItem.category)
        assertEquals("ng/mL", labItem.unit)
        assertEquals("15 - 150", labItem.referenceRange)
        assertEquals("Normal", labItem.defaultResult)
        assertTrue(labItem.isCustom)
    }

    @Test
    fun testMedicationCodexItemModelAndDisplayLabel() {
        val medWithDosage = MedicationCodexItem(
            id = "test-med-1",
            name = "Folic Acid",
            dosage = "5 mg",
            route = "Oral",
            frequency = "Once daily",
            isProphylaxis = true,
            isCustom = false,
        )
        assertEquals("Folic Acid 5 mg", medWithDosage.displayLabel)
        assertTrue(medWithDosage.isProphylaxis)
        assertEquals("Oral", medWithDosage.route)
        assertEquals("Once daily", medWithDosage.frequency)

        val medWithoutDosage = MedicationCodexItem(
            id = "test-med-2",
            name = "Paracetamol",
            dosage = "",
            isCustom = true,
        )
        assertEquals("Paracetamol", medWithoutDosage.displayLabel)
        assertTrue(medWithoutDosage.isCustom)
    }

    @Test
    fun testMockCodexDelegateOperations() {
        // Test in-memory implementation of delegates
        val labList = mutableListOf<LabCodexItem>()
        val labDelegate = object : LabCodexDelegate {
            override val labCodexItems = kotlinx.coroutines.flow.MutableStateFlow<List<LabCodexItem>>(emptyList())
            override fun addLabCodexItem(item: LabCodexItem): Boolean {
                labList.removeAll { it.testName.equals(item.testName, ignoreCase = true) }
                labList.add(item)
                labCodexItems.value = labList.toList()
                return true
            }
            override fun deleteLabCodexItem(itemId: String): Boolean {
                val removed = labList.removeIf { it.id == itemId }
                labCodexItems.value = labList.toList()
                return removed
            }
            override fun getLabCodexItem(itemId: String): LabCodexItem? = labList.find { it.id == itemId }
            override fun findLabCodexItemByName(name: String): LabCodexItem? = labList.find { it.testName.equals(name, ignoreCase = true) }
            override val labCategoryGroups = kotlinx.coroutines.flow.MutableStateFlow<List<LabCategoryGroup>>(emptyList())
            override fun addLabCategoryGroup(group: LabCategoryGroup): Boolean = true
            override fun deleteLabCategoryGroup(groupId: String): Boolean = true
            override fun getLabCategoryGroup(groupId: String): LabCategoryGroup? = null
            override fun resetLabCategoryGroupDefaults() {}
            override fun resetLabDefaults() {
                labList.clear()
                labCodexItems.value = emptyList()
            }
        }

        val testItem = LabCodexItem(testName = "Blood Glucose", category = "Biochemistry")
        labDelegate.addLabCodexItem(testItem)

        assertEquals(1, labDelegate.labCodexItems.value.size)
        assertNotNull(labDelegate.findLabCodexItemByName("blood glucose"))

        labDelegate.deleteLabCodexItem(testItem.id)
        assertTrue(labDelegate.labCodexItems.value.isEmpty())
    }

    @Test
    fun testLabCategoryGroupModelAndOperations() {
        val group = LabCategoryGroup(
            id = "grp-1",
            name = "First Trimester Booking",
            description = "Initial routine pregnancy profile",
            testNames = listOf("Complete Blood Count (CBC)", "Blood Grouping & Rh Factor"),
            isCustom = true,
        )
        assertEquals("First Trimester Booking", group.name)
        assertEquals(2, group.testNames.size)
        assertTrue(group.isCustom)

        val groupsList = mutableListOf<LabCategoryGroup>()
        val mockDelegate = object : LabCodexDelegate {
            override val labCodexItems = kotlinx.coroutines.flow.MutableStateFlow<List<LabCodexItem>>(emptyList())
            override val labCategoryGroups = kotlinx.coroutines.flow.MutableStateFlow<List<LabCategoryGroup>>(emptyList())

            override fun addLabCodexItem(item: LabCodexItem): Boolean = true
            override fun deleteLabCodexItem(itemId: String): Boolean = true
            override fun getLabCodexItem(itemId: String): LabCodexItem? = null
            override fun findLabCodexItemByName(name: String): LabCodexItem? = null
            override fun resetLabDefaults() {}

            override fun addLabCategoryGroup(group: LabCategoryGroup): Boolean {
                groupsList.removeAll { it.name.equals(group.name, ignoreCase = true) }
                groupsList.add(group)
                labCategoryGroups.value = groupsList.toList()
                return true
            }

            override fun deleteLabCategoryGroup(groupId: String): Boolean {
                val removed = groupsList.removeIf { it.id == groupId }
                labCategoryGroups.value = groupsList.toList()
                return removed
            }

            override fun getLabCategoryGroup(groupId: String): LabCategoryGroup? = groupsList.find { it.id == groupId }
            override fun resetLabCategoryGroupDefaults() {
                groupsList.clear()
                labCategoryGroups.value = emptyList()
            }
        }

        mockDelegate.addLabCategoryGroup(group)
        assertEquals(1, mockDelegate.labCategoryGroups.value.size)
        assertEquals("First Trimester Booking", mockDelegate.getLabCategoryGroup("grp-1")?.name)

        val deleted = mockDelegate.deleteLabCategoryGroup("grp-1")
        assertTrue(deleted)
        assertTrue(mockDelegate.labCategoryGroups.value.isEmpty())
    }
}
