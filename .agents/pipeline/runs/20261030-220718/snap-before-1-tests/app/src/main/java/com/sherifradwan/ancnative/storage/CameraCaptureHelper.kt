package com.sherifradwan.ancnative.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object CameraCaptureHelper {

    /**
     * Creates a temporary file in the app's cache directory and returns a content URI
     * via FileProvider suitable for launching the camera capture intent.
     */
    fun createCameraTempUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(cameraDir, "capture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile,
        )
    }
}
