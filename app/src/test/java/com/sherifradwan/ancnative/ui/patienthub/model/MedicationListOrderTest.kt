package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [stableMedicationOrder].
 *
 * Assumption: [DemoMedicationRecord] can be constructed with at least (id, patientId, name, dosage, route,
 * frequency, startDate, endDate, status, notes, isProphylaxis, revision) as used elsewhere in the app tests;
 * only [DemoMedicationRecord.id] is significant to the ordering contract, so other fields are filled with
 * benign placeholders.
 */
class MedicationListOrderTest {

    private fun med(id: String, name: String = id) = DemoMedicationRecord(
        id = id,
        patientId = "p1",
        name = name,
        dosage = "1 mg",
        route = "Oral",
        frequency = "Once daily",
        startDate = "2026-01-01",
        endDate = null,
        status = "active",
        notes = null,
        isProphylaxis = false,
        revision = 1,
    )

    @Test
    fun keptRowsPreserveTheirPreviousOrder() {
        val a = med("a")
        val b = med("b")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("c", "a", "b"), meds = listOf(a, b, c))
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
    }

    @Test
    fun newRowsAreAppendedAtTheEnd() {
        val a = med("a")
        val b = med("b")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("a", "b"), meds = listOf(a, b, c))
        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun removedRowsAreDropped() {
        val a = med("a")
        val c = med("c")
        val result = stableMedicationOrder(shownIds = listOf("a", "b", "c"), meds = listOf(a, c))
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun emptyShownReturnsMedsInTheirCurrentOrder() {
        val a = med("a")
        val b = med("b")
        val result = stableMedicationOrder(shownIds = emptyList(), meds = listOf(a, b))
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun keptOrderNewAppendedAndRemovedDroppedTogether() {
        val a = med("a")
        val c = med("c")
        val d = med("d")
        // shown had a,b,c in that order; b removed, d is new -> kept a,c keep order, d appended.
        val result = stableMedicationOrder(shownIds = listOf("a", "b", "c"), meds = listOf(a, c, d))
        assertEquals(listOf("a", "c", "d"), result.map { it.id })
        assertTrue(result.none { it.id == "b" })
    }
}
