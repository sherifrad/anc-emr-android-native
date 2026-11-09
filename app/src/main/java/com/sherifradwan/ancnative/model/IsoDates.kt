package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val FULL_TERM_DAYS = 280L

fun eddFromLmp(lmp: LocalDate): LocalDate = lmp.plusDays(FULL_TERM_DAYS)

fun eddFromScan(scanDate: LocalDate, gaWeeks: Int, gaDays: Int): LocalDate? {
    if (gaWeeks !in 0..42 || gaDays !in 0..6) return null
    return scanDate.plusDays(FULL_TERM_DAYS - (gaWeeks * 7L + gaDays))
}

fun ivfGaDaysAtTransfer(embryoAgeDays: Int): Long = 14L + embryoAgeDays

fun ivfEddFromTransfer(transfer: LocalDate, embryoAgeDays: Int): LocalDate =
    transfer.plusDays(FULL_TERM_DAYS - ivfGaDaysAtTransfer(embryoAgeDays))

fun formatIsoDate(date: LocalDate): String = date.toString()

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

private val DmyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)

fun formatDmy(date: LocalDate): String = date.format(DmyFormatter)

fun formatDmy(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return parseIsoDateOrNull(iso)?.let(::formatDmy) ?: iso.trim()
}
