package com.sherifradwan.ancnative.ui.patienthub.model

data class MedicationTile(
    val id: String,
    val name: String,
    val dosage: String,
    val route: String,
    val isCustom: Boolean = false,
)

fun filterMedicationTiles(
    allTiles: List<MedicationTile>,
    selectedTab: String,
    searchQuery: String,
    pastMedicationNames: Set<String>,
): List<MedicationTile> {
    val q = searchQuery.trim().lowercase()
    val baseList = if (selectedTab == "recent") {
        allTiles.filter { it.name.trim().lowercase() in pastMedicationNames }
    } else {
        allTiles
    }
    return if (q.isEmpty()) {
        baseList
    } else {
        baseList.filter { it.name.lowercase().contains(q) }
    }
}
