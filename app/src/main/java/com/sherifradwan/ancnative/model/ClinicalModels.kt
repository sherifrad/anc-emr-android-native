package com.sherifradwan.ancnative.model

import com.sherifradwan.ancnative.storage.DeletableRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val MEDICATION_STATUS_ACTIVE = "Active"
const val MEDICATION_STATUS_NON_COMPLIANT = "Patient non-compliant"
const val NORMAL_FETAL_NUMBER = "Singleton"
const val NORMAL_PRESENTATION = "Cephalic"
const val NORMAL_AMNIOTIC_FLUID = "Normal"

fun effectiveMedicationStatus(status: String, isNonCompliant: Boolean = false): String {
    return if (status == MEDICATION_STATUS_ACTIVE && isNonCompliant) {
        MEDICATION_STATUS_NON_COMPLIANT
    } else {
        status
    }
}

const val MEDICATION_STATUS_DISCONTINUED = "Discontinued"
const val MEDICATION_STATUS_COMPLETED = "Completed"

/** The only two statuses the UI shows. Stored values (Active, Completed, Discontinued, Patient non-compliant) are unchanged. */
const val MEDICATION_LABEL_ACTIVE = MEDICATION_STATUS_ACTIVE
const val MEDICATION_LABEL_STOPPED = "Stopped"

fun medicationStatusLabel(status: String, isNonCompliant: Boolean = false): String =
    if (isOngoingMedication(status, isNonCompliant)) MEDICATION_LABEL_ACTIVE else MEDICATION_LABEL_STOPPED

/** The instructions line of a medication: dose, route and the Note text, joined with " · ". Blank parts are left out, nothing is invented. */
fun medicationInstructions(dosage: String?, route: String?, notes: String?): String =
    listOfNotNull(dosage, route, notes).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")

/** The two choices the medication editor offers. */
enum class MedicationOnOff { ON, STOPPED }

/**
 * Which editor choice a stored (effective) status shows as: Active and Patient non-compliant are On,
 * Completed and Discontinued are Stopped. Blank counts as Active. Any other value maps to null (neither is shown selected).
 */
fun medicationEditorChoice(status: String): MedicationOnOff? = when (status.ifBlank { MEDICATION_STATUS_ACTIVE }) {
    MEDICATION_STATUS_ACTIVE, MEDICATION_STATUS_NON_COMPLIANT -> MedicationOnOff.ON
    MEDICATION_STATUS_COMPLETED, MEDICATION_STATUS_DISCONTINUED -> MedicationOnOff.STOPPED
    else -> null
}

/**
 * Status to store after the user taps a choice. Tapping the choice that already matches the stored status
 * keeps the stored value (Completed and Patient non-compliant are not overwritten); otherwise On stores Active
 * and Stopped stores Discontinued.
 */
fun medicationStatusAfterChoice(currentStatus: String, choice: MedicationOnOff): String {
    if (medicationEditorChoice(currentStatus) == choice) return currentStatus.ifBlank { MEDICATION_STATUS_ACTIVE }
    return if (choice == MedicationOnOff.ON) MEDICATION_STATUS_ACTIVE else MEDICATION_STATUS_DISCONTINUED
}

fun isOngoingMedication(status: String, isNonCompliant: Boolean = false): Boolean {
    val effective = effectiveMedicationStatus(status, isNonCompliant)
    return effective == MEDICATION_STATUS_ACTIVE || effective == MEDICATION_STATUS_NON_COMPLIANT
}

fun ultrasoundImpressionFromChoices(
    fetalNumber: String? = "",
    presentation: String? = "",
    placentalSite: String? = "",
    amnioticFluid: String? = "",
): String {
    val fn = fetalNumber?.trim().orEmpty()
    val fnPhrase = when {
        fn.equals("Singleton", ignoreCase = true) -> "singleton pregnancy"
        fn.equals("Twins", ignoreCase = true) -> "twin pregnancy"
        fn.equals("Higher order", ignoreCase = true) -> "higher-order multiple pregnancy"
        else -> null
    }

    val pres = presentation?.trim().orEmpty()
    val presPhrase = if (ClinicalVocabulary.fetalPresentations.any { it.equals(pres, ignoreCase = true) }) {
        "${pres.lowercase()} presentation"
    } else null

    val site = placentalSite?.trim().orEmpty()
    val sitePhrase = when {
        !ClinicalVocabulary.placentalSites.any { it.equals(site, ignoreCase = true) } -> null
        site.equals("Previa", ignoreCase = true) -> "placenta previa"
        else -> "${site.lowercase()} placenta"
    }

    val fluid = amnioticFluid?.trim().orEmpty()
    val fluidPhrase = when {
        !ClinicalVocabulary.amnioticFluids.any { it.equals(fluid, ignoreCase = true) } -> null
        fluid.equals("Normal", ignoreCase = true) -> "normal amniotic fluid"
        fluid.equals("Oligohydramnios", ignoreCase = true) -> "oligohydramnios"
        fluid.equals("Polyhydramnios", ignoreCase = true) -> "polyhydramnios"
        else -> null
    }

    val phrases = listOfNotNull(fnPhrase, presPhrase, sitePhrase, fluidPhrase)
    if (phrases.isEmpty()) return ""

    val joined = phrases.joinToString(", ")
    return joined.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + "."
}

fun mergeImpression(existing: String?, generated: String?): String {
    val ext = existing.orEmpty()
    val gen = generated.orEmpty()
    if (gen.isBlank()) return ext
    if (ext.isBlank()) return gen
    if (ext.contains(gen)) return ext
    return ext.trimEnd() + "\n" + gen
}

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
    override val id: String,
    override val patientId: String,
    val date: String,
    val testCategory: String,
    val testName: String,
    val result: String,
    val unit: String?,
    val referenceRange: String?,
    val isAbnormal: Boolean,
    val notes: String?,
    val labName: String? = null,
    override val revision: Long = 1,
) : DeletableRecord

data class DemoMedicationRecord(
    override val id: String,
    override val patientId: String,
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
    override val revision: Long = 1,
) : DeletableRecord

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
    val embryoAges = listOf(3, 5, 6)
    val fetalNumbers = setOf("Singleton", "Twins", "Higher order")
    val fetalPresentations = setOf("Cephalic", "Breech", "Transverse", "Variable")
    val placentalSites = setOf("Anterior", "Posterior", "Fundal", "Low-lying", "Previa")
    val amnioticFluids = setOf("Normal", "Oligohydramnios", "Polyhydramnios")
    val testCategories = setOf("Hematology", "Urinalysis", "Biochemistry", "Serology", "Other")
    val medicationRoutes = setOf("Oral", "IM", "IV", "Topical", "Subcutaneous")
    val medicationStatuses = listOf(MEDICATION_STATUS_ACTIVE, "Completed", "Discontinued", MEDICATION_STATUS_NON_COMPLIANT)
    val labNamePresets = listOf("Al Borg Labs", "El Mokhtabar", "Hospital Lab", "Alfa Labs")

    fun isValidIsoDate(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        return parseIsoDateOrNull(value) != null
    }
}
