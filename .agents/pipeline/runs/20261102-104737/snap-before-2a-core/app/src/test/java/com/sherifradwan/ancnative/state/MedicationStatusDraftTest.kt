package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_NON_COMPLIANT
import com.sherifradwan.ancnative.storage.ClinicalCommandPreparation
import com.sherifradwan.ancnative.storage.SaveMedicationCommand
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationStatusDraftTest {
    private fun med(status: String, isNonCompliant: Boolean, isProphylaxis: Boolean = false) = DemoMedicationRecord(
        id = "m1", patientId = "p1", name = "Iron", dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-01-01", endDate = null, status = status,
        notes = null, isProphylaxis = isProphylaxis, isNonCompliant = isNonCompliant,
    )

    private fun patient(vararg meds: DemoMedicationRecord) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01",
        phone = "01000000000", visits = emptyList(), status = "active", medications = meds.toList(),
    )

    private fun editing(record: DemoMedicationRecord): AncState {
        val state = AncState(patients = listOf(patient(record)), vaultState = VaultState.Unlocked(1L))
        return AncReducer.reduce(state, AncEvent.StartMedicationEditor("p1", "m1", "m1", "session-1"))
    }

    private fun setStatus(state: AncState, value: String) =
        AncReducer.reduce(state, AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, value))

    private fun values(state: AncState) = state.medicationDraft!!.values

    private fun prepared(state: AncState): SaveMedicationCommand {
        val result = SaveMedicationCommand.prepare(state, "op-1", "m1")
        assertTrue(result is ClinicalCommandPreparation.Ready)
        return (result as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
    }

    @Test
    fun changingStatusToNonCompliantSetsFlagAndBackToActiveClearsIt() {
        var state = editing(med("Active", false))
        state = setStatus(state, MEDICATION_STATUS_NON_COMPLIANT)
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, values(state).status)
        assertTrue(values(state).isNonCompliant)
        state = setStatus(state, "Active")
        assertEquals("Active", values(state).status)
        assertFalse(values(state).isNonCompliant)
    }

    @Test
    fun otherStatusesClearTheFlag() {
        var state = setStatus(editing(med("Active", false)), MEDICATION_STATUS_NON_COMPLIANT)
        state = setStatus(state, "Completed")
        assertFalse(values(state).isNonCompliant)
        state = setStatus(setStatus(state, MEDICATION_STATUS_NON_COMPLIANT), "Discontinued")
        assertFalse(values(state).isNonCompliant)
    }

    @Test
    fun loadingOldActiveWithFlagGivesNonCompliantStatus() {
        val state = editing(med("Active", true))
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, values(state).status)
        assertTrue(values(state).isNonCompliant)
    }

    @Test
    fun loadingCompletedWithOldFlagKeepsStatus() {
        assertEquals("Completed", values(editing(med("Completed", true))).status)
    }

    @Test
    fun savingCommandCarriesConsistentFlagAndStatus() {
        val nonCompliant = setStatus(editing(med("Active", false)), MEDICATION_STATUS_NON_COMPLIANT)
        val cmd = prepared(nonCompliant)
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, cmd.status)
        assertTrue(cmd.isNonCompliant)

        val activeCmd = prepared(setStatus(nonCompliant, "Active"))
        assertEquals("Active", activeCmd.status)
        assertFalse(activeCmd.isNonCompliant)
    }

    @Test
    fun isProphylaxisOfLoadedRecordIsPreservedThroughStatusChangeAndSave() {
        var state = editing(med("Active", false, isProphylaxis = true))
        assertTrue(values(state).isProphylaxis)
        state = setStatus(state, MEDICATION_STATUS_NON_COMPLIANT)
        assertTrue(values(state).isProphylaxis)
        assertTrue(prepared(state).isProphylaxis)
    }

    @Test
    fun newDraftHasNoProphylaxisAndActiveStatus() {
        val state = AncState(patients = listOf(patient()), vaultState = VaultState.Unlocked(1L))
        val started = AncReducer.reduce(state, AncEvent.StartMedicationEditor("p1", null, "m2", "session-2"))
        assertFalse(values(started).isProphylaxis)
        assertEquals("Active", values(started).status)
    }
}
