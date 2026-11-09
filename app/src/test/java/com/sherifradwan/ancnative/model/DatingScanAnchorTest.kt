package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * D2a: the discordance anchor is the earliest scan dated on or after the LMP and not after today.
 * Every case uses a 8 d gap between the LMP and scan due dates, which is an alert at dating GA
 * 63 to 111 d (limit 7 d) and below, and no alert at GA 112 to 153 d (limit 10 d) and above.
 * The anchor therefore decides the outcome.
 */
class DatingScanAnchorTest {
    private val lmp = LocalDate.of(2026, 1, 1)
    private val lmpEdd = lmp.plusDays(FULL_TERM_DAYS)
    private val gapDays = 8L

    private val gaAlertDay = lmp.plusDays(70)
    private val gaNoAlertDay = lmp.plusDays(130)

    private fun scan(date: String) = DemoUltrasoundScan(
        id = "s-$date", patientId = "p1", date = date,
        gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
        presentation = null, placentalSite = null, amnioticFluid = null,
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = null, impression = null,
    )

    private fun patient(scanDates: List<String>) = DemoPatient(
        id = "p1", caseId = "C1", displayName = "Anchor", dateOfBirth = null, phone = null,
        visits = emptyList(),
        dating = DemoPatientDating(
            patientId = "p1",
            lmpDate = lmp.toString(),
            ultrasoundEdd = lmpEdd.plusDays(gapDays).toString(),
            agreedEdd = lmpEdd.toString(),
            datingMethod = "LMP",
            notes = null,
        ),
        ultrasoundScans = scanDates.map(::scan),
    )

    private fun alert(scanDates: List<String>, today: LocalDate): Boolean =
        datingSummaryOf(patient(scanDates), today).discordance != null

    @Test
    fun earliestOfUnsortedScansIsTheAnchor() {
        val scans = listOf(gaNoAlertDay, gaAlertDay.plusDays(30), gaAlertDay, gaAlertDay.plusDays(10))
            .map(LocalDate::toString)
        assertEquals(true, alert(scans, today = lmp.plusDays(200)))
    }

    @Test
    fun earliestIsTheAnchorWhateverTheListOrder() {
        val early = gaAlertDay.toString()
        val late = gaNoAlertDay.toString()
        assertEquals(true, alert(listOf(early, late), today = lmp.plusDays(200)))
        assertEquals(true, alert(listOf(late, early), today = lmp.plusDays(200)))
    }

    @Test
    fun laterScansDoNotMoveTheAnchorWhenTheEarliestIsPastTheLimit() {
        val scans = listOf(gaNoAlertDay, gaNoAlertDay.plusDays(20)).map(LocalDate::toString)
        assertEquals(false, alert(scans, today = gaAlertDay.plusDays(100)))
    }

    @Test
    fun noScansMeasuresAtToday() {
        assertEquals(true, alert(emptyList(), today = gaAlertDay))
        assertEquals(false, alert(emptyList(), today = gaNoAlertDay))
    }

    @Test
    fun malformedScanDatesAreIgnored() {
        val scans = listOf("garbage", "", "  ", "2026-13-45", gaAlertDay.toString(), "31/03/2026")
        assertEquals(true, alert(scans, today = gaNoAlertDay))
    }

    @Test
    fun onlyInvalidScansFallBackToToday() {
        val invalid = listOf("garbage", "", "2026-02-30")
        assertEquals(true, alert(invalid, today = gaAlertDay))
        assertEquals(false, alert(invalid, today = gaNoAlertDay))
    }

    @Test
    fun scanDatedBeforeTheLmpIsIgnored() {
        val before = lmp.minusDays(30).toString()
        assertEquals(false, alert(listOf(before), today = gaNoAlertDay))
        assertEquals(true, alert(listOf(before), today = gaAlertDay))
    }

    @Test
    fun scanBeforeTheLmpDoesNotHideALaterValidScan() {
        val scans = listOf(lmp.minusDays(30).toString(), gaAlertDay.toString())
        assertEquals(true, alert(scans, today = gaNoAlertDay))
    }

    @Test
    fun scanOnTheLmpDateIsAccepted() {
        assertEquals(true, alert(listOf(lmp.toString()), today = gaNoAlertDay))
    }

    @Test
    fun scanDatedAfterTodayIsIgnored() {
        val future = gaNoAlertDay.toString()
        assertEquals(true, alert(listOf(future), today = gaAlertDay))
    }

    @Test
    fun futureScanDoesNotHideAnEarlierValidScan() {
        val scans = listOf(gaNoAlertDay.plusDays(60).toString(), gaAlertDay.toString())
        assertEquals(true, alert(scans, today = gaNoAlertDay))
    }

    @Test
    fun scanDatedTodayIsAccepted() {
        assertEquals(true, alert(listOf(gaAlertDay.toString()), today = gaAlertDay))
    }

    @Test
    fun scanAnchorWithOnlyInvalidScansMatchesNoScans() {
        val today = gaAlertDay
        val withInvalid = datingSummaryOf(patient(listOf("junk", lmp.minusDays(5).toString(), today.plusDays(9).toString())), today)
        val withNone = datingSummaryOf(patient(emptyList()), today)
        assertNotNull(withNone.discordance)
        assertEquals(withNone.discordance, withInvalid.discordance)
    }

    @Test
    fun extremeScanDatesNeverThrowAndAreIgnored() {
        val scans = listOf("+999999999-12-31", "-999999999-01-01", "9999-12-31", "0000-01-01")
        val s = datingSummaryOf(patient(scans), today = gaAlertDay)
        assertNotNull(s.discordance)
        assertNull(datingSummaryOf(patient(scans), today = gaNoAlertDay).discordance)
    }
}
