package com.sherifradwan.ancnative.storage

object ClinicalDeleteRule {
    fun evaluateRestore(
        receipt: ClinicalCommandReceipt?,
        latestReceipt: ClinicalCommandReceipt?,
        existingRow: Any?,
        cmd: ClinicalCommand,
    ): ClinicalCommandFailure? {
        val (expectedKind, expectedFingerprint, recordContentHash, sourceHash) = when (cmd) {
            is RestoreMedicationCommand -> {
                Quadruple(
                    ClinicalRecordKind.MEDICATION_DELETE.name,
                    cmd.sourceDelete.computeFingerprint(),
                    medicationContentHash(cmd.record),
                    cmd.sourceDelete.deletedContentHash,
                )
            }
            is RestoreLabInvestigationCommand -> {
                Quadruple(
                    ClinicalRecordKind.LAB_DELETE.name,
                    cmd.sourceDelete.computeFingerprint(),
                    labContentHash(cmd.record),
                    cmd.sourceDelete.deletedContentHash,
                )
            }
            else -> return ClinicalCommandFailure.INVALID_COMMAND
        }

        if (receipt == null) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.recordKind != expectedKind) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.patientId != cmd.patientId) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.recordId != cmd.recordId) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.fingerprint != expectedFingerprint) return ClinicalCommandFailure.RECORD_CONFLICT
        if (latestReceipt == null || latestReceipt.operationUuid != receipt.operationUuid) {
            return ClinicalCommandFailure.RECORD_CONFLICT
        }
        if (existingRow != null) return ClinicalCommandFailure.RECORD_CONFLICT
        if (recordContentHash != sourceHash) return ClinicalCommandFailure.INVALID_COMMAND

        return null
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
