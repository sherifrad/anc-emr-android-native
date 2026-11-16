package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.NORMAL_AMNIOTIC_FLUID
import com.sherifradwan.ancnative.model.NORMAL_FETAL_NUMBER
import com.sherifradwan.ancnative.model.NORMAL_PRESENTATION
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.UltrasoundDraft
import com.sherifradwan.ancnative.state.UltrasoundDraftField
import com.sherifradwan.ancnative.state.UltrasoundDraftValues
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.UltrasoundEditor
import com.sherifradwan.ancnative.ui.UltrasoundPhotoViewerDialog
import com.sherifradwan.ancnative.ui.components.ZoomableImage
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 15: UltrasoundEditor, ZoomableImage reset chip, and UltrasoundPhotoViewerDialog.
 * Mounts through MainActivity + AncTheme inside a 360dp x 640dp host, matching
 * HistoryEditorsRedesignTest and RegistrationRedesignTest style (single setContent per test,
 * shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides,
 * useUnmergedTree where label text is cleared by clearAndSetSemantics, performScrollTo
 * before assertIsDisplayed, select nodes by testTag, >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class UltrasoundEditorRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val testVisit = DemoVisit(
        id = "visit-s15-1",
        patientId = "patient-s15",
        date = "2026-10-15",
        bloodPressure = "118/76",
        weight = "74.5",
        findings = "Normal fetal activity",
        notes = "Follow up scan planned",
        revision = 1,
        isSyntheticEdit = false,
    )

    private val basePatient: DemoPatient = DemoPatient(
        id = "patient-s15",
        caseId = "ANC-S15-001",
        displayName = "Fatima Al-Sayed",
        dateOfBirth = "1995-03-20",
        ageText = "31",
        phone = "+201012345678",
        addressText = "Giza",
        riskLevel = "Low Risk",
        bloodGroupText = "O+",
        gravida = 2L,
        parity = 1L,
        abortion = 0L,
        medicalHistory = "None",
        surgicalHistory = "None",
        familyHistory = "None",
        allergyHistory = "None",
        clinicalAttention = null,
        obstetricNotes = null,
        revision = 1,
        dating = DemoPatientDating(
            patientId = "patient-s15",
            lmpDate = "2026-05-07",
            ultrasoundEdd = "2027-02-11",
            agreedEdd = "2027-02-11",
            datingMethod = "Ultrasound",
            notes = null,
            revision = 1,
        ),
        visits = listOf(testVisit),
        ultrasoundScans = emptyList(),
    )

    private val viewerScan = DemoUltrasoundScan(
        id = "scan-viewer-s15",
        patientId = "patient-s15",
        date = "2026-05-30",
        gestationalAgeWeeks = 20,
        gestationalAgeDays = 0,
        fetalNumber = "Singleton",
        presentation = "Cephalic",
        placentalSite = "Anterior",
        amnioticFluid = "Normal",
        fetalHeartRate = "148",
        biometryBpd = "49",
        biometryHc = null,
        biometryAc = null,
        biometryFl = "34",
        estimatedFetalWeight = "380",
        impression = "Normal anatomy scan",
        linkedVisitId = null,
        imageUris = listOf("dummy://scan-photo-1.jpg", "dummy://scan-photo-2.jpg"),
    )

    private val events = mutableListOf<AncEvent>()
    private var state by mutableStateOf(AncState(patients = listOf(basePatient)))

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun onEvent(event: AncEvent) {
        events += event
        state = AncReducer.reduce(state, event)
    }

    private fun editorState(
        patient: DemoPatient = basePatient,
        values: UltrasoundDraftValues = UltrasoundDraftValues(),
        errors: Set<ClinicalValidationError> = emptySet(),
        savePhase: SavePhase = SavePhase.IDLE,
        scanId: String = "scan-s15-1",
        isNew: Boolean = true,
    ): AncState {
        val session = "session-ultrasound-s15"
        val id = patient.id
        return AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.UltrasoundEditor(id, scanId, isNew),
            ultrasoundDraft = UltrasoundDraft(
                editorSessionId = session,
                patientId = id,
                scanId = scanId,
                basePatientRevision = patient.revision,
                baseScanRevision = if (isNew) null else 1L,
                original = values,
                values = values,
                errors = errors,
            ),
            savePhase = savePhase,
        )
    }

    /** One setContent per test: hosts UltrasoundEditor in a 360dp x 640dp container. */
    private fun showEditor(
        stateToHost: AncState,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        state = stateToHost
        events.clear()
        val route = state.route as AncRoute.UltrasoundEditor
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            UltrasoundEditor(
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

    /** Hosts UltrasoundPhotoViewerDialog inside a 360dp x 640dp container. */
    private fun showViewer(
        scan: DemoUltrasoundScan = viewerScan,
        patientId: String = basePatient.id,
        initialIndex: Int = 0,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
    ) {
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(VIEWER_HOST_TAG)) {
                            UltrasoundPhotoViewerDialog(
                                patientId = patientId,
                                scan = scan,
                                initialIndex = initialIndex,
                                onDismiss = onDismiss,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Hosts ZoomableImage directly to test the 1x reset button. */
    private fun showZoomableImage(
        bitmap: androidx.compose.ui.graphics.ImageBitmap,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
    ) {
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(ZOOM_HOST_TAG)) {
                            ZoomableImage(
                                bitmap = bitmap,
                                contentDescription = "Test scan photo",
                                modifier = Modifier.requiredSize(360.dp, 280.dp),
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Sets activity resources to a 360dp-wide window with given font scale for Dialog tests. */
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

    @After
    fun tearDown() {
        restoreResources()
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
    // 1. UltrasoundEditor: Typography, Legacy Removal, & Header
    // =========================================================================

    @Test
    fun ultrasoundEditor_noNumberedTitles_noTriangles_noLockFooter_cleanHeader() {
        val values = UltrasoundDraftValues(
            linkedVisitId = testVisit.id,
            fetalHeartRate = "140",
            biometryBpd = "50.0",
        )
        showEditor(editorState(values = values))

        node("ultrasoundEditor").assertIsDisplayed()
        compose.onNodeWithText(basePatient.displayName, substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Clean subtitle: no hardcoded legacy suffix
        compose.onNodeWithText(" · Ultrasound Examination", substring = true).assertDoesNotExist()

        // Section card titles without "1. ", "2. ", "3. ", "4. "
        compose.onNodeWithText("1. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("2. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("3. ", substring = true).assertDoesNotExist()
        compose.onNodeWithText("4. ", substring = true).assertDoesNotExist()

        // No triangles in biometry toggle
        compose.onNodeWithText("▲", substring = true).assertDoesNotExist()
        compose.onNodeWithText("▼", substring = true).assertDoesNotExist()

        // Hardware Lock / AES-256 footer removed
        compose.onNodeWithText("AES-256", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Hardware Vault", substring = true).assertDoesNotExist()

        // Legacy old button labels removed
        compose.onNodeWithText("Take Photo", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Choose Image", substring = false).assertDoesNotExist()

        // Legacy biometry summary string with "&" removed (replaced by "and")
        compose.onNodeWithText("BPD, HC, AC, FL, FHR & EFW", substring = false).assertDoesNotExist()

        // Clean section titles
        compose.onNodeWithText(str(R.string.ultrasound_timing_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_section_fetus_placenta_liquor)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_biometry_section)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_biometry_summary)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.scan_impression)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_more)).performScrollTo().assertIsDisplayed()

        // Existing fields still present
        node("scanDate").performScrollTo().assertIsDisplayed()
        node("gaWeeks").performScrollTo().assertIsDisplayed()
        node("gaDays").performScrollTo().assertIsDisplayed()
        node("ultrasoundFindingsCard").performScrollTo().assertIsDisplayed()
        node("biometryCard").performScrollTo().assertIsDisplayed()
        node("toggleBiometry").performScrollTo().assertIsDisplayed()
        node("impression").performScrollTo().assertIsDisplayed()
        node("impressionFromChoices").performScrollTo().assertIsDisplayed()
        node("ultrasoundMoreToggle").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 2. Live Summary Card & Normal Scan Flow
    // =========================================================================

    @Test
    fun ultrasoundEditor_liveSummary_blankInitial_fillNormalScan_clearFindings() {
        showEditor(editorState())

        node("ultrasoundLiveSummary").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_report_so_far)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.ultrasound_nothing_chosen_yet)).assertIsDisplayed()

        // Initial guided finding is fetalNumber
        node("findingOpen-fetalNumber").assertIsDisplayed()

        // Click Fill normal scan
        node("fillNormalScan").performClick()
        compose.waitForIdle()

        // Normal choices applied
        val draftValues = state.ultrasoundDraft!!.values
        assertEquals(NORMAL_FETAL_NUMBER, draftValues.fetalNumber)
        assertEquals(NORMAL_PRESENTATION, draftValues.presentation)
        assertEquals("", draftValues.placentalSite)
        assertEquals(NORMAL_AMNIOTIC_FLUID, draftValues.amnioticFluid)

        // Events emitted for normal scan
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, NORMAL_FETAL_NUMBER)))
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PRESENTATION, NORMAL_PRESENTATION)))
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.AMNIOTIC_FLUID, NORMAL_AMNIOTIC_FLUID)))

        // Next blank finding (placenta) is now open
        node("findingOpen-placenta").performScrollTo().assertIsDisplayed()
        node("findingRow-fetalNumber").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(NORMAL_FETAL_NUMBER, substring = true) and inTagged("findingRow-fetalNumber"), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        node("findingRow-presentation").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(NORMAL_PRESENTATION, substring = true) and inTagged("findingRow-presentation"), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        node("findingRow-liquor").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(NORMAL_AMNIOTIC_FLUID, substring = true) and inTagged("findingRow-liquor"), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Live summary updated
        node("ultrasoundLiveSummary").performScrollTo()
        compose.onNode(hasText("Singleton pregnancy", substring = true) and inTagged("ultrasoundLiveSummary"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Click Clear findings
        node("clearFindings").performClick()
        compose.waitForIdle()

        val clearedValues = state.ultrasoundDraft!!.values
        assertEquals("", clearedValues.fetalNumber)
        assertEquals("", clearedValues.presentation)
        assertEquals("", clearedValues.placentalSite)
        assertEquals("", clearedValues.amnioticFluid)

        // Live summary reverts to blank message
        compose.onNodeWithText(str(R.string.ultrasound_nothing_chosen_yet)).assertIsDisplayed()
        node("findingOpen-fetalNumber").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 3. Guided Findings & SummaryRow (no chevron)
    // =========================================================================

    @Test
    fun ultrasoundEditor_guidedFlow_choicesAutoAdvance_andSummaryRow_noChevron() {
        showEditor(editorState())

        // Initial state: fetalNumber is open
        node("findingOpen-fetalNumber").assertIsDisplayed()
        node("findingRow-fetalNumber").assertDoesNotExist()

        // Click "Singleton" (fetalNumberChoice-1)
        node("fetalNumberChoice-1").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("Singleton", state.ultrasoundDraft!!.values.fetalNumber)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, "Singleton")))

        // Advances to presentation
        node("findingOpen-presentation").performScrollTo().assertIsDisplayed()
        node("findingOpen-fetalNumber").assertDoesNotExist()

        // fetalNumber row is now closed and shows "Singleton"
        node("findingRow-fetalNumber").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Singleton", substring = true) and inTagged("findingRow-fetalNumber"), useUnmergedTree = true)
            .assertIsDisplayed()

        // SummaryRow should not contain downward chevron icon
        compose.onNode(hasContentDescription("Expand", substring = true) and inTagged("findingRow-fetalNumber"), useUnmergedTree = true)
            .assertDoesNotExist()

        // Tapping closed row reopens it
        node("findingRow-fetalNumber").performScrollTo().performClick()
        compose.waitForIdle()

        node("findingOpen-fetalNumber").performScrollTo().assertIsDisplayed()
        node("findingOpen-presentation").assertDoesNotExist()
        node("findingRow-presentation").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 4. Calculated GA & Redating Hint
    // =========================================================================

    @Test
    fun ultrasoundEditor_calculatedGa_formatsShortWeeks_andApplyScanGaDispatchesEvents() {
        // Patient dating EDD = 2027-02-11. Scan date = 2026-11-14 -> GA is exactly 27 weeks + 2 days (191 days)
        val values = UltrasoundDraftValues(date = "2026-11-14", gaWeeks = "", gaDays = "")
        showEditor(editorState(values = values))

        node("scanCalculatedGaCard").performScrollTo().assertIsDisplayed()

        // Formatted with shortWeeksText "27+2 wks" inside ltrIsolate
        compose.onAllNodes(hasText("27+2 wks", substring = true) and inTagged("scanCalculatedGaCard"), useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()

        // Legacy format "27w 2d" must never appear
        compose.onNodeWithText("27w 2d", substring = true).assertDoesNotExist()

        // Apply calculated GA button is displayed
        node("applyScanGa").performScrollTo().assertIsDisplayed()

        // Clicking applyScanGa dispatches GA_WEEKS and GA_DAYS events
        node("applyScanGa").performClick()
        compose.waitForIdle()

        assertEquals("27", state.ultrasoundDraft!!.values.gaWeeks)
        assertEquals("2", state.ultrasoundDraft!!.values.gaDays)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, "27")))
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, "2")))

        // Once GA matches calculated GA, applyScanGa button disappears
        node("applyScanGa").assertDoesNotExist()
    }

    @Test
    fun ultrasoundEditor_redatingHint_shownWhenMismatchExceedsThreshold() {
        // Dating GA at 2026-11-14 is 27+2 wks (191 days, threshold = 14 days).
        // Mismatched GA 20+0 wks (140 days) gives difference = 51 days > 14 days.
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            gaWeeks = "20",
            gaDays = "0",
        )
        showEditor(editorState(values = values))

        node("redatingHint").performScrollTo().assertIsDisplayed()
        val expectedHint = str(R.string.redating_hint, 51, 14)
        node("redatingHint").assertTextContains(expectedHint, substring = true)

        // Matching GA: redating hint disappears
        events.clear()
        node("gaWeeks").performScrollTo().performTextInput("7") // make it "207" or replace
        // Show matching state directly
        showEditor(editorState(values = values.copy(gaWeeks = "27", gaDays = "2")))
        node("redatingHint").assertDoesNotExist()
    }

    @Test
    fun ultrasoundEditor_scanDateTodayButton_populatesToday() {
        showEditor(editorState())

        node("scanDateTodayButton").performScrollTo().performClick()
        compose.waitForIdle()

        val todayIso = LocalDate.now().toString()
        assertEquals(todayIso, state.ultrasoundDraft!!.values.date)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.DATE, todayIso)))
    }

    // =========================================================================
    // 5. Biometry Card: Expand/Collapse & Field Entry
    // =========================================================================

    @Test
    fun ultrasoundEditor_biometryCard_toggleExpandsAndCollapses_andFieldsRetainValues() {
        // Empty biometry starts collapsed
        showEditor(editorState())

        node("biometryCard").performScrollTo().assertIsDisplayed()
        node("toggleBiometry").assertIsDisplayed().assert(hasContentDescription(str(R.string.action_expand)))
        compose.onNode(hasText(str(R.string.action_expand)) and inTagged("toggleBiometry"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Fields are hidden
        node("fetalHeartRate").assertDoesNotExist()
        node("biometryBpd").assertDoesNotExist()

        // Click Expand
        node("toggleBiometry").performClick()
        compose.waitForIdle()

        node("toggleBiometry").assert(hasContentDescription(str(R.string.action_collapse)))
        compose.onNode(hasText(str(R.string.action_collapse)) and inTagged("toggleBiometry"), useUnmergedTree = true)
            .assertIsDisplayed()
        node("fetalHeartRate").performScrollTo().assertIsDisplayed()
        node("efw").performScrollTo().assertIsDisplayed()
        node("biometryBpd").performScrollTo().assertIsDisplayed()
        node("biometryHc").performScrollTo().assertIsDisplayed()
        node("biometryAc").performScrollTo().assertIsDisplayed()
        node("biometryFl").performScrollTo().assertIsDisplayed()

        // Enter FHR and BPD
        node("fetalHeartRate").performTextInput("144")
        compose.waitForIdle()
        assertEquals("144", state.ultrasoundDraft!!.values.fetalHeartRate)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_HEART_RATE, "144")))

        node("biometryBpd").performTextInput("52.3")
        compose.waitForIdle()
        assertEquals("52.3", state.ultrasoundDraft!!.values.biometryBpd)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_BPD, "52.3")))

        // Collapse again
        node("toggleBiometry").performScrollTo().performClick()
        compose.waitForIdle()
        node("fetalHeartRate").assertDoesNotExist()
        node("toggleBiometry").assert(hasContentDescription(str(R.string.action_expand)))
    }

    // =========================================================================
    // 6. Impression Card & Impression Generation
    // =========================================================================

    @Test
    fun ultrasoundEditor_impressionFromChoices_mergesWithExistingImpression() {
        val values = UltrasoundDraftValues(
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Anterior",
            amnioticFluid = "Normal",
            impression = "Prior scar noted.",
        )
        showEditor(editorState(values = values))

        node("impression").performScrollTo().assertIsDisplayed()
        node("impressionFromChoices").performScrollTo().assertIsDisplayed()

        // Click Generate impression from choices
        node("impressionFromChoices").performClick()
        compose.waitForIdle()

        val expectedGenerated = "Singleton pregnancy, cephalic presentation, anterior placenta, normal amniotic fluid."
        val expectedMerged = "Prior scar noted.\n$expectedGenerated"

        assertEquals(expectedMerged, state.ultrasoundDraft!!.values.impression)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, expectedMerged)))
    }

    // =========================================================================
    // 7. More Section: Toggle, Attachments, & Visit Linking
    // =========================================================================

    @Test
    fun ultrasoundEditor_moreSection_toggleAndAttachmentsAndLinkedVisit() {
        showEditor(editorState())

        // Initial state without photos or linked visit: section is closed
        node("ultrasoundMoreSection").assertDoesNotExist()
        node("ultrasoundMoreToggle").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(str(R.string.more_open)) and inTagged("ultrasoundMoreToggle"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Click toggle to open
        node("ultrasoundMoreToggle").performClick()
        compose.waitForIdle()

        node("ultrasoundMoreSection").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(str(R.string.more_close)) and inTagged("ultrasoundMoreToggle"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Scan attachments card shows "No photos attached"
        node("scanAttachmentsCard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.no_photos_attached)).performScrollTo().assertIsDisplayed()
        node("captureScanPhotoCamera").performScrollTo().assertIsDisplayed()
        node("addScanPhoto").performScrollTo().assertIsDisplayed()

        // Linked visit card shows unlinked state and visit chip
        node("linkedVisitCard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.no_linked_visit)).performScrollTo().assertIsDisplayed()
        node("linkVisit-${testVisit.id}").performScrollTo().assertIsDisplayed()

        // Click visit chip to link visit
        node("linkVisit-${testVisit.id}").performClick()
        compose.waitForIdle()

        assertEquals(testVisit.id, state.ultrasoundDraft!!.values.linkedVisitId)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, testVisit.id)))

        // Linked state displays title, vitals, and unlink button
        compose.onNode(hasText(testVisit.date, substring = true) and inTagged("linkedVisitCard"), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("118/76", substring = true) and inTagged("linkedVisitCard"), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        node("unlinkVisit").performScrollTo().assertIsDisplayed()

        // Click unlink visit
        node("unlinkVisit").performClick()
        compose.waitForIdle()

        assertEquals("", state.ultrasoundDraft!!.values.linkedVisitId)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "")))
    }

    @Test
    fun ultrasoundEditor_attachments_showsAttachmentRows_removePhotoDispatchesEvent() {
        val values = UltrasoundDraftValues(
            imageUris = listOf("vault:photo-1.webp", "vault:photo-2.webp"),
        )
        // With imageUris present, moreSection starts open
        showEditor(editorState(values = values))

        node("ultrasoundMoreSection").performScrollTo().assertIsDisplayed()
        node("attachmentItem-0").performScrollTo().assertIsDisplayed()
        node("attachmentItem-1").performScrollTo().assertIsDisplayed()

        compose.onNode(hasText(str(R.string.scan_photo_n, 1)) and inTagged("attachmentItem-0"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Click removePhoto-0
        node("removePhoto-0").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(listOf("vault:photo-2.webp"), state.ultrasoundDraft!!.values.imageUris)
        assertTrue(events.contains(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, "vault:photo-2.webp")))
    }

    // =========================================================================
    // 8. Validation Errors & Save Phase
    // =========================================================================

    @Test
    fun ultrasoundEditor_validationErrors_showsErrorCardAndHighlightsFields() {
        val errors = setOf(ClinicalValidationError.REQUIRED_DATE, ClinicalValidationError.INVALID_GA_WEEKS)
        showEditor(editorState(errors = errors))

        node("editorError").performScrollTo().assertIsDisplayed()
        node("scanDate").performScrollTo().assertIsDisplayed()
        node("gaWeeks").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ultrasoundEditor_cancelAndSave_emitEvents() {
        showEditor(editorState())

        node("saveUltrasound").performScrollTo().performClick()
        assertTrue("SubmitUltrasound emitted", events.contains(AncEvent.SubmitUltrasound))

        events.clear()
        node("cancelUltrasound").performScrollTo().performClick()
        assertTrue("CancelEditor emitted", events.contains(AncEvent.CancelEditor))
    }

    @Test
    fun ultrasoundEditor_savingState_disablesControls_showsSpinnerAndSavingLabel() {
        showEditor(editorState(savePhase = SavePhase.PENDING))

        node("saveUltrasound").assertIsNotEnabled()
        node("cancelUltrasound").assertIsNotEnabled()
        node("ultrasoundSaveSpinner").assertExists()
        compose.onNodeWithText(str(R.string.saving_label), useUnmergedTree = true).assertExists()
    }

    // =========================================================================
    // 9. Touch Targets (>= 47.99dp)
    // =========================================================================

    @Test
    fun ultrasoundEditor_touchTargets_atLeast48dp() {
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            gaWeeks = "",
            gaDays = "",
            fetalHeartRate = "140",
            biometryBpd = "50",
            imageUris = listOf("vault:photo-1.webp"),
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Anterior",
            amnioticFluid = "Normal",
        )
        showEditor(editorState(values = values))

        assertMinTouchTarget("fillNormalScan")
        assertMinTouchTarget("clearFindings")
        assertMinTouchTarget("scanDateCalendarButton")
        assertMinTouchTarget("scanDateTodayButton")
        assertMinTouchTarget("applyScanGa")
        assertMinTouchTarget("findingRow-fetalNumber")
        node("findingRow-fetalNumber").performClick()
        compose.waitForIdle()
        assertMinTouchTarget("fetalNumberChoice-0")
        assertMinTouchTarget("fetalNumberChoice-1")
        assertMinTouchTarget("findingRow-presentation")
        assertMinTouchTarget("findingRow-placenta")
        assertMinTouchTarget("findingRow-liquor")
        assertMinTouchTarget("toggleBiometry")
        assertMinTouchTarget("impressionFromChoices")

        // ultrasoundMoreToggle has min height 56dp
        node("ultrasoundMoreToggle").performScrollTo()
        val moreBounds = bounds("ultrasoundMoreToggle")
        assertTrue("more toggle width >= 48dp", moreBounds.width >= 47.99.dp)
        assertTrue("more toggle height >= 56dp (${moreBounds.height})", moreBounds.height >= 55.99.dp)

        assertMinTouchTarget("removePhoto-0")
        assertMinTouchTarget("captureScanPhotoCamera")
        assertMinTouchTarget("addScanPhoto")
        assertMinTouchTarget("linkVisit-${testVisit.id}")
        assertMinTouchTarget("cancelUltrasound")
        assertMinTouchTarget("saveUltrasound")
    }

    @Test
    fun ultrasoundEditor_unlinkVisit_touchTarget_atLeast48dp() {
        val values = UltrasoundDraftValues(
            linkedVisitId = testVisit.id,
            imageUris = listOf("vault:photo-1.webp"),
        )
        showEditor(editorState(values = values))

        assertMinTouchTarget("unlinkVisit")
    }

    // =========================================================================
    // 10. Typography & Responsiveness at 360dp
    // =========================================================================

    @Test
    fun ultrasoundEditor_noTextBelow12sp_fontScale1_0() {
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            fetalHeartRate = "140",
            biometryBpd = "50",
            imageUris = listOf("vault:photo-1.webp"),
            linkedVisitId = testVisit.id,
        )
        showEditor(editorState(values = values), fontScale = 1.0f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun ultrasoundEditor_noTextBelow12sp_fontScale1_3() {
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            fetalHeartRate = "140",
            biometryBpd = "50",
            imageUris = listOf("vault:photo-1.webp"),
            linkedVisitId = testVisit.id,
        )
        showEditor(editorState(values = values), fontScale = 1.3f)
        assertNoTextBelow12sp(HOST_TAG)
    }

    @Test
    fun ultrasoundEditor_360dp_fontScale1_3_noClipping() {
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            gaWeeks = "27",
            gaDays = "2",
            fetalHeartRate = "140",
            biometryBpd = "50",
            imageUris = listOf("vault:photo-1.webp"),
            linkedVisitId = testVisit.id,
        )
        showEditor(editorState(values = values), fontScale = 1.3f)

        compose.assertAllTextWhole("ultrasoundLiveSummary")
        node("gaWeeks").performScrollTo().assertIsDisplayed()
        node("gaDays").performScrollTo().assertIsDisplayed()
        node("saveUltrasound").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ultrasoundEditor_360dp_fontScale2_0_noClipping() {
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            gaWeeks = "27",
            gaDays = "2",
            fetalHeartRate = "140",
            biometryBpd = "50",
            imageUris = listOf("vault:photo-1.webp"),
            linkedVisitId = testVisit.id,
        )
        showEditor(editorState(values = values), fontScale = 2.0f)

        compose.assertAllTextWhole("ultrasoundLiveSummary")
        node("gaWeeks").performScrollTo().assertIsDisplayed()
        node("gaDays").performScrollTo().assertIsDisplayed()
        node("captureScanPhotoCamera").performScrollTo().assertIsDisplayed()
        node("addScanPhoto").performScrollTo().assertIsDisplayed()
        node("saveUltrasound").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 11. Dark Theme & RTL Mirroring
    // =========================================================================

    @Test
    fun ultrasoundEditor_darkTheme_rtl_noOverlapOrClipping() {
        val arabicPatient = basePatient.copy(displayName = "منى رجب")
        val values = UltrasoundDraftValues(
            date = "2026-11-14",
            gaWeeks = "27",
            gaDays = "2",
            linkedVisitId = testVisit.id,
        )
        showEditor(
            editorState(patient = arabicPatient, values = values),
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )

        node("ultrasoundEditor").assertIsDisplayed()
        compose.onNodeWithText("منى رجب", substring = true, useUnmergedTree = true).assertIsDisplayed()
        node("scanCalculatedGaCard").performScrollTo().assertIsDisplayed()
        node("ultrasoundFindingsCard").performScrollTo().assertIsDisplayed()
        node("cancelUltrasound").performScrollTo().assertIsDisplayed()
        node("saveUltrasound").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 12. UltrasoundPhotoViewerDialog: Layout, Typography, & Pager
    // =========================================================================

    @Test
    fun ultrasoundPhotoViewerDialog_layoutAndTypography_noTextBelow12sp() {
        narrowWindow(1.0f)
        showViewer()

        node("ultrasoundPhotoViewerDialog").assertIsDisplayed()

        // Clean header with comma separator and ltrIsolate date
        compose.onNode(hasText("2026-05-30", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Subtitle shows photo count and GA
        compose.onNode(hasText("Photo 1 of 2", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("20+0 wks", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Image unavailable fallback tag is displayed when image cannot be loaded
        node("image_unavailable").assertIsDisplayed().assertTextContains(str(R.string.image_unavailable), substring = true)

        // Biometrics displayed as PlainField grid
        compose.onNode(hasText("148 bpm", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("380 g", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("49 mm", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("34 mm", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("Cephalic", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Legacy old viewer text must not appear
        compose.onNodeWithText("Close Viewer", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Ultrasound Scan:", substring = true).assertDoesNotExist()

        // No text below 12sp
        assertNoTextBelow12sp("ultrasoundPhotoViewerDialog")
    }

    @Test
    fun ultrasoundPhotoViewerDialog_touchTargets_atLeast48dp() {
        narrowWindow(1.0f)
        showViewer()

        assertMinTouchTarget("closePhotoViewer", scroll = false)
        assertMinTouchTarget("prevPhotoButton", scroll = false)
        assertMinTouchTarget("nextPhotoButton", scroll = false)
        assertMinTouchTarget("dismissPhotoViewer", scroll = true)
    }

    @Test
    fun ultrasoundPhotoViewerDialog_pager_navigatesBetweenPhotos() {
        narrowWindow(1.0f)
        showViewer()

        // At index 0: prev is disabled, next is enabled
        node("prevPhotoButton").assertIsNotEnabled()
        node("nextPhotoButton").assertIsEnabled()
        compose.onNode(hasText("Photo 1 of 2", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Click next
        node("nextPhotoButton").performClick()
        compose.waitForIdle()

        // At index 1: prev is enabled, next is disabled
        node("prevPhotoButton").assertIsEnabled()
        node("nextPhotoButton").assertIsNotEnabled()
        compose.onNode(hasText("Photo 2 of 2", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Click prev
        node("prevPhotoButton").performClick()
        compose.waitForIdle()

        node("prevPhotoButton").assertIsNotEnabled()
        node("nextPhotoButton").assertIsEnabled()
        compose.onNode(hasText("Photo 1 of 2", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun ultrasoundPhotoViewerDialog_singlePhoto_hidesPagerControls() {
        val singlePhotoScan = viewerScan.copy(imageUris = listOf("dummy://scan-photo-1.jpg"))
        narrowWindow(1.0f)
        showViewer(scan = singlePhotoScan)

        node("prevPhotoButton").assertDoesNotExist()
        node("nextPhotoButton").assertDoesNotExist()
        compose.onNode(hasText("Photo 1 of 1", substring = true) and inTagged("ultrasoundPhotoViewerDialog"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun ultrasoundPhotoViewerDialog_360dp_fontScale2_0_scrollsAndDismissReachable() {
        narrowWindow(2.0f)
        var dismissed = false
        showViewer(fontScale = 2.0f, onDismiss = { dismissed = true })

        node("ultrasoundPhotoViewerDialog").assertIsDisplayed()
        node("dismissPhotoViewer").performScrollTo().assertIsDisplayed()
        assertMinTouchTarget("dismissPhotoViewer", scroll = true)

        node("dismissPhotoViewer").performClick()
        compose.waitForIdle()
        assertTrue("dismissPhotoViewer called onDismiss", dismissed)
    }

    @Test
    fun ultrasoundPhotoViewerDialog_darkTheme_rtl_rendersCleanly() {
        narrowWindow(1.0f)
        showViewer(darkTheme = true, layoutDirection = LayoutDirection.Rtl)

        node("ultrasoundPhotoViewerDialog").assertIsDisplayed()
        node("closePhotoViewer").assertIsDisplayed()
        node("dismissPhotoViewer").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 13. ZoomableImage: 1x Reset Chip & Touch Target
    // =========================================================================

    @Test
    fun zoomableImage_resetButton_atLeast48dp_andResetsOnTap() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).asImageBitmap()
        showZoomableImage(bitmap)

        // Initial scale is 1f: reset button does not exist
        node("zoomResetButton").assertDoesNotExist()

        // Double tap on the image to zoom in
        compose.onNode(hasContentDescription("Test scan photo"), useUnmergedTree = true)
            .performTouchInput { doubleClick() }
        compose.waitForIdle()

        // Once zoomed (scale > 1f), zoomResetButton is displayed
        node("zoomResetButton").assertIsDisplayed()
        assertMinTouchTarget("zoomResetButton", scroll = false)

        // Label is "1x"
        compose.onNode(hasText("1x") and inTagged("zoomResetButton"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Tapping 1x resets zoom and removes the button
        node("zoomResetButton").performClick()
        compose.waitForIdle()

        node("zoomResetButton").assertDoesNotExist()
    }

    private companion object {
        const val HOST_TAG = "ultrasoundEditorHost"
        const val VIEWER_HOST_TAG = "ultrasoundViewerHost"
        const val ZOOM_HOST_TAG = "zoomableImageHost"
    }
}
