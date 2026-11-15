FILE: app/src/androidTest/java/com/sherifradwan/ancnative/LabsTabRedesignTest.kt
```kotlin
package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Labs tab band card (redesign slice 8): report cards grouped by date, panel/individual
 * grouping, abnormal flagging, actions, 360dp and RTL, and shared header/empty states.
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
        referenceRange: String? = "—",
        isAbnormal: Boolean = false,
        labName: String? = "City Lab",
        date: String = reportDate,
    ) = DemoLabInvestigation(
        id = id,
        patientId = base.id,
        date = date,
        testName = testName,
        result = result,
        unit = unit,
        referenceRange = referenceRange,
        isAbnormal = isAbnormal,
        testCategory = testCategory,
        labName = labName,
    )

    private fun patientWith(
        vararg labs: DemoLabInvestigation,
        status: String? = "active",
    ) = base.copy(status = status, dating = dating(), labInvestigations = labs.toList())

    private fun host(
        widthDp: Int?,
        fontScale: Float?,
        layoutDirection: LayoutDirection,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
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
    ) = host(widthDp, fontScale, layoutDirection) {
        LabsTab(
            patient = patient,
            canEdit = canEdit,
            onEvent = onEvent,
            onAddInvestigation = {},
        )
    }

    private val cardTag get() = "labReportCard-$reportDate"

    private fun inCard(): SemanticsMatcher = inTagged(cardTag)

    private fun countInCard(matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inCard(), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun boundsInCard(text: String, substring: Boolean = false): Rect =
        compose.onNode(hasText(text, substring = substring) and inCard(), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun layoutOf(text: String, substring: Boolean = false): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(hasText(text, substring = substring) and inCard(), useUnmergedTree = true)
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$text' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 0.01dp absorbs px-to-dp float rounding (a 48dp control can read back as 47.999985dp).
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    private fun scrollToTag(tag: String) {
        compose.onNodeWithTag("labsTabContent").performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    private fun cbcPanel(
        hbId: String = "hb1",
        hbResult: String = "9.8",
        hbAbnormal: Boolean = true,
        hctId: String = "hct1",
    ) = listOf(
        lab(
            id = hbId,
            testName = "Hemoglobin",
            result = hbResult,
            unit = "g/dL",
            testCategory = "Complete Blood Count (CBC)",
            referenceRange = "11.0-14.0",
            isAbnormal = hbAbnormal,
        ),
        lab(
            id = hctId,
            testName = "Hematocrit",
            result = "33",
            unit = "%",
            testCategory = "Complete Blood Count (CBC)",
            referenceRange = "33-39",
            isAbnormal = false,
        ),
    )

    // ---- Band, chips, subtitle ----

    @Test
    fun reportCard_showsDateChipSubtitleWithGaAndRowContent() {
        val hb = lab("hb1", "Hemoglobin", "12.1", unit = "g/dL", testCategory = "Hematology", referenceRange = "11.0-14.0")
        showLabs(patientWith(hb))

        scrollToTag(cardTag)
        compose.onNodeWithTag(cardTag).assertIsDisplayed()
        compose.onNode(hasText(reportDate) and inCard(), useUnmergedTree = true).assertIsDisplayed()

        // All normal chip and no abnormal chip.
        assertEquals(1, countInCard(hasText("All normal")))
        assertEquals(0, countInCard(hasText("abnormal", substring = true, ignoreCase = true)))

        // Subtitle: lab name, test count, GA ltr-isolated.
        compose.onNode(hasText("City Lab", substring = true) and inCard(), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("1 test", substring = true) and inCard(), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("⁦28+0 wks⁩", substring = true) and inCard(), useUnmergedTree = true).assertExists()

        // Row content: name, result+unit ltr-isolated, reference line, no abnormal flag.
        compose.onNodeWithTag("lab-hb1").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Hemoglobin") and inTagged("lab-hb1"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦12.1 g/dL⁩", substring = true) and inTagged("lab-hb1"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Hematology, 11.0-14.0") and inTagged("lab-hb1"), useUnmergedTree = true).assertExists()
        assertEquals(0, countInCard(hasText("Abnormal")))
        assertEquals(0, countInCard(hasText("ABNORMAL")))
    }

    @Test
    fun reportWithoutLabName_usesLaboratoryReportAndBlankReferenceShowsCategoryOnly() {
        val glucose = lab(
            id = "glu1",
            testName = "Glucose",
            result = "90",
            unit = "mg/dL",
            testCategory = "Chemistry",
            referenceRange = "",
            labName = null,
        )
        showLabs(patientWith(glucose))

        scrollToTag(cardTag)
        compose.onNode(hasText("Laboratory report", substring = true) and inCard(), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Chemistry") and inTagged("lab-glu1"), useUnmergedTree = true).assertExists()
        assertEquals(0, countInCard(hasText("Chemistry,", substring = true)))
    }

    // ---- Panels, individuals, toggling ----

    @Test
    fun panelGroup_clickableToggleWithStateAndIndividualHeadingOnlyWhenMultipleGroups() {
        val items = cbcPanel() + lab(
            id = "crp1",
            testName = "CRP",
            result = "12",
            unit = "mg/L",
            testCategory = "Inflammation",
            referenceRange = "<5",
            isAbnormal = true,
        )
        showLabs(patientWith(*items.toTypedArray()))

        scrollToTag(cardTag)
        // Panel header: title with count, button role, expanded by default, abnormal status word.
        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasStateDescription("Expanded"))
        compose.onNode(hasText("1 abnormal") and inCard(), useUnmergedTree = true).assertExists()

        // Panel rows visible when expanded.
        compose.onNodeWithTag("lab-hb1").assertExists()
        compose.onNodeWithTag("lab-hct1").assertExists()

        // Individual heading shown because there is more than one group.
        compose.onNode(hasText("Individual investigations") and inCard(), useUnmergedTree = true).assertIsDisplayed()

        // Collapse removes panel rows, keeps header; expand restores.
        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true)
            .performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true)
            .assert(hasStateDescription("Collapsed"))
        compose.onNodeWithTag("lab-hb1").assertDoesNotExist()
        compose.onNodeWithTag("lab-hct1").assertDoesNotExist()

        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true)
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("lab-hb1").assertExists()
        compose.onNodeWithTag("lab-hct1").assertExists()
    }

    @Test
    fun onlyIndividualGroup_hidesIndividualInvestigationsHeading() {
        val hb = lab("hb1", "Hemoglobin", "12.1", unit = "g/dL")
        showLabs(patientWith(hb))

        scrollToTag(cardTag)
        assertEquals(0, countInCard(hasText("Individual investigations")))
        assertEquals(0, countInCard(hasText("Complete Blood Count", substring = true)))
        compose.onNodeWithTag("lab-hb1").assertIsDisplayed()
    }

    @Test
    fun allNormalPanel_showsNormalStatusWord() {
        val items = listOf(
            lab("hb1", "Hemoglobin", "12.5", unit = "g/dL", testCategory = "Complete Blood Count (CBC)", isAbnormal = false),
            lab("hct1", "Hematocrit", "36", unit = "%", testCategory = "Complete Blood Count (CBC)", isAbnormal = false),
        )
        showLabs(patientWith(*items.toTypedArray()))

        scrollToTag(cardTag)
        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Normal") and inCard(), useUnmergedTree = true).assertExists()
        assertEquals(1, countInCard(hasText("All normal")))
    }

    // ---- Clinical safety ----

    @Test
    fun clinicalSafety_everyAbnormalFlaggedAndChipCountEqualsAbnormalCount() {
        val hb = lab(
            id = "hb-low",
            testName = "Hemoglobin",
            result = "9.8",
            unit = "g/dL",
            testCategory = "Complete Blood Count (CBC)",
            referenceRange = "11.0-14.0",
            isAbnormal = true,
        )
        val hct = lab(
            id = "hct-n",
            testName = "Hematocrit",
            result = "33",
            unit = "%",
            testCategory = "Complete Blood Count (CBC)",
            referenceRange = "33-39",
            isAbnormal = false,
        )
        val crp = lab(
            id = "crp-high",
            testName = "CRP",
            result = "12",
            unit = "mg/L",
            testCategory = "Inflammation",
            referenceRange = "<5",
            isAbnormal = true,
        )
        val glucose = lab(
            id = "glu-n",
            testName = "Glucose",
            result = "90",
            unit = "mg/dL",
            testCategory = "Chemistry",
            referenceRange = "70-99",
            isAbnormal = false,
        )
        showLabs(patientWith(hb, hct, crp, glucose))

        scrollToTag(cardTag)
        // Report chip count equals abnormal count (Hb + CRP = 2).
        assertEquals(1, countInCard(hasText("2 abnormal")))
        assertEquals(0, countInCard(hasText("All normal")))

        // Panel abnormal status reflects 1 abnormal inside CBC.
        compose.onNode(hasText("Complete Blood Count (CBC) (2)") and inCard(), useUnmergedTree = true).assertExists()
        assertEquals(1, countInCard(hasText("1 abnormal")))

        // Each abnormal row shows result ltr-isolated and "Abnormal" word; normals never do.
        compose.onNode(hasText("⁦9.8 g/dL⁩", substring = true) and inTagged("lab-hb-low"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Abnormal") and inTagged("lab-hb-low"), useUnmergedTree = true).assertExists()

        compose.onNode(hasText("⁦12 mg/L⁩", substring = true) and inTagged("lab-crp-high"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Abnormal") and inTagged("lab-crp-high"), useUnmergedTree = true).assertExists()

        compose.onNode(hasText("Abnormal") and inTagged("lab-hct-n"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("Abnormal") and inTagged("lab-glu-n"), useUnmergedTree = true).assertDoesNotExist()

        // Never uppercase ABNORMAL, no ▲ ✓.
        assertEquals(0, countInCard(hasText("ABNORMAL")))
        assertEquals(0, countInCard(hasText("▲")))
        assertEquals(0, countInCard(hasText("✓")))
    }

    // ---- Actions and events ----

    @Test
    fun actions_editable_emitEventsHaveTouchTargetsAndDescriptions() {
        val hb = lab("hb1", "Hemoglobin", "12.1", unit = "g/dL")
        val events = mutableListOf<AncEvent>()
        val patient = patientWith(hb)
        showLabs(patient, canEdit = true, onEvent = { events += it })

        scrollToTag(cardTag)

        // Header add action.
        compose.onNodeWithTag("addInvestigationBtn").assertIsDisplayed()
        assertMinTouchTarget("addInvestigationBtn")

        // Add test to report action.
        compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(
            hasTestTag("addTestToReportBtn-$reportDate") and hasContentDescription("Add test to report"),
            useUnmergedTree = true,
        ).assertExists()
        assertMinTouchTarget("addTestToReportBtn-$reportDate")

        // Delete control.
        compose.onNodeWithTag("deleteLab-hb1", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNode(hasTestTag("deleteLab-hb1") and hasContentDescription("Delete test")).assertExists()
        assertMinTouchTarget("deleteLab-hb1")

        // Row click emits edit event.
        compose.onNodeWithTag("lab-hb1").performClick()
        compose.waitForIdle()
        val edit = events.filterIsInstance<AncEvent.StartLabEditor>().single()
        assertEquals(patient.id, edit.patientId)
        assertEquals("hb1", edit.labId)
        assertEquals("hb1", edit.newLabId)
        assertTrue(edit.editorSessionId.isNotBlank())

        // Delete emits real delete event.
        compose.onNodeWithTag("deleteLab-hb1").performClick()
        compose.waitForIdle()
        assertEquals(
            AncEvent.RequestDelete(patient.id, DeletableType.LAB, "hb1"),
            events.filterIsInstance<AncEvent.RequestDelete>().single(),
        )
    }

    @Test
    fun readOnly_hidesEditAffordancesAndAddActions() {
        val hb = lab("hb1", "Hemoglobin", "12.1", unit = "g/dL")
        val events = mutableListOf<AncEvent>()
        showLabs(patientWith(hb), canEdit = false, onEvent = { events += it })

        scrollToTag(cardTag)
        compose.onNodeWithTag("addInvestigationBtn").assertDoesNotExist()
        compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("deleteLab-hb1", useUnmergedTree = true).assertDoesNotExist()

        // Row click must not emit edit when not editable.
        compose.onNodeWithTag("lab-hb1").performClick()
        compose.waitForIdle()
        assertTrue(events.filterIsInstance<AncEvent.StartLabEditor>().isEmpty())
    }

    @Test
    fun archivedPatient_hidesHeaderAddActionEvenWhenCanEdit() {
        val hb = lab("hb1", "Hemoglobin", "12.1", unit = "g/dL")
        showLabs(patientWith(hb, status = "archived"), canEdit = true)

        compose.onNode(hasText("Lab reports (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertDoesNotExist()
        // Per-report add remains governed by canEdit.
        compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).assertIsDisplayed()
    }

    // ---- Empty and header states ----

    @Test
    fun emptyList_showsZeroTitleAddActionAndNoRecordsCard() {
        showLabs(patientWith())

        compose.onNodeWithTag("labsTabContent").assertIsDisplayed()
        compose.onNode(hasText("Lab reports (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addInvestigationBtn").assertIsDisplayed()
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(1)
    }

    // ---- Narrow and RTL ----

    @Test
    fun narrow360dpFontScale2_nothingEllipsizedAndCardFitsViewport() {
        val items = cbcPanel() + lab(
            id = "crp1",
            testName = "C-reactive protein (high sensitivity)",
            result = "12.4",
            unit = "mg/L",
            testCategory = "Inflammation markers",
            referenceRange = "Less than 5.0 mg/L",
            isAbnormal = true,
        )
        showLabs(patientWith(*items.toTypedArray()), widthDp = 360, fontScale = 2.0f)

        scrollToTag(cardTag)
        listOf(
            reportDate,
            "2 abnormal",
            "City Lab",
            "28+0 wks",
            "Hemoglobin",
            "9.8 g/dL",
            "Hematocrit",
            "C-reactive protein (high sensitivity)",
            "Inflammation markers, Less than 5.0 mg/L",
            "Individual investigations",
        ).forEach { compose.assertNotEllipsized(it, cardTag) }

        val card = compose.onNodeWithTag(cardTag).getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        assertMinTouchTarget("addInvestigationBtn")
        assertMinTouchTarget("deleteLab-hb1")
    }

    @Test
    fun rtl_valuesStayLtrIsolated_andActionsSitAtLayoutEnd() {
        val hb = lab("hb1", "Hemoglobin", "9.8", unit = "g/dL", isAbnormal = true)
        showLabs(patientWith(hb), layoutDirection = LayoutDirection.Rtl)

        scrollToTag(cardTag)
        compose.onNode(hasText("⁦9.8 g/dL⁩", substring = true) and inTagged("lab-hb1"), useUnmergedTree = true)
            .assertExists()
        compose.onNode(hasText("⁦28+0 wks⁩", substring = true) and inCard(), useUnmergedTree = true)
            .assertExists()

        // Layout end in RTL is the visual left: delete is left of the row/result area.
        val delete = compose.onNodeWithTag("deleteLab-hb1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val result = boundsInCard("9.8", substring = true)
        assertTrue(
            "Delete must be left of the result in RTL (delete=$delete result=$result)",
            delete.right <= result.left + 1.dp,
        )

        val add = compose.onNodeWithTag("addTestToReportBtn-$reportDate", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = boundsInCard(reportDate)
        assertTrue(
            "Report add action must be left of the title in RTL (add=$add title=$title)",
            add.right <= title.left + 1.dp,
        )
    }
}
```
[Tokens] upload: 24772 download: 6579 cache_read: 0 cache_write: 0

