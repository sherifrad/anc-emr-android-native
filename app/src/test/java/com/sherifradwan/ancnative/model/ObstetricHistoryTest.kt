package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round 3s / Q102 unit tests (SPEC_FIX_R3.md, section "Round 3s"):
 * Shared obstetric history counting and summary rules.
 *
 * Rules:
 *  - PreviousDeliveryCounts(cesarean, vaginal, stillbirth, earlyLoss)
 *  - Cesarean: birthMethod == "Cesarean" || deliveryBy == "CS" (case-insensitive, trimmed).
 *  - Vaginal: birthMethod == "Vaginal" || deliveryBy == "Vaginal" (case-insensitive, trimmed).
 *  - Structured fields win; conflicting structured values count as Cesarean once (never double-counted).
 *  - Hysterotomy is counted as neither cesarean nor vaginal.
 *  - Free text outcomeText fallback only when birthMethod and deliveryBy are both null/blank:
 *    whole-word case-insensitive "CS"/"Cesarean" -> cesarean, "SVD"/"Vaginal" -> vaginal.
 *    No substring matching ("CSF" is not CS, "Vaginally" is not Vaginal).
 *  - Stillbirth: outcome == "Stillbirth" || outcomeText contains "Stillbirth" or "IUFD" (ignore case).
 *  - Early loss: outcome == "EarlyPregnancyLoss" || outcomeText contains "Miscarriage" or "Loss" (ignore case).
 *  - previousDeliverySummary(patient): exact string branches per spec.
 */
class ObstetricHistoryTest {

    private fun preg(
        id: String = "preg-1",
        birthMethod: String? = null,
        deliveryBy: String? = null,
        outcomeText: String? = null,
        outcome: String? = "LiveBirth",
    ): DemoPreviousPregnancy = DemoPreviousPregnancy(
        id = id,
        patientId = "p-1",
        modelVersion = 3,
        dateText = "2024-01-01",
        outcomeText = outcomeText,
        pregnancyType = "Singleton",
        otherDetails = null,
        placeAndDoctor = null,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = null,
        terminationMethod = null,
        provenanceKind = "CLINICIAN_ENTRY",
        clinicianId = "clinician-1",
        recordedAt = "2024-01-01",
        updatedAt = "2024-01-01",
        revision = 1,
    )

    @Test
    fun pf5_oldRowWithOutcomeTextOnlyStillCountsFromFreeText() {
        val counts = previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "CS")))
        assertEquals(1, counts.cesarean)
        assertEquals(0, counts.vaginal)
        val vaginal = previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "SVD")))
        assertEquals(1, vaginal.vaginal)
    }

    private fun patient(
        gravida: Long? = 1L,
        parity: Long? = 0L,
        abortion: Long? = 0L,
        pregnancies: List<DemoPreviousPregnancy> = emptyList(),
    ): DemoPatient = DemoPatient(
        id = "p-test",
        caseId = "CASE-1",
        displayName = "Test Patient",
        dateOfBirth = null,
        phone = null,
        visits = emptyList(),
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        previousPregnancies = pregnancies,
    )

    // --- Counting tests ---

    @Test
    fun emptyList_returnsAllZeroCounts() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(emptyList()),
        )
    }

    @Test
    fun structuredCesarean_combinationsCountAsCesarean() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Cesarean", deliveryBy = null))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "CS"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Cesarean", deliveryBy = "CS"))),
        )
    }

    @Test
    fun structuredCesarean_caseAndWhitespaceIgnored() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "  cesarean  ", deliveryBy = null))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "  cs  "))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "CESAREAN", deliveryBy = "CS"))),
        )
    }

    @Test
    fun structuredVaginal_combinationsCountAsVaginal() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Vaginal", deliveryBy = null))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "Vaginal"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Vaginal", deliveryBy = "Vaginal"))),
        )
    }

    @Test
    fun structuredVaginal_caseAndWhitespaceIgnored() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "  vaginal  ", deliveryBy = null))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "  VAGINAL  "))),
        )
    }

    @Test
    fun conflictingStructuredFields_countAsCesareanOnce() {
        // Cesarean wins when both structured fields conflict; never double-count
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Cesarean", deliveryBy = "Vaginal"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Vaginal", deliveryBy = "CS"))),
        )
    }

    @Test
    fun hysterotomy_countsAsNeitherCesareanNorVaginal() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "Hysterotomy"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Hysterotomy", deliveryBy = "Hysterotomy"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "hysterotomy", deliveryBy = null))),
        )
        // Even if outcomeText mentions CS, structured deliveryBy = "Hysterotomy" prevents fallback
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = "Hysterotomy", outcomeText = "CS"))),
        )
    }

    @Test
    fun outcomeTextFallback_whenStructuredFieldsBothNullOrBlank() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "Emergency CS"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "", deliveryBy = "  ", outcomeText = "Elective Cesarean delivery"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "Spontaneous SVD at 39 weeks"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = " ", deliveryBy = null, outcomeText = "Normal Vaginal delivery"))),
        )
        // Case-insensitive in outcomeText
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "cs"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "svd"))),
        )
    }

    @Test
    fun outcomeTextFallback_conflictingWords_cesareanWins() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "Trial of labor after CS resulted in repeat Cesarean not SVD"))),
        )
    }

    @Test
    fun outcomeTextFallback_wholeWordsOnly_noSubstringMatching() {
        // "CSF" is not a CS
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "Dural puncture with CSF leak"))),
        )
        // "Vaginally" is not matched by whole-word rule
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "Delivered vaginally at hospital"))),
        )
        // "CSFix" or other prefix/suffix
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "CSFix token"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = null, deliveryBy = null, outcomeText = "NonSVD"))),
        )
    }

    @Test
    fun structuredFieldsWinOverOutcomeText() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Vaginal", deliveryBy = null, outcomeText = "History of CS"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 0, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Cesarean", deliveryBy = null, outcomeText = "Prior SVD"))),
        )
    }

    @Test
    fun stillbirthRules_countedFromOutcomeOrOutcomeText() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 1, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(outcome = "Stillbirth"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 1, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "Macerated stillbirth at 34 weeks"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 1, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "Intrauterine fetal demise (IUFD)"))),
        )
        // Stillbirth alongside delivery route (counts both)
        assertEquals(
            PreviousDeliveryCounts(cesarean = 1, vaginal = 0, stillbirth = 1, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Cesarean", deliveryBy = "CS", outcome = "Stillbirth"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 1, stillbirth = 1, earlyLoss = 0),
            previousDeliveryCounts(listOf(preg(birthMethod = "Vaginal", deliveryBy = "Vaginal", outcomeText = "SVD, IUFD at term"))),
        )
    }

    @Test
    fun earlyLossRules_countedFromOutcomeOrOutcomeText() {
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 1),
            previousDeliveryCounts(listOf(preg(outcome = "EarlyPregnancyLoss"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 1),
            previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "Spontaneous miscarriage at 8 weeks"))),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 0, vaginal = 0, stillbirth = 0, earlyLoss = 1),
            previousDeliveryCounts(listOf(preg(outcome = null, outcomeText = "First trimester pregnancy loss"))),
        )
    }

    @Test
    fun multiplePregnancies_aggregateCounts() {
        val list = listOf(
            preg(id = "1", birthMethod = "Cesarean", deliveryBy = "CS"),
            preg(id = "2", birthMethod = "Cesarean", deliveryBy = "CS"),
            preg(id = "3", birthMethod = "Vaginal", deliveryBy = "Vaginal"),
            preg(id = "4", outcome = "Stillbirth"),
            preg(id = "5", outcome = "EarlyPregnancyLoss"),
            preg(id = "6", outcome = null, outcomeText = "Recurrent miscarriage"),
        )
        assertEquals(
            PreviousDeliveryCounts(cesarean = 2, vaginal = 1, stillbirth = 1, earlyLoss = 2),
            previousDeliveryCounts(list),
        )
    }

    // --- Summary text tests ---

    @Test
    fun summary_branch1_primigravida() {
        // g == 1 && p == 0 && a == 0 && pregnancies empty -> "Primigravida"
        assertEquals("Primigravida", previousDeliverySummary(patient(gravida = 1L, parity = 0L, abortion = 0L)))
        // Nulls default to g=1, p=0, a=0
        assertEquals("Primigravida", previousDeliverySummary(patient(gravida = null, parity = null, abortion = null)))
    }

    @Test
    fun summary_branch2_p0NoPriorDeliveries() {
        // p == 0 && pregnancies empty (with g > 1 or a > 0) -> "P0 · No prior deliveries"
        assertEquals("P0 · No prior deliveries", previousDeliverySummary(patient(gravida = 2L, parity = 0L, abortion = 1L)))
        assertEquals("P0 · No prior deliveries", previousDeliverySummary(patient(gravida = 3L, parity = 0L, abortion = 2L)))
        assertEquals("P0 · No prior deliveries", previousDeliverySummary(patient(gravida = 2L, parity = 0L, abortion = 0L)))
    }

    @Test
    fun summary_branch3_cesareanOnly() {
        val p1 = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(preg(birthMethod = "Cesarean")),
        )
        assertEquals("Prev 1 CS", previousDeliverySummary(p1))

        val p2 = patient(
            gravida = 3L, parity = 2L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Cesarean"),
                preg(id = "2", deliveryBy = "CS"),
            ),
        )
        assertEquals("Prev 2 CS", previousDeliverySummary(p2))
    }

    @Test
    fun summary_branch3_vaginalOnly() {
        val p1 = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(preg(birthMethod = "Vaginal")),
        )
        assertEquals("Prev 1 SVD", previousDeliverySummary(p1))

        val p2 = patient(
            gravida = 3L, parity = 2L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Vaginal"),
                preg(id = "2", deliveryBy = "Vaginal"),
            ),
        )
        assertEquals("Prev 2 SVD", previousDeliverySummary(p2))
    }

    @Test
    fun summary_branch3_multipleCSAndVaginal_csFirstJoinedByComma() {
        val p = patient(
            gravida = 4L, parity = 3L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Cesarean"),
                preg(id = "2", birthMethod = "Cesarean"),
                preg(id = "3", birthMethod = "Vaginal"),
            ),
        )
        assertEquals("Prev 2 CS, 1 SVD", previousDeliverySummary(p))

        val p2 = patient(
            gravida = 3L, parity = 2L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Cesarean"),
                preg(id = "2", birthMethod = "Vaginal"),
            ),
        )
        assertEquals("Prev 1 CS, 1 SVD", previousDeliverySummary(p2))

        val p3 = patient(
            gravida = 4L, parity = 3L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Cesarean"),
                preg(id = "2", birthMethod = "Vaginal"),
                preg(id = "3", birthMethod = "Vaginal"),
            ),
        )
        assertEquals("Prev 1 CS, 2 SVD", previousDeliverySummary(p3))
    }

    @Test
    fun summary_branch3_withStillbirthSuffix_realCountAppended() {
        // CS with 1 stillbirth
        val p1 = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(
                preg(birthMethod = "Cesarean", outcome = "Stillbirth"),
            ),
        )
        assertEquals("Prev 1 CS · 1 IUFD", previousDeliverySummary(p1))

        // SVD with 1 stillbirth
        val p2 = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(
                preg(birthMethod = "Vaginal", outcome = "Stillbirth"),
            ),
        )
        assertEquals("Prev 1 SVD · 1 IUFD", previousDeliverySummary(p2))

        // 2 CS, 1 SVD with 2 stillbirths
        val p3 = patient(
            gravida = 6L, parity = 5L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = "Cesarean", outcome = "LiveBirth"),
                preg(id = "2", birthMethod = "Cesarean", outcome = "Stillbirth"),
                preg(id = "3", birthMethod = "Vaginal", outcome = "Stillbirth"),
            ),
        )
        assertEquals("Prev 2 CS, 1 SVD · 2 IUFD", previousDeliverySummary(p3))
    }

    @Test
    fun summary_branch4_priorStillbirthWithoutCSOrVaginal() {
        val p = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(
                preg(deliveryBy = "Hysterotomy", outcome = "Stillbirth"),
            ),
        )
        assertEquals("Prior Stillbirth (IUFD)", previousDeliverySummary(p))
    }

    @Test
    fun summary_branch5_priorMiscarriages() {
        val p1 = patient(
            gravida = 2L, parity = 0L, abortion = 1L,
            pregnancies = listOf(
                preg(outcome = "EarlyPregnancyLoss"),
            ),
        )
        assertEquals("1 Prior Miscarriage", previousDeliverySummary(p1))

        val p2 = patient(
            gravida = 4L, parity = 0L, abortion = 3L,
            pregnancies = listOf(
                preg(id = "1", outcome = "EarlyPregnancyLoss"),
                preg(id = "2", outcomeText = "Miscarriage at 7 weeks"),
                preg(id = "3", outcomeText = "Early pregnancy loss"),
            ),
        )
        assertEquals("3 Prior Miscarriages", previousDeliverySummary(p2))
    }

    @Test
    fun summary_priorMiscarriage_count1AndCount2() {
        val p1 = patient(
            gravida = 2L, parity = 0L, abortion = 1L,
            pregnancies = listOf(
                preg(outcome = "EarlyPregnancyLoss"),
            ),
        )
        assertEquals("1 Prior Miscarriage", previousDeliverySummary(p1))

        val p2 = patient(
            gravida = 3L, parity = 0L, abortion = 2L,
            pregnancies = listOf(
                preg(id = "1", outcome = "EarlyPregnancyLoss"),
                preg(id = "2", outcome = "EarlyPregnancyLoss"),
            ),
        )
        assertEquals("2 Prior Miscarriages", previousDeliverySummary(p2))
    }

    @Test
    fun summary_branch6_parousWithoutCSOrVaginalOrLoss() {
        // Parous with no pregnancy records (e.g. historical P without details)
        val p1 = patient(gravida = 3L, parity = 2L, abortion = 0L)
        assertEquals("Parous (P2)", previousDeliverySummary(p1))

        // Parous with hysterotomy delivery (neither CS nor SVD, no loss)
        val p2 = patient(
            gravida = 2L, parity = 1L, abortion = 0L,
            pregnancies = listOf(preg(deliveryBy = "Hysterotomy", outcome = "LiveBirth")),
        )
        assertEquals("Parous (P1)", previousDeliverySummary(p2))
    }

    @Test
    fun summary_branch7_priorPregnanciesWithoutDeliveryOrLossOrParity() {
        val p = patient(
            gravida = 2L, parity = 0L, abortion = 0L,
            pregnancies = listOf(
                preg(birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
            ),
        )
        assertEquals("1 Prior Pregnancy", previousDeliverySummary(p))

        val p2 = patient(
            gravida = 3L, parity = 0L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
                preg(id = "2", birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
            ),
        )
        assertEquals("2 Prior Pregnancies", previousDeliverySummary(p2))
    }

    @Test
    fun summary_priorPregnancy_count1AndCount2() {
        val p1 = patient(
            gravida = 2L, parity = 0L, abortion = 0L,
            pregnancies = listOf(
                preg(birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
            ),
        )
        assertEquals("1 Prior Pregnancy", previousDeliverySummary(p1))

        val p2 = patient(
            gravida = 3L, parity = 0L, abortion = 0L,
            pregnancies = listOf(
                preg(id = "1", birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
                preg(id = "2", birthMethod = null, deliveryBy = null, outcome = null, outcomeText = null),
            ),
        )
        assertEquals("2 Prior Pregnancies", previousDeliverySummary(p2))
    }

    @Test
    fun summary_branch8_fallbackNoPriorDeliveries() {
        // When parity is 0 and gravida is not 1 and pregnancies empty (caught by branch 2 "P0 · No prior deliveries")
        // But if gravida is 0 and parity is 0 with no pregnancies:
        val p = patient(gravida = 0L, parity = 0L, abortion = 0L)
        assertEquals("P0 · No prior deliveries", previousDeliverySummary(p))
    }
}
