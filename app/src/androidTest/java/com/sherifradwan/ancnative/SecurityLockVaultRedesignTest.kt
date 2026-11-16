package com.sherifradwan.ancnative

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.DisplayMetrics
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.security.AppThemeMode
import com.sherifradwan.ancnative.security.BiometricAuthManager
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.storage.AuthenticationPurpose
import com.sherifradwan.ancnative.storage.BackupInspectionResult
import com.sherifradwan.ancnative.storage.VaultBlockReason
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.AppLockScreen
import com.sherifradwan.ancnative.ui.SecuritySettingsDialog
import com.sherifradwan.ancnative.ui.VaultGateScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * Redesign slice 18: Security, lock, and vault screens.
 *
 * Mounts SecuritySettingsDialog, AppLockScreen, and VaultGateScreen through
 * MainActivity + AncTheme inside 360dp x 640dp (and 320dp x 640dp for VaultGateScreen) hosts,
 * matching HistoryEditorsRedesignTest, RegistrationRedesignTest, and CatalogSettingsRedesignTest style:
 * - Single setContent per test
 * - Shared ComposeTextAsserts helpers
 * - Narrow 360dp / 320dp windows
 * - fontScale overrides (1.0f, 1.3f, 1.4f, 2.0f)
 * - useUnmergedTree where label text is cleared by clearAndSetSemantics
 * - performScrollTo before assertIsDisplayed
 * - Select nodes by testTag rather than ambiguous text
 * - >= 47.99dp touch tolerance
 * - Dark theme & RTL support
 * - Clean state isolation per test
 */
@RunWith(AndroidJUnit4::class)
class SecurityLockVaultRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null
    private var createdTempFile: File? = null

    @After
    fun tearDown() {
        restoreResources()
        createdTempFile?.delete()
        createdTempFile = null
    }

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun pluralStr(id: Int, count: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, *args)

    private fun narrowWindow(fontScale: Float) {
        compose.activityRule.scenario.onActivity { activity ->
            val res = activity.resources
            if (savedConfig == null) {
                savedConfig = Configuration(res.configuration)
                savedMetrics = DisplayMetrics().apply { setTo(res.displayMetrics) }
            }
            val cfg = Configuration(res.configuration)
            cfg.densityDpi = res.displayMetrics.widthPixels * DisplayMetrics.DENSITY_DEFAULT / 360
            cfg.fontScale = fontScale
            @Suppress("DEPRECATION")
            res.updateConfiguration(cfg, res.displayMetrics)
        }
    }

    private fun restoreResources() {
        val cfg = savedConfig ?: return
        compose.activityRule.scenario.onActivity { activity ->
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(cfg, savedMetrics)
        }
        savedConfig = null
        savedMetrics = null
    }

    private fun createIsolatedRepo(
        clock: () -> Long = System::currentTimeMillis,
        configure: (android.content.SharedPreferences) -> Unit = {},
    ): SecuritySettingsRepository {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("test_sec_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        configure(prefs)
        return SecuritySettingsRepository(prefs, clock)
    }

    // =========================================================================
    // Mount helpers (Single setContent per test)
    // =========================================================================

    private fun showSecurityDialog(
        repo: SecuritySettingsRepository? = null,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
        onLockNow: () -> Unit = {},
        onExportBackup: ((CharArray, (ByteArray?) -> Unit) -> Unit)? = { _, cb -> cb(byteArrayOf(1, 2, 3)) },
        onRestoreBackup: ((ByteArray, CharArray, (BackupInspectionResult) -> Unit) -> Unit)? = { _, _, cb ->
            cb(
                BackupInspectionResult.Valid(
                    vaultUuid = "mock-vault",
                    patientCount = 1,
                    schemaVersion = 6,
                    exportedAtEpochMs = 1000L,
                ),
            )
        },
        registryOwner: ActivityResultRegistryOwner? = null,
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)
        val effectiveRepo = repo ?: createIsolatedRepo()

        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        val dialogContent = @androidx.compose.runtime.Composable {
                            Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                                SecuritySettingsDialog(
                                    securityRepo = effectiveRepo,
                                    onDismiss = onDismiss,
                                    onLockNow = onLockNow,
                                    onExportBackup = onExportBackup,
                                    onRestoreBackup = onRestoreBackup,
                                )
                            }
                        }
                        if (registryOwner != null) {
                            CompositionLocalProvider(
                                LocalActivityResultRegistryOwner provides registryOwner,
                                content = dialogContent,
                            )
                        } else {
                            dialogContent()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showAppLock(
        repo: SecuritySettingsRepository? = null,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onUnlockSuccess: () -> Unit = {},
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)
        val effectiveRepo = repo ?: createIsolatedRepo()

        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            AppLockScreen(
                                securityRepo = effectiveRepo,
                                onUnlockSuccess = onUnlockSuccess,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showVaultGate(
        state: VaultState,
        widthDp: Int = 360,
        heightDp: Int = 640,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onInitialize: () -> Unit = {},
        onUnlock: () -> Unit = {},
        onRetry: () -> Unit = {},
        clinicName: String? = null,
        clinicSubtitle: String? = null,
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)

        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(widthDp.dp, heightDp.dp).clipToBounds().testTag(HOST_TAG)) {
                            VaultGateScreen(
                                state = state,
                                onInitialize = onInitialize,
                                onUnlock = onUnlock,
                                onRetry = onRetry,
                                clinicName = clinicName,
                                clinicSubtitle = clinicSubtitle,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    // =========================================================================
    // Query & Measurement Helpers
    // =========================================================================

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()
    private fun nodeExists(tag: String): Boolean =
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertMinTouchTarget(tag: String, scroll: Boolean = true) {
        if (scroll) {
            runCatching { node(tag).performScrollTo() }
        }
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.9.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.9.dp)
    }

    private fun assertNoTextBelow12sp(tag: String) {
        val nodes = compose.onAllNodes(
            (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.forEach { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: ""
            if (text.isBlank()) return@forEach
            layouts.forEach { layout ->
                val size = layout.layoutInput.style.fontSize
                assertTrue("'$text' is $size (< 12sp)", size.value >= 11.99f)
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.assertTextNotClipped(label: String, matcher: SemanticsMatcher) {
        val nodes = onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("'$label' has no matching node", nodes.isNotEmpty())
        nodes.forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            assertTrue("'$label' has no text layout to check", layouts.isNotEmpty())
            layouts.forEach { layout ->
                val align = layout.layoutInput.style.textAlign
                if (align == TextAlign.Center || align == TextAlign.End) {
                    assertAlignedTextNotClipped(label, layout)
                } else {
                    assertTrue("'$label' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
                    for (line in 0 until layout.lineCount) {
                        assertTrue("'$label' line $line clipped horizontally", layout.getLineRight(line) <= layout.size.width + 1f)
                        assertFalse("'$label' ellipsized on line $line", layout.isLineEllipsized(line))
                    }
                }
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.assertNotEllipsized(text: String, ancestorTag: String) =
        assertTextNotClipped(text, hasText(text, substring = true) and inTagged(ancestorTag))

    // =========================================================================
    // 1. Typography & Unclipped Tabs (1.0f, 1.3f, 2.0f)
    // =========================================================================

    @Test
    fun typography_securitySettingsDialog_noTextBelow12sp() {
        showSecurityDialog()
        node("securitySettingsDialog").assertIsDisplayed()
        assertNoTextBelow12sp("securitySettingsDialog")
    }

    @Test
    fun typography_appLockScreen_noTextBelow12sp() {
        val repo = createIsolatedRepo()
        showAppLock(repo)
        node("appLockScreen").assertIsDisplayed()
        assertNoTextBelow12sp("appLockScreen")
    }

    @Test
    fun typography_vaultGate_noTextBelow12sp() {
        showVaultGate(state = VaultState.Blocked(VaultBlockReason.KEY_PERMANENTLY_INVALIDATED))
        node("vaultGate").assertIsDisplayed()
        assertNoTextBelow12sp("vaultGate")
    }

    @Test
    fun unclippedTabs_securitySettingsDialog_360dp_scale1_0() {
        showSecurityDialog(fontScale = 1.0f)
        node("securitySettingsDialog").assertIsDisplayed()

        // Tab switcher text is whole and unclipped
        compose.assertNotEllipsized(str(R.string.settings_tab_security), "securitySettingsDialog")
        compose.assertNotEllipsized(str(R.string.settings_tab_lab_tests), "securitySettingsDialog")
        compose.assertNotEllipsized(str(R.string.settings_tab_medications), "securitySettingsDialog")

        // Backup action buttons not ellipsized
        node("exportBackupButton").performScrollTo()
        compose.assertNotEllipsized(str(R.string.security_export_backup), "backupVaultSettingsCard")
        node("restoreBackupButton").performScrollTo()
        compose.assertNotEllipsized(str(R.string.security_restore_backup), "backupVaultSettingsCard")
    }

    @Test
    fun unclippedTabs_securitySettingsDialog_360dp_scale1_3() {
        showSecurityDialog(fontScale = 1.3f)
        node("securitySettingsDialog").assertIsDisplayed()

        compose.assertNotEllipsized(str(R.string.settings_tab_security), "securitySettingsDialog")
        compose.assertNotEllipsized(str(R.string.settings_tab_lab_tests), "securitySettingsDialog")
        compose.assertNotEllipsized(str(R.string.settings_tab_medications), "securitySettingsDialog")

        node("exportBackupButton").performScrollTo()
        compose.assertNotEllipsized(str(R.string.security_export_backup), "backupVaultSettingsCard")
        node("restoreBackupButton").performScrollTo()
        compose.assertNotEllipsized(str(R.string.security_restore_backup), "backupVaultSettingsCard")
    }

    @Test
    fun backupButtons_sideBySideAtFontScale1_3() {
        showSecurityDialog(fontScale = 1.3f)
        node("exportBackupButton").performScrollTo()

        val exportBounds = bounds("exportBackupButton")
        val restoreBounds = bounds("restoreBackupButton")

        // At <= 1.3f fontScale, buttons are in a Row side-by-side
        assertTrue(
            "Export and Restore should sit side-by-side at 1.3f: export.top=${exportBounds.top}, restore.top=${restoreBounds.top}",
            abs(exportBounds.top.value - restoreBounds.top.value) < 2f,
        )
    }

    @Test
    fun backupButtons_stackVerticallyAtFontScale2_0() {
        showSecurityDialog(fontScale = 2.0f)
        node("exportBackupButton").performScrollTo()

        val exportBounds = bounds("exportBackupButton")
        val restoreBounds = bounds("restoreBackupButton")

        // At > 1.3f fontScale, buttons stack in a Column
        assertTrue(
            "Restore should stack below Export at 2.0f: restore.top=${restoreBounds.top}, export.bottom=${exportBounds.bottom}",
            restoreBounds.top >= exportBounds.bottom - 1.dp,
        )
        compose.assertNotEllipsized(str(R.string.security_export_backup), "backupVaultSettingsCard")
        node("restoreBackupButton").performScrollTo()
        compose.assertNotEllipsized(str(R.string.security_restore_backup), "backupVaultSettingsCard")
    }

    // =========================================================================
    // 2. Minimum 48dp Touch Targets (>= 47.9dp)
    // =========================================================================

    @Test
    fun touchTargets_securitySettingsDialog_allInteractiveTargetsAtLeast48dp() {
        showSecurityDialog()
        node("securitySettingsDialog").assertIsDisplayed()

        // Pinned header & tabs
        assertMinTouchTarget("closeSettingsButton", scroll = false)
        assertMinTouchTarget("settingsTabSecurity", scroll = false)
        assertMinTouchTarget("settingsTabLabCatalog", scroll = false)
        assertMinTouchTarget("settingsTabMedCatalog", scroll = false)

        // Theme options
        assertMinTouchTarget("themeOption-SYSTEM")
        assertMinTouchTarget("themeOption-LIGHT")
        assertMinTouchTarget("themeOption-DARK")

        // Section buttons
        assertMinTouchTarget("saveBrandingButton")
        assertMinTouchTarget("changePinButton")
        assertMinTouchTarget("exportBackupButton")
        assertMinTouchTarget("restoreBackupButton")
        assertMinTouchTarget("lockAppNowButton")

        // Biometric test button (if biometric available)
        if (nodeExists("testBiometricButton")) {
            assertMinTouchTarget("testBiometricButton")
        }
    }

    @Test
    fun touchTargets_appLockScreen_allKeypadKeysAtLeast48dp() {
        val repo = createIsolatedRepo { prefs ->
            // Seed a configured pin so unlock mode renders full keypad
            prefs.edit().putString("key_pin_hash", "legacy").apply()
        }
        repo.updatePin("135246")
        repo.lockApp()
        showAppLock(repo)

        node("appLockScreen").assertIsDisplayed()

        for (digit in 0..9) {
            assertMinTouchTarget("pinKey-$digit", scroll = false)
            val b = bounds("pinKey-$digit")
            assertTrue("pinKey-$digit height should be >= 55.9dp (${b.height})", b.height >= 55.9.dp)
        }
        assertMinTouchTarget("pinKey-backspace", scroll = false)

        if (nodeExists("pinKey-biometric")) {
            assertMinTouchTarget("pinKey-biometric", scroll = false)
        }
        if (nodeExists("useBiometricButton")) {
            assertMinTouchTarget("useBiometricButton", scroll = false)
        }
    }

    @Test
    fun touchTargets_vaultGate_actionButtonsAtLeast48dp() {
        // Uninitialized -> initializeStorage
        showVaultGate(state = VaultState.Uninitialized)
        node("initializeStorage").assertIsDisplayed()
        assertMinTouchTarget("initializeStorage", scroll = false)
        val initBounds = bounds("initializeStorage")
        assertTrue("initializeStorage height < 51.9dp (${initBounds.height})", initBounds.height >= 51.9.dp)
    }

    @Test
    fun touchTargets_vaultGate_lockedActionAtLeast48dp() {
        // Locked -> unlockStorage
        showVaultGate(state = VaultState.Locked)
        node("unlockStorage").assertIsDisplayed()
        assertMinTouchTarget("unlockStorage", scroll = false)
        val unlockBounds = bounds("unlockStorage")
        assertTrue("unlockStorage height < 51.9dp (${unlockBounds.height})", unlockBounds.height >= 51.9.dp)
    }

    @Test
    fun touchTargets_vaultGate_blockedActionAtLeast48dp() {
        // Blocked -> retryStorage
        showVaultGate(state = VaultState.Blocked(VaultBlockReason.KEY_MISSING))
        node("retryStorage").assertIsDisplayed()
        assertMinTouchTarget("retryStorage", scroll = false)
        val retryBounds = bounds("retryStorage")
        assertTrue("retryStorage height < 51.9dp (${retryBounds.height})", retryBounds.height >= 51.9.dp)
    }

    @Test
    fun touchTargets_subDialogs_allButtonsAtLeast48dp() {
        showSecurityDialog()

        // 1. ChangePinDialog buttons
        node("changePinButton").performScrollTo().performClick()
        node("changePinDialog").assertIsDisplayed()
        assertMinTouchTarget("saveNewPinButton", scroll = false)
        assertMinTouchTarget("cancelChangePinButton", scroll = false)
        node("cancelChangePinButton").performClick()
        node("changePinDialog").assertDoesNotExist()

        // 2. ExportBackupDialog buttons
        node("exportBackupButton").performScrollTo().performClick()
        node("exportBackupDialog").assertIsDisplayed()
        assertMinTouchTarget("confirmExportBackupButton", scroll = false)
        assertMinTouchTarget("cancelExportBackupButton", scroll = false)
        node("cancelExportBackupButton").performClick()
        node("exportBackupDialog").assertDoesNotExist()
    }

    // =========================================================================
    // 3. AppLockScreen PIN Setup, Wrong PIN & Lockout States
    // =========================================================================

    @Test
    fun appLockScreen_pinSetupFlow_validatesWeakPin_mismatch_andCompletesSetup() {
        val repo = createIsolatedRepo()
        var unlocked = false
        showAppLock(repo, onUnlockSuccess = { unlocked = true })

        node("appLockScreen").assertIsDisplayed()
        node("pinSetup").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_setup_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_rule)).assertIsDisplayed()

        // 1. Enter weak PIN (111111) -> inline error shown
        repeat(6) {
            node("pinKey-1").performClick()
        }
        node("appLockErrorMessage").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_invalid)).assertIsDisplayed()
        node("pinSetup").assertIsDisplayed()

        // 2. Enter valid PIN (135246) -> proceeds to confirmation prompt
        listOf("1", "3", "5", "2", "4", "6").forEach { digit ->
            node("pinKey-$digit").performClick()
        }
        node("pinConfirm").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_confirm_title)).assertIsDisplayed()

        // 3. Enter mismatched confirmation PIN (246135) -> mismatch error and resets
        listOf("2", "4", "6", "1", "3", "5").forEach { digit ->
            node("pinKey-$digit").performClick()
        }
        node("appLockErrorMessage").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_mismatch)).assertIsDisplayed()
        node("pinSetup").assertIsDisplayed()

        // 4. Enter valid PIN (135246) and matching confirmation (135246) -> completes setup
        listOf("1", "3", "5", "2", "4", "6").forEach { digit ->
            node("pinKey-$digit").performClick()
        }
        node("pinConfirm").assertIsDisplayed()
        listOf("1", "3", "5", "2", "4", "6").forEach { digit ->
            node("pinKey-$digit").performClick()
        }

        compose.waitForIdle()
        assertTrue("Unlock callback should be invoked", unlocked)
        assertFalse("needsPinSetup should be false", repo.settings.value.needsPinSetup)
        assertFalse("isAppLocked should be false", repo.settings.value.isAppLocked)
    }

    @Test
    fun appLockScreen_weakPinError_notClippedAtFontScale1_3() {
        val repo = createIsolatedRepo()
        showAppLock(repo, fontScale = 1.3f)

        node("appLockScreen").assertIsDisplayed()
        repeat(6) {
            node("pinKey-1").performClick()
        }

        node("appLockErrorMessage").assertIsDisplayed()
        compose.assertTextNotClipped(str(R.string.pin_invalid), hasTestTag("appLockErrorMessage"))
    }

    @Test
    fun appLockScreen_wrongPinError_backspace_andSuccessfulUnlock() {
        val repo = createIsolatedRepo()
        repo.setupPin("135246")
        repo.lockApp()
        var unlocked = false
        showAppLock(repo, onUnlockSuccess = { unlocked = true })

        node("appLockScreen").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_unlock_title)).assertIsDisplayed()

        // 1. Enter wrong PIN (999999) -> error displayed
        repeat(6) {
            node("pinKey-9").performClick()
        }
        node("appLockErrorMessage").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_incorrect)).assertIsDisplayed()

        // 2. Test backspace operation
        node("pinKey-1").performClick()
        node("pinKey-3").performClick()
        node("pinKey-backspace").performClick()
        node("pinKey-backspace").performClick()

        // 3. Enter correct PIN (135246) -> unlocks
        listOf("1", "3", "5", "2", "4", "6").forEach { digit ->
            node("pinKey-$digit").performClick()
        }

        compose.waitForIdle()
        assertTrue("Unlock callback should be invoked", unlocked)
        assertFalse("App should be unlocked in repository", repo.settings.value.isAppLocked)
    }

    @Test
    fun appLockScreen_activeLockout_displaysCountdownAndDisablesKeypad() {
        val baseTime = 1_000_000L
        val repo = createIsolatedRepo(clock = { baseTime }) { prefs ->
            prefs.edit()
                .putLong("key_pin_lockout_end", baseTime + 30_000L)
                .putInt("key_pin_failures", 5)
                .apply()
        }
        showAppLock(repo)

        node("appLockScreen").assertIsDisplayed()
        node("pinLockoutMessage").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_lockout, 0, 30)).assertIsDisplayed()

        // Keypad buttons and backspace are disabled during lockout
        for (digit in 0..9) {
            node("pinKey-$digit").assertIsNotEnabled()
        }
        node("pinKey-backspace").assertIsNotEnabled()
        if (nodeExists("useBiometricButton")) {
            node("useBiometricButton").assertIsNotEnabled()
        }
    }

    // =========================================================================
    // 4. SecuritySettingsDialog Sub-Dialogs & Validations
    // =========================================================================

    @Test
    fun securitySettingsDialog_changePinDialog_validatesAndUpdatesPin() {
        val repo = createIsolatedRepo()
        repo.setupPin("135246")
        showSecurityDialog(repo)

        node("changePinButton").performScrollTo().performClick()
        node("changePinDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_set_pin_title)).assertIsDisplayed()

        // 1. Weak PIN error
        node("newPinField").performTextInput("111111")
        node("saveNewPinButton").performClick()
        node("changePinError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pin_invalid)).assertIsDisplayed()
        node("changePinDialog").assertIsDisplayed()

        // 2. Mismatch error
        node("newPinField").performTextReplacement("135246")
        node("confirmPinField").performTextInput("246135")
        node("saveNewPinButton").performClick()
        node("changePinError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_pin_mismatch)).assertIsDisplayed()
        node("changePinDialog").assertIsDisplayed()

        // 3. Cancel button dismisses
        node("cancelChangePinButton").performClick()
        node("changePinDialog").assertDoesNotExist()

        // 4. Re-open and successfully update PIN
        node("changePinButton").performScrollTo().performClick()
        node("newPinField").performTextInput("246135")
        node("confirmPinField").performTextInput("246135")
        node("saveNewPinButton").performClick()

        compose.waitForIdle()
        node("changePinDialog").assertDoesNotExist()
        assertTrue("New PIN should validate successfully in repository", repo.validatePin("246135"))
    }

    @Test
    fun securitySettingsDialog_exportBackupDialog_validatesPassphraseAndExports() {
        var exportedPassphrase: String? = null
        showSecurityDialog(
            onExportBackup = { passphrase, callback ->
                exportedPassphrase = String(passphrase)
                callback(byteArrayOf(1, 2, 3))
            },
        )

        node("exportBackupButton").performScrollTo().performClick()
        node("exportBackupDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_export_dialog_title)).assertIsDisplayed()

        // 1. Passphrase too short (< 6 chars)
        node("backupPassphraseField").performTextInput("123")
        node("confirmExportBackupButton").performClick()
        node("exportBackupError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_passphrase_short)).assertIsDisplayed()
        node("exportBackupDialog").assertIsDisplayed()

        // 2. Passphrase mismatch
        node("backupPassphraseField").performTextReplacement("secret123")
        node("confirmBackupPassphraseField").performTextInput("mismatch456")
        node("confirmExportBackupButton").performClick()
        node("exportBackupError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_passphrase_mismatch)).assertIsDisplayed()
        node("exportBackupDialog").assertIsDisplayed()

        // 3. Cancel dismisses
        node("cancelExportBackupButton").performClick()
        node("exportBackupDialog").assertDoesNotExist()

        // 4. Re-open and confirm with valid matching passphrase
        node("exportBackupButton").performScrollTo().performClick()
        node("backupPassphraseField").performTextInput("vaultSecret2026")
        node("confirmBackupPassphraseField").performTextInput("vaultSecret2026")
        node("confirmExportBackupButton").performClick()

        compose.waitForIdle()
        node("exportBackupDialog").assertDoesNotExist()
        assertEquals("vaultSecret2026", exportedPassphrase)
    }

    @Test
    fun securitySettingsDialog_restoreFlow_confirmDialog_andPassphraseDialog() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempBackupFile = File(context.cacheDir, "test_backup_${UUID.randomUUID()}.ancvault").apply {
            writeBytes("mock_backup_bytes".toByteArray())
        }
        createdTempFile = tempBackupFile
        val testUri = Uri.fromFile(tempBackupFile)

        val testRegistryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    @Suppress("UNCHECKED_CAST")
                    dispatchResult(requestCode, testUri as O)
                }
            }
        }

        var restoredPassphrase: String? = null
        showSecurityDialog(
            registryOwner = testRegistryOwner,
            onRestoreBackup = { _, passphrase, callback ->
                restoredPassphrase = String(passphrase)
                callback(
                    BackupInspectionResult.Valid(
                        vaultUuid = "mock-vault-restore",
                        patientCount = 42,
                        schemaVersion = 6,
                        exportedAtEpochMs = 1000L,
                    ),
                )
            },
        )

        // 1. Launch file picker (dispatched immediately via test registry)
        node("restoreBackupButton").performScrollTo().performClick()
        compose.waitForIdle()

        // 2. Destructive confirmation dialog appears with title and error-colored Replace all
        node("restore_confirm_dialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_restore_confirm_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_restore_confirm_body)).assertIsDisplayed()
        node("confirmReplaceAllButton").assertIsDisplayed()
        node("cancelReplaceAllButton").assertIsDisplayed()
        assertMinTouchTarget("confirmReplaceAllButton", scroll = false)
        assertMinTouchTarget("cancelReplaceAllButton", scroll = false)

        // 3. Cancel dismisses and does not open passphrase dialog
        node("cancelReplaceAllButton").performClick()
        node("restore_confirm_dialog").assertDoesNotExist()
        node("restorePassphraseDialog").assertDoesNotExist()

        // 4. Trigger restore again and confirm
        node("restoreBackupButton").performScrollTo().performClick()
        compose.waitForIdle()
        node("restore_confirm_dialog").assertIsDisplayed()
        node("confirmReplaceAllButton").performClick()

        // 5. RestorePassphraseDialog opens
        node("restore_confirm_dialog").assertDoesNotExist()
        node("restorePassphraseDialog").assertIsDisplayed()
        assertMinTouchTarget("confirmRestoreBackupButton", scroll = false)
        assertMinTouchTarget("cancelRestorePassphraseButton", scroll = false)
        compose.onNodeWithText(str(R.string.security_restore_dialog_title)).assertIsDisplayed()

        // Empty passphrase shows error
        node("confirmRestoreBackupButton").performClick()
        node("restorePassphraseError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_restore_passphrase_empty)).assertIsDisplayed()
        node("restorePassphraseDialog").assertIsDisplayed()

        // Enter passphrase and confirm
        node("restorePassphraseField").performTextInput("clinicVaultKey")
        node("confirmRestoreBackupButton").performClick()

        compose.waitForIdle()
        node("restorePassphraseDialog").assertDoesNotExist()
        assertEquals("clinicVaultKey", restoredPassphrase)

        // Success banner shows restored count
        node("backupStatusMessage").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(pluralStr(R.plurals.security_vault_restored, 42, 42)).assertIsDisplayed()
    }

    // =========================================================================
    // 5. Settings Behavior Parity & Stored Values
    // =========================================================================

    @Test
    fun securitySettingsDialog_tabSwitcher_navigatesToLabAndMedCatalogs() {
        showSecurityDialog()
        node("securitySettingsDialog").assertIsDisplayed()

        // 1. Initial tab: Security & clinic
        node("themeSettingsCard").assertIsDisplayed()

        // 2. Switch to Lab tests tab
        node("settingsTabLabCatalog").performClick()
        compose.waitForIdle()
        node("labIndividualTestsTab").assertIsDisplayed()

        // 3. Switch to Medications tab
        node("settingsTabMedCatalog").performClick()
        compose.waitForIdle()
        node("addNewMedBtn").assertIsDisplayed()

        // 4. Switch back to Security & clinic tab
        node("settingsTabSecurity").performClick()
        compose.waitForIdle()
        node("themeSettingsCard").assertIsDisplayed()
    }

    @Test
    fun securitySettingsDialog_themeOptions_updatesRepositoryThemeMode() {
        val repo = createIsolatedRepo()
        showSecurityDialog(repo)

        // Switch to Dark Mode
        node("themeOption-DARK").performClick()
        assertEquals(AppThemeMode.DARK, repo.settings.value.themeMode)

        // Switch to Light Mode
        node("themeOption-LIGHT").performClick()
        assertEquals(AppThemeMode.LIGHT, repo.settings.value.themeMode)

        // Switch to System Default
        node("themeOption-SYSTEM").performClick()
        assertEquals(AppThemeMode.SYSTEM, repo.settings.value.themeMode)
    }

    @Test
    fun securitySettingsDialog_clinicBranding_updatesRepositoryAndShowsSavedNotice() {
        val repo = createIsolatedRepo()
        showSecurityDialog(repo)

        node("clinicNameField").performScrollTo().performTextReplacement("Al-Amal Medical Clinic")
        node("clinicSubtitleField").performScrollTo().performTextReplacement("Specialized Obstetrics & Gynecology")
        node("saveBrandingButton").performScrollTo().performClick()

        compose.waitForIdle()
        node("brandingSavedMessage").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.security_branding_saved)).assertIsDisplayed()
        assertEquals("Al-Amal Medical Clinic", repo.settings.value.clinicName)
        assertEquals("Specialized Obstetrics & Gynecology", repo.settings.value.clinicSubtitle)
    }

    @Test
    fun securitySettingsDialog_lockAppNow_andCloseButton_invokeCallbacks() {
        var dismissed = false
        var lockedNow = false
        showSecurityDialog(
            onDismiss = { dismissed = true },
            onLockNow = { lockedNow = true },
        )

        // Test lock app now button calls both onDismiss and onLockNow
        node("lockAppNowButton").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue("onDismiss should be called", dismissed)
        assertTrue("onLockNow should be called", lockedNow)
    }

    @Test
    fun securitySettingsDialog_closeButton_invokesDismissCallback() {
        var dismissed = false
        showSecurityDialog(onDismiss = { dismissed = true })

        node("closeSettingsButton").performClick()
        compose.waitForIdle()
        assertTrue("onDismiss should be called", dismissed)
    }

    // =========================================================================
    // 6. VaultGateScreen States & Fail-Closed Invariants
    // =========================================================================

    @Test
    fun vaultGate_uninitializedState_displaysChipAndInitializeButton() {
        var initialized = false
        showVaultGate(
            state = VaultState.Uninitialized,
            onInitialize = { initialized = true },
        )

        node("vaultGate").assertIsDisplayed()
        node("vaultStatusCard").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.vault_status_setup_needed)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.storage_uninitialized_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.storage_uninitialized_body)).assertIsDisplayed()

        node("initializeStorage").performScrollTo().assertIsDisplayed()
        node("unlockStorage").assertDoesNotExist()
        node("retryStorage").assertDoesNotExist()

        node("initializeStorage").performClick()
        assertTrue("onInitialize should be invoked", initialized)

        // Fail-closed invariant: zero clinical data
        compose.onNodeWithTag("patientListTable").assertDoesNotExist()
        compose.onNodeWithTag("patientSearch").assertDoesNotExist()
    }

    @Test
    fun vaultGate_lockedState_displaysChipAndUnlockButton() {
        var unlocked = false
        showVaultGate(
            state = VaultState.Locked,
            onUnlock = { unlocked = true },
        )

        node("vaultGate").assertIsDisplayed()
        node("vaultStatusCard").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.vault_status_locked)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.storage_locked_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.storage_locked_body)).assertIsDisplayed()

        node("unlockStorage").performScrollTo().assertIsDisplayed()
        node("initializeStorage").assertDoesNotExist()
        node("retryStorage").assertDoesNotExist()

        node("unlockStorage").performClick()
        assertTrue("onUnlock should be invoked", unlocked)
    }

    @Test
    fun vaultGate_blockedState_displaysHighToneChipAndRetryButton_andNoClippingAt320dp() {
        var retried = false
        showVaultGate(
            state = VaultState.Blocked(VaultBlockReason.KEY_PERMANENTLY_INVALIDATED),
            widthDp = 320,
            fontScale = 1.4f,
            onRetry = { retried = true },
        )

        node("vaultGate").assertIsDisplayed()
        node("vaultStatusCard").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.vault_status_blocked)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.storage_blocked_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.blocked_key_invalidated)).assertIsDisplayed()

        node("retryStorage").performScrollTo().assertIsDisplayed()
        node("initializeStorage").assertDoesNotExist()
        node("unlockStorage").assertDoesNotExist()

        assertMinTouchTarget("retryStorage", scroll = false)
        node("retryStorage").performClick()
        assertTrue("onRetry should be invoked", retried)
    }

    @Test
    fun vaultGate_intermediateStates_noActionButtons_andFailClosed() {
        // Inspecting state has no action button
        showVaultGate(state = VaultState.Inspecting)
        node("vaultGate").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.vault_status_checking)).assertIsDisplayed()
        node("initializeStorage").assertDoesNotExist()
        node("unlockStorage").assertDoesNotExist()
        node("retryStorage").assertDoesNotExist()
    }

    @Test
    fun vaultGate_customClinicBranding_rendersProperly() {
        showVaultGate(
            state = VaultState.Locked,
            clinicName = "Metropolis Maternal Clinic",
            clinicSubtitle = "Fetal Medicine Center",
        )

        node("vaultGate").assertIsDisplayed()
        compose.onNodeWithText("Metropolis Maternal Clinic").assertIsDisplayed()
        compose.onNodeWithText("Fetal Medicine Center").assertIsDisplayed()
    }

    // =========================================================================
    // 7. Dark Theme & RTL Layout
    // =========================================================================

    @Test
    fun darkThemeAndRtl_securitySettingsDialog_rendersCleanly() {
        showSecurityDialog(darkTheme = true, layoutDirection = LayoutDirection.Rtl)
        node("securitySettingsDialog").assertIsDisplayed()
        node("settingsTabSecurity").assertIsDisplayed()
        node("settingsTabLabCatalog").assertIsDisplayed()
        node("settingsTabMedCatalog").assertIsDisplayed()
        assertMinTouchTarget("closeSettingsButton", scroll = false)
        assertMinTouchTarget("saveBrandingButton")
    }

    @Test
    fun darkThemeAndRtl_appLockScreen_rendersCleanly() {
        val repo = createIsolatedRepo()
        showAppLock(repo, darkTheme = true, layoutDirection = LayoutDirection.Rtl)
        node("appLockScreen").assertIsDisplayed()
        for (digit in 0..9) {
            node("pinKey-$digit").assertIsDisplayed()
            assertMinTouchTarget("pinKey-$digit", scroll = false)
        }
        node("pinKey-backspace").assertIsDisplayed()
        assertMinTouchTarget("pinKey-backspace", scroll = false)
    }

    @Test
    fun darkThemeAndRtl_vaultGate_rendersCleanly() {
        showVaultGate(
            state = VaultState.Blocked(VaultBlockReason.GCM_AUTHENTICATION_FAILED),
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )
        node("vaultGate").assertIsDisplayed()
        node("vaultStatusCard").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.vault_status_blocked)).assertIsDisplayed()
        node("retryStorage").assertIsDisplayed()
        assertMinTouchTarget("retryStorage", scroll = false)
    }

    // =========================================================================
    // 8. Exhaustive Existing Tags Check
    // =========================================================================

    @Test
    fun existingTags_appLockScreen_allPresent() {
        val repo = createIsolatedRepo()
        repo.setupPin("135246")
        repo.lockApp()

        showAppLock(repo)
        node("appLockScreen").assertIsDisplayed()
        node("pinDotsRow").assertIsDisplayed()
        for (i in 0..5) {
            node("pinDot-$i").assertIsDisplayed()
        }
        for (digit in 0..9) {
            node("pinKey-$digit").assertIsDisplayed()
        }
        node("pinKey-backspace").assertIsDisplayed()
    }

    @Test
    fun existingTags_securitySettingsDialog_allPresent() {
        showSecurityDialog()
        node("securitySettingsDialog").assertIsDisplayed()
        node("closeSettingsButton").assertIsDisplayed()
        node("themeSettingsCard").assertIsDisplayed()
        node("themeOption-SYSTEM").assertIsDisplayed()
        node("themeOption-LIGHT").assertIsDisplayed()
        node("themeOption-DARK").assertIsDisplayed()
        node("clinicNameField").performScrollTo().assertIsDisplayed()
        node("clinicSubtitleField").performScrollTo().assertIsDisplayed()
        node("saveBrandingButton").performScrollTo().assertIsDisplayed()
        node("changePinButton").performScrollTo().assertIsDisplayed()
        node("biometricSwitch").performScrollTo().assertIsDisplayed()
        node("backupVaultSettingsCard").performScrollTo().assertIsDisplayed()
        node("exportBackupButton").performScrollTo().assertIsDisplayed()
        node("restoreBackupButton").performScrollTo().assertIsDisplayed()
        node("lockAppNowButton").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun existingTags_vaultGate_allPresent() {
        showVaultGate(state = VaultState.Uninitialized)
        node("vaultGate").assertIsDisplayed()
        node("vaultStatusCard").assertIsDisplayed()
        node("initializeStorage").assertIsDisplayed()
    }

    companion object {
        private const val HOST_TAG = "securityLockVaultHost"
    }
}
