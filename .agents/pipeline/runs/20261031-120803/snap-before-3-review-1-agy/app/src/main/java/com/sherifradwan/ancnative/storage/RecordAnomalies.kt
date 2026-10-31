package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.PatientVocabulary

internal data class RecordAnomaly(val patientId: String, val field: String, val value: String?)

internal fun findBlockingRecordProblems(snapshot: RecordSnapshot): List<String> = buildList {
    snapshot.patients.forEach { patient ->
        if (patient.revision < 1) add("Patient ${patient.id} has invalid revision")
        patient.visits.forEach { visit ->
            if (visit.revision < 1) add("Patient ${patient.id} has visit with invalid revision")
        }
        patient.previousPregnancies.forEach { pregnancy ->
            if (pregnancy.revision < 1) add("Patient ${patient.id} has previous pregnancy with invalid revision")
            if (pregnancy.patientId != patient.id) add("Patient ${patient.id} has previous pregnancy assigned to another patient")
            if (pregnancy.modelVersion != 3) add("Patient ${patient.id} has previous pregnancy with unsupported model version")
        }
    }
}

internal fun findRecordAnomalies(snapshot: RecordSnapshot): List<RecordAnomaly> = buildList {
    snapshot.patients.forEach { patient ->
        patient.status?.let { if (it !in PatientVocabulary.statuses) add(RecordAnomaly(patient.id, "status", it)) }
        patient.riskLevel?.let { if (it !in PatientVocabulary.riskLevels) add(RecordAnomaly(patient.id, "riskLevel", it)) }
        patient.bloodGroupText?.let { if (it !in PatientVocabulary.bloodGroups) add(RecordAnomaly(patient.id, "bloodGroupText", it)) }
        val gpa = listOf(patient.gravida, patient.parity, patient.abortion)
        if (gpa.any { it == null } && gpa.any { it != null } ||
            gpa.filterNotNull().any { it !in 0..PatientVocabulary.MAX_SAFE_GPA }
        ) {
            add(RecordAnomaly(patient.id, "gpa", gpa.joinToString("/") { it?.toString() ?: "null" }))
        }
        patient.previousPregnancies.forEach { pregnancy ->
            if (pregnancy.provenanceKind != PatientVocabulary.CLINICIAN_ENTRY) {
                add(RecordAnomaly(patient.id, "previousPregnancy.provenanceKind", pregnancy.provenanceKind))
            }
            if (pregnancy.clinicianId.isBlank()) {
                add(RecordAnomaly(patient.id, "previousPregnancy.clinicianId", pregnancy.clinicianId))
            }
        }
    }
}
