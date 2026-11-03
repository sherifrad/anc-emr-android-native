package com.sherifradwan.ancnative.storage

object ClinicalDeleteRule {
    fun evaluateRestore(
        receipt: ClinicalCommandReceipt?,
        latestReceipt: ClinicalCommandReceipt?,
        existingRow: Any?,
        cmd: ClinicalCommand,
    ): ClinicalCommandFailure? {
        val restoreCmd = cmd as? RestoreRecordCommand ?: return ClinicalCommandFailure.INVALID_COMMAND

        val adapter = DeletableRegistry.of<DeletableRecord>(restoreCmd.type)
        val expectedKind = adapter.deleteReceiptKind
        val expectedFingerprint = restoreCmd.sourceDelete.computeFingerprint()
        val recordContentHash = contentHash(restoreCmd.record)
        val sourceHash = restoreCmd.sourceDelete.deletedContentHash

        if (receipt == null) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.recordKind != expectedKind) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.patientId != restoreCmd.patientId) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.recordId != restoreCmd.recordId) return ClinicalCommandFailure.RECORD_CONFLICT
        if (receipt.fingerprint != expectedFingerprint) return ClinicalCommandFailure.RECORD_CONFLICT
        if (latestReceipt == null || latestReceipt.operationUuid != receipt.operationUuid) {
            return ClinicalCommandFailure.RECORD_CONFLICT
        }
        if (existingRow != null) return ClinicalCommandFailure.RECORD_CONFLICT
        if (recordContentHash != sourceHash) return ClinicalCommandFailure.INVALID_COMMAND

        return null
    }
}
