package com.sherifradwan.ancnative

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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientStatusDraft
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.UltrasoundDraft
import com.sherifradwan.ancnative.state.UltrasoundDraftValues
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.PatientStatusEditor
import com.sherifradwan.ancnative.ui.UltrasoundEditor
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.PatientHubHeader
import com.sherifradwan.ancnative.ui.patienthub.PatientHubHeaderState
import com.sherifradwan.ancnative.ui.patienthub.PatientHubScreen
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.MedsTab
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Polish batch redesign verification tests (360dp narrow window, single setContent per test,
 * testTags, unmerged tree assertions).
 *
 * Verifies:
 * (a) Scan card GA chip and EFW in report dialog show "Not recorded" when missing, and actual values when present.
 * (b) PatientHubHeader hero and collapsed top bar draw Arabic name right-aligned near right padding, English left-aligned.
 * (c) EditorPage subtitle draws Arabic name right-aligned.
 * (d) Date labels (Visit date, Scan date, Delivery date) do not contain "YYYY".
 * (e) Meds tab active prescriptions title is sentence case; prescribe buttons contain no "+" text, have tags, and >=48dp.
 */
@RunWith(AndroidJUnit4::class)
class PolishBatchRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val basePatient: DemoPatient = DemoFixtures.patients.first()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun textLayoutOf(matcher: SemanticsMatcher): TextLayoutResult {
        val node = compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        return layouts.first()
    }

    private fun host360dp(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, 1f),
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        Box(
                            Modifier
                                .requiredSize(360.dp, 640.dp)
                                .clipToBounds()
                                .testTag(HOST_TAG),
                        ) {
                            content()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    // =========================================================================
    // (a) Scan GA chip and EFW in native report dialog
    // =========================================================================

    @Test
    fun reportDialog_scanWithoutGaAndEfw_showsNotRecorded_andScanWithGaAndEfw_showsValues() {
        val scanEmpty = DemoUltrasoundScan(
            id = "scan-polish-empty",
            patientId = basePatient.id,
            date = "2026-03-01",
            gestationalAgeWeeks = null,
            gestationalAgeDays = null,
            fetalNumber = null,
            presentation = "Cephalic",
            placentalSite = "Anterior",
            amnioticFluid = "Normal",
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
        )
        val scanFull = DemoUltrasoundScan(
            id = "scan-polish-full",
            patientId = basePatient.id,
            date = "2026-03-15",
            gestationalAgeWeeks = 32,
            gestationalAgeDays = 4,
            fetalNumber = "Single",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "140",
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = "1950",
            impression = "Appropriate for gestational age",
        )
        val patient = basePatient.copy(
            ultrasoundScans = listOf(scanFull, scanEmpty),
        )

        host360dp {
            PatientTwoPageReportDialog(
                patient = patient,
                onDismiss = {},
            )
        }

        // Switch to Page 2 where Ultrasound scans are presented
        compose.onNodeWithTag("reportPage2Chip", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val notRecordedStr = str(R.string.not_recorded)

        // Section header
        compose.onNodeWithText(str(R.string.report_section_scans), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Scan 1 (with GA and EFW): date 15/03/2026
        compose.onNodeWithText("15/03/2026", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ltrIsolate("32w + 4d"), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ltrIsolate("1950 g"), substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Scan 2 (empty GA and EFW): date 01/03/2026
        compose.onNodeWithText("01/03/2026", substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Both GA chip and EFW field show "Not recorded" for the empty scan
        compose.onAllNodes(hasText(notRecordedStr), useUnmergedTree = true)
            .fetchSemanticsNodes().let { nodes ->
                assertTrue("Expected at least 2 'Not recorded' occurrences in ultrasound card", nodes.size >= 2)
            }
    }

    // =========================================================================
    // (b) PatientHubHeader hero draws Arabic name right-aligned and English left-aligned
    // =========================================================================

    @Test
    fun patientHubHeader_hero_arabicPatientName_drawsRightAligned_within24dpOfRightPadding() {
        val arabicPatient = basePatient.copy(displayName = "منى رجب")
        val state = AncState(
            patients = listOf(arabicPatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
        )

        host360dp {
            PatientHubScreen(
                state = state,
                route = AncRoute.Workspace(arabicPatient.id, WorkspaceSection.EPISODE_OVERVIEW),
                onEvent = {},
                onStartVisit = { _, _ -> },
            )
        }

        val nameNode = compose.onNodeWithTag("hubPatientName", useUnmergedTree = true)
        nameNode.assertIsDisplayed()

        // Right edge within 24 dp of screen right padding (360dp - 16dp container padding = 344dp)
        val bounds = nameNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Arabic hubPatientName right edge (${bounds.right}) within 24dp of screen right padding (344dp)",
            abs(bounds.right.value - 344f) <= 24f,
        )

        // Text layout is RTL and right-aligned
        val layout = textLayoutOf(hasTestTag("hubPatientName"))
        assertEquals(TextAlign.Right, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))
        assertTrue(
            "Arabic text right edge near node layout width (${layout.getLineRight(0)} vs ${layout.size.width})",
            abs(layout.size.width - layout.getLineRight(0)) <= 4f,
        )
    }

    @Test
    fun patientHubHeader_hero_englishPatientName_drawsLeftAligned_nearLeftPadding() {
        val englishPatient = basePatient.copy(displayName = "Mona Ragab")
        val state = AncState(
            patients = listOf(englishPatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
        )

        host360dp {
            PatientHubScreen(
                state = state,
                route = AncRoute.Workspace(englishPatient.id, WorkspaceSection.EPISODE_OVERVIEW),
                onEvent = {},
                onStartVisit = { _, _ -> },
            )
        }

        val nameNode = compose.onNodeWithTag("hubPatientName", useUnmergedTree = true)
        nameNode.assertIsDisplayed()

        // Left edge near left container padding (16dp)
        val bounds = nameNode.getUnclippedBoundsInRoot()
        assertTrue(
            "English hubPatientName left edge (${bounds.left}) within 24dp of screen left padding (16dp)",
            abs(bounds.left.value - 16f) <= 24f,
        )

        // Text layout is LTR and left-aligned
        val layout = textLayoutOf(hasTestTag("hubPatientName"))
        assertEquals(TextAlign.Left, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Ltr, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, layout.getParagraphDirection(0))
        assertTrue(
            "English text left edge near 0: ${layout.getLineLeft(0)}",
            layout.getLineLeft(0) <= 4f,
        )
    }

    @Test
    fun patientHubHeader_collapsedTopBar_arabicNameRightAligned_andEnglishNameLeftAligned() {
        val arabicPatient = basePatient.copy(displayName = "منى رجب")
        val englishPatient = basePatient.copy(displayName = "Mona Ragab")

        var currentPatient by mutableStateOf(arabicPatient)
        val base = compose.activity.resources.displayMetrics.density
        val collapsedPx = 56f * base
        val headerState = PatientHubHeaderState(collapsedHeightPx = collapsedPx).apply {
            expandedHeightPx = 400f * base
            offsetPx = -10_000f
        }

        host360dp {
            PatientHubHeader(
                patient = currentPatient,
                headerState = headerState,
                selectedTab = PatientHubTab.OVERVIEW,
                onTabSelected = {},
                severeBp = null,
                onBack = {},
                onEditPatient = {},
                onOpenReport = {},
                onOpenAttentionDialog = {},
            )
        }

        compose.onNodeWithTag("stickyBackButton", useUnmergedTree = true).assertIsDisplayed()
        val arabicStickyLayout = textLayoutOf(hasText("منى رجب") and !hasTestTag("hubPatientName"))
        assertEquals(TextAlign.Right, arabicStickyLayout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, arabicStickyLayout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, arabicStickyLayout.getParagraphDirection(0))
        assertTrue(
            "Collapsed Arabic text right edge near width (${arabicStickyLayout.getLineRight(0)} vs ${arabicStickyLayout.size.width})",
            abs(arabicStickyLayout.size.width - arabicStickyLayout.getLineRight(0)) <= 4f,
        )

        compose.activity.runOnUiThread {
            currentPatient = englishPatient
        }
        compose.waitForIdle()

        compose.onNodeWithTag("stickyBackButton", useUnmergedTree = true).assertIsDisplayed()
        val englishStickyLayout = textLayoutOf(hasText("Mona Ragab") and !hasTestTag("hubPatientName"))
        assertEquals(TextAlign.Left, englishStickyLayout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Ltr, englishStickyLayout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Ltr, englishStickyLayout.getParagraphDirection(0))
        assertTrue(
            "Collapsed English text left edge near 0: ${englishStickyLayout.getLineLeft(0)}",
            englishStickyLayout.getLineLeft(0) <= 4f,
        )
    }

    // =========================================================================
    // (c) EditorPage subtitle draws Arabic name right-aligned
    // =========================================================================

    @Test
    fun editorPage_subtitle_arabicPatientName_drawsRightAligned() {
        val arabicPatient = basePatient.copy(displayName = "منى رجب")
        val statusState = AncState(
            patients = listOf(arabicPatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.StatusEditor(arabicPatient.id, "archived"),
            statusDraft = PatientStatusDraft(
                editorSessionId = "session-status-editor-arabic",
                patientId = arabicPatient.id,
                baseRevision = arabicPatient.revision,
                targetStatus = "archived",
                reason = "",
                errors = emptySet(),
            ),
        )

        host360dp {
            PatientStatusEditor(
                state = statusState,
                route = statusState.route as AncRoute.StatusEditor,
                onEvent = {},
            )
        }

        val nameNode = compose.onNodeWithText("منى رجب", useUnmergedTree = true)
        nameNode.assertIsDisplayed()

        // Subtitle node bounds right edge within 24dp of right container padding
        val bounds = nameNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Arabic subtitle right edge (${bounds.right}) within 24dp of screen right edge (360dp)",
            360f - bounds.right.value <= 24f,
        )

        // Subtitle text layout is RTL and right-aligned
        val layout = textLayoutOf(hasText("منى رجب"))
        assertEquals(TextAlign.Right, layout.layoutInput.style.textAlign)
        assertEquals(TextDirection.Rtl, layout.layoutInput.style.textDirection)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))
        assertTrue(
            "Arabic subtitle lineRight near layout width (${layout.getLineRight(0)} vs ${layout.size.width})",
            abs(layout.size.width - layout.getLineRight(0)) <= 4f,
        )
    }

    // =========================================================================
    // (d) Date labels: Visit date, Scan date and Delivery date do not contain "YYYY"
    // =========================================================================

    @Test
    fun dateLabels_visitScanDelivery_doNotContainYyyy() {
        val visitLabel = str(R.string.visit_date)
        val scanLabel = str(R.string.scan_date)
        val deliveryLabel = str(R.string.delivery_outcome_date_label)

        assertEquals("Visit date", visitLabel)
        assertEquals("Scan date", scanLabel)
        assertEquals("Delivery date", deliveryLabel)

        assertFalse("visit_date contains 'YYYY': $visitLabel", visitLabel.contains("YYYY", ignoreCase = true))
        assertFalse("scan_date contains 'YYYY': $scanLabel", scanLabel.contains("YYYY", ignoreCase = true))
        assertFalse("delivery_outcome_date_label contains 'YYYY': $deliveryLabel", deliveryLabel.contains("YYYY", ignoreCase = true))

        // UI rendering verification: host UltrasoundEditor to verify the field label
        val ultrasoundState = AncState(
            patients = listOf(basePatient),
            vaultState = VaultState.Unlocked(sessionEpoch = 1L),
            route = AncRoute.UltrasoundEditor(basePatient.id, "scan-new-date-test", isNew = true),
            ultrasoundDraft = UltrasoundDraft(
                editorSessionId = "session-date-test",
                patientId = basePatient.id,
                scanId = "scan-new-date-test",
                basePatientRevision = basePatient.revision,
                baseScanRevision = null,
                original = UltrasoundDraftValues(),
                values = UltrasoundDraftValues(),
                errors = emptySet(),
            ),
            savePhase = SavePhase.IDLE,
        )

        host360dp {
            UltrasoundEditor(
                state = ultrasoundState,
                route = ultrasoundState.route as AncRoute.UltrasoundEditor,
                onEvent = {},
            )
        }

        compose.onNodeWithText("Scan date", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Scan date (YYYY-MM-DD)", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("YYYY", substring = true) and hasText("Scan", substring = true), useUnmergedTree = true).assertDoesNotExist()
    }

    // =========================================================================
    // (e) Meds tab: sentence case title and no "+" text on prescribe buttons
    // =========================================================================

    @Test
    fun medsTab_activePrescriptionsTitleIsSentenceCase_andAddCustomMedicationBtnHasNoPlus_andMinTouchTarget() {
        host360dp {
            MedsTab(
                patient = basePatient,
                canEdit = true,
                actionsAllowed = true,
                medicationDraft = null,
                savePhase = SavePhase.IDLE,
                saveError = null,
                quickSave = null,
                stopUndo = null,
                onEvent = {},
            )
        }

        // Active title is sentence case (lowercase 'p' in prescriptions)
        val titleNode = compose.onNodeWithTag("medsActiveTitle", useUnmergedTree = true)
        titleNode.assertIsDisplayed()
        val titleText = titleNode.fetchSemanticsNode().config[SemanticsProperties.Text].first().text
        assertTrue("Expected title to start with 'Active prescription': $titleText", titleText.startsWith("Active prescription"))
        assertFalse("Expected no capital P after first word in active prescriptions title: $titleText", titleText.contains("Prescription"))

        // addCustomMedicationBtn has testTag, >=48dp touch target, and no "+" text
        val addBtn = compose.onNodeWithTag("addCustomMedicationBtn", useUnmergedTree = true)
        addBtn.assertIsDisplayed()
        addBtn.assertHeightIsAtLeast(48.dp)
        addBtn.assertWidthIsAtLeast(48.dp)

        val buttonTextNodes = compose.onAllNodes(
            hasAnyAncestor(hasTestTag("addCustomMedicationBtn")) and
                (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        val buttonTexts = buttonTextNodes.flatMap { it.config.getOrNull(SemanticsProperties.Text) ?: emptyList() }.map { it.text }
        assertTrue("addCustomMedicationBtn texts: $buttonTexts", buttonTexts.isNotEmpty())
        assertTrue("addCustomMedicationBtn must not contain '+': $buttonTexts", buttonTexts.none { it.contains("+") })
    }

    @Test
    fun medsTab_emptyStatePrescribeButtonHasNoPlus_andMinTouchTarget() {
        val emptyPatient = basePatient.copy(medications = emptyList())

        host360dp {
            MedsTab(
                patient = emptyPatient,
                canEdit = true,
                actionsAllowed = true,
                medicationDraft = null,
                savePhase = SavePhase.IDLE,
                saveError = null,
                quickSave = null,
                stopUndo = null,
                onEvent = {},
            )
        }

        // medEmptyPrescribe has testTag, >=48dp touch target, and no "+" text
        val emptyPrescribeBtn = compose.onNodeWithTag("medEmptyPrescribe", useUnmergedTree = true)
        emptyPrescribeBtn.assertIsDisplayed()
        emptyPrescribeBtn.assertHeightIsAtLeast(48.dp)
        emptyPrescribeBtn.assertWidthIsAtLeast(48.dp)

        val emptyTextNodes = compose.onAllNodes(
            hasAnyAncestor(hasTestTag("medEmptyPrescribe")) and
                (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        val emptyTexts = emptyTextNodes.flatMap { it.config.getOrNull(SemanticsProperties.Text) ?: emptyList() }.map { it.text }
        assertTrue("medEmptyPrescribe texts: $emptyTexts", emptyTexts.isNotEmpty())
        assertTrue("medEmptyPrescribe must not contain '+': $emptyTexts", emptyTexts.none { it.contains("+") })
    }

    private companion object {
        const val HOST_TAG = "polishBatchAppHost"
    }
}
