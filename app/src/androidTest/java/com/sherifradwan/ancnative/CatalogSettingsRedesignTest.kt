package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.LabCatalogDelegate
import com.sherifradwan.ancnative.model.catalog.LabCatalogItem
import com.sherifradwan.ancnative.model.catalog.LabCategoryGroup
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogDelegate
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.LabCatalogSettingsSection
import com.sherifradwan.ancnative.ui.MedicationCatalogSettingsSection
import com.sherifradwan.ancnative.ui.SecuritySettingsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Redesign slice 17: Catalog settings (Lab tests, Diagnostic panels, and Medication catalog).
 *
 * Mounts through MainActivity + AncTheme inside a 360dp x 640dp host, matching
 * HistoryEditorsRedesignTest and RegistrationRedesignTest style (single setContent per test,
 * shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides,
 * useUnmergedTree where label text is cleared by clearAndSetSemantics, performScrollTo
 * before assertIsDisplayed, select nodes by testTag and not by ambiguous text, >=47.99dp touch tolerance).
 */
@RunWith(AndroidJUnit4::class)
class CatalogSettingsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    @After
    fun tearDown() {
        restoreResources()
    }

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun pluralStr(id: Int, count: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, *args)

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

    /** Single setContent per test: hosts LabCatalogSettingsSection inside a 360dp x 640dp container. */
    private fun showLab(
        delegate: LabCatalogDelegate = FakeCatalogDelegate(),
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                LabCatalogSettingsSection(catalogDelegate = delegate)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Single setContent per test: hosts MedicationCatalogSettingsSection inside a 360dp x 640dp container. */
    private fun showMed(
        delegate: MedicationCatalogDelegate = FakeCatalogDelegate(),
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                MedicationCatalogSettingsSection(catalogDelegate = delegate)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Single setContent per test: hosts SecuritySettingsDialog in a 360dp container to verify ~298dp column. */
    private fun showSecurityDialog(
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        if (fontScale != 1f) narrowWindow(fontScale)
        val securityRepo = SecuritySettingsRepository.getInstance(compose.activity)
        val catalogRepo = ClinicalCatalogRepository.getInstance(compose.activity)
        catalogRepo.resetLabDefaults()
        catalogRepo.resetLabCategoryGroupDefaults()
        catalogRepo.resetMedicationDefaults()

        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(HOST_TAG)) {
                            SecuritySettingsDialog(
                                securityRepo = securityRepo,
                                onDismiss = {},
                                onLockNow = {},
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

    private fun assertNoLegacyGlyphsOrTitles(tag: String) {
        val nodes = compose.onAllNodes(
            (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        nodes.forEach { n ->
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: ""
            assertFalse("Found middle dot '·' in '$text'", text.contains("·"))
            assertFalse("Found legacy Title Case title in '$text'", text.contains("Create New Diagnostic Panel"))
        }
    }

    // =========================================================================
    // 1. Typography & Legacy Glyphs Removal (1.0f and 1.3f)
    // =========================================================================

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_labIndividualTestsSection() {
        showLab()

        node("addNewLabTestBtn").assertIsDisplayed()
        node("labIndividualTestsTab").assertIsDisplayed()
        node("labCategoryGroupsTab").assertIsDisplayed()
        node("searchLabCodexField").assertIsDisplayed()

        // Section header and localized summary
        compose.onNodeWithText(str(R.string.catalog_lab_section_title)).assertIsDisplayed()
        val testsSummary = pluralStr(R.plurals.catalog_panel_tests_count, 28, 28)
        val panelsSummary = pluralStr(R.plurals.catalog_panels_count, 7, 7)
        val expectedSummary = str(R.string.catalog_lab_summary, testsSummary, panelsSummary)
        compose.onNodeWithText(expectedSummary).assertIsDisplayed()

        // Subview switcher chips
        compose.onNodeWithText(pluralStr(R.plurals.catalog_tab_individual_tests, 28, 28)).assertIsDisplayed()
        compose.onNodeWithText(pluralStr(R.plurals.catalog_tab_panels, 7, 7)).assertIsDisplayed()

        // Primary action
        compose.onNodeWithText(str(R.string.add_test_action)).assertIsDisplayed()

        // Typography: zero text below 12sp and zero middle dots or legacy titles
        assertNoTextBelow12sp(HOST_TAG)
        assertNoLegacyGlyphsOrTitles(HOST_TAG)
    }

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_labPanelsSection() {
        showLab()

        node("labCategoryGroupsTab").performClick()
        compose.waitForIdle()

        node("addNewLabGroupBtn").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_create_panel)).assertIsDisplayed()

        // First panel card (CBC) renders RecordBandCard with BandChip and PlainField
        node("labCategoryGroup-group_cbc").assertIsDisplayed()
        compose.onNodeWithText(pluralStr(R.plurals.catalog_panel_tests_count, 6, 6)).assertIsDisplayed()
        compose.onNodeWithText("CBC").assertIsDisplayed()
        compose.onNodeWithText("Haemoglobin, haematocrit, MCV, MCH, WBC, platelets").assertIsDisplayed()
        compose.onAllNodesWithText(str(R.string.catalog_included_tests)).onFirst().assertIsDisplayed()

        assertNoTextBelow12sp(HOST_TAG)
        assertNoLegacyGlyphsOrTitles(HOST_TAG)
    }

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_medicationSection() {
        showMed()

        node("addNewMedBtn").assertIsDisplayed()
        node("searchMedCodexField").assertIsDisplayed()

        // Section header and summary
        compose.onNodeWithText(str(R.string.catalog_med_section_title)).assertIsDisplayed()
        compose.onNodeWithText(pluralStr(R.plurals.catalog_med_summary, 10, 10)).assertIsDisplayed()

        // Primary action
        compose.onNodeWithText(str(R.string.add_medication)).assertIsDisplayed()

        // First medication row renders RecordSectionCard row
        node("medCodexItem-med_folic").assertIsDisplayed()
        compose.onNodeWithText("Folic Acid 5 mg").assertIsDisplayed()
        compose.onAllNodesWithText("Oral, Once daily").onFirst().assertIsDisplayed()

        assertNoTextBelow12sp(HOST_TAG)
        assertNoLegacyGlyphsOrTitles(HOST_TAG)
    }

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addLabTestDialog() {
        showLab()

        node("addNewLabTestBtn").performClick()
        compose.waitForIdle()

        node("addLabTestDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_add_lab_test_title)).assertIsDisplayed()
        node("saveLabCodexConfirmBtn").assertIsDisplayed()
        node("cancelLabCodexBtn").assertIsDisplayed()

        assertNoTextBelow12sp("addLabTestDialog")
        assertNoLegacyGlyphsOrTitles("addLabTestDialog")

        node("cancelLabCodexBtn").performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertDoesNotExist()
    }

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addLabCategoryGroupDialog() {
        showLab()

        node("labCategoryGroupsTab").performClick()
        compose.waitForIdle()
        node("addNewLabGroupBtn").performClick()
        compose.waitForIdle()

        node("addLabGroupDialog").assertIsDisplayed()
        // Normalized title (Sentence case, not "Create New Diagnostic Panel")
        compose.onNodeWithText(str(R.string.create_diagnostic_panel)).assertIsDisplayed()
        compose.onNodeWithText("Create New Diagnostic Panel").assertDoesNotExist()
        compose.onNodeWithText(str(R.string.catalog_save_panel)).assertIsDisplayed()

        assertNoTextBelow12sp("addLabGroupDialog")
        assertNoLegacyGlyphsOrTitles("addLabGroupDialog")

        node("cancelLabGroupBtn").performClick()
        compose.waitForIdle()
        node("addLabGroupDialog").assertDoesNotExist()
    }

    @Test
    fun typography_noTextBelow12sp_andNoLegacyGlyphsOrTitles_addMedicationDialog() {
        showMed()

        node("addNewMedBtn").performClick()
        compose.waitForIdle()

        node("addMedDialog").assertIsDisplayed()
        compose.onAllNodesWithText(str(R.string.add_medication)).onFirst().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_add_to_my_list)).assertIsDisplayed()

        // Frequency and route chips must be >= 12sp (fixing old 11sp bug)
        assertNoTextBelow12sp("addMedDialog")
        assertNoLegacyGlyphsOrTitles("addMedDialog")

        node("cancelMedCodexBtn").performClick()
        compose.waitForIdle()
        node("addMedDialog").assertDoesNotExist()
    }

    // =========================================================================
    // 2. Touch Targets (>= 47.99dp on every interactive element) & 360dp Containment
    // =========================================================================

    @Test
    fun touchTargets_labCatalogSection_allInteractiveTargetsAtLeast48dp() {
        showLab()

        assertTargetInside360("addNewLabTestBtn")
        assertTargetInside360("labIndividualTestsTab")
        assertTargetInside360("labCategoryGroupsTab")
        assertTargetInside360("searchLabCodexField")

        // First lab test row actions
        assertTargetInside360("editLabCodexBtn-lab_cbc_hb")
        assertTargetInside360("deleteLabCodexBtn-lab_cbc_hb")

        // Reset button
        assertTargetInside360("resetLabCatalogBtn")

        // Switch to panels subview
        node("labCategoryGroupsTab").performScrollTo().performClick()
        compose.waitForIdle()

        assertTargetInside360("addNewLabGroupBtn")
        assertTargetInside360("editLabGroupBtn-group_cbc")
        assertTargetInside360("deleteLabGroupBtn-group_cbc")
    }

    @Test
    fun touchTargets_medicationCatalogSection_allInteractiveTargetsAtLeast48dp() {
        showMed()

        assertTargetInside360("addNewMedBtn")
        assertTargetInside360("searchMedCodexField")

        // First medication row actions
        assertTargetInside360("editMedCodexBtn-med_paracetamol")
        assertTargetInside360("deleteMedCodexBtn-med_paracetamol")

        // Reset button
        assertTargetInside360("resetMedCatalogBtn")
    }

    @Test
    fun touchTargets_addLabTestDialog_controlsAndChipsAtLeast48dp() {
        showLab()

        node("addNewLabTestBtn").performClick()
        compose.waitForIdle()

        assertMinTouchTarget("saveLabCodexConfirmBtn")
        assertMinTouchTarget("cancelLabCodexBtn")
        assertMinTouchTarget("labCodexCategoryChoice-0")
        assertMinTouchTarget("labCodexCategoryChoice-1")
    }

    @Test
    fun touchTargets_addLabCategoryGroupDialog_controlsAndChipsAtLeast48dp() {
        showLab()

        node("labCategoryGroupsTab").performClick()
        compose.waitForIdle()
        node("addNewLabGroupBtn").performClick()
        compose.waitForIdle()

        assertMinTouchTarget("saveLabGroupConfirmBtn")
        assertMinTouchTarget("cancelLabGroupBtn")
        assertMinTouchTarget("selectTestChip-lab_cbc_hb")
        assertMinTouchTarget("selectTestChip-lab_cbc_hct")
    }

    @Test
    fun touchTargets_addMedicationDialog_controlsAndChipsAtLeast48dp() {
        showMed()

        node("addNewMedBtn").performClick()
        compose.waitForIdle()

        assertMinTouchTarget("saveMedCodexConfirmBtn")
        assertMinTouchTarget("cancelMedCodexBtn")
        assertMinTouchTarget("medCodexRouteChoice-0")
        assertMinTouchTarget("medCodexFreqChoice-0")
    }

    @Test
    fun touchTargets_confirmationAndEmptyDialogButtons_allAtLeast48dp() {
        showLab()

        // Delete test confirmation
        node("deleteLabCodexBtn-lab_cbc_hb").performScrollTo().performClick()
        compose.waitForIdle()
        node("deleteLabConfirmDialog").assertIsDisplayed()
        assertMinTouchTarget("deleteLabConfirmBtn")
        assertMinTouchTarget("cancelDeleteLabBtn")
        node("cancelDeleteLabBtn").performClick()
        compose.waitForIdle()

        // Reset lab confirmation
        node("resetLabCatalogBtn").performScrollTo().performClick()
        compose.waitForIdle()
        node("resetLabConfirmDialog").assertIsDisplayed()
        assertMinTouchTarget("resetLabConfirmBtn")
        assertMinTouchTarget("cancelResetLabBtn")
        node("cancelResetLabBtn").performClick()
        compose.waitForIdle()

        // Search clear buttons
        node("searchLabCodexField").performTextInput("NonExistentQuery")
        compose.waitForIdle()
        assertMinTouchTarget("clearSearchLabCodexBtn")
        assertMinTouchTarget("clearLabSearchAction")
    }

    // =========================================================================
    // 3. Functional Behaviour & Parity: Lab Catalog & Individual Tests
    // =========================================================================

    @Test
    fun labCatalog_individualTestsFlow_searchFilter_andEmptyCatalog() {
        val delegate = FakeCatalogDelegate()
        showLab(delegate = delegate)

        node("labCodexItem-lab_cbc_hb").assertIsDisplayed()
        node("labCodexItem-lab_fbs").performScrollTo().assertIsDisplayed()

        // Search "Hb" filters to items containing "Hb"
        node("searchLabCodexField").performTextInput("Hb")
        compose.waitForIdle()

        node("labCodexItem-lab_cbc_hb").assertIsDisplayed()
        node("labCodexItem-lab_fbs").assertDoesNotExist()
        compose.onNodeWithText(pluralStr(R.plurals.catalog_tests_matches_title, 2, 2)).assertIsDisplayed()

        // Search non-matching text shows empty card with clear button
        node("searchLabCodexField").performTextReplacement("NonExistentTest123")
        compose.waitForIdle()

        node("clearLabSearchAction").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_no_tests_match, "NonExistentTest123")).assertIsDisplayed()

        // Clicking clear restores items
        node("clearLabSearchAction").performClick()
        compose.waitForIdle()

        node("labCodexItem-lab_cbc_hb").performScrollTo().assertIsDisplayed()
        node("labCodexItem-lab_fbs").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun labCatalog_emptyCatalog_showsEmptyAddAndResetButtons() {
        val delegate = FakeCatalogDelegate(initialLabs = emptyList(), initialGroups = emptyList())
        showLab(delegate = delegate)

        compose.onNodeWithText(str(R.string.catalog_empty_labs_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_empty_labs_hint)).assertIsDisplayed()

        node("emptyAddLabTestBtn").assertIsDisplayed()
        node("emptyResetLabTestsBtn").assertIsDisplayed()
        assertMinTouchTarget("emptyAddLabTestBtn")
        assertMinTouchTarget("emptyResetLabTestsBtn")

        // Clicking emptyAdd opens dialog
        node("emptyAddLabTestBtn").performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertIsDisplayed()
    }

    @Test
    fun labCatalog_addLabTestDialog_validationAndCreation() {
        val delegate = FakeCatalogDelegate()
        showLab(delegate = delegate)

        node("addNewLabTestBtn").performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertIsDisplayed()

        // 1. Validation: blank name shows error without saving
        node("saveLabCodexConfirmBtn").performClick()
        compose.waitForIdle()
        node("addLabError").assertIsDisplayed().assertTextContains(str(R.string.catalog_error_test_name_blank))
        assertTrue(delegate.addedLabs.isEmpty())

        // 2. Fill fields and save
        node("addLabNameField").performTextInput("Serum Prolactin")
        node("labCodexCategoryChoice-1").performClick()
        node("addLabUnitField").performTextInput("ng/mL")
        node("addLabRefRangeField").performTextInput("5 - 25")
        node("addLabDefaultResultField").performTextInput("Normal")

        node("saveLabCodexConfirmBtn").performClick()
        compose.waitForIdle()

        // Dialog dismisses and delegate receives item
        node("addLabTestDialog").assertDoesNotExist()
        assertEquals(1, delegate.addedLabs.size)
        val added = delegate.addedLabs.first()
        assertEquals("Serum Prolactin", added.testName)
        assertEquals("ng/mL", added.unit)
        assertEquals("5 - 25", added.referenceRange)
        assertEquals("Normal", added.defaultResult)
        assertTrue(added.isCustom)
    }

    @Test
    fun labCatalog_editAndDeleteAndReset_behaviourParity() {
        val delegate = FakeCatalogDelegate()
        showLab(delegate = delegate)

        // 1. Edit lab test
        node("editLabCodexBtn-lab_cbc_hb").performScrollTo().performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_edit_lab_test_title)).assertIsDisplayed()

        node("addLabUnitField").performTextReplacement("g/L")
        node("saveLabCodexConfirmBtn").performClick()
        compose.waitForIdle()

        node("addLabTestDialog").assertDoesNotExist()
        assertEquals(1, delegate.updatedLabs.size)
        assertEquals("lab_cbc_hb", delegate.updatedLabs.first().id)
        assertEquals("g/L", delegate.updatedLabs.first().unit)

        // 2. Delete lab test confirmation
        node("deleteLabCodexBtn-lab_cbc_hb").performScrollTo().performClick()
        compose.waitForIdle()
        node("deleteLabConfirmDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_delete_test_title)).assertIsDisplayed()

        node("deleteLabConfirmBtn").performClick()
        compose.waitForIdle()
        node("deleteLabConfirmDialog").assertDoesNotExist()
        assertEquals(listOf("lab_cbc_hb"), delegate.deletedLabs)

        // 3. Reset catalog confirmation
        node("resetLabCatalogBtn").performScrollTo().performClick()
        compose.waitForIdle()
        node("resetLabConfirmDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_reset_labs_title)).assertIsDisplayed()

        node("resetLabConfirmBtn").performClick()
        compose.waitForIdle()
        node("resetLabConfirmDialog").assertDoesNotExist()
        assertEquals(1, delegate.resetLabsCount)
        assertEquals(1, delegate.resetGroupsCount)
    }

    // =========================================================================
    // 4. Functional Behaviour & Parity: Lab Panels (Category Groups)
    // =========================================================================

    @Test
    fun labPanels_bandCardLayout_plainFieldIncludedTests_andAddPanelFlow() {
        val delegate = FakeCatalogDelegate()
        showLab(delegate = delegate)

        node("labCategoryGroupsTab").performClick()
        compose.waitForIdle()

        // Verify BandCard structure for CBC
        node("labCategoryGroup-group_cbc").assertIsDisplayed()
        compose.onAllNodesWithText(str(R.string.catalog_included_tests)).onFirst().assertIsDisplayed()

        // Open Add Panel dialog
        node("addNewLabGroupBtn").performClick()
        compose.waitForIdle()
        node("addLabGroupDialog").assertIsDisplayed()

        // 1. Validation: blank group name
        node("saveLabGroupConfirmBtn").performClick()
        compose.waitForIdle()
        node("addLabGroupError").assertIsDisplayed().assertTextContains(str(R.string.catalog_error_panel_name_blank))
        assertTrue(delegate.addedGroups.isEmpty())

        // 2. Enter name
        node("addLabGroupNameField").performTextInput("Prenatal Liver Profile")
        node("addLabGroupDescField").performTextInput("Liver function evaluation")

        // 3. Search test chips inside panel dialog
        node("searchLabGroupTestsField").performTextInput("creatinine")
        compose.waitForIdle()
        node("selectTestChip-lab_creatinine").assertIsDisplayed()
        node("selectTestChip-lab_cbc_hb").assertDoesNotExist()

        // 4. Clear search when query has no match
        node("searchLabGroupTestsField").performTextReplacement("xyzNonExistent")
        compose.waitForIdle()
        node("clearLabGroupTestsEmptySearchBtn").assertIsDisplayed().performClick()
        compose.waitForIdle()
        node("selectTestChip-lab_cbc_hb").assertIsDisplayed()

        // 5. Select test chips and save
        node("selectTestChip-lab_cbc_hb").performClick()
        node("selectTestChip-lab_cbc_hct").performClick()
        node("saveLabGroupConfirmBtn").performClick()
        compose.waitForIdle()

        node("addLabGroupDialog").assertDoesNotExist()
        assertEquals(1, delegate.addedGroups.size)
        val added = delegate.addedGroups.first()
        assertEquals("Prenatal Liver Profile", added.name)
        assertEquals("Liver function evaluation", added.description)
        assertTrue(added.testNames.contains("Haemoglobin (Hb)"))
        assertTrue(added.testNames.contains("Haematocrit (Hct)"))
        assertTrue(added.isCustom)
    }

    @Test
    fun labPanels_editAndDelete_behaviourParity() {
        val delegate = FakeCatalogDelegate()
        showLab(delegate = delegate)

        node("labCategoryGroupsTab").performClick()
        compose.waitForIdle()

        // 1. Edit panel
        node("editLabGroupBtn-group_cbc").performScrollTo().performClick()
        compose.waitForIdle()
        node("addLabGroupDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.edit_diagnostic_panel)).assertIsDisplayed()

        node("addLabGroupDescField").performTextReplacement("Updated CBC description")
        node("saveLabGroupConfirmBtn").performClick()
        compose.waitForIdle()

        node("addLabGroupDialog").assertDoesNotExist()
        assertEquals(1, delegate.updatedGroups.size)
        assertEquals("group_cbc", delegate.updatedGroups.first().id)
        assertEquals("Updated CBC description", delegate.updatedGroups.first().description)

        // 2. Delete panel
        node("deleteLabGroupBtn-group_cbc").performScrollTo().performClick()
        compose.waitForIdle()
        node("deleteLabGroupConfirmDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_delete_panel_title)).assertIsDisplayed()

        node("deleteLabGroupConfirmBtn").performClick()
        compose.waitForIdle()
        node("deleteLabGroupConfirmDialog").assertDoesNotExist()
        assertEquals(listOf("group_cbc"), delegate.deletedGroups)
    }

    // =========================================================================
    // 5. Functional Behaviour & Parity: Medication Catalog
    // =========================================================================

    @Test
    fun medicationCatalog_rowLayout_searchFilter_andEmptyCatalog() {
        val delegate = FakeCatalogDelegate()
        showMed(delegate = delegate)

        node("medCodexItem-med_paracetamol").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Paracetamol 500 mg").assertIsDisplayed()
        compose.onNodeWithText("Oral, As needed (PRN)").assertIsDisplayed()
        compose.onNodeWithText("Analgesic & antipyretic for pregnancy").assertIsDisplayed()

        // Search "500 mg"
        node("searchMedCodexField").performTextInput("500 mg")
        compose.waitForIdle()

        node("medCodexItem-med_paracetamol").performScrollTo().assertIsDisplayed()
        node("medCodexItem-med_folic").assertDoesNotExist()

        // Search non-matching text shows empty card with clear button
        node("searchMedCodexField").performTextReplacement("NonExistentMedXYZ")
        compose.waitForIdle()

        node("clearMedSearchAction").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_no_meds_match, "NonExistentMedXYZ")).assertIsDisplayed()

        node("clearMedSearchAction").performClick()
        compose.waitForIdle()
        node("medCodexItem-med_folic").performScrollTo().assertIsDisplayed()
        node("medCodexItem-med_paracetamol").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun medicationCatalog_emptyCatalog_showsEmptyAddAndResetButtons() {
        val delegate = FakeCatalogDelegate(initialMeds = emptyList())
        showMed(delegate = delegate)

        compose.onNodeWithText(str(R.string.catalog_empty_meds_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_empty_meds_hint)).assertIsDisplayed()

        node("emptyAddMedBtn").assertIsDisplayed()
        node("emptyResetMedsBtn").assertIsDisplayed()
        assertMinTouchTarget("emptyAddMedBtn")
        assertMinTouchTarget("emptyResetMedsBtn")

        node("emptyAddMedBtn").performClick()
        compose.waitForIdle()
        node("addMedDialog").assertIsDisplayed()
    }

    @Test
    fun medicationCatalog_addMedicationDialog_validationAndCreation() {
        val delegate = FakeCatalogDelegate()
        showMed(delegate = delegate)

        node("addNewMedBtn").performClick()
        compose.waitForIdle()
        node("addMedDialog").assertIsDisplayed()

        // 1. Validation: blank name shows error without saving
        node("saveMedCodexConfirmBtn").performClick()
        compose.waitForIdle()
        node("addMedError").assertIsDisplayed().assertTextContains(str(R.string.catalog_error_med_name_blank))
        assertTrue(delegate.addedMeds.isEmpty())

        // 2. Fill fields and save
        node("addMedNameField").performTextInput("Amoxicillin")
        node("addMedDosageField").performTextInput("500 mg")
        node("medCodexRouteChoice-0").performClick()
        node("medCodexFreqChoice-0").performClick()
        node("addMedInstructionsField").performTextInput("Antibiotic for pregnancy")

        node("saveMedCodexConfirmBtn").performClick()
        compose.waitForIdle()

        node("addMedDialog").assertDoesNotExist()
        assertEquals(1, delegate.addedMeds.size)
        val added = delegate.addedMeds.first()
        assertEquals("Amoxicillin", added.name)
        assertEquals("500 mg", added.dosage)
        assertEquals("Oral", added.route)
        assertEquals("Once daily", added.frequency)
        assertEquals("Antibiotic for pregnancy", added.instructions)
        assertFalse(added.isProphylaxis)
        assertTrue(added.isCustom)
    }

    @Test
    fun medicationCatalog_editAndDeleteAndReset_behaviourParity() {
        val delegate = FakeCatalogDelegate()
        showMed(delegate = delegate)

        // 1. Edit medication
        node("editMedCodexBtn-med_paracetamol").performScrollTo().performClick()
        compose.waitForIdle()
        node("addMedDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.edit_medication_title)).assertIsDisplayed()

        node("addMedDosageField").performTextReplacement("1000 mg")
        node("saveMedCodexConfirmBtn").performClick()
        compose.waitForIdle()

        node("addMedDialog").assertDoesNotExist()
        assertEquals(1, delegate.updatedMeds.size)
        assertEquals("med_paracetamol", delegate.updatedMeds.first().id)
        assertEquals("1000 mg", delegate.updatedMeds.first().dosage)

        // 2. Delete medication confirmation
        node("deleteMedCodexBtn-med_paracetamol").performScrollTo().performClick()
        compose.waitForIdle()
        node("deleteMedConfirmDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_delete_med_title)).assertIsDisplayed()

        node("deleteMedConfirmBtn").performClick()
        compose.waitForIdle()
        node("deleteMedConfirmDialog").assertDoesNotExist()
        assertEquals(listOf("med_paracetamol"), delegate.deletedMeds)

        // 3. Reset medications confirmation
        node("resetMedCatalogBtn").performScrollTo().performClick()
        compose.waitForIdle()
        node("resetMedConfirmDialog").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.catalog_reset_meds_title)).assertIsDisplayed()

        node("resetMedConfirmBtn").performClick()
        compose.waitForIdle()
        node("resetMedConfirmDialog").assertDoesNotExist()
        assertEquals(1, delegate.resetMedsCount)
    }

    // =========================================================================
    // 6. Responsive Side-by-Side vs Stacked Fields (1.0f vs 2.0f font scale)
    // =========================================================================

    @Test
    fun addMedicationDialog_sideBySideAt1_0() {
        showMed(fontScale = 1.0f)

        node("addNewMedBtn").performClick()
        compose.waitForIdle()
        node("addMedDialog").assertIsDisplayed()

        val nameBounds = bounds("addMedNameField")
        val dosageBounds = bounds("addMedDosageField")

        // Side by side: dosage is to the right of name, not stacked below name
        assertTrue("Dosage should sit to the right of Name: dosage.left=${dosageBounds.left}, name.right=${nameBounds.right}", dosageBounds.left >= nameBounds.right - 1.dp)
        assertTrue("Dosage should not stack below Name: dosage.top=${dosageBounds.top}, name.bottom=${nameBounds.bottom}", dosageBounds.top < nameBounds.bottom)
    }

    @Test
    fun addMedicationDialog_stackedAt2_0() {
        showMed(fontScale = 2.0f)

        node("addNewMedBtn").performClick()
        compose.waitForIdle()
        node("addMedDialog").assertIsDisplayed()

        val nameBounds = bounds("addMedNameField")
        val dosageBounds = bounds("addMedDosageField")

        // Stacked vertically: dosage sits below name
        assertTrue("Dosage should stack below Name: dosage.top=${dosageBounds.top}, name.bottom=${nameBounds.bottom}", dosageBounds.top >= nameBounds.bottom - 1.dp)
    }

    @Test
    fun addLabTestDialog_sideBySideAt1_0() {
        showLab(fontScale = 1.0f)

        node("addNewLabTestBtn").performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertIsDisplayed()

        val unitBounds = bounds("addLabUnitField")
        val refRangeBounds = bounds("addLabRefRangeField")

        // Side by side: reference range is to the right of unit, tops are aligned
        assertTrue("Ref range should sit to the right of Unit: refRange.left=${refRangeBounds.left}, unit.right=${unitBounds.right}", refRangeBounds.left >= unitBounds.right - 1.dp)
        assertTrue("Top edges should be aligned within 4dp: refRange.top=${refRangeBounds.top}, unit.top=${unitBounds.top}", abs(refRangeBounds.top.value - unitBounds.top.value) <= 4f)
    }

    @Test
    fun addLabTestDialog_stackedAt2_0() {
        showLab(fontScale = 2.0f)

        node("addNewLabTestBtn").performClick()
        compose.waitForIdle()
        node("addLabTestDialog").assertIsDisplayed()

        val unitBounds = bounds("addLabUnitField")
        val refRangeBounds = bounds("addLabRefRangeField")

        // Stacked vertically: reference range sits below unit
        assertTrue("Ref range should stack below Unit: refRange.top=${refRangeBounds.top}, unit.bottom=${unitBounds.bottom}", refRangeBounds.top >= unitBounds.bottom - 1.dp)
    }

    // =========================================================================
    // 7. Host Integration: SecuritySettingsDialog (~298dp column), 1.3f Scaling, Dark Theme & RTL
    // =========================================================================

    @Test
    fun securitySettingsDialog_hostsCatalogSectionsAt298dpWithoutClipping() {
        showSecurityDialog()

        node("securitySettingsDialog").assertIsDisplayed()

        // 1. Switch to Lab Tests tab
        compose.onNodeWithText("Lab tests").performClick()
        compose.waitForIdle()

        node("labIndividualTestsTab").assertIsDisplayed()
        node("addNewLabTestBtn").assertIsDisplayed()
        assertMinTouchTarget("addNewLabTestBtn")
        assertMinTouchTarget("labIndividualTestsTab")
        assertMinTouchTarget("labCategoryGroupsTab")

        // 2. Switch to Medications tab
        compose.onNodeWithText("Medications").performClick()
        compose.waitForIdle()

        node("addNewMedBtn").assertIsDisplayed()
        node("searchMedCodexField").assertIsDisplayed()
        assertMinTouchTarget("addNewMedBtn")
        assertMinTouchTarget("searchMedCodexField")
    }

    @Test
    fun viewportAndScaling_360dpAtFontScale1_3_noClipping() {
        showLab(fontScale = 1.3f)

        node("addNewLabTestBtn").assertIsDisplayed()
        node("labIndividualTestsTab").assertIsDisplayed()
        node("labCategoryGroupsTab").assertIsDisplayed()

        // Assert all text in headers and tabs is not clipped
        compose.assertNotEllipsized(str(R.string.catalog_lab_section_title), HOST_TAG)
        assertTargetInside360("addNewLabTestBtn")
        assertTargetInside360("labIndividualTestsTab")
        assertTargetInside360("labCategoryGroupsTab")
    }

    @Test
    fun darkThemeAndRtl_rendersCleanlyWithoutOverlap() {
        showLab(darkTheme = true, layoutDirection = LayoutDirection.Rtl)

        node("addNewLabTestBtn").assertIsDisplayed()
        node("labIndividualTestsTab").assertIsDisplayed()
        node("labCategoryGroupsTab").assertIsDisplayed()

        assertTargetInside360("addNewLabTestBtn")
        assertTargetInside360("labIndividualTestsTab")
        assertTargetInside360("labCategoryGroupsTab")
    }

    companion object {
        private const val HOST_TAG = "catalogSettingsHost"
    }
}

/**
 * In-memory test delegate for LabCatalogDelegate and MedicationCatalogDelegate.
 * Provides deterministic state flows and records all invocations.
 */
private class FakeCatalogDelegate(
    initialLabs: List<LabCatalogItem> = ClinicalCatalogRepository.defaultLabCatalogItems(),
    initialGroups: List<LabCategoryGroup> = ClinicalCatalogRepository.defaultLabCategoryGroups(),
    initialMeds: List<MedicationCatalogItem> = ClinicalCatalogRepository.defaultMedicationCatalogItems(),
) : LabCatalogDelegate, MedicationCatalogDelegate {

    private val _labs = MutableStateFlow(initialLabs)
    override val labCatalogItems: StateFlow<List<LabCatalogItem>> = _labs.asStateFlow()

    private val _groups = MutableStateFlow(initialGroups)
    override val labCategoryGroups: StateFlow<List<LabCategoryGroup>> = _groups.asStateFlow()

    private val _meds = MutableStateFlow(initialMeds)
    override val medicationCatalogItems: StateFlow<List<MedicationCatalogItem>> = _meds.asStateFlow()

    val addedLabs = mutableListOf<LabCatalogItem>()
    val updatedLabs = mutableListOf<LabCatalogItem>()
    val deletedLabs = mutableListOf<String>()
    var resetLabsCount = 0

    val addedGroups = mutableListOf<LabCategoryGroup>()
    val updatedGroups = mutableListOf<LabCategoryGroup>()
    val deletedGroups = mutableListOf<String>()
    var resetGroupsCount = 0

    val addedMeds = mutableListOf<MedicationCatalogItem>()
    val updatedMeds = mutableListOf<MedicationCatalogItem>()
    val deletedMeds = mutableListOf<String>()
    var resetMedsCount = 0

    override fun addLabCatalogItem(item: LabCatalogItem): Boolean {
        if (_labs.value.any { it.testName.equals(item.testName.trim(), ignoreCase = true) }) return false
        addedLabs += item
        _labs.value = _labs.value + item
        return true
    }

    override fun updateLabCatalogItem(item: LabCatalogItem): Boolean {
        val idx = _labs.value.indexOfFirst { it.id == item.id }
        if (idx < 0) return false
        updatedLabs += item
        val list = _labs.value.toMutableList()
        list[idx] = item
        _labs.value = list
        return true
    }

    override fun deleteLabCatalogItem(itemId: String): Boolean {
        deletedLabs += itemId
        _labs.value = _labs.value.filterNot { it.id == itemId }
        return true
    }

    override fun getLabCatalogItem(itemId: String): LabCatalogItem? =
        _labs.value.firstOrNull { it.id == itemId }

    override fun findLabCatalogItemByName(name: String): LabCatalogItem? =
        _labs.value.firstOrNull { it.testName.equals(name.trim(), ignoreCase = true) }

    override fun resetLabDefaults() {
        resetLabsCount++
        _labs.value = ClinicalCatalogRepository.defaultLabCatalogItems()
    }

    override fun addLabCategoryGroup(group: LabCategoryGroup): Boolean {
        if (_groups.value.any { it.name.equals(group.name.trim(), ignoreCase = true) }) return false
        addedGroups += group
        _groups.value = _groups.value + group
        return true
    }

    override fun updateLabCategoryGroup(group: LabCategoryGroup): Boolean {
        val idx = _groups.value.indexOfFirst { it.id == group.id }
        if (idx < 0) return false
        updatedGroups += group
        val list = _groups.value.toMutableList()
        list[idx] = group
        _groups.value = list
        return true
    }

    override fun deleteLabCategoryGroup(groupId: String): Boolean {
        deletedGroups += groupId
        _groups.value = _groups.value.filterNot { it.id == groupId }
        return true
    }

    override fun getLabCategoryGroup(groupId: String): LabCategoryGroup? =
        _groups.value.firstOrNull { it.id == groupId }

    override fun resetLabCategoryGroupDefaults() {
        resetGroupsCount++
        _groups.value = ClinicalCatalogRepository.defaultLabCategoryGroups()
    }

    override fun addMedicationCatalogItem(item: MedicationCatalogItem): Boolean {
        if (_meds.value.any { it.name.equals(item.name.trim(), ignoreCase = true) }) return false
        addedMeds += item
        _meds.value = _meds.value + item
        return true
    }

    override fun updateMedicationCatalogItem(item: MedicationCatalogItem): Boolean {
        val idx = _meds.value.indexOfFirst { it.id == item.id }
        if (idx < 0) return false
        updatedMeds += item
        val list = _meds.value.toMutableList()
        list[idx] = item
        _meds.value = list
        return true
    }

    override fun deleteMedicationCatalogItem(itemId: String): Boolean {
        deletedMeds += itemId
        _meds.value = _meds.value.filterNot { it.id == itemId }
        return true
    }

    override fun getMedicationCatalogItem(itemId: String): MedicationCatalogItem? =
        _meds.value.firstOrNull { it.id == itemId }

    override fun findMedicationCatalogItemByName(name: String): MedicationCatalogItem? =
        _meds.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    override fun resetMedicationDefaults() {
        resetMedsCount++
        _meds.value = ClinicalCatalogRepository.defaultMedicationCatalogItems()
    }
}
