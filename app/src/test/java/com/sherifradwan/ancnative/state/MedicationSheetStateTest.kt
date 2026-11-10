package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_NON_COMPLIANT
import com.sherifradwan.ancnative.storage.ClinicalCommandPreparation
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.SaveMedicationCommand
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The medication sheet and the one-tap stop in the reducer (design: Technical Plan Sections 1 and 4). */
class MedicationSheetStateTest {
    // JUnit makes a new instance for every test, so this is read once per test.
    private val today = LocalDate.now().toString()
    private val medsTab = AncRoute.Workspace("p1", WorkspaceSection.MEDICATIONS)

    private fun med(
        id: String = "m1",
        status: String = MEDICATION_STATUS_ACTIVE,
        endDate: String? = null,
        revision: Long = 3L,
        nonCompliant: Boolean = false,
    ) = DemoMedicationRecord(
        id = id, patientId = "p1", name = "Iron $id", dosage = "100 mg", route = "Oral", frequency = "Once daily",
        startDate = "2026-01-01", endDate = endDate, status = status, notes = "After food", isProphylaxis = true,
        isNonCompliant = nonCompliant, revision = revision,
    )

    private fun patient(vararg meds: DemoMedicationRecord, status: String = "active", revision: Long = 5L) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01", phone = "01000000000",
        visits = emptyList(), status = status, medications = meds.toList(), revision = revision,
    )

    private val secondPatient = DemoPatient(
        id = "p2", caseId = "P2", displayName = "Patient Two", dateOfBirth = "1990-01-01", phone = "01000000001",
        visits = emptyList(), status = "active",
    )

    private fun base(vararg meds: DemoMedicationRecord = arrayOf(med(), med("m2"))) =
        AncState(patients = listOf(patient(*meds), secondPatient), vaultState = VaultState.Unlocked(1L), route = medsTab)

    private fun AncState.reduce(event: AncEvent) = AncReducer.reduce(this, event)

    private val prefill = MedicationPrefill(name = "Folic acid", dosage = "5 mg", route = "Oral", frequency = "Once daily")

    private fun addFromTile(state: AncState = base(), session: String = "s-add") =
        state.reduce(AncEvent.StartMedicationEditor("p1", null, "new-1", session, prefill = prefill))

    private fun edit(state: AncState = base(), id: String = "m1", session: String = "s-edit") =
        state.reduce(AncEvent.StartMedicationEditor("p1", id, id, session))

    private fun custom(state: AncState = base()) =
        state.reduce(AncEvent.StartMedicationEditor("p1", null, "new-2", "s-custom"))

    private val blank = MedicationDraftValues(route = "Oral", status = MEDICATION_STATUS_ACTIVE, startDate = today)

    // ---- original / values at start ----

    @Test
    fun addFromTileStartsFromBlankDefaultsWithTheTileOnTop() {
        val draft = addFromTile().medicationDraft!!
        assertEquals(blank, draft.original)
        assertEquals(blank.copy(name = "Folic acid", dosage = "5 mg", frequency = "Once daily"), draft.values)
        assertNull(draft.baseMedicationRevision)
        assertEquals(5L, draft.basePatientRevision)
        assertTrue("dirty on open, so it is recovered after a crash", draft.isDirty)
        assertFalse("no field has been changed yet", draft.touched)
    }

    @Test
    fun tileWithoutRouteKeepsTheDefaultRouteAndBlankFrequency() {
        val bare = base().reduce(
            AncEvent.StartMedicationEditor("p1", null, "n", "s", prefill = MedicationPrefill("Custom drug", "", "", "")),
        ).medicationDraft!!
        assertEquals(blank.copy(name = "Custom drug"), bare.values)
    }

    @Test
    fun editStartsWithTheStoredRowOnBothSides() {
        val draft = edit().medicationDraft!!
        assertEquals(draft.original, draft.values)
        assertEquals("Iron m1", draft.values.name)
        assertEquals("2026-01-01", draft.values.startDate)
        assertEquals(3L, draft.baseMedicationRevision)
        assertFalse(draft.isDirty)
        assertFalse(draft.touched)
    }

    @Test
    fun prefillIsIgnoredWhenEditingAStoredRow() {
        val draft = base().reduce(AncEvent.StartMedicationEditor("p1", "m1", "m1", "s", prefill = prefill)).medicationDraft!!
        assertEquals("Iron m1", draft.values.name)
        assertEquals(draft.original, draft.values)
    }

    @Test
    fun customStartsBlankOnBothSides() {
        val draft = custom().medicationDraft!!
        assertEquals(blank, draft.original)
        assertEquals(blank, draft.values)
        assertFalse(draft.isDirty)
    }

    @Test
    fun editOfAMissingRowIsIgnored() {
        val state = base()
        assertSame(state, state.reduce(AncEvent.StartMedicationEditor("p1", "gone", "gone", "s")))
    }

    @Test
    fun startingADraftNeverChangesTheRoute() {
        for (started in listOf(addFromTile(), edit(), custom())) {
            assertEquals(medsTab, started.route)
            assertNotNull(started.medicationDraft)
        }
        // A start from another tab also leaves the route alone.
        val onVisits = base().copy(route = AncRoute.Workspace("p1", WorkspaceSection.VISITS))
        assertEquals(onVisits.route, edit(onVisits).route)
    }

    // ---- touched ----

    @Test
    fun firstFieldChangeSetsTouchedWithoutAffectingDirty() {
        val opened = edit()
        val sameName = opened.reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "Iron m1")).medicationDraft!!
        assertTrue(sameName.touched)
        assertFalse("the value did not change", sameName.isDirty)

        val tile = addFromTile().reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.NOTES, "x")).medicationDraft!!
        assertTrue(tile.touched)
        assertTrue(tile.isDirty)
    }

    // ---- start ignored while busy ----

    @Test
    fun startIsIgnoredWhileASaveRuns() {
        for (phase in listOf(SavePhase.PENDING, SavePhase.RECONCILING, SavePhase.UNRESOLVED)) {
            val saving = base().copy(savePhase = phase)
            assertNull(phase.name, addFromTile(saving).medicationDraft)
            assertNull(phase.name, edit(saving).medicationDraft)
            assertNull(phase.name, custom(saving).medicationDraft)
        }
    }

    @Test
    fun startIsIgnoredWhileAnotherMedicationDraftIsOpenEvenIfClean() {
        val open = edit()
        assertFalse(open.hasDirtyDraft)
        assertSame(open, edit(open, id = "m2", session = "s-2"))
        assertSame(open, addFromTile(open, session = "s-3"))
    }

    // ---- cancel, back and the discard prompt ----

    @Test
    fun cancelClosesTheSheetWithoutASecondPromptAndStaysOnTheTab() {
        val untouched = addFromTile()
        val touched = untouched.reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, "1 mg"))
        for (state in listOf(untouched, touched, edit())) {
            val closed = state.reduce(AncEvent.CancelEditor)
            assertNull(closed.medicationDraft)
            assertFalse("the sheet already asked", closed.showDiscardDialog)
            assertEquals(medsTab, closed.route)
        }
    }

    @Test
    fun backOnAnUntouchedDraftClosesTheSheetAndNotTheWorkspace() {
        val closed = addFromTile().reduce(AncEvent.Back)
        assertNull(closed.medicationDraft)
        assertFalse(closed.showDiscardDialog)
        assertEquals(medsTab, closed.route)
    }

    @Test
    fun backOnATouchedDraftAsksAndDiscardStaysOnTheTab() {
        val touched = addFromTile().reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, "1 mg"))
        val asked = touched.reduce(AncEvent.Back)
        assertTrue(asked.showDiscardDialog)
        assertNotNull(asked.medicationDraft)

        val kept = asked.reduce(AncEvent.KeepEditing)
        assertEquals(touched.medicationDraft, kept.medicationDraft)

        val discarded = asked.reduce(AncEvent.ConfirmDiscard)
        assertNull(discarded.medicationDraft)
        assertEquals(medsTab, discarded.route)
    }

    @Test
    fun backWithoutADraftStillLeavesTheWorkspace() {
        assertEquals(AncRoute.PatientList, base().reduce(AncEvent.Back).route)
    }

    @Test
    fun editorHookReportsTheOpenRowAndClosesOnlyTheDraft() {
        val open = edit()
        assertTrue(MedicationEditorHook.isOpenFor(open, "m1"))
        assertFalse(MedicationEditorHook.isOpenFor(open, "m2"))
        val closed = MedicationEditorHook.closeEditor(open, "p1")
        assertNull(closed.medicationDraft)
        assertEquals(medsTab, closed.route)
        assertEquals(open.copy(medicationDraft = null), closed)
    }

    @Test
    fun confirmDeleteOfTheOpenRowClosesTheSheetOnTheTab() {
        val open = edit()
        val confirmed = open.copy(deleteConfirmation = PendingDeletion("p1", DeletableType.MEDICATION, "m1"))
            .reduce(AncEvent.ConfirmDelete)
        assertNull(confirmed.medicationDraft)
        assertEquals(medsTab, confirmed.route)
    }

    // ---- status toggle ----

    @Test
    fun switchingToStoppedFillsABlankEndDateWithToday() {
        val stopped = edit().reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
        assertEquals(today, stopped.medicationDraft!!.values.endDate)
        assertEquals("2026-01-01", stopped.medicationDraft!!.values.startDate)
    }

    @Test
    fun switchingToStoppedKeepsAnEndDateAlreadyGiven() {
        val state = edit().reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, "2026-02-02"))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
        assertEquals("2026-02-02", state.medicationDraft!!.values.endDate)
    }

    @Test
    fun switchingBackToActiveKeepsTheStartDateAndClearsTheEndDate() {
        val opened = edit()
        val back = opened
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_ACTIVE))
            .medicationDraft!!
        assertEquals("2026-01-01", back.values.startDate)
        assertEquals("", back.values.endDate)
        assertFalse("back where it started", back.isDirty)
    }

    @Test
    fun switchingBetweenActiveAndNonCompliantLeavesTheEndDateAlone() {
        val state = base(med(endDate = "2026-09-09"))
        val next = edit(state).reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_NON_COMPLIANT))
        assertEquals("2026-09-09", next.medicationDraft!!.values.endDate)
    }

    @Test
    fun stoppedDraftPreparesAValidCommandEndingToday() {
        val stopped = edit().reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
        val command = (SaveMedicationCommand.prepare(stopped, "op", "m1") as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        assertEquals(today, command.endDate)
        assertEquals(MEDICATION_STATUS_DISCONTINUED, command.status)
    }

    // ---- one-tap stop ----

    private fun stop(state: AncState = base(), id: String = "m1", session: String = "stop-1") =
        state.reduce(AncEvent.StopMedication("p1", id, session))

    private fun commandOf(state: AncState): SaveMedicationCommand {
        val quick = state.medicationQuickSave!!
        val prepared = SaveMedicationCommand.prepare(state.copy(medicationDraft = quick.draft), "00000000-0000-4000-8000-000000000001", quick.draft.medicationId)
        return (prepared as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
    }

    private fun tokenOf(state: AncState) = commandOf(state).let { PendingSaveToken(it.operationUuid, it.editorSessionId) }

    /** The patients after the store committed [command] as revision [revision]. */
    private fun committed(state: AncState, command: SaveMedicationCommand, revision: Long) = state.patients.map { p ->
        if (p.id != command.patientId) p else p.copy(
            revision = p.revision + 1,
            medications = p.medications.map {
                if (it.id != command.medicationId) it else it.copy(
                    status = command.status, endDate = command.endDate, isNonCompliant = command.isNonCompliant, revision = revision,
                )
            },
        )
    }

    private fun commit(state: AncState, revision: Long): AncState {
        val command = commandOf(state)
        val token = tokenOf(state)
        val pending = state.reduce(AncEvent.SaveStarted(token))
        assertEquals(SavePhase.PENDING, pending.savePhase)
        return pending.reduce(
            AncEvent.RecordCommitted(token, committed(state, command, revision), medsTab, SavedNotice.MEDICATIONS, committedAtMillis = 1_000L),
        )
    }

    @Test
    fun stopOpensNoDraftAndBuildsTheStoredRowAsStoppedTodayAtItsRevision() {
        val state = stop()
        assertNull(state.medicationDraft)
        assertEquals(medsTab, state.route)
        assertNull("not an editor session, so nothing is autosaved", activeEditorSessionId(state))
        val command = commandOf(state)
        assertEquals("m1", command.medicationId)
        assertEquals(3L, command.expectedMedicationRevision)
        assertEquals(5L, command.expectedPatientRevision)
        assertEquals(MEDICATION_STATUS_DISCONTINUED, command.status)
        assertEquals(today, command.endDate)
        assertFalse(command.isNonCompliant)
        // Everything else is the stored row.
        assertEquals("Iron m1", command.name)
        assertEquals("100 mg", command.dosage)
        assertEquals("Once daily", command.frequency)
        assertEquals("2026-01-01", command.startDate)
        assertEquals("After food", command.notes)
        assertTrue(command.isProphylaxis)
    }

    @Test
    fun whileTheStopSavesEveryOtherStartIsIgnored() {
        val pending = stop().reduce(AncEvent.SaveStarted(tokenOf(stop())))
        assertEquals(SavePhase.PENDING, pending.savePhase)
        assertSame(pending, addFromTile(pending))
        assertSame(pending, edit(pending, id = "m2"))
        assertSame(pending, stop(pending, id = "m2", session = "stop-2"))
        assertNull(pending.reduce(AncEvent.RequestDelete("p1", DeletableType.MEDICATION, "m2")).deleteConfirmation)
    }

    @Test
    fun stopSuccessShowsTheUndoBarAndKeepsTheScreen() {
        val state = stop()
        val token = tokenOf(state)
        // The clinician swiped to another tab while it saved: the commit does not pull them back.
        val elsewhere = state.reduce(AncEvent.SaveStarted(token)).reduce(AncEvent.SelectSection(WorkspaceSection.VISITS))
        val done = elsewhere.reduce(AncEvent.RecordCommitted(token, committed(state, commandOf(state), 4L), medsTab, SavedNotice.MEDICATIONS))
        assertEquals(AncRoute.Workspace("p1", WorkspaceSection.VISITS), done.route)
        assertNull(done.medicationQuickSave)
        assertEquals(SavePhase.IDLE, done.savePhase)
        val undo = done.medicationStopUndo!!
        assertEquals("m1", undo.medicationId)
        assertEquals(MEDICATION_STATUS_ACTIVE, undo.previousStatus)
        assertEquals("", undo.previousEndDate)
        assertEquals(MEDICATION_STATUS_DISCONTINUED, done.patient("p1")!!.medications.first { it.id == "m1" }.status)
    }

    @Test
    fun undoIsASecondSaveOfTheSameRowWithItsNewRevision() {
        val stopped = commit(stop(base(med(endDate = "2026-12-31", nonCompliant = true))), revision = 4L)
        val undo = stopped.medicationStopUndo!!
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, undo.previousStatus)

        val undoing = stopped.reduce(AncEvent.UndoMedicationStop(undo.token, "undo-1"))
        assertNull("the bar goes as Undo starts", undoing.medicationStopUndo)
        assertNull(undoing.medicationDraft)
        val command = commandOf(undoing)
        assertEquals("m1", command.medicationId)
        assertEquals("the revision the stop produced", 4L, command.expectedMedicationRevision)
        assertEquals(6L, command.expectedPatientRevision)
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, command.status)
        assertTrue(command.isNonCompliant)
        assertEquals("2026-12-31", command.endDate)

        val restored = commit(undoing, revision = 5L)
        assertNull("an Undo offers no further Undo", restored.medicationStopUndo)
        val row = restored.patient("p1")!!.medications.first { it.id == "m1" }
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, row.status)
        assertEquals("2026-12-31", row.endDate)
        assertEquals(medsTab, restored.route)
    }

    @Test
    fun undoWithAStaleTokenIsIgnored() {
        val stopped = commit(stop(), revision = 4L)
        assertSame(stopped, stopped.reduce(AncEvent.UndoMedicationStop("other", "undo-1")))
    }

    @Test
    fun stopFailureLeavesTheRowActiveAndShowsTheCause() {
        val state = stop()
        val token = tokenOf(state)
        val failed = state.reduce(AncEvent.SaveStarted(token))
            .reduce(AncEvent.SaveFailed(token, SaveUiError.STALE_OR_CONFLICT, unresolved = false))
        assertEquals(SavePhase.ERROR, failed.savePhase)
        assertEquals(SaveUiError.STALE_OR_CONFLICT, failed.saveError)
        assertNotNull("kept so the Meds tab can show the cause", failed.medicationQuickSave)
        assertNull(failed.medicationStopUndo)
        assertEquals(MEDICATION_STATUS_ACTIVE, failed.patient("p1")!!.medications.first { it.id == "m1" }.status)

        // The next action starts cleanly and clears the failure.
        val next = edit(failed)
        assertNotNull(next.medicationDraft)
        assertNull(next.medicationQuickSave)
        assertEquals(SavePhase.IDLE, next.savePhase)
        assertNull(next.saveError)
        // A second stop tap is allowed after a failure.
        assertNotNull(stop(failed, session = "stop-2").medicationQuickSave)
    }

    @Test
    fun aStoredRowThatFailsTheSaveRulesIsReportedNotSaved() {
        val failed = stop().reduce(AncEvent.ClinicalValidationFailed(ClinicalRecordKind.MEDICATION_UPDATE, setOf(ClinicalValidationError.REQUIRED_DOSAGE)))
        assertEquals(SavePhase.ERROR, failed.savePhase)
        assertEquals(SaveUiError.CONFIRMED_NOT_SAVED, failed.saveError)
    }

    @Test
    fun stopIsIgnoredWhenItCannotRun() {
        val stoppedRow = base(med(status = MEDICATION_STATUS_DISCONTINUED))
        assertSame(stoppedRow, stop(stoppedRow))
        val open = edit()
        assertSame("a draft is open", open, stop(open, id = "m2"))
        for (phase in listOf(SavePhase.PENDING, SavePhase.RECONCILING, SavePhase.UNRESOLVED)) {
            val saving = base().copy(savePhase = phase)
            assertSame(phase.name, saving, stop(saving))
        }
        val archived = AncState(patients = listOf(patient(med(), status = "archived")), vaultState = VaultState.Unlocked(1L), route = medsTab)
        assertSame(archived, stop(archived))
        val undoRunning = base().copy(
            undoOffer = UndoOffer("t", "p1", DeletableType.MEDICATION, UndoAction.DELETE, "x", phase = UndoPhase.RUNNING, recordId = "m2"),
        )
        assertNull(stop(undoRunning).medicationQuickSave)
        assertNull(stop(base(), id = "missing").medicationQuickSave)
    }

    @Test
    fun theUndoBarIsDroppedWhenAnotherActionStartsOrThePatientChanges() {
        val shown = commit(stop(), revision = 4L)
        assertNotNull(shown.medicationStopUndo)
        val drops = listOf(
            AncEvent.StartMedicationEditor("p1", "m2", "m2", "s"),
            AncEvent.StartMedicationEditor("p1", null, "n", "s", prefill = prefill),
            AncEvent.StopMedication("p1", "m2", "stop-2"),
            AncEvent.RequestDelete("p1", DeletableType.MEDICATION, "m2"),
            AncEvent.SelectSection(WorkspaceSection.VISITS),
            AncEvent.OpenWorkspace("p2"),
            AncEvent.StartVisit("p1", null, "v"),
            AncEvent.Back,
        )
        for (event in drops) {
            assertNull(event.toString(), shown.reduce(event).medicationStopUndo)
        }
        // Unrelated events keep it, and only its own expiry removes it.
        assertNotNull(shown.reduce(AncEvent.DismissNotice).medicationStopUndo)
        assertNotNull(shown.reduce(AncEvent.MedicationStopUndoExpired("other")).medicationStopUndo)
        assertNull(shown.reduce(AncEvent.MedicationStopUndoExpired(shown.medicationStopUndo!!.token)).medicationStopUndo)
        assertNull(shown.clearedForLock().medicationStopUndo)
    }

    @Test
    fun theUndoBarCountsFromTheCommitTimeTheEventCarries() {
        val undo = commit(stop(), revision = 4L).medicationStopUndo!!
        assertEquals(1_000L, undo.committedAtMillis)
        assertEquals(MEDICATION_STOP_UNDO_MILLIS, undo.remainingMillis(1_000L))
        assertEquals(4_000L, undo.remainingMillis(7_000L))
        assertEquals("over: the bar goes at once when shown again", 0L, undo.remainingMillis(11_000L))
        assertEquals("a clock that went back never gives more than the window", MEDICATION_STOP_UNDO_MILLIS, undo.remainingMillis(0L))
        assertEquals(500L, undo.remainingMillis(1_000L, windowMillis = 500L))
    }

    @Test
    fun aSecondStopTapOnTheSameRowWhileItSavesIsIgnored() {
        val pending = stop().reduce(AncEvent.SaveStarted(tokenOf(stop())))
        assertSame(pending, stop(pending, id = "m1", session = "stop-again"))
    }

    @Test
    fun stopOverwritesAnEndDateTheActiveRowAlreadyHad() {
        val command = commandOf(stop(base(med(endDate = "2027-05-05"))))
        assertEquals(today, command.endDate)
        val undo = commit(stop(base(med(endDate = "2027-05-05"))), revision = 4L).medicationStopUndo!!
        assertEquals("Undo puts the old end date back", "2027-05-05", undo.previousEndDate)
    }

    private fun failedStop(): AncState {
        val state = stop()
        val token = tokenOf(state)
        return state.reduce(AncEvent.SaveStarted(token))
            .reduce(AncEvent.SaveFailed(token, SaveUiError.STALE_OR_CONFLICT, unresolved = false))
    }

    @Test
    fun theFailureMessageCanBeDismissed() {
        val dismissed = failedStop().reduce(AncEvent.DismissMedicationStopFailure)
        assertNull(dismissed.medicationQuickSave)
        assertEquals(SavePhase.IDLE, dismissed.savePhase)
        assertNull(dismissed.saveError)
        assertEquals(MEDICATION_STATUS_ACTIVE, dismissed.patient("p1")!!.medications.first { it.id == "m1" }.status)
    }

    @Test
    fun dismissDoesNothingWhileTheStopSavesOrIsUnresolved() {
        val pending = stop().reduce(AncEvent.SaveStarted(tokenOf(stop())))
        assertSame(pending, pending.reduce(AncEvent.DismissMedicationStopFailure))
        val token = tokenOf(stop())
        val unresolved = pending.reduce(AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true))
        assertEquals(unresolved, unresolved.reduce(AncEvent.DismissMedicationStopFailure))
        // A failed sheet save is the sheet's own message: the stop dismiss leaves it alone.
        val sheetError = edit().copy(savePhase = SavePhase.ERROR, saveError = SaveUiError.STORAGE_ERROR)
        assertSame(sheetError, sheetError.reduce(AncEvent.DismissMedicationStopFailure))
    }

    @Test
    fun theFailureMessageGoesWhenAnotherActionStartsOrThePatientChanges() {
        for (event in listOf(
            AncEvent.RequestDelete("p1", DeletableType.MEDICATION, "m2"),
            AncEvent.OpenWorkspace("p2"),
            AncEvent.StartMedicationEditor("p1", "m2", "m2", "s"),
            AncEvent.StartMedicationEditor("p1", null, "n", "s", prefill = prefill),
            AncEvent.UndoRequested("t"),
            AncEvent.Back,
        )) {
            val next = failedStop().reduce(event)
            assertNull(event.toString(), next.medicationQuickSave)
            assertNull(event.toString(), next.saveError)
        }
        assertNotNull(failedStop().reduce(AncEvent.DismissNotice).medicationQuickSave)
    }

    @Test
    fun aTabChangeKeepsTheFailureCauseSoItIsNotLostUnseen() {
        val away = failedStop().reduce(AncEvent.SelectSection(WorkspaceSection.VISITS))
        assertNotNull(away.medicationQuickSave)
        assertEquals(SaveUiError.STALE_OR_CONFLICT, away.saveError)
        val back = away.reduce(AncEvent.SelectSection(WorkspaceSection.MEDICATIONS))
        assertNotNull(back.medicationQuickSave)
        assertEquals(SavePhase.ERROR, back.savePhase)
    }

    @Test
    fun anotherStopOrAnEditCanStartAfterAFailedStop() {
        val failed = failedStop()
        assertFalse("the hidden stop draft is not a dirty draft", failed.hasDirtyDraft)
        val dismissed = failed.reduce(AncEvent.DismissMedicationStopFailure)
        assertEquals("m2", stop(dismissed, id = "m2", session = "stop-2").medicationQuickSave!!.draft.medicationId)
        assertEquals("m1", edit(dismissed).medicationDraft!!.medicationId)
        // Without dismissing first, the next action replaces the failure too.
        assertEquals("m2", stop(failed, id = "m2", session = "stop-3").medicationQuickSave!!.draft.medicationId)
        assertEquals("m2", edit(failed, id = "m2").medicationDraft!!.medicationId)
    }

    @Test
    fun theHiddenStopDraftNeverCountsAsADirtyDraftSoStopsCanRepeat() {
        val saving = stop().reduce(AncEvent.SaveStarted(tokenOf(stop())))
        assertTrue(saving.medicationQuickSave!!.draft.isDirty)
        assertFalse(saving.hasDirtyDraft)
        val first = commit(stop(), revision = 4L)
        val second = stop(first, id = "m2", session = "stop-2")
        assertEquals("m2", second.medicationQuickSave!!.draft.medicationId)
        assertEquals(today, commandOf(second).endDate)
    }

    @Test
    fun aStoppedRowToggledActiveThenStoppedAgainKeepsItsStoredStopDate() {
        val state = base(med(status = MEDICATION_STATUS_DISCONTINUED, endDate = "2026-04-04"))
        val again = edit(state)
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_ACTIVE))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
            .medicationDraft!!
        assertEquals("2026-04-04", again.values.endDate)
        assertFalse("back where it started", again.isDirty)
    }

    @Test
    fun anActiveRowWithAPlannedEndDateStopsTodayNotOnThatDate() {
        val state = base(med(endDate = "2027-12-31"))
        val cleared = edit(state).reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, ""))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, MEDICATION_STATUS_DISCONTINUED))
        assertEquals(today, cleared.medicationDraft!!.values.endDate)
    }

    @Test
    fun aLockDropsTheHiddenStopDraft() {
        assertNull(failedStop().clearedForLock().medicationQuickSave)
        assertNull(stop().clearedForLock().medicationQuickSave)
    }

    // ---- UNRESOLVED ----

    /**
     * Documents today's rule, kept on purpose: when a medication save's outcome is unknown, editing stays blocked.
     * The sheet cannot be changed, closed or replaced, and no one-tap stop can start, until the vault is locked.
     */
    @Test
    fun unresolvedSaveKeepsMedicationEditingBlocked() {
        val draft = addFromTile()
        val command = (SaveMedicationCommand.prepare(draft, "00000000-0000-4000-8000-000000000002", "new-1") as ClinicalCommandPreparation.Ready).command
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        val unresolved = draft.reduce(AncEvent.SaveStarted(token))
            .reduce(AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true))
        assertEquals(SavePhase.UNRESOLVED, unresolved.savePhase)
        assertEquals(token, unresolved.pendingSave)
        for (event in listOf(
            AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "x"),
            AncEvent.CancelEditor,
            AncEvent.Back,
            AncEvent.ConfirmDiscard,
            AncEvent.StartMedicationEditor("p1", "m1", "m1", "s2"),
            AncEvent.StopMedication("p1", "m1", "stop"),
        )) {
            val next = unresolved.reduce(event)
            assertEquals(event.toString(), draft.medicationDraft, next.medicationDraft)
            assertEquals(event.toString(), SavePhase.UNRESOLVED, next.savePhase)
            assertNull(event.toString(), next.medicationQuickSave)
        }
    }

    // ---- recovery ----

    private fun recover(open: AncState, fields: Map<String, String>): AncState {
        val slot = activeDraftSlot(open)!!
        val record = DraftRecord(slot.patientId, DraftScreenKind.MEDICATION, DraftEditorKind.EDIT, "m1", 3L, fields, 1000L)
        val prompt = DraftRecoveryPrompt(slot, activeEditorSessionId(open)!!, record, isStale = false)
        return open.reduce(AncEvent.DraftRecoveryFound(prompt)).reduce(AncEvent.AcceptDraftRecovery)
    }

    @Test
    fun recoveringAnAddOrEditDraftReopensTheSheetOnTheTab() {
        for (open in listOf(addFromTile(), edit())) {
            val recovered = recover(open, mapOf("dosage" to "250 mg", "notes" to "recovered"))
            assertEquals(medsTab, recovered.route)
            val draft = recovered.medicationDraft!!
            assertEquals("250 mg", draft.values.dosage)
            assertEquals("recovered", draft.values.notes)
            assertTrue(draft.isDirty)
            assertTrue("recovered work asks before it is discarded", draft.touched)
            assertNull(recovered.draftRecoveryPrompt)
        }
    }
}
