package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoAttachment
import org.junit.Assert.assertEquals
import org.junit.Test

/** The label the clinician sees for an attachment in the shared delete dialog, Undo bar and failure notice. */
class AttachmentDeleteAdapterTest {
    private fun attachment(title: String?, category: String = AttachmentCategories.ULTRASOUND, capturedAt: String = "2026-10-30T20:50:18.778306Z") =
        DemoAttachment(
            id = "x1",
            patientId = "p1",
            fileName = "att_d8b472c1-8195-4cb9-a96b-19c5a03dfc73.webp",
            filePath = "patient_attachments/p1/att_d8b472c1.webp",
            category = category,
            fileSizeBytes = 3198,
            sha256Hash = "00",
            title = title,
            capturedAt = capturedAt,
        )

    @Test
    fun aTitleWinsOverEverythingElse() {
        assertEquals("Scan at 36 weeks", AttachmentDeleteAdapter.label(attachment("Scan at 36 weeks")))
    }

    @Test
    fun aBlankTitleFallsBackToCategoryAndDay() {
        assertEquals("Ultrasound Scan · 2026-10-30", AttachmentDeleteAdapter.label(attachment("   ")))
    }

    @Test
    fun aMissingTitleFallsBackToCategoryAndDay() {
        assertEquals("Laboratory Investigation · 2026-10-30", AttachmentDeleteAdapter.label(attachment(null, AttachmentCategories.LAB)))
    }

    @Test
    fun theInternalFileNameIsNeverTheLabel() {
        val label = AttachmentDeleteAdapter.label(attachment(null))
        assertEquals(false, label.contains("att_"))
        assertEquals(false, label.contains(".webp"))
    }

    @Test
    fun anUnparseableCaptureDateKeepsTheCategoryOnly() {
        assertEquals("Clinical Photo", AttachmentDeleteAdapter.label(attachment(null, AttachmentCategories.CLINICAL_PHOTO, capturedAt = "")))
        assertEquals("Clinical Photo", AttachmentDeleteAdapter.label(attachment(null, AttachmentCategories.CLINICAL_PHOTO, capturedAt = "unknown")))
    }
}
