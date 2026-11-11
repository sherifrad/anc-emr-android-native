package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.state.RegistrationStep

enum class PatientEditorGroup { IDENTITY, OBSTETRIC }

private val IDENTITY_ERRORS = setOf(
    PatientValidationError.FULL_NAME_REQUIRED,
    PatientValidationError.AGE_REQUIRED,
)

private val OBSTETRIC_ERRORS = setOf(
    PatientValidationError.GPA_ALL_OR_NONE,
    PatientValidationError.INVALID_GPA,
    PatientValidationError.INVALID_BLOOD_GROUP,
    PatientValidationError.INVALID_RISK_LEVEL,
)

/**
 * Which field groups the patient editor shows, the current step's group first. Outside a flow ([step] null) both show.
 * A group the step hides still appears when it owns one of [errors], so a failed field is never invisible.
 */
fun patientEditorGroups(step: RegistrationStep?, errors: Set<PatientValidationError>): Set<PatientEditorGroup> {
    val groups = linkedSetOf<PatientEditorGroup>()
    when (step) {
        RegistrationStep.IDENTITY -> groups += PatientEditorGroup.IDENTITY
        RegistrationStep.OBSTETRIC -> groups += PatientEditorGroup.OBSTETRIC
        else -> groups += PatientEditorGroup.entries
    }
    if (errors.any { it in IDENTITY_ERRORS }) groups += PatientEditorGroup.IDENTITY
    if (errors.any { it in OBSTETRIC_ERRORS }) groups += PatientEditorGroup.OBSTETRIC
    return groups
}
