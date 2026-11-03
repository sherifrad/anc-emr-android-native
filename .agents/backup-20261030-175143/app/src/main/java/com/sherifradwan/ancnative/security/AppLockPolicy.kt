package com.sherifradwan.ancnative.security

internal const val BACKGROUND_LOCK_MS = 600_000L

internal fun shouldLockOnReturn(backgroundSinceMs: Long?, nowMs: Long): Boolean {
    if (backgroundSinceMs == null) return false
    return nowMs < backgroundSinceMs || nowMs - backgroundSinceMs >= BACKGROUND_LOCK_MS
}
