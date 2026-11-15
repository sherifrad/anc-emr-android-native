package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.ui.patienthub.components.pickerVisibleTiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [pickerVisibleTiles].
 *
 * Assumption: [MedicationTile] can be constructed with (id, name, dosage, route, frequency) as used by the
 * picker; a minimal local factory supplies benign defaults for non-name fields.
 */
class PickerVisibleTilesTest {

    private fun tile(id: String, name: String) = MedicationTile(
        id = id,
        name = name,
        dosage = "1 mg",
        route = "Oral",
        frequency = "Once daily",
    )

    @Test
    fun emptyQueryTakesLimitWhenNotShowingAll() {
        val filtered = (1..10).map { tile("t$it", "Drug $it") }
        val result = pickerVisibleTiles(filtered = filtered, query = "", showAll = false, limit = 8)
        assertEquals(filtered.take(8).map { it.id }, result.map { it.id })
    }

    @Test
    fun queryRanksNamesStartingWithQueryFirstThenTakesLimit() {
        val filtered = listOf(
            tile("1", "Zinc sulfate"),
            tile("2", "Aspirin"),
            tile("3", "Azithromycin"),
            tile("4", "Paracetamol"),
            tile("5", "Amoxicillin"),
            tile("6", "Magnesium"),
        )
        val result = pickerVisibleTiles(filtered = filtered, query = "a", showAll = false, limit = 3)
        // Prefix matches (aspirin, azithromycin, amoxicillin in filtered order) come first, limited to 3.
        assertEquals(listOf("2", "3", "5"), result.map { it.id })
        result.forEach { assertTrue(it.name.lowercase().startsWith("a")) }
    }

    @Test
    fun showAllReturnsAllRankedResults() {
        val filtered = listOf(
            tile("1", "Zinc"),
            tile("2", "Aspirin"),
            tile("3", "Azithromycin"),
            tile("4", "Magnesium"),
        )
        val result = pickerVisibleTiles(filtered = filtered, query = "a", showAll = true, limit = 1)
        // All returned, prefix-first ranking preserved (aspirin, azithromycin, then non-prefix in filtered order).
        assertEquals(listOf("2", "3", "1", "4"), result.map { it.id })
        assertEquals(filtered.size, result.size)
    }

    @Test
    fun emptyQueryShowAllReturnsAll() {
        val filtered = (1..5).map { tile("t$it", "Drug $it") }
        val result = pickerVisibleTiles(filtered = filtered, query = "", showAll = true, limit = 2)
        assertEquals(filtered.map { it.id }, result.map { it.id })
    }
}
