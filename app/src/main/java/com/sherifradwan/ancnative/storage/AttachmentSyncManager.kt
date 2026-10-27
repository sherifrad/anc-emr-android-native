package com.sherifradwan.ancnative.storage

import android.content.Context
import java.io.File

object AttachmentCategories {
    const val ULTRASOUND = "ULTRASOUND"
    const val LAB = "LAB"
    const val PRESCRIPTION = "PRESCRIPTION"
    const val CLINICAL_PHOTO = "CLINICAL_PHOTO"
    const val OTHER = "OTHER"

    fun localizedName(category: String): String = when (category) {
        ULTRASOUND -> "Ultrasound Scan"
        LAB -> "Laboratory Investigation"
        PRESCRIPTION -> "Prescription / Report"
        CLINICAL_PHOTO -> "Clinical Photo"
        else -> "General Medical Attachment"
    }
}

object AttachmentSyncStatus {
    const val LOCAL_ONLY = "LOCAL_ONLY"
    const val PENDING_SYNC = "PENDING_SYNC"
    const val SYNCED_CLOUD = "SYNCED_CLOUD"
    const val SYNC_ERROR = "SYNC_ERROR"

    fun localizedStatus(status: String): String = when (status) {
        LOCAL_ONLY -> "Locally Encrypted & Secured"
        PENDING_SYNC -> "Pending Cloud Sync"
        SYNCED_CLOUD -> "Synced with Clinical Cloud"
        SYNC_ERROR -> "Cloud Sync Error"
        else -> status
    }
}

data class AttachmentSyncStats(
    val totalCount: Int,
    val totalSizeBytes: Long,
    val syncedCount: Int,
    val pendingCount: Int,
    val localOnlyCount: Int,
)

object AttachmentSyncManager {

    /**
     * Calculates statistics of stored attachments for reporting and cloud sync monitoring.
     */
    fun computeStats(
        context: Context,
        patientId: String,
        attachments: List<SavedAttachmentResult>,
    ): AttachmentSyncStats {
        var totalBytes = 0L
        var localOnly = 0
        val pending = 0
        val synced = 0

        for (att in attachments) {
            totalBytes += att.fileSizeBytes
            localOnly++
        }

        return AttachmentSyncStats(
            totalCount = attachments.size,
            totalSizeBytes = totalBytes,
            syncedCount = synced,
            pendingCount = pending,
            localOnlyCount = localOnly,
        )
    }

    /**
     * Formats bytes into a clean, clinician-friendly string (e.g. "245 KB" or "1.2 MB").
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
