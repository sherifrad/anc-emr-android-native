package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The real reducer view of a payload cleanup that failed after the delete was committed. */
class PayloadCleanupStateTest {
    private fun patient(id: String) = DemoPatient(
        id = id,
        caseId = id.uppercase(),
        displayName = "Patient $id",
        dateOfBirth = "1995-01-01",
        phone = "01000000000",
        visits = emptyList(),
        attachments = listOf(
            DemoAttachment(
                id = "x1",
                patientId = id,
                fileName = "scan.webp",
                filePath = "patient_attachments/$id/scan.webp",
                category = "Ultrasound",
                fileSizeBytes = 1024,
                sha256Hash = "00",
                title = "Scan 2026-01-05",
                capturedAt = "2026-01-05T00:00:00Z",
            ),
        ),
    )

    private fun state(unlocked: Boolean = true) = AncState(
        patients = listOf(patient("p1"), patient("p2")),
        vaultState = if (unlocked) VaultState.Unlocked(1L) else VaultState.Locked,
    )

    private fun cleanupFailure() = AncEvent.PayloadCleanupFailed(
        token = "t1",
        error = SaveUiError.STORAGE_ERROR,
        patientId = "p1",
        type = DeletableType.ATTACHMENT,
        label = "Scan 2026-01-05",
        patientName = "Patient p1",
    )

    @Test
    fun aCleanupFailureNamesTheRecordWhileUnlocked() {
        val next = AncReducer.reduce(state(), cleanupFailure())
        val message = next.deleteMessage!!
        assertEquals(DeleteMessageKind.CLEANUP_FAILED, message.kind)
        assertEquals("p1", message.patientId)
        assertEquals(DeletableType.ATTACHMENT, message.type)
        assertEquals("Scan 2026-01-05", message.label)
        assertEquals("Patient p1", message.patientName)
    }

    @Test
    fun aLockedStateIsNeverWritten() {
        val next = AncReducer.reduce(state(unlocked = false), cleanupFailure())
        assertNull(next.deleteMessage)
    }

    @Test
    fun anUndoFailureAboutTheSameRecordIsTheStrongerMessage() {
        val withUndoFailure = AncReducer.reduce(
            state(),
            AncEvent.UndoFailed(
                token = "t1",
                error = SaveUiError.STORAGE_ERROR,
                offer = UndoOffer(
                    token = "t1",
                    patientId = "p1",
                    type = DeletableType.ATTACHMENT,
                    action = UndoAction.DELETE,
                    label = "Scan 2026-01-05",
                    patientName = "Patient p1",
                    recordId = "x1",
                ),
            ),
        )
        assertEquals(DeleteMessageKind.UNDO_FAILED, withUndoFailure.deleteMessage?.kind)
        val next = AncReducer.reduce(withUndoFailure, cleanupFailure())
        assertEquals(DeleteMessageKind.UNDO_FAILED, next.deleteMessage?.kind)
    }

    @Test
    fun theCleanupNoticeSurvivesAPatientSwitch() {
        val withMessage = AncReducer.reduce(state(), cleanupFailure())
        val onAnotherPatient = withMessage.copy(route = AncRoute.Workspace("p2", WorkspaceSection.ATTACHMENTS))
        // Any reduce runs the UI-consistency pass that normally drops another patient's message.
        val next = AncReducer.reduce(onAnotherPatient, AncEvent.UndoExpired("no-such-token"))
        assertEquals(DeleteMessageKind.CLEANUP_FAILED, next.deleteMessage?.kind)
        assertEquals("p1", next.deleteMessage?.patientId)
    }

    @Test
    fun anotherPatientsSuccessDoesNotWipeIt() {
        val withMessage = AncReducer.reduce(state(), cleanupFailure())
        val next = AncReducer.reduce(
            withMessage,
            AncEvent.UndoOffered(
                UndoOffer(
                    token = "t2",
                    patientId = "p2",
                    type = DeletableType.ATTACHMENT,
                    action = UndoAction.DELETE,
                    label = "Another scan",
                    patientName = "Patient p2",
                    recordId = "y1",
                ),
            ),
        )
        assertEquals(DeleteMessageKind.CLEANUP_FAILED, next.deleteMessage?.kind)
        assertEquals("p1", next.deleteMessage?.patientId)
    }
}
