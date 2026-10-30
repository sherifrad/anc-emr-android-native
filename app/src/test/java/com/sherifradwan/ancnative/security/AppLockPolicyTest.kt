package com.sherifradwan.ancnative.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test fun constantIsTenMinutes() = assertEquals(600_000L, BACKGROUND_LOCK_MS)

    @Test fun nullNeverLocks() {
        assertFalse(shouldLockOnReturn(null, 5))
        assertFalse(shouldLockOnReturn(null, Long.MAX_VALUE))
    }

    @Test fun justUnderBoundaryDoesNotLock() {
        assertFalse(shouldLockOnReturn(1_000, 600_999))
    }

    @Test fun exactBoundaryLocks() {
        assertTrue(shouldLockOnReturn(1_000, 601_000))
    }

    @Test fun overBoundaryLocks() {
        assertTrue(shouldLockOnReturn(1_000, 601_001))
        assertTrue(shouldLockOnReturn(0, 10_000_000))
    }

    @Test fun clockMovedBackLocks() {
        assertTrue(shouldLockOnReturn(1_000, 500))
        assertTrue(shouldLockOnReturn(1_000, 999))
    }

    @Test fun sameInstantDoesNotLock() {
        assertFalse(shouldLockOnReturn(1_000, 1_000))
    }

    @Test fun shortAbsenceDoesNotLock() {
        assertFalse(shouldLockOnReturn(1_000, 61_000))
    }
}
