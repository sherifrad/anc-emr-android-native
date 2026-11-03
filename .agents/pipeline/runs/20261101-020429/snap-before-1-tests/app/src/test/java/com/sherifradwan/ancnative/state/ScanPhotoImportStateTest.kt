package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScanPhotoImportStateTest {
    private val patientId = DemoFixtures.patients[0].id
    private fun initialState() = AncState(patients = DemoFixtures.patients)
    private fun AncState.reduce(event: AncEvent): AncState = AncReducer.reduce(this, event)
    private fun withDraft(): AncState = initialState().reduce(
        AncEvent.StartUltrasoundEditor(patientId, scanId = null, newScanId = "scan-new", editorSessionId = "sess-1"),
    ).also { assertNotNull(it.ultrasoundDraft) }

    @Test fun appendsRefToDraft() {
        val after = withDraft().reduce(AncEvent.ScanPhotoImported(patientId, "vault:a.webp"))
        assertEquals(listOf("vault:a.webp"), after.ultrasoundDraft!!.values.imageUris)
    }

    @Test fun keepsOrderAndExistingPhotos() {
        val after = withDraft()
            .reduce(AncEvent.ScanPhotoImported(patientId, "vault:a.webp"))
            .reduce(AncEvent.ScanPhotoImported(patientId, "vault:b.webp"))
        assertEquals(listOf("vault:a.webp", "vault:b.webp"), after.ultrasoundDraft!!.values.imageUris)
    }

    @Test fun neverDuplicatesARef() {
        val once = withDraft().reduce(AncEvent.ScanPhotoImported(patientId, "vault:a.webp"))
        val twice = once.reduce(AncEvent.ScanPhotoImported(patientId, "vault:a.webp"))
        assertEquals(listOf("vault:a.webp"), twice.ultrasoundDraft!!.values.imageUris)
    }

    @Test fun noDraftLeavesStateUnchanged() {
        val before = initialState()
        assertEquals(before, before.reduce(AncEvent.ScanPhotoImported(patientId, "vault:a.webp")))
    }

    @Test fun draftOfAnotherPatientIsUntouched() {
        val before = withDraft()
        val after = before.reduce(AncEvent.ScanPhotoImported("some-other-patient", "vault:a.webp"))
        assertEquals(before, after)
    }
}
