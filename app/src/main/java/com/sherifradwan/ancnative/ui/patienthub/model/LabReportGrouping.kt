package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoLabInvestigation

data class ClinicalLabReport(
    val date: String,
    val labName: String?,
    val items: List<DemoLabInvestigation>,
) {
    val totalCount: Int get() = items.size
    val abnormalCount: Int get() = items.count { it.isAbnormal }
    val isAnyAbnormal: Boolean get() = abnormalCount > 0
}

data class CompositeTestGroup(
    val title: String,
    val isComposite: Boolean,
    val items: List<DemoLabInvestigation>,
) {
    val abnormalCount: Int get() = items.count { it.isAbnormal }
    val isAnyAbnormal: Boolean get() = abnormalCount > 0
    val summaryPillText: String
        get() {
            val abnormalItem = items.firstOrNull { it.isAbnormal }
            return if (abnormalItem != null) {
                "${abnormalItem.testName} ${abnormalItem.result} [ABNORMAL]"
            } else {
                "ALL NORMAL ✓"
            }
        }
}

private val CBC_KEYWORDS = listOf("hemoglobin", "platelet", "wbc", "white blood", "rbc", "red blood", "hematocrit", "hct", "mcv", "mch", "mchc")
private val URINE_KEYWORDS = listOf("urine", "pus cells", "nitrite", "leukocyte", "protein")
private val STOOL_KEYWORDS = listOf("stool", "occult blood", "ova", "parasite")
private val LFT_KEYWORDS = listOf("alt", "ast", "sgpt", "sgot", "bilirubin", "alkaline phosphatase")
private val KFT_KEYWORDS = listOf("creatinine", "urea", "uric acid", "bun")

fun partitionReportIntoCompositeGroups(
    items: List<DemoLabInvestigation>,
): List<CompositeTestGroup> {
    val cbcItems = mutableListOf<DemoLabInvestigation>()
    val urineItems = mutableListOf<DemoLabInvestigation>()
    val stoolItems = mutableListOf<DemoLabInvestigation>()
    val lftItems = mutableListOf<DemoLabInvestigation>()
    val kftItems = mutableListOf<DemoLabInvestigation>()
    val standaloneItems = mutableListOf<DemoLabInvestigation>()

    items.forEach { lab ->
        val nameLower = lab.testName.lowercase()
        val catLower = lab.testCategory.lowercase()
        when {
            CBC_KEYWORDS.any { nameLower.contains(it) } -> cbcItems.add(lab)
            catLower.contains("urine") || URINE_KEYWORDS.any { nameLower.contains(it) } -> urineItems.add(lab)
            catLower.contains("stool") || STOOL_KEYWORDS.any { nameLower.contains(it) } -> stoolItems.add(lab)
            LFT_KEYWORDS.any { nameLower.contains(it) } -> lftItems.add(lab)
            KFT_KEYWORDS.any { nameLower.contains(it) } -> kftItems.add(lab)
            else -> standaloneItems.add(lab)
        }
    }

    val groups = mutableListOf<CompositeTestGroup>()
    if (cbcItems.size >= 2) {
        groups.add(CompositeTestGroup("Complete Blood Count (CBC)", isComposite = true, cbcItems))
    } else {
        standaloneItems.addAll(cbcItems)
    }

    if (urineItems.size >= 2) {
        groups.add(CompositeTestGroup("Complete Urine Analysis", isComposite = true, urineItems))
    } else {
        standaloneItems.addAll(urineItems)
    }

    if (stoolItems.size >= 2) {
        groups.add(CompositeTestGroup("Stool Analysis", isComposite = true, stoolItems))
    } else {
        standaloneItems.addAll(stoolItems)
    }

    if (lftItems.size >= 2) {
        groups.add(CompositeTestGroup("Liver Function Panel (LFT)", isComposite = true, lftItems))
    } else {
        standaloneItems.addAll(lftItems)
    }

    if (kftItems.size >= 2) {
        groups.add(CompositeTestGroup("Renal Function Panel (KFT)", isComposite = true, kftItems))
    } else {
        standaloneItems.addAll(kftItems)
    }

    if (standaloneItems.isNotEmpty()) {
        groups.add(CompositeTestGroup("Individual Investigations", isComposite = false, standaloneItems))
    }

    return groups
}

fun groupLabsIntoReports(
    labs: List<DemoLabInvestigation>,
): List<ClinicalLabReport> {
    return labs
        .groupBy { it.date to it.labName }
        .map { (key, groupLabs) ->
            ClinicalLabReport(
                date = key.first,
                labName = key.second,
                items = groupLabs,
            )
        }
        .sortedByDescending { it.date }
}
