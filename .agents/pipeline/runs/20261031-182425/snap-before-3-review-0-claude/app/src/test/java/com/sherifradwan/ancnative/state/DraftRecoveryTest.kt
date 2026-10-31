package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftRecoveryTest {
    private val unlocked = VaultState.Unlocked(1L)
    private fun base() = AncState(patients = emptyList(), vaultState = unlocked)

    private fun visit(session: String = "s1", id: String? = "v1", revision: Int? = 3, values: DraftValues = DraftValues()) =
        base().copy(draft = VisitDraft(session, "p1", id, revision, DraftValues(), values))

    private fun patient(session: String = "s1", revision: Long? = 4L) =
        base().copy(patientDraft = PatientDraft(session, "p1", revision, null, PatientDraftValues(), PatientDraftValues()))

    private fun histories(session: String = "s1") =
        base().copy(historiesDraft = HistoriesDraft(session, "p1", 5L, HistoriesDraftValues(), HistoriesDraftValues()))

    private fun pregnancy(session: String = "s1", recordRevision: Long? = 7L) = base().copy(
        pregnancyDraft = PreviousPregnancyDraft(
            session, "p1", "pr1", 5L, recordRevision, null, PreviousPregnancyDraftValues(), PreviousPregnancyDraftValues(),
        ),
    )

    private fun status(session: String = "s1") =
        base().copy(statusDraft = PatientStatusDraft(session, "p1", 6L, "archived", ""))

    private fun dating(session: String = "s1") =
        base().copy(datingDraft = DatingDraft(session, "p1", 8L, DatingDraftValues(), DatingDraftValues()))

    private fun ultrasound(session: String = "s1") = base().copy(
        ultrasoundDraft = UltrasoundDraft(session, "p1", "u1", 9L, 2L, UltrasoundDraftValues(), UltrasoundDraftValues()),
    )

    private fun lab(session: String = "s1", values: LabDraftValues = LabDraftValues()) =
        base().copy(labDraft = LabDraft(session, "p1", "l1", 9L, 3L, LabDraftValues(), values))

    private fun medication(session: String = "s1") = base().copy(
        medicationDraft = MedicationDraft(session, "p1", "m1", 9L, 4L, MedicationDraftValues(), MedicationDraftValues()),
    )

    private fun recordFor(state: AncState, fields: Map<String, String>): DraftRecord {
        val slot = activeDraftSlot(state)!!
        return DraftRecord(slot.patientId, slot.kind, DraftEditorKind.EDIT, null, null, fields, 1000L)
    }

    private fun prompt(state: AncState, fields: Map<String, String> = mapOf("notes" to "stored"), stale: Boolean = false) =
        DraftRecoveryPrompt(activeDraftSlot(state)!!, activeEditorSessionId(state)!!, recordFor(state, fields), stale)

    private fun withPrompt(state: AncState, fields: Map<String, String> = mapOf("notes" to "stored")): AncState =
        AncReducer.reduce(state, AncEvent.DraftRecoveryFound(prompt(state, fields)))

    @Test
    fun sessionIdAndBaseRevisionForEachOfTheNineEditors() {
        val cases = listOf(
            visit() to 3L,
            patient() to 4L,
            histories() to 5L,
            pregnancy() to 7L,
            status() to 6L,
            dating() to 8L,
            ultrasound() to 2L,
            lab() to 3L,
            medication() to 4L,
        )
        cases.forEach { (state, revision) ->
            assertEquals("s1", activeEditorSessionId(state))
            assertEquals(revision, activeEditorBaseRevision(state))
        }
    }

    @Test
    fun newRecordsHaveNoBaseRevision() {
        assertNull(activeEditorBaseRevision(visit(id = null, revision = null)))
        assertNull(activeEditorBaseRevision(patient(revision = null)))
        assertNull(activeEditorBaseRevision(pregnancy(recordRevision = null)))
    }

    @Test
    fun noEditorGivesNoSessionAndNoRevision() {
        assertNull(activeEditorSessionId(base()))
        assertNull(activeEditorBaseRevision(base()))
    }

    @Test
    fun baseRevisionMatchesWhatTheDraftRecordStores() {
        val typed = listOf(
            visit(values = DraftValues(notes = "x")),
            lab(values = LabDraftValues(result = "1")),
        )
        typed.forEach { state ->
            assertEquals(draftRecordFor(state, 1L)!!.baseRevision, activeEditorBaseRevision(state))
        }
    }

    private fun stored(baseRevision: Long?) =
        DraftRecord("p1", DraftScreenKind.VISIT, DraftEditorKind.EDIT, "v1", baseRevision, emptyMap(), 1L)

    @Test
    fun staleTruthTable() {
        assertFalse(isDraftStale(stored(null), null))
        assertFalse(isDraftStale(stored(3L), null))
        assertFalse(isDraftStale(stored(null), 3L))
        assertFalse(isDraftStale(stored(3L), 3L))
        assertTrue(isDraftStale(stored(3L), 4L))
        assertFalse(isDraftStale(stored(4L), 3L))
    }

    @Test
    fun ageBoundaries() {
        val saved = 1_000_000_000L
        fun age(deltaMillis: Long) = draftAge(saved + deltaMillis, saved)
        val second = 1000L
        val minute = 60 * second
        val hour = 60 * minute
        val day = 24 * hour
        assertEquals(DraftAge.LessThanMinute, age(0))
        assertEquals(DraftAge.LessThanMinute, age(59 * second))
        assertEquals(DraftAge.Minutes(1), age(60 * second))
        assertEquals(DraftAge.Minutes(1), age(119 * second))
        assertEquals(DraftAge.Minutes(59), age(59 * minute))
        assertEquals(DraftAge.Hours(1), age(60 * minute))
        assertEquals(DraftAge.Hours(23), age(23 * hour))
        assertEquals(DraftAge.Hours(23), age(24 * hour - 1))
        assertEquals(DraftAge.Days(1), age(24 * hour))
        assertEquals(DraftAge.Days(40), age(40 * day))
        assertEquals(DraftAge.LessThanMinute, draftAge(saved - 5 * day, saved))
    }

    @Test
    fun foundSetsPromptOnlyForTheOpenEditorSession() {
        val state = visit()
        assertEquals(prompt(state), withPrompt(state).draftRecoveryPrompt)

        val wrongSession = prompt(state).copy(editorSessionId = "other")
        assertNull(AncReducer.reduce(state, AncEvent.DraftRecoveryFound(wrongSession)).draftRecoveryPrompt)
        assertNull(AncReducer.reduce(base(), AncEvent.DraftRecoveryFound(prompt(state))).draftRecoveryPrompt)
    }

    @Test
    fun foundWhileAPromptIsSetIsIgnored() {
        val first = withPrompt(visit(), mapOf("notes" to "first"))
        val second = prompt(visit(), mapOf("notes" to "second"))
        assertEquals(first, AncReducer.reduce(first, AncEvent.DraftRecoveryFound(second)))
    }

    @Test
    fun acceptAppliesStoredTextWithNewSessionAndClearsPrompt() {
        val accepted = AncReducer.reduce(withPrompt(visit()), AncEvent.AcceptDraftRecovery)
        assertNull(accepted.draftRecoveryPrompt)
        assertEquals("stored", accepted.draft!!.values.notes)
        assertTrue(accepted.hasDirtyDraft)
        assertNotNull(activeEditorSessionId(accepted))
        assertNotEquals("s1", activeEditorSessionId(accepted))
    }

    @Test
    fun acceptWithoutPromptChangesNothing() {
        val opened = visit()
        assertEquals(opened, AncReducer.reduce(opened, AncEvent.AcceptDraftRecovery))
    }

    @Test
    fun discardClearsPromptAndLeavesEditorClean() {
        val opened = visit()
        val discarded = AncReducer.reduce(withPrompt(opened), AncEvent.DiscardDraftRecovery)
        assertEquals(opened, discarded)
        assertFalse(discarded.hasDirtyDraft)
        assertEquals("s1", activeEditorSessionId(discarded))
    }

    @Test
    fun closingOrReplacingTheEditorClearsThePrompt() {
        val cancelled = AncReducer.reduce(withPrompt(visit()), AncEvent.CancelEditor)
        assertNull(cancelled.draftRecoveryPrompt)
        assertNull(activeEditorSessionId(cancelled))

        val back = AncReducer.reduce(withPrompt(lab()), AncEvent.Back)
        assertNull(back.draftRecoveryPrompt)

        val replaced = AncReducer.reduce(withPrompt(visit()), AncEvent.StartVisit("p1", null, "s2"))
        assertNull(replaced.draftRecoveryPrompt)
    }

    @Test
    fun valueChangesAreIgnoredWhileAPromptIsSet() {
        val visitState = withPrompt(visit())
        assertEquals(visitState, AncReducer.reduce(visitState, AncEvent.DraftChanged(DraftField.NOTES, "typed")))

        val labState = withPrompt(lab(), mapOf("result" to "stored"))
        assertEquals(labState, AncReducer.reduce(labState, AncEvent.LabDraftChanged(LabDraftField.RESULT, "typed")))
        assertEquals(labState, AncReducer.reduce(labState, AncEvent.LabAbnormalChanged(true)))

        val statusState = withPrompt(status(), mapOf("reason" to "stored"))
        assertEquals(statusState, AncReducer.reduce(statusState, AncEvent.StatusReasonChanged("typed")))
    }

    @Test
    fun valueChangesWorkAgainAfterTheChoice() {
        val decided = AncReducer.reduce(withPrompt(visit()), AncEvent.DiscardDraftRecovery)
        val typed = AncReducer.reduce(decided, AncEvent.DraftChanged(DraftField.NOTES, "typed"))
        assertEquals("typed", typed.draft!!.values.notes)
    }
}
