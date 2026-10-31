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

interface LabCodexDelegate {
    val labCodexItems: StateFlow<List<LabCodexItem>>
    val labCategoryGroups: StateFlow<List<LabCategoryGroup>>

    fun addLabCodexItem(item: LabCodexItem): Boolean
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
