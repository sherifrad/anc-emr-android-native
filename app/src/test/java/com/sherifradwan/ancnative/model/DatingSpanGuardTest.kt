package com.sherifradwan.ancnative.model

import com.sherifradwan.ancnative.ui.report.PatientPrintHtmlGenerator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D6b and D6c: extreme dates never throw and never print a wrapped gap; the 560 day span is the exact limit. */
class DatingSpanGuardTest {
    private val anchor = LocalDate.of(2026, 6, 1)
    private val spanLimit = 560L

    private fun dating(lmp: String?, scanEdd: String?, agreed: String?, method: String? = "LMP") =
        DemoPatientDating(
            patientId = "p1", lmpDate = lmp, ultrasoundEdd = scanEdd, agreedEdd = agreed,
            datingMethod = method, notes = null,
        )

    private fun patient(d: DemoPatientDating) = DemoPatient(
        id = "p1", caseId = "C1", displayName = "Span Test", dateOfBirth = null, phone = null,
        visits = emptyList(), dating = d, ultrasoundScans = emptyList(),
    )

    private fun discordance(d: DemoPatientDating) = datingSummary(d, anchor, anchor).discordance

    private val extremeLmp = "+999999999-12-31"

    @Test
    fun extremeLmpWithScanEddNeverThrowsInTextPrintOrShare() {
        for (agreed in listOf(null, "", "2026-12-01")) {
            val d = dating(extremeLmp, "2026-12-08", agreed)
            val summary = datingSummary(d, anchor, anchor)
            assertNull("agreed=$agreed", summary.discordance)
            assertNull("agreed=$agreed", datingDiscordanceLine(d, summary))
            val p = patient(d)
            val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = p, asOfDate = anchor)
            assertFalse(html, html.contains("gap "))
            val share = ClinicalSummaryGenerator.generateTextSummary(p)
            assertFalse(share, share.contains("gap "))
        }
    }

    @Test
    fun discordanceLineSurvivesAnLmpEddThatOverflows() {
        val d = dating(extremeLmp, "2026-12-08", null)
        val forced = RedatingAdvice(differenceDays = 9, thresholdDays = 7)
        val summary = DatingSummary(DatingOrigin.None, null, EddLabel.PLAIN, "LMP", forced)
        assertNull(datingDiscordanceLine(d, summary))
    }

    @Test
    fun scanEddAtExactlyTheSpanLimitIsAcceptedAndOneMoreDayIsNot() {
        val lmp = anchor.minusDays(80)
        fun at(days: Long) = discordance(dating(lmp.toString(), anchor.plusDays(days).toString(), null))
        assertNotNull(at(spanLimit))
        assertNull(at(spanLimit + 1))
        assertNotNull(at(-spanLimit))
        assertNull(at(-spanLimit - 1))
    }

    @Test
    fun lmpAtExactlyTheSpanLimitIsAcceptedAndOneMoreDayIsNot() {
        val scanEdd = anchor.plusDays(100).toString()
        fun at(days: Long) = discordance(dating(anchor.minusDays(days).toString(), scanEdd, null))
        assertNotNull(at(spanLimit))
        assertNull(at(spanLimit + 1))
        assertNotNull(at(-spanLimit))
        assertNull(at(-spanLimit - 1))
    }

    @Test
    fun redatingAdviceNeverWrapsToANegativeOrTruncatedGap() {
        assertNull(redatingAdvice(0L, Int.MAX_VALUE.toLong() * 2))
        assertNull(redatingAdvice(Int.MAX_VALUE.toLong() * 2, 0L))
        assertEquals(9, redatingAdvice(70L, 79L)?.differenceDays)
        assertTrue(redatingAdvice(0L, 4_294_967_296L + 9L) == null)
    }
}
