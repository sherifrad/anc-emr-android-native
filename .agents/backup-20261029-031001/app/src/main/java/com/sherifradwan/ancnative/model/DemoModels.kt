package com.sherifradwan.ancnative.model

data class DemoPatient(
    val id: String,
    val caseId: String,
    val displayName: String,
    val dateOfBirth: String?,
    val phone: String?,
    val visits: List<DemoVisit>,
    val createdAt: String? = null,
    val status: String? = null,
    val archivedAt: String? = null,
    val archivedReason: String? = null,
    val riskLevel: String? = null,
    val ageText: String? = null,
    val addressText: String? = null,
    val bloodGroupText: String? = null,
    val medicalHistory: String? = null,
    val surgicalHistory: String? = null,
    val familyHistory: String? = null,
    val allergyHistory: String? = null,
    val gravida: Long? = null,
    val parity: Long? = null,
    val abortion: Long? = null,
    val obstetricNotes: String? = null,
    val clinicalAttention: String? = null,
    val revision: Long = 1,
    val previousPregnancies: List<DemoPreviousPregnancy> = emptyList(),
    val dating: DemoPatientDating? = null,
    val ultrasoundScans: List<DemoUltrasoundScan> = emptyList(),
    val labInvestigations: List<DemoLabInvestigation> = emptyList(),
    val medications: List<DemoMedicationRecord> = emptyList(),
    val attachments: List<DemoAttachment> = emptyList(),
)

data class DemoVisit(
    val id: String,
    val date: String,
    val bloodPressure: String?,
    val weight: String?,
    val findings: String?,
    val notes: String?,
    val revision: Int,
    val isSyntheticEdit: Boolean,
    val linkedUltrasoundId: String? = null,
)

data class DemoPreviousPregnancy(
    val id: String,
    val patientId: String,
    val modelVersion: Int,
    val dateText: String?,
    val outcomeText: String?,
    val pregnancyType: String?,
    val otherDetails: String?,
    val placeAndDoctor: String?,
    val deliveryBy: String?,
    val outcome: String?,
    val birthMethod: String?,
    val lossManagement: String?,
    val terminationMethod: String?,
    val provenanceKind: String,
    val clinicianId: String,
    val recordedAt: String,
    val updatedAt: String,
    val revision: Long,
)

object PatientVocabulary {
    val bloodGroups = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val riskLevels = setOf("Low Risk", "Moderate Risk", "High Risk")
    val statuses = setOf("active", "archived")
    val pregnancyTypes = setOf("Singleton", "Twin", "Other multiple")
    val deliveryModes = setOf("CS", "Vaginal", "Hysterotomy")
    val pregnancyOutcomes = setOf(
        "LiveBirth",
        "Stillbirth",
        "EarlyPregnancyLoss",
        "TerminationOfPregnancy",
    )
    val birthMethods = setOf("Vaginal", "Cesarean")
    val lossManagementMethods = setOf("Expectant", "Medical", "Surgical")
    val terminationMethods = setOf("Medical", "Surgical", "Hysterotomy")

    const val CLINICIAN_ENTRY = "CLINICIAN_ENTRY"
    const val MAX_SAFE_GPA = 9_007_199_254_740_991L
}

enum class PatientValidationError {
    FULL_NAME_REQUIRED,
    CASE_ID_REQUIRED,
    CASE_ID_CONFLICT,
    INVALID_BLOOD_GROUP,
    INVALID_RISK_LEVEL,
    INVALID_STATUS,
    GPA_ALL_OR_NONE,
    INVALID_GPA,
    ARCHIVE_REASON_REQUIRED,
    CLINICIAN_ID_REQUIRED,
    INVALID_PREGNANCY_TYPE,
    INVALID_DELIVERY_MODE,
    INVALID_PREGNANCY_OUTCOME,
    INVALID_BIRTH_METHOD,
    INVALID_LOSS_MANAGEMENT,
    INVALID_TERMINATION_METHOD,
    OUTCOME_METHOD_MISMATCH,
    STALE_REVISION,
    SAVE_FAILED,
}

data class PatientClinicalInput(
    val caseId: String,
    val displayName: String,
    val dateOfBirth: String?,
    val phone: String?,
    val ageText: String?,
    val addressText: String?,
    val bloodGroupText: String?,
    val riskLevel: String?,
    val gravida: Long?,
    val parity: Long?,
    val abortion: Long?,
    val obstetricNotes: String?,
)

fun PatientClinicalInput.validationErrors(): Set<PatientValidationError> = buildSet {
    if (displayName.isBlank() || displayName != displayName.trim()) add(PatientValidationError.FULL_NAME_REQUIRED)
    if (caseId.isBlank() || caseId != caseId.trim()) add(PatientValidationError.CASE_ID_REQUIRED)
    if (bloodGroupText != null && bloodGroupText !in PatientVocabulary.bloodGroups) {
        add(PatientValidationError.INVALID_BLOOD_GROUP)
    }
    if (riskLevel != null && riskLevel !in PatientVocabulary.riskLevels) {
        add(PatientValidationError.INVALID_RISK_LEVEL)
    }
    val gpa = listOf(gravida, parity, abortion)
    if (gpa.any { it == null } && gpa.any { it != null }) add(PatientValidationError.GPA_ALL_OR_NONE)
    if (gpa.filterNotNull().any { it < 0 || it > PatientVocabulary.MAX_SAFE_GPA }) {
        add(PatientValidationError.INVALID_GPA)
    }
}

data class PreviousPregnancyClinicalInput(
    val pregnancyType: String?,
    val deliveryBy: String?,
    val outcome: String?,
    val birthMethod: String?,
    val lossManagement: String?,
    val terminationMethod: String?,
    val provenanceKind: String,
    val clinicianId: String,
)

fun PreviousPregnancyClinicalInput.validationErrors(): Set<PatientValidationError> = buildSet {
    if (pregnancyType != null && pregnancyType !in PatientVocabulary.pregnancyTypes) {
        add(PatientValidationError.INVALID_PREGNANCY_TYPE)
    }
    if (deliveryBy != null && deliveryBy !in PatientVocabulary.deliveryModes) {
        add(PatientValidationError.INVALID_DELIVERY_MODE)
    }
    if (outcome != null && outcome !in PatientVocabulary.pregnancyOutcomes) {
        add(PatientValidationError.INVALID_PREGNANCY_OUTCOME)
    }
    if (birthMethod != null && birthMethod !in PatientVocabulary.birthMethods) {
        add(PatientValidationError.INVALID_BIRTH_METHOD)
    }
    if (lossManagement != null && lossManagement !in PatientVocabulary.lossManagementMethods) {
        add(PatientValidationError.INVALID_LOSS_MANAGEMENT)
    }
    if (terminationMethod != null && terminationMethod !in PatientVocabulary.terminationMethods) {
        add(PatientValidationError.INVALID_TERMINATION_METHOD)
    }
    if (provenanceKind != PatientVocabulary.CLINICIAN_ENTRY || clinicianId.isBlank()) {
        add(PatientValidationError.CLINICIAN_ID_REQUIRED)
    }

    val birthOutcome = outcome == "LiveBirth" || outcome == "Stillbirth"
    val lossOutcome = outcome == "EarlyPregnancyLoss"
    val terminationOutcome = outcome == "TerminationOfPregnancy"
    if (
        (!birthOutcome && birthMethod != null) ||
        (!lossOutcome && lossManagement != null) ||
        (!terminationOutcome && terminationMethod != null) ||
        (birthOutcome && (lossManagement != null || terminationMethod != null)) ||
        (lossOutcome && (birthMethod != null || terminationMethod != null)) ||
        (terminationOutcome && (birthMethod != null || lossManagement != null))
    ) {
        add(PatientValidationError.OUTCOME_METHOD_MISMATCH)
    }
}
