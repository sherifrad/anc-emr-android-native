package com.sherifradwan.ancnative

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import com.sherifradwan.ancnative.ui.report.patientShareText
import com.sherifradwan.ancnative.ui.report.reportStatusLine
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 20: Two-page clinical report dialog (Level 3).
 *
 * Mounts through MainActivity inside a 360dp-wide window host, matching HistoryEditorsRedesignTest,
 * RegistrationRedesignTest, and AttachmentsRedesignTest style (single setContent per test,
 * shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides, useUnmergedTree,
 * performScrollTo before assertIsDisplayed, select nodes by testTag, >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class ReportDialogRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val basePatient: DemoPatient = DemoFixtures.patients.first()

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()

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

    private fun assertNoClippingInContainer(tag: String) {
        val container = compose.onNode(hasTestTag(tag), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val nodes = compose.onAllNodes(
            (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.forEach { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val layout = layouts.firstOrNull() ?: return@forEach
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: ""
            if (text.isBlank()) return@forEach
            assertTrue("'$text' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
            for (line in 0 until layout.lineCount) {
                val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
                assertTrue(
                    "'$text' line $line wider than node ($lineWidth > ${layout.size.width})",
                    lineWidth <= layout.size.width + 1f,
                )
                assertFalse("'$text' ellipsized on line $line", layout.isLineEllipsized(line))
            }
            val byId = SemanticsMatcher("node ${n.id}") { it.id == n.id }
            val b = compose.onNode(byId, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("'$text' starts outside $tag (${b.left} < ${container.left - 1.dp})", b.left >= container.left - 1.dp)
            assertTrue("'$text' runs past $tag (${b.right} > ${container.right + 1.dp})", b.right <= container.right + 1.dp)
        }
    }

    /**
     * Holds the activity resources at a 360dp-wide window and [fontScale] for this test.
     * Dialog window builds its density from these resources, matching AttachmentsRedesignTest.
     */
    private fun narrowWindow(fontScale: Float) {
        compose.activityRule.scenario.onActivity { activity ->
            val res = activity.resources
            savedConfig = Configuration(res.configuration)
            savedMetrics = DisplayMetrics().apply { setTo(res.displayMetrics) }
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
    }

    @After
    fun tearDown() {
        restoreResources()
    }

    private fun show(
        patient: DemoPatient,
        securitySettings: SecuritySettingsState? = null,
        fontScale: Float = 1.0f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
    ) = showDynamic(
        patientProvider = { patient },
        securitySettings = securitySettings,
        fontScale = fontScale,
        darkTheme = darkTheme,
        layoutDirection = layoutDirection,
        onDismiss = onDismiss,
    )

    private fun showDynamic(
        patientProvider: () -> DemoPatient,
        securitySettings: SecuritySettingsState? = null,
        fontScale: Float = 1.0f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
    ) {
        narrowWindow(fontScale)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        PatientTwoPageReportDialog(
                            patient = patientProvider(),
                            securitySettings = securitySettings,
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showApp(
        stateToHost: AncState,
        fontScale: Float = 1.0f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onEventCollector: ((AncEvent) -> Unit)? = null,
    ) {
        narrowWindow(fontScale)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(
                            Modifier
                                .requiredSize(360.dp, 640.dp)
                                .clipToBounds()
                                .testTag(HOST_TAG),
                        ) {
                            AncApp(
                                state = stateToHost,
                                onEvent = { event -> onEventCollector?.invoke(event) },
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

    private fun visit(
        id: String,
        patientId: String = basePatient.id,
        date: String,
        bloodPressure: String? = "110/70",
        weight: String? = "65 kg",
        findings: String? = null,
        notes: String? = null,
    ) = DemoVisit(
        id = id,
        patientId = patientId,
        date = date,
        bloodPressure = bloodPressure,
        weight = weight,
        findings = findings,
        notes = notes,
    )

    private fun scan(
        id: String,
        patientId: String = basePatient.id,
        date: String,
        gaWeeks: Int? = null,
        presentation: String? = "Cephalic",
        placentalSite: String? = "Anterior",
        amnioticFluid: String? = "Normal",
        efw: String? = null,
        imageUris: List<String> = emptyList(),
    ) = DemoUltrasoundScan(
        id = id,
        patientId = patientId,
        date = date,
        gestationalAgeWeeks = gaWeeks,
        gestationalAgeDays = 0,
        fetalNumber = "Singleton",
        presentation = presentation,
        placentalSite = placentalSite,
        amnioticFluid = amnioticFluid,
        fetalHeartRate = null,
        biometryBpd = null,
        biometryHc = null,
        biometryAc = null,
        biometryFl = null,
        estimatedFetalWeight = efw,
        impression = null,
        imageUris = imageUris,
    )

    private fun medication(
        id: String,
        patientId: String = basePatient.id,
        name: String,
        dosage: String = "10 mg",
        route: String = "Oral",
        frequency: String = "Once daily",
        startDate: String = "2026-03-01",
        endDate: String? = null,
        status: String = "Active",
        notes: String? = null,
        isProphylaxis: Boolean = false,
    ) = DemoMedicationRecord(
        id = id,
        patientId = patientId,
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        status = status,
        notes = notes,
        isProphylaxis = isProphylaxis,
    )

    // =========================================================================
    // 1. Kept tags & page switching & dismiss
    // =========================================================================

    @Test
    fun keptTags_displayedAndHierarchy_pageSwitching_andCloseCallback() {
        var dismissed = false
        show(basePatient, onDismiss = { dismissed = true })

        // Outer surface and header controls
        node("patientTwoPageReportDialog").assertIsDisplayed()
        node("closeReportDialog").assertIsDisplayed()
        node("shareReportButton").assertIsDisplayed()
        node("printReportButton").assertIsDisplayed().assertIsEnabled()
        node("reportPageSwitcher").assertIsDisplayed()
            .assertContentDescriptionEquals(str(R.string.report_pages_tab_group))
        node("reportPage1Chip").assertIsDisplayed()
        node("reportPage2Chip").assertIsDisplayed()
        node("reportPageContent").assertIsDisplayed()

        // Page 1 initial sections are composed
        compose.onNodeWithText(str(R.string.report_section_patient_identity), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_pregnancy_dating), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_history), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_previous_pregnancies), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_visits), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 2 sections must not exist on Page 1
        compose.onNodeWithText(str(R.string.report_section_scans), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.report_section_labs), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.report_section_delivery_plan), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.report_section_attestation), useUnmergedTree = true).assertDoesNotExist()

        // Switch to Page 2
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Page 2 sections are composed
        compose.onNodeWithText(str(R.string.report_badge_continued), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_page2_subtitle), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_scans), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_labs), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_current_meds), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_delivery_plan), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_attestation), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 1 sections must not exist on Page 2
        compose.onNodeWithText(str(R.string.report_section_patient_identity), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.report_section_history), useUnmergedTree = true).assertDoesNotExist()

        // Switch back to Page 1
        node("reportPage1Chip").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.report_section_patient_identity), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Close invokes callback
        node("closeReportDialog").performClick()
        compose.waitForIdle()
        assertTrue("onDismiss was called on close click", dismissed)
    }

    // =========================================================================
    // 2. Minimum 48dp touch targets on all interactive controls
    // =========================================================================

    @Test
    fun touchTargets_allInteractiveControls_atLeast48dp() {
        show(basePatient)

        assertMinTouchTarget("closeReportDialog")
        assertMinTouchTarget("shareReportButton")
        assertMinTouchTarget("printReportButton")
        assertMinTouchTarget("reportPage1Chip")
        assertMinTouchTarget("reportPage2Chip")
    }

    // =========================================================================
    // 3. Clinical data parity & Negative invariants (no fees, no frequency)
    // =========================================================================

    @Test
    fun clinicalDataParity_populatedPatient_allValuesRendered_noFeesOrFrequency() {
        val testPatient = basePatient.copy(
            displayName = "Mona Ragab",
            phone = "01001234567",
            ageText = "29",
            bloodGroupText = "A+",
            addressText = "Cairo, Egypt",
            obstetricNotes = "Agreed delivery fee: 12000 EGP FEE_SECRET_99000 at Mothercare Hospital.",
            medications = listOf(
                medication(
                    id = "med-test-s20-1",
                    patientId = basePatient.id,
                    name = "Test Medication Alpha",
                    dosage = "10 mg",
                    frequency = "FREQ_SECRET_Q8H",
                    notes = "Take after breakfast",
                ),
            ),
        )

        show(testPatient)

        // Header meta and patient identity
        compose.onNodeWithText(str(R.string.report_dialog_kicker), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText("Mona Ragab", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        compose.onNodeWithText(testPatient.caseId, substring = true, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("01001234567", substring = true, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("A+", substring = true, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_age_years_format, 29), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cairo, Egypt", substring = true, useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // Page 1: History, past pregnancies, and visits
        compose.onNodeWithText(str(R.string.report_section_history), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_gpa_label), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.medical_history), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.surgical_history), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.family_history), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_visits), useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // Switch to Page 2
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Page 2: Medication details rendered
        compose.onNodeWithText("Test Medication Alpha", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("10 mg", substring = true, useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // Page 2: Delivery plan and attestation
        compose.onNodeWithText(str(R.string.report_section_delivery_plan), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_attestation), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_doctor_name_en), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_doctor_specialty_en), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_doctor_name_ar), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_doctor_specialty_ar), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("01201720808", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // LEVEL 3 NEGATIVE INVARIANTS:
        // Delivery fees must NEVER be rendered
        compose.onNodeWithText("FEE_SECRET_99000", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("12000", substring = true, useUnmergedTree = true).assertDoesNotExist()
        // Medication frequency must NEVER be rendered
        compose.onNodeWithText("FREQ_SECRET_Q8H", substring = true, useUnmergedTree = true).assertDoesNotExist()
        // Dashed Stamp box must NOT be rendered in Compose UI
        compose.onNodeWithText("Stamp", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // =========================================================================
    // 4. Clamping: limits visits, scans, labs and renders hidden notices
    // =========================================================================

    @Test
    fun clamping_limitsVisitsScansLabs_andRendersHiddenNotices() {
        val visits15 = (1..15).map { i ->
            visit(
                id = "visit-clamp-$i",
                date = "2026-0${(i % 9) + 1}-10",
                bloodPressure = "110/70",
                weight = "${60 + i} kg",
                findings = "Routine visit findings $i",
                notes = "Notes $i",
            )
        }
        val scans9 = (1..9).map { i ->
            scan(
                id = "scan-clamp-$i",
                date = "2026-0${(i % 9) + 1}-12",
                gaWeeks = 10 + i * 2,
                efw = "${500 + i * 200}",
                imageUris = if (i == 1) listOf("https://anc.local/img.jpg") else emptyList(),
            )
        }
        val labs12 = (1..12).map { i ->
            DemoLabInvestigation(
                id = "lab-clamp-$i",
                patientId = basePatient.id,
                date = "2026-0${(i % 9) + 1}-15",
                testCategory = "Biochemistry",
                testName = "Lab Investigation Test $i",
                result = "${10 + i}",
                unit = "mg/dL",
                referenceRange = "5-20",
                isAbnormal = false,
                notes = "Note $i",
            )
        }
        val clampedPatient = basePatient.copy(
            visits = visits15,
            ultrasoundScans = scans9,
            labInvestigations = labs12,
        )

        show(clampedPatient)

        // Page 1: 15 visits clamped to 12 -> 3 hidden notice
        val expectedVisitsNotice = str(R.string.report_hidden_visits_format, 3)
        compose.onNodeWithText(expectedVisitsNotice, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Oldest 3 visits (#1..#3) are hidden; #4 and #15 are visible
        compose.onNodeWithText(ltrIsolate("#4"), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ltrIsolate("#15"), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ltrIsolate("#1"), substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(ltrIsolate("#2"), substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(ltrIsolate("#3"), substring = true, useUnmergedTree = true).assertDoesNotExist()

        // Switch to Page 2
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Page 2: 9 scans clamped to 7 -> 2 hidden scans notice
        val expectedScansNotice = str(R.string.report_hidden_scans_format, 2)
        compose.onNodeWithText(expectedScansNotice, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 2: 12 labs clamped to 10 -> 2 hidden labs notice
        val expectedLabsNotice = str(R.string.report_hidden_labs_format, 2)
        compose.onNodeWithText(expectedLabsNotice, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 5. Conditional alerts & clinical flags
    // =========================================================================

    @Test
    fun conditionalAlerts_highRisk_rhNegative_clinicalAttention_allergy_severeBp_abnormalLab_stoppedMeds() {
        val severeVisit = visit(
            id = "visit-severe-bp-1",
            patientId = "10000000-0000-4000-8000-000000000004",
            date = "2026-10-20",
            bloodPressure = "160/110",
            weight = "78 kg",
            findings = "Severe hypertension recorded on assessment",
            notes = "Emergency obstetric assessment protocol",
        )
        val candidate = DemoFixtures.patients.find { it.caseId == "DEMO-ANC-004" }
            ?: DemoFixtures.patients[3]
        val alertPatient = candidate.copy(
            riskLevel = "High Risk",
            bloodGroupText = "O-",
            clinicalAttention = "Critical: Prior IUFD at 34w + Primary APS + Rh-Negative",
            allergyHistory = "Penicillin (moderate urticaria)",
            delivered = true,
            status = "archived",
            visits = candidate.visits + severeVisit,
        )

        show(alertPatient)

        // Page 1: status line banner
        val statusLine = reportStatusLine(alertPatient)
        assertNotNull("status line must not be null for archived/delivered patient", statusLine)
        compose.onNodeWithText(statusLine!!, substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 1: High risk tag and Rh negative tag
        compose.onNodeWithText(str(R.string.high_risk), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.rh_negative_tag), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 1: Clinical attention note box
        compose.onNodeWithText(str(R.string.clinical_attention), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Critical: Prior IUFD at 34w", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 1: Drug allergies alert card
        compose.onNodeWithText(str(R.string.report_drug_allergies_label), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Penicillin (moderate urticaria)", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 1: Severe BP status word
        compose.onNodeWithText(str(R.string.report_bp_severe), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Switch to Page 2
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Page 2: Abnormal lab status word
        compose.onNodeWithText(str(R.string.lab_abnormal), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 2: Stopped medications section card
        compose.onNodeWithText(str(R.string.report_section_stopped_meds), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Page 2: Blood bank precautions in delivery plan
        compose.onNodeWithText("Blood bank precautions", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Anti-D Prophylaxis + Matched Packed RBCs Cross-match", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 6. Raised BP warning
    // =========================================================================

    @Test
    fun raisedBpAlert_rendersRaisedBpWarning() {
        val raisedVisit = visit(
            id = "visit-raised-bp",
            date = "2026-09-01",
            bloodPressure = "145/95",
            weight = "68 kg",
            findings = "Borderline hypertension observed",
        )
        val patient = basePatient.copy(visits = listOf(raisedVisit))

        show(patient)

        compose.onNodeWithText(str(R.string.report_bp_raised), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 7. Stopped medications card omitted when empty
    // =========================================================================

    @Test
    fun stoppedMedications_omittedWhenEmpty() {
        val activeOnlyMeds = listOf(
            medication(
                id = "med-active-only",
                patientId = basePatient.id,
                name = "Active Supplement",
                dosage = "5 mg",
                frequency = "Once daily",
                startDate = "2026-01-01",
                status = "Active",
            ),
        )
        val patient = basePatient.copy(medications = activeOnlyMeds)

        show(patient)

        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(str(R.string.report_section_current_meds), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_stopped_meds), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // =========================================================================
    // 8. Empty sections & blank histories fallback strings
    // =========================================================================

    @Test
    fun emptySections_andBlankHistories_renderExpectedFallbacks() {
        val emptyPatient = DemoPatient(
            id = "empty-patient-s20",
            caseId = "DEMO-EMPTY-001",
            displayName = "Empty Patient",
            dateOfBirth = null,
            phone = null,
            visits = emptyList(),
            riskLevel = null,
            ageText = null,
            addressText = null,
            bloodGroupText = null,
            medicalHistory = null,
            surgicalHistory = null,
            familyHistory = null,
            allergyHistory = null,
            gravida = null,
            parity = null,
            abortion = null,
            obstetricNotes = null,
            clinicalAttention = null,
            delivered = false,
            revision = 1,
            previousPregnancies = emptyList(),
            dating = null,
            ultrasoundScans = emptyList(),
            labInvestigations = emptyList(),
            medications = emptyList(),
        )

        show(emptyPatient)

        // Page 1 fallbacks
        compose.onNodeWithText(str(R.string.report_phone_unspecified), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_ga_not_recorded), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Blank histories render "Not recorded"
        val notRecordedNodes = compose.onAllNodes(hasText(str(R.string.not_recorded)), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Blank history fields must show Not recorded", notRecordedNodes.size >= 4)

        // Empty previous pregnancies and visits
        compose.onNodeWithText(
            str(R.string.report_empty_previous_pregnancies_format, previousDeliverySummary(emptyPatient)),
            substring = true,
            useUnmergedTree = true,
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_empty_visits), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Switch to Page 2
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Page 2 empty fallbacks
        compose.onNodeWithText(str(R.string.report_empty_scans), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_empty_labs), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_empty_current_meds), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_section_stopped_meds), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // =========================================================================
    // 9. Share action launches chooser intent wrapping patientShareText
    // =========================================================================

    @Test
    fun shareAction_launchesChooserIntent_wrappingSendIntentWithSummary() {
        show(basePatient)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var capturedIntent: Intent? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult {
                capturedIntent = intent
                return Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            }
        }
        instrumentation.addMonitor(monitor)

        try {
            node("shareReportButton").performClick()
            compose.waitForIdle()

            assertEquals("Share action must trigger activity monitor", 1, monitor.hits)
            val chooserIntent = capturedIntent
            assertNotNull("Chooser intent must not be null", chooserIntent)
            assertEquals(Intent.ACTION_CHOOSER, chooserIntent!!.action)
            assertEquals(str(R.string.report_share_chooser_title), chooserIntent.getStringExtra(Intent.EXTRA_TITLE))

            @Suppress("DEPRECATION")
            val sendIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertNotNull("Wrapped send intent must not be null", sendIntent)
            assertEquals(Intent.ACTION_SEND, sendIntent!!.action)
            assertEquals("text/plain", sendIntent.type)
            assertEquals(
                str(R.string.report_share_subject_format, basePatient.displayName),
                sendIntent.getStringExtra(Intent.EXTRA_SUBJECT),
            )
            assertEquals(patientShareText(basePatient), sendIntent.getStringExtra(Intent.EXTRA_TEXT))
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    // =========================================================================
    // 10 & 11. Typography: no text below 12sp at fontScale 1.0 and 1.3
    // =========================================================================

    @Test
    fun typography_noTextBelow12sp_fontScale1_0_page1AndPage2() {
        show(basePatient, fontScale = 1.0f)
        assertNoTextBelow12sp("patientTwoPageReportDialog")

        node("reportPage2Chip").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("patientTwoPageReportDialog")
    }

    @Test
    fun typography_noTextBelow12sp_fontScale1_3_page1AndPage2() {
        show(basePatient, fontScale = 1.3f)
        assertNoTextBelow12sp("patientTwoPageReportDialog")

        node("reportPage2Chip").performClick()
        compose.waitForIdle()
        assertNoTextBelow12sp("patientTwoPageReportDialog")
    }

    // =========================================================================
    // 12. Narrow 360dp window + fontScale 1.3: no clipping
    // =========================================================================

    @Test
    fun narrow360dp_fontScale1_3_page1AndPage2_noClipping() {
        show(basePatient, fontScale = 1.3f)

        assertNoClippingInContainer("patientTwoPageReportDialog")
        compose.assertAllTextWhole("reportPageSwitcher")

        node("reportPage2Chip").performClick()
        compose.waitForIdle()
        assertNoClippingInContainer("patientTwoPageReportDialog")
    }

    // =========================================================================
    // 13. Narrow 360dp window + fontScale 2.0: no clipping
    // =========================================================================

    @Test
    fun narrow360dp_fontScale2_0_page1AndPage2_noClipping() {
        show(basePatient, fontScale = 2.0f)

        node("reportPageSwitcher").assertIsDisplayed()
        node("reportPage1Chip").assertIsDisplayed()
        node("reportPage2Chip").assertIsDisplayed()
        node("closeReportDialog").assertIsDisplayed()
        node("shareReportButton").assertIsDisplayed()
        node("printReportButton").assertIsDisplayed()
        compose.onAllNodesWithText(basePatient.displayName, substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()

        assertNoClippingInContainer("patientTwoPageReportDialog")

        node("reportPage2Chip").performClick()
        compose.waitForIdle()
        assertNoClippingInContainer("patientTwoPageReportDialog")
    }

    // =========================================================================
    // 14. RTL Arabic + Dark theme + fontScale 1.3: layout integrity
    // =========================================================================

    @Test
    fun rtlArabic_darkTheme_layoutIntegrity() {
        val arabicPatient = basePatient.copy(
            displayName = "منى رجب",
            bloodGroupText = "B+",
        )
        show(
            patient = arabicPatient,
            fontScale = 1.3f,
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl,
        )

        node("patientTwoPageReportDialog").assertIsDisplayed()
        compose.onAllNodesWithText("منى رجب", substring = true, useUnmergedTree = true).onFirst().assertIsDisplayed()
        node("closeReportDialog").assertIsDisplayed()
        node("shareReportButton").assertIsDisplayed()
        node("printReportButton").assertIsDisplayed()
        node("reportPage1Chip").assertIsDisplayed()
        node("reportPage2Chip").assertIsDisplayed()

        // Switch to Page 2 in RTL Dark theme
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(str(R.string.report_section_scans), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_doctor_name_ar), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 15. Integration with PatientHub / AncApp
    // =========================================================================

    @Test
    fun patientHubIntegration_workspaceReportButton_opensAndDismissesDialog() {
        val state = AncState(
            patients = listOf(basePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.Workspace(basePatient.id, WorkspaceSection.EPISODE_OVERVIEW),
        )
        showApp(state)

        // Report button sits in the hub header; clicking opens report dialog
        compose.onNodeWithTag("workspaceReportButton", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        node("patientTwoPageReportDialog").assertIsDisplayed()
        node("reportPageSwitcher").assertIsDisplayed()
        node("reportPage1Chip").assertIsDisplayed()
        node("reportPage2Chip").assertIsDisplayed()

        // Close dismisses dialog
        node("closeReportDialog").performClick()
        compose.waitForIdle()

        node("patientTwoPageReportDialog").assertDoesNotExist()
    }

    // =========================================================================
    // 16. Post-term dating: 42+ weeks shows (Post-term), 40 weeks shows no post-term
    // =========================================================================

    @Test
    fun pregnancyDating_ga42WeeksOrMore_showsPostTermFormat_and40WeeksShowsNoPostTerm() {
        val today = LocalDate.now()
        val postTermDating = DemoPatientDating(
            patientId = basePatient.id,
            lmpDate = today.minusDays(280 + 14).toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val termDating = DemoPatientDating(
            patientId = basePatient.id,
            lmpDate = today.minusDays(280).toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val patient42 = basePatient.copy(dating = postTermDating, delivered = false)
        val patient40 = basePatient.copy(dating = termDating, delivered = false)

        var currentPatient by mutableStateOf(patient42)
        showDynamic(patientProvider = { currentPatient })

        val postTermText = str(R.string.report_ga_post_term_format, "42+0 wks")
        compose.onNodeWithText(postTermText, substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        compose.runOnIdle { currentPatient = patient40 }
        compose.waitForIdle()

        compose.onNodeWithText("40+0 wks", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_ga_post_term_format, "40+0 wks"), substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithText("(Post-term)", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // =========================================================================
    // 17. Visit with no computable GA shows "-" GA chip in visits card
    // =========================================================================

    @Test
    fun visitsCard_visitWithNoComputableGa_showsDashGaChip() {
        val noGaVisit = visit(
            id = "visit-no-ga-1",
            date = "2026-03-01",
            bloodPressure = "110/70",
            weight = "65 kg",
            findings = "Routine prenatal checkup",
        )
        val patient = basePatient.copy(
            dating = null,
            gravida = 1L,
            parity = 0L,
            abortion = 0L,
            previousPregnancies = emptyList(),
            visits = listOf(noGaVisit),
        )

        show(patient)

        compose.onNodeWithText(str(R.string.report_section_visits), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ltrIsolate("#1"), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("01/03/2026", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("-", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 18. Non-delivered patient with null dating shows EDD Not recorded
    // =========================================================================

    @Test
    fun pregnancyDating_nonDeliveredPatientWithNullDating_showsEddRowWithNotRecorded() {
        val patient = basePatient.copy(
            dating = null,
            delivered = false,
        )

        show(patient)

        compose.onNodeWithText(str(R.string.report_section_pregnancy_dating), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("EDD", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("EDD") and hasText(str(R.string.not_recorded)))
            .performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 19. Custom clinicSubtitle still shows fixed report_clinic_subtitle_full
    // =========================================================================

    @Test
    fun clinicMetaCard_customClinicSubtitle_stillShowsFixedFullSubtitleString() {
        val customSubtitle = "Private Fetal Medicine Center of Cairo"
        val settings = SecuritySettingsState(clinicSubtitle = customSubtitle)

        show(basePatient, securitySettings = settings)

        compose.onNodeWithText(str(R.string.report_clinic_subtitle_full), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(customSubtitle, substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    private companion object {
        const val HOST_TAG = "reportDialogAppHost"
    }
}
