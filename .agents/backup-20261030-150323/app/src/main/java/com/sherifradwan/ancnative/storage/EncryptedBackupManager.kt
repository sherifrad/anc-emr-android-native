package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.PatientVocabulary
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.JvmName
import org.json.JSONArray
import org.json.JSONObject

sealed interface BackupInspectionResult {
    data class Valid(
        val vaultUuid: String,
        val patientCount: Int,
        val schemaVersion: Int,
        val exportedAtEpochMs: Long,
    ) : BackupInspectionResult
    data object InvalidPassphraseOrCorrupted : BackupInspectionResult
    data class Error(val reason: String) : BackupInspectionResult
}

data class CanonicalBackupPayload(
    val vaultUuid: String,
    val schemaVersion: Int,
    val exportedAtEpochMs: Long,
    val patients: List<PatientWithVisits>,
)

object EncryptedBackupManager {
    private const val MAGIC = "ANCVBK01"
    private const val SCHEMA_VERSION = 3
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_LEN_BITS = 128
    private const val PBKDF2_ITERATIONS = 65_536
    private const val KEY_LEN_BITS = 256

    @JvmName("createEncryptedBackupForDemoPatients")
    fun createEncryptedBackup(
        vaultUuid: String,
        patients: List<DemoPatient>,
        passphrase: CharArray,
        exportedAtEpochMs: Long = System.currentTimeMillis(),
    ): ByteArray {
        val envelope = canonicalEnvelope(
            vaultUuid = vaultUuid,
            exportedAtEpochMs = exportedAtEpochMs,
            patients = patients.toJsonArray { patient -> patient.toCanonicalJson() },
        )
        return encryptEnvelope(envelope, passphrase)
    }

    @JvmName("createEncryptedBackupForPatientTrees")
    fun createEncryptedBackup(
        vaultUuid: String,
        patients: List<PatientWithVisits>,
        passphrase: CharArray,
        exportedAtEpochMs: Long = System.currentTimeMillis(),
    ): ByteArray {
        val envelope = canonicalEnvelope(
            vaultUuid = vaultUuid,
            exportedAtEpochMs = exportedAtEpochMs,
            patients = patients.toJsonArray { patient -> patient.toCanonicalJson() },
        )
        return encryptEnvelope(envelope, passphrase)
    }

    fun decryptAndParseBackup(
        backupBytes: ByteArray,
        passphrase: CharArray,
    ): CanonicalBackupPayload? {
        return try {
            val envelope = decryptEnvelope(backupBytes, passphrase) ?: return null
            if (envelope.optString("magic") != MAGIC) return null
            if (envelope.optInt("schemaVersion") != SCHEMA_VERSION) return null
            val patients = envelope.optJSONArray("patients") ?: return null
            CanonicalBackupPayload(
                vaultUuid = envelope.getString("vaultUuid"),
                schemaVersion = envelope.getInt("schemaVersion"),
                exportedAtEpochMs = envelope.getLong("exportedAtEpochMs"),
                patients = patients.toObjectList { index -> getJSONObject(index).toPatientWithVisits() },
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun inspectBackup(
        backupBytes: ByteArray,
        passphrase: CharArray,
    ): BackupInspectionResult {
        val headerError = validateHeader(backupBytes)
        if (headerError != null) return headerError

        return try {
            val envelope = decryptEnvelope(backupBytes, passphrase)
                ?: return BackupInspectionResult.InvalidPassphraseOrCorrupted
            val patients = envelope.optJSONArray("patients")
                ?: return BackupInspectionResult.Error("Missing patients")
            BackupInspectionResult.Valid(
                vaultUuid = envelope.getString("vaultUuid"),
                patientCount = patients.length(),
                schemaVersion = envelope.getInt("schemaVersion"),
                exportedAtEpochMs = envelope.getLong("exportedAtEpochMs"),
            )
        } catch (_: AEADBadTagException) {
            BackupInspectionResult.InvalidPassphraseOrCorrupted
        } catch (t: Throwable) {
            BackupInspectionResult.Error(t.message ?: "Decryption error")
        }
    }

    private fun canonicalEnvelope(
        vaultUuid: String,
        exportedAtEpochMs: Long,
        patients: JSONArray,
    ): JSONObject = JSONObject().apply {
        put("magic", MAGIC)
        put("vaultUuid", vaultUuid)
        put("schemaVersion", SCHEMA_VERSION)
        put("exportedAtEpochMs", exportedAtEpochMs)
        put("patients", patients)
    }

    private fun encryptEnvelope(envelope: JSONObject, passphrase: CharArray): ByteArray {
        val plaintextBytes = envelope.toString().toByteArray(StandardCharsets.UTF_8)
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val derivedKey = deriveKey(passphrase, salt)

        val secretKey = SecretKeySpec(derivedKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LEN_BITS, iv))
        val magicBytes = MAGIC.toByteArray(StandardCharsets.US_ASCII)
        cipher.updateAAD(magicBytes)
        val ciphertext = cipher.doFinal(plaintextBytes)

        val buffer = ByteBuffer.allocate(magicBytes.size + SALT_LEN + IV_LEN + ciphertext.size)
        buffer.put(magicBytes)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    private fun decryptEnvelope(backupBytes: ByteArray, passphrase: CharArray): JSONObject? {
        val magicBytes = MAGIC.toByteArray(StandardCharsets.US_ASCII)
        val headerLen = magicBytes.size + SALT_LEN + IV_LEN
        if (backupBytes.size <= headerLen) return null

        val buffer = ByteBuffer.wrap(backupBytes)
        val readMagic = ByteArray(magicBytes.size)
        buffer.get(readMagic)
        if (!readMagic.contentEquals(magicBytes)) return null

        val salt = ByteArray(SALT_LEN)
        buffer.get(salt)
        val iv = ByteArray(IV_LEN)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val derivedKey = deriveKey(passphrase, salt)
        val secretKey = SecretKeySpec(derivedKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LEN_BITS, iv))
        cipher.updateAAD(magicBytes)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return JSONObject(String(decryptedBytes, StandardCharsets.UTF_8))
    }

    private fun validateHeader(backupBytes: ByteArray): BackupInspectionResult.Error? {
        val magicBytes = MAGIC.toByteArray(StandardCharsets.US_ASCII)
        val headerLen = magicBytes.size + SALT_LEN + IV_LEN
        if (backupBytes.size <= headerLen) {
            return BackupInspectionResult.Error("Payload too short")
        }
        val readMagic = backupBytes.copyOfRange(0, magicBytes.size)
        if (!readMagic.contentEquals(magicBytes)) {
            return BackupInspectionResult.Error("Invalid magic header")
        }
        return null
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LEN_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(keySpec).encoded
        } finally {
            keySpec.clearPassword()
        }
    }

    private fun PatientWithVisits.toCanonicalJson(): JSONObject {
        val p = patient
        return JSONObject().apply {
            put("id", p.id)
            put("caseId", p.caseId)
            put("displayName", p.displayName)
            putNullable("dateOfBirth", p.dateOfBirth)
            putNullable("phone", p.phone)
            putNullable("createdAt", p.createdAt)
            putNullable("status", p.status)
            putNullable("archivedAt", p.archivedAt)
            putNullable("archivedReason", p.archivedReason)
            putNullable("riskLevel", p.riskLevel)
            putNullable("ageText", p.ageText)
            putNullable("addressText", p.addressText)
            putNullable("bloodGroupText", p.bloodGroupText)
            putNullable("medicalHistory", p.medicalHistory)
            putNullable("surgicalHistory", p.surgicalHistory)
            putNullable("familyHistory", p.familyHistory)
            putNullable("allergyHistory", p.allergyHistory)
            putNullable("gravida", p.gravida)
            putNullable("parity", p.parity)
            putNullable("abortion", p.abortion)
            putNullable("obstetricNotes", p.obstetricNotes)
            putNullable("clinicalAttention", p.clinicalAttention)
            put("revision", p.revision)
            put("visits", visits.toJsonArray { it.toCanonicalJson() })
            put("previousPregnancies", previousPregnancies.toJsonArray { it.toCanonicalJson() })
            put("dating", dating?.toCanonicalJson() ?: JSONObject.NULL)
            put("ultrasoundScans", ultrasoundScans.toJsonArray { it.toCanonicalJson() })
            put("labInvestigations", labInvestigations.toJsonArray { it.toCanonicalJson() })
            put("medications", medications.toJsonArray { it.toCanonicalJson() })
            put("attachments", attachments.toJsonArray { it.toCanonicalJson() })
        }
    }

    private fun DemoPatient.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("caseId", caseId)
        put("displayName", displayName)
        putNullable("dateOfBirth", dateOfBirth)
        putNullable("phone", phone)
        putNullable("createdAt", createdAt)
        putNullable("status", status)
        putNullable("archivedAt", archivedAt)
        putNullable("archivedReason", archivedReason)
        putNullable("riskLevel", riskLevel)
        putNullable("ageText", ageText)
        putNullable("addressText", addressText)
        putNullable("bloodGroupText", bloodGroupText)
        putNullable("medicalHistory", medicalHistory)
        putNullable("surgicalHistory", surgicalHistory)
        putNullable("familyHistory", familyHistory)
        putNullable("allergyHistory", allergyHistory)
        putNullable("gravida", gravida)
        putNullable("parity", parity)
        putNullable("abortion", abortion)
        putNullable("obstetricNotes", obstetricNotes)
        putNullable("clinicalAttention", clinicalAttention)
        put("revision", revision)
        put("visits", visits.toJsonArray { visit ->
            JSONObject().apply {
                put("id", visit.id)
                put("date", visit.date)
                putNullable("bloodPressure", visit.bloodPressure)
                putNullable("weight", visit.weight)
                putNullable("findings", visit.findings)
                putNullable("notes", visit.notes)
                put("revision", visit.revision)
                put("isSyntheticEdit", visit.isSyntheticEdit)
                putNullable("linkedUltrasoundId", visit.linkedUltrasoundId)
            }
        })
        put("previousPregnancies", previousPregnancies.toJsonArray { pregnancy ->
            JSONObject().apply {
                put("id", pregnancy.id)
                put("patientId", pregnancy.patientId)
                put("modelVersion", pregnancy.modelVersion)
                putNullable("dateText", pregnancy.dateText)
                putNullable("outcomeText", pregnancy.outcomeText)
                putNullable("pregnancyType", pregnancy.pregnancyType)
                putNullable("otherDetails", pregnancy.otherDetails)
                putNullable("placeAndDoctor", pregnancy.placeAndDoctor)
                putNullable("deliveryBy", pregnancy.deliveryBy)
                putNullable("outcome", pregnancy.outcome)
                putNullable("birthMethod", pregnancy.birthMethod)
                putNullable("lossManagement", pregnancy.lossManagement)
                putNullable("terminationMethod", pregnancy.terminationMethod)
                putNullable("provenanceKind", pregnancy.provenanceKind)
                putNullable("clinicianId", pregnancy.clinicianId)
                putNullable("recordedAt", pregnancy.recordedAt)
                putNullable("updatedAt", pregnancy.updatedAt)
                put("revision", pregnancy.revision)
            }
        })
        put("dating", dating?.let { dating ->
            JSONObject().apply {
                put("patientId", dating.patientId)
                putNullable("lmpDate", dating.lmpDate)
                put("lmpCertainty", JSONObject.NULL)
                put("ultrasoundScanDate", JSONObject.NULL)
                put("ultrasoundGestationalWeeks", JSONObject.NULL)
                put("ultrasoundGestationalDays", JSONObject.NULL)
                putNullable("ivfTransferDate", dating.transferDate)
                putNullable("ivfEmbryoAgeDays", dating.embryoAgeDays)
                putNullable("edd", dating.agreedEdd ?: dating.ultrasoundEdd)
                putNullable("datingMethod", dating.datingMethod)
                putNullable("notes", dating.notes)
                put("decisionConfirmedAt", JSONObject.NULL)
                put("decisionClinicianId", JSONObject.NULL)
                put("revision", dating.revision)
            }
        } ?: JSONObject.NULL)
        put("ultrasoundScans", ultrasoundScans.toJsonArray { scan ->
            JSONObject().apply {
                put("id", scan.id)
                put("patientId", scan.patientId)
                put("date", scan.date)
                putNullable("gestationalAgeWeeks", scan.gestationalAgeWeeks)
                putNullable("gestationalAgeDays", scan.gestationalAgeDays)
                putNullable("fetalNumber", scan.fetalNumber)
                putNullable("presentation", scan.presentation)
                putNullable("placentalSite", scan.placentalSite)
                putNullable("amnioticFluid", scan.amnioticFluid)
                putNullable("fetalHeartRate", scan.fetalHeartRate)
                putNullable("biometryBpd", scan.biometryBpd)
                putNullable("biometryHc", scan.biometryHc)
                putNullable("biometryAc", scan.biometryAc)
                putNullable("biometryFl", scan.biometryFl)
                putNullable("estimatedFetalWeight", scan.estimatedFetalWeight)
                putNullable("impression", scan.impression)
                putNullable("linkedVisitId", scan.linkedVisitId)
                put("imageUris", JSONArray(scan.imageUris))
                put("revision", scan.revision)
            }
        })
        put("labInvestigations", labInvestigations.toJsonArray { lab ->
            JSONObject().apply {
                put("id", lab.id)
                put("patientId", lab.patientId)
                put("date", lab.date)
                put("testCategory", lab.testCategory)
                put("testName", lab.testName)
                put("result", lab.result)
                putNullable("unit", lab.unit)
                putNullable("referenceRange", lab.referenceRange)
                put("isAbnormal", lab.isAbnormal)
                putNullable("notes", lab.notes)
                putNullable("labName", lab.labName)
                put("revision", lab.revision)
            }
        })
        put("medications", medications.toJsonArray { medication ->
            JSONObject().apply {
                put("id", medication.id)
                put("patientId", medication.patientId)
                put("name", medication.name)
                putNullable("dosage", medication.dosage)
                putNullable("route", medication.route)
                putNullable("frequency", medication.frequency)
                putNullable("startDate", medication.startDate)
                putNullable("endDate", medication.endDate)
                put("status", medication.status)
                putNullable("notes", medication.notes)
                put("isProphylaxis", medication.isProphylaxis)
                put("isNonCompliant", medication.isNonCompliant)
                putNullable("adverseEffects", medication.adverseNotes)
                put("revision", medication.revision)
            }
        })
        put("attachments", attachments.toJsonArray { attachment ->
            JSONObject().apply {
                put("id", attachment.id)
                put("patientId", attachment.patientId)
                put("fileName", attachment.fileName)
                put("category", attachment.category)
                put("mimeType", attachment.mimeType)
                put("fileSizeBytes", attachment.fileSizeBytes)
                put("sha256Hash", attachment.sha256Hash)
                putNullable("title", attachment.title)
                put("capturedAt", attachment.capturedAt)
                put("thumbnailDataUrl", JSONObject.NULL)
                put("dataUrl", JSONObject.NULL)
                put("revision", attachment.revision)
            }
        })
    }

    private fun VisitEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("date", date)
        putNullable("bloodPressure", bloodPressure)
        putNullable("weight", weight)
        putNullable("findings", findings)
        putNullable("notes", notes)
        put("revision", revision)
        put("isSyntheticEdit", isSyntheticEdit)
        putNullable("linkedUltrasoundId", linkedUltrasoundId)
    }

    private fun PreviousPregnancyEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("modelVersion", modelVersion)
        putNullable("dateText", dateText)
        putNullable("outcomeText", outcomeText)
        putNullable("pregnancyType", pregnancyType)
        putNullable("otherDetails", otherDetails)
        putNullable("placeAndDoctor", placeAndDoctor)
        putNullable("deliveryBy", deliveryBy)
        putNullable("outcome", outcome)
        putNullable("birthMethod", birthMethod)
        putNullable("lossManagement", lossManagement)
        putNullable("terminationMethod", terminationMethod)
        putNullable("provenanceKind", provenanceKind)
        putNullable("clinicianId", clinicianId)
        putNullable("recordedAt", recordedAt)
        putNullable("updatedAt", updatedAt)
        put("revision", revision)
    }

    private fun PatientDatingEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("patientId", patientId)
        putNullable("lmpDate", lmpDate)
        put("lmpCertainty", JSONObject.NULL)
        put("ultrasoundScanDate", JSONObject.NULL)
        put("ultrasoundGestationalWeeks", JSONObject.NULL)
        put("ultrasoundGestationalDays", JSONObject.NULL)
        putNullable("ivfTransferDate", transferDate)
        putNullable("ivfEmbryoAgeDays", embryoAgeDays)
        putNullable("edd", agreedEdd ?: ultrasoundEdd)
        putNullable("datingMethod", datingMethod)
        putNullable("notes", notes)
        put("decisionConfirmedAt", JSONObject.NULL)
        put("decisionClinicianId", JSONObject.NULL)
        put("revision", revision)
    }

    private fun UltrasoundScanEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("date", date)
        putNullable("gestationalAgeWeeks", gaWeeks)
        putNullable("gestationalAgeDays", gaDays)
        putNullable("fetalNumber", fetalNumber)
        putNullable("presentation", presentation)
        putNullable("placentalSite", placentalSite)
        putNullable("amnioticFluid", amnioticFluid)
        putNullable("fetalHeartRate", fetalHeartRate)
        putNullable("biometryBpd", biometryBpd)
        putNullable("biometryHc", biometryHc)
        putNullable("biometryAc", biometryAc)
        putNullable("biometryFl", biometryFl)
        putNullable("estimatedFetalWeight", estimatedFetalWeight)
        putNullable("impression", impression)
        putNullable("linkedVisitId", linkedVisitId)
        put("imageUris", JSONArray(ImageUriCodec.deserialize(imageUris)))
        put("revision", revision)
    }

    private fun LabInvestigationEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("date", date)
        put("testCategory", testCategory)
        put("testName", testName)
        put("result", result)
        putNullable("unit", unit)
        putNullable("referenceRange", referenceRange)
        put("isAbnormal", isAbnormal)
        putNullable("notes", notes)
        putNullable("labName", labName)
        put("revision", revision)
    }

    private fun MedicationEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("name", name)
        putNullable("dosage", dosage)
        putNullable("route", route)
        putNullable("frequency", frequency)
        putNullable("startDate", startDate)
        putNullable("endDate", endDate)
        put("status", status)
        putNullable("notes", notes)
        put("isProphylaxis", isProphylaxis)
        put("isNonCompliant", isNonCompliant)
        putNullable("adverseEffects", adverseEffects)
        put("revision", revision)
    }

    private fun PatientAttachmentEntity.toCanonicalJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("fileName", fileName)
        put("category", category)
        put("mimeType", mimeType)
        put("fileSizeBytes", fileSizeBytes)
        put("sha256Hash", sha256Hash)
        putNullable("title", title)
        put("capturedAt", capturedAt)
        put("thumbnailDataUrl", JSONObject.NULL)
        put("dataUrl", JSONObject.NULL)
        put("revision", revision)
    }

    private fun JSONObject.toPatientWithVisits(): PatientWithVisits {
        val patientId = getString("id")
        return PatientWithVisits(
            patient = PatientEntity(
                id = patientId,
                caseId = getString("caseId"),
                displayName = getString("displayName"),
                dateOfBirth = optNullableString("dateOfBirth"),
                phone = optNullableString("phone"),
                createdAt = optNullableString("createdAt"),
                status = optNullableString("status"),
                archivedAt = optNullableString("archivedAt"),
                archivedReason = optNullableString("archivedReason"),
                riskLevel = optNullableString("riskLevel"),
                ageText = optNullableString("ageText"),
                addressText = optNullableString("addressText"),
                bloodGroupText = optNullableString("bloodGroupText"),
                medicalHistory = optNullableString("medicalHistory"),
                surgicalHistory = optNullableString("surgicalHistory"),
                familyHistory = optNullableString("familyHistory"),
                allergyHistory = optNullableString("allergyHistory"),
                gravida = optNullableLong("gravida"),
                parity = optNullableLong("parity"),
                abortion = optNullableLong("abortion"),
                obstetricNotes = optNullableString("obstetricNotes"),
                clinicalAttention = optNullableString("clinicalAttention"),
                revision = optLong("revision", 1),
            ),
            visits = optJSONArray("visits").toObjectListOrEmpty { index ->
                getJSONObject(index).toVisitEntity(patientId)
            },
            previousPregnancies = optJSONArray("previousPregnancies").toObjectListOrEmpty { index ->
                getJSONObject(index).toPreviousPregnancyEntity(patientId)
            },
            dating = if (isNull("dating")) null else optJSONObject("dating")?.toPatientDatingEntity(patientId),
            ultrasoundScans = optJSONArray("ultrasoundScans").toObjectListOrEmpty { index ->
                getJSONObject(index).toUltrasoundScanEntity(patientId)
            },
            labInvestigations = optJSONArray("labInvestigations").toObjectListOrEmpty { index ->
                getJSONObject(index).toLabInvestigationEntity(patientId)
            },
            medications = optJSONArray("medications").toObjectListOrEmpty { index ->
                getJSONObject(index).toMedicationEntity(patientId)
            },
            attachments = optJSONArray("attachments").toObjectListOrEmpty { index ->
                getJSONObject(index).toPatientAttachmentEntity(patientId)
            },
        )
    }

    private fun JSONObject.toVisitEntity(patientId: String): VisitEntity = VisitEntity(
        id = getString("id"),
        patientId = patientId,
        date = getString("date"),
        bloodPressure = optNullableString("bloodPressure"),
        weight = optNullableString("weight"),
        findings = optNullableString("findings"),
        notes = optNullableString("notes"),
        revision = optInt("revision", 1),
        isSyntheticEdit = optBoolean("isSyntheticEdit", false),
        linkedUltrasoundId = optNullableString("linkedUltrasoundId"),
    )

    private fun JSONObject.toPreviousPregnancyEntity(parentPatientId: String): PreviousPregnancyEntity =
        PreviousPregnancyEntity(
            id = getString("id"),
            patientId = optNullableString("patientId") ?: parentPatientId,
            modelVersion = optInt("modelVersion", 3),
            dateText = optNullableString("dateText"),
            outcomeText = optNullableString("outcomeText"),
            pregnancyType = optNullableString("pregnancyType"),
            otherDetails = optNullableString("otherDetails"),
            placeAndDoctor = optNullableString("placeAndDoctor"),
            deliveryBy = optNullableString("deliveryBy"),
            outcome = optNullableString("outcome"),
            birthMethod = optNullableString("birthMethod"),
            lossManagement = optNullableString("lossManagement"),
            terminationMethod = optNullableString("terminationMethod"),
            provenanceKind = optNullableString("provenanceKind").takeIf { !it.isNullOrBlank() } ?: PatientVocabulary.CLINICIAN_ENTRY,
            clinicianId = optNullableString("clinicianId").takeIf { !it.isNullOrBlank() } ?: "clinician-import",
            recordedAt = optNullableString("recordedAt").orEmpty(),
            updatedAt = optNullableString("updatedAt").orEmpty(),
            revision = optLong("revision", 1),
        )

    private fun JSONObject.toPatientDatingEntity(parentPatientId: String): PatientDatingEntity {
        val edd = optNullableString("edd")
        return PatientDatingEntity(
            patientId = optNullableString("patientId") ?: parentPatientId,
            lmpDate = optNullableString("lmpDate"),
            ultrasoundEdd = edd,
            agreedEdd = edd,
            datingMethod = optNullableString("datingMethod"),
            notes = optNullableString("notes"),
            transferDate = optNullableString("ivfTransferDate"),
            embryoAgeDays = optNullableInt("ivfEmbryoAgeDays"),
            revision = optLong("revision", 1),
        )
    }

    private fun JSONObject.toUltrasoundScanEntity(parentPatientId: String): UltrasoundScanEntity =
        UltrasoundScanEntity(
            id = getString("id"),
            patientId = optNullableString("patientId") ?: parentPatientId,
            date = getString("date"),
            gaWeeks = optNullableInt("gestationalAgeWeeks"),
            gaDays = optNullableInt("gestationalAgeDays"),
            fetalNumber = optNullableString("fetalNumber"),
            presentation = optNullableString("presentation"),
            placentalSite = optNullableString("placentalSite"),
            amnioticFluid = optNullableString("amnioticFluid"),
            fetalHeartRate = optNullableString("fetalHeartRate"),
            biometryBpd = optNullableString("biometryBpd"),
            biometryHc = optNullableString("biometryHc"),
            biometryAc = optNullableString("biometryAc"),
            biometryFl = optNullableString("biometryFl"),
            estimatedFetalWeight = optNullableString("estimatedFetalWeight"),
            impression = optNullableString("impression"),
            linkedVisitId = optNullableString("linkedVisitId"),
            imageUris = ImageUriCodec.serialize(optJSONArray("imageUris").toStringListOrEmpty()),
            revision = optLong("revision", 1),
        )

    private fun JSONObject.toLabInvestigationEntity(parentPatientId: String): LabInvestigationEntity =
        LabInvestigationEntity(
            id = getString("id"),
            patientId = optNullableString("patientId") ?: parentPatientId,
            date = getString("date"),
            testCategory = getString("testCategory"),
            testName = getString("testName"),
            result = getString("result"),
            unit = optNullableString("unit"),
            referenceRange = optNullableString("referenceRange"),
            isAbnormal = optBoolean("isAbnormal", false),
            notes = optNullableString("notes"),
            labName = optNullableString("labName"),
            revision = optLong("revision", 1),
        )

    private fun JSONObject.toMedicationEntity(parentPatientId: String): MedicationEntity = MedicationEntity(
        id = getString("id"),
        patientId = optNullableString("patientId") ?: parentPatientId,
        name = getString("name"),
        dosage = optNullableString("dosage").orEmpty(),
        route = optNullableString("route").orEmpty(),
        frequency = optNullableString("frequency").orEmpty(),
        startDate = optNullableString("startDate").orEmpty(),
        endDate = optNullableString("endDate"),
        status = optNullableString("status").orEmpty(),
        notes = optNullableString("notes"),
        isProphylaxis = optBoolean("isProphylaxis", false),
        isNonCompliant = optBoolean("isNonCompliant", false),
        adverseEffects = optNullableString("adverseEffects"),
        revision = optLong("revision", 1),
    )

    private fun JSONObject.toPatientAttachmentEntity(parentPatientId: String): PatientAttachmentEntity =
        PatientAttachmentEntity(
            id = getString("id"),
            patientId = optNullableString("patientId") ?: parentPatientId,
            fileName = getString("fileName"),
            filePath = optNullableString("dataUrl").orEmpty(),
            category = getString("category"),
            mimeType = optNullableString("mimeType") ?: "image/webp",
            fileSizeBytes = optLong("fileSizeBytes", 0),
            sha256Hash = getString("sha256Hash"),
            title = optNullableString("title"),
            capturedAt = getString("capturedAt"),
            revision = optLong("revision", 1),
        )

    private fun JSONObject.putNullable(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) optString(name) else null

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (has(name) && !isNull(name)) optLong(name) else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private inline fun <T> Iterable<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        forEach { array.put(mapper(it)) }
        return array
    }

    private inline fun <T> JSONArray.toObjectList(mapper: JSONArray.(Int) -> T): List<T> =
        List(length()) { index -> mapper(index) }

    private inline fun <T> JSONArray?.toObjectListOrEmpty(mapper: JSONArray.(Int) -> T): List<T> =
        this?.toObjectList(mapper).orEmpty()

    private fun JSONArray?.toStringListOrEmpty(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> getString(index) }
    }
}
