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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.DraftValues
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.VisitDraft
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.VisitEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 14: VisitEditor, VisitEditorSteps, VisitPatientBanner, VisitStepHeader.
 * Mounts VisitEditor through MainActivity + AncTheme inside a 360dp x 640dp host,
 * matching HistoryEditorsRedesignTest and RegistrationRedesignTest style (single setContent
 * per test, shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides,
 * useUnmergedTree where label text is cleared by clearAndSetSemantics, performScrollTo
 * before assertIsDisplayed, select nodes by testTag, >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class VisitEditorRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val scan1 = DemoUltrasoundScan(
        id = "scan-s14-1",
        patientId = "patient-s14",
        date = "2026-10-10",
        gestationalAgeWeeks = 22,
        gestationalAgeDays = 3,
        fetalNumber = "Singleton",
        presentation = "Cephalic",
        placentalSite = "Anterior",
        amnioticFluid = "Normal",
        fetalHeartRate = "142",
        biometryBpd = null,
        biometryHc = null,
        biometryAc = null,
        biometryFl = null,
        estimatedFetalWeight = null,
        impression = "Normal anatomy scan",
        linkedVisitId = null,
    )

    private val pastVisit = DemoVisit(
        id = "past-v1",
        patientId = "patient-s14",
        date = "2026-10-15",
        bloodPressure = "118/76",
        weight = "74.5",
        findings = "FHR 144 bpm · Oedema none · Movements good · Urine negative",
        notes = "Past note",
        revision = 1,
        isSyntheticEdit = false,
    )

    private val monaRagab = DemoPatient(
        id = "patient-s14",
        caseId = "ANC-S14-001",
        displayName = "Mona Ragab",
        dateOfBirth = "1996-05-10",
        ageText = "30",
        phone = "+201001112233",
        addressText = "Cairo",
        riskLevel = "Low Risk",
        bloodGroupText = "A+",
        gravida = 4L,
        parity = 1L,
        abortion = 2L,
        medicalHistory = "None",
        surgicalHistory = "None",
        familyHistory = "None",
        allergyHistory = "None",
        clinicalAttention = null,
        obstetricNotes = null,
        revision = 1,
        dating = DemoPatientDating(
            patientId = "patient-s14",
            lmpDate = "2026-05-07",
            ultrasoundEdd = "2027-02-11",
            agreedEdd = "2027-02-11",
            datingMethod = "Ultrasound",
            notes = null,
            revision = 1,
        ),
        visits = listOf(pastVisit),
        ultrasoundScans = listOf(scan1),
    )

    private val events = mutableListOf<AncEvent>()
    private var onSaveCalled = false
    private var state by mutableStateOf(AncState(patients = listOf(monaRagab)))

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun onEvent(event: AncEvent) {
        events += event
        state = AncReducer.reduce(state, event)
    }

    private fun makeState(
        patient: DemoPatient = monaRagab,
        values: DraftValues = DraftValues(date = "2026-11-14", bloodPressure = "110/70", systolic = "110", diastolic = "70", weight = "70.0", fetalHeartRate = "140"),
        errors: Set<DraftError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
        visitId: String? = null,
    ): AncState {
        val session = "visit-session-s14"
        return AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.VisitEditor(patient.id),
            draft = VisitDraft(
                editorSessionId = session,
                patientId = patient.id,
                visitId = visitId,
                baseRevision = 1,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    private fun show(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        onSaveCalled = false
        val route = state.route as AncRoute.VisitEditor
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            VisitEditor(
                                state = state,
                                route = route,
                                onEvent = ::onEvent,
                                onSave = { onSaveCalled = true },
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

    private fun assertMinTouchTarget(tag: String, scroll: Boolean = true) {
        if (scroll) {
            node(tag).performScrollTo()
        }
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertTargetInside360(tag: String, scroll: Boolean = true) {
        if (scroll) {
            node(tag).performScrollTo()
        }
        val b = bounds(tag)
        val host = bounds(HOST_TAG)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
        assertTrue("$tag starts outside host left: b=$b host=$host", b.left >= host.left - 1.dp)
        assertTrue("$tag runs past host right: b=$b host=$host", b.right <= host.right + 1.dp)
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
                if (size.isSp) {
                    assertTrue("'$text' is $size (< 12sp)", size.value >= 11.99f)
                }
            }
        }
    }

    private fun layoutOf(label: String, matcher: SemanticsMatcher): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    companion object {
        private const val HOST_TAG = "hostContainer"
    }

    // =========================================================================
    // 1. Text size & clean hierarchy (no text under 12sp, no legacy strings)
    // =========================================================================

    @Test
    fun walkSemanticsTree_noTextBelow12sp_andNoLegacyStrings_atFontScale1_0() {
        show(makeState(), fontScale = 1.0f)

        // Step 0: Date & BP
        node("visitEditor").assertIsDisplayed()
        assertNoTextBelow12sp("visitEditor")
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Tamper-Evident", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Normal Risk", substring = true).assertDoesNotExist()
        compose.onNodeWithText("(Flag)", substring = true).assertDoesNotExist()
        compose.onNodeWithText("27w 2d", substring = true).assertDoesNotExist()

        // Step 1: Wt & FHR
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("(Flag)", substring = true).assertDoesNotExist()

        // Step 2: Findings
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("(Flag)", substring = true).assertDoesNotExist()

        // Step 3: Review
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("27w 2d", substring = true).assertDoesNotExist()
    }

    @Test
    fun walkSemanticsTree_noTextBelow12sp_atFontScale1_3() {
        show(makeState(), fontScale = 1.3f)

        // Step 0
        assertNoTextBelow12sp("visitEditor")

        // Step 1
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")

        // Step 2
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")

        // Step 3
        node("nextStep").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("visitEditor")
    }

    @Test
    fun savePhaseFallback_noTextBelow12sp_preservesWorkflowSubtitleAndStaleCard() {
        val s = makeState(
            errors = setOf(DraftError.STALE_REVISION),
            savePhase = SavePhase.ERROR,
        )
        show(s)

        node("visitEditor").assertIsDisplayed()

        // Patient banner shows in every save phase; subtitle is empty
        compose.onNodeWithText(monaRagab.displayName, useUnmergedTree = true).assertIsDisplayed()

        // Error cards
        node("staleError").performScrollTo().assertIsDisplayed()
        node("saveStatus").performScrollTo().assertIsDisplayed()

        // Banner and StepHeader hidden in non-idle fallback
        compose.onNodeWithText("Step 1 of 4").assertDoesNotExist()
        node("stepSegment-0").assertDoesNotExist()

        // Fallback fields present
        node("visitDate").performScrollTo().assertIsDisplayed()
        node("visitBloodPressure").performScrollTo().assertIsDisplayed()
        node("visitWeight").performScrollTo().assertIsDisplayed()
        node("visitFindings").performScrollTo().assertIsDisplayed()
        node("visitNotes").performScrollTo().assertIsDisplayed()
        node("cancelVisit").assertIsDisplayed()
        node("saveVisit").assertIsDisplayed()

        assertNoTextBelow12sp("visitEditor")
    }

    // =========================================================================
    // 2. Patient Banner (Mona Ragab, parity, GA, blood group, risk & RTL alignment)
    // =========================================================================

    @Test
    fun patientBanner_monaRagab_gaParityBloodGroup_andArabicAlignment() {
        val arabicPatient = monaRagab.copy(displayName = "منى رجب")
        show(makeState(patient = arabicPatient))

        // Arabic name displayed right-aligned
        compose.onNodeWithText("منى رجب", useUnmergedTree = true).assertIsDisplayed()
        val layout = layoutOf("name", hasText("منى رجب"))
        assertEquals(TextAlign.Right, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))

        // BandChips for GA, Parity, Blood Group
        compose.onAllNodesWithText("27+2 wks", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        compose.onNode(hasText("G4 P1 A2", substring = true), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("A+", substring = true), useUnmergedTree = true).assertIsDisplayed()

        // Low/Normal risk does NOT show a risk pill
        compose.onNodeWithText("Normal Risk", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Low Risk", substring = true).assertDoesNotExist()
    }

    @Test
    fun patientBanner_englishName_alignedLeftEdge() {
        show(makeState(patient = monaRagab))

        compose.onNodeWithText("Mona Ragab", useUnmergedTree = true).assertIsDisplayed()
        val layout = layoutOf("name", hasText("Mona Ragab"))
        assertEquals(TextAlign.Left, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Ltr, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, layout.getParagraphDirection(0))
    }

    @Test
    fun patientBanner_moderateAndHighRiskTags_andRhNegative() {
        val highRiskRhNeg = monaRagab.copy(
            riskLevel = "High Risk",
            bloodGroupText = "A-",
        )
        show(makeState(patient = highRiskRhNeg))

        // High Risk StatusTag
        compose.onNodeWithText(str(R.string.high_risk), substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Rh negative blood group tag
        compose.onNode(hasText("Rh negative", substring = true), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun patientBanner_moderateRiskTag_displayed() {
        val modRisk = monaRagab.copy(riskLevel = "Medium")
        show(makeState(patient = modRisk))

        compose.onNodeWithText(str(R.string.moderate_risk), substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // 3. Step Header (Tab navigation, direct jumps, fontScale hiding labels)
    // =========================================================================

    @Test
    fun stepHeader_navigationJumpsDirectly_andDisplaysLabelsAtNormalFontScale() {
        show(makeState(), fontScale = 1.0f)

        // Step 0 active
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()
        val stepTitles = listOf(
            R.string.step_date_bp,
            R.string.step_wt_fh,
            R.string.step_findings,
            R.string.step_review_save,
        )
        for (i in 0 until 4) {
            val cd = str(R.string.visit_step_segment_cd, str(stepTitles[i]), i + 1, 4)
            compose.onNode(hasTestTag("stepSegment-$i") and hasContentDescription(cd), useUnmergedTree = true)
                .assertIsDisplayed()
        }

        // Jump directly to Step 3 (Review & Save)
        node("stepSegment-3").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 4 of 4").assertIsDisplayed()
        node("saveVisit").assertIsDisplayed()

        // Jump directly to Step 1 (Wt & FHR)
        node("stepSegment-1").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 2 of 4").assertIsDisplayed()
        node("visitWeight").performScrollTo().assertIsDisplayed()

        // Jump back to Step 0 (Date & BP)
        node("stepSegment-0").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()
        node("visitDate").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun stepHeader_largeFontScale2_0_hidesShortLabelsPreservesTabs() {
        show(makeState(), fontScale = 2.0f)

        // Short labels hidden above 1.3f
        compose.onNodeWithText(str(R.string.visit_step_short_date_bp)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.visit_step_short_wt_fhr)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.visit_step_short_findings)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.visit_step_short_review)).assertDoesNotExist()

        // Header title and step index still visible
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()

        // Segments still exist and are clickable
        node("stepSegment-3").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 4 of 4").assertIsDisplayed()
    }

    // =========================================================================
    // 4. Touch Targets (>= 47.99dp on every listed control)
    // =========================================================================

    @Test
    fun step0_and_step1_touchTargets_atLeast48dp() {
        show(makeState())

        // Step 0 segments
        assertTargetInside360("stepSegment-0", scroll = false)
        assertTargetInside360("stepSegment-1", scroll = false)
        assertTargetInside360("stepSegment-2", scroll = false)
        assertTargetInside360("stepSegment-3", scroll = false)

        // Step 0 date chips & button
        assertMinTouchTarget("visitDateToday")
        assertMinTouchTarget("visitDateYesterday")
        assertMinTouchTarget("visitDateCalendarButton")

        // Step 0 BP chips
        assertMinTouchTarget("bpPreset-sameLast")
        assertMinTouchTarget("bpPreset-normal")
        assertMinTouchTarget("bpPreset-addOther")

        // Step 0 bottom bar buttons
        assertTargetInside360("cancelVisit", scroll = false)
        assertTargetInside360("nextStep", scroll = false)

        // Advance to Step 1
        node("nextStep").performClick()
        compose.waitForIdle()

        // Step 1 weight chips & stepper
        assertMinTouchTarget("weightPreset-sameLast")
        assertMinTouchTarget("weightPreset-addOther")
        assertMinTouchTarget("visitWeightDecrease")
        assertMinTouchTarget("visitWeightIncrease")

        // Step 1 FHR chips & stepper
        assertMinTouchTarget("fhrPreset-sameLast")
        assertMinTouchTarget("fhrPreset-135")
        assertMinTouchTarget("fhrPreset-140")
        assertMinTouchTarget("fhrPreset-145")
        assertMinTouchTarget("fhrPreset-150")
        assertMinTouchTarget("visitFetalHeartRateDecrease")
        assertMinTouchTarget("visitFetalHeartRateIncrease")

        // Step 1 bottom bar
        assertTargetInside360("backStep", scroll = false)
        assertTargetInside360("nextStep", scroll = false)
    }

    @Test
    fun step2_and_step3_touchTargets_atLeast48dp() {
        show(makeState())

        // Jump to Step 2
        node("stepSegment-2").performClick()
        compose.waitForIdle()

        // Quick-fill button
        assertMinTouchTarget("fillNormalValues")

        // Bedside findings chips
        assertMinTouchTarget("oedemaChoice-None")
        assertMinTouchTarget("oedemaChoice-Mild")
        assertMinTouchTarget("oedemaChoice-Marked")
        assertMinTouchTarget("oedemaChoice-addOther")

        assertMinTouchTarget("movementsChoice-Good")
        assertMinTouchTarget("movementsChoice-Reduced")
        assertMinTouchTarget("movementsChoice-Absent")
        assertMinTouchTarget("movementsChoice-addOther")

        assertMinTouchTarget("urineChoice-Negative")
        assertMinTouchTarget("urineChoice-Trace")
        assertMinTouchTarget("urineChoice-+")
        assertMinTouchTarget("urineChoice-++")
        assertMinTouchTarget("urineChoice-addOther")

        // Advance to Step 3
        node("nextStep").performClick()
        compose.waitForIdle()

        // 7 Review rows
        assertMinTouchTarget("summaryRow-date")
        assertMinTouchTarget("summaryRow-bp")
        assertMinTouchTarget("summaryRow-weight")
        assertMinTouchTarget("summaryRow-fhr")
        assertMinTouchTarget("summaryRow-oedema")
        assertMinTouchTarget("summaryRow-movements")
        assertMinTouchTarget("summaryRow-urine")

        // Ultrasound link chip
        assertMinTouchTarget("linkScan-scan-s14-1")

        // Bottom bar
        assertTargetInside360("backStep", scroll = false)
        assertTargetInside360("saveVisit", scroll = false)
    }

    @Test
    fun customChoiceDialog_touchTargets_atLeast48dp_andDismisses() {
        show(makeState())

        node("bpPreset-addOther").performScrollTo().performClick()
        compose.waitForIdle()

        node("customChoiceInput").assertIsDisplayed()
        assertMinTouchTarget("customChoiceConfirm", scroll = false)
        assertMinTouchTarget("customChoiceCancel", scroll = false)

        node("customChoiceCancel").performClick()
        compose.waitForIdle()

        node("customChoiceInput").assertDoesNotExist()
    }

    // =========================================================================
    // 5. Behaviour Parity & Step 0 (BP dual sync, high BP warnings, non-blocking)
    // =========================================================================

    @Test
    fun step0_bpDualSync_highBpWarningRaisedAndSevere_nonBlockingSave() {
        show(makeState())

        // Enter 150/95 (Raised BP)
        node("visitSystolic").performTextReplacement("150")
        node("visitDiastolic").performTextReplacement("95")
        compose.waitForIdle()

        assertEquals("150/95", state.draft?.values?.bloodPressure)
        node("highBpWarning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.band_raised_bp)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.high_bp_flagged_warning)).performScrollTo().assertIsDisplayed()

        // Enter 170/115 (Severe BP)
        node("visitSystolic").performTextReplacement("170")
        node("visitDiastolic").performTextReplacement("115")
        compose.waitForIdle()

        assertEquals("170/115", state.draft?.values?.bloodPressure)
        node("highBpWarning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.band_severe_bp)).performScrollTo().assertIsDisplayed()

        // Advance to review: high BP does not block saving
        node("stepSegment-3").performScrollTo().performClick()
        compose.waitForIdle()
        node("saveVisit").assertIsDisplayed().assertIsEnabled()

        // Return to Step 0 and reset to normal BP
        node("stepSegment-0").performScrollTo().performClick()
        compose.waitForIdle()
        node("visitBloodPressure").performScrollTo().performTextReplacement("120/80")
        compose.waitForIdle()

        assertEquals("120", state.draft?.values?.systolic)
        assertEquals("80", state.draft?.values?.diastolic)
        node("highBpWarning").assertDoesNotExist()
    }

    @Test
    fun step0_todayAndYesterdayChips_populateDate() {
        show(makeState())

        val todayStr = java.time.LocalDate.now().toString()
        val yestStr = java.time.LocalDate.now().minusDays(1).toString()

        node("visitDateToday").performClick()
        compose.waitForIdle()
        assertEquals(todayStr, state.draft?.values?.date)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.DATE, todayStr)))

        node("visitDateYesterday").performClick()
        compose.waitForIdle()
        assertEquals(yestStr, state.draft?.values?.date)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.DATE, yestStr)))
    }

    // =========================================================================
    // 6. Step 1 (Weight and FHR steppers, bounds, increments, baseline status words)
    // =========================================================================

    @Test
    fun step1_weightStepper_incrementsDecrementsAndClamps() {
        show(makeState())
        node("stepSegment-1").performClick()
        compose.waitForIdle()

        // Initial 70.0 -> decrease by 0.1 -> 69.9
        node("visitWeightDecrease").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("69.9", state.draft?.values?.weight)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.WEIGHT, "69.9")))

        // Increase by 0.1 -> 70.0
        node("visitWeightIncrease").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("70.0", state.draft?.values?.weight)

        // Lower bound clamp (30.0)
        node("visitWeight").performTextReplacement("30.0")
        compose.waitForIdle()
        node("visitWeightDecrease").performClick()
        compose.waitForIdle()
        assertEquals("30.0", state.draft?.values?.weight)

        // Upper bound clamp (250.0)
        node("visitWeight").performTextReplacement("250.0")
        compose.waitForIdle()
        node("visitWeightIncrease").performClick()
        compose.waitForIdle()
        assertEquals("250.0", state.draft?.values?.weight)
    }

    @Test
    fun step1_fhrStepper_incrementsDecrementsAndClamps() {
        show(makeState())
        node("stepSegment-1").performClick()
        compose.waitForIdle()

        // Initial 140 -> decrease by 2 -> 138
        node("visitFetalHeartRateDecrease").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("138", state.draft?.values?.fetalHeartRate)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.FETAL_HEART_RATE, "138")))

        // Increase by 2 -> 140
        node("visitFetalHeartRateIncrease").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("140", state.draft?.values?.fetalHeartRate)

        // Lower bound clamp (60)
        node("visitFetalHeartRate").performTextReplacement("60")
        compose.waitForIdle()
        node("visitFetalHeartRateDecrease").performClick()
        compose.waitForIdle()
        assertEquals("60", state.draft?.values?.fetalHeartRate)

        // Upper bound clamp (220)
        node("visitFetalHeartRate").performTextReplacement("220")
        compose.waitForIdle()
        node("visitFetalHeartRateIncrease").performClick()
        compose.waitForIdle()
        assertEquals("220", state.draft?.values?.fetalHeartRate)
    }

    @Test
    fun step1_fhrBaselineStatusWords_noIcons() {
        show(makeState())
        node("stepSegment-1").performClick()
        compose.waitForIdle()

        // 140 -> Normal baseline
        compose.onNodeWithText(str(R.string.visit_fhr_normal_baseline)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("✓", substring = true).assertDoesNotExist()
        compose.onNodeWithText("⚠️", substring = true).assertDoesNotExist()

        // 100 -> Bradycardia
        node("visitFetalHeartRate").performTextReplacement("100")
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.visit_fhr_bradycardia)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("✓", substring = true).assertDoesNotExist()
        compose.onNodeWithText("⚠️", substring = true).assertDoesNotExist()

        // 170 -> Tachycardia
        node("visitFetalHeartRate").performTextReplacement("170")
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.visit_fhr_tachycardia)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("✓", substring = true).assertDoesNotExist()
        compose.onNodeWithText("⚠️", substring = true).assertDoesNotExist()
    }

    // =========================================================================
    // 7. Step 2 (Bedside findings, quick fill, flags without "(Flag)" chip text)
    // =========================================================================

    @Test
    fun step2_fillNormalValues_andBedsideFlaggedStatusWords() {
        val s = makeState(
            values = DraftValues(
                date = "2026-11-14",
                bloodPressure = "110/70",
                systolic = "110",
                diastolic = "70",
                weight = "70.0",
                fetalHeartRate = "140",
                oedema = "Marked",
                fetalMovements = "",
                urineProtein = "",
            ),
        )
        show(s)
        node("stepSegment-2").performClick()
        compose.waitForIdle()

        // Marked oedema initially has "Flagged" status word
        compose.onNodeWithText(str(R.string.visit_finding_flagged)).performScrollTo().assertIsDisplayed()

        // Chips do not carry "(Flag)" text
        compose.onNodeWithText("(Flag)", substring = true).assertDoesNotExist()

        // Click fill normal values
        node("fillNormalValues").performScrollTo().performClick()
        compose.waitForIdle()

        // Oedema remains Marked (not overwritten); movements & urine populated
        assertEquals("Marked", state.draft?.values?.oedema)
        assertEquals("Good", state.draft?.values?.fetalMovements)
        assertEquals("Negative", state.draft?.values?.urineProtein)

        // Tap urineChoice-+ -> "Flagged" appears beside urine protein
        node("urineChoice-+").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("+", state.draft?.values?.urineProtein)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.URINE_PROTEIN, "+")))
    }

    // =========================================================================
    // 8. Step 3 (Review rows sentence case, summary card, linked ultrasound)
    // =========================================================================

    @Test
    fun step3_summaryCard_reviewRowsSentenceCase_andJumpToStep() {
        show(makeState())
        node("stepSegment-3").performClick()
        compose.waitForIdle()

        // Summary Card
        node("visitSummaryCard").performScrollTo().assertIsDisplayed()

        // Sentence-case "Fetal heart rate"
        compose.onNodeWithText(str(R.string.fetal_heart_rate), substring = false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fetal Heart Rate", substring = false).assertDoesNotExist()

        // "Edit" action trailing text on rows
        val editLabel = str(R.string.action_edit)
        val editNodes = compose.onAllNodes(hasText(editLabel), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("Expected at least 7 Edit actions, got ${editNodes.size}", editNodes.size >= 7)

        // Downward arrow icon replaced
        compose.onNode(hasContentDescription("KeyboardArrowDown", substring = true)).assertDoesNotExist()

        // Tap summaryRow-bp jumps back to Step 0
        node("summaryRow-bp").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()
        node("visitBloodPressure").performScrollTo().assertIsDisplayed()

        // Jump back to Step 3 and click saveVisit
        node("stepSegment-3").performScrollTo().performClick()
        compose.waitForIdle()
        node("saveVisit").performClick()
        compose.waitForIdle()
        assertTrue("onSave was not called", onSaveCalled)
    }

    @Test
    fun step3_linkedUltrasound_linkAndUnlink_togglesDisplay() {
        show(makeState())
        node("stepSegment-3").performClick()
        compose.waitForIdle()

        // Initially unlinked: linkScan-scan-s14-1 chip is displayed
        compose.onNodeWithText(str(R.string.link_to_ultrasound)).performScrollTo().assertIsDisplayed()
        node("linkScan-scan-s14-1").performScrollTo().assertIsDisplayed()

        // Tap link chip
        node("linkScan-scan-s14-1").performClick()
        compose.waitForIdle()
        assertEquals("scan-s14-1", state.draft?.values?.linkedUltrasoundId)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.LINKED_ULTRASOUND, "scan-s14-1")))

        // Now linked: unlinkUltrasound button is displayed with scan GA
        compose.onNode(hasText("22+3 wks", substring = true)).performScrollTo().assertIsDisplayed()
        node("unlinkUltrasound").performScrollTo().assertIsDisplayed()

        // Tap unlink
        node("unlinkUltrasound").performClick()
        compose.waitForIdle()
        assertEquals("", state.draft?.values?.linkedUltrasoundId)
        assertTrue(events.contains(AncEvent.DraftChanged(DraftField.LINKED_ULTRASOUND, "")))
        node("linkScan-scan-s14-1").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 9. Validation Errors (Field errors show their localized messages)
    // =========================================================================

    @Test
    fun validationErrors_displayedOnInvalidDraftFields() {
        val s = makeState(
            errors = setOf(
                DraftError.DATE_REQUIRED,
                DraftError.BLOOD_PRESSURE_INVALID,
                DraftError.WEIGHT_INVALID,
            ),
        )
        show(s)

        // Step 0: Date and BP errors displayed
        compose.onNodeWithText(str(R.string.required_date)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.blood_pressure_invalid)).performScrollTo().assertIsDisplayed()

        // Step 1: Weight error displayed
        node("stepSegment-1").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.weight_invalid)).performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 10. Responsive 360dp & Large Font Scale (2.0f)
    // =========================================================================

    @Test
    fun layout360dp_fontScale2_0_noHorizontalOverflowOrClipping() {
        show(makeState(), fontScale = 2.0f)

        // Step 0: bottom bar buttons fit inside 360dp host
        assertTargetInside360("cancelVisit", scroll = false)
        assertTargetInside360("nextStep", scroll = false)

        // Advance to Step 1: Stepper buttons expand without clipping
        node("nextStep").performClick()
        compose.waitForIdle()
        assertTargetInside360("visitWeightDecrease")
        assertTargetInside360("visitWeightIncrease")

        // Advance to Step 3: Review rows fit inside 360dp host
        node("stepSegment-3").performClick()
        compose.waitForIdle()
        assertTargetInside360("summaryRow-fhr")
        assertTargetInside360("saveVisit", scroll = false)
    }

    // =========================================================================
    // 11. Dark Theme & RTL Layout
    // =========================================================================

    @Test
    fun darkTheme_andRtl_layoutCleanWithoutOverlap() {
        show(
            makeState(patient = monaRagab.copy(displayName = "منى رجب")),
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )

        // Step 0 renders cleanly in RTL and Dark Theme
        node("visitEditor").assertIsDisplayed()
        compose.onNodeWithText("منى رجب", useUnmergedTree = true).assertIsDisplayed()
        node("visitDate").performScrollTo().assertIsDisplayed()
        node("visitBloodPressure").performScrollTo().assertIsDisplayed()

        // Advance to Step 3
        node("stepSegment-3").performScrollTo().performClick()
        compose.waitForIdle()
        node("visitSummaryCard").performScrollTo().assertIsDisplayed()
        node("saveVisit").assertIsDisplayed()
    }
}
