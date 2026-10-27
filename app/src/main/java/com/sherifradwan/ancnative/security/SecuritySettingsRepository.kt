package com.sherifradwan.ancnative.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

enum class AutoLockTimeout(val displayNameAr: String, val displayNameEn: String, val durationMs: Long) {
    IMMEDIATELY("فوراً عند مغادرة التطبيق", "Immediately on exit", 0L),
    ONE_MINUTE("بعد دقيقة واحدة", "After 1 minute", 60_000L),
    FIVE_MINUTES("بعد 5 دقائق", "After 5 minutes", 300_000L),
    FIFTEEN_MINUTES("بعد 15 دقيقة", "After 15 minutes", 900_000L),
    NEVER("لا يقفل أبداً (إيقاف القفل)", "Never (Lock Disabled)", Long.MAX_VALUE),
}

enum class AppThemeMode(val displayNameAr: String, val displayNameEn: String) {
    SYSTEM("تلقائي (حسب النظام)", "System Default"),
    LIGHT("فاتح (نهاري عيادة)", "Light Mode"),
    DARK("داكن (فحص سونار)", "Dark Mode"),
}

data class SecuritySettingsState(
    val isPinConfigured: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.NEVER,
    val isAppLocked: Boolean = false,
    val lastUnlockedAt: Long = 0L,
    val clinicName: String = "Dr. Sherif Radwan Clinic",
    val clinicSubtitle: String = "رعاية الأمومة وطب الجنين · Maternal-Fetal Medicine",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

class SecuritySettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SecuritySettingsState> = _settings.asStateFlow()

    init {
        // Ensure default PIN is seeded if not present
        if (!prefs.contains(KEY_PIN_HASH)) {
            val defaultHash = hashPin(DEFAULT_PIN)
            prefs.edit().putString(KEY_PIN_HASH, defaultHash).apply()
        }
    }

    private fun loadSettings(): SecuritySettingsState {
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val timeoutName = prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.NEVER.name)
        val timeout = runCatching { AutoLockTimeout.valueOf(timeoutName ?: AutoLockTimeout.NEVER.name) }
            .getOrDefault(AutoLockTimeout.NEVER)
        val isPinSet = prefs.contains(KEY_PIN_HASH)
        val rawClinicName = prefs.getString(KEY_CLINIC_NAME, "Dr. Sherif Radwan Clinic") ?: "Dr. Sherif Radwan Clinic"
        val clinicName = if (rawClinicName == "دكتور شريف رضوان كلينك") "Dr. Sherif Radwan Clinic" else rawClinicName
        val clinicSub = prefs.getString(KEY_CLINIC_SUBTITLE, "رعاية الأمومة وطب الجنين · Maternal-Fetal Medicine") ?: "رعاية الأمومة وطب الجنين · Maternal-Fetal Medicine"
        val themeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeName ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)

        return SecuritySettingsState(
            isPinConfigured = isPinSet,
            isBiometricEnabled = biometric,
            autoLockTimeout = timeout,
            isAppLocked = false,
            lastUnlockedAt = 0L,
            clinicName = clinicName,
            clinicSubtitle = clinicSub,
            themeMode = themeMode,
        )
    }

    fun validatePin(pin: String): Boolean {
        if (pin.length != 6) return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val candidateHash = hashPin(pin)
        return candidateHash == storedHash
    }

    fun updatePin(newPin: String): Boolean {
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) return false
        val newHash = hashPin(newPin)
        prefs.edit().putString(KEY_PIN_HASH, newHash).apply()
        _settings.value = _settings.value.copy(isPinConfigured = true)
        return true
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
        val now = System.currentTimeMillis()
        _settings.value = _settings.value.copy(
            isAppLocked = false,
            lastUnlockedAt = now,
        )
    }

    fun lockApp() {
        _settings.value = _settings.value.copy(isAppLocked = true)
    }

    fun shouldLockOnBackground(elapsedSinceStopMs: Long): Boolean {
        val timeout = _settings.value.autoLockTimeout
        return when (timeout) {
            AutoLockTimeout.NEVER -> false
            AutoLockTimeout.IMMEDIATELY -> true
            else -> elapsedSinceStopMs >= timeout.durationMs
        }
    }

    private fun hashPin(pin: String): String {
        val salted = "ANC_CLINIC_SALT_v1_${pin}_SECURE"
        val bytes = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun updateClinicBranding(name: String, subtitle: String) {
        val safeName = name.trim().ifEmpty { "دكتور شريف رضوان كلينك" }
        val safeSub = subtitle.trim().ifEmpty { "Dr. Sherif Radwan Clinic · رعاية الأمومة وطب الجنين" }
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
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val KEY_CLINIC_NAME = "key_clinic_name"
        private const val KEY_CLINIC_SUBTITLE = "key_clinic_subtitle"
        private const val KEY_THEME_MODE = "key_theme_mode"
        const val DEFAULT_PIN = "123456"

        @Volatile
        private var instance: SecuritySettingsRepository? = null

        fun getInstance(context: Context): SecuritySettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SecuritySettingsRepository(context).also { instance = it }
            }
        }
    }
}
