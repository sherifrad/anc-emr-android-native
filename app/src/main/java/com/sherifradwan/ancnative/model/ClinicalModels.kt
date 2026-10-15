package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DemoPatientDating(
    val patientId: String,
    val lmpDate: String?,
    val ultrasoundEdd: String?,
    val agreedEdd: String?,
    val datingMethod: String?,
    val notes: String?,
    val transferDate: String? = null,
    val embryoAgeDays: Int? = null,
    val revision: Long = 1,
) {
    fun calculateGestationalAge(asOfDate: LocalDate = LocalDate.now()): String? {
        return GestationalAgeEngine.calculate(this, asOfDate)?.formatted
    }

    fun calculateGestationalAgeResult(asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        return GestationalAgeEngine.calculate(this, asOfDate)
    }

    fun calculateEstimatedDueDate(): LocalDate? {
        return GestationalAgeEngine.calculateEstimatedDueDate(this)
    }
}

data class DemoUltrasoundScan(
    val id: String,
    val patientId: String,
    val date: String,
    val gestationalAgeWeeks: Int?,
    val gestationalAgeDays: Int?,
    val fetalNumber: String?,
    val presentation: String?,
    val placentalSite: String?,
    val amnioticFluid: String?,
    val fetalHeartRate: String?,
    val biometryBpd: String?,
    val biometryHc: String?,
    val biometryAc: String?,
    val biometryFl: String?,
    val estimatedFetalWeight: String?,
    val impression: String?,
    val linkedVisitId: String? = null,
    val imageUris: List<String> = emptyList(),
    val revision: Long = 1,
)

data class DemoLabInvestigation(
    val id: String,
    val patientId: String,
    val date: String,
    val testCategory: String,
    val testName: String,
    val result: String,
    val unit: String?,
    val referenceRange: String?,
    val isAbnormal: Boolean,
    val notes: String?,
    val labName: String? = null,
    val revision: Long = 1,
)

data class DemoMedicationRecord(
    val id: String,
    val patientId: String,
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    val startDate: String,
    val endDate: String?,
    val status: String,
    val notes: String?,
    val isProphylaxis: Boolean,
    val isNonCompliant: Boolean = false,
    val adverseNotes: String? = null,
    val revision: Long = 1,
)

data class DemoAttachment(
    val id: String,
    val patientId: String,
    val fileName: String,
    val filePath: String,
    val category: String,
    val mimeType: String = "image/webp",
    val fileSizeBytes: Long,
    val sha256Hash: String,
    val title: String?,
    val capturedAt: String,
    val linkedRecordId: String? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val cloudUrl: String? = null,
    val cloudSyncedAt: String? = null,
    val revision: Long = 1,
)

object ClinicalVocabulary {
    val datingMethods = setOf("LMP", "Ultrasound", "IVF_TRANSFER", "Clinical")
    val embryoAges = listOf(3, 5)
    val fetalNumbers = setOf("Singleton", "Twins", "Higher order")
    val fetalPresentations = setOf("Cephalic", "Breech", "Transverse", "Variable")
    val placentalSites = setOf("Anterior", "Posterior", "Fundal", "Low-lying", "Previa")
    val amnioticFluids = setOf("Normal", "Oligohydramnios", "Polyhydramnios")
    val testCategories = setOf("Hematology", "Urinalysis", "Biochemistry", "Serology", "Other")
    val medicationRoutes = setOf("Oral", "IM", "IV", "Topical", "Subcutaneous")
    val medicationStatuses = setOf("Active", "Completed", "Discontinued")

    fun isValidIsoDate(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        return try {
            LocalDate.parse(value.trim())
            true
        } catch (_: Exception) {
            false
        }
    }
}
