package com.sherifradwan.ancnative.model

data class PreviousDeliveryCounts(
    val cesarean: Int,
    val vaginal: Int,
    val stillbirth: Int,
    val earlyLoss: Int,
)

private val CS_WORD_REGEX = Regex("""\b(?:CS|Cesarean)\b""", RegexOption.IGNORE_CASE)
private val VAGINAL_WORD_REGEX = Regex("""\b(?:SVD|Vaginal)\b""", RegexOption.IGNORE_CASE)

fun isStillbirth(pregnancy: DemoPreviousPregnancy): Boolean {
    val text = pregnancy.outcomeText
    return pregnancy.outcome == "Stillbirth" ||
        (text != null && (text.contains("Stillbirth", ignoreCase = true) || text.contains("IUFD", ignoreCase = true)))
}

fun isEarlyLoss(pregnancy: DemoPreviousPregnancy): Boolean {
    val text = pregnancy.outcomeText
    return pregnancy.outcome == "EarlyPregnancyLoss" ||
        (text != null && (text.contains("Miscarriage", ignoreCase = true) || text.contains("Loss", ignoreCase = true)))
}

fun previousDeliveryCounts(pregnancies: List<DemoPreviousPregnancy>): PreviousDeliveryCounts {
    var cesarean = 0
    var vaginal = 0
    var stillbirth = 0
    var earlyLoss = 0

    for (p in pregnancies) {
        val bm = p.birthMethod?.trim()
        val db = p.deliveryBy?.trim()

        val isStructCs = bm.equals("Cesarean", ignoreCase = true) || db.equals("CS", ignoreCase = true)
        val isStructVag = bm.equals("Vaginal", ignoreCase = true) || db.equals("Vaginal", ignoreCase = true)

        if (isStructCs) {
            cesarean++
        } else if (isStructVag) {
            vaginal++
        } else if (bm.isNullOrBlank() && db.isNullOrBlank()) {
            val text = p.outcomeText
            if (text != null) {
                if (CS_WORD_REGEX.containsMatchIn(text)) {
                    cesarean++
                } else if (VAGINAL_WORD_REGEX.containsMatchIn(text)) {
                    vaginal++
                }
            }
        }

        if (isStillbirth(p)) {
            stillbirth++
        }

        if (isEarlyLoss(p)) {
            earlyLoss++
        }
    }

    return PreviousDeliveryCounts(
        cesarean = cesarean,
        vaginal = vaginal,
        stillbirth = stillbirth,
        earlyLoss = earlyLoss,
    )
}

fun previousDeliverySummary(patient: DemoPatient): String {
    val g = patient.gravida ?: 1L
    val p = patient.parity ?: 0L
    val a = patient.abortion ?: 0L
    val pregnancies = patient.previousPregnancies
    val c = previousDeliveryCounts(pregnancies)

    return when {
        g == 1L && p == 0L && a == 0L && pregnancies.isEmpty() -> "Primigravida"
        p == 0L && pregnancies.isEmpty() -> "P0 · No prior deliveries"
        c.cesarean > 0 || c.vaginal > 0 -> {
            val parts = mutableListOf<String>()
            if (c.cesarean > 0) parts.add("${c.cesarean} CS")
            if (c.vaginal > 0) parts.add("${c.vaginal} SVD")
            val routes = parts.joinToString(", ")
            val iufd = if (c.stillbirth > 0) " · ${c.stillbirth} IUFD" else ""
            "Prev $routes$iufd"
        }
        c.stillbirth > 0 -> "Prior Stillbirth (IUFD)"
        c.earlyLoss > 0 -> "${c.earlyLoss} Prior Miscarriages"
        p > 0L -> "Parous (P$p)"
        pregnancies.isNotEmpty() -> "${pregnancies.size} Prior Pregnancies"
        else -> "No prior deliveries"
    }
}
