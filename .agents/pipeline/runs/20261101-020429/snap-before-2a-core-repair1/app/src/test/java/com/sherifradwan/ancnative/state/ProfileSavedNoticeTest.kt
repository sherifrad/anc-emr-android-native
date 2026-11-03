package com.sherifradwan.ancnative.state

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSavedNoticeTest {
    @Test
    fun noDatingAttemptReportsPatientSaved() {
        assertEquals(
            SavedNotice.PATIENT,
            profileSavedNotice(datingAttempted = false, datingSaved = false),
        )
    }

    @Test
    fun datingAttemptedAndSavedReportsPatientSaved() {
        assertEquals(
            SavedNotice.PATIENT,
            profileSavedNotice(datingAttempted = true, datingSaved = true),
        )
    }

    @Test
    fun datingSavedWithoutAnAttemptStillReportsPatientSaved() {
        assertEquals(
            SavedNotice.PATIENT,
            profileSavedNotice(datingAttempted = false, datingSaved = true),
        )
    }

    @Test
    fun datingAttemptedButNotSavedReportsPatientDatingNotUpdated() {
        assertEquals(
            SavedNotice.PATIENT_DATING_NOT_UPDATED,
            profileSavedNotice(datingAttempted = true, datingSaved = false),
        )
    }
}
