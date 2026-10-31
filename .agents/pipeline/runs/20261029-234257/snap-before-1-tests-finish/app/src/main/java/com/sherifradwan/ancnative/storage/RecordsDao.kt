package com.sherifradwan.ancnative.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RecordsDao {
    @Transaction
    @Query("SELECT * FROM patients ORDER BY case_id")
    suspend fun loadPatientsWithVisits(): List<PatientWithVisits>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun patient(patientId: String): PatientEntity?

    @Query("SELECT * FROM visits WHERE id = :visitId LIMIT 1")
    suspend fun visit(visitId: String): VisitEntity?

    @Query("SELECT * FROM previous_pregnancies WHERE id = :pregnancyId LIMIT 1")
    suspend fun previousPregnancy(pregnancyId: String): PreviousPregnancyEntity?

    @Query("SELECT * FROM vault_metadata LIMIT 2")
    suspend fun metadataRows(): List<VaultMetadataEntity>

    @Query("SELECT * FROM save_receipts WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun receipt(operationUuid: String): SaveReceiptEntity?

    @Query("SELECT * FROM patient_command_receipts WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun patientCommandReceipt(operationUuid: String): PatientCommandReceiptEntity?

    @Query(
        "SELECT COUNT(*) FROM patients WHERE case_id = :caseId AND (:excludingPatientId IS NULL OR id != :excludingPatientId)",
    )
    suspend fun caseIdCount(caseId: String, excludingPatientId: String?): Int

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun patientCount(): Int

    @Query("SELECT COUNT(*) FROM visits")
    suspend fun visitCount(): Int

    @Query("SELECT COUNT(*) FROM vault_metadata")
    suspend fun metadataCount(): Int

    @Query("SELECT COUNT(*) FROM save_receipts")
    suspend fun receiptCount(): Int

    @Query("SELECT COUNT(*) FROM previous_pregnancies")
    suspend fun previousPregnancyCount(): Int

    @Query("SELECT COUNT(*) FROM patient_command_receipts")
    suspend fun patientCommandReceiptCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMetadata(metadata: VaultMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: SaveReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPreviousPregnancy(pregnancy: PreviousPregnancyEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatientCommandReceipt(receipt: PatientCommandReceiptEntity)

    @Query(
        """
        UPDATE visits
        SET recorded_date = :date,
            blood_pressure = :bloodPressure,
            weight = :weight,
            findings = :findings,
            notes = :notes,
            linked_ultrasound_id = :linkedUltrasoundId,
            revision = revision + 1,
            is_synthetic_edit = 1
        WHERE id = :visitId
          AND patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updateVisit(
        visitId: String,
        patientId: String,
        expectedRevision: Int,
        date: String,
        bloodPressure: String?,
        weight: String?,
        findings: String?,
        notes: String?,
        linkedUltrasoundId: String? = null,
    ): Int

    @Query("UPDATE ultrasound_scans SET linked_visit_id = :visitId, revision = revision + 1 WHERE id = :scanId AND patient_id = :patientId")
    suspend fun linkUltrasoundToVisit(scanId: String, visitId: String, patientId: String): Int

    @Query("UPDATE ultrasound_scans SET linked_visit_id = NULL, revision = revision + 1 WHERE linked_visit_id = :visitId AND id != :exceptScanId")
    suspend fun unlinkUltrasoundFromVisit(visitId: String, exceptScanId: String = ""): Int

    @Query("UPDATE visits SET linked_ultrasound_id = :scanId, revision = revision + 1 WHERE id = :visitId AND patient_id = :patientId")
    suspend fun linkVisitToUltrasound(visitId: String, scanId: String, patientId: String): Int

    @Query("UPDATE visits SET linked_ultrasound_id = NULL, revision = revision + 1 WHERE linked_ultrasound_id = :scanId AND id != :exceptVisitId")
    suspend fun unlinkVisitFromUltrasound(scanId: String, exceptVisitId: String = ""): Int

    @Query(
        """
        UPDATE patients
        SET case_id = :caseId,
            display_name = :displayName,
            date_of_birth = :dateOfBirth,
            phone = :phone,
            risk_level = :riskLevel,
            age_text = :ageText,
            address_text = :addressText,
            blood_group_text = :bloodGroupText,
            gravida = :gravida,
            parity = :parity,
            abortion = :abortion,
            obstetric_notes = :obstetricNotes,
            clinical_attention = :clinicalAttention,
            revision = revision + 1
        WHERE id = :patientId
          AND revision = :expectedRevision
          AND (patient_status IS NULL OR patient_status != 'archived')
        """,
    )
    suspend fun updatePatientProfile(
        patientId: String,
        expectedRevision: Long,
        caseId: String,
        displayName: String,
        dateOfBirth: String?,
        phone: String?,
        riskLevel: String?,
        ageText: String?,
        addressText: String?,
        bloodGroupText: String?,
        gravida: Long?,
        parity: Long?,
        abortion: Long?,
        obstetricNotes: String?,
        clinicalAttention: String? = null,
    ): Int

    @Query(
        """
        UPDATE patients
        SET medical_history = :medicalHistory,
            surgical_history = :surgicalHistory,
            family_history = :familyHistory,
            allergy_history = :allergyHistory,
            revision = revision + 1
        WHERE id = :patientId
          AND revision = :expectedRevision
          AND (patient_status IS NULL OR patient_status != 'archived')
        """,
    )
    suspend fun updateOtherHistories(
        patientId: String,
        expectedRevision: Long,
        medicalHistory: String?,
        surgicalHistory: String?,
        familyHistory: String?,
        allergyHistory: String?,
    ): Int

    @Query(
        """
        UPDATE patients
        SET patient_status = 'archived',
            archived_at = :changedAt,
            archived_reason = :reason,
            revision = revision + 1
        WHERE id = :patientId
          AND revision = :expectedRevision
          AND (patient_status IS NULL OR patient_status != 'archived')
        """,
    )
    suspend fun archivePatient(
        patientId: String,
        expectedRevision: Long,
        changedAt: String,
        reason: String,
    ): Int

    @Query(
        """
        UPDATE patients
        SET patient_status = 'active',
            archived_at = NULL,
            archived_reason = NULL,
            revision = revision + 1
        WHERE id = :patientId
          AND revision = :expectedRevision
          AND patient_status = 'archived'
        """,
    )
    suspend fun unarchivePatient(patientId: String, expectedRevision: Long): Int

    @Query(
        """
        UPDATE patients
        SET revision = revision + 1
        WHERE id = :patientId
          AND revision = :expectedRevision
          AND (patient_status IS NULL OR patient_status != 'archived')
        """,
    )
    suspend fun incrementPatientRevision(patientId: String, expectedRevision: Long): Int

    @Query(
        """
        UPDATE previous_pregnancies
        SET date_text = :dateText,
            outcome_text = :outcomeText,
            pregnancy_type = :pregnancyType,
            other_details = :otherDetails,
            place_and_doctor = :placeAndDoctor,
            delivery_by = :deliveryBy,
            outcome = :outcome,
            birth_method = :birthMethod,
            loss_management = :lossManagement,
            termination_method = :terminationMethod,
            provenance_kind = :provenanceKind,
            clinician_id = :clinicianId,
            updated_at = :updatedAt,
            revision = revision + 1
        WHERE id = :pregnancyId
          AND patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updatePreviousPregnancy(
        pregnancyId: String,
        patientId: String,
        expectedRevision: Long,
        dateText: String?,
        outcomeText: String?,
        pregnancyType: String?,
        otherDetails: String?,
        placeAndDoctor: String?,
        deliveryBy: String?,
        outcome: String?,
        birthMethod: String?,
        lossManagement: String?,
        terminationMethod: String?,
        provenanceKind: String,
        clinicianId: String,
        updatedAt: String,
    ): Int

    @Query("SELECT * FROM patient_dating WHERE patient_id = :patientId LIMIT 1")
    suspend fun patientDating(patientId: String): PatientDatingEntity?

    @Query("SELECT * FROM ultrasound_scans WHERE id = :scanId LIMIT 1")
    suspend fun ultrasoundScan(scanId: String): UltrasoundScanEntity?

    @Query("SELECT * FROM lab_investigations WHERE id = :labId LIMIT 1")
    suspend fun labInvestigation(labId: String): LabInvestigationEntity?

    @Query("SELECT * FROM medication_records WHERE id = :medicationId LIMIT 1")
    suspend fun medication(medicationId: String): MedicationEntity?

    @Query("SELECT * FROM clinical_command_receipts WHERE operation_uuid = :operationUuid LIMIT 1")
    suspend fun clinicalCommandReceipt(operationUuid: String): ClinicalCommandReceiptEntity?

    @Query("SELECT COUNT(*) FROM patient_dating")
    suspend fun patientDatingCount(): Int

    @Query("SELECT COUNT(*) FROM ultrasound_scans")
    suspend fun ultrasoundScanCount(): Int

    @Query("SELECT COUNT(*) FROM lab_investigations")
    suspend fun labInvestigationCount(): Int

    @Query("SELECT COUNT(*) FROM medication_records")
    suspend fun medicationCount(): Int

    @Query("SELECT COUNT(*) FROM clinical_command_receipts")
    suspend fun clinicalCommandReceiptCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatientDating(dating: PatientDatingEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUltrasoundScan(scan: UltrasoundScanEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLabInvestigation(lab: LabInvestigationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMedication(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertClinicalCommandReceipt(receipt: ClinicalCommandReceiptEntity)

    @Query(
        """
        UPDATE patient_dating
        SET lmp_date = :lmpDate,
            ultrasound_edd = :ultrasoundEdd,
            agreed_edd = :agreedEdd,
            dating_method = :datingMethod,
            transfer_date = :transferDate,
            embryo_age_days = :embryoAgeDays,
            notes = :notes,
            revision = revision + 1
        WHERE patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updatePatientDating(
        patientId: String,
        expectedRevision: Long,
        lmpDate: String?,
        ultrasoundEdd: String?,
        agreedEdd: String?,
        datingMethod: String?,
        transferDate: String?,
        embryoAgeDays: Int?,
        notes: String?,
    ): Int

    @Query(
        """
        UPDATE ultrasound_scans
        SET scan_date = :date,
            ga_weeks = :gaWeeks,
            ga_days = :gaDays,
            fetal_number = :fetalNumber,
            presentation = :presentation,
            placental_site = :placentalSite,
            amniotic_fluid = :amnioticFluid,
            fetal_heart_rate = :fetalHeartRate,
            biometry_bpd = :biometryBpd,
            biometry_hc = :biometryHc,
            biometry_ac = :biometryAc,
            biometry_fl = :biometryFl,
            estimated_fetal_weight = :estimatedFetalWeight,
            impression = :impression,
            linked_visit_id = :linkedVisitId,
            image_uris = :imageUris,
            revision = revision + 1
        WHERE id = :scanId
          AND patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updateUltrasoundScan(
        scanId: String,
        patientId: String,
        expectedRevision: Long,
        date: String,
        gaWeeks: Int?,
        gaDays: Int?,
        fetalNumber: String?,
        presentation: String?,
        placentalSite: String?,
        amnioticFluid: String?,
        fetalHeartRate: String?,
        biometryBpd: String?,
        biometryHc: String?,
        biometryAc: String?,
        biometryFl: String?,
        estimatedFetalWeight: String?,
        impression: String?,
        linkedVisitId: String? = null,
        imageUris: String? = null,
    ): Int

    @Query(
        """
        UPDATE lab_investigations
        SET test_date = :date,
            test_category = :testCategory,
            test_name = :testName,
            result = :result,
            unit = :unit,
            reference_range = :referenceRange,
            is_abnormal = :isAbnormal,
            notes = :notes,
            lab_name = :labName,
            revision = revision + 1
        WHERE id = :labId
          AND patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updateLabInvestigation(
        labId: String,
        patientId: String,
        expectedRevision: Long,
        date: String,
        testCategory: String,
        testName: String,
        result: String,
        unit: String?,
        referenceRange: String?,
        isAbnormal: Boolean,
        notes: String?,
        labName: String? = null,
    ): Int

    @Query(
        """
        UPDATE medication_records
        SET name = :name,
            dosage = :dosage,
            route = :route,
            frequency = :frequency,
            start_date = :startDate,
            end_date = :endDate,
            status = :status,
            notes = :notes,
            is_prophylaxis = :isProphylaxis,
            is_non_compliant = :isNonCompliant,
            adverse_effects = :adverseEffects,
            revision = revision + 1
        WHERE id = :medicationId
          AND patient_id = :patientId
          AND revision = :expectedRevision
        """,
    )
    suspend fun updateMedication(
        medicationId: String,
        patientId: String,
        expectedRevision: Long,
        name: String,
        dosage: String,
        route: String,
        frequency: String,
        startDate: String,
        endDate: String?,
        status: String,
        notes: String?,
        isProphylaxis: Boolean,
        isNonCompliant: Boolean = false,
        adverseEffects: String? = null,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttachment(attachment: PatientAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttachments(attachments: List<PatientAttachmentEntity>)

    @Query("SELECT * FROM patient_attachments WHERE patient_id = :patientId ORDER BY captured_at DESC")
    suspend fun attachmentsForPatient(patientId: String): List<PatientAttachmentEntity>

    @Query("SELECT * FROM patient_attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun attachment(attachmentId: String): PatientAttachmentEntity?

    @Query("DELETE FROM patient_attachments WHERE id = :attachmentId AND patient_id = :patientId")
    suspend fun deleteAttachment(attachmentId: String, patientId: String): Int

    @Query("SELECT COUNT(*) FROM patient_attachments")
    suspend fun attachmentCount(): Int

    @Query(
        """
        UPDATE patient_attachments
        SET sync_status = :status,
            cloud_url = :cloudUrl,
            cloud_synced_at = :syncedAt
        WHERE id = :attachmentId
        """,
    )
    suspend fun updateAttachmentSyncStatus(
        attachmentId: String,
        status: String,
        cloudUrl: String?,
        syncedAt: String?,
    ): Int
}
