package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Round 3x: writes one full sample report to app/build/print-sample/report.html so the
 * layout can be checked by eye in a browser (A4 width and 380 px). Fails only if
 * generation or writing throws.
 */
class PrintSampleDumpTest {

    private fun date(i: Int): String = LocalDate.of(2026, 3, 1).plusDays(i.toLong() * 9).toString()

    private val longValue = "Unbrokenverylongfindingtextwithoutanyspaces".repeat(3)

    private fun patient(): DemoPatient {
        val visits = (0 until 15).map { i ->
            DemoVisit(
                id = "dump-visit-$i", date = date(i),
                bloodPressure = if (i == 7) "165/105" else "120/80", weight = "68.5",
                findings = if (i == 3) longValue else "Fundal height appropriate, fetal movements felt",
                notes = null, revision = 1, isSyntheticEdit = false,
            )
        }
        val scans = (0 until 9).map { i ->
            DemoUltrasoundScan(
                id = "dump-scan-$i", patientId = "dump-1", date = date(i),
                gestationalAgeWeeks = 12 + i, gestationalAgeDays = 3, fetalNumber = null,
                presentation = "Cephalic", placentalSite = "Anterior", amnioticFluid = "Normal",
                fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
                biometryFl = null, estimatedFetalWeight = "${1000 + i * 100}",
                impression = if (i == 0) longValue else "Normal growth",
            )
        }
        val labs = (0 until 12).map { i ->
            DemoLabInvestigation(
                id = "dump-lab-$i", patientId = "dump-1", date = date(i), testCategory = "Blood",
                testName = "Haemoglobin $i", result = "11.$i", unit = "g/dL",
                referenceRange = "11.0 - 15.0", isAbnormal = i == 2, notes = null,
            )
        }
        val meds = (0 until 10).map { i ->
            DemoMedicationRecord(
                id = "dump-med-$i", patientId = "dump-1", name = "Medication $i", dosage = "500 mg",
                route = "PO", frequency = "Twice daily", startDate = "2026-01-01", endDate = null,
                status = "active", notes = if (i == 0) longValue else "after meals", isProphylaxis = i % 2 == 0,
            )
        }
        val previous = (0 until 3).map { i ->
            DemoPreviousPregnancy(
                id = "dump-prev-$i", patientId = "dump-1", modelVersion = 1, dateText = "${2015 + i}",
                outcomeText = null, pregnancyType = null,
                otherDetails = if (i == 0) longValue else "unremarkable",
                placeAndDoctor = "City Maternity Hospital, Dr Abdelrahman", deliveryBy = null,
                outcome = "LiveBirth", birthMethod = null, lossManagement = null,
                terminationMethod = null, provenanceKind = "t", clinicianId = "c",
                recordedAt = "2026-01-01", updatedAt = "2026-01-01", revision = 1,
            )
        }
        return DemoPatient(
            id = "dump-1",
            caseId = "DUMP-1",
            displayName = "Sample Layout Patient",
            dateOfBirth = null,
            phone = null,
            visits = visits,
            riskLevel = "High Risk",
            ageText = "29",
            bloodGroupText = "O+",
        ).copy(
            allergyHistory = "Penicillin rash",
            medicalHistory = "Hypothyroidism",
            surgicalHistory = "Appendectomy 2015",
            obstetricNotes = "Elective repeat caesarean booked at 39 weeks",
            dating = DemoPatientDating(
                patientId = "dump-1", lmpDate = "2026-02-01", ultrasoundEdd = null,
                agreedEdd = "2026-11-08", datingMethod = "LMP", notes = null,
            ),
            previousPregnancies = previous,
            medications = meds,
            ultrasoundScans = scans,
            labInvestigations = labs,
        )
    }

    @Test
    fun writeSampleReport() {
        val html = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = patient(),
            asOfDate = LocalDate.of(2026, 10, 31),
        )
        val work = File(System.getProperty("user.dir") ?: ".")
        val buildDir = if (work.name == "app") File(work, "build") else File(work, "app/build")
        val out = File(buildDir, "print-sample")
        out.mkdirs()
        File(out, "report.html").writeText(html)
    }
}
