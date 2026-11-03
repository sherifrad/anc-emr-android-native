package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveUiErrorTextTest {
    private val expected = mapOf(
        SaveUiError.CONFIRMED_NOT_SAVED to R.string.save_confirmed_not_saved,
        SaveUiError.STALE_OR_CONFLICT to R.string.save_stale_conflict,
        SaveUiError.CROSS_PATIENT_REJECTED to R.string.save_cross_patient_rejected,
        SaveUiError.DUPLICATE_OPERATION_MISMATCH to R.string.save_operation_mismatch,
        SaveUiError.PATIENT_ARCHIVED to R.string.save_patient_archived,
        SaveUiError.CASE_ID_CONFLICT to R.string.save_case_id_conflict,
        SaveUiError.STORAGE_ERROR to R.string.save_storage_error,
        SaveUiError.OUTCOME_UNRESOLVED to R.string.save_unresolved,
    )

    @Test
    fun everyErrorMapsToItsString() {
        assertEquals(SaveUiError.values().toSet(), expected.keys)
        for ((error, res) in expected) {
            assertEquals(error.name, res, error.stringRes())
        }
    }

    @Test
    fun nullMapsToStorageError() {
        val none: SaveUiError? = null
        assertEquals(R.string.save_storage_error, none.stringRes())
    }

    @Test
    fun allValuesAreDistinct() {
        val ids = SaveUiError.values().map { it.stringRes() }
        assertEquals(ids.size, ids.toSet().size)
    }
}
