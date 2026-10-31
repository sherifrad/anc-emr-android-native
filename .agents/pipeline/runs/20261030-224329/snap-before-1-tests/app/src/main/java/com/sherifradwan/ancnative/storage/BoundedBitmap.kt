package com.sherifradwan.ancnative.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

internal object BoundedBitmap {
    fun decodeFile(path: String, maxSide: Int): Bitmap? =
        decodeStream({ runCatching { FileInputStream(path) }.getOrNull() }, maxSide)

    fun decodeBytes(bytes: ByteArray, maxSide: Int): Bitmap? = safely {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions(bounds, maxSide))
    }

    fun decodeStream(open: () -> InputStream?, maxSide: Int): Bitmap? = safely {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        open()?.use { BitmapFactory.decodeStream(it, null, decodeOptions(bounds, maxSide)) }
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
