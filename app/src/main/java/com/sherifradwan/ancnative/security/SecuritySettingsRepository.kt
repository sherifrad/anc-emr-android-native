package com.sherifradwan.ancnative.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Base64
import java.security.MessageDigest

enum class AutoLockTimeout(val displayNameAr: String, val displayNameEn: String, val durationMs: Long) {
    IMMEDIATELY("Immediately on exit", "Immediately on exit", 0L),
    ONE_MINUTE("After 1 minute", "After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", "After 5 minutes", 300_000L),
    FIFTEEN_MINUTES("After 15 minutes", "After 15 minutes", 900_000L),
    TEN_MINUTES("After 10 minutes", "After 10 minutes", 600_000L),
}

enum class AppThemeMode(val displayNameAr: String, val displayNameEn: String) {
    SYSTEM("System Default", "System Default"),
    LIGHT("Light Mode", "Light Mode"),
    DARK("Dark Mode", "Dark Mode"),
}

data class SecuritySettingsState(
    val isPinConfigured: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.TEN_MINUTES,
    val isAppLocked: Boolean = true,
    val lastUnlockedAt: Long = 0L,
    val clinicName: String = "Dr. Sherif Radwan Clinic",
    val clinicSubtitle: String = "Maternal-Fetal Medicine & Obstetrics",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val needsPinSetup: Boolean = true,
    val isLegacyPin: Boolean = false,
    val lockoutRemainingMs: Long = 0L,
)

class SecuritySettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SecuritySettingsState> = _settings.asStateFlow()

    private fun loadSettings(): SecuritySettingsState {
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val timeoutName = prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.TEN_MINUTES.name)
        val timeout = runCatching { AutoLockTimeout.valueOf(timeoutName ?: AutoLockTimeout.TEN_MINUTES.name) }
            .getOrDefault(AutoLockTimeout.TEN_MINUTES).let { if (it.name == "NEVER") AutoLockTimeout.TEN_MINUTES else it }
        val isPinSet = prefs.contains(KEY_PIN_HASH_V2)
        val legacy = !isPinSet && prefs.contains(KEY_PIN_HASH)
        val configured = isPinSet || legacy
        val needsPinSetup = !configured
        val rawClinicName = prefs.getString(KEY_CLINIC_NAME, "Dr. Sherif Radwan Clinic") ?: "Dr. Sherif Radwan Clinic"
        val clinicName = if (rawClinicName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }) "Dr. Sherif Radwan Clinic" else rawClinicName
        val rawClinicSub = prefs.getString(KEY_CLINIC_SUBTITLE, "Maternal-Fetal Medicine & Obstetrics") ?: "Maternal-Fetal Medicine & Obstetrics"
        val clinicSub = if (rawClinicSub.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }) "Maternal-Fetal Medicine & Obstetrics" else rawClinicSub
        val themeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeName ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)

        return SecuritySettingsState(
            isPinConfigured = configured,
            isBiometricEnabled = biometric,
            autoLockTimeout = timeout,
            isAppLocked = configured,
            lastUnlockedAt = 0L,
            clinicName = clinicName,
            clinicSubtitle = clinicSub,
            themeMode = themeMode,
            needsPinSetup = needsPinSetup,
            isLegacyPin = legacy,
            lockoutRemainingMs = remainingLockoutMs(),
        )
    }

    fun validatePin(pin: String): Boolean {
        val remaining = remainingLockoutMs()
        if (remaining > 0) { refreshLockout(); return false }
        val valid = if (prefs.contains(KEY_PIN_HASH_V2)) {
            val salt = decode(KEY_PIN_SALT_V2) ?: return false
            val expected = decode(KEY_PIN_HASH_V2) ?: return false
            pinMatches(pin, salt, prefs.getInt(KEY_PIN_ITERATIONS_V2, PIN_ITERATIONS), expected)
        } else {
            val oldHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
            legacyHashPin(pin) == oldHash
        }
        if (!valid) {
            val failures = prefs.getInt(KEY_FAILURES, 0) + 1
            val delay = lockoutDelayMs(failures)
            prefs.edit().putInt(KEY_FAILURES, failures).putLong(KEY_LOCKOUT_END, System.currentTimeMillis() + delay).apply()
            refreshLockout()
            return false
        }
        val pinChangeRequired = !isValidNewPin(pin)
        prefs.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKOUT_END, 0L).apply()
        if (pinChangeRequired) {
            prefs.edit().putBoolean("key_pin_change_required", true).apply()
        } else {
            prefs.edit().putBoolean(KEY_PIN_CHANGE_REQUIRED, false).apply()
        }
        if (prefs.contains(KEY_PIN_HASH)) {
            storeV2(pin)
        }
        _settings.value = _settings.value.copy(
            isPinConfigured = true,
            isLegacyPin = false,
            needsPinSetup = pinChangeRequired,
            isAppLocked = if (pinChangeRequired) true else _settings.value.isAppLocked,
        )
        refreshLockout()
        return true
    }

    fun updatePin(newPin: String): Boolean {
        if (!isValidNewPin(newPin)) return false
        storeV2(newPin)
        prefs.edit().putBoolean(KEY_PIN_CHANGE_REQUIRED, false).apply()
        _settings.value = _settings.value.copy(isPinConfigured = true, needsPinSetup = false, isLegacyPin = false, isAppLocked = false)
        return true
    }

    fun setupPin(pin: String): Boolean = updatePin(pin)

    private fun storeV2(pin: String) {
        val salt = newSalt()
        prefs.edit().putString(KEY_PIN_HASH_V2, encode(hashPin(pin, salt)))
            .putString(KEY_PIN_SALT_V2, encode(salt)).putInt(KEY_PIN_ITERATIONS_V2, PIN_ITERATIONS)
            .remove(KEY_PIN_HASH).apply()
    }

    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(key: String) = prefs.getString(key, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
    private fun remainingLockoutMs() = (prefs.getLong(KEY_LOCKOUT_END, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
    private fun refreshLockout() { _settings.value = _settings.value.copy(lockoutRemainingMs = remainingLockoutMs()) }
    private fun legacyHashPin(pin: String): String {
        val salted = "ANC_CLINIC_SALT_v1_${pin}_SECURE"
        return MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(isBiometricEnabled = enabled)
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putString(KEY_AUTO_LOCK_TIMEOUT, timeout.name).apply()
        _settings.value = _settings.value.copy(autoLockTimeout = timeout)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun unlockApp() {
        if (remainingLockoutMs() > 0) return
        val now = System.currentTimeMillis()
        _settings.value = _settings.value.copy(
            isAppLocked = false,
            lastUnlockedAt = now,
        )
    }

    fun lockApp() {
        _settings.value = _settings.value.copy(isAppLocked = true, needsPinSetup = !_settings.value.isPinConfigured)
    }

    fun shouldLockOnBackground(elapsedSinceStopMs: Long): Boolean {
        val timeout = _settings.value.autoLockTimeout
        return when (timeout) {
            AutoLockTimeout.IMMEDIATELY -> true
            else -> elapsedSinceStopMs >= timeout.durationMs
        }
    }

    fun getLockoutRemainingMs(): Long = remainingLockoutMs().also { refreshLockout() }

    fun recordBackgroundTime(timeMs: Long) { prefs.edit().putLong(KEY_BACKGROUND_TIME, timeMs).apply() }
    fun backgroundTimeMs(): Long? = prefs.getLong(KEY_BACKGROUND_TIME, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }
    fun lockIfBackgroundTimeoutElapsed(nowMs: Long = System.currentTimeMillis()) {
        if (shouldLockOnReturn(prefs.getLong(KEY_BACKGROUND_TIME, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }, nowMs)) lockApp()
    }

    fun updateClinicBranding(name: String, subtitle: String) {
        val safeName = name.trim().ifEmpty { "Dr. Sherif Radwan Clinic" }
        val safeSub = subtitle.trim().ifEmpty { "Dr. Sherif Radwan Clinic · Maternal-Fetal Medicine & Obstetrics" }
        prefs.edit()
            .putString(KEY_CLINIC_NAME, safeName)
            .putString(KEY_CLINIC_SUBTITLE, safeSub)
            .apply()
        _settings.value = _settings.value.copy(
            clinicName = safeName,
            clinicSubtitle = safeSub,
        )
    }

    companion object {
        private const val PREFS_NAME = "anc_security_preferences"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_HASH_V2 = "key_pin_hash_v2"
        private const val KEY_PIN_SALT_V2 = "key_pin_salt_v2"
        private const val KEY_PIN_ITERATIONS_V2 = "key_pin_iterations_v2"
        private const val KEY_PIN_CHANGE_REQUIRED = "key_pin_change_required"
        private const val KEY_FAILURES = "key_pin_failures"
        private const val KEY_LOCKOUT_END = "key_pin_lockout_end"
        private const val KEY_BACKGROUND_TIME = "key_background_time"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val KEY_CLINIC_NAME = "key_clinic_name"
        private const val KEY_CLINIC_SUBTITLE = "key_clinic_subtitle"
        private const val KEY_THEME_MODE = "key_theme_mode"

        @Volatile
        private var instance: SecuritySettingsRepository? = null

        fun getInstance(context: Context): SecuritySettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SecuritySettingsRepository(context).also { instance = it }
            }
        }
    }
}
