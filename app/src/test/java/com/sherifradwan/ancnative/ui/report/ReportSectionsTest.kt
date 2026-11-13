package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DeliveryNoteSections
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.MEDICATION_LABEL_ACTIVE
import com.sherifradwan.ancnative.model.composeDeliveryNotes
import com.sherifradwan.ancnative.model.medicationInstructions
import com.sherifradwan.ancnative.model.medicationStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportSectionsTest {

    private fun isoDay(i: Int): String = LocalDate.of(2026, 1, 1).plusDays(i.toLong() * 7).toString()

    private fun patient(visits: List<DemoVisit> = emptyList()): DemoPatient = DemoPatient(
        id = "p1",
        caseId = "CASE-1",
        displayName = "Test Patient",
        dateOfBirth = "1997-05-04",
        phone = "01000000000",
        visits = visits,
    )

    private fun visit(i: Int): DemoVisit = DemoVisit(
        id = "v-$i", date = isoDay(i), bloodPressure = "120/80", weight = "68.5",
        findings = "Fundal height appropriate", notes = null, revision = 1, isSyntheticEdit = false,
    )

    private fun scan(i: Int): DemoUltrasoundScan = DemoUltrasoundScan(
        id = "s-$i", patientId = "p1", date = isoDay(i),
        gestationalAgeWeeks = 12 + i, gestationalAgeDays = 3, fetalNumber = null,
        presentation = "Cephalic", placentalSite = "Anterior", amnioticFluid = "Normal",
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = "${1000 + i * 100}", impression = "Normal growth",
    )

    private fun lab(i: Int): DemoLabInvestigation = DemoLabInvestigation(
        id = "l-$i", patientId = "p1", date = isoDay(i), testCategory = "Blood",
        testName = "Haemoglobin $i", result = "11.$i", unit = "g/dL",
        referenceRange = "11.0 - 15.0", isAbnormal = false, notes = null,
    )

    private fun medication(
        id: String,
        name: String,
        status: String,
        startDate: String,
        dosage: String = "500 mg",
        route: String = "PO",
        frequency: String = "Twice daily",
        notes: String? = null,
        isProphylaxis: Boolean = false,
        isNonCompliant: Boolean = false,
        adverseNotes: String? = null,
    ): DemoMedicationRecord = DemoMedicationRecord(
        id = id, patientId = "p1", name = name, dosage = dosage, route = route,
        frequency = frequency, startDate = startDate, endDate = null, status = status,
        notes = notes, isProphylaxis = isProphylaxis, isNonCompliant = isNonCompliant,
        adverseNotes = adverseNotes,
    )

    private fun dating(): DemoPatientDating = DemoPatientDating(
        patientId = "p1", lmpDate = "2026-01-01", ultrasoundEdd = null,
        agreedEdd = null, datingMethod = "LMP", notes = null,
    )

    @Test
    fun s1_newest_visits_are_kept() {
        val result = reportVisits(patient((0 until 15).map { visit(it) }), 12)
        assertEquals((3 until 15).map { "v-$it" }, result.rows.map { it.id })
        assertEquals(3, result.hidden)
    }

    @Test
    fun s1_all_visits_kept_below_limit() {
        val result = reportVisits(patient((0 until 5).map { visit(it) }), 12)
        assertEquals(5, result.rows.size)
        assertEquals(0, result.hidden)
    }

    @Test
    fun s1_visits_are_sorted_oldest_first() {
        val result = reportVisits(patient(listOf(visit(2), visit(0), visit(1))), 12)
        assertEquals(listOf("v-0", "v-1", "v-2"), result.rows.map { it.id })
    }

    @Test
    fun s2_newest_scans_are_kept() {
        val result = reportScans(patient().copy(ultrasoundScans = (0 until 15).map { scan(it) }), 12)
        assertEquals((3 until 15).map { "s-$it" }, result.rows.map { it.id })
        assertEquals(3, result.hidden)
    }

    @Test
    fun s2_all_scans_kept_below_limit() {
        val result = reportScans(patient().copy(ultrasoundScans = (0 until 5).map { scan(it) }), 12)
        assertEquals(5, result.rows.size)
        assertEquals(0, result.hidden)
    }

    @Test
    fun s3_newest_labs_are_kept_first() {
        val result = reportLabs(patient().copy(labInvestigations = (0 until 12).map { lab(it) }), 5)
        assertEquals(listOf("l-11", "l-10", "l-9", "l-8", "l-7"), result.rows.map { it.id })
        assertEquals(7, result.hidden)
    }

    @Test
    fun s3_all_labs_kept_below_limit() {
        val result = reportLabs(patient().copy(labInvestigations = (0 until 3).map { lab(it) }), 5)
        assertEquals(listOf("l-2", "l-1", "l-0"), result.rows.map { it.id })
        assertEquals(0, result.hidden)
    }

    @Test
    fun s4_active_and_stopped_medications_are_split() {
        val meds = listOf(
            medication(id = "m-old-active", name = "Iron", status = "Active", startDate = "2026-01-01"),
            medication(id = "m-new-active", name = "Calcium", status = "Active", startDate = "2026-02-01"),
            medication(id = "m-completed", name = "Antibiotic", status = "Completed", startDate = "2026-03-01"),
            medication(id = "m-non-compliant", name = "Folate", status = "Patient non-compliant", startDate = "2026-01-15"),
            medication(id = "m-discontinued", name = "Aspirin", status = "Discontinued", startDate = "2025-12-01"),
        )
        val result = reportMedications(patient().copy(medications = meds))
        assertEquals(listOf("m-new-active", "m-non-compliant", "m-old-active"), result.current.map { it.id })
        assertEquals(listOf("m-completed", "m-discontinued"), result.stopped.map { it.id })
        assertEquals(meds.size, result.current.size + result.stopped.size)
        assertEquals(MEDICATION_LABEL_ACTIVE, medicationStatusLabel("Active", false))
    }

    @Test
    fun s5_risk_tiers_map_one_to_one() {
        assertEquals(ReportRisk.MEDIUM, reportRisk("moderate"))
        assertEquals(ReportRisk.HIGH, reportRisk("high"))
        assertEquals(ReportRisk.LOW, reportRisk("low"))
        assertEquals("High risk", ReportRisk.HIGH.label)
    }

    @Test
    fun s5_missing_risk_is_unrecorded() {
        assertEquals(ReportRisk.UNRECORDED, reportRisk(null))
        assertEquals(ReportRisk.UNRECORDED, reportRisk(""))
        assertEquals(ReportRisk.UNRECORDED, reportRisk("   "))
    }

    @Test
    fun s6_active_patient_has_no_status_line() {
        assertNull(reportStatusLine(patient()))
    }

    @Test
    fun s6_delivered_patient_shows_the_delivery_line() {
        val delivered = patient().copy(
            delivered = true,
            deliveryDate = "2026-10-08",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
        )
        val line = reportStatusLine(delivered)
        assertNotNull(line)
        assertTrue(line!!.startsWith("Delivered"))
    }

    @Test
    fun s6_archived_patient_shows_archived() {
        assertEquals("Archived record", reportStatusLine(patient().copy(status = "archived")))
    }

    @Test
    fun s6_archived_and_delivered_combine_both_lines() {
        val archivedDelivered = patient().copy(
            status = "archived",
            delivered = true,
            deliveryDate = "2026-10-08",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
        )
        val line = reportStatusLine(archivedDelivered)
        assertNotNull(line)
        assertTrue(line!!.startsWith("Archived record · Delivered"))
    }

    @Test
    fun s7_gestational_age_comes_from_the_dating() {
        val dating = dating()
        val asOf = LocalDate.of(2026, 3, 1)
        val patient = patient().copy(dating = dating)
        assertEquals(dating.calculateGestationalAgeResult(asOf)?.formatted, reportGestationalAge(patient, asOf))
    }

    @Test
    fun s7_delivered_patient_never_gets_a_current_gestational_age() {
        val delivered = patient().copy(dating = dating(), delivered = true, deliveryDate = "2026-10-08")
        assertNull(reportGestationalAge(delivered, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun s7_no_dating_means_no_gestational_age() {
        assertNull(reportGestationalAge(patient(), LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun s8_iso_date_is_printed_day_first() {
        assertEquals("01/03/2026", reportDate("2026-03-01"))
    }

    @Test
    fun s8_missing_date_is_not_recorded() {
        assertEquals(NOT_RECORDED, reportDate(null))
        assertEquals(NOT_RECORDED, reportDate(""))
        assertEquals(NOT_RECORDED, reportDate("   "))
    }

    @Test
    fun s8_non_iso_text_is_kept() {
        assertEquals("March 2026", reportDate("  March 2026  "))
    }

    @Test
    fun s9_findings_and_notes_are_both_printed() {
        val visit = visit(0).copy(findings = "Fundal height 30 cm", notes = "Take iron")
        assertEquals("Fundal height 30 cm · Take iron", visitText(visit))
    }

    @Test
    fun s9_blank_parts_are_left_out() {
        val visit = visit(0).copy(findings = "  ", notes = " Take iron ")
        assertEquals("Take iron", visitText(visit))
    }

    @Test
    fun s9_nothing_recorded() {
        val visit = visit(0).copy(findings = null, notes = null)
        assertEquals(NOT_RECORDED, visitText(visit))
    }

    @Test
    fun s10_weeks_and_days() {
        val scan = scan(0).copy(gestationalAgeWeeks = 12, gestationalAgeDays = 3)
        assertEquals("12w + 3d", scanGestationalAge(scan))
    }

    @Test
    fun s10_weeks_only() {
        val scan = scan(0).copy(gestationalAgeWeeks = 12, gestationalAgeDays = null)
        assertEquals("12w", scanGestationalAge(scan))
    }

    @Test
    fun s10_days_only() {
        val scan = scan(0).copy(gestationalAgeWeeks = null, gestationalAgeDays = 3)
        assertEquals("3d", scanGestationalAge(scan))
    }

    @Test
    fun s10_nothing_recorded() {
        val scan = scan(0).copy(gestationalAgeWeeks = null, gestationalAgeDays = null)
        assertEquals("-", scanGestationalAge(scan))
    }

    @Test
    fun s11_all_scan_parts_in_order() {
        val scan = scan(0).copy(
            fetalNumber = "Singleton", fetalHeartRate = "140", impression = "Normal growth",
            biometryBpd = "5.1", biometryHc = "18.2", biometryAc = "15.0", biometryFl = "3.0",
        )
        assertEquals(
            "Fetuses: Singleton · FHR: 140 · Normal growth · BPD: 5.1 · HC: 18.2 · AC: 15.0 · FL: 3.0",
            scanNotes(scan),
        )
    }

    @Test
    fun s11_impression_never_hides_biometry() {
        val scan = scan(0).copy(
            fetalNumber = null, fetalHeartRate = null, impression = "Normal growth",
            biometryBpd = "5.1", biometryHc = null, biometryAc = null, biometryFl = null,
        )
        assertEquals("Normal growth · BPD: 5.1", scanNotes(scan))
    }

    @Test
    fun s11_nothing_recorded() {
        val scan = scan(0).copy(
            fetalNumber = null, fetalHeartRate = null, impression = null,
            biometryBpd = null, biometryHc = null, biometryAc = null, biometryFl = null,
        )
        assertEquals(NOT_RECORDED, scanNotes(scan))
    }

    @Test
    fun s12_plain_number_gets_grams() {
        assertEquals("1200 g", scanWeight(scan(0).copy(estimatedFetalWeight = "1200")))
    }

    @Test
    fun s12_weight_with_unit_is_kept() {
        assertEquals("1.2 kg", scanWeight(scan(0).copy(estimatedFetalWeight = "1.2 kg")))
    }

    @Test
    fun s12_missing_weight() {
        assertEquals("-", scanWeight(scan(0).copy(estimatedFetalWeight = null)))
        assertEquals("-", scanWeight(scan(0).copy(estimatedFetalWeight = "")))
    }

    @Test
    fun s13_detail_joins_instructions_prophylaxis_and_adverse_and_never_the_frequency() {
        val medication = medication(
            id = "m1", name = "Iron", status = "Active", startDate = "2026-01-01",
            dosage = "500 mg", route = "PO", frequency = "Twice daily", notes = "after meals",
            isProphylaxis = true, adverseNotes = "Nausea",
        )
        assertEquals(
            "500 mg · PO · after meals · Prophylaxis · Adverse: Nausea",
            medicationDetail(medication),
        )
    }

    @Test
    fun s13_detail_without_extras_is_the_instructions() {
        val medication = medication(
            id = "m2", name = "Iron", status = "Active", startDate = "2026-01-01",
            dosage = "500 mg", route = "PO", frequency = "", notes = null,
        )
        assertEquals(medicationInstructions("500 mg", "PO", null), medicationDetail(medication))
    }

    @Test
    fun s13_blank_medication_has_empty_detail() {
        val medication = medication(
            id = "m3", name = "Iron", status = "Active", startDate = "2026-01-01",
            dosage = "", route = "", frequency = "", notes = null,
        )
        assertEquals("", medicationDetail(medication))
    }

    @Test
    fun s14_start_date_only() {
        val medication = medication(id = "m1", name = "Iron", status = "Active", startDate = "2026-03-01")
        assertEquals("From 01/03/2026", medicationDates(medication))
    }

    @Test
    fun s14_start_and_end_dates() {
        val medication = medication(id = "m2", name = "Iron", status = "Active", startDate = "2026-03-01")
            .copy(endDate = "2026-04-15")
        assertEquals("01/03/2026 to 15/04/2026", medicationDates(medication))
    }

    @Test
    fun s14_blank_start_date_gives_empty_text() {
        val medication = medication(id = "m3", name = "Iron", status = "Active", startDate = "")
        assertEquals("", medicationDates(medication))
    }

    @Test
    fun s15_result_and_unit() {
        val lab = lab(0).copy(result = "11.2", unit = "g/dL")
        assertEquals("11.2 g/dL", labResult(lab))
    }

    @Test
    fun s15_blank_unit_leaves_no_trailing_space() {
        assertEquals("11.2", labResult(lab(0).copy(result = "11.2", unit = "   ")))
        assertEquals("11.2", labResult(lab(0).copy(result = "11.2", unit = null)))
    }

    @Test
    fun s15_lab_extra_joins_name_and_notes() {
        val lab = lab(0).copy(labName = "City Lab", notes = "Fasting")
        assertEquals("City Lab · Fasting", labExtra(lab))
    }

    @Test
    fun s15_lab_extra_empty_when_missing() {
        assertEquals("", labExtra(lab(0).copy(labName = null, notes = null)))
    }

    @Test
    fun s16_plan_rows_are_ordered_and_exclude_fees() {
        val notes = composeDeliveryNotes(
            DeliveryNoteSections(
                hospital = "City Hospital",
                fees = "5000 EGP",
                plan = "vaginal",
                postpartum = "Review in 6 weeks",
            )
        )
        val rows = deliveryPlanRows(patient().copy(obstetricNotes = notes))
        assertEquals(listOf("Planned delivery", "Hospital", "Postpartum"), rows.map { it.label })
        assertEquals(listOf("vaginal", "City Hospital", "Review in 6 weeks"), rows.map { it.value })
        assertTrue(rows.none { it.value.contains("5000") })
    }

    @Test
    fun s16_no_notes_means_no_rows() {
        assertTrue(deliveryPlanRows(patient()).isEmpty())
    }
}
