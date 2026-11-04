package com.sherifradwan.ancnative.storage

import android.content.Context
import com.sherifradwan.ancnative.model.DemoPatient
import java.io.File

/** One payload file on disk, named relative to the attachments root. */
data class PayloadFile(
    val patientDir: String,
    val fileName: String,
    val lastModifiedMillis: Long,
) {
    val key: String get() = payloadKey(patientDir, fileName)
}

/** The single key shape used for "this row still owns this file". */
fun payloadKey(patientDir: String, fileName: String): String = "$patientDir/$fileName"

/**
 * Removes attachment payload files that no row points at any more.
 *
 * A committed delete removes its file when the Undo window closes. This sweep is the safety net for the paths that
 * cannot finish (the app was killed inside the window, the file deletion failed, or an older session left debris):
 * the owner decided on 2026-11-04 that the next unlock heals them.
 *
 * Two rules keep it safe: a file that a row still references is never touched, and a file younger than
 * [MIN_AGE_MILLIS] is left alone so a file written just before the sweep can never be mistaken for an orphan.
 */
object AttachmentOrphanSweeper {
    /** A freshly written file may not yet have its row: give it a minute before it can be called an orphan. */
    const val MIN_AGE_MILLIS = 60_000L

    /** Pure planning step: which present files have no row and are old enough to go. */
    fun planOrphans(
        present: List<PayloadFile>,
        referencedKeys: Set<String>,
        nowMillis: Long,
        minAgeMillis: Long = MIN_AGE_MILLIS,
    ): List<PayloadFile> = present.filter { file ->
        // An unknown mtime (0) must never read as "very old": keep the file rather than guess.
        val knownAge = file.lastModifiedMillis > 0L && nowMillis - file.lastModifiedMillis >= minAgeMillis
        file.key !in referencedKeys && knownAge
    }

    /**
     * Every file that a row still owns, in the same key shape as [PayloadFile.key].
     *
     * Two things write into the same per-patient folder: attachment rows, and ultrasound scan photos. A scan photo has
     * no attachment row — it is referenced only from `ultrasound_scans.image_uris` as a `vault:<file>` ref — so reading
     * `patient.attachments` alone would classify every scan photo as an orphan and delete clinical images.
     */
    fun referencedKeys(patients: List<DemoPatient>): Set<String> = buildSet {
        for (patient in patients) {
            val dir = PatientAttachmentVault.patientDirName(patient.id)
            for (attachment in patient.attachments) {
                attachment.fileName.takeIf { it.isNotBlank() }?.let { add(payloadKey(dir, it)) }
            }
            for (scan in patient.ultrasoundScans) {
                for (uri in scan.imageUris) {
                    scanPhotoFileName(uri)?.let { add(payloadKey(dir, it)) }
                }
            }
        }
    }

    /**
     * Best effort by design: a sweep may never block or fail an unlock. Returns how many files were removed.
     * The caller is the vault open path, after the snapshot has been validated.
     */
    fun sweep(context: Context, patients: List<DemoPatient>, nowMillis: Long = System.currentTimeMillis()): Int =
        try {
            val root = PatientAttachmentVault.attachmentsRoot(context)
            val doomed = planOrphans(currentFiles(root), referencedKeys(patients), nowMillis)
            doomed.count { file -> File(File(root, file.patientDir), file.fileName).delete() }
        } catch (failure: Throwable) {
            android.util.Log.w("AttachmentSweep", "orphan sweep skipped: ${failure.message}")
            0
        }

    /** Only direct children of the attachments root are candidates; nothing outside it is ever listed. */
    private fun currentFiles(root: File): List<PayloadFile> {
        if (!root.isDirectory) return emptyList()
        val patientDirs = root.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return patientDirs.flatMap { dir ->
            dir.listFiles()?.filter { it.isFile }?.map { file ->
                PayloadFile(dir.name, file.name, file.lastModified())
            }.orEmpty()
        }
    }
}
