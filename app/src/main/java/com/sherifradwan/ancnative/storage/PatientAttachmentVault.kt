package com.sherifradwan.ancnative.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.UUID

data class SavedAttachmentResult(
    val attachmentId: String,
    val patientId: String,
    val fileName: String,
    val relativePath: String,
    val absolutePath: String,
    val fileSizeBytes: Long,
    val sha256Hash: String,
    val mimeType: String = "image/webp",
    val capturedAt: String = Instant.now().toString(),
)

object PatientAttachmentVault {

    private const val ATTACHMENTS_ROOT_DIR = "patient_attachments"

    /**
     * Resolves and secures the isolated folder for a specific patient.
     * Prevents path traversal and validates directory isolation.
     */
    /** The on-disk folder name for a patient: one rule for the writer, the reader and the orphan sweep. */
    fun patientDirName(patientId: String): String {
        require(patientId.isNotBlank()) { "patientId cannot be blank" }
        return patientId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    /** Root of every patient attachment folder. The orphan sweep must never look outside it. */
    fun attachmentsRoot(context: Context): File = File(context.filesDir, ATTACHMENTS_ROOT_DIR)

    fun getPatientDirectory(context: Context, patientId: String): File {
        // Ensure no path traversal in patientId
        val sanitizedPatientId = patientDirName(patientId)
        val rootDir = attachmentsRoot(context)
        val patientDir = File(rootDir, sanitizedPatientId)

        if (!patientDir.exists()) {
            patientDir.mkdirs()
        }

        // Verify that patientDir does not escape rootDir
        val canonicalRoot = rootDir.canonicalFile
        val canonicalPatientDir = patientDir.canonicalFile
        if (!canonicalPatientDir.toPath().startsWith(canonicalRoot.toPath())) {
            throw SecurityException("Path traversal attempt detected: $patientId")
        }

        return canonicalPatientDir
    }

    /**
     * Ingests, resizes, compresses to WebP, calculates SHA-256 hash, and saves the file
     * strictly inside the patient's isolated directory.
     */
    fun saveAttachment(
        context: Context,
        patientId: String,
        sourceUri: Uri,
        attachmentId: String = "att_${UUID.randomUUID()}",
        preset: PhotoQualityPreset = PhotoQualityPreset.DEFAULT,
    ): SavedAttachmentResult {
        val patientDir = getPatientDirectory(context, patientId)
        val fileName = "$attachmentId.webp"
        val destinationFile = File(patientDir, fileName)

        // Read source stream
        val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $sourceUri")

        return compressAndWrite(
            patientId = patientId,
            attachmentId = attachmentId,
            destinationFile = destinationFile,
            fileName = fileName,
            inputStream = inputStream,
            preset = preset,
        )
    }

    /**
     * Ingests raw input stream or bytes and saves into the patient's vault.
     */
    fun saveAttachmentFromStream(
        context: Context,
        patientId: String,
        inputStream: InputStream,
        attachmentId: String = "att_${UUID.randomUUID()}",
        preset: PhotoQualityPreset = PhotoQualityPreset.DEFAULT,
    ): SavedAttachmentResult {
        val patientDir = getPatientDirectory(context, patientId)
        val fileName = "$attachmentId.webp"
        val destinationFile = File(patientDir, fileName)

        return compressAndWrite(
            patientId = patientId,
            attachmentId = attachmentId,
            destinationFile = destinationFile,
            fileName = fileName,
            inputStream = inputStream,
            preset = preset,
        )
    }

    private fun compressAndWrite(
        patientId: String,
        attachmentId: String,
        destinationFile: File,
        fileName: String,
        inputStream: InputStream,
        preset: PhotoQualityPreset,
    ): SavedAttachmentResult {
        val rawBytes = inputStream.use { it.readBytes() }
        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("Source image stream is empty")
        }

        // 1-2. Decode, sampled down so a huge source cannot run out of memory
        val sampledBitmap = BoundedBitmap.decodeBytes(rawBytes, preset.maxSide)
            ?: throw IllegalStateException("Failed to decode bitmap from source image")

        // 3. Scale precisely to the preset's longest side maintaining aspect ratio
        val finalBitmap = scaleToMaxDimension(sampledBitmap, preset.maxSide)

        // 4. Compress to WebP
        val outputStream = ByteArrayOutputStream()
        val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        finalBitmap.compress(format, preset.webpQuality, outputStream)
        if (finalBitmap != sampledBitmap) {
            finalBitmap.recycle()
        }
        sampledBitmap.recycle()

        val webpBytes = outputStream.toByteArray()

        // 5. Calculate SHA-256 cryptographic hash
        val sha256Hex = sha256Hex(webpBytes)

        // 6. Write file to disk
        FileOutputStream(destinationFile).use { fos ->
            fos.write(webpBytes)
            fos.flush()
        }

        val relativePath = "$ATTACHMENTS_ROOT_DIR/${destinationFile.parentFile?.name}/$fileName"

        return SavedAttachmentResult(
            attachmentId = attachmentId,
            patientId = patientId,
            fileName = fileName,
            relativePath = relativePath,
            absolutePath = destinationFile.absolutePath,
            fileSizeBytes = destinationFile.length(),
            sha256Hash = sha256Hex,
            mimeType = "image/webp",
        )
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * Resolves the absolute File for an attachment belonging to a patient.
     * Ensures that the file resides strictly within the patient's folder.
     */
    fun resolveAttachmentFile(context: Context, patientId: String, fileName: String): File? {
        val patientDir = getPatientDirectory(context, patientId)
        val file = File(patientDir, fileName)
        val canonicalFile = file.canonicalFile
        if (!canonicalFile.toPath().startsWith(patientDir.canonicalFile.toPath())) {
            throw SecurityException("Cross-patient attachment access denied: $fileName")
        }
        return if (canonicalFile.exists()) canonicalFile else null
    }

    /**
     * Deletes an attachment file after ensuring patient boundary ownership.
     */
    fun deleteAttachment(context: Context, patientId: String, fileName: String): Boolean {
        val file = resolveAttachmentFile(context, patientId, fileName) ?: return true // missing = success (idempotent)
        return try { file.delete() } catch (e: SecurityException) { false }
    }

    /**
     * Verifies the cryptographic integrity of a file against its expected SHA-256.
     */
    fun verifyIntegrity(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val actualHex = FileInputStream(file).use { sha256Hex(it) }
        return actualHex.equals(expectedHash, ignoreCase = true)
    }
}
