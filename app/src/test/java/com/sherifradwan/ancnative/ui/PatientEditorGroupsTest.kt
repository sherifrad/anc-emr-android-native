package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.state.RegistrationStep
import org.junit.Assert.assertEquals
import org.junit.Test

class PatientEditorGroupsTest {
    private val both = setOf(PatientEditorGroup.IDENTITY, PatientEditorGroup.OBSTETRIC)

    @Test
    fun outside_a_flow_both_groups_show() {
        assertEquals(both, patientEditorGroups(null, emptySet()))
    }

    @Test
    fun identity_step_shows_only_identity() {
        assertEquals(setOf(PatientEditorGroup.IDENTITY), patientEditorGroups(RegistrationStep.IDENTITY, emptySet()))
    }

    @Test
    fun obstetric_step_shows_only_obstetric() {
        assertEquals(setOf(PatientEditorGroup.OBSTETRIC), patientEditorGroups(RegistrationStep.OBSTETRIC, emptySet()))
    }

    @Test
    fun hidden_obstetric_group_is_revealed_by_each_error_it_owns() {
        listOf(
            PatientValidationError.INVALID_GPA,
            PatientValidationError.GPA_ALL_OR_NONE,
            PatientValidationError.INVALID_BLOOD_GROUP,
            PatientValidationError.INVALID_RISK_LEVEL,
        ).forEach { error ->
            assertEquals(
                error.name,
                both,
                patientEditorGroups(RegistrationStep.IDENTITY, setOf(error)),
            )
        }
    }

    @Test
    fun hidden_identity_group_is_revealed_by_each_error_it_owns() {
        listOf(PatientValidationError.AGE_REQUIRED, PatientValidationError.FULL_NAME_REQUIRED).forEach { error ->
            assertEquals(error.name, both, patientEditorGroups(RegistrationStep.OBSTETRIC, setOf(error)))
        }
    }

    @Test
    fun current_group_comes_first_then_the_revealed_one() {
        assertEquals(
            listOf(PatientEditorGroup.OBSTETRIC, PatientEditorGroup.IDENTITY),
            patientEditorGroups(RegistrationStep.OBSTETRIC, setOf(PatientValidationError.AGE_REQUIRED)).toList(),
        )
    }

    @Test
    fun errors_that_belong_to_no_field_reveal_nothing() {
        listOf(
            PatientValidationError.STALE_REVISION,
            PatientValidationError.SAVE_FAILED,
            PatientValidationError.CASE_ID_REQUIRED,
            PatientValidationError.CASE_ID_CONFLICT,
        ).forEach { error ->
            assertEquals(
                setOf(PatientEditorGroup.IDENTITY),
                patientEditorGroups(RegistrationStep.IDENTITY, setOf(error)),
            )
            assertEquals(
                setOf(PatientEditorGroup.OBSTETRIC),
                patientEditorGroups(RegistrationStep.OBSTETRIC, setOf(error)),
            )
        }
    }

    @Test
    fun an_error_in_the_shown_group_does_not_add_the_other_group() {
        assertEquals(
            setOf(PatientEditorGroup.IDENTITY),
            patientEditorGroups(RegistrationStep.IDENTITY, setOf(PatientValidationError.AGE_REQUIRED)),
        )
    }
}
