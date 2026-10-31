package com.sherifradwan.ancnative.security

import com.sherifradwan.ancnative.storage.sha256Hex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySettingsRepositoryTest {
    private val goodPin = "483926"
    private val wrongPin = "159357"
    private var now = 1_000_000L
    private val prefs = FakeSharedPreferences()

    private fun repo() = SecuritySettingsRepository(prefs) { now }

    private fun repoWithPin(pin: String = goodPin): SecuritySettingsRepository =
        repo().also { assertTrue(it.updatePin(pin)) }

    private fun seedLegacyPin(pin: String) {
        prefs.edit().putString("key_pin_hash", sha256Hex("ANC_CLINIC_SALT_v1_${pin}_SECURE")).apply()
    }

    private fun failTimes(repo: SecuritySettingsRepository, times: Int) =
        repeat(times) { assertFalse(repo.validatePin(wrongPin)) }

    @Test fun fourWrongPinsDoNotLockOut() {
        val repo = repoWithPin()
        failTimes(repo, 4)
        assertEquals(0L, repo.getLockoutRemainingMs())
        assertTrue(repo.validatePin(goodPin))
    }

    @Test fun fifthWrongPinStartsThirtySecondLockout() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        assertEquals(lockoutDelayMs(5), repo.getLockoutRemainingMs())
        assertEquals(30_000L, repo.settings.value.lockoutRemainingMs)
    }

    @Test fun lockoutCountsDownWithTheClock() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        now += 10_000
        assertEquals(20_000L, repo.getLockoutRemainingMs())
        now += 20_000
        assertEquals(0L, repo.getLockoutRemainingMs())
    }

    @Test fun correctPinDuringLockoutIsRejected() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        assertFalse(repo.validatePin(goodPin))
        now += 29_999
        assertFalse(repo.validatePin(goodPin))
        assertEquals(1L, repo.getLockoutRemainingMs())
    }

    @Test fun wrongPinDuringLockoutDoesNotExtendIt() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        now += 10_000
        assertFalse(repo.validatePin(wrongPin))
        assertEquals(20_000L, repo.getLockoutRemainingMs())
    }

    @Test fun correctPinWorksOnceLockoutHasElapsed() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        now += 30_000
        assertTrue(repo.validatePin(goodPin))
        assertEquals(0L, repo.getLockoutRemainingMs())
    }

    @Test fun waitsGrowWithFurtherWrongPins() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        for (failures in 6..8) {
            now += lockoutDelayMs(failures - 1)
            assertFalse(repo.validatePin(wrongPin))
            assertEquals("after $failures failures", lockoutDelayMs(failures), repo.getLockoutRemainingMs())
        }
        assertEquals(900_000L, repo.getLockoutRemainingMs())
        now += 900_000
        assertFalse(repo.validatePin(wrongPin))
        assertEquals("wait stays at the maximum", 900_000L, repo.getLockoutRemainingMs())
    }

    @Test fun successfulPinResetsFailuresAndLockout() {
        val repo = repoWithPin()
        failTimes(repo, 4)
        assertTrue(repo.validatePin(goodPin))
        failTimes(repo, 4)
        assertEquals("failures restarted from zero, so 4 more is still no lockout", 0L, repo.getLockoutRemainingMs())
    }

    @Test fun successAfterLockoutExpiryClearsStoredLockout() {
        val repo = repoWithPin()
        failTimes(repo, 5)
        now += 30_000
        assertTrue(repo.validatePin(goodPin))
        assertFalse(repo.validatePin(wrongPin))
        assertEquals("one failure after success starts no lockout", 0L, repo.getLockoutRemainingMs())
    }

    @Test fun lockoutSurvivesRestartOfTheRepository() {
        failTimes(repoWithPin(), 5)
        now += 5_000
        val reopened = repo()
        assertEquals(25_000L, reopened.settings.value.lockoutRemainingMs)
        assertFalse(reopened.validatePin(goodPin))
    }

    @Test fun legacyPinVerifiesMigratesToV2AndStillVerifies() {
        seedLegacyPin(goodPin)
        val repo = repo()
        assertTrue(repo.settings.value.isLegacyPin)
        assertTrue(prefs.contains("key_pin_hash"))

        assertTrue(repo.validatePin(goodPin))
        assertTrue("v2 hash stored", prefs.contains("key_pin_hash_v2"))
        assertTrue(prefs.contains("key_pin_salt_v2"))
        assertFalse("legacy key removed", prefs.contains("key_pin_hash"))
        assertFalse(repo.settings.value.isLegacyPin)
        assertFalse(repo.settings.value.needsPinSetup)

        assertTrue(repo.validatePin(goodPin))
        assertFalse(repo.validatePin(wrongPin))
    }

    @Test fun migratedPinVerifiesAfterRestart() {
        seedLegacyPin(goodPin)
        assertTrue(repo().validatePin(goodPin))
        val reopened = repo()
        assertFalse(reopened.settings.value.isLegacyPin)
        assertTrue(reopened.validatePin(goodPin))
        assertFalse(reopened.validatePin(wrongPin))
    }

    @Test fun wrongLegacyPinIsRejectedAndKeepsLegacyHash() {
        seedLegacyPin(goodPin)
        val repo = repo()
        assertFalse(repo.validatePin(wrongPin))
        assertTrue(prefs.contains("key_pin_hash"))
        assertFalse(prefs.contains("key_pin_hash_v2"))
    }

    @Test fun weakButCorrectLegacyPinSetsNeedsPinSetup() {
        seedLegacyPin("123456")
        val repo = repo()
        assertTrue(repo.validatePin("123456"))
        assertTrue(repo.settings.value.needsPinSetup)
        assertTrue(repo.settings.value.isAppLocked)
    }

    @Test fun strongCorrectPinDoesNotNeedPinSetup() {
        seedLegacyPin(goodPin)
        val repo = repo()
        assertTrue(repo.validatePin(goodPin))
        assertFalse(repo.settings.value.needsPinSetup)
    }

    @Test fun wrongPinWithNoStoredPinReturnsFalse() {
        val repo = repo()
        assertFalse(repo.validatePin(wrongPin))
        assertFalse(repo.validatePin(goodPin))
        assertFalse(repo.settings.value.isPinConfigured)
    }

    @Test fun updatePinRejectsWeakPinAndKeepsTheOldOne() {
        val repo = repoWithPin()
        assertFalse(repo.updatePin("111111"))
        assertTrue(repo.validatePin(goodPin))
    }
}
