package com.sherifradwan.ancnative.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream

internal fun computeSampleSize(width: Int, height: Int, maxSide: Int): Int {
    if (width <= 0 || height <= 0 || maxSide <= 0) return 1
    if (width <= maxSide || height <= maxSide) return 1
    val halfWidth = width / 2
    val halfHeight = height / 2
    var sample = 1
    while ((halfWidth / sample) >= maxSide && (halfHeight / sample) >= maxSide) {
        sample *= 2
    }
    return sample
}

internal data class ExifTransform(val degrees: Float, val flipHorizontal: Boolean)

internal fun exifTransform(orientation: Int): ExifTransform = when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifTransform(0f, true)
    ExifInterface.ORIENTATION_ROTATE_180 -> ExifTransform(180f, false)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifTransform(180f, true)
    ExifInterface.ORIENTATION_TRANSPOSE -> ExifTransform(90f, true)
    ExifInterface.ORIENTATION_ROTATE_90 -> ExifTransform(90f, false)
    ExifInterface.ORIENTATION_TRANSVERSE -> ExifTransform(270f, true)
    ExifInterface.ORIENTATION_ROTATE_270 -> ExifTransform(270f, false)
    else -> ExifTransform(0f, false)
}

internal object BoundedBitmap {
    fun decodeFile(path: String, maxSide: Int): Bitmap? =
        decodeStream({ runCatching { FileInputStream(path) }.getOrNull() }, maxSide)

    fun decodeBytes(bytes: ByteArray, maxSide: Int): Bitmap? = safely {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions(bounds, maxSide))
            ?.let { upright(it) { ByteArrayInputStream(bytes) } }
    }

    fun decodeStream(open: () -> InputStream?, maxSide: Int): Bitmap? = safely {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        open()?.use { BitmapFactory.decodeStream(it, null, decodeOptions(bounds, maxSide)) }
            ?.let { upright(it, open) }
    }

    private fun upright(bitmap: Bitmap, open: () -> InputStream?): Bitmap {
        val orientation = try {
            open()?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val transform = exifTransform(orientation)
        if (transform.degrees == 0f && !transform.flipHorizontal) return bitmap
        val matrix = Matrix().apply {
            if (transform.flipHorizontal) postScale(-1f, 1f)
            postRotate(transform.degrees)
        }
        val rotated = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            return bitmap
        } catch (_: OutOfMemoryError) {
            return bitmap
        }
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun decodeOptions(bounds: BitmapFactory.Options, maxSide: Int) = BitmapFactory.Options().apply {
        inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    private fun safely(block: () -> Bitmap?): Bitmap? = try {
        block()
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}
