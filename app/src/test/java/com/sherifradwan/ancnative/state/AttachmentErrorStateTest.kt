package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttachmentErrorStateTest {
    private val patientId = DemoFixtures.patients[0].id
    private fun initialState() = AncState(patients = DemoFixtures.patients)
    private fun AncState.reduce(event: AncEvent): AncState = AncReducer.reduce(this, event)

    @Test fun noErrorByDefault() {
        assertNull(initialState().attachmentError)
    }

    @Test fun saveFailureSetsErrorAndChangesNothingElse() {
        val before = initialState()
        val after = before.reduce(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
        assertEquals(AttachmentError.SAVE_FAILED, after.attachmentError)
        assertEquals(before, after.copy(attachmentError = null))
    }

    @Test fun newerFailureReplacesOlderOne() {
        val after = initialState()
            .reduce(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
        assertEquals(AttachmentError.SAVE_FAILED, after.attachmentError)
    }

    @Test fun dismissClearsErrorAndChangesNothingElse() {
        val failed = initialState().reduce(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
        val dismissed = failed.reduce(AncEvent.DismissAttachmentError)
        assertNull(dismissed.attachmentError)
        assertEquals(initialState(), dismissed)
    }

    @Test fun dismissWithoutErrorIsANoOp() {
        val before = initialState()
        assertEquals(before, before.reduce(AncEvent.DismissAttachmentError))
    }

    @Test fun unrelatedEventsKeepTheError() {
        val failed = initialState().reduce(AncEvent.AttachmentFailed(AttachmentError.SAVE_FAILED))
        val afterWorkspace = failed.reduce(AncEvent.OpenWorkspace(patientId))
        assertEquals(AttachmentError.SAVE_FAILED, afterWorkspace.attachmentError)
    }
}
