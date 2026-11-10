package com.sherifradwan.ancnative.model

/**
 * Rules of the patient profile form. The command builder (`SavePatientProfileCommand.prepare`) enforces the full
 * rule, using the age stored when the editor opened. `isStructurallyValid` only enforces the create case, because
 * a stored command does not carry the age the row had before it.
 */
object PatientProfileRules {
    /**
     * Required for a new patient; for an existing one only if it already has a stored age. The Identity step of the
     * registration flow forces it for every patient.
     */
    fun ageRequired(isNew: Boolean, storedAgeText: String?, forceAgeRequired: Boolean = false): Boolean =
        isNew || forceAgeRequired || !storedAgeText.isNullOrBlank()
}
