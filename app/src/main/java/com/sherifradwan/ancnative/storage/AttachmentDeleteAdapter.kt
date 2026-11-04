package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection
import java.security.MessageDigest

object AttachmentDeleteAdapter : DeletableRecordAdapter<DemoAttachment>, PayloadFileAdapter {
    override val type: DeletableType = DeletableType.ATTACHMENT
    override val fingerprintTag: String = "ATT"
    override val deleteReceiptKind: String = "ATTACHMENT_DELETE"
    override val restoreReceiptKind: String = "ATTACHMENT_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.ATTACHMENTS
    override val deletedNotice: SavedNotice = SavedNotice.ATTACHMENT_DELETED
    override val restoredNotice: SavedNotice = SavedNotice.ATTACHMENT_RESTORED
    override val saveUndoneNotice: SavedNotice = SavedNotice.ATTACHMENT_DELETED // no save-undo flow

    /**
     * What the clinician sees in the confirm dialog, the Undo bar and the failure notice. Never the internal
     * `att_<uuid>.webp` name: titles are optional, so fall back to the category and the capture date.
     */
    override fun label(record: DemoAttachment): String {
        record.title?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val category = AttachmentCategories.localizedName(record.category)
        val day = record.capturedAt.take(10).takeIf { it.length == 10 && it[4] == '-' && it[7] == '-' }
        return if (day != null) "$category · $day" else category
    }

    override fun canonical(record: DemoAttachment): String =
        "${record.patientId}|${record.id}|${record.fileName}|${record.filePath}|${record.category}|${record.mimeType}|${record.fileSizeBytes}|${record.sha256Hash}|${record.title}|${record.capturedAt}|${record.linkedRecordId}|${record.syncStatus}|${record.cloudUrl}|${record.cloudSyncedAt}"

    override suspend fun load(dao: RecordsDao, id: String): DemoAttachment? =
        dao.attachment(id)?.let { e ->
            DemoAttachment(
                id = e.id, patientId = e.patientId, fileName = e.fileName, filePath = e.filePath,
                category = e.category, mimeType = e.mimeType, fileSizeBytes = e.fileSizeBytes,
                sha256Hash = e.sha256Hash, title = e.title, capturedAt = e.capturedAt,
                linkedRecordId = e.linkedRecordId, syncStatus = e.syncStatus, cloudUrl = e.cloudUrl,
                cloudSyncedAt = e.cloudSyncedAt, revision = e.revision,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int =
        dao.deleteAttachmentRevision(id, patientId, expectedRevision)

    override suspend fun insertRow(dao: RecordsDao, record: DemoAttachment, revision: Long) {
        dao.insertAttachment(PatientAttachmentEntity(
            id = record.id, patientId = record.patientId, fileName = record.fileName,
            filePath = record.filePath, category = record.category, mimeType = record.mimeType,
            fileSizeBytes = record.fileSizeBytes, sha256Hash = record.sha256Hash, title = record.title,
            capturedAt = record.capturedAt, linkedRecordId = record.linkedRecordId,
            syncStatus = record.syncStatus, cloudUrl = record.cloudUrl, cloudSyncedAt = record.cloudSyncedAt,
            revision = revision,
        ))
    }

    override fun record(patient: DemoPatient, id: String): DemoAttachment? =
        patient.attachments.firstOrNull { it.id == id }

    override val hasPayloadFile: Boolean = true

    override suspend fun finalizeDelete(record: DemoAttachment, context: android.content.Context): FinalizeResult {
        return try {
            val ok = PatientAttachmentVault.deleteAttachment(context, record.patientId, record.fileName)
            if (ok) FinalizeResult.Success else FinalizeResult.Failed("delete returned false")
        } catch (e: Exception) {
            FinalizeResult.Failed(e.message ?: "unknown")
        }
    }
}

interface PayloadFileAdapter {
    val hasPayloadFile: Boolean
    suspend fun finalizeDelete(record: DemoAttachment, context: android.content.Context): FinalizeResult
}

sealed class FinalizeResult {
    object Success : FinalizeResult()
    data class Failed(val reason: String) : FinalizeResult()
}
