package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.security.AutoLockTimeout
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityWorkflowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLockScreenValidatesSixDigitPinAndUnlocksWithDefaultPin() {
        val activity = compose.activity
        val securityRepo = SecuritySettingsRepository.getInstance(activity)
        securityRepo.updatePin("123456")
        securityRepo.lockApp()

        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1001L),
            route = AncRoute.PatientList,
        )

        activity.runOnUiThread {
            activity.setContent {
                AncTheme {
                    AncApp(
                        state = state,
                        onEvent = {},
                        onStartVisit = { _, _ -> },
                        onSaveVisit = {},
                        onInitialize = {},
                        onUnlock = {},
                        onLock = { securityRepo.lockApp() },
                        onRetryStorage = {},
                        securityRepo = securityRepo,
                    )
                }
            }
        }

        // 1. Verify AppLockScreen is displayed
        compose.onNodeWithTag("appLockScreen").assertIsDisplayed()
        compose.onNodeWithText("دكتور شريف رضوان كلينك").assertIsDisplayed()

        // 2. Type incorrect PIN: 999999
        repeat(6) {
            compose.onNodeWithTag("pinKey-9").performClick()
        }
        // Verify error message is displayed
        compose.onNodeWithText("رمز المرور غير صحيح. يرجى المحاولة مرة أخرى.").assertIsDisplayed()

        // 3. Type correct PIN: 123456
        listOf("1", "2", "3", "4", "5", "6").forEach { digit ->
            compose.onNodeWithTag("pinKey-$digit").performClick()
        }

        // 4. Verify unlocked and patient list screen is visible
        compose.waitForIdle()
        assertFalse(securityRepo.settings.value.isAppLocked)
        compose.onNodeWithTag("appLockScreen").assertDoesNotExist()
        compose.onNodeWithTag("patientListTable").assertIsDisplayed()
    }

    @Test
    fun securitySettingsDialogAllowsAutoLockTimeoutConfigurationAndManualLock() {
        val activity = compose.activity
        val securityRepo = SecuritySettingsRepository.getInstance(activity)
        securityRepo.unlockApp()

        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1002L),
            route = AncRoute.PatientList,
        )

        activity.runOnUiThread {
            activity.setContent {
                AncTheme {
                    AncApp(
                        state = state,
                        onEvent = {},
                        onStartVisit = { _, _ -> },
                        onSaveVisit = {},
                        onInitialize = {},
                        onUnlock = {},
                        onLock = { securityRepo.lockApp() },
                        onRetryStorage = {},
                        securityRepo = securityRepo,
                    )
                }
            }
        }

        // 1. Open Security Settings dialog via Top Bar
        compose.onNodeWithTag("openSecuritySettings").performClick()
        compose.onNodeWithTag("securitySettingsDialog").assertIsDisplayed()
        compose.onNodeWithText("إعدادات الأمان وقفل التطبيق").assertIsDisplayed()

        // 2. Select Auto-Lock Timeout: "لا يقفل أبداً (إيقاف القفل)" (NEVER)
        compose.onNodeWithTag("timeoutOption-${AutoLockTimeout.NEVER.name}")
            .performScrollTo()
            .performClick()

        // Verify repository updated
        assertEquals(AutoLockTimeout.NEVER, securityRepo.settings.value.autoLockTimeout)

        // 3. Test changing to 1 minute
        compose.onNodeWithTag("timeoutOption-${AutoLockTimeout.ONE_MINUTE.name}")
            .performScrollTo()
            .performClick()
        assertEquals(AutoLockTimeout.ONE_MINUTE, securityRepo.settings.value.autoLockTimeout)

        // 4. Click Lock App Now
        compose.onNodeWithTag("lockAppNowButton")
            .performScrollTo()
            .performClick()

        compose.waitForIdle()
        assertTrue(securityRepo.settings.value.isAppLocked)
        compose.onNodeWithTag("appLockScreen").assertIsDisplayed()
    }
}
