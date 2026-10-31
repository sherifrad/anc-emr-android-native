package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
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
        securityRepo.updatePin("135246")
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
        compose.onNodeWithText("Dr. Sherif Radwan Clinic").assertIsDisplayed()

        // 2. Type incorrect PIN: 999999
        repeat(6) {
            compose.onNodeWithTag("pinKey-9").performClick()
        }
        // Verify error message is displayed
        compose.onNodeWithText("Incorrect PIN. Please try again.").assertIsDisplayed()

        // 3. Type correct PIN: 135246
        listOf("1", "3", "5", "2", "4", "6").forEach { digit ->
            compose.onNodeWithTag("pinKey-$digit").performClick()
        }

        // 4. Verify unlocked and patient list screen is visible
        compose.waitForIdle()
        assertFalse(securityRepo.settings.value.isAppLocked)
        compose.onNodeWithTag("appLockScreen").assertDoesNotExist()
        compose.onNodeWithTag("patientListTable").assertIsDisplayed()
    }

    @Test
    fun securitySettingsDialogDisplaysFixedAutoLockTimeoutAndAllowsManualLock() {
        val activity = compose.activity
        val securityRepo = SecuritySettingsRepository.getInstance(activity)
        securityRepo.updatePin("135246")
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
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Clinic & System Settings").fetchSemanticsNodes().isNotEmpty()
        }
        // The dialog animates in: wait until the title is really on screen, not just in the tree.
        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching { compose.onNodeWithText("Clinic & System Settings").assertIsDisplayed() }.isSuccess
        }
        compose.onNodeWithTag("securitySettingsDialog").assertIsDisplayed()
        compose.onNodeWithText("Clinic & System Settings").assertIsDisplayed()

        // 2. Verify fixed Auto-Lock Timeout is displayed (D6: fixed at 10 minutes)
        compose.onNodeWithText("Auto-Lock Timeout").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Locks after 10 minutes away.").performScrollTo().assertIsDisplayed()

        // 3. Click Lock App Now
        compose.onNodeWithTag("lockAppNowButton")
            .performScrollTo()
            .performClick()

        compose.waitForIdle()
        assertTrue(securityRepo.settings.value.isAppLocked)
        compose.onNodeWithTag("appLockScreen").assertIsDisplayed()
    }
}
