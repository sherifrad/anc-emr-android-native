package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupVaultIntegrationAndZeroDataLossTest {

    private val masterPassphrase = "DrSherifRadwanClinicSecureVault#2026".toCharArray()
    private val wrongPassphrase = "IncorrectPassphrase#999".toCharArray()
    private val vaultUuid = UUID.randomUUID().toString()

    @Test
    fun exportAndRestoreAllDemoPatientsZeroDataLoss() {
        val originalPatients = DemoFixtures.patients
        assertEquals(4, originalPatients.size)

        // 1. Export entire patient population to encrypted AES-256 GCM backup
        val exportTime = System.currentTimeMillis()
        val encryptedBackupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = originalPatients,
            passphrase = masterPassphrase,
            exportedAtEpochMs = exportTime,
        )

        assertTrue(encryptedBackupBytes.isNotEmpty())
        assertTrue("Ciphertext must exceed minimum envelope overhead", encryptedBackupBytes.size > 200)

        // 2. Inspect with correct passphrase
        val inspection = EncryptedBackupManager.inspectBackup(encryptedBackupBytes, masterPassphrase)
        assertTrue(inspection is BackupInspectionResult.Valid)
        val valid = inspection as BackupInspectionResult.Valid
        assertEquals(vaultUuid, valid.vaultUuid)
        assertEquals(originalPatients.size, valid.patientCount)
        assertEquals(3, valid.schemaVersion)
        assertEquals(exportTime, valid.exportedAtEpochMs)

        // 3. Inspect with incorrect passphrase fails closed
        val wrongInspection = EncryptedBackupManager.inspectBackup(encryptedBackupBytes, wrongPassphrase)
        assertTrue(wrongInspection is BackupInspectionResult.InvalidPassphraseOrCorrupted)

        // 4. Decrypt and parse full canonical payload
        val payload = EncryptedBackupManager.decryptAndParseBackup(encryptedBackupBytes, masterPassphrase)
        assertNotNull("Payload must decrypt cleanly", payload)
        assertEquals(vaultUuid, payload!!.vaultUuid)
        assertEquals(3, payload.schemaVersion)
        assertEquals(exportTime, payload.exportedAtEpochMs)
        assertEquals(originalPatients.size, payload.patients.size)

        // 5. Verify Zero Data Loss across all patients and child relations
        val repository = RecordsRepository(
            database = createDummyRecordsDatabase(),
            vaultUuid = vaultUuid,
        )

        with(repository) {
            for (original in originalPatients) {
                val restoredTree = payload.patients.first { it.patient.caseId == original.caseId }

                // Patient Profile
                val restoredPatient = restoredTree.patient.toDomain(
                    visits = restoredTree.visits.map { it.toDomain() },
                    pregnancies = restoredTree.previousPregnancies.map { it.toDomain() },
                    dating = restoredTree.dating?.toDomain(),
                    ultrasoundScans = restoredTree.ultrasoundScans.map { it.toDomain() },
                    labInvestigations = restoredTree.labInvestigations.map { it.toDomain() },
                    medications = restoredTree.medications.map { it.toDomain() },
                    attachments = restoredTree.attachments.map { it.toDomain() },
                )

                assertEquals(original.caseId, restoredPatient.caseId)
                assertEquals(original.displayName, restoredPatient.displayName)
                assertEquals(original.phone, restoredPatient.phone)
                assertEquals(original.bloodGroupText, restoredPatient.bloodGroupText)
                assertEquals(original.riskLevel, restoredPatient.riskLevel)
                assertEquals(original.gravida, restoredPatient.gravida)
                assertEquals(original.parity, restoredPatient.parity)
                assertEquals(original.abortion, restoredPatient.abortion)
                assertEquals(original.medicalHistory, restoredPatient.medicalHistory)
                assertEquals(original.surgicalHistory, restoredPatient.surgicalHistory)
                assertEquals(original.familyHistory, restoredPatient.familyHistory)
                assertEquals(original.allergyHistory, restoredPatient.allergyHistory)

                // Pregnancy Dating
                if (original.dating != null) {
                    assertNotNull("Dating must not be lost", restoredPatient.dating)
                    assertEquals(original.dating!!.lmpDate, restoredPatient.dating!!.lmpDate)
                    assertEquals(original.dating!!.ultrasoundEdd, restoredPatient.dating!!.ultrasoundEdd)
                    assertEquals(original.dating!!.agreedEdd, restoredPatient.dating!!.agreedEdd)
                    assertEquals(original.dating!!.datingMethod, restoredPatient.dating!!.datingMethod)
                }

                // Visits
                assertEquals(original.visits.size, restoredPatient.visits.size)
                for (idx in original.visits.indices) {
                    val origV = original.visits[idx]
                    val restV = restoredPatient.visits.first { it.id == origV.id }
                    assertEquals(origV.date, restV.date)
                    assertEquals(origV.bloodPressure, restV.bloodPressure)
                    assertEquals(origV.weight, restV.weight)
                    assertEquals(origV.findings, restV.findings)
                    assertEquals(origV.notes, restV.notes)
                }

                // Ultrasound Scans
                assertEquals(original.ultrasoundScans.size, restoredPatient.ultrasoundScans.size)
                for (idx in original.ultrasoundScans.indices) {
                    val origS = original.ultrasoundScans[idx]
                    val restS = restoredPatient.ultrasoundScans.first { it.id == origS.id }
                    assertEquals(origS.date, restS.date)
                    assertEquals(origS.gestationalAgeWeeks, restS.gestationalAgeWeeks)
                    assertEquals(origS.gestationalAgeDays, restS.gestationalAgeDays)
                    assertEquals(origS.fetalNumber, restS.fetalNumber)
                    assertEquals(origS.presentation, restS.presentation)
                    assertEquals(origS.placentalSite, restS.placentalSite)
                    assertEquals(origS.amnioticFluid, restS.amnioticFluid)
                    assertEquals(origS.fetalHeartRate, restS.fetalHeartRate)
                    assertEquals(origS.estimatedFetalWeight, restS.estimatedFetalWeight)
                    assertEquals(origS.biometryBpd, restS.biometryBpd)
                    assertEquals(origS.biometryHc, restS.biometryHc)
                    assertEquals(origS.biometryAc, restS.biometryAc)
                    assertEquals(origS.biometryFl, restS.biometryFl)
                    assertEquals(origS.impression, restS.impression)
                    assertEquals(origS.imageUris, restS.imageUris)
                }

                // Lab Investigations
                assertEquals(original.labInvestigations.size, restoredPatient.labInvestigations.size)
                for (idx in original.labInvestigations.indices) {
                    val origL = original.labInvestigations[idx]
                    val restL = restoredPatient.labInvestigations.first { it.id == origL.id }
                    assertEquals(origL.testName, restL.testName)
                    assertEquals(origL.testCategory, restL.testCategory)
                    assertEquals(origL.result, restL.result)
                    assertEquals(origL.unit, restL.unit)
                    assertEquals(origL.referenceRange, restL.referenceRange)
                    assertEquals(origL.isAbnormal, restL.isAbnormal)
                }

                // Medications
                assertEquals(original.medications.size, restoredPatient.medications.size)
                for (idx in original.medications.indices) {
                    val origM = original.medications[idx]
                    val restM = restoredPatient.medications.first { it.id == origM.id }
                    assertEquals(origM.name, restM.name)
                    assertEquals(origM.dosage, restM.dosage)
                    assertEquals(origM.route, restM.route)
                    assertEquals(origM.frequency, restM.frequency)
                    assertEquals(origM.isProphylaxis, restM.isProphylaxis)
                }

                // Previous Pregnancies
                assertEquals(original.previousPregnancies.size, restoredPatient.previousPregnancies.size)
                for (idx in original.previousPregnancies.indices) {
                    val origP = original.previousPregnancies[idx]
                    val restP = restoredPatient.previousPregnancies.first { it.id == origP.id }
                    assertEquals(origP.dateText, restP.dateText)
                    assertEquals(origP.outcome, restP.outcome)
                    assertEquals(origP.birthMethod, restP.birthMethod)
                    assertEquals(origP.placeAndDoctor, restP.placeAndDoctor)
                }
            }
        }
    }

    @Test
    fun encryptedBackupSecurityTamperingBitFlipFailsClosed() {
        val encryptedBackupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = DemoFixtures.patients,
            passphrase = masterPassphrase,
        )

        // Flip byte in ciphertext payload
        val corruptedBytes = encryptedBackupBytes.clone()
        val targetIndex = corruptedBytes.size - 10
        corruptedBytes[targetIndex] = (corruptedBytes[targetIndex].toInt() xor 0x01).toByte()

        val result = EncryptedBackupManager.inspectBackup(corruptedBytes, masterPassphrase)
        assertTrue(
            "Tampered AEAD ciphertext must fail closed as InvalidPassphraseOrCorrupted",
            result is BackupInspectionResult.InvalidPassphraseOrCorrupted,
        )

        val parsed = EncryptedBackupManager.decryptAndParseBackup(corruptedBytes, masterPassphrase)
        assertTrue("Corrupted payload must never decrypt", parsed == null)
    }

    private fun createDummyRecordsDatabase(): RecordsDatabase {
        val dummyDao = java.lang.reflect.Proxy.newProxyInstance(
            RecordsDao::class.java.classLoader,
            arrayOf(RecordsDao::class.java),
        ) { _, _, _ -> null } as RecordsDao

        return object : RecordsDatabase() {
            override fun recordsDao(): RecordsDao = dummyDao
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = error("Not needed")
            override fun createInvalidationTracker() = error("Not needed")
            @Deprecated("Deprecated in RoomDatabase")
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun clearAllTables() {}
        }
    }
}
