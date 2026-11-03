package com.sherifradwan.ancnative.storage

const val SCAN_PHOTO_REF_PREFIX = "vault:"

private val SCAN_PHOTO_FILE_NAME = Regex("[A-Za-z0-9_-]+\\.webp")

fun scanPhotoRef(fileName: String): String = SCAN_PHOTO_REF_PREFIX + fileName

fun isScanPhotoRef(value: String): Boolean = value.startsWith(SCAN_PHOTO_REF_PREFIX)

/** The vault file name of a scan photo ref, or null for anything that is not a well-formed ref. */
fun scanPhotoFileName(value: String): String? =
    if (isScanPhotoRef(value)) value.removePrefix(SCAN_PHOTO_REF_PREFIX).takeIf { SCAN_PHOTO_FILE_NAME.matches(it) } else null
