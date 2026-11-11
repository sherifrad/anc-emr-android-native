package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.catalog.CustomChoiceItem
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.state.DEFAULT_MEDICATION_ROUTE

/** One picker tile. Its dose, route and frequency fill a new draft; a custom-choice tile has a blank dose and frequency. */
data class MedicationTile(
    val id: String,
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    val isCustom: Boolean = false,
)

/** The picker's two sources: every drug on the list, or only drugs this patient has been given before. */
const val PICKER_SOURCE_COMMON = "common"
const val PICKER_SOURCE_RECENT = "recent"

/**
 * The one rule for "the same drug name" in the picker: trimmed and without case, the way a save stores the name
 * (trimmed). Recent and the already-active check both use it.
 */
fun medicationNameKey(name: String): String = name.trim().lowercase()

/** The picker's tiles: the medication list, then the doctor's own drug names (blank dose and frequency, default route). */
fun medicationTiles(catalog: List<MedicationCatalogItem>, customNames: List<CustomChoiceItem>): List<MedicationTile> =
    catalog.map { MedicationTile(it.id, it.name, it.dosage, it.route, it.frequency, it.isCustom) } +
        customNames.map { MedicationTile("custom_${it.id}", it.label, "", DEFAULT_MEDICATION_ROUTE, "", isCustom = true) }

/** Name keys of the drugs this patient takes now. A tile whose name is among them must not start a second prescription. */
fun activeMedicationNameKeys(medications: List<DemoMedicationRecord>): Set<String> =
    medications.filter { isOngoingMedication(it.status, it.isNonCompliant) }.mapTo(HashSet()) { medicationNameKey(it.name) }

fun MedicationTile.isAlreadyActive(activeNameKeys: Set<String>): Boolean = medicationNameKey(name) in activeNameKeys

/**
 * The tiles the picker shows for [selectedTab] and [searchQuery]. Recent keeps the tiles whose name (by
 * [medicationNameKey]) is among [pastMedicationNames], the names of every drug the patient has had.
 */
fun filterMedicationTiles(
    allTiles: List<MedicationTile>,
    selectedTab: String,
    searchQuery: String,
    pastMedicationNames: Set<String>,
): List<MedicationTile> {
    val q = searchQuery.trim().lowercase()
    val baseList = if (selectedTab == PICKER_SOURCE_RECENT) {
        val past = pastMedicationNames.mapTo(HashSet(), ::medicationNameKey)
        allTiles.filter { medicationNameKey(it.name) in past }
    } else {
        allTiles
    }
    return if (q.isEmpty()) {
        baseList
    } else {
        baseList.filter { it.name.lowercase().contains(q) }
    }
}
