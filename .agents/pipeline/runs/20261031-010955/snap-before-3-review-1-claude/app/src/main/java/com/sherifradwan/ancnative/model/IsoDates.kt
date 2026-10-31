package com.sherifradwan.ancnative.model

import java.time.LocalDate

const val FULL_TERM_DAYS = 280L

fun eddFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(FULL_TERM_DAYS)

fun parseIsoDateOrNull(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        LocalDate.parse(value.trim())
    } catch (_: Exception) {
        null
    }
}

fun parseCanonicalIsoDateOrNull(value: String?): LocalDate? {
    if (value == null) return null
    return try {
        LocalDate.parse(value).takeIf { it.toString() == value }
    } catch (_: Exception) {
        null
    }
}
