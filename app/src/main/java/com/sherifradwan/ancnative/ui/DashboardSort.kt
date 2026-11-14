package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import java.time.Instant
import java.time.LocalDate

enum class DashboardSort {
    NEWEST_ADDED,
    RECENTLY_SEEN,
    NAME,
    DUE_DATE,
}

private fun parseCreatedAtOrNull(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return try {
        Instant.parse(raw)
    } catch (_: Exception) {
        null
    }
}

fun sortDashboardPatients(
    patients: List<DemoPatient>,
    sort: DashboardSort,
    today: LocalDate = LocalDate.now(),
): List<DemoPatient> {
    if (patients.isEmpty()) return emptyList()
    return when (sort) {
        DashboardSort.NEWEST_ADDED -> {
            val (dated, undated) = patients.partition { parseCreatedAtOrNull(it.createdAt) != null }
            dated.sortedByDescending { parseCreatedAtOrNull(it.createdAt) } + undated
        }
        DashboardSort.RECENTLY_SEEN -> {
            val preSorted = sortDashboardPatients(patients, DashboardSort.NEWEST_ADDED, today)
            val withVisit = mutableListOf<Pair<DemoPatient, LocalDate>>()
            val withoutVisit = mutableListOf<DemoPatient>()
            for (patient in preSorted) {
                val latestVisitDate = patient.visits.mapNotNull { parseIsoDateOrNull(it.date) }.maxOrNull()
                if (latestVisitDate != null) {
                    withVisit.add(patient to latestVisitDate)
                } else {
                    withoutVisit.add(patient)
                }
            }
            withVisit.sortedByDescending { it.second }.map { it.first } + withoutVisit
        }
        DashboardSort.NAME -> {
            val (named, blank) = patients.partition { it.displayName.isNotBlank() }
            named.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName.trim() }) + blank
        }
        DashboardSort.DUE_DATE -> {
            val preSorted = sortDashboardPatients(patients, DashboardSort.NEWEST_ADDED, today)
            val withEdd = mutableListOf<Pair<DemoPatient, LocalDate>>()
            val withoutEdd = mutableListOf<DemoPatient>()
            for (patient in preSorted) {
                val edd = datingSummaryOf(patient, today).edd
                if (edd != null) {
                    withEdd.add(patient to edd)
                } else {
                    withoutEdd.add(patient)
                }
            }
            withEdd.sortedBy { it.second }.map { it.first } + withoutEdd
        }
    }
}
