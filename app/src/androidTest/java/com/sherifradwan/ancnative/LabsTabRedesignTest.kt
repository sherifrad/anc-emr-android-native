package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.LabsTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Labs tab band card (redesign slice 8): one card per report, panel / individual groups, abnormal flagging,
 * actions and gates, blank results, 360dp at font scale 2 and RTL, plus the shared header and empty card.
 */
@RunWith(AndroidJUnit4::class)
class LabsTabRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val base: DemoPatient = DemoFixtures.patients.first()
    private val today: LocalDate = LocalDate.now()
    private val reportDate: String = today.toString()

    // LMP 196 days ago -> 28+0 wks on a report dated today.
    private val ga28w0dLmp: LocalDate = today.minusDays(196)

    private val cbc = "Complete Blood Count (CBC)"
    private val cbcTitle = "$cbc (2)"
    private val kftTitle = "Renal Function Panel (KFT) (2)"

    /** Theme colours read inside the composition, so value-colour checks compare with the real scheme. */
    private var errorColor: Color = Color.Unspecified
    private var onSurfaceColor: Color = Color.Unspecified

    private fun dating() = DemoPatientDating(
        patientId = base.id,
        lmpDate = ga28w0dLmp.toString(),
        ultrasoundEdd = null,
        agreedEdd = null,
        datingMethod = "LMP",
        notes = null,
    )

    private fun lab(
        id: String,
        testName: String,
        result: String,
        unit: String? = null,
        testCategory: String = "Hematology",
        referenceRange: String? = null,
        isAbnormal: Boolean = false,
        labName: String? = "City Lab",
    ) = DemoLabInvestigation(
        id = id,
        patientId = base.id,
        date = reportDate,
        testCategory = testCategory,
        testName = testName,
        result = result,
        unit = unit,
        referenceRange = referenceRange,
        isAbnormal = isAbnormal,
        notes = null,
        labName = labName,
    )

    private fun patientWith(vararg labs: DemoLabInvestigation, status: String? = "active") =
        base.copy(status = status, dating = dating(), labInvestigations = labs.toList())

    // Clinical safety fixture: Hb 9.8 low inside a CBC panel with a normal Hct, CRP high and glucose normal as
    // individual investigations.
    private val hbLow = lab("hb-low", "Hemoglobin", "9.8", "g/dL", cbc, "11.0-14.0", isAbnormal = true)
    private val hctNormal = lab("hct-n", "Hematocrit", "36", "%", cbc, "33-39")
    private val crpHigh = lab("crp-high", "CRP", "12", "mg/L", "Inflammation", "<5", isAbnormal = true)
    private val glucoseNormal = lab("glu-n", "Glucose", "90", "mg/dL", "Chemistry", "70-99")
    private val creatinine = lab("cr-n", "Creatinine", "0.7", "mg/dL", "Renal", "0.5-1.1")
    private val urea = lab("urea-n", "Urea", "20", "mg/dL", "Renal", "15-40")

    private fun host(
        widthDp: Int?,
        fontScale: Float?,
        layoutDirection: LayoutDirection,
        content: @Composable () -> Unit,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    errorColor = MaterialTheme.colorScheme.error
                    onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    CompositionLocalProvider(
                        LocalDensity provides scaled,
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showLabs(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onEvent: (AncEvent) -> Unit = {},
        onAddInvestigation: () -> Unit = {},
    ) = host(widthDp, fontScale, layoutDirection) {
        LabsTab(patient = patient, canEdit = canEdit, onEvent = onEvent, onAddInvestigation = onAddInvestigation)
    }

    private val cardTag get() = "labReportCard-$reportDate"

    private fun inCard(): SemanticsMatcher = inTagged(cardTag)

    private fun countInCard(matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inCard(), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun countInRow(labId: String, matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inTagged("lab-$labId"), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun boundsOf(matcher: SemanticsMatcher): Rect =
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun layoutOf(matcher: SemanticsMatcher, label: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    /**
     * Clip check for end-aligned result text. A shrink-wrapped TextAlign.End line is laid out against the full
     * paragraph width (getLineRight can exceed size.width) but drawn inside the node, so compare each line's own
     * width with the node width instead of its right edge.
     */
    private fun assertEndAlignedNotClipped(label: String, matcher: SemanticsMatcher) {
        val layout = layoutOf(matcher, label)
        assertTrue("'$label' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
        for (line in 0 until layout.lineCount) {
            val width = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("'$label' line $line wider than its node ($width > ${layout.size.width})", width <= layout.size.width + 1f)
            assertFalse("'$label' ellipsized on line $line", layout.isLineEllipsized(line))
        }
    }

    /** The panel header is one clickable merged node carrying the title, its status word and its state. */
    private fun panelHeader(title: String) = compose.onNode(hasText(title) and hasClickAction() and inCard())

    private fun assertPanelState(title: String, state: String) {
        panelHeader(title)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
    }

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 0.01dp absorbs px-to-dp float rounding (a 48dp control can read back as 47.999985dp); a real shortfall is >= 1px.
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    private fun scrollToTag(tag: String) {
        compose.onNodeWithTag("labsTabContent").performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    // ---- Clinical safety ----

    @Test
    fun clinicalSafety_everyAbnormalRowRedAndWorded_chipCountEqualsAbnormalCount() {
        showLabs(patientWith(hbLow, hctNormal, crpHigh, glucoseNormal))
        scrollToTag(cardTag)

        // Report chip: abnormal count is 2 (Hb + CRP); the "All normal" chip must not appear.
        assertEquals(1, countInCard(hasText("2 abnormal")))
        assertEquals(0, countInCard(hasText("All normal")))
        // Panel word counts only its own panel (Hb).
        compose.onNode(hasText(cbcTitle) and inCard(), useUnmergedTree = true).assertExists()
        assertEquals(1, countInCard(hasText("1 abnormal")))
        compose.onNode(hasText("Individual investigations") and inCard(), useUnmergedTree = true).assertExists()

        // Every abnormal row: value in the error colour plus the "Abnormal" word.
        listOf(hbLow to "⁦9.8 g/dL⁩", crpHigh to "⁦12 mg/L⁩").forEach { (row, value) ->
            compose.onNodeWithTag("lab-${row.id}").performScrollTo()
            val style = layoutOf(hasText(value) and inTagged("lab-${row.id}"), value).layoutInput.style
            assertEquals("${row.testName} value must be red", errorColor, style.color)
            assertEquals("${row.testName} must say Abnormal", 1, countInRow(row.id, hasText("Abnormal")))
        }
        // Normal rows: plain colour, no word.
        listOf(hctNormal to "⁦36 %⁩", glucoseNormal to "⁦90 mg/dL⁩").forEach { (row, value) ->
            compose.onNodeWithTag("lab-${row.id}").performScrollTo()
            val style = layoutOf(hasText(value) and inTagged("lab-${row.id}"), value).layoutInput.style
            assertEquals("${row.testName} value must not be red", onSurfaceColor, style.color)
            assertNotEquals(errorColor, style.color)
            assertEquals("${row.testName} must not say Abnormal", 0, countInRow(row.id, hasText("Abnormal")))
        }
        // The number of row flags equals the chip count; no old uppercase or symbol flags.
        assertEquals(2, countInCard(hasText("Abnormal")))
        assertEquals(0, countInCard(hasText("ABNORMAL", substring = true)))
        assertEquals(0, countInCard(hasText("▲", substring = true)))
        assertEquals(0, countInCard(hasText("✓", substring = true)))
    }

    @Test
    fun allNormalReport_showsAllNormalChipOnce_andPanelSaysNormal() {
        showLabs(patientWith(hctNormal, lab("hb-n", "Hemoglobin", "12.5", "g/dL", cbc, "11.0-14.0"), glucoseNormal))
        scrollToTag(cardTag)

        assertEquals(1, countInCard(hasText("All normal")))
        assertEquals(0, countInCard(hasText("abnormal", substring = true, ignoreCase = true)))
        assertEquals("panel status word", 1, countInCard(hasText("Normal")))
        panelHeader(cbcTitle).assertExists()
    }

    @Test
    fun collapsedPanel_stillShowsItsAbnormalWord() {
        showLabs(patientWith(hbLow, hctNormal, crpHigh, glucoseNormal))
        scrollToTag(cardTag)

        panelHeader(cbcTitle).performScrollTo().performClick()
        compose.waitForIdle()
        assertPanelState(cbcTitle, "Collapsed")
        compose.onNodeWithTag("lab-${hbLow.id}").assertDoesNotExist()

        // The hidden abnormal Hb is still announced by the panel word, and the report chip is unchanged.
        assertEquals(1, countInCard(hasText("1 abnormal")))
        assertEquals(1, countInCard(hasText("2 abnormal")))
        compose.onNodeWithTag("lab-${crpHigh.id}").assertExists()
    }

    // ---- Panels ----

    @Test
    fun panelToggle_removesAndRestoresOnlyItsOwnRows() {
        showLabs(patientWith(hbLow, hctNormal, creatinine, urea))
        scrollToTag(cardTag)

        assertPanelState(cbcTitle, "Expanded")
        assertPanelState(kftTitle, "Expanded")
        // Only panels: no individual heading.
        assertEquals(0, countInCard(hasText("Individual investigations")))

        panelHeader(cbcTitle).performScrollTo().performClick()
        compose.waitForIdle()
        assertPanelState(cbcTitle, "Collapsed")
        assertPanelState(kftTitle, "Expanded")
        compose.onNodeWithTag("lab-${hbLow.id}").assertDoesNotExist()
        compose.onNodeWithTag("lab-${hctNormal.id}").assertDoesNotExist()
        compose.onNodeWithTag("lab-${creatinine.id}").assertExists()
        compose.onNodeWithTag("lab-${urea.id}").assertExists()

        panelHeader(cbcTitle).performScrollTo().performClick()
        compose.waitForIdle()
        assertPanelState(cbcTitle, "Expanded")
        compose.onNodeWithTag("lab-${hbLow.id}").assertExists()
        compose.onNodeWithTag("lab-${hctNormal.id}").assertExists()
    }

    @Test
    fun panelOpenState_followsItsPanelWhenAnEarlierPanelDisappears() {
        var patient by mutableStateOf(patientWith(hbLow, hctNormal, creatinine, urea))
        host(null, null, LayoutDirection.Ltr) {
            LabsTab(patient = patient, canEdit = true, onEvent = {}, onAddInvestigation = {})
        }
        scrollToTag(cardTag)

        panelHeader(kftTitle).performScrollTo().performClick()
        compose.waitForIdle()
        assertPanelState(kftTitle, "Collapsed")
        assertPanelState(cbcTitle, "Expanded")

        // Removing Hct breaks up the CBC panel, so the renal panel moves to the first group slot.
        compose.runOnIdle { patient = patientWith(hbLow, creatinine, urea) }
        compose.waitForIdle()

        assertEquals(0, countInCard(hasText(cbcTitle)))
        assertPanelState(kftTitle, "Collapsed")
        compose.onNodeWithTag("lab-${creatinine.id}").assertDoesNotExist()
        compose.onNodeWithTag("lab-${urea.id}").assertDoesNotExist()
        // Hb is now an individual investigation, still flagged.
        compose.onNodeWithTag("lab-${hbLow.id}").assertExists()
        assertEquals(1, countInRow(hbLow.id, hasText("Abnormal")))
    }

    @Test
    fun singleIndividualGroup_hasNoHeadingAndNoPanel() {
        showLabs(patientWith(glucoseNormal))
        scrollToTag(cardTag)

        assertEquals(0, countInCard(hasText("Individual investigations")))
        assertEquals(0, countInCard(hasText(cbc, substring = true)))
        compose.onNodeWithTag("lab-${glucoseNormal.id}").assertIsDisplayed()
    }

    // ---- Row content ----

    @Test
    fun rowAndSubtitle_showNameRefLineValueUnitAndLtrIsolatedGa() {
        showLabs(patientWith(glucoseNormal))
        scrollToTag(cardTag)

        compose.onNode(hasText(reportDate) and inCard(), useUnmergedTree = true).assertIsDisplayed()
        // Subtitle: lab name, test count, GA isolated so it reads left to right in RTL text.
        compose.onNode(hasText("City Lab, 1 test, ⁦28+0 wks⁩") and inCard(), useUnmergedTree = true)
            .assertIsDisplayed()

        val row = inTagged("lab-${glucoseNormal.id}")
        compose.onNode(hasText("Glucose") and row, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Chemistry, ref 70-99") and row, useUnmergedTree = true).assertExists()
        compose.onNode(hasText("⁦90 mg/dL⁩") and row, useUnmergedTree = true).assertExists()
    }

    @Test
    fun noLabNameAndNoRange_subtitleUsesDefaultName_refLineIsCategoryOnly_pluralTests() {
        showLabs(
            patientWith(
                lab("g1", "Glucose", "90", "mg/dL", "Chemistry", referenceRange = "", labName = null),
                lab("t1", "TSH", "1.2", "mIU/L", "Endocrine", referenceRange = null, labName = null),
            ),
        )
        scrollToTag(cardTag)

        compose.onNode(hasText("Laboratory report, 2 tests, ⁦28+0 wks⁩") and inCard(), useUnmergedTree = true)
            .assertExists()
        compose.onNode(hasText("Chemistry") and inTagged("lab-g1"), useUnmergedTree = true).assertExists()
        assertEquals(0, countInRow("g1", hasText("ref", substring = true)))
        compose.onNode(hasText("Endocrine") and inTagged("lab-t1"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun blankResult_showsNotRecordedWithoutUnit() {
        showLabs(patientWith(lab("blank1", "Hemoglobin", "", "g/dL", "Hematology", "11.0-14.0")))
        scrollToTag(cardTag)

        compose.onNodeWithTag("lab-blank1").performScrollTo()
        compose.onNode(hasText("⁦Not recorded⁩") and inTagged("lab-blank1"), useUnmergedTree = true)
            .assertIsDisplayed()
        assertEquals("no unit next to a missing value", 0, countInRow("blank1", hasText("g/dL", substring = true)))
    }

    // ---- Actions, events and gates ----

    @Test
    fun editable_rowEditDeleteAndBothAddActions_emitRealEvents() {
        val events = mutableListOf<AncEvent>()
        var addCalls = 0
        val patient = patientWith(hbLow, glucoseNormal)
        showLabs(patient, canEdit = true, onEvent = { events += it }, onAddInvestigation = { addCalls++ })

        compose.onNode(hasText("Laboratory reports (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertIsDisplayed()
        compose.onNode(hasText("Add test") and inTagged("addInvestigationBtn"), useUnmergedTree = true).assertExists()
        assertMinTouchTarget("addInvestigationBtn")
        compose.onNodeWithTag("addInvestigationBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, addCalls)

        val reportAdd = "addTestToReportBtn-$reportDate"
        scrollToTag(cardTag)
        compose.onNodeWithTag(reportAdd).assertIsDisplayed()
        compose.onNode(hasTestTag(reportAdd) and hasContentDescription("Add a test to this report")).assertExists()
        assertMinTouchTarget(reportAdd)
        compose.onNodeWithTag(reportAdd).performClick()
        compose.waitForIdle()
        assertEquals(2, addCalls)

        compose.onNodeWithTag("deleteLab-${hbLow.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasTestTag("deleteLab-${hbLow.id}") and hasContentDescription("Delete test Hemoglobin"))
            .assertExists()
        assertMinTouchTarget("deleteLab-${hbLow.id}")
        assertMinTouchTarget("lab-${hbLow.id}")

        compose.onNodeWithTag("lab-${hbLow.id}").performClick()
        compose.waitForIdle()
        val edit = events.filterIsInstance<AncEvent.StartLabEditor>().single()
        assertEquals(patient.id, edit.patientId)
        assertEquals(hbLow.id, edit.labId)
        assertEquals(hbLow.id, edit.newLabId)
        assertTrue(edit.editorSessionId.isNotBlank())

        compose.onNodeWithTag("deleteLab-${hbLow.id}").performClick()
        compose.waitForIdle()
        assertEquals(
            AncEvent.RequestDelete(patient.id, DeletableType.LAB, hbLow.id),
            events.filterIsInstance<AncEvent.RequestDelete>().single(),
        )
        // Deleting must not also open the editor.
        assertEquals(1, events.filterIsInstance<AncEvent.StartLabEditor>().size)
    }

    @Test
    fun readOnly_hidesDeleteAndBothAdds_rowClickEmitsNothing() {
        val events = mutableListOf<AncEvent>()
        showLabs(patientWith(hbLow), canEdit = false, onEvent = { events += it })
        scrollToTag(cardTag)

        compose.onNode(hasText("Laboratory reports (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertDoesNotExist()
        compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("deleteLab-${hbLow.id}", useUnmergedTree = true).assertDoesNotExist()
        // The clinical flag stays visible read-only.
        assertEquals(1, countInRow(hbLow.id, hasText("Abnormal")))

        compose.onNodeWithTag("lab-${hbLow.id}").performClick()
        compose.waitForIdle()
        assertTrue("read-only row must not emit events: $events", events.isEmpty())
    }

    @Test
    fun archivedPatient_hidesHeaderAddButKeepsTitleAndRecords() {
        showLabs(patientWith(hbLow, status = "archived"), canEdit = true)

        compose.onNode(hasText("Laboratory reports (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertDoesNotExist()
        scrollToTag(cardTag)
        compose.onNodeWithTag("lab-${hbLow.id}").assertExists()
    }

    @Test
    fun emptyList_showsZeroTitleAddButtonAndOneNoRecordsCard() {
        showLabs(patientWith())

        compose.onNodeWithTag("labsTabContent").assertIsDisplayed()
        compose.onNode(hasText("Laboratory reports (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertIsDisplayed()
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(1)
    }

    // ---- 360dp at font scale 2 ----

    @Test
    fun longFreeTextResult_at360dpFontScale2_testNameStaysWholeAndVisible() {
        val longResult = "Reactive, confirmatory testing advised at the referral laboratory before delivery"
        val vdrl = lab("vdrl1", "VDRL", longResult, testCategory = "Serology", isAbnormal = true)
        showLabs(patientWith(vdrl), widthDp = 360, fontScale = 2.0f)
        scrollToTag(cardTag)
        compose.onNodeWithTag("lab-vdrl1").performScrollTo()

        val row = inTagged("lab-vdrl1")
        val nameMatcher = hasText("VDRL") and row
        compose.onNode(nameMatcher, useUnmergedTree = true).assertIsDisplayed()
        compose.assertTextNotClipped("VDRL", nameMatcher)
        assertEquals("the test name must not be squeezed onto several lines", 1, layoutOf(nameMatcher, "VDRL").lineCount)

        val resultMatcher = hasText("⁦$longResult⁩") and row
        compose.assertTextNotClipped("long result", resultMatcher)
        assertTrue("a long result wraps instead of pushing the name out", layoutOf(resultMatcher, "long result").lineCount > 1)

        // Name, result and delete sit side by side without overlapping, all inside the card.
        val name = boundsOf(nameMatcher)
        val result = boundsOf(resultMatcher)
        val delete = boundsOf(hasTestTag("deleteLab-vdrl1"))
        val cardPx = boundsOf(hasTestTag(cardTag))
        val card = compose.onNodeWithTag(cardTag).getUnclippedBoundsInRoot()
        assertTrue("name overlaps result (name=$name result=$result)", name.right <= result.left + 1f)
        assertTrue("result overlaps delete (result=$result delete=$delete)", result.right <= delete.left + 1f)
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        assertTrue("delete inside the card (delete=$delete card=$cardPx)", delete.right <= cardPx.right + 1f)
        assertMinTouchTarget("deleteLab-vdrl1")
        assertEquals(1, countInRow("vdrl1", hasText("Abnormal")))
    }

    @Test
    fun narrow360dpFontScale2_nothingEllipsizedAndCardFitsViewport() {
        val crpLong = lab(
            id = "crp-long",
            testName = "C-reactive protein",
            result = "12.4",
            unit = "mg/L",
            testCategory = "Inflammation markers",
            referenceRange = "Less than 5.0 mg/L",
            isAbnormal = true,
        )
        showLabs(patientWith(hbLow, hctNormal, crpLong), widthDp = 360, fontScale = 2.0f)
        scrollToTag(cardTag)

        listOf(
            reportDate,
            "2 abnormal",
            "City Lab",
            "28+0 wks",
            cbcTitle,
            "1 abnormal",
            "Hemoglobin",
            "Hematocrit",
            "Individual investigations",
            "C-reactive protein",
            "Inflammation markers, ref Less than 5.0 mg/L",
        ).forEach { compose.assertNotEllipsized(it, cardTag) }
        listOf("⁦9.8 g/dL⁩", "⁦36 %⁩", "⁦12.4 mg/L⁩").forEach {
            assertEndAlignedNotClipped(it, hasText(it) and inCard())
        }

        val card = compose.onNodeWithTag(cardTag).getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        assertMinTouchTarget("addTestToReportBtn-$reportDate")
        val add = compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("report add inside the card (add=$add card=$card)", add.right <= card.right + 1.dp)
        compose.onNodeWithTag("deleteLab-${hbLow.id}").performScrollTo()
        assertMinTouchTarget("deleteLab-${hbLow.id}")
    }

    // ---- RTL ----

    @Test
    fun rtl_valuesAndGaStayLtrIsolated_andActionsSitAtLayoutEnd() {
        showLabs(patientWith(hbLow), layoutDirection = LayoutDirection.Rtl)
        scrollToTag(cardTag)

        val row = inTagged("lab-${hbLow.id}")
        val valueMatcher = hasText("⁦9.8 g/dL⁩") and row
        compose.onNode(valueMatcher, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦28+0 wks⁩", substring = true) and inCard(), useUnmergedTree = true)
            .assertIsDisplayed()

        // Layout end in RTL is the visual left: name (start) right of value, value right of delete.
        val name = boundsOf(hasText("Hemoglobin") and row)
        val value = boundsOf(valueMatcher)
        val delete = boundsOf(hasTestTag("deleteLab-${hbLow.id}"))
        assertTrue("value must be left of the name in RTL (value=$value name=$name)", value.right <= name.left + 1f)
        assertTrue("delete must be left of the value in RTL (delete=$delete value=$value)", delete.right <= value.left + 1f)

        val add = boundsOf(hasTestTag("addTestToReportBtn-$reportDate"))
        val title = boundsOf(hasText(reportDate) and inCard())
        assertTrue("report add must be left of the title in RTL (add=$add title=$title)", add.right <= title.left + 1f)
    }
}
