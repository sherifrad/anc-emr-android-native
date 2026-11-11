package com.sherifradwan.ancnative.model

/** Rules of the previous-pregnancy form, shared by the draft reducer, the command builder and validation. */
object PreviousPregnancyFormRules {
    const val DEFAULT_CLINICIAN_ID = "Dr. Sherif Radwan"

    const val MODE_VAGINAL = "Vaginal"
    const val MODE_CS = "CS"
    val deliveryModeChoices = listOf(MODE_VAGINAL, MODE_CS)

    data class DeliveryColumns(val deliveryBy: String?, val birthMethod: String?)

    fun hasDeliveryMode(outcome: String?): Boolean = outcome == "LiveBirth" || outcome == "Stillbirth"

    /**
     * The one rule for the stored delivery columns. They are never cleared on their own: a picked mode (even an
     * empty one) overwrites both through the Vaginal/CS mapping, a change of outcome to loss or termination
     * (compared with the outcome the row was opened with) clears them, and otherwise they are kept as stored.
     */
    fun deliveryColumns(
        outcome: String?,
        originalOutcome: String?,
        deliveryBy: String?,
        birthMethod: String?,
        pickedMode: String? = null,
        originalDeliveryBy: String? = null,
        originalBirthMethod: String? = null,
    ): DeliveryColumns = when {
        pickedMode != null -> when (pickedMode) {
            MODE_VAGINAL -> DeliveryColumns(MODE_VAGINAL, "Vaginal")
            MODE_CS -> DeliveryColumns(MODE_CS, "Cesarean")
            else -> DeliveryColumns(null, null)
        }
        (outcome == "EarlyPregnancyLoss" || outcome == "TerminationOfPregnancy") && outcome != originalOutcome ->
            DeliveryColumns(null, null)
        hasDeliveryMode(outcome) && hasDeliveryMode(originalOutcome) && deliveryBy.isNullOrBlank() && birthMethod.isNullOrBlank() ->
            DeliveryColumns(originalDeliveryBy?.ifBlank { null }, originalBirthMethod?.ifBlank { null })
        else -> DeliveryColumns(deliveryBy?.ifBlank { null }, birthMethod?.ifBlank { null })
    }

    /** The form choice an existing row shows, with the reader priority of ObstetricHistory: Cesarean wins. */
    fun modeOf(deliveryBy: String?, birthMethod: String?): String {
        val bm = birthMethod?.trim()
        val db = deliveryBy?.trim()
        return when {
            bm.equals("Cesarean", ignoreCase = true) || db.equals(MODE_CS, ignoreCase = true) -> MODE_CS
            bm.equals("Vaginal", ignoreCase = true) || db.equals(MODE_VAGINAL, ignoreCase = true) -> MODE_VAGINAL
            else -> ""
        }
    }

    /** New pregnancies get the default clinician; an existing row keeps its stored id. Never blank. */
    fun clinicianIdFor(isNew: Boolean, storedClinicianId: String): String =
        if (isNew) DEFAULT_CLINICIAN_ID else storedClinicianId.trim().ifEmpty { DEFAULT_CLINICIAN_ID }

    /** Required for a new pregnancy; for an existing row only if it already had a value. */
    fun placeAndDoctorRequired(isNew: Boolean, originalPlaceAndDoctor: String?): Boolean =
        isNew || !originalPlaceAndDoctor.isNullOrBlank()
}
