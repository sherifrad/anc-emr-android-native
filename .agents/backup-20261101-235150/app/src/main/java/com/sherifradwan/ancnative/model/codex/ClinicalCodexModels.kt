package com.sherifradwan.ancnative.model.codex

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class LabCodexItem(
    val id: String = UUID.randomUUID().toString(),
    val testName: String,
    val category: String, // Hematology, Biochemistry, Serology, Urinalysis, Other
    val unit: String = "",
    val referenceRange: String = "",
    val defaultResult: String = "",
    val isCustom: Boolean = false,
) {
    val displayLabel: String
        get() = testName
}

data class MedicationCodexItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String,
    val route: String = "Oral", // Oral, IM, IV, Topical, Subcutaneous
    val frequency: String = "Once daily",
    val isProphylaxis: Boolean = true,
    val instructions: String = "",
    val isCustom: Boolean = false,
) {
    val displayLabel: String
        get() = if (dosage.isNotBlank()) "$name $dosage" else name
}

data class LabCategoryGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val testNames: List<String> = emptyList(),
    val isCustom: Boolean = false,
)

data class LabItemEdit(
    val items: List<LabCodexItem>,
    val groups: List<LabCategoryGroup>,
)

data class LabPanelResolution(
    val tests: List<LabCodexItem>,
    val missing: List<String>,
)

fun resolveLabPanel(
    panel: LabCategoryGroup,
    items: List<LabCodexItem>,
): LabPanelResolution {
    val tests = mutableListOf<LabCodexItem>()
    val missing = mutableListOf<String>()
    val seenItemIds = mutableSetOf<String>()

    for (rawName in panel.testNames) {
        val trimmedName = rawName.trim()
        val matchedItem = items.firstOrNull { it.testName.trim().equals(trimmedName, ignoreCase = true) }
        if (matchedItem != null) {
            if (seenItemIds.add(matchedItem.id)) {
                tests.add(matchedItem)
            }
        } else {
            missing.add(rawName)
        }
    }

    return LabPanelResolution(tests = tests, missing = missing)
}

data class LabPanelStep(
    val item: LabCodexItem?,
    val remainingIds: List<String>,
)

fun nextLabPanelTest(
    remainingIds: List<String>,
    items: List<LabCodexItem>,
): LabPanelStep {
    for (i in remainingIds.indices) {
        val id = remainingIds[i]
        val match = items.firstOrNull { it.id == id }
        if (match != null) {
            return LabPanelStep(item = match, remainingIds = remainingIds.drop(i + 1))
        }
    }
    return LabPanelStep(item = null, remainingIds = emptyList())
}

fun applyLabItemEdit(
    items: List<LabCodexItem>,
    groups: List<LabCategoryGroup>,
    edited: LabCodexItem,
): LabItemEdit? {
    val trimmedName = edited.testName.trim()
    if (trimmedName.isBlank()) return null

    val existingIndex = items.indexOfFirst { it.id == edited.id }
    if (existingIndex < 0) return null
    val oldItem = items[existingIndex]

    if (items.any { it.id != edited.id && it.testName.trim().equals(trimmedName, ignoreCase = true) }) {
        return null
    }

    val updatedItem = edited.copy(
        testName = trimmedName,
        category = edited.category.trim(),
        unit = edited.unit.trim(),
        referenceRange = edited.referenceRange.trim(),
        defaultResult = edited.defaultResult.trim(),
        isCustom = true,
    )

    val newItems = items.toMutableList()
    newItems[existingIndex] = updatedItem

    val nameChanged = oldItem.testName.trim() != trimmedName
    val newGroups = if (nameChanged) {
        groups.map { group ->
            group.copy(
                testNames = group.testNames.map { testName ->
                    if (testName.trim().equals(oldItem.testName.trim(), ignoreCase = true)) {
                        trimmedName
                    } else {
                        testName
                    }
                }
            )
        }
    } else {
        groups
    }

    return LabItemEdit(items = newItems, groups = newGroups)
}

interface LabCodexDelegate {
    val labCodexItems: StateFlow<List<LabCodexItem>>
    val labCategoryGroups: StateFlow<List<LabCategoryGroup>>

    fun addLabCodexItem(item: LabCodexItem): Boolean
    fun updateLabCodexItem(item: LabCodexItem): Boolean
    fun deleteLabCodexItem(itemId: String): Boolean
    fun getLabCodexItem(itemId: String): LabCodexItem?
    fun findLabCodexItemByName(name: String): LabCodexItem?
    fun resetLabDefaults()

    // Lab Category Groups (Panels)
    fun addLabCategoryGroup(group: LabCategoryGroup): Boolean
    fun deleteLabCategoryGroup(groupId: String): Boolean
    fun getLabCategoryGroup(groupId: String): LabCategoryGroup?
    fun resetLabCategoryGroupDefaults()
}

interface MedicationCodexDelegate {
    val medicationCodexItems: StateFlow<List<MedicationCodexItem>>
    fun addMedicationCodexItem(item: MedicationCodexItem): Boolean
    fun deleteMedicationCodexItem(itemId: String): Boolean
    fun getMedicationCodexItem(itemId: String): MedicationCodexItem?
    fun findMedicationCodexItemByName(name: String): MedicationCodexItem?
    fun resetMedicationDefaults()
}
