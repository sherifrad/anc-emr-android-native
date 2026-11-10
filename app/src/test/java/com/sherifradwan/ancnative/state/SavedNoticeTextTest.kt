package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedNoticeTextTest {
    private val expected = mapOf(
        SavedNotice.VISIT to R.string.visit_saved,
        SavedNotice.PATIENT to R.string.patient_saved,
        SavedNotice.HISTORIES to R.string.histories_saved,
        SavedNotice.PREGNANCY to R.string.pregnancy_saved,
        SavedNotice.ARCHIVED to R.string.patient_archived_notice,
        SavedNotice.UNARCHIVED to R.string.patient_unarchived_notice,
        SavedNotice.DATING to R.string.dating_saved,
        SavedNotice.ULTRASOUND to R.string.ultrasound_saved,
        SavedNotice.LABORATORY to R.string.lab_saved,
        SavedNotice.MEDICATIONS to R.string.medication_saved,
        SavedNotice.MEDICATION_DELETED to R.string.medication_deleted,
        SavedNotice.MEDICATION_RESTORED to R.string.medication_restored,
        SavedNotice.MEDICATION_SAVE_UNDONE to R.string.medication_save_undone,
        SavedNotice.LAB_DELETED to R.string.lab_deleted,
        SavedNotice.LAB_RESTORED to R.string.lab_restored,
        SavedNotice.LAB_SAVE_UNDONE to R.string.lab_save_undone,
        SavedNotice.ULTRASOUND_DELETED to R.string.ultrasound_deleted,
        SavedNotice.ULTRASOUND_RESTORED to R.string.ultrasound_restored,
        SavedNotice.ULTRASOUND_SAVE_UNDONE to R.string.ultrasound_save_undone,
        SavedNotice.VISIT_DELETED to R.string.visit_deleted,
        SavedNotice.VISIT_RESTORED to R.string.visit_restored,
        SavedNotice.VISIT_SAVE_UNDONE to R.string.visit_save_undone,
        SavedNotice.PREVIOUS_PREGNANCY_DELETED to R.string.previous_pregnancy_deleted,
        SavedNotice.PREVIOUS_PREGNANCY_RESTORED to R.string.previous_pregnancy_restored,
        SavedNotice.PREVIOUS_PREGNANCY_SAVE_UNDONE to R.string.previous_pregnancy_save_undone,
        SavedNotice.ATTACHMENT_DELETED to R.string.attachment_deleted,
        SavedNotice.ATTACHMENT_RESTORED to R.string.attachment_restored,
    )

    @Test
    fun everyNoticeMapsToItsOwnString() {
        assertEquals(SavedNotice.values().toSet(), expected.keys)
        for ((notice, res) in expected) assertEquals(notice.name, res, notice.stringRes())
        assertEquals("every notice has a distinct string", expected.size, expected.values.toSet().size)
    }

    @Test
    fun nullNoticeFallsBackToVisitSaved() {
        val none: SavedNotice? = null
        assertEquals(R.string.visit_saved, none.stringRes())
    }

    @Test
    fun deleteRestoreAndSaveUndoNeverReadAsASave() {
        val saved = setOf(R.string.medication_saved, R.string.lab_saved, R.string.ultrasound_saved, R.string.visit_saved)
        for (type in DeletableType.values()) {
            val adapter = DeletableRegistry.of<com.sherifradwan.ancnative.storage.DeletableRecord>(type)
            val three = listOf(adapter.deletedNotice, adapter.restoredNotice, adapter.saveUndoneNotice)
            // Attachments have no save-undo flow: their saveUndoneNotice is the deleted notice, so only two are distinct.
            val noSaveUndo = type == DeletableType.ATTACHMENT
            assertEquals(type.name, if (noSaveUndo) 2 else 3, three.toSet().size)
            if (noSaveUndo) assertEquals(type.name, adapter.deletedNotice, adapter.saveUndoneNotice)
            for (n in three) {
                assertTrue("$type $n", n.stringRes() !in saved)
                assertNotEquals(type.name, SavedNotice.MEDICATIONS, n)
                assertNotEquals(type.name, SavedNotice.LABORATORY, n)
                assertNotEquals(type.name, SavedNotice.ULTRASOUND, n)
                assertNotEquals(type.name, SavedNotice.VISIT, n)
            }
        }
    }
}
