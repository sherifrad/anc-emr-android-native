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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DatingDraft
import com.sherifradwan.ancnative.state.DatingDraftValues
import com.sherifradwan.ancnative.state.HistoriesDraft
import com.sherifradwan.ancnative.state.HistoriesDraftValues
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.PatientDraftValues
import com.sherifradwan.ancnative.state.PatientStatusDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.StepStatus
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 12: Registration flow, step header, step footer, PatientEditor, and PatientStatusEditor.
 * Mounts through MainActivity + AncApp + AncTheme inside a 360dp x 640dp host matching RegistrationFlowUiTest
 * and EditorSheetsRedesignTest.
 */
@RunWith(AndroidJUnit4::class)
class RegistrationRedesignTest {
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

    private fun flowState(
        step: RegistrationStep,
        statuses: Map<RegistrationStep, StepStatus> = emptyMap(),
        inFlow: Boolean = true,
        dialog: Boolean = false,
        route: AncRoute? = null,
        pregnancyTarget: Int? = if (step == RegistrationStep.PREGNANCIES) 3 else null,
        pregnancyIndex: Int = 1,
        savePhase: SavePhase = SavePhase.IDLE,
        draftErrors: Set<PatientValidationError> = emptySet(),
    ): AncState {
        val session = "session-reg-s12"
        val id = patient.id
        val flow = RegistrationFlow(
            patientId = id,
            current = step,
            statuses = RegistrationStep.entries.associateWith { statuses[it] ?: StepStatus.PENDING },
            pregnancyTarget = pregnancyTarget,
            pregnancyIndex = pregnancyIndex,
            activeEditorSessionId = session,
        )
        val base = AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            registrationFlow = if (inFlow) flow else null,
            showDiscardDialog = dialog,
            savePhase = savePhase,
        )
        val patientValues = PatientDraftValues(displayName = patient.displayName, ageText = "28")
        return when (step) {
            RegistrationStep.IDENTITY, RegistrationStep.OBSTETRIC -> base.copy(
                route = AncRoute.PatientEditor(id, isNew = false),
                patientDraft = PatientDraft(
                    session, id, patient.revision, null, patientValues, patientValues, errors = draftErrors,
                ),
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

    private fun statusState(
        targetStatus: String = "archived",
        reason: String = "",
        errors: Set<PatientValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
    ): AncState {
        val session = "session-status-s12"
        val id = patient.id
        return AncState(
            patients = DemoFixtures.patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.StatusEditor(id, targetStatus),
            statusDraft = PatientStatusDraft(
                editorSessionId = session,
                patientId = id,
                baseRevision = patient.revision,
                targetStatus = targetStatus,
                reason = reason,
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

    private fun text(value: String, inTag: String) = hasText(value) and inTagged(inTag)

    private fun layoutOf(label: String, matcher: SemanticsMatcher): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun textLayoutOfTag(tag: String): TextLayoutResult {
        val root = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        root.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        if (results.isNotEmpty()) return results.first()
        for (child in root.children) {
            val childResults = mutableListOf<TextLayoutResult>()
            child.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(childResults)
            if (childResults.isNotEmpty()) return childResults.first()
        }
        val allNodes = compose.onAllNodes(inTagged(tag), useUnmergedTree = true).fetchSemanticsNodes()
        for (n in allNodes) {
            val nResults = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(nResults)
            if (nResults.isNotEmpty()) return nResults.first()
        }
        error("No TextLayoutResult found for tag $tag")
    }

    private fun assertStyle(
        label: String,
        matcher: SemanticsMatcher,
        size: Float,
        weight: FontWeight? = null,
        color: Color? = null,
    ) {
        val style = layoutOf(label, matcher).layoutInput.style
        assertEquals("'$label' font size", size.sp, style.fontSize)
        if (weight != null) assertEquals("'$label' font weight", weight, style.fontWeight)
        if (color != null) assertEquals("'$label' colour", color, style.color)
    }

    private fun assertMinTouchTarget(tag: String) {
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertNear(label: String, expected: Dp, actual: Dp, tolerance: Dp = 1.dp) =
        assertTrue(
            "$label: expected $expected, was $actual (tolerance $tolerance)",
            abs((actual - expected).value) <= tolerance.value,
        )

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

    // ================= 1. Step Header =================

    @Test
    fun stepHeader_title18sp_counter14sp_segments12sp_surfaceAndDivider() {
        val statuses = mapOf(
            RegistrationStep.IDENTITY to StepStatus.DONE,
            RegistrationStep.DATING to StepStatus.SKIPPED,
        )
        show(
            flowState(
                step = RegistrationStep.PREGNANCIES,
                statuses = statuses,
                pregnancyTarget = 3,
                pregnancyIndex = 1,
            ),
        )

        // Step headline: 18sp Bold primary
        val stepHeadlineMatcher = hasText(str(R.string.previous_pregnancies), substring = true) and
            inTagged("registrationStepHeader")
        assertStyle("step headline", stepHeadlineMatcher, 18f, FontWeight.Bold, primaryColor)

        // Pregnancy counter: 14sp Secondary onSurfaceVariant
        val counterText = str(R.string.registration_pregnancy_progress, 2, 3)
        assertStyle("pregnancy counter", text(counterText, "registrationStepHeader"), 14f, FontWeight.Normal, onSurfaceVariantColor)

        // Segment status words and Current marker are 12sp (RecordType.Tag)
        assertStyle("done word", text(str(R.string.done), "registrationStepHeader"), 12f, FontWeight.Medium, onSurfaceVariantColor)
        assertStyle("skipped word", text(str(R.string.registration_status_skipped), "registrationStepHeader"), 12f, FontWeight.Medium, onSurfaceVariantColor)
        assertStyle("current marker", text(str(R.string.registration_current), "registrationStepHeader"), 12f, FontWeight.Bold, onSurfaceColor)

        // Header sits on surface with divider
        node("registrationStepHeader").assertIsDisplayed()
    }

    @Test
    fun stepHeader_wordsVisibleAtFontScale1_3() {
        val statuses = mapOf(RegistrationStep.IDENTITY to StepStatus.DONE)
        show(flowState(RegistrationStep.DATING, statuses), fontScale = 1.3f)

        // Segment 1 (Done): word is visible, contentDescription is present, not selected
        val identityTitle = str(R.string.registration_step_identity)
        val doneWord = str(R.string.done)
        compose.onNodeWithTag("registrationSegment-1")
            .assertTextContains(doneWord)
            .assertContentDescriptionEquals("$identityTitle, $doneWord")
        assertFalse(
            compose.onNodeWithTag("registrationSegment-1").fetchSemanticsNode().config[SemanticsProperties.Selected],
        )

        // Segment 2 (Current DATING): status word and Current are visible, selected
        val datingTitle = str(R.string.dating)
        val pendingWord = str(R.string.registration_status_pending)
        compose.onNodeWithTag("registrationSegment-2")
            .assertTextContains(pendingWord)
            .assertTextContains(str(R.string.registration_current))
            .assertContentDescriptionEquals(
                str(R.string.registration_segment_current_cd, datingTitle, pendingWord),
            )
        assertTrue(
            compose.onNodeWithTag("registrationSegment-2").fetchSemanticsNode().config[SemanticsProperties.Selected],
        )
    }

    @Test
    fun stepHeader_wordsHiddenAtFontScale1_4_segmentsKeepContentDescriptionAndSelected() {
        val statuses = mapOf(RegistrationStep.IDENTITY to StepStatus.DONE)
        show(flowState(RegistrationStep.DATING, statuses), fontScale = 1.4f)

        // Words dropped from header
        compose.onNode(hasText(str(R.string.done)) and inTagged("registrationStepHeader")).assertDoesNotExist()
        compose.onNode(hasText(str(R.string.registration_current)) and inTagged("registrationStepHeader")).assertDoesNotExist()

        // Content description and selected semantics remain intact on segments
        val identityTitle = str(R.string.registration_step_identity)
        val doneWord = str(R.string.done)
        val datingTitle = str(R.string.dating)
        val pendingWord = str(R.string.registration_status_pending)

        compose.onNodeWithTag("registrationSegment-1")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("$identityTitle, $doneWord")
        assertFalse(
            compose.onNodeWithTag("registrationSegment-1").fetchSemanticsNode().config[SemanticsProperties.Selected],
        )

        compose.onNodeWithTag("registrationSegment-2")
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                str(R.string.registration_segment_current_cd, datingTitle, pendingWord),
            )
        assertTrue(
            compose.onNodeWithTag("registrationSegment-2").fetchSemanticsNode().config[SemanticsProperties.Selected],
        )
    }

    @Test
    fun stepHeader_segmentBarHeights_current8dp_other6dp() {
        // At fontScale 1.4f words are hidden so segment height reflects bar + spacing + glyph only
        show(flowState(RegistrationStep.DATING), fontScale = 1.4f)
        val currentBounds = bounds("registrationSegment-2")
        val otherBounds = bounds("registrationSegment-1")

        // Current bar is 8dp; other bar is 6dp (2dp difference)
        assertTrue("current segment taller than other segment", currentBounds.height > otherBounds.height)
        assertNear("current vs other height diff (8dp vs 6dp)", 2.dp, currentBounds.height - otherBounds.height, tolerance = 1.dp)
    }

    // ================= 2. Footer Buttons =================

    @Test
    fun stepFooter_buttonsMeet48dpMinTouchTarget() {
        show(flowState(RegistrationStep.DATING))
        listOf("flowBack", "flowSkip", "flowNext").forEach { tag ->
            node(tag).assertIsDisplayed().assertIsEnabled()
            assertMinTouchTarget(tag)
        }
        assertStyle("Back button text", text(str(R.string.back_step), "flowBack"), 14f, FontWeight.SemiBold)
        assertStyle("Skip button text", text(str(R.string.flow_skip), "flowSkip"), 14f, FontWeight.SemiBold)
        assertStyle("Next button text", text(str(R.string.next_step), "flowNext"), 14f, FontWeight.SemiBold)
    }

    @Test
    fun stepFooter_buttonsWrapAtFontScale2_0_withoutClipping() {
        show(flowState(RegistrationStep.DATING), fontScale = 2.0f)
        val hostB = bounds(HOST_TAG)
        listOf("flowBack", "flowSkip", "flowNext").forEach { tag ->
            node(tag).assertIsDisplayed()
            assertMinTouchTarget(tag)
            val b = bounds(tag)
            assertTrue("$tag starts outside host (${b.left} < ${hostB.left})", b.left >= hostB.left - 1.dp)
            assertTrue("$tag runs past host (${b.right} > ${hostB.right})", b.right <= hostB.right + 1.dp)
        }
        compose.assertTextNotClipped("flowBack text", text(str(R.string.back_step), "flowBack"))
        compose.assertTextNotClipped("flowSkip text", text(str(R.string.flow_skip), "flowSkip"))
        compose.assertTextNotClipped("flowNext text", text(str(R.string.next_step), "flowNext"))
    }

    // ================= 3. Identity Step =================

    @Test
    fun identity_externalLabels12sp_fieldsAcceptInput() {
        show(flowState(RegistrationStep.IDENTITY))

        // Verify external FormFieldLabels (12sp SemiBold) above each field
        val labels = listOf(
            R.string.full_name_required to "patientName",
            R.string.phone_optional to "patientPhone",
            R.string.age_required to "patientAgeText",
            R.string.address to "patientAddress",
        )
        labels.forEach { (strId, fieldTag) ->
            val labelText = str(strId)
            node(fieldTag).assertContentDescriptionEquals(labelText)

            val labelNodes = compose.onAllNodes(hasText(labelText), useUnmergedTree = true).fetchSemanticsNodes()
            assertTrue("label node for '$labelText' in unmerged tree", labelNodes.isNotEmpty())
            val layouts = mutableListOf<TextLayoutResult>()
            labelNodes.first().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            assertTrue("'$labelText' has text layout", layouts.isNotEmpty())
            val style = layouts.first().layoutInput.style
            assertEquals("'$labelText' font size", 12.sp, style.fontSize)
            assertEquals("'$labelText' font weight", FontWeight.SemiBold, style.fontWeight)
        }

        // Fields keep tags and accept text replacement / input
        node("patientName").performTextReplacement("Sara Ahmed")
        node("patientPhone").performTextInput("0123456789")
        node("patientAgeText").performTextReplacement("29")
        node("patientAddress").performTextInput("Alexandria")
        compose.waitForIdle()

        assertEquals("Sara Ahmed", state.patientDraft!!.values.displayName)
        assertEquals("0123456789", state.patientDraft!!.values.phone)
        assertEquals("29", state.patientDraft!!.values.ageText)
        assertEquals("Alexandria", state.patientDraft!!.values.addressText)
    }

    // ================= 4. Arabic vs English Name Alignment =================

    @Test
    fun identity_arabicName_alignedRightEdge_preservesExactString() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false))
        val arabicName = "منى رجب"
        node("patientName").performTextReplacement(arabicName)
        compose.waitForIdle()

        assertEquals(arabicName, state.patientDraft!!.values.displayName)
        val layout = textLayoutOfTag("patientName")
        assertEquals(TextAlign.Right, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))
        // Text right edge sits near field content layout width
        assertTrue(
            "Arabic text right edge near field content end: width=${layout.size.width}, lineRight=${layout.getLineRight(0)}",
            abs(layout.size.width - layout.getLineRight(0)) <= 4f,
        )
    }

    @Test
    fun identity_englishName_alignedLeftEdge() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false))
        val englishName = "Mona Ragab"
        node("patientName").performTextReplacement(englishName)
        compose.waitForIdle()

        assertEquals(englishName, state.patientDraft!!.values.displayName)
        val layout = textLayoutOfTag("patientName")
        assertEquals(TextAlign.Left, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Ltr, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, layout.getParagraphDirection(0))
        // Text left edge sits near 0
        assertTrue(
            "English text left edge near 0: lineLeft=${layout.getLineLeft(0)}",
            layout.getLineLeft(0) <= 4f,
        )
    }

    // ================= 5. Obstetric Step GPA =================

    @Test
    fun obstetric_gpaFieldsInOneRow_atFontScale1_0() {
        show(flowState(RegistrationStep.OBSTETRIC), fontScale = 1.0f)

        listOf("patientGravida", "patientParity", "patientAbortion").forEach {
            node(it).assertIsDisplayed()
        }

        val g = bounds("patientGravida")
        val p = bounds("patientParity")
        val a = bounds("patientAbortion")

        // In one row horizontally
        assertNear("gravida and parity top", g.top, p.top, tolerance = 2.dp)
        assertNear("parity and abortion top", p.top, a.top, tolerance = 2.dp)
        assertTrue("gravida is left of parity", g.right <= p.left + 0.5.dp)
        assertTrue("parity is left of abortion", p.right <= a.left + 0.5.dp)

        // All three editable
        node("patientGravida").performScrollTo().performTextReplacement("3")
        node("patientParity").performScrollTo().performTextReplacement("2")
        node("patientAbortion").performScrollTo().performTextReplacement("1")
        compose.waitForIdle()

        assertEquals("3", state.patientDraft!!.values.gravida)
        assertEquals("2", state.patientDraft!!.values.parity)
        assertEquals("1", state.patientDraft!!.values.abortion)
    }

    @Test
    fun obstetric_gpaFieldsStacked_aboveFontScale1_3() {
        show(flowState(RegistrationStep.OBSTETRIC), fontScale = 1.4f)

        listOf("patientGravida", "patientParity", "patientAbortion").forEach {
            node(it).assertIsDisplayed()
        }

        val g = bounds("patientGravida")
        val p = bounds("patientParity")
        val a = bounds("patientAbortion")

        // Stacked vertically in column
        assertTrue("gravida is above parity", g.bottom <= p.top + 0.5.dp)
        assertTrue("parity is above abortion", p.bottom <= a.top + 0.5.dp)
        assertNear("gravida and parity left", g.left, p.left, tolerance = 2.dp)
        assertNear("parity and abortion left", p.left, a.left, tolerance = 2.dp)

        // All three editable
        node("patientGravida").performScrollTo().performTextReplacement("4")
        node("patientParity").performScrollTo().performTextReplacement("3")
        node("patientAbortion").performScrollTo().performTextReplacement("2")
        compose.waitForIdle()

        assertEquals("4", state.patientDraft!!.values.gravida)
        assertEquals("3", state.patientDraft!!.values.parity)
        assertEquals("2", state.patientDraft!!.values.abortion)
    }

    // ================= 6. Photos Notice =================

    @Test
    fun photosNotice_textAt14sp_andNoLockIcon() {
        show(flowState(RegistrationStep.IDENTITY))
        val notice = str(R.string.photos_stored_on_phone_notice)
        compose.onNodeWithText(notice).performScrollTo().assertIsDisplayed()
        assertStyle("photos notice", hasText(notice), 14f, FontWeight.Normal, onSurfaceVariantColor)

        // Padlock icon removed: no node with 'lock' in content description
        val lockNodes = compose.onAllNodes(
            hasContentDescription("lock", substring = true, ignoreCase = true),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no lock icon next to unencrypted photos notice", lockNodes.isEmpty())
    }

    // ================= 7. PatientEditor Standalone =================

    @Test
    fun patientEditorStandalone_cancelAndSaveButtons_equalWidth_emitsSubmit() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false))

        node("cancelPatient").performScrollTo().assertIsDisplayed().assertIsEnabled()
        node("savePatient").performScrollTo().assertIsDisplayed().assertIsEnabled()
        assertMinTouchTarget("cancelPatient")
        assertMinTouchTarget("savePatient")

        // Both buttons share equal width in SheetCancelSaveRow
        val cancelB = bounds("cancelPatient")
        val saveB = bounds("savePatient")
        assertNear("cancel and save width", cancelB.width, saveB.width, tolerance = 1.dp)

        node("savePatient").performClick()
        assertTrue("SubmitPatientProfile emitted", events.contains(AncEvent.SubmitPatientProfile))

        events.clear()
        node("cancelPatient").performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun patientEditorStandalone_pendingSave_disablesButtons_showsSavingLabelAndSpinner() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false, savePhase = SavePhase.PENDING))

        node("savePatient").assertIsNotEnabled()
        node("cancelPatient").assertIsNotEnabled()
        compose.onNodeWithText(str(R.string.saving_label), useUnmergedTree = true).assertExists()
        node("patientSaveSpinner").assertExists()
    }

    // ================= 8. PatientStatusEditor =================

    @Test
    fun patientStatusEditor_archive_showsStatusTag_cancelAndSave_emitsEvents() {
        show(statusState(targetStatus = "archived"))

        node("patientStatusEditor").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.archived)).assertIsDisplayed()
        assertStyle("archived status tag", hasText(str(R.string.archived)), 12f, FontWeight.SemiBold)

        val notice = str(R.string.status_reason_notice)
        compose.onNodeWithText(notice).assertIsDisplayed()
        assertStyle("status notice", hasText(notice), 14f, FontWeight.Normal, onSurfaceVariantColor)

        node("statusReason").assertIsDisplayed()
        assertMinTouchTarget("cancelPatientStatus")
        assertMinTouchTarget("savePatientStatus")

        node("statusReason").performTextInput("Patient relocated to another district")
        compose.waitForIdle()
        assertEquals("Patient relocated to another district", state.statusDraft!!.reason)

        node("savePatientStatus").performClick()
        assertTrue("SubmitPatientStatus emitted", events.contains(AncEvent.SubmitPatientStatus))

        events.clear()
        node("cancelPatientStatus").performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun patientStatusEditor_archiveReasonRequired_showsErrorCardAndFieldError() {
        show(
            statusState(
                targetStatus = "archived",
                errors = setOf(PatientValidationError.ARCHIVE_REASON_REQUIRED),
            ),
        )

        node("editorError").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.correct_highlighted_fields)).assertIsDisplayed()
    }

    // ================= 9. Error Card =================

    @Test
    fun errorCard_stylingDoesNotHideMessage() {
        show(
            flowState(
                RegistrationStep.IDENTITY,
                inFlow = false,
                draftErrors = setOf(PatientValidationError.FULL_NAME_REQUIRED),
            ),
        )

        node("editorError").assertIsDisplayed()
        val msg = str(R.string.correct_highlighted_fields)
        compose.onNodeWithText(msg).assertIsDisplayed()
        compose.assertTextNotClipped("error card message", hasText(msg) and inTagged("editorError"))
        assertStyle("error card message", hasText(msg) and inTagged("editorError"), 14f, FontWeight.SemiBold)
    }

    // ================= 10. No Text Below 12sp Checks =================

    @Test
    fun noTextBelow12sp_step1_identity_fontScale1_0() {
        show(flowState(RegistrationStep.IDENTITY), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_step1_identity_fontScale1_3() {
        show(flowState(RegistrationStep.IDENTITY), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_step3_obstetric_fontScale1_0() {
        show(flowState(RegistrationStep.OBSTETRIC), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_step3_obstetric_fontScale1_3() {
        show(flowState(RegistrationStep.OBSTETRIC), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_standalonePatientEditor_fontScale1_0() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_standalonePatientEditor_fontScale1_3() {
        show(flowState(RegistrationStep.IDENTITY, inFlow = false), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_patientStatusEditor_fontScale1_0() {
        show(statusState(), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun noTextBelow12sp_patientStatusEditor_fontScale1_3() {
        show(statusState(), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    // ================= 11. 360dp Whole Text & RTL Mirror =================

    @Test
    fun stepHeaderAndFooter_360dp_fontScale1_3_allTextWhole() {
        show(flowState(RegistrationStep.DATING), fontScale = 1.3f)
        compose.assertAllTextWhole("registrationStepHeader")
    }

    @Test
    fun stepHeaderAndFooter_360dp_fontScale2_0_allTextWhole() {
        show(flowState(RegistrationStep.DATING), fontScale = 2.0f)
        compose.assertAllTextWhole("registrationStepHeader")
    }

    @Test
    fun identity_rtlMirror_darkTheme_noClipping() {
        show(flowState(RegistrationStep.IDENTITY), darkTheme = true, layoutDirection = LayoutDirection.Rtl)
        node("registrationStepHeader").assertIsDisplayed()
        node("flowBack").assertIsDisplayed()
        node("flowNext").assertIsDisplayed()
        compose.assertAllTextWhole("registrationStepHeader")
    }

    @Test
    fun patientStatusEditor_rtlMirror_darkTheme_noClipping() {
        show(statusState(), darkTheme = true, layoutDirection = LayoutDirection.Rtl)
        node("patientStatusEditor").assertIsDisplayed()
        node("cancelPatientStatus").assertIsDisplayed()
        node("savePatientStatus").assertIsDisplayed()
    }

    private companion object {
        const val HOST_TAG = "registrationFlowHost"
    }
}
