package com.sherifradwan.ancnative.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object CameraCaptureHelper {

    private fun cameraDir(context: Context): File = File(context.cacheDir, "camera")

    /** Creates a new capture file path inside the app's cache/camera folder. */
    fun createCameraTempFile(context: Context): File {
        val cameraDir = cameraDir(context).apply {
            if (!exists()) mkdirs()
        }
        return File(cameraDir, "capture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
    }

    /** FileProvider content URI for a file in the app's cache. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Temporary capture file URI, via FileProvider. */
    fun createCameraTempUri(context: Context): Uri = uriFor(context, createCameraTempFile(context))

    /**
     * Deletes the file behind [uri] only when it is one of our FileProvider URIs that resolves
     * (canonically) to a file inside cacheDir/camera. Returns true only when a file was deleted.
     */
    fun deleteIfCaptureTemp(context: Context, uri: Uri): Boolean =
        captureTempFile(
            cameraDir(context), "${context.packageName}.fileprovider", uri.scheme, uri.authority, uri.pathSegments,
        )?.delete() == true

    /**
     * Removes capture files older than [olderThanMs] left behind in cacheDir/camera, except those
     * still referenced by a saved scan ([referencedUris], matched by last path segment).
     */
    fun clearStaleCaptures(context: Context, olderThanMs: Long, referencedUris: Set<String>) {
        val referencedNames = referencedUris.map { it.substringAfterLast('/') }.toSet()
        staleCaptureFiles(cameraDir(context), olderThanMs, System.currentTimeMillis(), referencedNames).forEach { it.delete() }
    }
}

/**
 * The canonical file inside [cameraDir] that a FileProvider URI points at, or null unless the URI is ours
 * and resolves to a regular file directly in [cameraDir]. Never throws.
 */
fun captureTempFile(
    cameraDir: File,
    expectedAuthority: String,
    scheme: String?,
    authority: String?,
    pathSegments: List<String>,
): File? {
    return try {
        if (scheme != "content" || authority != expectedAuthority) return null
        // file_paths.xml maps cacheDir/camera/ to /camera_photos/<relative path>
        if (pathSegments.size < 2 || pathSegments[0] != "camera_photos") return null
        val root = cameraDir.canonicalFile
        val candidate = File(root, pathSegments.drop(1).joinToString(File.separator)).canonicalFile
        if (candidate.isFile && candidate.parentFile == root) candidate else null
    } catch (e: Exception) {
        null
    }
}

/** Regular files in [dir] older than [olderThanMs] whose name is not in [referencedNames]. */
fun staleCaptureFiles(dir: File, olderThanMs: Long, nowMs: Long, referencedNames: Set<String>): List<File> {
    val cutoff = nowMs - olderThanMs
    return dir.listFiles().orEmpty().filter { it.isFile && it.lastModified() < cutoff && it.name !in referencedNames }
}
