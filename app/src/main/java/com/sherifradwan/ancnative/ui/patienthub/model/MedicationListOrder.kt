package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoMedicationRecord

/** Rows already shown keep their order; rows new since [shownIds] join the end; removed rows drop out. */
fun stableMedicationOrder(shownIds: List<String>, meds: List<DemoMedicationRecord>): List<DemoMedicationRecord> {
    val byId = meds.associateBy { it.id }
    val kept = shownIds.mapNotNull { byId[it] }
    val keptIds = kept.mapTo(HashSet()) { it.id }
    return kept + meds.filter { it.id !in keptIds }
}
