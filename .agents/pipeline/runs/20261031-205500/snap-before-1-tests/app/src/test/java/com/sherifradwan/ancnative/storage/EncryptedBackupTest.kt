package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.ClinicalSummaryGenerator
import com.sherifradwan.ancnative.model.DemoFixtures
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupTest {
    private val passphrase = "StrongClinicalPassphrase#2026".toCharArray()
    private val wrongPassphrase = "WrongPassphrase123".toCharArray()
    private val vaultUuid = UUID.randomUUID().toString()

    @Test
    fun createAndInspectBackupWithCorrectPassphraseSucceeds() {
        val patients = DemoFixtures.patients
        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = patients,
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )

        assertTrue(backupBytes.isNotEmpty())
        val result = EncryptedBackupManager.inspectBackup(backupBytes, passphrase)
        assertTrue(result is BackupInspectionResult.Valid)
        val valid = result as BackupInspectionResult.Valid
        assertEquals(vaultUuid, valid.vaultUuid)
        assertEquals(patients.size, valid.patientCount)
        assertEquals(3, valid.schemaVersion)
        assertEquals(1775890000000L, valid.exportedAtEpochMs)
    }

    @Test
    fun inspectBackupWithWrongPassphraseFailsClosed() {
        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = DemoFixtures.patients,
            passphrase = passphrase,
        )

        val result = EncryptedBackupManager.inspectBackup(backupBytes, wrongPassphrase)
        assertTrue(result is BackupInspectionResult.InvalidPassphraseOrCorrupted)
    }

    @Test
    fun inspectBackupWithTamperedCiphertextFailsClosed() {
        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = DemoFixtures.patients,
            passphrase = passphrase,
        )
        val tamperedBytes = backupBytes.clone()
        tamperedBytes[tamperedBytes.size - 5] = (tamperedBytes[tamperedBytes.size - 5].toInt() xor 0xFF).toByte()

        val result = EncryptedBackupManager.inspectBackup(tamperedBytes, passphrase)
        assertTrue(result is BackupInspectionResult.InvalidPassphraseOrCorrupted)
    }

    @Test
    fun createAndParsePatientWithVisitsBackupRoundTripsCanonicalTree() {
        val patientId = "patient-canonical-1"
        val imageUris = listOf("content://scan/one", "file:///tmp/scan two.webp")
        val patientTree = PatientWithVisits(
            patient = PatientEntity(
                id = patientId,
                caseId = "ANC-2026-001",
                displayName = "Canonical Test Patient",
                dateOfBirth = "1993-04-12",
                phone = "+201001112223",
                createdAt = "2026-10-14T01:00:00Z",
                status = "active",
                archivedAt = null,
                archivedReason = null,
                riskLevel = "High Risk",
                ageText = "33 years",
                addressText = "Cairo",
                bloodGroupText = "O+",
                medicalHistory = "GDM",
                surgicalHistory = "Appendectomy",
                familyHistory = "Diabetes",
                allergyHistory = "Penicillin",
                gravida = 3,
                parity = 1,
                abortion = 1,
                obstetricNotes = "Prior CS",
                clinicalAttention = "Needs close follow-up",
                revision = 7,
            ),
            visits = listOf(
                VisitEntity(
                    id = "visit-1",
                    patientId = patientId,
                    date = "2026-10-14",
                    bloodPressure = "130/85",
                    weight = "72 kg",
                    findings = "Stable",
                    notes = "Review in one week",
                    revision = 3,
                    isSyntheticEdit = true,
                    linkedUltrasoundId = "scan-1",
                ),
            ),
            previousPregnancies = listOf(
                PreviousPregnancyEntity(
                    id = "preg-1",
                    patientId = patientId,
                    modelVersion = 2,
                    dateText = "2022",
                    outcomeText = "Live birth",
                    pregnancyType = "Singleton",
                    otherDetails = "No complications",
                    placeAndDoctor = "Clinic A",
                    deliveryBy = "CS",
                    outcome = "LiveBirth",
                    birthMethod = "Cesarean",
                    lossManagement = null,
                    terminationMethod = null,
                    provenanceKind = "CLINICIAN_ENTRY",
                    clinicianId = "clinician-1",
                    recordedAt = "2026-10-14T01:01:00Z",
                    updatedAt = "2026-10-14T01:02:00Z",
                    revision = 4,
                ),
            ),
            dating = PatientDatingEntity(
                patientId = patientId,
                lmpDate = "2026-02-13",
                ultrasoundEdd = "2026-11-20",
                agreedEdd = "2026-11-20",
                datingMethod = "Ultrasound",
                notes = "Confirmed by first trimester scan",
                transferDate = "2026-02-18",
                embryoAgeDays = 5,
                revision = 5,
            ),
            ultrasoundScans = listOf(
                UltrasoundScanEntity(
                    id = "scan-1",
                    patientId = patientId,
                    date = "2026-05-08",
                    gaWeeks = 12,
                    gaDays = 3,
                    fetalNumber = "Singleton",
                    presentation = "Cephalic",
                    placentalSite = "Anterior",
                    amnioticFluid = "Normal",
                    fetalHeartRate = "150",
                    biometryBpd = "21 mm",
                    biometryHc = "80 mm",
                    biometryAc = "65 mm",
                    biometryFl = "10 mm",
                    estimatedFetalWeight = "58 g",
                    impression = "Viable fetus",
                    linkedVisitId = "visit-1",
                    imageUris = ImageUriCodec.serialize(imageUris),
                    revision = 6,
                ),
            ),
            labInvestigations = listOf(
                LabInvestigationEntity(
                    id = "lab-1",
                    patientId = patientId,
                    date = "2026-10-10",
                    testCategory = "Hematology",
                    testName = "Hb",
                    result = "10.8",
                    unit = "g/dL",
                    referenceRange = "11-13",
                    isAbnormal = true,
                    notes = "Mild anemia",
                    labName = "ANC Lab",
                    revision = 2,
                ),
            ),
            medications = listOf(
                MedicationEntity(
                    id = "med-1",
                    patientId = patientId,
                    name = "Iron",
                    dosage = "100 mg",
                    route = "Oral",
                    frequency = "Daily",
                    startDate = "2026-10-10",
                    endDate = null,
                    status = "Active",
                    notes = "After meals",
                    isProphylaxis = false,
                    isNonCompliant = true,
                    adverseEffects = "Nausea",
                    revision = 3,
                ),
            ),
            attachments = listOf(
                PatientAttachmentEntity(
                    id = "att-1",
                    patientId = patientId,
                    fileName = "scan.webp",
                    filePath = "/local/scan.webp",
                    category = "ULTRASOUND",
                    mimeType = "image/webp",
                    fileSizeBytes = 2048,
                    sha256Hash = "abc123",
                    title = "Dating scan",
                    capturedAt = "2026-05-08T10:00:00Z",
                    revision = 2,
                ),
            ),
        )

        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = listOf(patientTree),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )

        val payload = EncryptedBackupManager.decryptAndParseBackup(backupBytes, passphrase)
        assertTrue(payload != null)
        assertEquals(vaultUuid, payload!!.vaultUuid)
        assertEquals(3, payload.schemaVersion)
        assertEquals(1775890000000L, payload.exportedAtEpochMs)
        assertEquals(1, payload.patients.size)

        val parsed = payload.patients.single()
        assertEquals(patientTree.patient, parsed.patient)
        assertEquals(patientTree.visits.single(), parsed.visits.single())
        assertEquals(patientTree.previousPregnancies.single(), parsed.previousPregnancies.single())
        assertEquals("2026-11-20", parsed.dating?.agreedEdd)
        assertEquals(patientTree.dating?.lmpDate, parsed.dating?.lmpDate)
        assertEquals(patientTree.dating?.embryoAgeDays, parsed.dating?.embryoAgeDays)
        assertEquals(imageUris, ImageUriCodec.deserialize(parsed.ultrasoundScans.single().imageUris))
        assertEquals(patientTree.labInvestigations.single(), parsed.labInvestigations.single())
        assertEquals(patientTree.medications.single(), parsed.medications.single())
        assertEquals("att-1", parsed.attachments.single().id)
        assertEquals("", parsed.attachments.single().filePath)
        assertEquals("abc123", parsed.attachments.single().sha256Hash)
    }

    @Test
    fun fullCaseFromBookingToDeliveryExportsAndRestoresEncryptedBackupWithZeroLoss() {
        val maya = DemoFixtures.patients.first { it.caseId == "DEMO-ANC-001" }
        assertEquals(10, maya.visits.size)
        assertEquals(4, maya.ultrasoundScans.size)
        assertEquals(6, maya.labInvestigations.size)
        assertEquals(4, maya.medications.size)
        assertEquals(1, maya.previousPregnancies.size)

        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = listOf(maya),
            passphrase = passphrase,
            exportedAtEpochMs = 1775895000000L,
        )

        assertTrue(backupBytes.isNotEmpty())

        val inspected = EncryptedBackupManager.inspectBackup(backupBytes, passphrase)
        assertTrue(inspected is BackupInspectionResult.Valid)
        val valid = inspected as BackupInspectionResult.Valid
        assertEquals(1, valid.patientCount)
        assertEquals(3, valid.schemaVersion)

        val restored = EncryptedBackupManager.decryptAndParseBackup(backupBytes, passphrase)
        org.junit.Assert.assertNotNull(restored)
        val restoredPatient = restored!!.patients.single()

        assertEquals("Maya Example", restoredPatient.patient.displayName)
        assertEquals("DEMO-ANC-001", restoredPatient.patient.caseId)
        assertEquals("O+", restoredPatient.patient.bloodGroupText)
        assertEquals(10, restoredPatient.visits.size)
        assertEquals(4, restoredPatient.ultrasoundScans.size)
        assertEquals(6, restoredPatient.labInvestigations.size)
        assertEquals(4, restoredPatient.medications.size)
        assertEquals(1, restoredPatient.previousPregnancies.size)
        assertEquals("2026-01-10", restoredPatient.dating?.lmpDate)
        assertEquals("2026-10-17", restoredPatient.dating?.agreedEdd)

        // Verify Delivery notes in obstetricNotes
        assertTrue(restoredPatient.patient.obstetricNotes?.contains("Spontaneous Vaginal Delivery") == true)
        assertTrue(restoredPatient.patient.obstetricNotes?.contains("3.35 kg") == true)
    }

    @Test
    fun clinicalSummaryGeneratorOutputsComprehensiveText() {
        val patient = DemoFixtures.patients.first()
        val text = ClinicalSummaryGenerator.generateTextSummary(patient)

        assertTrue(text.contains("ANTENATAL CARE CLINICAL SUMMARY"))
        assertTrue(text.contains(patient.displayName))
        assertTrue(text.contains(patient.caseId))
        assertTrue(text.contains("PREGNANCY DATING"))
        assertTrue(text.contains("RECENT VISITS"))
        assertTrue(text.contains("ULTRASOUND SCANS"))
        assertTrue(text.contains("LABORATORY INVESTIGATIONS"))
        assertTrue(text.contains("MEDICATIONS & PRESCRIPTIONS"))
    }
}
