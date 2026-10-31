package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

/**
 * Round 3p / Q94 (.agents/pipeline/SPEC_FIX_R3.md, section "Round 3p"): every
 * printed section title is shown ONCE. The duplicate capital `<span
 * class="en-sub">...</span>` line inside each section-title block and the
 * `.section-title .en-sub` CSS rule are removed by the print lane; the visible
 * title keeps exactly the wording the first span has.
 *
 * The builders copy the shapes of PatientPrintHtmlGeneratorTest.kt (that file
 * is not edited by this lane). context = null keeps the test JVM-only.
 */
class PrintSectionTitlesTest {

    private val AS_OF_DATE = LocalDate.of(2026, 10, 31)

    // The 8 core section titles, exactly the wording the first span prints.
    private val TITLES = listOf(
        "Pregnancy Dating",
        "Obstetric & Medical History",
        "Previous Pregnancies & Obstetric Outcomes",
        "Antenatal Care Visits Log",
        "Ultrasound Biometry & Fetal Surveillance",
        "Laboratory Investigations",
        "Active Medications & Prophylaxis",
        "Delivery Management Plan & Attestation",
    )

    // The capital duplicate wording the removed en-sub spans carried.
    private val CAPITAL_DUPLICATES = listOf(
        "PREGNANCY DATING",
        "OBSTETRIC & MEDICAL HISTORY",
        "PREVIOUS PREGNANCIES & OUTCOMES",
        "ANTENATAL CARE VISITS LOG",
        "ULTRASOUND BIOMETRY & SURVEILLANCE",
        "LABORATORY INVESTIGATIONS",
        "ACTIVE MEDICATIONS & PROPHYLAXIS",
        "DELIVERY MANAGEMENT PLAN & CONSULTANT ATTESTATION",
        "ULTRASOUND ATTACHMENTS",
    )

    private val PHOTO_TITLE = "Clinical Imaging & Ultrasound Scans"

    private fun dating(agreedEdd: String?) = DemoPatientDating(
        patientId = "r3p-print-1", lmpDate = "2026-02-01", ultrasoundEdd = null,
        agreedEdd = agreedEdd, datingMethod = "LMP", notes = null,
    )

    private fun visit() = DemoVisit(
        id = "r3p-visit", date = "2026-09-01", bloodPressure = "120/80", weight = "68.5",
        findings = "Fundal height ok", notes = "side note", revision = 1,
        isSyntheticEdit = false,
    )

    private fun prevPreg() = DemoPreviousPregnancy(
        id = "r3p-prev", patientId = "r3p-print-1", modelVersion = 1, dateText = "2020",
        outcomeText = null, pregnancyType = null, otherDetails = "unremarkable",
        placeAndDoctor = "City Hospital, Dr A", deliveryBy = null, outcome = "LiveBirth",
        birthMethod = null, lossManagement = null, terminationMethod = null,
        provenanceKind = "t", clinicianId = "c", recordedAt = "2026-01-01",
        updatedAt = "2026-01-01", revision = 1,
    )

    private fun med() = DemoMedicationRecord(
        id = "r3p-med", patientId = "r3p-print-1", name = "Folic acid", dosage = "5 mg",
        route = "PO", frequency = "OD", startDate = "2026-01-01", endDate = null,
        status = "active", notes = "after meals", isProphylaxis = false,
    )

    private fun scanWithPhoto() = DemoUltrasoundScan(
        id = "r3p-scan", patientId = "r3p-print-1", date = "2026-09-01",
        gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
        presentation = "Breech", placentalSite = null, amnioticFluid = null,
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = null, impression = "Normal growth",
    ).copy(imageUris = listOf("content://x/1"))

    private fun fullPatient() = DemoPatient(
        id = "r3p-print-1",
        caseId = "R3P-PRINT-1",
        displayName = "Titles Test Patient",
        dateOfBirth = null,
        phone = null,
        visits = listOf(visit()),
        riskLevel = "Low Risk",
        ageText = "29",
        bloodGroupText = "O+",
    ).copy(
        allergyHistory = "Penicillin rash",
        medicalHistory = "Hypothyroidism",
        surgicalHistory = "Appendectomy 2015",
        obstetricNotes = "Elective repeat caesarean booked at 39 weeks",
        dating = dating("2026-11-08"),
        previousPregnancies = listOf(prevPreg()),
        medications = listOf(med()),
        ultrasoundScans = listOf(scanWithPhoto()),
    )

    private fun html(): String = PatientPrintHtmlGenerator.generateHtml(
        context = null,
        patient = fullPatient(),
        asOfDate = AS_OF_DATE,
    )

    /** HTML comments carry wording echoes but are never printed as text. */
    private fun withoutHtmlComments(html: String): String =
        Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(html, "")

    /** How many times the title text is the text of a title span. */
    private fun spanCount(html: String, title: String): Int =
        Regex.escape("""<span>$title</span>""").toRegex().findAll(html).count()

    @Test
    fun q94_noEnSub_anywhereInGeneratedHtml() {
        assertFalse(
            "the generated HTML still contains en-sub (span or CSS rule)",
            html().contains("en-sub"),
        )
    }

    @Test
    fun q94_everyCoreSectionTitle_appearsExactlyOnce() {
        val generated = html()
        for (title in TITLES) {
            assertEquals(
                "section title '$title' must be printed exactly once",
                1,
                spanCount(generated, title),
            )
        }
    }

    @Test
    fun q94_noCapitalTitleDuplicate_printedAsText() {
        val rendered = withoutHtmlComments(html())
        for (caps in CAPITAL_DUPLICATES) {
            assertFalse(
                "capital duplicate '$caps' must not be printed",
                rendered.contains(caps),
            )
        }
    }

    @Test
    fun q94_photoSectionTitle_appearsOnceWithScanPhoto() {
        val generated = html()
        assertEquals(
            "'$PHOTO_TITLE' must be printed exactly once when the patient has scan photos",
            1,
            spanCount(generated, PHOTO_TITLE),
        )
    }
}
