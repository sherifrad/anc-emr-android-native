package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.util.DisplayMetrics
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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.LabBatchDraft
import com.sherifradwan.ancnative.state.LabBatchItem
import com.sherifradwan.ancnative.state.LabBatchTab
import com.sherifradwan.ancnative.state.LabDraft
import com.sherifradwan.ancnative.state.LabDraftField
import com.sherifradwan.ancnative.state.LabDraftValues
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.LabBatchScreen
import com.sherifradwan.ancnative.ui.LabEditor
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 16: LabEditor, LabBatchScreen, shared LabPatientBanner, LabMetadataDialog,
 * LabReadoutStrip, and QuickPresetChips collapsible restyle.
 *
 * Mounts through MainActivity + AncTheme inside a 360dp x 640dp host, matching
 * HistoryEditorsRedesignTest and RegistrationRedesignTest style (single setContent per test,
 * shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides,
 * useUnmergedTree where label text is cleared by clearAndSetSemantics, performScrollTo
 * before assertIsDisplayed, select nodes by testTag and not by ambiguous text, >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class LabEditorRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val monaRagab = DemoPatient(
        id = "patient-s16",
        caseId = "ANC-S16-001",
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
            patientId = "patient-s16",
            lmpDate = "2026-05-07",
            ultrasoundEdd = "2027-02-11",
            agreedEdd = "2027-02-11",
            datingMethod = "Ultrasound",
            notes = null,
            revision = 1,
        ),
        visits = emptyList(),
        labInvestigations = emptyList(),
    )

    private val events = mutableListOf<AncEvent>()
    private var state by mutableStateOf(AncState(patients = listOf(monaRagab)))

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun onEvent(event: AncEvent) {
        events += event
        state = AncReducer.reduce(state, event)
    }

    private fun labEditorState(
        patient: DemoPatient = monaRagab,
        values: LabDraftValues = LabDraftValues(date = "2026-11-14", labName = "Al Borg"),
        errors: Set<ClinicalValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
        labId: String = "lab-s16-1",
        isNew: Boolean = true,
    ): AncState {
        val session = "session-lab-s16"
        val id = patient.id
        return AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.LabEditor(id, labId, isNew),
            labDraft = LabDraft(
                editorSessionId = session,
                patientId = id,
                labId = labId,
                basePatientRevision = patient.revision,
                baseLabRevision = if (isNew) null else 1L,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    private fun labBatchState(
        patient: DemoPatient = monaRagab,
        date: String = "2026-11-14",
        labName: String? = "Al Borg",
        items: List<LabBatchItem> = emptyList(),
        cursor: Int = 0,
        tab: LabBatchTab = LabBatchTab.ADD,
        errors: Set<ClinicalValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
    ): AncState {
        val session = "session-labbatch-s16"
        val id = patient.id
        return AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.LabBatchEditor(id),
            labBatchDraft = LabBatchDraft(
                patientId = id,
                editorSessionId = session,
                basePatientRevision = patient.revision,
                date = date,
                labName = labName,
                items = items,
                cursor = cursor,
                tab = tab,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    /** Single setContent per test: hosts LabEditor inside a 360dp x 640dp container. */
    private fun showLabEditor(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        val route = state.route as AncRoute.LabEditor
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            LabEditor(
                                state = state,
                                route = route,
                                onEvent = ::onEvent,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Single setContent per test: hosts LabBatchScreen inside a 360dp x 640dp container. */
    private fun showLabBatch(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        val route = state.route as AncRoute.LabBatchEditor
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            LabBatchScreen(
                                state = state,
                                route = route,
                                onEvent = ::onEvent,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        restoreResources()
    }

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

    // ---- measuring and query helpers ----

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()

    private fun assertMinTouchTarget(tag: String, scroll: Boolean = true) {
        if (scroll) {
            runCatching { node(tag).performScrollTo() }
        }
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertTargetInside360(tag: String, scroll: Boolean = true) {
        if (scroll) {
            runCatching { node(tag).performScrollTo() }
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

    // =========================================================================
    // 1. Typography & Legacy Glyphs Removal
    // =========================================================================

    @Test
    fun labEditor_typography_noTextBelow12sp_andNoLegacyGlyphsOrLockFooter() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        node("labEditor").assertIsDisplayed()

        // Open details toggle to expose test name and presets
        node("labEditDetailsToggle").performScrollTo().performClick()
        compose.waitForIdle()

        // Open add tests toggle to expose catalog category filters
        node("labAddTestsToggle").performScrollTo().performClick()
        compose.waitForIdle()

        assertNoTextBelow12sp("labEditor")

        // Assert legacy glyphs, lock footer, and obsolete text never appear
        compose.onNodeWithText("AES-256", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Normal Risk", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("27w 2d", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("✓", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▲", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▼", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("◀", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▶", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▾", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("+ +", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun labBatchScreen_typography_noTextBelow12sp_addTab() {
        showLabBatch(labBatchState(tab = LabBatchTab.ADD))

        node("labBatchScreen").assertIsDisplayed()
        assertNoTextBelow12sp("labBatchScreen")

        compose.onNodeWithText("AES-256", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Normal Risk", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("27w 2d", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("✓", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▲", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▼", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("◀", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▶", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▾", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun labBatchScreen_typography_noTextBelow12sp_enterTab() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Protein", "Urinalysis", "", "Trace", "", isAbnormal = true),
        )
        showLabBatch(labBatchState(items = items, tab = LabBatchTab.ENTER))

        node("labBatchScreen").assertIsDisplayed()
        assertNoTextBelow12sp("labBatchScreen")

        compose.onNodeWithText("AES-256", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("✓", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▲", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▼", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("◀", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▶", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun labBatchScreen_typography_noTextBelow12sp_reviewTab() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Platelets", "Hematology", "x10^9/L", "220", "150-450"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Glucose", "Urinalysis", "", "", ""),
        )
        showLabBatch(labBatchState(items = items, tab = LabBatchTab.REVIEW))

        node("labBatchScreen").assertIsDisplayed()
        assertNoTextBelow12sp("labBatchScreen")

        compose.onNodeWithText("AES-256", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("✓", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▲", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("▼", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // =========================================================================
    // 2. Patient Banner & GA & RTL Alignment
    // =========================================================================

    @Test
    fun patientBanner_englishName_leftAligned_ga_parity_bloodGroup() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(patient = monaRagab, values = values))

        // English name displayed left-aligned
        compose.onNodeWithText("Mona Ragab", useUnmergedTree = true).assertIsDisplayed()
        val layout = layoutOf("name", hasText("Mona Ragab"))
        assertEquals(TextAlign.Left, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Ltr, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, layout.getParagraphDirection(0))

        // BandChips for GA, Parity, Blood Group
        compose.onAllNodesWithText("27+2 wks", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        compose.onNode(hasText("G4 P1 A2", substring = true), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("A+", substring = true), useUnmergedTree = true).assertIsDisplayed()

        // Low Risk does NOT render a risk pill
        compose.onNodeWithText("Normal Risk", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Low Risk", substring = true, useUnmergedTree = true).assertDoesNotExist()

        // BatchInfoBar shows date, GA, labName, and Change action without dropdown icon
        compose.onNodeWithText("Al Borg", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.lab_batch_change), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun patientBanner_arabicName_rightAligned() {
        val arabicPatient = monaRagab.copy(displayName = "منى رجب")
        val values = LabDraftValues(date = "2026-11-14", labName = "Al Borg")
        showLabEditor(labEditorState(patient = arabicPatient, values = values))

        compose.onNodeWithText("منى رجب", useUnmergedTree = true).assertIsDisplayed()
        val layout = layoutOf("name", hasText("منى رجب"))
        assertEquals(TextAlign.Right, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))
    }

    @Test
    fun patientBanner_highRisk_andRhNegative_tagAndTone() {
        val highRiskRhNeg = monaRagab.copy(
            riskLevel = "High Risk",
            bloodGroupText = "A-",
        )
        val values = LabDraftValues(date = "2026-11-14", labName = "Al Borg")
        showLabEditor(labEditorState(patient = highRiskRhNeg, values = values))

        // High Risk StatusTag displayed
        compose.onNodeWithText(str(R.string.high_risk), substring = true, useUnmergedTree = true).assertIsDisplayed()
        // Blood group A- displayed
        compose.onNode(hasText("A-", substring = true), useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // 3. Touch Targets (>= 48dp) & 360dp Ergonomics
    // =========================================================================

    @Test
    fun labEditor_touchTargets_atLeast48dp_inside360dpHost() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        // Footer buttons (bottom bar)
        assertTargetInside360("cancelLab", scroll = false)
        assertTargetInside360("saveLab", scroll = false)
        assertTargetInside360("addAnotherLab", scroll = false)

        // Metadata change card (>= 56dp tall)
        assertTargetInside360("labBatchChange")
        assertTrue("labBatchChange height < 56dp", bounds("labBatchChange").height >= 55.99.dp)

        // Details toggle (>= 48dp)
        assertTargetInside360("labEditDetailsToggle")

        // Flag toggle row (>= 56dp tall) and Switch
        assertTargetInside360("labFlagToggle")
        assertTrue("labFlagToggle height < 56dp", bounds("labFlagToggle").height >= 55.99.dp)
        node("labAbnormal").assertIsDisplayed()

        // Add tests toggle (>= 56dp tall)
        assertTargetInside360("labAddTestsToggle")
        assertTrue("labAddTestsToggle height < 56dp", bounds("labAddTestsToggle").height >= 55.99.dp)

        // Open add tests to inspect category chips
        node("labAddTestsToggle").performScrollTo().performClick()
        compose.waitForIdle()

        assertMinTouchTarget("allLabTestsChip")
        assertMinTouchTarget("labCategoryChoice-0")
    }

    @Test
    fun labEditor_dipstickPresets_touchTargets_atLeast48dp() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Urinalysis",
            testName = "Urine Protein",
            result = "Normal",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        for (i in 0..4) {
            assertMinTouchTarget("labResultPreset-$i")
        }
    }

    @Test
    fun labEditor_metadataDialog_touchTargets_atLeast48dp() {
        // With blank date, metadata dialog opens on initial render
        val values = LabDraftValues(date = "", labName = "")
        showLabEditor(labEditorState(values = values))

        node("labBatchDone").assertIsDisplayed()
        assertMinTouchTarget("labBatchDone", scroll = false)
        assertMinTouchTarget("labDateTodayButton", scroll = false)
        assertMinTouchTarget("labDateCalendarButton", scroll = false)
        assertMinTouchTarget("labNamePreset-0", scroll = false)
    }

    @Test
    fun labBatchScreen_touchTargets_headerTabsAndPanels_atLeast48dp() {
        showLabBatch(labBatchState(tab = LabBatchTab.ADD))

        assertTargetInside360("labBatchBack")
        assertTargetInside360("labBatchCancel", scroll = false)
        assertTargetInside360("labBatchSave", scroll = false)

        assertTargetInside360("labBatchTab-ADD", scroll = false)
        assertTargetInside360("labBatchTab-ENTER", scroll = false)
        assertTargetInside360("labBatchTab-REVIEW", scroll = false)

        // Panel actions: OutlinedButton for Choose
        val chooseButtons = compose.onAllNodes(hasText(str(R.string.lab_batch_choose)) and hasClickAction())
        val chooseCount = chooseButtons.fetchSemanticsNodes().size
        assertTrue("No choose buttons found", chooseCount > 0)
        val chooseBounds = chooseButtons[0].getUnclippedBoundsInRoot()
        assertTrue("Choose button width < 48dp (${chooseBounds.width})", chooseBounds.width >= 47.99.dp)
        assertTrue("Choose button height < 48dp (${chooseBounds.height})", chooseBounds.height >= 47.99.dp)

        assertMinTouchTarget("labBatchAdd-group_cbc")
        assertMinTouchTarget("labBatchOften-0")
        assertMinTouchTarget("labBatchOften-1")
        assertMinTouchTarget("labBatchOften-2")
        assertMinTouchTarget("labBatchAddOtherChip")
    }

    @Test
    fun labBatchScreen_touchTargets_enterAndReviewTabs_atLeast48dp() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Protein", "Urinalysis", "", "Trace", "", isAbnormal = true),
        )
        showLabBatch(labBatchState(items = items, cursor = 0, tab = LabBatchTab.ENTER))

        // labBatchDeleteItem: raised from 32dp to >= 48dp
        val delBounds = bounds("labBatchDeleteItem")
        assertTrue("labBatchDeleteItem width < 48dp (${delBounds.width})", delBounds.width >= 47.99.dp)
        assertTrue("labBatchDeleteItem height < 48dp (${delBounds.height})", delBounds.height >= 47.99.dp)

        val flagRow = compose.onNode(hasAnyDescendant(hasTestTag("labBatchFlag")) and hasClickAction(), useUnmergedTree = true)
        runCatching { flagRow.performScrollTo() }
        val flagBounds = flagRow.getUnclippedBoundsInRoot()
        assertTrue("labBatchFlag row width < 48dp (${flagBounds.width})", flagBounds.width >= 47.99.dp)
        assertTrue("labBatchFlag row height < 48dp (${flagBounds.height})", flagBounds.height >= 47.99.dp)
        node("labBatchFlag").assertIsDisplayed()

        assertMinTouchTarget("labBatchPrev")
        assertMinTouchTarget("labBatchNext")
        assertMinTouchTarget("labBatchAddInline")

        // Switch to Urine Protein (cursor 1) to inspect QualitativeChoiceButton
        node("labBatchNext").performScrollTo().performClick()
        compose.waitForIdle()

        val choiceButton = compose.onNode(hasText("Normal") and hasClickAction())
        choiceButton.performScrollTo().assertIsDisplayed()
        val normalBounds = choiceButton.getUnclippedBoundsInRoot()
        assertTrue("QualitativeChoiceButton height < 52dp (${normalBounds.height})", normalBounds.height >= 51.99.dp)

        // Switch to Review tab
        node("labBatchTab-REVIEW").performScrollTo().performClick()
        compose.waitForIdle()

        // Edit button and mark normal
        val editTag = "labBatchEdit-${items[0].labId}"
        assertMinTouchTarget(editTag)
    }

    // =========================================================================
    // 4. Font Scaling (1.3f and 2.0f) Ergonomics & No Clipping
    // =========================================================================

    @Test
    fun labEditor_fontScale2_0_verticalStacking_noHorizontalOverflow() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values), fontScale = 2.0f)

        node("labEditor").assertIsDisplayed()
        node("labUnit").performScrollTo().assertIsDisplayed()
        node("labRefRange").performScrollTo().assertIsDisplayed()

        val unitBounds = bounds("labUnit")
        val refBounds = bounds("labRefRange")
        val host = bounds(HOST_TAG)

        // When fontScale > 1.3f (narrowStacked), unit and refRange stack vertically
        assertTrue(
            "Expected labUnit above labRefRange in stacked layout: unit.bottom=${unitBounds.bottom}, ref.top=${refBounds.top}",
            unitBounds.bottom <= refBounds.top + 1.dp,
        )
        assertTrue("labUnit runs outside host: $unitBounds", unitBounds.right <= host.right + 1.dp)
        assertTrue("labRefRange runs outside host: $refBounds", refBounds.right <= host.right + 1.dp)

        // Bottom bar buttons wrap with maxLines = 2 without horizontal overflow
        val cancelBounds = bounds("cancelLab")
        val saveBounds = bounds("saveLab")
        val addAnotherBounds = bounds("addAnotherLab")

        assertTrue("cancelLab starts outside host: $cancelBounds", cancelBounds.left >= host.left - 1.dp)
        assertTrue("saveLab ends outside host: $saveBounds", saveBounds.right <= host.right + 1.dp)
        assertTrue("addAnotherLab ends outside host: $addAnotherBounds", addAnotherBounds.right <= host.right + 1.dp)
    }

    @Test
    fun labBatchScreen_fontScale2_0_flowRowTabs_noHorizontalOverflow() {
        showLabBatch(labBatchState(tab = LabBatchTab.ADD), fontScale = 2.0f)

        node("labBatchScreen").assertIsDisplayed()
        val host = bounds(HOST_TAG)

        val tabAdd = bounds("labBatchTab-ADD")
        val tabEnter = bounds("labBatchTab-ENTER")
        val tabReview = bounds("labBatchTab-REVIEW")

        // Under fontScale > 1.3f, 3-tab bar uses FlowRow so tabs wrap cleanly within host
        assertTrue("tabAdd outside host: $tabAdd", tabAdd.left >= host.left - 1.dp && tabAdd.right <= host.right + 1.dp)
        assertTrue("tabEnter outside host: $tabEnter", tabEnter.left >= host.left - 1.dp && tabEnter.right <= host.right + 1.dp)
        assertTrue("tabReview outside host: $tabReview", tabReview.left >= host.left - 1.dp && tabReview.right <= host.right + 1.dp)

        // Bottom bar buttons
        val cancelBounds = bounds("labBatchCancel")
        val saveBounds = bounds("labBatchSave")
        assertTrue("labBatchCancel outside host: $cancelBounds", cancelBounds.left >= host.left - 1.dp)
        assertTrue("labBatchSave outside host: $saveBounds", saveBounds.right <= host.right + 1.dp)
    }

    // =========================================================================
    // 5. LabEditor Behaviour Parity & Flow
    // =========================================================================

    @Test
    fun labEditor_quantitativeAutoFlag_normalLowHigh() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            isAbnormal = false,
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        // Initial 11.2 within 11.0 - 15.0: readout shows 11.2 and flag shows Normal
        node("labStoryValue").assertIsDisplayed().assertTextContains("11.2")
        node("labResultFlag").assertIsDisplayed()
        compose.onNode(hasText(str(R.string.lab_flag_normal)) and hasAnyAncestor(hasTestTag("labResultFlag"))).assertIsDisplayed()
        node("labAbnormal").assertIsOff()

        // Enter low result 9.8: auto-flag updates to Low and abnormal switch turns ON
        node("labResult").performScrollTo().performTextReplacement("9.8")
        compose.waitForIdle()

        node("labStoryValue").assertTextContains("9.8")
        node("labResultFlag").assertIsDisplayed()
        compose.onNode(hasText(str(R.string.lab_flag_low)) and hasAnyAncestor(hasTestTag("labResultFlag"))).assertIsDisplayed()
        node("labAbnormal").assertIsOn()
        assertTrue(events.any { it is AncEvent.LabAbnormalChanged && it.isAbnormal })

        // Enter high result 16.5: auto-flag updates to High and abnormal switch remains ON
        node("labResult").performScrollTo().performTextReplacement("16.5")
        compose.waitForIdle()

        node("labStoryValue").assertTextContains("16.5")
        node("labResultFlag").assertIsDisplayed()
        compose.onNode(hasText(str(R.string.lab_flag_high)) and hasAnyAncestor(hasTestTag("labResultFlag"))).assertIsDisplayed()
        node("labAbnormal").assertIsOn()
    }

    @Test
    fun labEditor_dipstickChoices_updatesDraftAndAbnormal() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Urinalysis",
            testName = "Urine Protein",
            result = "Normal",
            isAbnormal = false,
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        // Tap Trace (preset 1) -> result becomes Trace, abnormal becomes true
        node("labResultPreset-1").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("Trace", state.labDraft?.values?.result)
        assertEquals(true, state.labDraft?.values?.isAbnormal)
        assertTrue(events.contains(AncEvent.LabDraftChanged(LabDraftField.RESULT, "Trace")))
        assertTrue(events.contains(AncEvent.LabAbnormalChanged(true)))

        // Tap Normal (preset 0) -> result becomes Normal, abnormal becomes false
        node("labResultPreset-0").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("Normal", state.labDraft?.values?.result)
        assertEquals(false, state.labDraft?.values?.isAbnormal)
        assertTrue(events.contains(AncEvent.LabAbnormalChanged(false)))
    }

    @Test
    fun labEditor_presetCollapse_collapsesAfterFiveUntilToggled() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "",
            testName = "",
            result = "",
            labName = "Al Borg",
        )
        // With blank testName, testSelectorOpen starts true
        showLabEditor(labEditorState(values = values))

        node("labPresetToggle").performScrollTo().assertIsDisplayed()
        compose.onNode(
            hasText(str(R.string.show_all_count, 28)) and (hasTestTag("labPresetToggle") or hasAnyAncestor(hasTestTag("labPresetToggle"))),
            useUnmergedTree = true,
        ).assertIsDisplayed()

        // First 5 chips visible, 6th chip not in tree
        node("labPreset-0").assertIsDisplayed()
        node("labPreset-4").assertIsDisplayed()
        node("labPreset-5").assertDoesNotExist()

        // Tap toggle to expand
        node("labPresetToggle").performScrollTo().performClick()
        compose.waitForIdle()

        node("labPreset-5").performScrollTo().assertIsDisplayed()
        node("labPresetToggle").performScrollTo().assertIsDisplayed()
        compose.onNode(
            hasText(str(R.string.show_fewer)) and (hasTestTag("labPresetToggle") or hasAnyAncestor(hasTestTag("labPresetToggle"))),
            useUnmergedTree = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun labEditor_actionDispatches_andValidationErrors() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "12.0",
            unit = "g/dL",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        // Save dispatches AncEvent.SubmitLab
        node("saveLab").performClick()
        compose.waitForIdle()
        assertTrue(events.contains(AncEvent.SubmitLab))

        // Add another dispatches AncEvent.SubmitLabAndAddAnother
        node("addAnotherLab").performClick()
        compose.waitForIdle()
        assertTrue(events.contains(AncEvent.SubmitLabAndAddAnother))

        // Cancel dispatches AncEvent.CancelEditor
        node("cancelLab").performClick()
        compose.waitForIdle()
        assertTrue(events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun labEditor_saveSpinner_whenPendingSave() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "12.0",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values, savePhase = SavePhase.PENDING))

        node("labSaveSpinner").assertIsDisplayed()
        node("saveLab").assertIsNotEnabled()
    }

    @Test
    fun labEditor_validationErrorCard_displayedWhenErrorsPresent() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "",
            result = "",
            labName = "Al Borg",
        )
        val errors = setOf(ClinicalValidationError.REQUIRED_NAME, ClinicalValidationError.REQUIRED_RESULT)
        showLabEditor(labEditorState(values = values, errors = errors))

        node("editorError").assertIsDisplayed()
    }

    @Test
    fun labEditor_saveToLabCodex_shownForNewCustomTest() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Custom Platelet Aggregation Test",
            result = "Normal",
            unit = "%",
            referenceRange = "70-100",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        // Open add tests card
        node("labAddTestsToggle").performScrollTo().performClick()
        compose.waitForIdle()

        node("saveToLabCodexBanner").performScrollTo().assertIsDisplayed()
        node("saveToLabCodexButton").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 6. LabBatchScreen Behaviour Parity & Flow
    // =========================================================================

    @Test
    fun labBatchScreen_metadataDialog_dispatchesDateAndLabName() {
        showLabBatch(labBatchState(date = "2026-11-14", labName = "Al Borg", tab = LabBatchTab.ADD))

        // Tap Change to open metadata dialog
        compose.onNodeWithText(str(R.string.lab_batch_change), useUnmergedTree = true).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(str(R.string.lab_batch_edit_metadata), useUnmergedTree = true).assertIsDisplayed()

        // Click Today button
        node("labBatchDateTodayButton").performClick()
        compose.waitForIdle()
        assertTrue(events.any { it is AncEvent.LabBatchDateChanged })

        // Click Lab preset chip
        node("labBatchLabPreset-0").performClick()
        compose.waitForIdle()
        assertTrue(events.any { it is AncEvent.LabBatchLabNameChanged })

        // Click Done to dismiss
        compose.onNodeWithText(str(R.string.done), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.lab_batch_edit_metadata), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun labBatchScreen_validationErrorCard_displayedWhenErrorsPresent() {
        val errors = setOf(ClinicalValidationError.REQUIRED_DATE)
        showLabBatch(labBatchState(date = "", errors = errors, tab = LabBatchTab.ADD))

        node("editorError").assertIsDisplayed()
    }

    @Test
    fun labBatchScreen_addTab_searchNoMatches_andOftenAdded() {
        showLabBatch(labBatchState(tab = LabBatchTab.ADD))

        // Search for nonexistent test shows honest empty message
        node("labBatchSearch").performScrollTo().performTextInput("xyz999nomatch")
        compose.waitForIdle()

        compose.onNodeWithText(str(R.string.lab_batch_no_search_matches), useUnmergedTree = true).assertIsDisplayed()

        // Clear search
        node("labBatchSearch").performTextReplacement("")
        compose.waitForIdle()

        // Click Often added Ferritin (maps to catalog item "Serum Ferritin")
        node("labBatchOften-0").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(events.any { it is AncEvent.LabBatchItemsAdded && it.items.any { item -> item.testName == "Serum Ferritin" } })
    }

    @Test
    fun labBatchScreen_choosePanelDialog_elementsCountAndDeselect() {
        showLabBatch(labBatchState(tab = LabBatchTab.ADD))

        // Click Choose on CBC panel
        compose.onAllNodesWithText(str(R.string.lab_batch_choose), useUnmergedTree = true)[0].performClick()
        compose.waitForIdle()

        compose.onNodeWithText(str(R.string.lab_batch_choose_tests), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("CBC · 6 elements", useUnmergedTree = true).assertIsDisplayed()

        // Deselect MCV
        compose.onNodeWithText("MCV", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        // Button shows "Add 5 selected"
        compose.onNodeWithText("Add 5 selected", useUnmergedTree = true).assertIsDisplayed().performClick()
        compose.waitForIdle()

        assertTrue(events.any { it is AncEvent.LabBatchItemsAdded && it.items.size == 5 && it.items.none { item -> item.testName == "MCV" } })
    }

    @Test
    fun labBatchScreen_enterAndReviewFlow_markEmptyNormal() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Protein", "Urinalysis", "", "", ""),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Glucose", "Urinalysis", "", "", ""),
        )
        showLabBatch(labBatchState(items = items, cursor = 0, tab = LabBatchTab.ENTER))

        // Enter result and range on Hb
        node("labBatchResult").performTextInput("11.5")
        node("labBatchRange").performTextInput("11.0-15.0")
        compose.waitForIdle()

        assertTrue(events.any { it is AncEvent.LabBatchResultChanged })
        assertTrue(events.any { it is AncEvent.LabBatchRangeChanged })

        // Switch to Review tab
        node("labBatchTab-REVIEW").performClick()
        compose.waitForIdle()

        assertTrue(events.contains(AncEvent.LabBatchTabSelected(LabBatchTab.REVIEW)))

        // Mark empty normal card
        node("labBatchMarkNormal").performScrollTo().assertIsDisplayed()
        node("labBatchMarkNormal").performClick()
        compose.waitForIdle()

        assertTrue(events.any { it is AncEvent.LabBatchResultChanged && it.value == "Negative" })

        // Save batch
        node("labBatchSave").performClick()
        compose.waitForIdle()
        assertTrue(events.contains(AncEvent.SubmitLabBatch))

        // Cancel batch
        node("labBatchCancel").performClick()
        compose.waitForIdle()
        assertTrue(events.contains(AncEvent.CancelLabBatch))
    }

    // =========================================================================
    // 7. Dark Theme & RTL Layout
    // =========================================================================

    @Test
    fun labEditor_darkTheme_and_rtl_rendersCleanly() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            labName = "Al Borg",
        )
        showLabEditor(
            labEditorState(values = values),
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )

        node("labEditor").assertIsDisplayed()
        val host = bounds(HOST_TAG)
        val card = bounds("labTestCard")

        assertTrue("labTestCard runs past host left: $card", card.left >= host.left - 1.dp)
        assertTrue("labTestCard runs past host right: $card", card.right <= host.right + 1.dp)
    }

    @Test
    fun labBatchScreen_darkTheme_and_rtl_rendersCleanly() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Protein", "Urinalysis", "", "Normal", ""),
        )
        showLabBatch(
            labBatchState(items = items, tab = LabBatchTab.REVIEW),
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )

        node("labBatchScreen").assertIsDisplayed()
        val host = bounds(HOST_TAG)
        val reviewBounds = bounds("labBatchReview")

        assertTrue("labBatchReview runs past host left: $reviewBounds", reviewBounds.left >= host.left - 1.dp)
        assertTrue("labBatchReview runs past host right: $reviewBounds", reviewBounds.right <= host.right + 1.dp)
    }

    // =========================================================================
    // 8. Existing Tags Still Present
    // =========================================================================

    @Test
    fun existingTags_labEditor_allPresent() {
        val values = LabDraftValues(
            date = "2026-11-14",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.2",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            labName = "Al Borg",
        )
        showLabEditor(labEditorState(values = values))

        node("labEditor").assertIsDisplayed()
        node("labBatchChange").performScrollTo().assertIsDisplayed()
        node("cancelLab").assertIsDisplayed()
        node("saveLab").assertIsDisplayed()
        node("addAnotherLab").assertIsDisplayed()
        node("labTestCard").performScrollTo().assertIsDisplayed()
        node("labTestTitle").performScrollTo().assertIsDisplayed()
        node("labEditDetailsToggle").performScrollTo().assertIsDisplayed()
        node("labStoryDisplay").performScrollTo().assertIsDisplayed()
        node("labStoryValue").performScrollTo().assertIsDisplayed()
        node("labResultFlag").performScrollTo().assertIsDisplayed()
        node("labResult").performScrollTo().assertIsDisplayed()
        node("labUnit").performScrollTo().assertIsDisplayed()
        node("labRefRange").performScrollTo().assertIsDisplayed()
        node("labFlagToggle").performScrollTo().assertIsDisplayed()
        node("labAbnormal").performScrollTo().assertIsDisplayed()
        node("labNoteToggle").performScrollTo().assertIsDisplayed()
        node("labNotes").performScrollTo().assertIsDisplayed()
        node("labAddTestsCard").performScrollTo().assertIsDisplayed()
        node("labAddTestsToggle").performScrollTo().assertIsDisplayed()

        // Open details toggle to verify labName and presets (and category chips)
        node("labEditDetailsToggle").performScrollTo().performClick()
        compose.waitForIdle()

        node("labName").performScrollTo().assertIsDisplayed()
        node("labPreset-0").performScrollTo().assertIsDisplayed()
        node("allLabTestsChip").performScrollTo().assertIsDisplayed()
        node("labCategoryChoice-0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun existingTags_labBatchScreen_allPresent() {
        val items = listOf(
            LabBatchItem(UUID.randomUUID().toString(), "Hemoglobin", "Hematology", "g/dL", "10.8", "11.0-15.0"),
            LabBatchItem(UUID.randomUUID().toString(), "Urine Protein", "Urinalysis", "", "Normal", ""),
        )
        showLabBatch(labBatchState(items = items, tab = LabBatchTab.ENTER))

        node("labBatchScreen").assertIsDisplayed()
        node("labBatchBack").assertIsDisplayed()
        node("labBatchCancel").assertIsDisplayed()
        node("labBatchSave").assertIsDisplayed()
        node("labBatchTab-ADD").assertIsDisplayed()
        node("labBatchTab-ENTER").assertIsDisplayed()
        node("labBatchTab-REVIEW").assertIsDisplayed()
        node("labBatchEnter").performScrollTo().assertIsDisplayed()
        node("labBatchDeleteItem").performScrollTo().assertIsDisplayed()
        node("labBatchResult").performScrollTo().assertIsDisplayed()
        node("labBatchRange").performScrollTo().assertIsDisplayed()
        node("labBatchFlag").performScrollTo().assertIsDisplayed()
        node("labBatchNote").performScrollTo().assertIsDisplayed()
        node("labBatchPrev").performScrollTo().assertIsDisplayed()
        node("labBatchNext").performScrollTo().assertIsDisplayed()
        node("labBatchAddInline").performScrollTo().assertIsDisplayed()

        // Switch to Review tab
        node("labBatchTab-REVIEW").performScrollTo().performClick()
        compose.waitForIdle()

        node("labBatchReview").performScrollTo().assertIsDisplayed()
        node("labBatchEdit-${items[0].labId}").performScrollTo().assertIsDisplayed()

        // Switch to Add tab
        node("labBatchTab-ADD").performScrollTo().performClick()
        compose.waitForIdle()

        node("labBatchAddList").performScrollTo().assertIsDisplayed()
        node("labBatchAdd-group_cbc").performScrollTo().assertIsDisplayed()
        node("labBatchSearch").performScrollTo().assertIsDisplayed()
        node("labBatchOften-0").performScrollTo().assertIsDisplayed()
        node("labBatchAddOtherChip").performScrollTo().assertIsDisplayed()
    }

    companion object {
        private const val HOST_TAG = "test_host"
    }
}
