package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Test

/** Only the pure sample-size rule is tested; the Bitmap decoders need Android and are not covered here. */
class BoundedBitmapTest {
    @Test fun smallImageIsNotSampled() {
        assertEquals(1, computeSampleSize(800, 600, 2048))
    }

    @Test fun imageExactlyAtMaxSideIsNotSampled() {
        assertEquals(1, computeSampleSize(2048, 2048, 2048))
    }

    @Test fun oneSideAtOrBelowMaxMeansNoSampling() {
        assertEquals(1, computeSampleSize(10_000, 2048, 2048))
        assertEquals(1, computeSampleSize(2048, 10_000, 2048))
        assertEquals(1, computeSampleSize(10_000, 100, 2048))
    }

    @Test fun exactThresholdForSampleSizeTwo() {
        // half of each side must still be >= maxSide: 4096/2 = 2048 qualifies, 4095/2 = 2047 does not.
        assertEquals(2, computeSampleSize(4096, 4096, 2048))
        assertEquals(1, computeSampleSize(4095, 4095, 2048))
        assertEquals(1, computeSampleSize(4096, 4095, 2048))
        assertEquals(1, computeSampleSize(4095, 4096, 2048))
    }

    @Test fun largerImagesGetLargerPowersOfTwo() {
        assertEquals(4, computeSampleSize(8192, 8192, 2048))
        assertEquals(2, computeSampleSize(8191, 8191, 2048))
        assertEquals(8, computeSampleSize(16_384, 16_384, 2048))
    }

    @Test fun theSmallerSideDecidesTheSampleSize() {
        assertEquals(2, computeSampleSize(20_000, 4096, 2048))
        assertEquals(2, computeSampleSize(4096, 20_000, 2048))
    }

    @Test fun resultIsAlwaysAPowerOfTwoAtLeastOne() {
        for (side in listOf(1, 1023, 1024, 2047, 2048, 2049, 4096, 5000, 12_345, 65_536)) {
            val s = computeSampleSize(side, side, 1024)
            assertEquals("side $side", 1, Integer.bitCount(s))
            assertEquals("side $side", true, s >= 1)
        }
    }

    @Test fun zeroOrNegativeDimensionsGiveOne() {
        assertEquals(1, computeSampleSize(0, 0, 2048))
        assertEquals(1, computeSampleSize(0, 5000, 2048))
        assertEquals(1, computeSampleSize(5000, 0, 2048))
        assertEquals(1, computeSampleSize(-1, -1, 2048))
        assertEquals(1, computeSampleSize(-4096, 8192, 2048))
        assertEquals(1, computeSampleSize(8192, -4096, 2048))
    }

    // android.media.ExifInterface ORIENTATION_* values as plain ints (1..8).
    @Test fun exifOrientationsMapToRotationAndFlip() {
        assertEquals(ExifTransform(0f, false), exifTransform(1))
        assertEquals(ExifTransform(0f, true), exifTransform(2))
        assertEquals(ExifTransform(180f, false), exifTransform(3))
        assertEquals(ExifTransform(180f, true), exifTransform(4))
        assertEquals(ExifTransform(90f, true), exifTransform(5))
        assertEquals(ExifTransform(90f, false), exifTransform(6))
        assertEquals(ExifTransform(270f, true), exifTransform(7))
        assertEquals(ExifTransform(270f, false), exifTransform(8))
    }

    @Test fun unknownExifOrientationIsNormal() {
        for (value in listOf(0, 9, -1, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertEquals("orientation $value", ExifTransform(0f, false), exifTransform(value))
        }
    }

    @Test fun smallerMaxSideSamplesMore() {
        assertEquals(2, computeSampleSize(2048, 2048, 1024))
        assertEquals(1, computeSampleSize(2047, 2047, 1024))
    }
}
