package com.sherifradwan.ancnative.model

/**
 * Rules of the patient profile form. The command builder (`SavePatientProfileCommand.prepare`) enforces the full
 * rule, using the age stored when the editor opened. `isStructurallyValid` only enforces the create case, because
 * a stored command does not carry the age the row had before it.
 */
object PatientProfileRules {
    /** Required for a new patient; for an existing one only if it already has a stored age. */
    fun ageRequired(isNew: Boolean, storedAgeText: String?): Boolean =
        isNew || !storedAgeText.isNullOrBlank()
}
