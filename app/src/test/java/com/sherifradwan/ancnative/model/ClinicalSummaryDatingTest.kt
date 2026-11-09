package com.sherifradwan.ancnative.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalSummaryDatingTest {
    private fun patient(
        lmp: String? = null,
        us: String? = null,
        agreed: String? = null,
        method: String? = null,
        transfer: String? = null,
        embryoAge: Int? = null,
        scanDate: String? = "2026-08-27",
    ): DemoPatient {
        val scans = listOfNotNull(
            scanDate?.let {
                DemoUltrasoundScan(
                    id = "s1", patientId = "p1", date = it,
                    gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
                    presentation = null, placentalSite = null, amnioticFluid = null,
                    fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
                    biometryFl = null, estimatedFetalWeight = null, impression = null,
                )
            },
        )
        return DemoPatient(
            id = "p1", caseId = "C1", displayName = "Share Test", dateOfBirth = null, phone = null,
            visits = emptyList(),
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = lmp, ultrasoundEdd = us, agreedEdd = agreed,
                datingMethod = method, notes = null, transferDate = transfer, embryoAgeDays = embryoAge,
            ),
            ultrasoundScans = scans,
        )
    }

    private fun datingBlock(p: DemoPatient): String {
        val text = ClinicalSummaryGenerator.generateTextSummary(p).replace(' ', ' ')
        val after = text.substringAfter("--- PREGNANCY DATING ---")
        return after.substringBefore("--- RECENT VISITS")
    }

    @Test
    fun lmpPatientSharesLmpAndEddInDmy() {
        val block = datingBlock(patient(lmp = "2026-06-08", agreed = "2027-03-15", method = "LMP"))
        assertTrue(block, Regex("""LMP:? 08/06/2026""").containsMatchIn(block))
        assertTrue(block, Regex("""EDD:? 15/03/2027""").containsMatchIn(block))
        assertFalse(block, block.contains("2026-06-08"))
        assertFalse(block, block.contains("Confirmed EDD"))
    }

    @Test
    fun ivfPatientSharesEtWithTransferDateAndNeverLmp() {
        val block = datingBlock(
            patient(lmp = "2026-06-01", agreed = "2027-03-08", method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5),
        )
        assertTrue(block, Regex("""ET:? 20/06/2026""").containsMatchIn(block))
        assertFalse(block, Regex("""\bLMP\b""").containsMatchIn(block))
        assertFalse(block, block.contains("IVF_TRANSFER"))
        assertTrue(block, Regex("""Method:? ET\b""").containsMatchIn(block))
    }

    @Test
    fun methodSharedAsLabelNotRawCode() {
        val us = datingBlock(patient(lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-24", method = "Ultrasound"))
        assertTrue(us, Regex("""Method:? by ultrasound""").containsMatchIn(us))
        val clinical = datingBlock(patient(agreed = "2027-04-02", method = "Clinical"))
        assertTrue(clinical, Regex("""Method:? clinical""").containsMatchIn(clinical))
    }

    @Test
    fun discordanceSharesBothDueDatesAndGap() {
        val block = datingBlock(patient(lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-15", method = "LMP"))
        assertTrue(block, block.contains("LMP EDD 15/03/2027"))
        assertTrue(block, block.contains("US EDD 24/03/2027"))
        assertTrue(block, block.contains("gap 9 d"))
    }

    @Test
    fun noDiscordanceOmitsScanEddLine() {
        val block = datingBlock(patient(lmp = "2026-06-08", us = "2027-03-18", agreed = "2027-03-15", method = "LMP"))
        assertFalse(block, block.contains("US EDD"))
        assertFalse(block, block.contains("gap"))
    }

    @Test
    fun referredPatientSharesConfirmedEddAndNoLmpOrEtRow() {
        val block = datingBlock(patient(agreed = "2027-04-02", method = "Clinical", scanDate = null))
        assertTrue(block, Regex("""Confirmed EDD:? 02/04/2027""").containsMatchIn(block))
        assertFalse(block, Regex("""\bLMP\b""").containsMatchIn(block))
        assertFalse(block, Regex("""\bET\b""").containsMatchIn(block))
    }

    @Test
    fun ivfPatientNeverSharesScanEddOrGap() {
        val block = datingBlock(
            patient(
                lmp = "2026-06-08", us = "2027-04-09", agreed = "2027-03-08",
                method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5,
            ),
        )
        assertFalse(block, block.contains("US EDD"))
        assertFalse(block, block.contains("gap"))
    }

    @Test
    fun ultrasoundMethodNeverSharesScanEddOrGap() {
        val block = datingBlock(patient(lmp = "2026-06-08", us = "2027-04-09", agreed = null, method = "Ultrasound"))
        assertFalse(block, block.contains("US EDD"))
        assertFalse(block, block.contains("gap"))
    }

    @Test
    fun scanEddEarlierThanLmpEddSharesPositiveGap() {
        val block = datingBlock(patient(lmp = "2026-06-08", us = "2027-03-06", agreed = "2027-03-15", method = "LMP"))
        assertTrue(block, block.contains("LMP EDD 15/03/2027"))
        assertTrue(block, block.contains("US EDD 06/03/2027"))
        assertTrue(block, block.contains("gap 9 d"))
        assertFalse(block, block.contains("gap -"))
    }
}
