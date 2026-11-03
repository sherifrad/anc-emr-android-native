package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.severeBpAttention
import com.sherifradwan.ancnative.storage.SavePreparation
import com.sherifradwan.ancnative.storage.SaveVisitCommand
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitDraftStateAndCommandTest {

    private val patient = DemoFixtures.patients[0]
    private val today = LocalDate.parse("2026-11-01")

    @Test
    fun startVisit_newVisit_hasEmptyStructuredDraftValues() {
        val state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-1")) }

        val draft = state.draft!!
        assertEquals("", draft.values.systolic)
        assertEquals("", draft.values.diastolic)
        assertEquals("", draft.values.fundalHeight)
        assertEquals("", draft.values.oedema)
        assertEquals("", draft.values.fetalMovements)
        assertEquals("", draft.values.urineProtein)
    }

    @Test
    fun startVisit_existingVisit_populatesStructuredDraftValues() {
        val existingVisit = DemoVisit(
            id = "v-existing-1",
            date = "2026-10-15",
            bloodPressure = "142/92",
            weight = "72.5 kg",
            findings = "FH 28 cm · Oedema mild · Movements good · Urine trace",
            notes = "Counselled on BP",
            revision = 2,
            isSyntheticEdit = false,
        )
        val patientWithVisit = patient.copy(visits = listOf(existingVisit))
        val state = AncState(patients = listOf(patientWithVisit))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, existingVisit.id, "session-2")) }

        val draft = state.draft!!
        assertEquals("142", draft.values.systolic)
        assertEquals("92", draft.values.diastolic)
        assertEquals("142/92", draft.values.bloodPressure)
        assertEquals("28", draft.values.fundalHeight)
        assertEquals("Mild", draft.values.oedema)
        assertEquals("Good", draft.values.fetalMovements)
        assertEquals("Trace", draft.values.urineProtein)
    }

    @Test
    fun draftChanged_systolicAndDiastolic_synchronizesBloodPressure() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-3")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "130"))
        assertEquals("130", state.draft!!.values.systolic)

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "85"))
        assertEquals("85", state.draft!!.values.diastolic)
        assertEquals("130/85", state.draft!!.values.bloodPressure)
    }

    @Test
    fun draftChanged_bloodPressureString_synchronizesSystolicAndDiastolic() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-4")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, "115/75"))
        assertEquals("115/75", state.draft!!.values.bloodPressure)
        assertEquals("115", state.draft!!.values.systolic)
        assertEquals("75", state.draft!!.values.diastolic)
    }

    @Test
    fun draftChanged_clearBloodPressure_clearsBothSystolicAndDiastolicAndSavedBp() {
        // G-2: Clearing BP removes old values and saves as null/unrecorded
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-bp-clear")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, "140/90"))
        assertEquals("140", state.draft!!.values.systolic)
        assertEquals("90", state.draft!!.values.diastolic)

        // Clear via BLOOD_PRESSURE field
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, ""))
        assertEquals("", state.draft!!.values.bloodPressure)
        assertEquals("", state.draft!!.values.systolic)
        assertEquals("", state.draft!!.values.diastolic)

        val prep = SaveVisitCommand.prepare(state, "op-clear", "v-clear", today)
        assertTrue(prep is SavePreparation.Ready)
        assertNull((prep as SavePreparation.Ready).command.bloodPressure)
    }

    @Test
    fun draftChanged_clearSystolicAndDiastolic_clearsBloodPressure() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-clear-pair")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "130"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "80"))
        assertEquals("130/80", state.draft!!.values.bloodPressure)

        // Clear both components
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, ""))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, ""))
        assertEquals("", state.draft!!.values.bloodPressure)

        val prep = SaveVisitCommand.prepare(state, "op-clear-2", "v-clear-2", today)
        assertTrue(prep is SavePreparation.Ready)
        assertNull((prep as SavePreparation.Ready).command.bloodPressure)
    }

    @Test
    fun saveVisitCommand_partialBloodPressure_isRejectedWithInvalidBp() {
        // G-2: Lone systolic must NOT be silently dropped or saved
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-partial-bp")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "185"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, ""))

        val prep = SaveVisitCommand.prepare(state, "op-partial", "v-partial", today)
        assertTrue(prep is SavePreparation.Rejected)
        val rejected = prep as SavePreparation.Rejected
        assertTrue(DraftError.BLOOD_PRESSURE_INVALID in rejected.errors)
    }

    @Test
    fun draftChanged_structuredFindings_updatesIndividualFields() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-5")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FUNDAL_HEIGHT, "30"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.OEDEMA, "None"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, "Good"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.URINE_PROTEIN, "Negative"))

        val draft = state.draft!!
        assertEquals("30", draft.values.fundalHeight)
        assertEquals("None", draft.values.oedema)
        assertEquals("Good", draft.values.fetalMovements)
        assertEquals("Negative", draft.values.urineProtein)
    }

    @Test
    fun saveVisitCommand_preparesWithStructuredFieldsComposed() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-6")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "120"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "80"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.WEIGHT, "70.5"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FUNDAL_HEIGHT, "28"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.OEDEMA, "None"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, "Good"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.URINE_PROTEIN, "Negative"))

        val prep = SaveVisitCommand.prepare(
            state = state,
            operationUuid = "op-uuid-1",
            newVisitId = "v-new-1",
            today = today,
        )

        assertTrue(prep is SavePreparation.Ready)
        val cmd = (prep as SavePreparation.Ready).command
        assertEquals("120/80", cmd.bloodPressure)
        assertEquals("70.5", cmd.weight)
        assertEquals("FH 28 cm · Oedema none · Movements good · Urine negative", cmd.findings)
    }

    @Test
    fun saveVisitCommand_editingExistingVisit_savesUpdatedStructuredFindings() {
        // G-1: Editing an existing visit's structured findings saves the NEW values, not the old free-text
        val existingVisit = DemoVisit(
            id = "v-edit-findings",
            date = "2026-10-15",
            bloodPressure = "120/80",
            weight = "70.0 kg",
            findings = "FH 28 cm · Oedema mild · Movements good · Urine trace",
            notes = "Old note",
            revision = 1,
            isSyntheticEdit = false,
        )
        val patientWithVisit = patient.copy(visits = listOf(existingVisit))
        var state = AncState(patients = listOf(patientWithVisit))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, existingVisit.id, "session-edit-1")) }

        // Clinician modifies structured fields to 30 cm and Marked oedema
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FUNDAL_HEIGHT, "30"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.OEDEMA, "Marked"))

        val prep = SaveVisitCommand.prepare(
            state = state,
            operationUuid = "op-edit-findings",
            newVisitId = "new-id",
            today = today,
        )

        assertTrue(prep is SavePreparation.Ready)
        val cmd = (prep as SavePreparation.Ready).command
        assertEquals("FH 30 cm · Oedema marked · Movements good · Urine trace", cmd.findings)
    }

    @Test
    fun saveVisitCommand_highBpIsAcceptedAndNeverBlocksSave() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-7")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        // High BP (150/95 >= 140/90)
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "150"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "95"))

        val prep = SaveVisitCommand.prepare(
            state = state,
            operationUuid = "op-uuid-2",
            newVisitId = "v-new-2",
            today = today,
        )

        // Ready, not rejected!
        assertTrue(prep is SavePreparation.Ready)
        val cmd = (prep as SavePreparation.Ready).command
        assertEquals("150/95", cmd.bloodPressure)
    }

    @Test
    fun saveVisitCommand_highBpIsVisibleToBpLevelAndSevereBpAttention() {
        // G-7: Verifies saved high BP is correctly identified by vital severity functions
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-severe-bp")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        // Severe BP (165/115 >= 160/110)
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "165"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "115"))

        val prep = SaveVisitCommand.prepare(state, "op-sev", "v-sev", today)
        assertTrue(prep is SavePreparation.Ready)
        val cmd = (prep as SavePreparation.Ready).command

        assertEquals(BpLevel.SEVERE, bpLevel(cmd.bloodPressure))
        val savedVisit = DemoVisit(
            id = cmd.visitId,
            date = cmd.date,
            bloodPressure = cmd.bloodPressure,
            weight = cmd.weight,
            findings = cmd.findings,
            notes = cmd.notes,
            revision = 1,
            isSyntheticEdit = false,
        )
        val attention = severeBpAttention(listOf(savedVisit))
        assertTrue(attention != null)
        assertEquals("165/115", attention!!.bloodPressure)
    }

    @Test
    fun saveVisitCommand_implausibleBpIsRejected() {
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-8")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        // Implausible BP: systolic < diastolic
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "80"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "120"))

        val prep = SaveVisitCommand.prepare(
            state = state,
            operationUuid = "op-uuid-3",
            newVisitId = "v-new-3",
            today = today,
        )

        assertTrue(prep is SavePreparation.Rejected)
        val rejected = prep as SavePreparation.Rejected
        assertTrue(DraftError.BLOOD_PRESSURE_INVALID in rejected.errors)
    }

    @Test
    fun draftPersistence_preservesAllStructuredFieldsRoundtrip() {
        // G-5: draftRecordFor -> applyDraftRecord roundtrip test for structured fields
        var state = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-roundtrip")) }

        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DATE, "2026-11-01"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.SYSTOLIC, "135"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.DIASTOLIC, "85"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.WEIGHT, "68.2"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FUNDAL_HEIGHT, "26"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.OEDEMA, "Mild"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.FETAL_MOVEMENTS, "Good"))
        state = AncReducer.reduce(state, AncEvent.DraftChanged(DraftField.URINE_PROTEIN, "Trace"))

        val nowMillis = 1730462400000L
        val draftRecord = draftRecordFor(state, nowMillis)
        assertTrue(draftRecord != null)

        // Simulate new session with initial empty draft and restore
        val freshDraftState = AncState(patients = listOf(patient))
            .let { AncReducer.reduce(it, AncEvent.StartVisit(patient.id, null, "session-fresh")) }

        val restoredState = applyDraftRecord(freshDraftState, draftRecord!!)
        val restoredDraft = restoredState.draft!!

        assertEquals("135", restoredDraft.values.systolic)
        assertEquals("85", restoredDraft.values.diastolic)
        assertEquals("135/85", restoredDraft.values.bloodPressure)
        assertEquals("68.2", restoredDraft.values.weight)
        assertEquals("26", restoredDraft.values.fundalHeight)
        assertEquals("Mild", restoredDraft.values.oedema)
        assertEquals("Good", restoredDraft.values.fetalMovements)
        assertEquals("Trace", restoredDraft.values.urineProtein)
    }
}
