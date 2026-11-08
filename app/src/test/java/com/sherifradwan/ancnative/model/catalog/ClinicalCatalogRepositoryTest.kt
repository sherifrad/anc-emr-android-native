package com.sherifradwan.ancnative.model.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalCatalogModelsTest {

    @Test
    fun testLabCatalogItemModelAndDisplayLabel() {
        val labItem = LabCatalogItem(
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
    fun testMedicationCatalogItemModelAndDisplayLabel() {
        val medWithDosage = MedicationCatalogItem(
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

        val medWithoutDosage = MedicationCatalogItem(
            id = "test-med-2",
            name = "Paracetamol",
            dosage = "",
            isCustom = true,
        )
        assertEquals("Paracetamol", medWithoutDosage.displayLabel)
        assertTrue(medWithoutDosage.isCustom)
    }

    @Test
    fun testMockCatalogDelegateOperations() {
        // Test in-memory implementation of delegates
        val labList = mutableListOf<LabCatalogItem>()
        val labDelegate = object : LabCatalogDelegate {
            override val labCatalogItems = kotlinx.coroutines.flow.MutableStateFlow<List<LabCatalogItem>>(emptyList())
            override fun addLabCatalogItem(item: LabCatalogItem): Boolean {
                labList.removeAll { it.testName.equals(item.testName, ignoreCase = true) }
                labList.add(item)
                labCatalogItems.value = labList.toList()
                return true
            }
            override fun updateLabCatalogItem(item: LabCatalogItem): Boolean = false
            override fun deleteLabCatalogItem(itemId: String): Boolean {
                val removed = labList.removeIf { it.id == itemId }
                labCatalogItems.value = labList.toList()
                return removed
            }
            override fun getLabCatalogItem(itemId: String): LabCatalogItem? = labList.find { it.id == itemId }
            override fun findLabCatalogItemByName(name: String): LabCatalogItem? = labList.find { it.testName.equals(name, ignoreCase = true) }
            override val labCategoryGroups = kotlinx.coroutines.flow.MutableStateFlow<List<LabCategoryGroup>>(emptyList())
            override fun addLabCategoryGroup(group: LabCategoryGroup): Boolean = true
            override fun deleteLabCategoryGroup(groupId: String): Boolean = true
            override fun getLabCategoryGroup(groupId: String): LabCategoryGroup? = null
            override fun resetLabCategoryGroupDefaults() {}
            override fun resetLabDefaults() {
                labList.clear()
                labCatalogItems.value = emptyList()
            }
        }

        val testItem = LabCatalogItem(testName = "Blood Glucose", category = "Biochemistry")
        labDelegate.addLabCatalogItem(testItem)

        assertEquals(1, labDelegate.labCatalogItems.value.size)
        assertNotNull(labDelegate.findLabCatalogItemByName("blood glucose"))

        labDelegate.deleteLabCatalogItem(testItem.id)
        assertTrue(labDelegate.labCatalogItems.value.isEmpty())
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
        val mockDelegate = object : LabCatalogDelegate {
            override val labCatalogItems = kotlinx.coroutines.flow.MutableStateFlow<List<LabCatalogItem>>(emptyList())
            override val labCategoryGroups = kotlinx.coroutines.flow.MutableStateFlow<List<LabCategoryGroup>>(emptyList())

            override fun addLabCatalogItem(item: LabCatalogItem): Boolean = true
            override fun updateLabCatalogItem(item: LabCatalogItem): Boolean = false
            override fun deleteLabCatalogItem(itemId: String): Boolean = true
            override fun getLabCatalogItem(itemId: String): LabCatalogItem? = null
            override fun findLabCatalogItemByName(name: String): LabCatalogItem? = null
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
