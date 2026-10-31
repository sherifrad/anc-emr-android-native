package com.sherifradwan.ancnative.model

object ClinicalSummaryGenerator {
    fun generateTextSummary(patient: DemoPatient): String {
        val sb = StringBuilder()
        sb.appendLine("=== ANTENATAL CARE CLINICAL SUMMARY ===")
        sb.appendLine("Patient: ${patient.displayName}")
        sb.appendLine("Case ID: ${patient.caseId}")
        if (!patient.dateOfBirth.isNullOrBlank()) sb.appendLine("DOB: ${patient.dateOfBirth}")
        if (!patient.phone.isNullOrBlank()) sb.appendLine("Phone: ${patient.phone}")
        if (!patient.bloodGroupText.isNullOrBlank()) sb.appendLine("Blood Group/Rh: ${patient.bloodGroupText}")
        if (!patient.riskLevel.isNullOrBlank()) sb.appendLine("Risk Level: ${patient.riskLevel.uppercase()}")
        val g = patient.gravida ?: "-"
        val p = patient.parity ?: "-"
        val a = patient.abortion ?: "-"
        sb.appendLine("Obstetric Profile: Gravida $g · Parity $p · Abortion $a")
        sb.appendLine()

        val dating = patient.dating
        sb.appendLine("--- PREGNANCY DATING ---")
        if (dating != null) {
            val ga = dating.calculateGestationalAge() ?: "Not calculated"
            sb.appendLine("Gestational Age: $ga")
            if (!dating.agreedEdd.isNullOrBlank()) sb.appendLine("Agreed EDD: ${dating.agreedEdd}")
            if (!dating.lmpDate.isNullOrBlank()) sb.appendLine("LMP: ${dating.lmpDate}")
            if (!dating.ultrasoundEdd.isNullOrBlank()) sb.appendLine("Ultrasound EDD: ${dating.ultrasoundEdd}")
            if (!dating.datingMethod.isNullOrBlank()) sb.appendLine("Method: ${dating.datingMethod}")
        } else {
            sb.appendLine("Dating: Not recorded")
        }
        sb.appendLine()

        sb.appendLine("--- RECENT VISITS (${patient.visits.size}) ---")
        if (patient.visits.isEmpty()) {
            sb.appendLine("No visits recorded.")
        } else {
            patient.visits.sortedByDescending { it.date }.forEach { v ->
                sb.append("• ${v.date}")
                if (!v.bloodPressure.isNullOrBlank()) sb.append(" | BP: ${v.bloodPressure}")
                if (!v.weight.isNullOrBlank()) sb.append(" | Wt: ${v.weight}")
                if (!v.findings.isNullOrBlank()) sb.append(" | Findings: ${v.findings}")
                if (!v.notes.isNullOrBlank()) sb.append(" | Notes: ${v.notes}")
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("--- ULTRASOUND SCANS (${patient.ultrasoundScans.size}) ---")
        if (patient.ultrasoundScans.isEmpty()) {
            sb.appendLine("No scans recorded.")
        } else {
            patient.ultrasoundScans.sortedByDescending { it.date }.forEach { s ->
                sb.append("• ${s.date}")
                val gaStr = when {
                    s.gestationalAgeWeeks != null && s.gestationalAgeDays != null -> "${s.gestationalAgeWeeks}w ${s.gestationalAgeDays}d"
                    s.gestationalAgeWeeks != null -> "${s.gestationalAgeWeeks}w"
                    else -> null
                }
                if (gaStr != null) sb.append(" | GA: $gaStr")
                if (!s.presentation.isNullOrBlank()) sb.append(" | Pres: ${s.presentation}")
                if (!s.placentalSite.isNullOrBlank()) sb.append(" | Placenta: ${s.placentalSite}")
                if (!s.fetalHeartRate.isNullOrBlank()) sb.append(" | FHR: ${s.fetalHeartRate}")
                if (!s.estimatedFetalWeight.isNullOrBlank()) sb.append(" | EFW: ${s.estimatedFetalWeight}")
                if (!s.impression.isNullOrBlank()) sb.append(" | Impression: ${s.impression}")
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("--- LABORATORY INVESTIGATIONS (${patient.labInvestigations.size}) ---")
        if (patient.labInvestigations.isEmpty()) {
            sb.appendLine("No laboratory investigations recorded.")
        } else {
            patient.labInvestigations.sortedByDescending { it.date }.forEach { l ->
                val flag = if (l.isAbnormal) "[ABNORMAL] " else ""
                sb.append("• $flag${l.testName}: ${l.result}")
                if (!l.unit.isNullOrBlank()) sb.append(" ${l.unit}")
                if (!l.referenceRange.isNullOrBlank()) sb.append(" (Ref: ${l.referenceRange})")
                sb.append(" [${l.date}]")
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("--- MEDICATIONS & PRESCRIPTIONS (${patient.medications.size}) ---")
        if (patient.medications.isEmpty()) {
            sb.appendLine("No medications recorded.")
        } else {
            patient.medications.forEach { m ->
                val kind = if (m.isProphylaxis) "[Prophylaxis] " else "[Therapeutic] "
                sb.append("• $kind${m.name} ${m.dosage}")
                if (!m.route.isNullOrBlank()) sb.append(" via ${m.route}")
                if (!m.frequency.isNullOrBlank()) sb.append(" (${m.frequency})")
                sb.append(" - Status: ${m.status}")
                sb.appendLine()
            }
        }
        sb.appendLine()
        sb.appendLine("Generated by ANC Native EMR · Synthetic Clinical Prototype")
        return sb.toString()
    }
}
