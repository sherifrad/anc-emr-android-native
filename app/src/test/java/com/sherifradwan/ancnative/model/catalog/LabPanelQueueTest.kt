package com.sherifradwan.ancnative.model.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabPanelQueueTest {
    private val hb = LabCatalogItem(id = "a", testName = "Hemoglobin", category = "Hematology")
    private val glucose = LabCatalogItem(id = "b", testName = "Glucose", category = "Biochemistry")
    private val urine = LabCatalogItem(id = "c", testName = "Urine protein", category = "Urinalysis")
    private val items = listOf(hb, glucose, urine)

    @Test
    fun advanceKeepsQueueOrder() {
        val first = nextLabPanelTest(listOf("c", "a", "b"), items)
        assertEquals(urine, first.item)
        assertEquals(listOf("a", "b"), first.remainingIds)
        val second = nextLabPanelTest(first.remainingIds, items)
        assertEquals(hb, second.item)
        assertEquals(listOf("b"), second.remainingIds)
    }

    @Test
    fun deletedIdIsSkipped() {
        val step = nextLabPanelTest(listOf("gone", "b", "c"), items)
        assertEquals(glucose, step.item)
        assertEquals(listOf("c"), step.remainingIds)
    }

    @Test
    fun allIdsGoneGivesNoItemAndEmptyRest() {
        val step = nextLabPanelTest(listOf("x", "y"), items)
        assertNull(step.item)
        assertTrue(step.remainingIds.isEmpty())
    }

    @Test
    fun lastIdLeavesEmptyRest() {
        val step = nextLabPanelTest(listOf("a"), items)
        assertEquals(hb, step.item)
        assertTrue(step.remainingIds.isEmpty())
    }

    @Test
    fun emptyInputGivesNoItem() {
        val step = nextLabPanelTest(emptyList(), items)
        assertNull(step.item)
        assertTrue(step.remainingIds.isEmpty())
    }

    @Test
    fun renamedTestIsStillFoundById() {
        val renamed = glucose.copy(testName = "Random glucose")
        val step = nextLabPanelTest(listOf("b"), listOf(hb, renamed, urine))
        assertNotNull(step.item)
        assertEquals("Random glucose", step.item?.testName)
    }
}
