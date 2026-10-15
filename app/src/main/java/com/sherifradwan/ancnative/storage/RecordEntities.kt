package com.sherifradwan.ancnative.storage

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "patients",
    indices = [Index(value = ["case_id"], unique = true)],
)
data class PatientEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "case_id") val caseId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: String?,
    val phone: String?,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "patient_status") val status: String? = null,
    @ColumnInfo(name = "archived_at") val archivedAt: String? = null,
    @ColumnInfo(name = "archived_reason") val archivedReason: String? = null,
    @ColumnInfo(name = "risk_level") val riskLevel: String? = null,
    @ColumnInfo(name = "age_text") val ageText: String? = null,
    @ColumnInfo(name = "address_text") val addressText: String? = null,
    @ColumnInfo(name = "blood_group_text") val bloodGroupText: String? = null,
    @ColumnInfo(name = "medical_history") val medicalHistory: String? = null,
    @ColumnInfo(name = "surgical_history") val surgicalHistory: String? = null,
    @ColumnInfo(name = "family_history") val familyHistory: String? = null,
    @ColumnInfo(name = "allergy_history") val allergyHistory: String? = null,
    val gravida: Long? = null,
    val parity: Long? = null,
    val abortion: Long? = null,
    @ColumnInfo(name = "obstetric_notes") val obstetricNotes: String? = null,
    @ColumnInfo(name = "clinical_attention") val clinicalAttention: String? = null,
    @ColumnInfo(defaultValue = "1") val revision: Long = 1,
)

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["patient_id"])],
)
data class VisitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "recorded_date") val date: String,
    @ColumnInfo(name = "blood_pressure") val bloodPressure: String?,
    val weight: String?,
    val findings: String?,
    val notes: String?,
    val revision: Int,
    @ColumnInfo(name = "is_synthetic_edit") val isSyntheticEdit: Boolean,
    @ColumnInfo(name = "linked_ultrasound_id") val linkedUltrasoundId: String? = null,
)

@Entity(tableName = "vault_metadata")
data class VaultMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "vault_uuid") val vaultUuid: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "is_synthetic") val isSynthetic: Boolean,
    val initialized: Boolean,
)

@Entity(
    tableName = "save_receipts",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visit_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["patient_id"]), Index(value = ["visit_id"])],
)
data class SaveReceiptEntity(
    @PrimaryKey
    @ColumnInfo(name = "operation_uuid") val operationUuid: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "visit_id") val visitId: String,
    @ColumnInfo(name = "committed_revision") val committedRevision: Int,
    val fingerprint: String,
)

@Entity(
    tableName = "previous_pregnancies",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["patient_id", "updated_at"]),
    ],
)
data class PreviousPregnancyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "model_version") val modelVersion: Int,
    @ColumnInfo(name = "date_text") val dateText: String?,
    @ColumnInfo(name = "outcome_text") val outcomeText: String?,
    @ColumnInfo(name = "pregnancy_type") val pregnancyType: String?,
    @ColumnInfo(name = "other_details") val otherDetails: String?,
    @ColumnInfo(name = "place_and_doctor") val placeAndDoctor: String?,
    @ColumnInfo(name = "delivery_by") val deliveryBy: String?,
    val outcome: String?,
    @ColumnInfo(name = "birth_method") val birthMethod: String?,
    @ColumnInfo(name = "loss_management") val lossManagement: String?,
    @ColumnInfo(name = "termination_method") val terminationMethod: String?,
    @ColumnInfo(name = "provenance_kind") val provenanceKind: String,
    @ColumnInfo(name = "clinician_id") val clinicianId: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    val revision: Long,
)

@Entity(
    tableName = "patient_command_receipts",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["record_kind", "record_id"]),
    ],
)
data class PatientCommandReceiptEntity(
    @PrimaryKey
    @ColumnInfo(name = "operation_uuid") val operationUuid: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "record_kind") val recordKind: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "committed_revision") val committedRevision: Long,
    @ColumnInfo(name = "committed_patient_revision") val committedPatientRevision: Long,
    val fingerprint: String,
)

@Entity(
    tableName = "patient_dating",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["patient_id"], unique = true)],
)
data class PatientDatingEntity(
    @PrimaryKey
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "lmp_date") val lmpDate: String?,
    @ColumnInfo(name = "ultrasound_edd") val ultrasoundEdd: String?,
    @ColumnInfo(name = "agreed_edd") val agreedEdd: String?,
    @ColumnInfo(name = "dating_method") val datingMethod: String?,
    val notes: String?,
    @ColumnInfo(name = "transfer_date") val transferDate: String? = null,
    @ColumnInfo(name = "embryo_age_days") val embryoAgeDays: Int? = null,
    val revision: Long,
)

@Entity(
    tableName = "ultrasound_scans",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["patient_id", "scan_date"]),
    ],
)
data class UltrasoundScanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "scan_date") val date: String,
    @ColumnInfo(name = "ga_weeks") val gaWeeks: Int?,
    @ColumnInfo(name = "ga_days") val gaDays: Int?,
    @ColumnInfo(name = "fetal_number") val fetalNumber: String?,
    val presentation: String?,
    @ColumnInfo(name = "placental_site") val placentalSite: String?,
    @ColumnInfo(name = "amniotic_fluid") val amnioticFluid: String?,
    @ColumnInfo(name = "fetal_heart_rate") val fetalHeartRate: String?,
    @ColumnInfo(name = "biometry_bpd") val biometryBpd: String?,
    @ColumnInfo(name = "biometry_hc") val biometryHc: String?,
    @ColumnInfo(name = "biometry_ac") val biometryAc: String?,
    @ColumnInfo(name = "biometry_fl") val biometryFl: String?,
    @ColumnInfo(name = "estimated_fetal_weight") val estimatedFetalWeight: String?,
    val impression: String?,
    @ColumnInfo(name = "linked_visit_id") val linkedVisitId: String? = null,
    @ColumnInfo(name = "image_uris") val imageUris: String? = null,
    val revision: Long,
)

@Entity(
    tableName = "lab_investigations",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["patient_id", "test_date"]),
    ],
)
data class LabInvestigationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "test_date") val date: String,
    @ColumnInfo(name = "test_category") val testCategory: String,
    @ColumnInfo(name = "test_name") val testName: String,
    val result: String,
    val unit: String?,
    @ColumnInfo(name = "reference_range") val referenceRange: String?,
    @ColumnInfo(name = "is_abnormal") val isAbnormal: Boolean,
    val notes: String?,
    @ColumnInfo(name = "lab_name") val labName: String? = null,
    val revision: Long,
)

@Entity(
    tableName = "medication_records",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["patient_id", "start_date"]),
    ],
)
data class MedicationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?,
    val status: String,
    val notes: String?,
    @ColumnInfo(name = "is_prophylaxis") val isProphylaxis: Boolean,
    @ColumnInfo(name = "is_non_compliant", defaultValue = "0") val isNonCompliant: Boolean = false,
    @ColumnInfo(name = "adverse_effects") val adverseEffects: String? = null,
    val revision: Long,
)

@Entity(
    tableName = "clinical_command_receipts",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["record_kind", "record_id"]),
    ],
)
data class ClinicalCommandReceiptEntity(
    @PrimaryKey
    @ColumnInfo(name = "operation_uuid") val operationUuid: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "record_kind") val recordKind: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "committed_revision") val committedRevision: Long,
    @ColumnInfo(name = "committed_patient_revision") val committedPatientRevision: Long,
    val fingerprint: String,
)

@Entity(
    tableName = "patient_attachments",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["patient_id"]),
        Index(value = ["patient_id", "category"]),
        Index(value = ["sha256_hash"]),
    ],
)
data class PatientAttachmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "patient_id") val patientId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/webp",
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "sha256_hash") val sha256Hash: String,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "captured_at") val capturedAt: String,
    @ColumnInfo(name = "linked_record_id") val linkedRecordId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "LOCAL_ONLY",
    @ColumnInfo(name = "cloud_url") val cloudUrl: String? = null,
    @ColumnInfo(name = "cloud_synced_at") val cloudSyncedAt: String? = null,
    val revision: Long = 1,
)

data class PatientWithVisits(
    @Embedded val patient: PatientEntity,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val visits: List<VisitEntity>,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val previousPregnancies: List<PreviousPregnancyEntity>,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val dating: PatientDatingEntity?,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val ultrasoundScans: List<UltrasoundScanEntity>,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val labInvestigations: List<LabInvestigationEntity>,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val medications: List<MedicationEntity>,
    @Relation(parentColumn = "id", entityColumn = "patient_id")
    val attachments: List<PatientAttachmentEntity> = emptyList(),
)

internal object ImageUriCodec {
    fun serialize(uris: List<String>): String? {
        if (uris.isEmpty()) return null
        return uris.joinToString("\n") { java.net.URLEncoder.encode(it, "UTF-8") }
    }

    fun deserialize(serialized: String?): List<String> {
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) null
            else runCatching { java.net.URLDecoder.decode(trimmed, "UTF-8") }.getOrDefault(trimmed)
        }
    }
}
