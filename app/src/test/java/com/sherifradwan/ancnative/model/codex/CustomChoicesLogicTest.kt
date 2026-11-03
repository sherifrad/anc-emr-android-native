package com.sherifradwan.ancnative.model.codex

import android.content.SharedPreferences
import com.sherifradwan.ancnative.storage.BackupInspectionResult
import com.sherifradwan.ancnative.storage.EncryptedBackupManager
import com.sherifradwan.ancnative.storage.PatientWithVisits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CustomChoicesLogicTest {

    // -------------------------------------------------------------
    // Oracle O1: Pure validation rules
    // -------------------------------------------------------------

    @Test
    fun validation_blankOrWhitespace_rejected() {
        val r1 = validateCustomChoice(null, emptyList())
        assertTrue(r1 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.BLANK, (r1 as CustomChoiceValidationResult.Invalid).reason)

        val r2 = validateCustomChoice("", emptyList())
        assertTrue(r2 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.BLANK, (r2 as CustomChoiceValidationResult.Invalid).reason)

        val r3 = validateCustomChoice("      ", emptyList())
        assertTrue(r3 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.BLANK, (r3 as CustomChoiceValidationResult.Invalid).reason)
    }

    @Test
    fun validation_tooLong_rejected() {
        val maxLen = 80
        val exactLength = "a".repeat(maxLen)
        val validResult = validateCustomChoice(exactLength, emptyList(), maxLength = maxLen)
        assertTrue(validResult is CustomChoiceValidationResult.Valid)
        assertEquals(exactLength, (validResult as CustomChoiceValidationResult.Valid).sanitizedLabel)

        val tooLong = "a".repeat(maxLen + 1)
        val r = validateCustomChoice(tooLong, emptyList(), maxLength = maxLen)
        assertTrue(r is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.TOO_LONG, (r as CustomChoiceValidationResult.Invalid).reason)
    }

    @Test
    fun validation_attentionPhrase_allowsUpTo140Chars() {
        val maxLen = maxCustomChoiceLength(CustomChoiceList.CLINICAL_ATTENTION_PHRASE)
        assertEquals(140, maxLen)

        val length140 = "x".repeat(140)
        val validResult = validateCustomChoice(length140, emptyList(), maxLength = maxLen)
        assertTrue(validResult is CustomChoiceValidationResult.Valid)

        val length141 = "x".repeat(141)
        val r = validateCustomChoice(length141, emptyList(), maxLength = maxLen)
        assertTrue(r is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.TOO_LONG, (r as CustomChoiceValidationResult.Invalid).reason)
    }

    @Test
    fun validation_controlCharactersAndLineBreaks_rejected() {
        // Line breaks
        val r1 = validateCustomChoice("First Line\nSecond Line", emptyList())
        assertTrue(r1 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r1 as CustomChoiceValidationResult.Invalid).reason)

        val r2 = validateCustomChoice("Text\rCarriage", emptyList())
        assertTrue(r2 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r2 as CustomChoiceValidationResult.Invalid).reason)

        // Trailing newline before trim must also be rejected
        val r3 = validateCustomChoice("Trailing\n", emptyList())
        assertTrue(r3 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r3 as CustomChoiceValidationResult.Invalid).reason)

        // Tabs and NUL bytes
        val r4 = validateCustomChoice("Tab\tSeparated", emptyList())
        assertTrue(r4 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r4 as CustomChoiceValidationResult.Invalid).reason)

        val r5 = validateCustomChoice("Null\u0000Byte", emptyList())
        assertTrue(r5 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r5 as CustomChoiceValidationResult.Invalid).reason)

        // Unicode line and paragraph separators
        val r6 = validateCustomChoice("Line\u2028Separator", emptyList())
        assertTrue(r6 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r6 as CustomChoiceValidationResult.Invalid).reason)

        val r7 = validateCustomChoice("Paragraph\u2029Separator", emptyList())
        assertTrue(r7 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS, (r7 as CustomChoiceValidationResult.Invalid).reason)
    }

    @Test
    fun validation_caseInsensitiveDuplicate_rejected() {
        val existing = listOf("Oral", "Intravenous", "Subcutaneous")

        val r1 = validateCustomChoice("oral", existing)
        assertTrue(r1 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.DUPLICATE, (r1 as CustomChoiceValidationResult.Invalid).reason)

        val r2 = validateCustomChoice("  ORAL  ", existing)
        assertTrue(r2 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.DUPLICATE, (r2 as CustomChoiceValidationResult.Invalid).reason)

        val r3 = validateCustomChoice("subcutaneous", existing)
        assertTrue(r3 is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.DUPLICATE, (r3 as CustomChoiceValidationResult.Invalid).reason)
    }

    @Test
    fun validation_validInput_trimsAndSucceeds() {
        val r = validateCustomChoice("   Sublingual Drops   ", listOf("Oral", "IV"))
        assertTrue(r is CustomChoiceValidationResult.Valid)
        assertEquals("Sublingual Drops", (r as CustomChoiceValidationResult.Valid).sanitizedLabel)
    }

    // -------------------------------------------------------------
    // Oracle O2: Clinical safety - dating methods invariance
    // -------------------------------------------------------------

    @Test
    fun datingMethods_notInCustomChoiceList() {
        // Clinical safety rule (Section 2.3): Dating methods (LMP, IVF, Scan, Clinical)
        // are fixed and have no "+ Add other".
        val allowedLists = setOf(
            CustomChoiceList.MEDICATION_NAME,
            CustomChoiceList.MEDICATION_DOSE,
            CustomChoiceList.MEDICATION_ROUTE,
            CustomChoiceList.LAB_TEST,
            CustomChoiceList.LAB_RESULT,
            CustomChoiceList.LAB_CATEGORY,
            CustomChoiceList.ULTRASOUND_PRESENTATION,
            CustomChoiceList.ULTRASOUND_PLACENTA,
            CustomChoiceList.ULTRASOUND_LIQUOR,
            CustomChoiceList.ULTRASOUND_FINDING,
            CustomChoiceList.PREVIOUS_PREGNANCY_OUTCOME,
            CustomChoiceList.PREVIOUS_PREGNANCY_MODE,
            CustomChoiceList.PREVIOUS_PREGNANCY_COMPLICATION,
            CustomChoiceList.CLINICAL_ATTENTION_PHRASE,
            CustomChoiceList.VISIT_OEDEMA,
            CustomChoiceList.VISIT_MOVEMENTS,
            CustomChoiceList.VISIT_URINE,
            CustomChoiceList.VISIT_BP,
            CustomChoiceList.VISIT_WEIGHT,
            CustomChoiceList.VISIT_FUNDAL_HEIGHT,
        )

        assertEquals(allowedLists, CustomChoiceList.entries.toSet())
        val names = CustomChoiceList.entries.map { it.name }
        assertFalse(names.any { it.contains("DATING", ignoreCase = true) })
        assertFalse(names.any { it.contains("LMP", ignoreCase = true) })
        assertFalse(names.any { it.contains("IVF", ignoreCase = true) })
    }

    // -------------------------------------------------------------
    // Oracle O3: Real Repository Tests (Persistence, Reload, Undo & Duplicate Protection)
    // -------------------------------------------------------------

    @Test
    fun clinicalCodexRepository_fullPersistenceAndLifecycle() {
        val fakePrefs = FakeSharedPreferences()
        val repo1 = ClinicalCodexRepository(fakePrefs)

        val listKind = CustomChoiceList.MEDICATION_ROUTE
        assertEquals(0, repo1.customChoices(listKind).value.size)

        // 1. Add valid custom choice
        val addRes = repo1.addCustomChoice(listKind, "Sublingual", existingDefaults = listOf("Oral", "IV"))
        assertTrue(addRes is CustomChoiceValidationResult.Valid)
        assertEquals(1, repo1.customChoices(listKind).value.size)
        val item1 = repo1.customChoices(listKind).value.first()
        assertEquals("Sublingual", item1.label)
        assertEquals(listKind, item1.list)

        // Add a second item to verify ordered indices on undo
        val addRes2 = repo1.addCustomChoice(listKind, "Transdermal")
        assertTrue(addRes2 is CustomChoiceValidationResult.Valid)
        assertEquals(2, repo1.customChoices(listKind).value.size)
        val item2 = repo1.customChoices(listKind).value[1]
        assertEquals("Transdermal", item2.label)

        // 2. Persistence check across instance reload
        val repo2 = ClinicalCodexRepository(fakePrefs)
        val reloaded = repo2.customChoices(listKind).value
        assertEquals(2, reloaded.size)
        assertEquals("Sublingual", reloaded[0].label)
        assertEquals("Transdermal", reloaded[1].label)

        // 3. Rename check
        val renameRes = repo2.renameCustomChoice(listKind, item1.id, "Sublingual Drops")
        assertTrue(renameRes is CustomChoiceValidationResult.Valid)
        assertEquals("Sublingual Drops", repo2.customChoices(listKind).value[0].label)

        // Rename to non-existent ID fails with NOT_FOUND
        val badRename = repo2.renameCustomChoice(listKind, "non-existent-id", "Valid Label")
        assertTrue(badRename is CustomChoiceValidationResult.Invalid)
        assertEquals(CustomChoiceValidationResult.Invalid.Reason.NOT_FOUND, (badRename as CustomChoiceValidationResult.Invalid).reason)

        // 4. Remove item 1 (index 0)
        val removed = repo2.removeCustomChoice(listKind, item1.id)
        assertNotNull(removed)
        assertEquals("Sublingual Drops", removed?.item?.label)
        assertEquals(0, removed?.originalIndex)
        assertEquals(1, repo2.customChoices(listKind).value.size)
        assertEquals("Transdermal", repo2.customChoices(listKind).value[0].label)

        // 5. Duplicate protection on Undo:
        // If a user removes "Sublingual Drops", then adds "sublingual drops", tapping Undo must NOT create a duplicate!
        val reAdd = repo2.addCustomChoice(listKind, "sublingual drops")
        assertTrue(reAdd is CustomChoiceValidationResult.Valid)
        val undoResult = repo2.restoreCustomChoice(removed!!)
        assertFalse("Undo must refuse to restore if same label already exists", undoResult)

        // Now remove the duplicate, and undo should successfully restore to index 0
        repo2.removeCustomChoice(listKind, repo2.customChoices(listKind).value.last().id)
        val successfulUndo = repo2.restoreCustomChoice(removed)
        assertTrue(successfulUndo)
        assertEquals(2, repo2.customChoices(listKind).value.size)
        assertEquals("Sublingual Drops", repo2.customChoices(listKind).value[0].label)
        assertEquals("Transdermal", repo2.customChoices(listKind).value[1].label)
    }

    @Test
    fun clinicalCodexRepository_restoreFromBackupValidatesSafely() {
        val fakePrefs = FakeSharedPreferences()
        val repo = ClinicalCodexRepository(fakePrefs)

        val backupData = mapOf(
            CustomChoiceList.VISIT_OEDEMA.storageKey to listOf(
                "Trace tibial",
                "   ",             // corrupted: blank
                "Trace tibial",     // duplicate
                "Valid sacral",
                "Multiline\nError", // invalid characters
            )
        )

        repo.restoreCustomChoicesFromBackup(backupData)
        val oedemaChoices = repo.customChoices(CustomChoiceList.VISIT_OEDEMA).value
        assertEquals(2, oedemaChoices.size)
        assertEquals("Trace tibial", oedemaChoices[0].label)
        assertEquals("Valid sacral", oedemaChoices[1].label)
    }

    // -------------------------------------------------------------
    // Oracle O4: Encrypted Backup & Restore with Custom Choices
    // -------------------------------------------------------------

    @Test
    fun encryptedBackup_preservesCustomChoicesRoundtrip() {
        val vaultUuid = UUID.randomUUID().toString()
        val passphrase = "Passphrase#2026".toCharArray()
        val customChoicesMap = mapOf(
            CustomChoiceList.MEDICATION_ROUTE.storageKey to listOf("Sublingual", "Transdermal"),
            CustomChoiceList.VISIT_OEDEMA.storageKey to listOf("Trace tibial"),
            CustomChoiceList.CLINICAL_ATTENTION_PHRASE.storageKey to listOf("Previous ectopic", "Bicornuate uterus"),
        )

        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = emptyList<PatientWithVisits>(),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
            customChoices = customChoicesMap,
        )

        assertTrue(backupBytes.isNotEmpty())
        val inspection = EncryptedBackupManager.inspectBackup(backupBytes, passphrase)
        assertTrue(inspection is BackupInspectionResult.Valid)
        val valid = inspection as BackupInspectionResult.Valid
        assertEquals(3, valid.schemaVersion)

        val payload = EncryptedBackupManager.decryptAndParseBackup(backupBytes, passphrase)
        assertNotNull(payload)
        assertEquals(3, payload?.schemaVersion)
        assertEquals(customChoicesMap, payload?.customChoices)
    }

    @Test
    fun encryptedBackup_legacyPayloadWithoutCustomChoices_parsesGracefully() {
        val vaultUuid = UUID.randomUUID().toString()
        val passphrase = "Passphrase#2026".toCharArray()

        // Create backup without custom choices (emptyMap default)
        val backupBytes = EncryptedBackupManager.createEncryptedBackup(
            vaultUuid = vaultUuid,
            patients = emptyList<PatientWithVisits>(),
            passphrase = passphrase,
            exportedAtEpochMs = 1775890000000L,
        )

        val payload = EncryptedBackupManager.decryptAndParseBackup(backupBytes, passphrase)
        assertNotNull(payload)
        assertEquals(3, payload?.schemaVersion)
        assertTrue(payload?.customChoices?.isEmpty() == true)
    }
}

/**
 * In-memory FakeSharedPreferences implementation for unit testing SharedPreferences persistence.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        private val modifications = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) modifications[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) modifications[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) modifications[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) modifications[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) modifications[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) modifications[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) modifications[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) {
                data.clear()
            }
            for ((key, value) in modifications) {
                if (value == null) {
                    data.remove(key)
                } else {
                    data[key] = value
                }
            }
            modifications.clear()
            clear = false
        }
    }
}
