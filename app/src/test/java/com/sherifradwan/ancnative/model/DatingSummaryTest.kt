package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatingSummaryTest {
    private val lmp = LocalDate.of(2026, 1, 1)
    private val lmpEdd = lmp.plusDays(FULL_TERM_DAYS)
    private val today = LocalDate.of(2026, 6, 1)

    private fun dating(
        lmpDate: String? = null,
        ultrasoundEdd: String? = null,
        agreedEdd: String? = null,
        method: String? = null,
        transferDate: String? = null,
        embryoAgeDays: Int? = null,
    ) = DemoPatientDating(
        patientId = "p1",
        lmpDate = lmpDate,
        ultrasoundEdd = ultrasoundEdd,
        agreedEdd = agreedEdd,
        datingMethod = method,
        notes = null,
        transferDate = transferDate,
        embryoAgeDays = embryoAgeDays,
    )

    private fun summary(d: DemoPatientDating?, scan: LocalDate? = null) =
        datingSummary(d, scan, today)

    private fun gapPatient(
        gapDays: Long,
        method: String? = "LMP",
        agreedEdd: String? = lmpEdd.toString(),
    ) = dating(
        lmpDate = lmp.toString(),
        ultrasoundEdd = lmpEdd.plusDays(gapDays).toString(),
        agreedEdd = agreedEdd,
        method = method,
    )

    private fun discordanceAtGa(gaDays: Long, gapDays: Long, method: String? = "LMP") =
        summary(gapPatient(gapDays, method), scan = lmp.plusDays(gaDays)).discordance

    @Test
    fun lmpMethodWithLmpOriginatesFromLmp() {
        val s = summary(dating(lmpDate = "2026-01-01", method = "LMP"))
        assertEquals(DatingOrigin.Lmp("2026-01-01"), s.origin)
    }

    @Test
    fun ultrasoundPatientWithLmpOnFileStillShowsLmp() {
        val s = summary(dating(lmpDate = "2026-01-01", ultrasoundEdd = "2026-10-10", method = "Ultrasound"))
        assertEquals(DatingOrigin.Lmp("2026-01-01"), s.origin)
    }

    @Test
    fun clinicalPatientWithLmpOnFileShowsLmp() {
        val s = summary(dating(lmpDate = "2026-01-01", method = "Clinical"))
        assertEquals(DatingOrigin.Lmp("2026-01-01"), s.origin)
    }

    @Test
    fun nullOrBlankMethodWithLmpShowsLmp() {
        assertEquals(DatingOrigin.Lmp("2026-01-01"), summary(dating(lmpDate = "2026-01-01", method = null)).origin)
        assertEquals(DatingOrigin.Lmp("2026-01-01"), summary(dating(lmpDate = "2026-01-01", method = "  ")).origin)
    }

    @Test
    fun noLmpAndNoTransferGivesNone() {
        assertEquals(DatingOrigin.None, summary(dating(method = "LMP")).origin)
        assertEquals(DatingOrigin.None, summary(dating(lmpDate = " ", method = null)).origin)
    }

    @Test
    fun ivfWithTransferDateOriginatesFromTransferWithEmbryoAge() {
        val s = summary(dating(method = "IVF_TRANSFER", transferDate = "2026-02-01", embryoAgeDays = 3))
        assertEquals(DatingOrigin.Transfer("2026-02-01", 3), s.origin)
    }

    @Test
    fun ivfWithNullEmbryoAgeKeepsItNull() {
        val s = summary(dating(method = "IVF_TRANSFER", transferDate = "2026-02-01", embryoAgeDays = null))
        assertEquals(DatingOrigin.Transfer("2026-02-01", null), s.origin)
    }

    @Test
    fun ivfAliasAnyCaseIsTheTransferMethod() {
        for (m in listOf("IVF", "ivf", " Ivf ", "ivf_transfer")) {
            val s = summary(dating(method = m, transferDate = "2026-02-01", embryoAgeDays = 5))
            assertEquals("method $m", DatingOrigin.Transfer("2026-02-01", 5), s.origin)
        }
    }

    @Test
    fun ivfWithoutTransferDateIsNoneEvenWithLmpOnFile() {
        assertEquals(DatingOrigin.None, summary(dating(lmpDate = "2026-01-01", method = "IVF_TRANSFER")).origin)
        assertEquals(
            DatingOrigin.None,
            summary(dating(lmpDate = "2026-01-01", method = "ivf", transferDate = "  ")).origin,
        )
    }

    @Test
    fun nonIvfMethodIgnoresTransferDate() {
        val s = summary(dating(lmpDate = "2026-01-01", method = "LMP", transferDate = "2026-02-01", embryoAgeDays = 5))
        assertEquals(DatingOrigin.Lmp("2026-01-01"), s.origin)
    }

    @Test
    fun malformedDatesAreKeptAsGiven() {
        assertEquals(DatingOrigin.Lmp("garbage"), summary(dating(lmpDate = "garbage", method = "LMP")).origin)
        assertEquals(
            DatingOrigin.Transfer("2026-13-45", 5),
            summary(dating(method = "IVF", transferDate = "2026-13-45", embryoAgeDays = 5)).origin,
        )
    }

    @Test
    fun eddComesFromTheEngine() {
        val d = dating(lmpDate = "2026-01-01", ultrasoundEdd = "2026-10-10", agreedEdd = "2026-10-05", method = "LMP")
        assertEquals(GestationalAgeEngine.calculateEstimatedDueDate(d), summary(d).edd)
        assertEquals(LocalDate.of(2026, 10, 5), summary(d).edd)
    }

    @Test
    fun eddFallsBackToMethodDueDateWithoutAgreedEdd() {
        assertEquals(lmpEdd, summary(dating(lmpDate = "2026-01-01", method = "LMP")).edd)
    }

    @Test
    fun confirmedLabelOnlyWhenAgreedEddIsTheSoleSource() {
        val d = dating(agreedEdd = "2026-10-05", method = "Clinical")
        assertEquals(EddLabel.CONFIRMED, summary(d).eddLabel)
    }

    @Test
    fun blankWhitespaceSourcesStillCountAsBlankForConfirmed() {
        val d = dating(lmpDate = " ", ultrasoundEdd = "", agreedEdd = "2026-10-05", transferDate = "  ", method = null)
        assertEquals(EddLabel.CONFIRMED, summary(d).eddLabel)
    }

    @Test
    fun autoFilledAgreedEddEqualToLmpEddIsPlain() {
        val d = dating(lmpDate = "2026-01-01", agreedEdd = lmpEdd.toString(), method = "LMP")
        assertEquals(EddLabel.PLAIN, summary(d).eddLabel)
    }

    @Test
    fun agreedEddWithScanEddOnlyIsPlain() {
        val d = dating(ultrasoundEdd = "2026-10-10", agreedEdd = "2026-10-10", method = "Ultrasound")
        assertEquals(EddLabel.PLAIN, summary(d).eddLabel)
    }

    @Test
    fun agreedEddWithTransferDateOnlyIsPlain() {
        val d = dating(agreedEdd = "2026-10-10", method = "IVF_TRANSFER", transferDate = "2026-02-01")
        assertEquals(EddLabel.PLAIN, summary(d).eddLabel)
    }

    @Test
    fun blankAgreedEddWithNoSourcesIsPlain() {
        assertEquals(EddLabel.PLAIN, summary(dating(agreedEdd = " ", method = "Clinical")).eddLabel)
        assertEquals(EddLabel.PLAIN, summary(dating(agreedEdd = null)).eddLabel)
    }

    @Test
    fun knownMethodCodesAreCanonical() {
        for (code in listOf("LMP", "Ultrasound", "IVF_TRANSFER", "Clinical")) {
            assertEquals(code, summary(dating(method = code)).methodCode)
        }
    }

    @Test
    fun methodCodeIsCaseInsensitiveAndTrimmed() {
        assertEquals("LMP", summary(dating(method = " lmp ")).methodCode)
        assertEquals("Ultrasound", summary(dating(method = "ULTRASOUND")).methodCode)
        assertEquals("Clinical", summary(dating(method = "clinical")).methodCode)
        assertEquals("IVF_TRANSFER", summary(dating(method = "ivf_transfer")).methodCode)
    }

    @Test
    fun methodAliasesMapToCanonicalCodes() {
        assertEquals("IVF_TRANSFER", summary(dating(method = "IVF")).methodCode)
        assertEquals("IVF_TRANSFER", summary(dating(method = "ivf")).methodCode)
        assertEquals("Ultrasound", summary(dating(method = "US")).methodCode)
        assertEquals("Ultrasound", summary(dating(method = "us")).methodCode)
    }

    @Test
    fun unknownMethodIsTheTrimmedRawString() {
        assertEquals("Quickening", summary(dating(method = "  Quickening ")).methodCode)
    }

    @Test
    fun blankOrNullMethodHasNoCode() {
        assertNull(summary(dating(method = null)).methodCode)
        assertNull(summary(dating(method = "   ")).methodCode)
    }

    @Test
    fun nullDatingGivesAnAllEmptySummary() {
        val s = summary(null)
        assertEquals(DatingOrigin.None, s.origin)
        assertNull(s.edd)
        assertEquals(EddLabel.PLAIN, s.eddLabel)
        assertNull(s.methodCode)
        assertNull(s.discordance)
    }

    @Test
    fun gapAtTheLimitGivesNoAlertAndOneOverAlertsAtEveryTierBoundary() {
        val tiers = listOf(62L to 5, 63L to 7, 111L to 7, 112L to 10, 153L to 10, 154L to 14, 195L to 14, 196L to 21)
        for ((ga, limit) in tiers) {
            for (sign in listOf(1L, -1L)) {
                assertNull(
                    "GA $ga gap ${sign * limit}",
                    discordanceAtGa(ga, sign * limit),
                )
                val advice = discordanceAtGa(ga, sign * (limit + 1))
                assertNotNull("GA $ga gap ${sign * (limit + 1)}", advice)
                assertEquals(limit, advice!!.thresholdDays)
                assertEquals(limit + 1, kotlin.math.abs(advice.differenceDays))
            }
        }
    }

    @Test
    fun measuresAtTodayWhenThereIsNoScanDate() {
        val d = gapPatient(gapDays = 8)
        val atToday = datingSummary(d, null, lmp.plusDays(70)).discordance
        assertNotNull(atToday)
        assertEquals(7, atToday!!.thresholdDays)
    }

    @Test
    fun noScanDateUsesTodayTierNotAnotherTier() {
        val d = gapPatient(gapDays = 8)
        assertNull(datingSummary(d, null, lmp.plusDays(112)).discordance)
        assertNotNull(datingSummary(d, null, lmp.plusDays(111)).discordance)
    }

    @Test
    fun scanDateTakesPrecedenceOverToday() {
        val d = gapPatient(gapDays = 8)
        assertNull(datingSummary(d, lmp.plusDays(112), lmp.plusDays(70)).discordance)
    }

    @Test
    fun clearedWhenMethodIsUltrasound() {
        assertNull(discordanceAtGa(70, 30, method = "Ultrasound"))
        assertNull(discordanceAtGa(70, 30, method = "us"))
    }

    @Test
    fun clearedWhenAgreedEddEqualsUltrasoundEdd() {
        val scanEdd = lmpEdd.plusDays(30)
        val d = gapPatient(gapDays = 30, agreedEdd = scanEdd.toString())
        assertNull(summary(d, scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun clearedWhenAgreedEddEqualsUltrasoundEddWithWhitespace() {
        val scanEdd = lmpEdd.plusDays(30)
        val d = gapPatient(gapDays = 30, agreedEdd = " $scanEdd ")
        assertNull(summary(d, scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun agreedEddDifferentFromUltrasoundEddDoesNotClear() {
        val d = gapPatient(gapDays = 30, agreedEdd = lmpEdd.plusDays(29).toString())
        assertNotNull(summary(d, scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun alertFiresForClinicalNullAndBlankMethods() {
        for (m in listOf("Clinical", null, " ")) {
            assertNotNull("method $m", discordanceAtGa(70, 30, method = m))
        }
    }

    @Test
    fun neverFiresForTheIvfTransferMethod() {
        for (m in listOf("IVF_TRANSFER", "IVF", "ivf")) {
            val d = gapPatient(gapDays = 30, method = m).copy(transferDate = "2026-02-01", embryoAgeDays = 5)
            assertNull("method $m", summary(d, scan = lmp.plusDays(70)).discordance)
        }
    }

    @Test
    fun neverFiresWithoutLmp() {
        val d = gapPatient(gapDays = 30).copy(lmpDate = null)
        assertNull(summary(d, scan = lmp.plusDays(70)).discordance)
        assertNull(summary(d.copy(lmpDate = "  "), scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun neverFiresWithoutUltrasoundEdd() {
        val d = gapPatient(gapDays = 30).copy(ultrasoundEdd = null)
        assertNull(summary(d, scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun malformedOrOutOfRangeDatesGiveNullAndNeverThrow() {
        val bad = listOf("garbage", "2026-13-45", "2026-02-30", "", "  ")
        for (b in bad) {
            val badLmp = gapPatient(gapDays = 30).copy(lmpDate = b)
            assertNull("lmp $b", summary(badLmp, scan = lmp.plusDays(70)).discordance)
            val badScan = gapPatient(gapDays = 30).copy(ultrasoundEdd = b)
            assertNull("scan edd $b", summary(badScan, scan = lmp.plusDays(70)).discordance)
        }
    }

    @Test
    fun malformedAgreedEddDoesNotThrowAndDoesNotClear() {
        val d = gapPatient(gapDays = 30, agreedEdd = "garbage")
        assertNotNull(summary(d, scan = lmp.plusDays(70)).discordance)
    }

    @Test
    fun summaryDiscordanceMatchesRedatingAdviceOnTheSameInputs() {
        val anchor = lmp.plusDays(100)
        val d = gapPatient(gapDays = 12)
        val scanGa = FULL_TERM_DAYS - java.time.temporal.ChronoUnit.DAYS.between(anchor, lmpEdd.plusDays(12))
        assertEquals(redatingAdvice(100, scanGa), summary(d, scan = anchor).discordance)
    }

    // D5: only an agreed EDD equal to the scan EDD clears the alert; a missing or unusable one never does.
    @Test
    fun discordanceRemainsWhenAgreedEddIsNullBlankWhitespaceOrMalformed() {
        val anchor = lmp.plusDays(80)
        for (agreed in listOf(null, "", "   ", "garbage", "2026-13-45", "2027-02-30")) {
            val s = summary(gapPatient(gapDays = 9, agreedEdd = agreed), scan = anchor)
            assertNotNull("agreedEdd '$agreed' must not clear the alert", s.discordance)
        }
    }

    @Test
    fun discordanceClearsOnlyWhenAgreedEddEqualsTheScanEdd() {
        val anchor = lmp.plusDays(80)
        val scanEdd = lmpEdd.plusDays(9)
        assertNull(summary(gapPatient(gapDays = 9, agreedEdd = scanEdd.toString()), scan = anchor).discordance)
        assertNull(summary(gapPatient(gapDays = 9, agreedEdd = " $scanEdd "), scan = anchor).discordance)
        assertNotNull(summary(gapPatient(gapDays = 9, agreedEdd = scanEdd.plusDays(1).toString()), scan = anchor).discordance)
        assertNotNull(summary(gapPatient(gapDays = 9, agreedEdd = lmpEdd.toString()), scan = anchor).discordance)
    }

    // D6a: parseable but extreme dates never throw, in any date field.
    private val extremeDates = listOf("9999-12-31", "+999999999-12-31", "-999999999-01-01", "0000-01-01")

    private fun assertNoThrow(label: String, block: () -> DatingSummary): DatingSummary =
        try {
            block()
        } catch (e: Exception) {
            throw AssertionError("$label threw $e", e)
        }

    @Test
    fun extremeLmpDateNeverThrows() {
        for (x in extremeDates) {
            val d = dating(lmpDate = x, ultrasoundEdd = lmpEdd.toString(), agreedEdd = lmpEdd.toString(), method = "LMP")
            val s = assertNoThrow("lmpDate $x") { summary(d, scan = lmp) }
            assertNull("lmpDate $x", s.discordance)
            assertNoThrow("lmpDate $x, no scan") { summary(d) }
            assertNoThrow("lmpDate $x, other method") { summary(d.copy(datingMethod = null)) }
        }
    }

    @Test
    fun extremeUltrasoundEddNeverThrows() {
        for (x in extremeDates) {
            val d = dating(lmpDate = lmp.toString(), ultrasoundEdd = x, agreedEdd = lmpEdd.toString(), method = "LMP")
            val s = assertNoThrow("ultrasoundEdd $x") { summary(d, scan = lmp.plusDays(80)) }
            assertNull("ultrasoundEdd $x", s.discordance)
            assertNoThrow("ultrasoundEdd $x, ultrasound method") { summary(d.copy(datingMethod = "Ultrasound")) }
            assertNoThrow("ultrasoundEdd $x, no method") { summary(d.copy(datingMethod = null, agreedEdd = null)) }
        }
    }

    @Test
    fun outOfRangeUltrasoundEddGivesNoDiscordance() {
        for (x in listOf("+999999999-12-31", "-999999999-01-01")) {
            val d = dating(lmpDate = lmp.toString(), ultrasoundEdd = x, agreedEdd = lmpEdd.toString(), method = "LMP")
            assertNull("ultrasoundEdd $x", summary(d, scan = lmp.plusDays(80)).discordance)
        }
    }

    @Test
    fun extremeAgreedEddNeverThrows() {
        for (x in extremeDates) {
            val d = dating(lmpDate = lmp.toString(), ultrasoundEdd = lmpEdd.plusDays(9).toString(), agreedEdd = x, method = "LMP")
            assertNoThrow("agreedEdd $x") { summary(d, scan = lmp.plusDays(80)) }
            val onlyAgreed = dating(agreedEdd = x, method = "Clinical")
            val s = assertNoThrow("agreedEdd $x only") { summary(onlyAgreed) }
            assertNull("agreedEdd $x only", s.discordance)
        }
    }

    @Test
    fun extremeTransferDateNeverThrows() {
        for (x in extremeDates) {
            for (m in listOf("IVF_TRANSFER", "IVF", "LMP", null)) {
                val d = dating(
                    lmpDate = lmp.toString(), ultrasoundEdd = lmpEdd.plusDays(9).toString(),
                    method = m, transferDate = x, embryoAgeDays = 5,
                )
                val s = assertNoThrow("transferDate $x method $m") { summary(d, scan = lmp.plusDays(80)) }
                if (m == "IVF_TRANSFER" || m == "IVF") assertNull("transferDate $x method $m", s.discordance)
            }
        }
    }
}
