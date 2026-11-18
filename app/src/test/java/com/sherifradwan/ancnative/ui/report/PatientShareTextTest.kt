package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DeliveryNoteSections
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.composeDeliveryNotes
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientShareTextTest {

    private val asOf = LocalDate.of(2026, 11, 18)

    private fun samplePatient(
        id: String = "p1",
        caseId: String = "ANC-101",
        displayName: String = "Sarah Jenkins",
        riskLevel: String? = "high",
        clinicalAttention: String? = null,
        allergyHistory: String? = null,
        bloodGroupText: String? = null,
        dating: DemoPatientDating? = DemoPatientDating(
            patientId = id,
            lmpDate = "2026-05-01",
            ultrasoundEdd = null,
            agreedEdd = "2027-02-05",
            datingMethod = "LMP",
            notes = null,
        ),
        visits: List<DemoVisit> = listOf(
            DemoVisit(
                id = "v1",
                patientId = id,
                date = "2026-11-01",
                bloodPressure = "120/80",
                weight = "68.5",
                findings = null,
                notes = null,
            ),
        ),
        scans: List<DemoUltrasoundScan> = listOf(
            DemoUltrasoundScan(
                id = "s1",
                patientId = id,
                date = "2026-11-01",
                gestationalAgeWeeks = 26,
                gestationalAgeDays = 2,
                fetalNumber = "Singleton",
                presentation = "Cephalic",
                biometryBpd = "75 mm",
                biometryHc = "270 mm",
                biometryAc = "250 mm",
                biometryFl = "53 mm",
                estimatedFetalWeight = "1650 g",
                placentalSite = "Posterior",
                amnioticFluid = "Normal",
                fetalHeartRate = "142 bpm",
                impression = "Normal growth",
            ),
        ),
        medications: List<DemoMedicationRecord> = listOf(
            DemoMedicationRecord(
                id = "m1",
                patientId = id,
                name = "Methyldopa",
                dosage = "250 mg",
                route = "Oral",
                frequency = "TDS",
                startDate = "2026-10-01",
                endDate = null,
                status = "Active",
                notes = null,
                isProphylaxis = false,
            ),
        ),
        obstetricNotes: String? = "Hospital: City General\nPlan: Planned vaginal delivery",
        delivered: Boolean = false,
        deliveryDate: String? = null,
        deliveryMode: String? = null,
        deliveryOutcome: String? = null,
    ): DemoPatient = DemoPatient(
        id = id,
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = "1994-06-15",
        phone = "+201000000000",
        visits = visits,
        riskLevel = riskLevel,
        clinicalAttention = clinicalAttention,
        allergyHistory = allergyHistory,
        bloodGroupText = bloodGroupText,
        dating = dating,
        ultrasoundScans = scans,
        medications = medications,
        obstetricNotes = obstetricNotes,
        delivered = delivered,
        deliveryDate = deliveryDate,
        deliveryMode = deliveryMode,
        deliveryOutcome = deliveryOutcome,
    )

    @Test
    fun neverMoreThan15LinesForVeryLongName40MedsLongAttentionAndAllergies() {
        val veryLongName = "Dr. Jane Supercalifragilisticexpialidocious Doe\nSecond Line Maiden Name\nThird Line Alias"
        val longAttention = buildString {
            append("Critical: Severe Pre-eclampsia history (2022).\n")
            append("Recurrent pregnancy loss (x3).\n")
            append("Known allergy: Penicillin anaphylaxis (ICU admission 2019).\n")
            append("Severe asthma requiring steroid inhaler.\n")
            append("Gestational diabetes diet controlled.\n")
            append("Repeat CS planned at 38w due to prior classical uterine scar.\n")
        }
        val longAllergy = "Severe anaphylaxis to beta-lactams and sulfonamides. Contact dermatitis with latex."

        val fortyMeds = (1..40).map { i ->
            DemoMedicationRecord(
                id = "med-$i",
                patientId = "p1",
                name = "LongMedicationNameNumber$i",
                dosage = "500 mg",
                route = "Oral",
                frequency = "OD",
                startDate = "2026-10-01",
                endDate = null,
                status = "Active",
                notes = null,
                isProphylaxis = false,
            )
        }

        val patient = samplePatient(
            displayName = veryLongName,
            clinicalAttention = longAttention,
            allergyHistory = longAllergy,
            medications = fortyMeds,
        )

        val text = patientShareText(patient, asOf = asOf)
        val lineCount = text.lines().size

        assertTrue("Summary must never exceed 15 lines (was $lineCount lines):\n$text", lineCount <= 15)
        assertTrue(text.contains("+37 more") || text.contains("more"))
    }

    @Test
    fun containsAllRequiredClinicalFieldsWhenPopulated() {
        val patient = samplePatient(
            displayName = "Nour El-Din",
            caseId = "ANC-2026-441",
            riskLevel = "high",
            clinicalAttention = "High risk twin pregnancy",
            dating = DemoPatientDating(
                patientId = "p1",
                lmpDate = "2026-05-01",
                ultrasoundEdd = null,
                agreedEdd = "2027-02-05",
                datingMethod = "LMP",
                notes = null,
            ),
            visits = listOf(
                DemoVisit(
                    id = "v1",
                    patientId = "p1",
                    date = "2026-11-10",
                    bloodPressure = "135/85",
                    weight = "71.4",
                    findings = "Normal",
                    notes = null,
                ),
            ),
            scans = listOf(
                DemoUltrasoundScan(
                    id = "s1",
                    patientId = "p1",
                    date = "2026-11-10",
                    gestationalAgeWeeks = 27,
                    gestationalAgeDays = 4,
                    fetalNumber = "Singleton",
                    presentation = "Cephalic",
                    biometryBpd = "76 mm",
                    biometryHc = "274 mm",
                    biometryAc = "255 mm",
                    biometryFl = "54 mm",
                    estimatedFetalWeight = "1720 g",
                    placentalSite = "Fundal",
                    amnioticFluid = "Normal",
                    fetalHeartRate = "140",
                    impression = null,
                ),
            ),
            medications = listOf(
                DemoMedicationRecord(
                    id = "m1",
                    patientId = "p1",
                    name = "Labetalol",
                    dosage = "100 mg",
                    route = "Oral",
                    frequency = "BD",
                    startDate = "2026-11-01",
                    endDate = null,
                    status = "Active",
                    notes = null,
                    isProphylaxis = false,
                ),
                DemoMedicationRecord(
                    id = "m2",
                    patientId = "p1",
                    name = "Aspirin",
                    dosage = "150 mg",
                    route = "Oral",
                    frequency = "OD",
                    startDate = "2026-11-01",
                    endDate = null,
                    status = "Active",
                    notes = null,
                    isProphylaxis = true,
                ),
            ),
            obstetricNotes = composeDeliveryNotes(
                DeliveryNoteSections(
                    hospital = "Nile Valley Hospital",
                    fees = null,
                    plan = "Elective CS at 38w",
                    postpartum = null,
                ),
            ),
        )

        val text = patientShareText(patient, asOf = asOf)

        assertTrue(text.contains("Patient: Nour El-Din · ID: ANC-2026-441"))
        assertTrue(text.contains("Risk: High risk"))
        assertTrue(text.contains("Clinical attention: High risk twin pregnancy"))
        assertTrue(text.contains("GA:") && text.contains("EDD: 05/02/2027"))
        assertTrue(text.contains("Last visit: 10/11/2026 · BP 135/85 · Wt 71.4 kg"))
        assertTrue(text.contains("Latest scan: 10/11/2026 · Singleton, Cephalic · BPD 76 mm, HC 274 mm, AC 255 mm, FL 54 mm, EFW 1720 g"))
        assertTrue(text.contains("Current medications: Labetalol, Aspirin"))
        assertTrue(text.contains("Hospital: Nile Valley Hospital"))
        assertTrue(text.contains("Planned delivery: Elective CS at 38w"))
        assertTrue(text.lines().size <= 15)
    }

    @Test
    fun medicationNamesTruncateWithPlusNMoreWhenLong() {
        val meds = listOf(
            DemoMedicationRecord("m1", "p1", "Enoxaparin", "4000 IU", "SC", "OD", "2026-10-01", null, "Active", null, false),
            DemoMedicationRecord("m2", "p1", "Methyldopa", "250 mg", "Oral", "TDS", "2026-10-01", null, "Active", null, false),
            DemoMedicationRecord("m3", "p1", "Aspirin", "150 mg", "Oral", "OD", "2026-10-01", null, "Active", null, false),
            DemoMedicationRecord("m4", "p1", "Ferrous Fumarate", "200 mg", "Oral", "OD", "2026-10-01", null, "Active", null, false),
            DemoMedicationRecord("m5", "p1", "Calcium Carbonate", "500 mg", "Oral", "BD", "2026-10-01", null, "Active", null, false),
        )
        val patient = samplePatient(medications = meds)
        val text = patientShareText(patient, asOf = asOf)

        assertTrue(text.contains("Current medications: Enoxaparin, Methyldopa, Aspirin +2 more"))
    }

    @Test
    fun blankValuesAreSkippedWithoutEmptyLabels() {
        val patient = DemoPatient(
            id = "p-blank",
            caseId = "",
            displayName = "Only Name",
            dateOfBirth = null,
            phone = null,
            visits = emptyList(),
            riskLevel = null,
            clinicalAttention = null,
            allergyHistory = null,
            dating = null,
            ultrasoundScans = emptyList(),
            medications = emptyList(),
            obstetricNotes = null,
        )

        val text = patientShareText(patient, asOf = asOf)

        assertEquals("Patient: Only Name", text)
        assertFalse(text.contains("ID:"))
        assertFalse(text.contains("Risk:"))
        assertFalse(text.contains("Clinical attention:"))
        assertFalse(text.contains("GA:"))
        assertFalse(text.contains("EDD:"))
        assertFalse(text.contains("Last visit:"))
        assertFalse(text.contains("Latest scan:"))
        assertFalse(text.contains("Current medications:"))
    }

    @Test
    fun emptyPatientDoesNotCrash() {
        val empty = DemoPatient(
            id = "p0",
            caseId = "",
            displayName = "",
            dateOfBirth = null,
            phone = null,
            visits = emptyList(),
        )

        val text = patientShareText(empty, asOf = asOf)
        assertNotNull(text)
        assertTrue(text.isEmpty())
    }

    @Test
    fun allergiesAndClinicalAttentionPresentWhenRecordedForSafety() {
        val patientWithSafetyNotes = samplePatient(
            clinicalAttention = "ALLERGY: Anaphylactic reaction to Ceftriaxone & Penicillin · Strict NPO pre-op",
            allergyHistory = "Penicillin, Ceftriaxone",
        )

        val text = patientShareText(patientWithSafetyNotes, asOf = asOf)

        assertTrue("Clinical attention must be present for patient safety", text.contains("Clinical attention:"))
        assertTrue(
            "Allergy notes in clinical attention must appear in share text",
            text.contains("ALLERGY: Anaphylactic reaction to Ceftriaxone & Penicillin · Strict NPO pre-op"),
        )
    }

    @Test
    fun deliveredPatientOmitsGestationalAgeAndShowsDeliveryOutcome() {
        val deliveredPatient = samplePatient(
            delivered = true,
            deliveryDate = "2026-11-15",
            deliveryMode = "caesarean",
            deliveryOutcome = "live_birth",
        )

        val text = patientShareText(deliveredPatient, asOf = asOf)

        assertFalse("Delivered patient must not show ongoing GA", text.contains("GA:"))
        assertTrue("Delivered patient must show delivery outcome", text.contains("Delivery Outcome:"))
        assertTrue(text.contains("Delivered 15 Nov 2026 · Caesarean · Live birth"))
    }

    @Test
    fun allergiesLinePresentAndCompleteForLongAllergyTextDirectlyAfterRiskLine() {
        val longAllergy = "Severe anaphylaxis to penicillin, cephalosporins, sulfonamides, NSAIDs, latex, peanuts, and contrast media; carries auto-injector; prior ICU intubation for angioedema"
        val patient = samplePatient(
            riskLevel = "high",
            allergyHistory = longAllergy,
        )

        val text = patientShareText(patient, asOf = asOf)
        val lines = text.lines()

        val riskIndex = lines.indexOfFirst { it.startsWith("Risk:") }
        val allergiesIndex = lines.indexOfFirst { it.startsWith("Allergies:") }

        assertTrue("Risk line must be present in share text", riskIndex >= 0)
        assertTrue("Allergies line must be present in share text", allergiesIndex >= 0)
        assertEquals("Allergies line must appear immediately after Risk line", riskIndex + 1, allergiesIndex)
        assertEquals(
            "Allergies line must be complete and untruncated",
            "Allergies: $longAllergy",
            lines[allergiesIndex],
        )
        assertFalse(lines[allergiesIndex].contains("..."))
        assertFalse(lines[allergiesIndex].contains("more"))
    }

    @Test
    fun withLongName40MedsLongAttentionDeliveryRowsAndRhNegativeOutputAtMost15LinesAndPreservesSafetyRows() {
        val veryLongName = "Dr. Jane Supercalifragilisticexpialidocious Doe\nSecond Line Maiden Name\nThird Line Alias"
        val longAttention = buildString {
            append("Critical: Severe Pre-eclampsia history (2022).\n")
            append("Recurrent pregnancy loss (x3).\n")
            append("Known allergy: Penicillin anaphylaxis (ICU admission 2019).\n")
            append("Severe asthma requiring steroid inhaler.\n")
            append("Gestational diabetes diet controlled.\n")
            append("Repeat CS planned at 38w due to prior classical uterine scar.\n")
        }
        val longAllergy = "Severe anaphylaxis to beta-lactams and sulfonamides. Contact dermatitis with latex."

        val fortyMeds = (1..40).map { i ->
            DemoMedicationRecord(
                id = "med-$i",
                patientId = "p1",
                name = "LongMedicationNameNumber$i",
                dosage = "500 mg",
                route = "Oral",
                frequency = "OD",
                startDate = "2026-10-01",
                endDate = null,
                status = "Active",
                notes = null,
                isProphylaxis = false,
            )
        }

        val obstetricNotes = composeDeliveryNotes(
            DeliveryNoteSections(
                hospital = "City General Hospital - Main Maternity Wing",
                fees = null,
                plan = "Repeat elective Caesarean section at 38 weeks with consultant obstetrician",
                postpartum = "Strict postpartum hemorrhage protocol with uterine balloon on standby",
            ),
        )

        val patient = samplePatient(
            displayName = veryLongName,
            riskLevel = "high",
            clinicalAttention = longAttention,
            allergyHistory = longAllergy,
            bloodGroupText = "O Negative",
            medications = fortyMeds,
            obstetricNotes = obstetricNotes,
        )

        val text = patientShareText(patient, asOf = asOf)
        val lines = text.lines()

        assertTrue("Summary must never exceed 15 lines (was ${lines.size} lines):\n$text", lines.size <= 15)
        assertTrue("Must contain risk line", lines.any { it.startsWith("Risk:") })
        assertTrue("Must contain allergies line", lines.any { it.startsWith("Allergies:") })
        assertTrue("Allergies line must contain full allergy text", text.contains(longAllergy))
        assertTrue("Must contain clinical attention line", lines.any { it.startsWith("Clinical attention:") })
        assertTrue(
            "Must contain delivery precaution / blood bank precautions line",
            lines.any { it.startsWith("Blood bank precautions:") && it.contains("Anti-D") },
        )
    }
}
