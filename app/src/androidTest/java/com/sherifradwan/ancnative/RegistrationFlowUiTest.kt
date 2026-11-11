package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DatingDraft
import com.sherifradwan.ancnative.state.DatingDraftValues
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.HistoriesDraft
import com.sherifradwan.ancnative.state.HistoriesDraftValues
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.PatientDraftValues
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.StepStatus
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistrationFlowUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient: DemoPatient = DemoFixtures.patients.first()
    private val events = mutableListOf<AncEvent>()
    private var density = 1f

    private fun flowState(
        step: RegistrationStep,
        statuses: Map<RegistrationStep, StepStatus> = emptyMap(),
        inFlow: Boolean = true,
        dialog: Boolean = false,
        route: AncRoute? = null,
    ): AncState {
        val session = "session-1"
        val id = patient.id
        val flow = RegistrationFlow(
            patientId = id,
            current = step,
            statuses = RegistrationStep.entries.associateWith { statuses[it] ?: StepStatus.PENDING },
            pregnancyTarget = if (step == RegistrationStep.PREGNANCIES) 3 else null,
            pregnancyIndex = 1,
            activeEditorSessionId = session,
        )
        val base = AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            registrationFlow = if (inFlow) flow else null,
            showDiscardDialog = dialog,
        )
        val patientValues = PatientDraftValues(displayName = patient.displayName, ageText = "28")
        return when (step) {
            RegistrationStep.IDENTITY, RegistrationStep.OBSTETRIC -> base.copy(
                route = AncRoute.PatientEditor(id, isNew = false),
                patientDraft = PatientDraft(session, id, patient.revision, null, patientValues, patientValues),
            )
            RegistrationStep.DATING -> base.copy(
                route = AncRoute.DatingEditor(id),
                datingDraft = DatingDraft(session, id, patient.revision, DatingDraftValues(), DatingDraftValues()),
            )
            RegistrationStep.HISTORIES -> base.copy(
                route = AncRoute.OtherHistoriesEditor(id),
                historiesDraft = HistoriesDraft(
                    session, id, patient.revision, HistoriesDraftValues(), HistoriesDraftValues(),
                ),
            )
            RegistrationStep.PREGNANCIES -> base.copy(
                route = AncRoute.PreviousPregnancyEditor(id, "pregnancy-new"),
                pregnancyDraft = PreviousPregnancyDraft(
                    session, id, "pregnancy-new", patient.revision, null, null,
                    PreviousPregnancyDraftValues(), PreviousPregnancyDraftValues(),
                ),
            )
        }.let { if (route != null) it.copy(route = route) else it }
    }

    private fun show(state: AncState, fontScale: Float = 1f, darkTheme: Boolean = false) {
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            density = base
            compose.activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(base, fontScale)) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                        AncTheme(darkTheme = darkTheme) {
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
            }
        }
        compose.waitForIdle()
    }

    private fun str(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun hostRect(): Rect {
        val host = compose.onNodeWithTag(HOST_TAG).fetchSemanticsNode()
        val origin = host.positionInRoot
        return Rect(origin.x, origin.y, origin.x + host.size.width, origin.y + host.size.height)
    }

    private fun assertButtonUsable(tag: String, label: String) {
        val node = compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        assertTouchTargetInsideHost(label, node.fetchSemanticsNode(), hostRect(), density, 48f, 48f)
    }

    // RF11
    @Test
    fun obstetricStepHeaderIsNotClippedAndNextNeedsNoScrolling() {
        listOf(1f to false, 1.4f to false, 1f to true).forEach { (scale, dark) ->
            show(flowState(RegistrationStep.OBSTETRIC), scale, dark)
            val title = str(R.string.obstetric_risk_section_title)
            compose.onNodeWithText("Step 3 of 5: $title", substring = true).assertIsDisplayed()
            compose.assertTextNotClipped(
                "header title at $scale dark=$dark",
                hasText(title, substring = true) and inTagged("registrationStepHeader"),
            )
            compose.onNodeWithTag("flowNext").assertIsDisplayed()
        }
    }

    // RF11 keyboard variant
    @Test
    fun segmentRowHidesWhileTheKeyboardShowsButTitleAndNextStay() {
        show(flowState(RegistrationStep.OBSTETRIC))
        compose.onNodeWithTag("registrationSegment-1").assertIsDisplayed()
        compose.onNodeWithTag("patientGravida").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagCount("registrationSegment-1") == 0 }
        compose.onNodeWithTag("registrationStepHeader").assertIsDisplayed()
        compose.onNodeWithTag("flowNext").assertIsDisplayed()
    }

    // RF12
    @Test
    fun footerButtonsStayUsableOnMiddleAndLastSteps() {
        listOf(RegistrationStep.DATING, RegistrationStep.PREGNANCIES).forEach { step ->
            listOf(1f, 1.4f, 2f).forEach { scale ->
                show(flowState(step), scale)
                listOf("flowBack", "flowSkip", "flowNext").forEach { tag ->
                    assertButtonUsable(tag, "$step $tag at $scale")
                }
            }
        }
    }

    @Test
    fun identityStepDisablesBackAndOffersNoSkip() {
        show(flowState(RegistrationStep.IDENTITY))
        compose.onNodeWithTag("flowBack").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("flowSkip").assertDoesNotExist()
        compose.onNodeWithTag("flowNext").assertIsDisplayed().assertIsEnabled()
    }

    // RF13
    @Test
    fun segmentsShowStatusWordsAndSpokenDescriptions() {
        val statuses = mapOf(
            RegistrationStep.IDENTITY to StepStatus.DONE,
            RegistrationStep.DATING to StepStatus.SKIPPED,
        )
        val identity = str(R.string.registration_step_identity)
        val dating = str(R.string.dating)
        listOf(1f, 1.3f).forEach { scale ->
            show(flowState(RegistrationStep.DATING, statuses), scale)
            compose.onNodeWithTag("registrationSegment-1").assertTextContains(str(R.string.done))
                .assertContentDescriptionEquals("$identity, ${str(R.string.done)}")
            compose.onNodeWithTag("registrationSegment-2")
                .assertTextContains(str(R.string.registration_status_skipped))
                .assertTextContains(str(R.string.registration_current))
                .assertContentDescriptionEquals(
                    str(R.string.registration_segment_current_cd, dating, str(R.string.registration_status_skipped)),
                )
            compose.onNodeWithTag("registrationSegment-3")
                .assertTextContains(str(R.string.registration_status_pending))
        }
        show(flowState(RegistrationStep.DATING, statuses), 1.4f)
        compose.onNodeWithTag("registrationSegment-1").assertIsDisplayed()
            .assertContentDescriptionEquals("$identity, ${str(R.string.done)}")
        compose.onNode(hasText(str(R.string.done)) and inTagged("registrationStepHeader")).assertDoesNotExist()
        compose.onNode(hasText(str(R.string.registration_current)) and inTagged("registrationStepHeader"))
            .assertDoesNotExist()
        val spoken = compose.onNodeWithTag("registrationSegment-2").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        assertTrue(spoken, spoken.endsWith(", current"))
    }

    // Wiring
    @Test
    fun nextOnEveryEditorStepSendsRegistrationNextAndNoSubmit() {
        RegistrationStep.entries.forEach { step ->
            show(flowState(step))
            compose.onNodeWithTag("flowNext").performClick()
            assertEquals("$step", 1, events.size)
            assertTrue("$step ${events.single()}", events.single() is AncEvent.RegistrationNext)
        }
    }

    @Test
    fun skipSendsFlowSkipAndNothingElse() {
        show(flowState(RegistrationStep.DATING))
        compose.onNodeWithTag("flowSkip").performClick()
        val event = events.single() as AncEvent.Flow
        assertEquals(FlowEvent.Skip, event.event)
    }

    @Test
    fun backButtonSendsFlowBack() {
        show(flowState(RegistrationStep.DATING))
        compose.onNodeWithTag("flowBack").performClick()
        assertEquals(FlowEvent.Back, (events.single() as AncEvent.Flow).event)
    }

    @Test
    fun saveButtonsAreAbsentInTheFlowAndPresentForHubStartedEditors() {
        val tags = mapOf(
            RegistrationStep.IDENTITY to "savePatient",
            RegistrationStep.DATING to "saveDating",
            RegistrationStep.HISTORIES to "saveHistories",
            RegistrationStep.PREGNANCIES to "savePregnancy",
        )
        tags.forEach { (step, tag) ->
            show(flowState(step))
            compose.onNodeWithTag(tag).assertDoesNotExist()
            compose.onNodeWithTag("flowNext").assertExists()
            show(flowState(step, inFlow = false))
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
            compose.onNodeWithTag("flowNext").assertDoesNotExist()
        }
    }

    @Test
    fun systemBackFollowsDialogThenFlowThenPlainBack() {
        show(flowState(RegistrationStep.DATING, dialog = true))
        pressSystemBack()
        assertEquals(AncEvent.KeepEditing, events.single())

        show(flowState(RegistrationStep.DATING))
        pressSystemBack()
        assertEquals(FlowEvent.Back, (events.single() as AncEvent.Flow).event)

        show(flowState(RegistrationStep.DATING, route = AncRoute.Workspace(patient.id)))
        pressSystemBack()
        assertEquals(AncEvent.Back, events.single())
    }

    private fun pressSystemBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagCount(tag: String): Int =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    private companion object {
        const val HOST_TAG = "registrationFlowHost"
    }
}
