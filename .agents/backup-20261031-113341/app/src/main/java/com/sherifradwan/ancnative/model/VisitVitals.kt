package com.sherifradwan.ancnative.model

data class BloodPressure(val systolic: Int, val diastolic: Int)

enum class BpLevel { NORMAL, RAISED, SEVERE }

private val bloodPressureRegex = Regex("""^(\d{1,4})\s*/\s*(\d{1,4})(?:\s*mmhg)?$""", RegexOption.IGNORE_CASE)
private val weightRegex = Regex("""^(\d{1,4})(?:[.,](\d))?(?:\s*(?:kg|kilograms|kilogram))?$""", RegexOption.IGNORE_CASE)

fun parseBloodPressure(raw: String?): BloodPressure? {
    val match = bloodPressureRegex.matchEntire(raw?.trim().orEmpty()) ?: return null
    val systolic = match.groupValues[1].toIntOrNull() ?: return null
    val diastolic = match.groupValues[2].toIntOrNull() ?: return null
    return BloodPressure(systolic, diastolic)
}

fun parseWeightKg(raw: String?): Double? {
    val match = weightRegex.matchEntire(raw?.trim().orEmpty()) ?: return null
    return "${match.groupValues[1]}.${match.groupValues[2].ifEmpty { "0" }}".toDoubleOrNull()
}

fun isPlausibleBloodPressure(bp: BloodPressure): Boolean =
    bp.systolic in 60..260 && bp.diastolic in 30..160 && bp.systolic > bp.diastolic

fun isPlausibleWeightKg(kg: Double): Boolean = kg in 30.0..250.0

fun bpLevel(raw: String?): BpLevel {
    val bp = parseBloodPressure(raw) ?: return BpLevel.NORMAL
    return when {
        bp.systolic >= 160 || bp.diastolic >= 110 -> BpLevel.SEVERE
        bp.systolic >= 140 || bp.diastolic >= 90 -> BpLevel.RAISED
        else -> BpLevel.NORMAL
    }
}
