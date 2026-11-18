package com.sherifradwan.ancnative.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.storage.scanPhotoFileName
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal const val PICKER_THUMBNAIL_MAX_SIDE_PX = 256
private const val PRINT_IMAGE_MAX_SIDE_PX = 1000
private const val PRINT_JPEG_QUALITY = 82
internal const val MAX_TOTAL_DATA_URI_BYTES = 24 * 1024 * 1024

/**
 * Resolves [candidate] from the patient attachment vault, a "content" URI, or a "file" URI
 * inside the app's files or cache directories, and decodes it bounded by [maxSide], returning
 * null on any disallowed path, missing file, or decode failure.
 */
internal fun decodePictureBitmap(
    context: Context,
    patientId: String,
    candidate: PictureCandidate,
    maxSide: Int,
): Bitmap? = runCatching {
    val location = candidate.location.trim()
    if (location.isEmpty()) return@runCatching null

    val vaultFileName = scanPhotoFileName(location)
    if (vaultFileName != null) {
        val file = PatientAttachmentVault.resolveAttachmentFile(context, patientId, vaultFileName)
        return@runCatching if (file != null && file.exists()) {
            BoundedBitmap.decodeFile(file.absolutePath, maxSide)
        } else {
            null
        }
    }

    val uri = Uri.parse(location)
    when {
        uri.scheme == "content" -> {
            BoundedBitmap.decodeStream({ context.contentResolver.openInputStream(uri) }, maxSide)
        }
        uri.scheme.equals("file", ignoreCase = true) -> {
            val file = resolveAllowedFileUri(context, uri) ?: return@runCatching null
            BoundedBitmap.decodeFile(file.absolutePath, maxSide)
        }
        uri.scheme == null && candidate.key.startsWith("att:") -> {
            val file = PatientAttachmentVault.resolveAttachmentFile(context, patientId, location)
            if (file != null && file.exists()) {
                BoundedBitmap.decodeFile(file.absolutePath, maxSide)
            } else {
                null
            }
        }
        else -> null
    }
}.getOrNull()

/**
 * Resolves a "file" [uri] only when its canonical path resides inside [context.filesDir] or
 * [context.cacheDir] canonical paths, preventing arbitrary filesystem reads.
 */
private fun resolveAllowedFileUri(context: Context, uri: Uri): File? {
    val authority = uri.authority
    if (!authority.isNullOrEmpty() && !authority.equals("localhost", ignoreCase = true)) {
        return null
    }
    val rawPath = uri.path?.takeIf { it.isNotBlank() } ?: return null
    val canonicalTarget = File(rawPath).canonicalFile
    if (!canonicalTarget.isFile) return null

    val targetPath = canonicalTarget.path
    val isInsideAllowedDir = listOfNotNull(context.filesDir, context.cacheDir).any { root ->
        val canonicalRootPath = root.canonicalFile.path
        val prefix = if (canonicalRootPath.endsWith(File.separator)) {
            canonicalRootPath
        } else {
            canonicalRootPath + File.separator
        }
        targetPath.startsWith(prefix)
    }
    return if (isInsideAllowedDir) canonicalTarget else null
}

/**
 * Decodes [candidate] at 1000 px max side and re-encodes it as a JPEG quality-82 data URI,
 * returning null if the source cannot be resolved or decoded.
 */
suspend fun encodePictureDataUri(
    context: Context,
    patientId: String,
    candidate: PictureCandidate,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val decoded = decodePictureBitmap(
            context = context,
            patientId = patientId,
            candidate = candidate,
            maxSide = PRINT_IMAGE_MAX_SIDE_PX,
        ) ?: return@runCatching null

        val scaled = scaleDownIfExceeds(decoded, PRINT_IMAGE_MAX_SIDE_PX)
        try {
            val outputStream = ByteArrayOutputStream()
            val compressed = scaled.compress(Bitmap.CompressFormat.JPEG, PRINT_JPEG_QUALITY, outputStream)
            if (!compressed) return@runCatching null
            val jpegBytes = outputStream.toByteArray()
            if (jpegBytes.isEmpty()) return@runCatching null
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } finally {
            if (scaled !== decoded) {
                scaled.recycle()
            }
            decoded.recycle()
        }
    }.getOrNull()
}

/**
 * Encodes selected [figures] in order until cumulative data URI bytes reach [maxTotalBytes],
 * leaving remaining figures unmapped so the print template shows "Image not available".
 */
suspend fun encodePicturesForPrint(
    context: Context,
    patientId: String,
    figures: List<Figure>,
    maxTotalBytes: Int = MAX_TOTAL_DATA_URI_BYTES,
): Map<String, String> = withContext(Dispatchers.Default) {
    if (figures.isEmpty()) return@withContext emptyMap()
    val encodedByKey = LinkedHashMap<String, String>(figures.size)
    var totalBytes = 0L
    for (figure in figures) {
        ensureActive()
        if (totalBytes >= maxTotalBytes) break
        val dataUri = encodePictureDataUri(context, patientId, figure.candidate) ?: continue
        val uriBytes = dataUri.length.toLong()
        if (totalBytes + uriBytes > maxTotalBytes) break
        encodedByKey[figure.candidate.key] = dataUri
        totalBytes += uriBytes
    }
    encodedByKey
}

private fun scaleDownIfExceeds(bitmap: Bitmap, maxSide: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0 || (width <= maxSide && height <= maxSide)) {
        return bitmap
    }
    val scale = minOf(maxSide.toFloat() / width, maxSide.toFloat() / height)
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
