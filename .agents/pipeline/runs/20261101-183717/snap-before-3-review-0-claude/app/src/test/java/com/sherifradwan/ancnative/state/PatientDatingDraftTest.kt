package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Round 4g / Q271-Q272: the new-patient draft carries the dating choice and keeps it across save and restore. */
class PatientDatingDraftTest {
    private fun base() = AncState(patients = emptyList(), vaultState = VaultState.Unlocked(1L))

    private fun stateWith(values: PatientDraftValues, original: PatientDraftValues = PatientDraftValues(), session: String = "s1") =
        base().copy(patientDraft = PatientDraft(session, "new", null, null, original, values))

    private val allSeven = PatientDraftValues(
        displayName = "Mona",
        datingMethod = "Ultrasound",
        transferDate = "2026-03-01",
        embryoAgeDays = "5",
        agreedEdd = "2026-11-20",
        scanDate = "2026-03-01",
        scanGaWeeks = "8",
        scanGaDays = "3",
    )

    private fun reduced(field: PatientDraftField, value: String): PatientDraftValues =
        AncReducer.reduce(stateWith(PatientDraftValues()), AncEvent.PatientDraftChanged(field, value)).patientDraft!!.values

    @Test fun reducerSetsEachNewField() {
        assertEquals("IVF_TRANSFER", reduced(PatientDraftField.DATING_METHOD, "IVF_TRANSFER").datingMethod)
        assertEquals("2026-03-01", reduced(PatientDraftField.TRANSFER_DATE, "2026-03-01").transferDate)
        assertEquals("5", reduced(PatientDraftField.EMBRYO_AGE_DAYS, "5").embryoAgeDays)
        assertEquals("2026-11-20", reduced(PatientDraftField.AGREED_EDD, "2026-11-20").agreedEdd)
        assertEquals("2026-03-01", reduced(PatientDraftField.SCAN_DATE, "2026-03-01").scanDate)
        assertEquals("8", reduced(PatientDraftField.SCAN_GA_WEEKS, "8").scanGaWeeks)
        assertEquals("3", reduced(PatientDraftField.SCAN_GA_DAYS, "3").scanGaDays)
    }

    @Test fun reducerChangesOnlyTheNamedField() {
        val v = reduced(PatientDraftField.SCAN_GA_WEEKS, "8")
        assertEquals(PatientDraftValues(scanGaWeeks = "8"), v)
    }

    @Test fun newFieldsDefaultToBlank() {
        val v = PatientDraftValues()
        assertEquals(listOf("", "", "", "", "", "", ""), listOf(v.datingMethod, v.transferDate, v.embryoAgeDays, v.agreedEdd, v.scanDate, v.scanGaWeeks, v.scanGaDays))
    }

    @Test fun saveThenRestoreKeepsAllSevenValues() {
        val typed = stateWith(allSeven)
        val record = draftRecordFor(typed, 1234L)!!
        val fresh = stateWith(PatientDraftValues(), session = "fresh")
        val restored = applyDraftRecord(fresh, record).patientDraft!!.values
        assertEquals(allSeven.datingMethod, restored.datingMethod)
        assertEquals(allSeven.transferDate, restored.transferDate)
        assertEquals(allSeven.embryoAgeDays, restored.embryoAgeDays)
        assertEquals(allSeven.agreedEdd, restored.agreedEdd)
        assertEquals(allSeven.scanDate, restored.scanDate)
        assertEquals(allSeven.scanGaWeeks, restored.scanGaWeeks)
        assertEquals(allSeven.scanGaDays, restored.scanGaDays)
        assertEquals("Mona", restored.displayName)
    }

    @Test fun oldRecordWithoutTheKeysRestoresUnchanged() {
        val typed = stateWith(allSeven)
        val full = draftRecordFor(typed, 1234L)!!
        val sevenKeys = setOf("datingMethod", "transferDate", "embryoAgeDays", "agreedEdd", "scanDate", "scanGaWeeks", "scanGaDays")
        val old: DraftRecord = full.copy(fields = full.fields - sevenKeys)
        val current = PatientDraftValues(datingMethod = "Clinical", agreedEdd = "2026-11-20")
        val fresh = stateWith(current, session = "fresh")
        val restored = applyDraftRecord(fresh, old).patientDraft!!.values
        assertEquals("Mona", restored.displayName)
        assertEquals(current.copy(displayName = "Mona"), restored)
    }
}
