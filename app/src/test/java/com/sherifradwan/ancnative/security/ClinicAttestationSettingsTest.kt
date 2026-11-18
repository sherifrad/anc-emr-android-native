package com.sherifradwan.ancnative.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for clinic attestation settings in [SecuritySettingsRepository] and [SecuritySettingsState].
 *
 * Verifies defaults, persistence, blank fallback, character caps/filters, Arabic field acceptance,
 * backward-compatibility for existing installs, and state isolation from PIN and clinic branding.
 */
class ClinicAttestationSettingsTest {

    private lateinit var prefs: FakeSharedPreferences
    private var now = 1_000_000L

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
    }

    private fun createRepo(): SecuritySettingsRepository =
        SecuritySettingsRepository(prefs) { now }

    @Test
    fun defaults_equalTheOldLiterals() {
        // Assert companion constants match the canonical literals
        assertEquals("Dr. Sherif Radwan", SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN)
        assertEquals("Specialist of Obstetrics, Gynecology & Laparoscopy", SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN)
        assertEquals("شريف جمال رضوان", SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR)
        assertEquals("أخصائي التوليد وأمراض النساء وتأخر الحمل", SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR)
        assertEquals("01201720808", SecuritySettingsState.DEFAULT_CLINIC_PHONE)

        // Fresh repo with empty preferences must yield the exact defaults
        val repo = createRepo()
        val state = repo.settings.value
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN, state.doctorNameEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN, state.doctorSpecialtyEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR, state.doctorNameAr)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR, state.doctorSpecialtyAr)
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, state.clinicPhone)
    }

    @Test
    fun updateClinicAttestation_persistsAndReloadsInNewRepositoryInstance() {
        val repo = createRepo()
        val newNameEn = "Dr. Jane Doe"
        val newSpecialtyEn = "Consultant of Maternal-Fetal Medicine"
        val newNameAr = "د. جين دو"
        val newSpecialtyAr = "استشارية طب وجراحة الجنين"
        val newPhone = "+20 (100) 123-4567"

        repo.updateClinicAttestation(
            nameEn = newNameEn,
            specialtyEn = newSpecialtyEn,
            nameAr = newNameAr,
            specialtyAr = newSpecialtyAr,
            phone = newPhone,
        )

        // Verify current repository StateFlow updated
        val updated = repo.settings.value
        assertEquals(newNameEn, updated.doctorNameEn)
        assertEquals(newSpecialtyEn, updated.doctorSpecialtyEn)
        assertEquals(newNameAr, updated.doctorNameAr)
        assertEquals(newSpecialtyAr, updated.doctorSpecialtyAr)
        assertEquals(newPhone, updated.clinicPhone)

        // Verify a newly instantiated repository reloads identical persisted values
        val reloadedRepo = createRepo()
        val reloaded = reloadedRepo.settings.value
        assertEquals(newNameEn, reloaded.doctorNameEn)
        assertEquals(newSpecialtyEn, reloaded.doctorSpecialtyEn)
        assertEquals(newNameAr, reloaded.doctorNameAr)
        assertEquals(newSpecialtyAr, reloaded.doctorSpecialtyAr)
        assertEquals(newPhone, reloaded.clinicPhone)
    }

    @Test
    fun blankValues_fallBackToDefaults() {
        val repo = createRepo()

        // Pass blanks and whitespace
        repo.updateClinicAttestation(
            nameEn = "   ",
            specialtyEn = "",
            nameAr = "  \t  ",
            specialtyAr = "",
            phone = "   ",
        )

        val state = repo.settings.value
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN, state.doctorNameEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN, state.doctorSpecialtyEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR, state.doctorNameAr)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR, state.doctorSpecialtyAr)
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, state.clinicPhone)

        // Confirm reload in a new repository instance also reflects defaults
        val reloaded = createRepo().settings.value
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN, reloaded.doctorNameEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN, reloaded.doctorSpecialtyEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR, reloaded.doctorNameAr)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR, reloaded.doctorSpecialtyAr)
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, reloaded.clinicPhone)
    }

    @Test
    fun namesCappedAt80Characters_phoneCappedAt24AndFiltered() {
        val repo = createRepo()

        val longNameEn = "Dr. " + "A".repeat(100)
        val longSpecialtyEn = "Consultant " + "B".repeat(100)
        val longNameAr = "د. " + "ش".repeat(100)
        val longSpecialtyAr = "أخصائي " + "ت".repeat(100)
        val rawPhoneWithIllegalChars = "Phone: +20 (100) 123-4567 ext #999!"

        repo.updateClinicAttestation(
            nameEn = longNameEn,
            specialtyEn = longSpecialtyEn,
            nameAr = longNameAr,
            specialtyAr = longSpecialtyAr,
            phone = rawPhoneWithIllegalChars,
        )

        val state = repo.settings.value
        assertEquals(80, state.doctorNameEn.length)
        assertEquals(longNameEn.take(80), state.doctorNameEn)

        assertEquals(80, state.doctorSpecialtyEn.length)
        assertEquals(longSpecialtyEn.take(80), state.doctorSpecialtyEn)

        assertEquals(80, state.doctorNameAr.length)
        assertEquals(longNameAr.take(80), state.doctorNameAr)

        assertEquals(80, state.doctorSpecialtyAr.length)
        assertEquals(longSpecialtyAr.take(80), state.doctorSpecialtyAr)

        // Phone filtering: only digits, +, space, -, (, ) are allowed, capped at 24 chars
        // "+20 (100) 123-4567  999" has length 24
        val expectedFilteredPhone = "+20 (100) 123-4567  999"
        assertEquals(expectedFilteredPhone, state.clinicPhone)
        assertTrue(state.clinicPhone.length <= 24)

        // Verify phone with length > 24 is truncated to 24
        val extraLongPhone = "123456789012345678901234567890"
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", extraLongPhone)
        assertEquals("123456789012345678901234", repo.settings.value.clinicPhone)
        assertEquals(24, repo.settings.value.clinicPhone.length)

        // Verify phone with only illegal characters falls back to default
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "invalid-letters-only-no-digits")
        // "invalid-letters-only-no-digits" contains '-' characters, which filter to "-----"
        // Let's test with characters containing ZERO allowed phone characters:
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "abcdef!@#$%^&*")
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, repo.settings.value.clinicPhone)
    }

    @Test
    fun arabicAcceptedInArabicFields() {
        val repo = createRepo()
        val customArabicName = "د. شريف جمال رضوان"
        val customArabicSpecialty = "استشاري أمراض النساء والتوليد والحقن المجهري"

        repo.updateClinicAttestation(
            nameEn = "Dr. Sherif Radwan",
            specialtyEn = "Consultant OB/GYN",
            nameAr = customArabicName,
            specialtyAr = customArabicSpecialty,
            phone = "01201720808",
        )

        val state = repo.settings.value
        assertEquals(customArabicName, state.doctorNameAr)
        assertEquals(customArabicSpecialty, state.doctorSpecialtyAr)

        // Verify reload retains Arabic text
        val reloaded = createRepo().settings.value
        assertEquals(customArabicName, reloaded.doctorNameAr)
        assertEquals(customArabicSpecialty, reloaded.doctorSpecialtyAr)
    }

    @Test
    fun existingInstallWithoutNewKeys_loadsDefaults() {
        // Pre-populate preferences as an existing install with older keys but no attestation keys
        prefs.edit()
            .putString("key_clinic_name", "My Established Clinic")
            .putString("key_clinic_subtitle", "Obstetrics & Perinatology")
            .putBoolean("key_biometric_enabled", true)
            .putString("key_auto_lock_timeout", AutoLockTimeout.FIVE_MINUTES.name)
            .apply()

        val repo = createRepo()
        val state = repo.settings.value

        // Existing settings preserved
        assertEquals("My Established Clinic", state.clinicName)
        assertEquals("Obstetrics & Perinatology", state.clinicSubtitle)
        assertTrue(state.isBiometricEnabled)
        assertEquals(AutoLockTimeout.FIVE_MINUTES, state.autoLockTimeout)

        // Attestation fields load canonical defaults
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN, state.doctorNameEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN, state.doctorSpecialtyEn)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR, state.doctorNameAr)
        assertEquals(SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR, state.doctorSpecialtyAr)
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, state.clinicPhone)
    }

    @Test
    fun updateClinicAttestation_leavesPinAndClinicBrandingStateUntouched() {
        val repo = createRepo()

        // Set up PIN and custom clinic branding
        val pin = "483926"
        assertTrue(repo.updatePin(pin))
        repo.updateClinicBranding("Premier Maternity Clinic", "Excellence in Fetal Medicine")

        val stateBefore = repo.settings.value
        assertTrue(stateBefore.isPinConfigured)
        assertEquals("Premier Maternity Clinic", stateBefore.clinicName)
        assertEquals("Excellence in Fetal Medicine", stateBefore.clinicSubtitle)

        // Call updateClinicAttestation
        repo.updateClinicAttestation(
            nameEn = "Dr. Custom Name",
            specialtyEn = "Custom Specialty",
            nameAr = "د. اسم مخصص",
            specialtyAr = "تخصص مخصص",
            phone = "01098765432",
        )

        val stateAfter = repo.settings.value

        // PIN state and branding remain untouched
        assertTrue(stateAfter.isPinConfigured)
        assertEquals(stateBefore.needsPinSetup, stateAfter.needsPinSetup)
        assertEquals(stateBefore.isLegacyPin, stateAfter.isLegacyPin)
        assertEquals("Premier Maternity Clinic", stateAfter.clinicName)
        assertEquals("Excellence in Fetal Medicine", stateAfter.clinicSubtitle)
        assertTrue("PIN still verifies successfully", repo.validatePin(pin))

        // Attestation state reflects the new values
        assertEquals("Dr. Custom Name", stateAfter.doctorNameEn)
        assertEquals("Custom Specialty", stateAfter.doctorSpecialtyEn)
        assertEquals("د. اسم مخصص", stateAfter.doctorNameAr)
        assertEquals("تخصص مخصص", stateAfter.doctorSpecialtyAr)
        assertEquals("01098765432", stateAfter.clinicPhone)
    }

    @Test
    fun arabicIndicAndPersianPhoneDigits_normalizedToAsciiAndReloaded() {
        val repo = createRepo()

        // Arabic-Indic digits (U+0660..U+0669)
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "٠١٢٠١٧٢٠٨٠٨")
        assertEquals("01201720808", repo.settings.value.clinicPhone)
        assertEquals("01201720808", createRepo().settings.value.clinicPhone)

        // Persian digits (U+06F0..U+06F9)
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "۰۱۲۳۴۵۶۷۸۹")
        assertEquals("0123456789", repo.settings.value.clinicPhone)

        // Mixed ASCII + Arabic-Indic with plus and spaces
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "+٢٠ 100 ٠٠٠")
        assertEquals("+20 100 000", repo.settings.value.clinicPhone)

        // Letters are still removed while Arabic-Indic digits are preserved
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "Tel: ٠١٠abc١٢٣")
        assertEquals("010123", repo.settings.value.clinicPhone)

        // Blank still falls back to default
        repo.updateClinicAttestation("Name", "Spec", "اسم", "تخصص", "   ")
        assertEquals(SecuritySettingsState.DEFAULT_CLINIC_PHONE, repo.settings.value.clinicPhone)
    }
}
