package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ClinicalDeleteRuleTest {
    private val record = DemoMedicationRecord(
        id = "med-1", patientId = "patient-1", name = "Ferrous Sulfate", dosage = "200 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-03-15", endDate = null, status = "Active",
        notes = null, isProphylaxis = false, revision = 3,
    )

    private val delete = DeleteMedicationCommand(
        operationUuid = UUID.randomUUID().toString(),
        patientId = "patient-1",
        expectedPatientRevision = 4,
        medicationId = "med-1",
        expectedMedicationRevision = 3,
        deletedContentHash = medicationContentHash(record),
    )

    private val restore = RestoreMedicationCommand(
        operationUuid = UUID.randomUUID().toString(),
        patientId = "patient-1",
        expectedPatientRevision = 5,
        record = record,
        sourceDelete = delete,
    )

    private val deleteReceipt = ClinicalCommandReceipt(
        operationUuid = delete.operationUuid,
        patientId = "patient-1",
        recordKind = ClinicalRecordKind.MEDICATION_DELETE.name,
        recordId = "med-1",
        committedRevision = 4,
        committedPatientRevision = 5,
        fingerprint = delete.computeFingerprint(),
    )

    @Test
    fun matchingLatestDeleteReceiptWithAbsentRowAllowsRestore() {
        assertNull(ClinicalDeleteRule.evaluateRestore(deleteReceipt, deleteReceipt, null, restore))
    }

    @Test
    fun wrongReceiptKindIsRecordConflict() {
        val update = deleteReceipt.copy(recordKind = ClinicalRecordKind.MEDICATION_UPDATE.name)
        assertEquals(ClinicalCommandFailure.RECORD_CONFLICT, ClinicalDeleteRule.evaluateRestore(update, update, null, restore))
        val lab = deleteReceipt.copy(recordKind = ClinicalRecordKind.LAB_DELETE.name)
        assertEquals(ClinicalCommandFailure.RECORD_CONFLICT, ClinicalDeleteRule.evaluateRestore(lab, lab, null, restore))
    }

    @Test
    fun receiptForAnotherPatientOrRecordIsRecordConflict() {
        val otherPatient = deleteReceipt.copy(patientId = "patient-2")
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(otherPatient, otherPatient, null, restore),
        )
        val otherRecord = deleteReceipt.copy(recordId = "med-2")
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(otherRecord, otherRecord, null, restore),
        )
    }

    @Test
    fun fingerprintMismatchIsRecordConflict() {
        val tampered = deleteReceipt.copy(fingerprint = delete.copy(expectedMedicationRevision = 9).computeFingerprint())
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(tampered, tampered, null, restore),
        )
    }

    @Test
    fun deleteReceiptThatIsNotTheLatestIsRecordConflict() {
        val later = deleteReceipt.copy(
            operationUuid = UUID.randomUUID().toString(),
            recordKind = ClinicalRecordKind.MEDICATION_RESTORE.name,
            committedRevision = 5,
            committedPatientRevision = 6,
        )
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(deleteReceipt, later, null, restore),
        )
    }

    @Test
    fun existingRowIsRecordConflict() {
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(deleteReceipt, deleteReceipt, record.copy(revision = 5), restore),
        )
    }

    @Test
    fun recordHashDifferingFromDeletedHashIsInvalidCommand() {
        val edited = restore.copy(record = record.copy(dosage = "400 mg"))
        assertEquals(
            ClinicalCommandFailure.INVALID_COMMAND,
            ClinicalDeleteRule.evaluateRestore(deleteReceipt, deleteReceipt, null, edited),
        )
    }

    @Test
    fun revisionDifferenceAloneDoesNotBlockRestore() {
        val sameContent = restore.copy(record = record.copy(revision = 77))
        assertNull(ClinicalDeleteRule.evaluateRestore(deleteReceipt, deleteReceipt, null, sameContent))
    }

    @Test
    fun labRestoreFollowsTheSameRules() {
        val lab = DemoLabInvestigation(
            id = "lab-1", patientId = "patient-1", date = "2026-02-10", testCategory = "Hematology",
            testName = "Hemoglobin", result = "11.2", unit = "g/dL", referenceRange = null, isAbnormal = false,
            notes = null, revision = 3,
        )
        val labDelete = DeleteLabInvestigationCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 4,
            labId = "lab-1",
            expectedLabRevision = 3,
            deletedContentHash = labContentHash(lab),
        )
        val labRestore = RestoreLabInvestigationCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 5,
            record = lab,
            sourceDelete = labDelete,
        )
        val receipt = ClinicalCommandReceipt(
            operationUuid = labDelete.operationUuid,
            patientId = "patient-1",
            recordKind = ClinicalRecordKind.LAB_DELETE.name,
            recordId = "lab-1",
            committedRevision = 4,
            committedPatientRevision = 5,
            fingerprint = labDelete.computeFingerprint(),
        )
        assertNull(ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, labRestore))
        val wrongKind = receipt.copy(recordKind = ClinicalRecordKind.MEDICATION_DELETE.name)
        assertEquals(
            ClinicalCommandFailure.RECORD_CONFLICT,
            ClinicalDeleteRule.evaluateRestore(wrongKind, wrongKind, null, labRestore),
        )
        assertEquals(
            ClinicalCommandFailure.INVALID_COMMAND,
            ClinicalDeleteRule.evaluateRestore(receipt, receipt, null, labRestore.copy(record = lab.copy(result = "5.0"))),
        )
    }
}
