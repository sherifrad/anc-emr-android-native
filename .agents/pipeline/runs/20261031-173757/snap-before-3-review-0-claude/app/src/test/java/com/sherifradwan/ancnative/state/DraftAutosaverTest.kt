package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class DraftAutosaverTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val debounce = 30L

    private class FakeStore : DraftStore {
        val saved: MutableList<DraftRecord> = Collections.synchronizedList(mutableListOf())
        val deleted: MutableList<DraftSlot> = Collections.synchronizedList(mutableListOf())
        @Volatile var failNext = 0
        @Volatile var throwNext = 0

        override suspend fun save(record: DraftRecord): Boolean {
            if (throwNext > 0) {
                throwNext--
                throw IllegalStateException("store down")
            }
            if (failNext > 0) {
                failNext--
                return false
            }
            saved += record
            return true
        }

        override suspend fun delete(slot: DraftSlot) {
            deleted += slot
        }
    }

    private val store = FakeStore()
    private val saver = DraftAutosaver(scope, store, debounce) { 42L }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private val unlocked = VaultState.Unlocked(1L)
    private fun base() = AncState(patients = emptyList(), vaultState = unlocked)

    private fun visit(notes: String, patientId: String = "p1", original: DraftValues = DraftValues()) = base().copy(
        draft = VisitDraft("s1", patientId, "v1", 3, original, original.copy(notes = notes)),
    )

    private fun lab(result: String) = base().copy(
        labDraft = LabDraft("s2", "p1", "l1", 9L, 3L, LabDraftValues(), LabDraftValues(result = result)),
    )

    private fun type(from: AncState, to: AncState) = saver.onStateChanged(from, to)

    @Test
    fun fastTypingSavesOnceWithLatestValues() = runBlocking {
        var prev = visit("")
        for (i in 1..5) {
            val next = visit("note $i")
            type(prev, next)
            prev = next
        }
        delay(debounce * 6)
        assertEquals(1, store.saved.size)
        assertEquals("note 5", store.saved.single().fields["notes"])
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun flushSavesNowAndSecondFlushDoesNothing() = runBlocking {
        type(visit(""), visit("typed"))
        saver.flush()
        assertEquals(1, store.saved.size)
        assertEquals("typed", store.saved.single().fields["notes"])
        saver.flush()
        delay(debounce * 4)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun flushWithNothingPendingIsNoOp() = runBlocking {
        saver.flush()
        assertTrue(store.saved.isEmpty())
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun closingEditorDeletesAndCancelsPendingSave() = runBlocking {
        val dirty = visit("typed")
        type(visit(""), dirty)
        type(dirty, base())
        delay(debounce * 4)
        saver.flush()
        assertTrue(store.saved.isEmpty())
        assertEquals(listOf(DraftSlot("p1", DraftScreenKind.VISIT)), store.deleted.toList())
    }

    @Test
    fun cleanEditorWithNoChangesStoresNothing() = runBlocking {
        type(base(), visit(""))
        type(visit(""), visit(""))
        delay(debounce * 4)
        saver.flush()
        assertTrue(store.saved.isEmpty())
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun lockIsNotADiscard() = runBlocking {
        val dirty = visit("typed")
        type(dirty, base().copy(vaultState = VaultState.Locked))
        saver.cancelPending()
        delay(debounce * 4)
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun cancelPendingForgetsSavesWithoutDeleting() = runBlocking {
        type(visit(""), visit("typed"))
        saver.cancelPending()
        delay(debounce * 4)
        saver.flush()
        assertTrue(store.saved.isEmpty())
        assertTrue(store.deleted.isEmpty())
    }

    @Test
    fun failingStoreNeverEscapesAndLaterSavesWork() = runBlocking {
        store.throwNext = 1
        type(visit(""), visit("one"))
        saver.flush()
        assertTrue(store.saved.isEmpty())

        store.failNext = 1
        type(visit("one"), visit("two"))
        saver.flush()
        assertTrue(store.saved.isEmpty())

        type(visit("two"), visit("three"))
        saver.flush()
        assertEquals("three", store.saved.single().fields["notes"])
    }

    @Test
    fun eachSlotKeepsItsOwnPendingSave() = runBlocking {
        type(visit(""), visit("visit text"))
        type(base().copy(labDraft = null), lab("11"))
        saver.flush()
        val kinds = store.saved.map { it.screenKind }.toSet()
        assertEquals(setOf(DraftScreenKind.VISIT, DraftScreenKind.LAB), kinds)
        assertEquals(2, store.saved.size)
        assertEquals("visit text", store.saved.first { it.screenKind == DraftScreenKind.VISIT }.fields["notes"])
        assertEquals("11", store.saved.first { it.screenKind == DraftScreenKind.LAB }.fields["result"])
    }

    @Test
    fun unchangedValuesAfterSaveAreNotSavedAgain() = runBlocking {
        type(visit(""), visit("same"))
        saver.flush()
        type(visit("same"), visit("same"))
        saver.flush()
        delay(debounce * 4)
        assertEquals(1, store.saved.size)
    }
}
