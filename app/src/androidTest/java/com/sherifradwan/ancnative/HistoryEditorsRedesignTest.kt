package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DatingDraft
import com.sherifradwan.ancnative.state.DatingDraftField
import com.sherifradwan.ancnative.state.DatingDraftValues
import com.sherifradwan.ancnative.state.HistoriesDraft
import com.sherifradwan.ancnative.state.HistoriesDraftField
import com.sherifradwan.ancnative.state.HistoriesDraftValues
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.StepStatus
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 13: OtherHistoriesEditor, PreviousPregnancyEditor, DatingEditor.
 * Mounts through MainActivity + AncApp + AncTheme inside a 360dp x 640dp host,
 * matching RegistrationRedesignTest style (single setContent per test,
 * shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides,
 * useUnmergedTree where label text is cleared, performScrollTo before assertIsDisplayed,
 * >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class HistoryEditorsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient: DemoPatient = DemoFixtures.patients.first()
    private val events = mutableListOf<AncEvent>()
    private var state by mutableStateOf(AncState(patients = DemoFixtures.patients))

    private var primaryColor = Color.Unspecified
    private var onSurfaceColor = Color.Unspecified
    private var onSurfaceVariantColor = Color.Unspecified
    private var errorColor = Color.Unspecified

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun onEvent(event: AncEvent) {
        events += event
        state = AncReducer.reduce(state, event)
    }

    private fun historiesState(
        inFlow: Boolean = false,
        values: HistoriesDraftValues = HistoriesDraftValues(),
        errors: Set<PatientValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
    ): AncState {
        val session = "session-histories-s13"
        val id = patient.id
        val flow = RegistrationFlow(
            patientId = id,
            current = RegistrationStep.HISTORIES,
            statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
            activeEditorSessionId = session,
        )
        return AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.OtherHistoriesEditor(id),
            registrationFlow = if (inFlow) flow else null,
            historiesDraft = HistoriesDraft(
                editorSessionId = session,
                patientId = id,
                baseRevision = patient.revision,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    private fun pregnancyState(
        inFlow: Boolean = false,
        values: PreviousPregnancyDraftValues = PreviousPregnancyDraftValues(),
        isEdit: Boolean = false,
        errors: Set<PatientValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
    ): AncState {
        val session = "session-pregnancy-s13"
        val id = patient.id
        val pregnancyId = "pregnancy-s13-1"
        val flow = RegistrationFlow(
            patientId = id,
            current = RegistrationStep.PREGNANCIES,
            statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
            activeEditorSessionId = session,
        )
        return AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.PreviousPregnancyEditor(id, pregnancyId),
            registrationFlow = if (inFlow) flow else null,
            pregnancyDraft = PreviousPregnancyDraft(
                editorSessionId = session,
                patientId = id,
                pregnancyId = pregnancyId,
                basePatientRevision = patient.revision,
                basePregnancyRevision = if (isEdit) 1L else null,
                recordedAt = if (isEdit) "2026-01-01" else null,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    private fun datingState(
        inFlow: Boolean = false,
        values: DatingDraftValues = DatingDraftValues(),
        errors: Set<ClinicalValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
    ): AncState {
        val session = "session-dating-s13"
        val id = patient.id
        val flow = RegistrationFlow(
            patientId = id,
            current = RegistrationStep.DATING,
            statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
            activeEditorSessionId = session,
        )
        return AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.DatingEditor(id),
            registrationFlow = if (inFlow) flow else null,
            datingDraft = DatingDraft(
                editorSessionId = session,
                patientId = id,
                basePatientRevision = patient.revision,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    /** One setContent per test: hosts the app in a 360dp x 640dp container. */
    private fun show(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    primaryColor = MaterialTheme.colorScheme.primary
                    onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    errorColor = MaterialTheme.colorScheme.error
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            AncApp(
                                state = state,
                                onEvent = ::onEvent,
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

    // ---- measuring and query helpers ----

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()
    private fun bounds(matcher: SemanticsMatcher): DpRect =
        compose.onNode(matcher, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertMinTouchTarget(tag: String) {
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
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

    // =========================================================================
    // 1. OtherHistoriesEditor
    // =========================================================================

    @Test
    fun otherHistoriesEditor_noNumberedTitles_noLockFooter_cleanHeader() {
        show(historiesState())

        node("otherHistoriesEditor").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName, substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Card headings have clean localized names without "1. ", "2. ", "3. ", "4. "
        compose.onNodeWithText("1. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("2. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("3. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("4. ", substring = true).assertDoesNotExist()

        // Padlock lock footer text removed
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Tamper-Evident", substring = true).assertDoesNotExist()

        // 4 section headings
        compose.onNodeWithText(str(R.string.medical_history)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.surgical_history)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.family_history)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.allergy_history)).performScrollTo().assertIsDisplayed()

        // Preset chip title is "Quick shortcuts" (common_presets)
        compose.onAllNodes(hasText(str(R.string.common_presets)), useUnmergedTree = true)
            .fetchSemanticsNodes().run { assertTrue(isNotEmpty()) }

        // Existing fields still present
        node("medicalHistory").performScrollTo().assertIsDisplayed()
        node("surgicalHistory").performScrollTo().assertIsDisplayed()
        node("familyHistory").performScrollTo().assertIsDisplayed()
        node("allergyHistory").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun otherHistoriesEditor_negativeHistoriesCard_noCheckPrefix_fillAllNegative_populatesFields() {
        show(historiesState())

        node("negativeHistoriesCard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.negative_histories_title)).performScrollTo().assertIsDisplayed()

        // Button text has no "✓ " prefix
        val fillLabel = str(R.string.fill_all_negative)
        node("fillAllNegativeHistories")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fillLabel)) or hasText(fillLabel, substring = true))

        compose.onNodeWithText("✓", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("✓", substring = true).assertDoesNotExist()

        // Clicking fillAllNegative populates all four fields
        node("fillAllNegativeHistories").performClick()
        compose.waitForIdle()

        val draft = state.historiesDraft!!.values
        assertEquals(str(R.string.no_medical_history), draft.medicalHistory)
        assertEquals(str(R.string.no_surgical_history), draft.surgicalHistory)
        assertEquals(str(R.string.no_family_history), draft.familyHistory)
        assertEquals(str(R.string.no_allergy_history), draft.allergyHistory)

        assertTrue(events.contains(AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, str(R.string.no_medical_history))))
        assertTrue(events.contains(AncEvent.HistoriesDraftChanged(HistoriesDraftField.SURGICAL, str(R.string.no_surgical_history))))
        assertTrue(events.contains(AncEvent.HistoriesDraftChanged(HistoriesDraftField.FAMILY, str(R.string.no_family_history))))
        assertTrue(events.contains(AncEvent.HistoriesDraftChanged(HistoriesDraftField.ALLERGY, str(R.string.no_allergy_history))))
    }

    @Test
    fun otherHistoriesEditor_presetChips_clickingPopulatesField() {
        show(historiesState())

        node("medHistoryPreset-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Chronic hypertension", state.historiesDraft!!.values.medicalHistory)

        node("surgHistoryPreset-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Appendectomy", state.historiesDraft!!.values.surgicalHistory)

        node("famHistoryPreset-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Family history of Diabetes", state.historiesDraft!!.values.familyHistory)

        node("allergyHistoryPreset-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Penicillin allergy", state.historiesDraft!!.values.allergyHistory)
    }

    @Test
    fun otherHistoriesEditor_touchTargets_atLeast48dp() {
        show(historiesState())

        assertMinTouchTarget("fillAllNegativeHistories")
        assertMinTouchTarget("medHistoryPreset-0")
        assertMinTouchTarget("medHistoryPreset-1")
        assertMinTouchTarget("surgHistoryPreset-0")
        assertMinTouchTarget("surgHistoryPreset-1")
        assertMinTouchTarget("famHistoryPreset-0")
        assertMinTouchTarget("famHistoryPreset-1")
        assertMinTouchTarget("allergyHistoryPreset-0")
        assertMinTouchTarget("allergyHistoryPreset-1")
        assertMinTouchTarget("cancelHistories")
        assertMinTouchTarget("saveHistories")
    }

    @Test
    fun otherHistoriesEditor_standalone_cancelAndSave_emitEvents() {
        show(historiesState())

        node("saveHistories").performScrollTo().performClick()
        assertTrue("SubmitOtherHistories emitted", events.contains(AncEvent.SubmitOtherHistories))

        events.clear()
        node("cancelHistories").performScrollTo().performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun otherHistoriesEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel() {
        show(historiesState(savePhase = SavePhase.PENDING))

        node("saveHistories").assertIsNotEnabled()
        node("cancelHistories").assertIsNotEnabled()
        node("historiesSaveSpinner").assertExists()
        compose.onNodeWithText(str(R.string.saving_label), useUnmergedTree = true).assertExists()
    }

    @Test
    fun otherHistoriesEditor_noTextBelow12sp_fontScale1_0() {
        show(historiesState(), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun otherHistoriesEditor_noTextBelow12sp_fontScale1_3() {
        show(historiesState(), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun otherHistoriesEditor_360dp_fontScale2_0_noClipping() {
        show(historiesState(), fontScale = 2.0f)
        compose.assertAllTextWhole("negativeHistoriesCard")
        node("medHistoryPreset-0").performScrollTo().assertIsDisplayed()
        node("saveHistories").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun otherHistoriesEditor_inFlow_hidesStandaloneCancelSave() {
        show(historiesState(inFlow = true))

        node("cancelHistories").assertDoesNotExist()
        node("saveHistories").assertDoesNotExist()
    }

    // =========================================================================
    // 2. PreviousPregnancyEditor
    // =========================================================================

    @Test
    fun previousPregnancyEditor_noNumberedTitles_noLockFooter_cleanCards() {
        show(pregnancyState())

        node("previousPregnancyEditor").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName, substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Clean titles without "1. " and "2. "
        compose.onNodeWithText("1. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("2. ", substring = true).assertDoesNotExist()

        // Padlock lock footer text removed
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Tamper-Evident", substring = true).assertDoesNotExist()

        // Section cards
        compose.onNodeWithText(str(R.string.pregnancy_core_facts_title)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.pregnancy_details_title)).performScrollTo().assertIsDisplayed()

        // Existing tags preserved
        node("pregnancyDateText").performScrollTo().assertIsDisplayed()
        node("pregnancyDateTextCalendarButton").performScrollTo().assertIsDisplayed()
        node("pregnancyOutcome-0").performScrollTo().assertIsDisplayed()
        node("pregnancyPlaceDoctor").performScrollTo().assertIsDisplayed()
        node("pregnancyType-0").performScrollTo().assertIsDisplayed()
        node("pregnancyOtherDetails").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun previousPregnancyEditor_outcomeChips_humanLabels_dispatchCanonicalCodes() {
        show(pregnancyState())

        // Chips show human labels selected by testTag
        val outcomeChips = listOf(
            "pregnancyOutcome-0" to str(R.string.unknown),
            "pregnancyOutcome-1" to str(R.string.outcome_live_birth),
            "pregnancyOutcome-2" to str(R.string.outcome_stillbirth),
            "pregnancyOutcome-3" to str(R.string.outcome_early_loss),
            "pregnancyOutcome-4" to str(R.string.outcome_termination),
        )
        for ((tag, label) in outcomeChips) {
            node(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assert(hasAnyDescendant(hasText(label)) or hasText(label))
        }

        // Click Live birth -> sets "LiveBirth"
        node("pregnancyOutcome-1").performClick()
        compose.waitForIdle()
        assertEquals("LiveBirth", state.pregnancyDraft!!.values.outcome)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "LiveBirth")))

        // Delivery mode choice group appears for LiveBirth: verify deliveryBy-0..2 chips
        val deliveryChips = listOf(
            "deliveryBy-0" to str(R.string.unknown),
            "deliveryBy-1" to str(R.string.delivery_mode_vaginal),
            "deliveryBy-2" to str(R.string.delivery_mode_cesarean),
        )
        for ((tag, label) in deliveryChips) {
            node(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assert(hasAnyDescendant(hasText(label)) or hasText(label))
        }

        // Click Stillbirth -> sets "Stillbirth"
        node("pregnancyOutcome-2").performClick()
        compose.waitForIdle()
        assertEquals("Stillbirth", state.pregnancyDraft!!.values.outcome)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "Stillbirth")))

        // Click Early pregnancy loss -> sets "EarlyPregnancyLoss"
        node("pregnancyOutcome-3").performClick()
        compose.waitForIdle()
        assertEquals("EarlyPregnancyLoss", state.pregnancyDraft!!.values.outcome)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss")))

        // Click Termination of pregnancy -> sets "TerminationOfPregnancy"
        node("pregnancyOutcome-4").performClick()
        compose.waitForIdle()
        assertEquals("TerminationOfPregnancy", state.pregnancyDraft!!.values.outcome)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "TerminationOfPregnancy")))
    }

    @Test
    fun previousPregnancyEditor_conditionalFields_deliveryMode_vaginalAndCesareanCodes() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "LiveBirth")))

        // Delivery mode choice group is displayed with human labels
        node("deliveryBy-0").performScrollTo().assertIsDisplayed()
        node("deliveryBy-1").performScrollTo().assertIsDisplayed() // Vaginal
        node("deliveryBy-2").performScrollTo().assertIsDisplayed() // Cesarean

        compose.onNodeWithText(str(R.string.delivery_mode_vaginal)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.delivery_mode_cesarean)).performScrollTo().assertIsDisplayed()

        // Loss management and termination methods are absent
        node("lossManagement-0").assertDoesNotExist()
        node("terminationMethod-0").assertDoesNotExist()

        // Click Cesarean -> sets canonical code "CS"
        node("deliveryBy-2").performClick()
        compose.waitForIdle()
        assertEquals("CS", state.pregnancyDraft!!.values.deliveryBy)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DELIVERY_BY, "CS")))

        // Click Vaginal -> sets canonical code "Vaginal"
        node("deliveryBy-1").performClick()
        compose.waitForIdle()
        assertEquals("Vaginal", state.pregnancyDraft!!.values.deliveryBy)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DELIVERY_BY, "Vaginal")))
    }

    @Test
    fun previousPregnancyEditor_conditionalFields_lossManagement_and_terminationMethod() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "EarlyPregnancyLoss")))

        // Delivery mode absent, loss management present
        node("deliveryBy-0").assertDoesNotExist()
        node("terminationMethod-0").assertDoesNotExist()
        node("lossManagement-0").performScrollTo().assertIsDisplayed()
        node("lossManagement-1").performScrollTo().assertIsDisplayed()

        node("lossManagement-1").performClick()
        compose.waitForIdle()
        assertEquals("Expectant", state.pregnancyDraft!!.values.lossManagement)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.LOSS_MANAGEMENT, "Expectant")))

        // Switch to TerminationOfPregnancy
        node("pregnancyOutcome-4").performClick()
        compose.waitForIdle()

        node("deliveryBy-0").assertDoesNotExist()
        node("lossManagement-0").assertDoesNotExist()
        node("terminationMethod-0").performScrollTo().assertIsDisplayed()
        node("terminationMethod-1").performScrollTo().assertIsDisplayed()

        node("terminationMethod-1").performClick()
        compose.waitForIdle()
        assertEquals("Medical", state.pregnancyDraft!!.values.terminationMethod)
        assertTrue(events.contains(AncEvent.PregnancyDraftChanged(PregnancyDraftField.TERMINATION_METHOD, "Medical")))
    }

    @Test
    fun previousPregnancyEditor_touchTargets_atLeast48dp() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "LiveBirth")))

        assertMinTouchTarget("pregnancyDateTextCalendarButton")
        assertMinTouchTarget("pregnancyOutcome-0")
        assertMinTouchTarget("pregnancyOutcome-1")
        assertMinTouchTarget("pregnancyOutcome-2")
        assertMinTouchTarget("pregnancyOutcome-3")
        assertMinTouchTarget("pregnancyOutcome-4")
        assertMinTouchTarget("deliveryBy-0")
        assertMinTouchTarget("deliveryBy-1")
        assertMinTouchTarget("deliveryBy-2")
        assertMinTouchTarget("pregnancyType-0")
        assertMinTouchTarget("pregnancyType-1")
        assertMinTouchTarget("cancelPregnancy")
        assertMinTouchTarget("savePregnancy")
    }

    @Test
    fun previousPregnancyEditor_standalone_cancelAndSave_emitEvents() {
        show(pregnancyState())

        node("savePregnancy").performScrollTo().performClick()
        assertTrue("SubmitPreviousPregnancy emitted", events.contains(AncEvent.SubmitPreviousPregnancy))

        events.clear()
        node("cancelPregnancy").performScrollTo().performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun previousPregnancyEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel() {
        show(pregnancyState(savePhase = SavePhase.PENDING))

        node("savePregnancy").assertIsNotEnabled()
        node("cancelPregnancy").assertIsNotEnabled()
        node("pregnancySaveSpinner").assertExists()
        compose.onNodeWithText(str(R.string.saving_label), useUnmergedTree = true).assertExists()
    }

    @Test
    fun previousPregnancyEditor_noTextBelow12sp_fontScale1_0() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "LiveBirth")), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun previousPregnancyEditor_noTextBelow12sp_fontScale1_3() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "LiveBirth")), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun previousPregnancyEditor_360dp_fontScale2_0_noClipping() {
        show(pregnancyState(values = PreviousPregnancyDraftValues(outcome = "TerminationOfPregnancy")), fontScale = 2.0f)
        node("pregnancyOutcome-4").performScrollTo().assertIsDisplayed()
        node("terminationMethod-1").performScrollTo().assertIsDisplayed()
        node("savePregnancy").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun previousPregnancyEditor_inFlow_hidesStandaloneCancelSave() {
        show(pregnancyState(inFlow = true))

        node("cancelPregnancy").assertDoesNotExist()
        node("savePregnancy").assertDoesNotExist()
    }

    // =========================================================================
    // 3. DatingEditor
    // =========================================================================

    @Test
    fun datingEditor_noNumberedTitles_noLockFooter_cleanSections() {
        show(datingState())

        node("datingEditor").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName, substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Clean section titles without "1. ", "2. ", "3. "
        compose.onNodeWithText("1. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("2. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("3. ", substring = true).assertDoesNotExist()

        // Padlock lock footer text removed
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Tamper-Evident", substring = true).assertDoesNotExist()

        // Baseline dating and Notes sections
        compose.onNodeWithText(str(R.string.dating_baseline_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.notes)).performScrollTo().assertIsDisplayed()

        // Existing tags preserved
        node("datingMethodDropdown").performScrollTo().assertIsDisplayed()
        node("agreedEdd").performScrollTo().assertIsDisplayed()
        node("agreedEddCalendarButton").performScrollTo().assertIsDisplayed()
        node("datingNotes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_calculatedCard_hiddenWhenEmpty_bothNull() {
        show(datingState(values = DatingDraftValues()))

        compose.onNodeWithText(str(R.string.dating_calculated_section)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.calculated_edd)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.current_gestational_age)).assertDoesNotExist()
    }

    @Test
    fun datingEditor_calculatedCard_shownWhenPresent_formatsDmyAndWeeksStyle() {
        // Target EDD: 27 weeks and 2 days (191 days) from LMP conception (280 - 191 = 89 days after today)
        val targetEdd = LocalDate.now().plusDays(280 - (27 * 7 + 2))
        val formattedDmy = formatDmy(targetEdd)!!

        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "Ultrasound",
                    ultrasoundEdd = targetEdd.toString(),
                    agreedEdd = targetEdd.toString(),
                ),
            ),
        )

        compose.onNodeWithText(str(R.string.dating_calculated_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.calculated_edd)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(formattedDmy).performScrollTo().assertIsDisplayed()

        compose.onNodeWithText(str(R.string.current_gestational_age)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("27+2 wks").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_calculatedCard_postTerm_showsPostBadge() {
        // EDD 15 days in the past -> Gestational Age >= 42 weeks (post-term)
        val postTermEdd = LocalDate.now().minusDays(15)

        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "Ultrasound",
                    ultrasoundEdd = postTermEdd.toString(),
                    agreedEdd = postTermEdd.toString(),
                ),
            ),
        )

        compose.onNodeWithText(str(R.string.dating_calculated_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.post_term_badge)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_fieldVisibility_lmpMethod_andTodayButton() {
        show(datingState(values = DatingDraftValues(datingMethod = "LMP")))

        node("lmpDate").performScrollTo().assertIsDisplayed()
        node("lmpDateCalendarButton").performScrollTo().assertIsDisplayed()
        node("lmpDateTodayButton").performScrollTo().assertIsDisplayed()

        node("ultrasoundEdd").assertDoesNotExist()
        node("ivfTransferDate").assertDoesNotExist()
        node("agreedEdd").performScrollTo().assertIsDisplayed()

        // Clicking today button populates today's date
        node("lmpDateTodayButton").performClick()
        compose.waitForIdle()
        assertEquals(LocalDate.now().toString(), state.datingDraft!!.values.lmpDate)
        assertTrue(events.contains(AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, LocalDate.now().toString())))
    }

    @Test
    fun datingEditor_fieldVisibility_ultrasoundMethod() {
        show(datingState(values = DatingDraftValues(datingMethod = "Ultrasound")))

        node("lmpDate").assertDoesNotExist()
        node("ultrasoundEdd").performScrollTo().assertIsDisplayed()
        node("ultrasoundEddCalendarButton").performScrollTo().assertIsDisplayed()
        node("ivfTransferDate").assertDoesNotExist()
        node("agreedEdd").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_fieldVisibility_ivfMethod_andEmbryoAgeChips() {
        show(datingState(values = DatingDraftValues(datingMethod = "IVF_TRANSFER")))

        node("lmpDate").assertDoesNotExist()
        node("ultrasoundEdd").assertDoesNotExist()
        node("ivfTransferDate").performScrollTo().assertIsDisplayed()
        node("ivfTransferDateCalendarButton").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText(str(R.string.embryo_age)).performScrollTo().assertIsDisplayed()
        node("embryoDay3").performScrollTo().assertIsDisplayed()
        node("embryoDay5").performScrollTo().assertIsDisplayed()
        node("embryoDay6").performScrollTo().assertIsDisplayed()

        node("embryoDay3").performClick()
        compose.waitForIdle()
        assertEquals("3", state.datingDraft!!.values.embryoAgeDays)
        assertTrue(events.contains(AncEvent.DatingDraftChanged(DatingDraftField.EMBRYO_AGE_DAYS, "3")))
    }

    @Test
    fun datingEditor_fieldVisibility_clinicalMethod_andRecordedDateNotes() {
        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "Ultrasound",
                    lmpDate = "2026-05-01",
                    ultrasoundEdd = "2027-02-11",
                ),
            ),
        )

        // With Ultrasound method, LMP is on record: RecordedDateNote displays formatted date
        val expectedLmpNote = str(R.string.lmp_on_record, formatDmy("2026-05-01")!!)
        compose.onNodeWithText(expectedLmpNote).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_touchTargets_atLeast48dp() {
        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    lmpDate = "2026-05-01",
                ),
            ),
        )

        assertMinTouchTarget("ivfTransferDateCalendarButton")
        assertMinTouchTarget("embryoDay3")
        assertMinTouchTarget("embryoDay5")
        assertMinTouchTarget("embryoDay6")
        assertMinTouchTarget("agreedEddCalendarButton")
        assertMinTouchTarget("cancelDating")
        assertMinTouchTarget("saveDating")
    }

    @Test
    fun datingEditor_lmpTodayButton_touchTarget_atLeast48dp() {
        show(datingState(values = DatingDraftValues(datingMethod = "LMP")))

        assertMinTouchTarget("lmpDateCalendarButton")
        assertMinTouchTarget("lmpDateTodayButton")
    }

    @Test
    fun datingEditor_standalone_cancelAndSave_emitEvents() {
        show(datingState())

        node("saveDating").performScrollTo().performClick()
        assertTrue("SubmitDating emitted", events.contains(AncEvent.SubmitDating))

        events.clear()
        node("cancelDating").performScrollTo().performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun datingEditor_standalone_saving_disablesButtons_showsSpinnerAndSavingLabel() {
        show(datingState(savePhase = SavePhase.PENDING))

        node("saveDating").assertIsNotEnabled()
        node("cancelDating").assertIsNotEnabled()
        node("datingSaveSpinner").assertExists()
        compose.onNodeWithText(str(R.string.saving_label), useUnmergedTree = true).assertExists()
    }

    @Test
    fun datingEditor_noTextBelow12sp_fontScale1_0() {
        val targetEdd = LocalDate.now().plusDays(89)
        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = "2026-10-01",
                    agreedEdd = targetEdd.toString(),
                ),
            ),
            fontScale = 1.0f,
        )
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun datingEditor_noTextBelow12sp_fontScale1_3() {
        val targetEdd = LocalDate.now().plusDays(89)
        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = "2026-10-01",
                    agreedEdd = targetEdd.toString(),
                ),
            ),
            fontScale = 1.3f,
        )
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun datingEditor_360dp_fontScale2_0_noClipping() {
        val targetEdd = LocalDate.now().plusDays(89)
        show(
            datingState(
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = "2026-10-01",
                    agreedEdd = targetEdd.toString(),
                ),
            ),
            fontScale = 2.0f,
        )

        compose.onNodeWithText(str(R.string.dating_calculated_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.embryo_age)).performScrollTo().assertIsDisplayed()
        node("saveDating").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_rtlMirror_darkTheme_noClipping() {
        show(datingState(), darkTheme = true, layoutDirection = LayoutDirection.Rtl)

        node("datingEditor").assertIsDisplayed()
        node("cancelDating").performScrollTo().assertIsDisplayed()
        node("saveDating").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun datingEditor_inFlow_hidesStandaloneCancelSave() {
        show(datingState(inFlow = true))

        node("cancelDating").assertDoesNotExist()
        node("saveDating").assertDoesNotExist()
    }

    private companion object {
        const val HOST_TAG = "historyEditorsHost"
    }
}
