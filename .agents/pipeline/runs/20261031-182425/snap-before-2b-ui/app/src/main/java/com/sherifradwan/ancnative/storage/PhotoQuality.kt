package com.sherifradwan.ancnative.storage

enum class PhotoQualityPreset(val maxSide: Int, val webpQuality: Int) {
    SMALL(1280, 70),
    STANDARD(1600, 75),
    DETAILED(1920, 85);

    companion object {
        val DEFAULT = SMALL

        fun defaultFor(category: String): PhotoQualityPreset =
            if (category == AttachmentCategories.ULTRASOUND) DETAILED else SMALL
    }
}
