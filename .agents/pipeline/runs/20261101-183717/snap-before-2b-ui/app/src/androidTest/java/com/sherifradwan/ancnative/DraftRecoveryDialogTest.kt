package com.sherifradwan.ancnative

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftRecoveryPrompt
import com.sherifradwan.ancnative.state.DraftSlot
import com.sherifradwan.ancnative.state.LabDraft
import com.sherifradwan.ancnative.state.LabDraftValues
import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftRecoveryDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private val events = mutableListOf<AncEvent>()

    private fun promptState(isStale: Boolean): AncState {
        val labId = "lab-recovery"
        val record = DraftRecord(
            patientId = patient.id,
            screenKind = DraftScreenKind.LAB,
            editorKind = DraftEditorKind.CREATE,
            recordId = labId,
            baseRevision = null,
            fields = mapOf("result" to "11"),
            updatedAtMillis = System.currentTimeMillis() - 3 * 60 * 60 * 1000L,
        )
        return AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 701L),
            route = AncRoute.LabEditor(patient.id, labId, isNew = true),
            labDraft = LabDraft("recovery-session", patient.id, labId, patient.revision, null, LabDraftValues(), LabDraftValues()),
            draftRecoveryPrompt = DraftRecoveryPrompt(
                slot = DraftSlot(patient.id, DraftScreenKind.LAB),
                editorSessionId = "recovery-session",
                record = record,
                isStale = isStale,
            ),
        )
    }

    private fun mount(state: AncState) {
        events.clear()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    AncApp(
                        state = state,
                        onEvent = { events += it },
                        onStartVisit = { _, _ -> },
                        onSaveVisit = {},
                        onInitialize = {},
                        onUnlock = {},
                        onLock = {},
                        onRetryStorage = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = compose.activity.getString(id)

    @Test
    fun alertShowsTitleScreenNameAgeAndBothButtons() {
        mount(promptState(isStale = false))
        compose.onNodeWithText(string(R.string.draft_recovery_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.draft_screen_lab), substring = true).assertIsDisplayed()
        compose.onNodeWithText("3 hours ago", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("draftContinue").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.draft_recovery_continue)).assertIsDisplayed()
        compose.onNodeWithTag("draftDiscard").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.draft_recovery_discard)).assertIsDisplayed()
    }

    @Test
    fun staleWarningAppearsOnlyWhenTheRecordChanged() {
        mount(promptState(isStale = false))
        compose.onNodeWithText(string(R.string.draft_recovery_stale), substring = true).assertDoesNotExist()
        mount(promptState(isStale = true))
        compose.onNodeWithText(string(R.string.draft_recovery_stale), substring = true).assertIsDisplayed()
    }

    @Test
    fun noAlertWithoutAPrompt() {
        mount(promptState(isStale = false).copy(draftRecoveryPrompt = null))
        compose.onNodeWithTag("draftContinue").assertDoesNotExist()
        compose.onNodeWithTag("draftDiscard").assertDoesNotExist()
    }

    @Test
    fun backPressDoesNotDismissTheAlert() {
        mount(promptState(isStale = false))
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("draftContinue").assertIsDisplayed()
        compose.onNodeWithTag("draftDiscard").assertIsDisplayed()
        assertTrue(events.none { it == AncEvent.AcceptDraftRecovery || it == AncEvent.DiscardDraftRecovery })
    }

    @Test
    fun continueButtonDispatchesAccept() {
        mount(promptState(isStale = false))
        compose.onNodeWithTag("draftContinue").performClick()
        compose.waitForIdle()
        assertEquals(listOf<AncEvent>(AncEvent.AcceptDraftRecovery), events.toList())
    }

    @Test
    fun discardButtonDispatchesDiscard() {
        mount(promptState(isStale = false))
        compose.onNodeWithTag("draftDiscard").performClick()
        compose.waitForIdle()
        assertEquals(listOf<AncEvent>(AncEvent.DiscardDraftRecovery), events.toList())
    }
}
