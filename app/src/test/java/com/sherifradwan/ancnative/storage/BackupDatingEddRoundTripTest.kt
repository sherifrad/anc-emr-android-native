package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.datingSummary
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupDatingEddRoundTripTest {
    private val passphrase = "StrongClinicalPassphrase#2026".toCharArray()
    private val patientId = "patient-dating-1"

    private fun tree(dating: PatientDatingEntity?) = PatientWithVisits(
        patient = PatientEntity(
            id = patientId,
            caseId = "ANC-DATING-1",
            displayName = "Dating Round Trip",
            dateOfBirth = null,
            phone = null,
        ),
        visits = emptyList(),
        previousPregnancies = emptyList(),
        dating = dating,
        ultrasoundScans = emptyList(),
        labInvestigations = emptyList(),
        medications = emptyList(),
    )

    private fun dating(method: String?, scanEdd: String?, agreedEdd: String?) = PatientDatingEntity(
        patientId = patientId,
        lmpDate = "2026-02-13",
        ultrasoundEdd = scanEdd,
        agreedEdd = agreedEdd,
        datingMethod = method,
        notes = null,
        revision = 1,
    )

    private fun roundTrip(dating: PatientDatingEntity): PatientDatingEntity {
        val bytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = "vault-dating",
            patients = listOf(tree(dating)),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )
        val payload = EncryptedBackupManager.decryptAndParseBackup(bytes, passphrase)
        assertNotNull(payload)
        return payload!!.patients.single().dating!!
    }

    @Test
    fun lmpPatientWithScanEddAndNoAgreedEddKeepsAgreedNullAndDiscordance() {
        val restored = roundTrip(dating("LMP", scanEdd = "2026-12-11", agreedEdd = null))

        assertNull(restored.agreedEdd)
        assertEquals("2026-12-11", restored.ultrasoundEdd)

        val summary = datingSummary(
            DemoPatientDating(
                patientId = restored.patientId,
                lmpDate = restored.lmpDate,
                ultrasoundEdd = restored.ultrasoundEdd,
                agreedEdd = restored.agreedEdd,
                datingMethod = restored.datingMethod,
                notes = restored.notes,
            ),
            earliestScanDate = LocalDate.of(2026, 5, 8),
            today = LocalDate.of(2026, 5, 8),
        )
        assertNotNull("discordance must survive a backup round trip", summary.discordance)
    }

    @Test
    fun agreedOnlyDatingKeepsScanEddNull() {
        val restored = roundTrip(dating("LMP", scanEdd = null, agreedEdd = "2026-11-20"))

        assertNull(restored.ultrasoundEdd)
        assertEquals("2026-11-20", restored.agreedEdd)
    }

    @Test
    fun differentScanAndAgreedEddsBothSurvive() {
        val restored = roundTrip(dating("Ultrasound", scanEdd = "2026-12-11", agreedEdd = "2026-11-20"))

        assertEquals("2026-12-11", restored.ultrasoundEdd)
        assertEquals("2026-11-20", restored.agreedEdd)
    }

    @Test
    fun bothNullStayBothNull() {
        val restored = roundTrip(dating("LMP", scanEdd = null, agreedEdd = null))

        assertNull(restored.ultrasoundEdd)
        assertNull(restored.agreedEdd)
    }

    @Test
    fun oldFormatBackupWithOnlyEddRestoresBothFieldsFromEdd() {
        val bytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = "vault-dating",
            patients = listOf(tree(dating("LMP", scanEdd = "2026-12-11", agreedEdd = "2026-11-20"))),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )
        val oldFormat = rewriteDatingAsOldFormat(bytes, edd = "2026-11-20")

        val payload = EncryptedBackupManager.decryptAndParseBackup(oldFormat, passphrase)
        assertNotNull(payload)
        val restored = payload!!.patients.single().dating!!
        assertEquals("2026-11-20", restored.ultrasoundEdd)
        assertEquals("2026-11-20", restored.agreedEdd)
    }

    /** No public seam exists for old-format files, so reuse the manager's own private crypto by reflection. */
    private fun rewriteDatingAsOldFormat(bytes: ByteArray, edd: String): ByteArray {
        val manager = EncryptedBackupManager::class.java
        val decrypt = manager.getDeclaredMethod("decryptEnvelope", ByteArray::class.java, CharArray::class.java)
            .apply { isAccessible = true }
        val encrypt = manager.getDeclaredMethod("encryptEnvelope", JSONObject::class.java, CharArray::class.java)
            .apply { isAccessible = true }
        val envelope = decrypt.invoke(EncryptedBackupManager, bytes, passphrase) as JSONObject
        val dating = envelope.getJSONArray("patients").getJSONObject(0).getJSONObject("dating")
        dating.remove("ultrasoundEdd")
        dating.remove("agreedEdd")
        dating.put("edd", edd)
        return encrypt.invoke(EncryptedBackupManager, envelope, passphrase) as ByteArray
    }

    // Production export goes through the DemoPatient overload (RecordSnapshot.patients).
    private fun demoRoundTrip(scanEdd: String?, agreedEdd: String?): PatientDatingEntity {
        val demo = DemoPatient(
            id = patientId,
            caseId = "ANC-DATING-1",
            displayName = "Dating Round Trip",
            dateOfBirth = null,
            phone = null,
            visits = emptyList(),
            dating = DemoPatientDating(
                patientId = patientId,
                lmpDate = "2026-02-13",
                ultrasoundEdd = scanEdd,
                agreedEdd = agreedEdd,
                datingMethod = "LMP",
                notes = null,
            ),
        )
        val bytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = "vault-dating",
            patients = listOf(demo),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )
        return EncryptedBackupManager.decryptAndParseBackup(bytes, passphrase)!!.patients.single().dating!!
    }

    @Test
    fun demoPathKeepsScanEddWithNoAgreedEdd() {
        val restored = demoRoundTrip(scanEdd = "2026-12-11", agreedEdd = null)
        assertEquals("2026-12-11", restored.ultrasoundEdd)
        assertNull(restored.agreedEdd)
    }

    @Test
    fun demoPathKeepsDifferentScanAndAgreedEdds() {
        val restored = demoRoundTrip(scanEdd = "2026-12-11", agreedEdd = "2026-11-20")
        assertEquals("2026-12-11", restored.ultrasoundEdd)
        assertEquals("2026-11-20", restored.agreedEdd)
    }

    @Test
    fun demoPathAgreedOnlyGainsNoScanEdd() {
        val restored = demoRoundTrip(scanEdd = null, agreedEdd = "2026-11-20")
        assertNull(restored.ultrasoundEdd)
        assertEquals("2026-11-20", restored.agreedEdd)
    }
}
