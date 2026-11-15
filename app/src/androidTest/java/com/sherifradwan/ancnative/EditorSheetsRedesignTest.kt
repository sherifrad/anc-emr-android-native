package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ChoiceGroup
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Redesign slice 11 (editor sheets): the medication sheet and the delivery plan sheet, mounted through the real AncApp
 * and reducer as MedsTabSheetTest and DeliveryPlanSheetTest do. Covers type tokens, buttons, the discard dialog, the
 * Delivered toggle row the owner asked for (fixed, aligned with the fields, constant when toggled, new fields below it,
 * wraps at 200% text, mirrored in RTL), 360dp at 130% and 200% text, and the ChoiceGroup default title style.
 *
 * A sheet lives in its own dialog window, which reads density from the activity resources, so the narrow and large-text
 * runs hold the activity resources at 360dp wide and the given font scale for that test (restored after), as
 * AttachmentsRedesignTest.narrowWindow does. The sheet is wrap-content, so positions are compared inside the sheet.
 */
@RunWith(AndroidJUnit4::class)
class EditorSheetsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var state by mutableStateOf(AncState(patients = emptyList()))

    private var primaryColor = Color.Unspecified
    private var errorColor = Color.Unspecified
    private var onSurfaceColor = Color.Unspecified
    private var backgroundLabelColor = Color.Unspecified
    private var titleMediumStyle = TextStyle.Default

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    @After
    fun tearDown() = restoreResources()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    // ---- fixtures ----

    private fun medRow(id: String, name: String) = DemoMedicationRecord(
        id = id, patientId = DemoFixtures.patients.first().id, name = name, dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-03-01", endDate = null, status = MEDICATION_STATUS_ACTIVE, notes = null,
        isProphylaxis = false, revision = 2,
    )

    private val medsPatient = DemoFixtures.patients.first().copy(medications = listOf(medRow("s11-m1", "UiIron")), status = "active")

    // Heuristic notes so every plan box opens with text (as DeliveryPlanSheetTest).
    private val freeNotes = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks"
    private val planPatient = DemoFixtures.patients.first().copy(obstetricNotes = freeNotes, status = "active", delivered = false)

    // ---- hosting ----

    private fun onEvent(event: AncEvent) {
        state = AncReducer.reduce(state, event)
    }

    /** One setContent per test: the real app on [route], optionally mirrored. */
    private fun host(patient: DemoPatient, route: AncRoute, layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
        state = AncState(patients = listOf(patient), vaultState = VaultState.Unlocked(sessionEpoch = 7311L), route = route)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    primaryColor = MaterialTheme.colorScheme.primary
                    errorColor = MaterialTheme.colorScheme.error
                    onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    backgroundLabelColor = FieldFamilies.background().label
                    titleMediumStyle = MaterialTheme.typography.titleMedium
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
        compose.waitForIdle()
    }

    /**
     * Holds the activity resources at a 360dp-wide window and [fontScale] for this test. A sheet's dialog window builds
     * its own density from these resources, so a LocalDensity override in the host never reaches sheet content.
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

    private fun openAddMedication(layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
        host(medsPatient, AncRoute.Workspace(medsPatient.id, WorkspaceSection.MEDICATIONS), layoutDirection)
        compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag("addCustomMedicationBtn"))
        click("addCustomMedicationBtn")
        node("medicationSheet").assertIsDisplayed()
    }

    private fun openEditMedication() {
        host(medsPatient, AncRoute.Workspace(medsPatient.id, WorkspaceSection.MEDICATIONS))
        compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag("editMedication-s11-m1"))
        click("editMedication-s11-m1")
        node("medicationSheet").assertIsDisplayed()
    }

    private fun openDeliveryPlan(patient: DemoPatient = planPatient, layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
        host(patient, AncRoute.Workspace(patient.id, WorkspaceSection.DELIVERY), layoutDirection)
        compose.openHubTab(PatientHubTab.PLAN)
        click("editDeliveryPlan")
        node("deliveryPlanSheet").assertIsDisplayed()
    }

    // ---- measuring ----

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun click(tag: String) {
        node(tag).performClick()
        compose.waitForIdle()
    }
    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()
    private fun bounds(matcher: SemanticsMatcher): DpRect = compose.onNode(matcher, useUnmergedTree = true).getUnclippedBoundsInRoot()
    private fun bounds(n: SemanticsNode): DpRect = bounds(SemanticsMatcher("node ${n.id}") { it.id == n.id })

    private fun text(value: String, inTag: String) = hasText(value) and inTagged(inTag)

    private fun layoutOf(label: String, matcher: SemanticsMatcher): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun assertStyle(label: String, matcher: SemanticsMatcher, size: Float, weight: FontWeight? = null, color: Color? = null) {
        val style = layoutOf(label, matcher).layoutInput.style
        assertEquals("'$label' font size", size.sp, style.fontSize)
        if (weight != null) assertEquals("'$label' font weight", weight, style.fontWeight)
        if (color != null) assertEquals("'$label' colour", color, style.color)
    }

    private fun assertMinTouchTarget(tag: String) {
        val b = bounds(tag)
        // 0.01dp absorbs px-to-dp float rounding (a 48dp control can read back as 47.999985dp); a real shortfall is >= 1px.
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertNear(label: String, expected: Dp, actual: Dp, tolerance: Dp = 1.dp) =
        assertTrue("$label: expected $expected, was $actual (tolerance $tolerance)", abs((actual - expected).value) <= tolerance.value)

    private fun centreY(r: DpRect) = (r.top + r.bottom) / 2

    /** Proves the sheet really rendered within 360dp and with [fontScale], through its title's text density. */
    private fun assertSheetNarrowAndScaled(sheetTag: String, title: SemanticsMatcher, fontScale: Float) {
        assertEquals("sheet text font scale", fontScale, layoutOf("sheet title", title).layoutInput.density.fontScale, 0.01f)
        val width = bounds(sheetTag).width
        assertTrue("$sheetTag must fit 360dp (width=$width)", width <= 360.dp)
    }

    // ---- the Delivered toggle row ----

    private val deliveredLabel get() = text(str(R.string.delivery_outcome_delivered_switch), "deliveryPlanSheet")

    /** The row is the merged semantics node that holds the switch and its label. */
    private fun toggleRow(): SemanticsNode {
        val switch = node("deliveryOutcomeSwitch").fetchSemanticsNode()
        val row = switch.parent!!
        val labelId = compose.onNode(deliveredLabel, useUnmergedTree = true).fetchSemanticsNode().id
        assertTrue("the Delivered label and the switch share one row", row.children.any { it.id == labelId })
        return row
    }

    private data class RowGeometry(
        val row: DpRect,
        val label: DpRect,
        val switch: DpRect,
        val hospital: DpRect,
        val sheet: DpRect,
    ) {
        val height get() = row.bottom - row.top
        val hospitalToRow get() = row.top - hospital.top
        val rowInSheet get() = row.top - sheet.top
    }

    private fun rowGeometry() = RowGeometry(
        row = bounds(toggleRow()),
        label = bounds(deliveredLabel),
        switch = bounds("deliveryOutcomeSwitch"),
        hospital = bounds("deliveryPlanHospitalField"),
        sheet = bounds("deliveryPlanSheet"),
    )

    /** Label at the fields' start edge, switch at their end edge, both centred on one line, gap of 12dp at least. */
    private fun assertRowAligned(g: RowGeometry, rtl: Boolean) {
        val postpartum = bounds("deliveryPlanPostpartumField")
        assertTrue("row is at least 56dp (${g.height})", g.height >= 56.dp - 0.01.dp)
        assertNear("label and switch centre-y", centreY(g.label), centreY(g.switch))
        if (!rtl) {
            assertNear("label left = hospital field left", g.hospital.left, g.label.left)
            assertNear("switch right = hospital field right", g.hospital.right, g.switch.right)
            assertNear("switch right = postpartum field right", postpartum.right, g.switch.right)
            assertTrue("label (${g.label.right}) must end 12dp before the switch (${g.switch.left})", g.label.right <= g.switch.left - 12.dp + 0.5.dp)
        } else {
            assertNear("RTL: label right = hospital field right", g.hospital.right, g.label.right)
            assertNear("RTL: switch left = hospital field left", g.hospital.left, g.switch.left)
            assertNear("RTL: switch left = postpartum field left", postpartum.left, g.switch.left)
            assertTrue("RTL: label (${g.label.left}) must start 12dp after the switch (${g.switch.right})", g.label.left >= g.switch.right + 12.dp - 0.5.dp)
        }
        assertTrue("label inside the row vertically", g.label.top >= g.row.top - 0.5.dp && g.label.bottom <= g.row.bottom + 0.5.dp)
        assertTrue("switch inside the row vertically", g.switch.top >= g.row.top - 0.5.dp && g.switch.bottom <= g.row.bottom + 0.5.dp)
    }

    // ================= Medication sheet =================

    @Test
    fun medication_addAndEditTitlesAre18spBoldPrimary() {
        openAddMedication()
        assertStyle("add title", text("Add medication", "medicationSheet"), 18f, FontWeight.Bold, primaryColor)
        click("cancelMedication")
        assertFalse(exists("medicationSheet"))
        compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag("editMedication-s11-m1"))
        click("editMedication-s11-m1")
        assertStyle("edit title", text("Edit medication", "medicationSheet"), 18f, FontWeight.Bold, primaryColor)
    }

    @Test
    fun medication_frequencyStatusAndRouteLabelsAre12spInTheFieldFamilyColour() {
        openAddMedication()
        listOf(R.string.frequency, R.string.status, R.string.route).forEach { id ->
            val label = str(id)
            node("medicationSheet").assertIsDisplayed()
            compose.onNode(text(label, "medicationSheet"), useUnmergedTree = true).performScrollTo()
            assertStyle(label, text(label, "medicationSheet"), 12f, FontWeight.SemiBold, backgroundLabelColor)
        }
    }

    @Test
    fun medication_existingRowRouteIsReadOnlyWithValueAndNoteAndNoIcon() {
        openEditMedication()
        node("medRouteReadOnly").performScrollTo().assertIsDisplayed()
        assertFalse("no route chips on an existing row", exists("medRouteChoice-0"))
        assertStyle("read-only label", text(str(R.string.medication_route_locked_label), "medRouteReadOnly"), 12f, FontWeight.SemiBold, backgroundLabelColor)
        assertStyle("route value", hasTestTag("medRouteValue"), 16f, FontWeight.Normal, onSurfaceColor)
        assertEquals(listOf("Oral"), node("medRouteValue").fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        assertStyle("locked note", text(str(R.string.medication_route_locked_note), "medRouteReadOnly"), 14f)
        // Label, value and note only: no icon (an icon with a description would add a child without text).
        val children = node("medRouteReadOnly").fetchSemanticsNode().children
        assertEquals("read-only route children", 3, children.size)
        children.forEach { child ->
            assertTrue("child ${child.id} is text only", child.config.getOrNull(SemanticsProperties.Text) != null)
            assertNull("child ${child.id} has no content description", child.config.getOrNull(SemanticsProperties.ContentDescription))
        }
    }

    @Test
    fun medication_buttonsAreAtLeast48dpAndSaveAndAddAnotherHasNoAmpersand() {
        openAddMedication()
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach { assertMinTouchTarget(it) }
        val words = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and inTagged("addAnotherMedication"), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
        assertEquals("Save and add another", words)
        assertFalse(words.contains("&"))
        assertStyle("Save and add another", text("Save and add another", "addAnotherMedication"), 14f, FontWeight.SemiBold)
        // Full-width row below Cancel and Save.
        val add = bounds("addAnotherMedication")
        val cancel = bounds("cancelMedication")
        val save = bounds("saveMedication")
        assertTrue("add another sits below Cancel and Save", add.top >= maxOf(cancel.bottom, save.bottom) - 0.5.dp)
        assertNear("add another spans from Cancel's start", cancel.left, add.left)
        assertNear("add another spans to Save's end", save.right, add.right)
    }

    @Test
    fun medication_editHasNoSaveAndAddAnother() {
        openEditMedication()
        assertMinTouchTarget("cancelMedication")
        assertMinTouchTarget("saveMedication")
        assertFalse(exists("addAnotherMedication"))
    }

    @Test
    fun medication_discardDialogTextsColoursAndActions() {
        openAddMedication()
        node("medName").performScrollTo().performTextInput("UiDraft")
        compose.waitForIdle()
        click("cancelMedication")
        node("medDiscardDialog").assertIsDisplayed()
        assertStyle("dialog title", text(str(R.string.discard_changes), "medDiscardDialog"), 18f, FontWeight.Bold, onSurfaceColor)
        assertStyle("dialog body", text(str(R.string.discard_body), "medDiscardDialog"), 16f)
        assertStyle("Discard", text(str(R.string.discard), "medDiscardConfirm"), 14f, FontWeight.SemiBold, errorColor)
        assertStyle("Keep editing", text(str(R.string.keep_editing), "medDiscardKeep"), 14f, FontWeight.SemiBold, primaryColor)
        assertMinTouchTarget("medDiscardConfirm")
        assertMinTouchTarget("medDiscardKeep")

        click("medDiscardKeep")
        assertFalse(exists("medDiscardDialog"))
        node("medicationSheet").assertIsDisplayed()
        assertEquals("UiDraft", state.medicationDraft!!.values.name)

        click("cancelMedication")
        click("medDiscardConfirm")
        assertFalse(exists("medicationSheet"))
        assertNull(state.medicationDraft)
    }

    @Test
    fun medication_360dpAt130PercentText_nothingClipped() = assertMedicationWholeAt360(1.3f)

    @Test
    fun medication_360dpAt200PercentText_nothingClipped() = assertMedicationWholeAt360(2.0f)

    private fun assertMedicationWholeAt360(fontScale: Float) {
        narrowWindow(fontScale)
        openAddMedication()
        assertSheetNarrowAndScaled("medicationSheet", text("Add medication", "medicationSheet"), fontScale)
        compose.assertAllTextWhole("medicationSheet")
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach { assertMinTouchTarget(it) }
    }

    @Test
    fun medication_rtlMirrorsTitleAndButtons() {
        openAddMedication(LayoutDirection.Rtl)
        val sheet = bounds("medicationSheet")
        val title = bounds(text("Add medication", "medicationSheet"))
        assertNear("RTL: title ends 16dp from the sheet's right edge", sheet.right - 16.dp, title.right)
        val cancel = bounds("cancelMedication")
        val save = bounds("saveMedication")
        assertTrue("RTL: Cancel must be right of Save", cancel.left > save.left)
        compose.assertAllTextWhole("medicationSheet")
    }

    // ================= Delivery plan sheet =================

    @Test
    fun delivery_titleAndLabelsAndNoNoRecordPlaceholder() {
        openDeliveryPlan(planPatient.copy(obstetricNotes = null))
        assertStyle("sheet title", text(str(R.string.edit_delivery_plan), "deliveryPlanSheet"), 18f, FontWeight.Bold, primaryColor)
        listOf("Agreed delivery hospital", "Agreed delivery fees", "Delivery plan", "Postpartum note").forEach { label ->
            assertStyle(label, text(label, "deliveryPlanSheet"), 12f, FontWeight.SemiBold)
        }
        assertStyle("Delivered label", deliveredLabel, 16f, FontWeight.Normal)
        val noRecord = compose.onAllNodes(hasText("No record", substring = true, ignoreCase = true) and inTagged("deliveryPlanSheet"), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("empty boxes show no 'No record' placeholder", noRecord.isEmpty())
    }

    @Test
    fun delivery_fourPlanFieldsComeFirstAndTheOutcomeSectionIsLast() {
        openDeliveryPlan()
        node("deliveryOutcomeSwitch").performScrollTo()
        val tops = listOf("deliveryPlanHospitalField", "deliveryPlanFeesField", "deliveryPlanPlanField", "deliveryPlanPostpartumField")
            .map { it to bounds(it).top }
        tops.zipWithNext().forEach { (a, b) -> assertTrue("${a.first} above ${b.first}", a.second < b.second) }
        val outcomeHeading = bounds(text(str(R.string.delivery_outcome_section), "deliveryPlanSheet"))
        assertTrue("outcome heading below the postpartum field", outcomeHeading.top >= bounds("deliveryPlanPostpartumField").bottom)
        assertTrue("hospital above the Delivered switch", bounds("deliveryPlanHospitalField").top < bounds("deliveryOutcomeSwitch").top)
        assertTrue("Delivered row below the outcome heading", bounds(toggleRow()).top >= outcomeHeading.bottom)
        // Nothing of the section sits above the plan fields.
        assertTrue(bounds("deliveryOutcomeSwitch").top > bounds("deliveryPlanPostpartumField").bottom)
    }

    @Test
    fun delivery_toggleRowIsAlignedWithTheFields_offAndOn() {
        openDeliveryPlan()
        node("deliveryOutcomeSwitch").performScrollTo()
        assertRowAligned(rowGeometry(), rtl = false)
        click("deliveryOutcomeSwitch")
        assertTrue(state.patientDraft!!.values.delivered)
        node("deliveryOutcomeSwitch").performScrollTo()
        assertRowAligned(rowGeometry(), rtl = false)
    }

    @Test
    fun delivery_toggleRowStaysFixedWhenToggledAndTheNewFieldsAppearBelowIt() {
        openDeliveryPlan()
        node("deliveryOutcomeSwitch").performScrollTo()
        compose.waitForIdle()
        val off = rowGeometry()
        assertFalse(exists("deliveryOutcomeDateField"))

        click("deliveryOutcomeSwitch")
        val on = rowGeometry()
        assertNear("row height on", off.height, on.height, 0.5.dp)
        assertNear("hospital-to-row distance on", off.hospitalToRow, on.hospitalToRow, 0.5.dp)
        assertNear("label offset in row on", off.label.top - off.row.top, on.label.top - on.row.top, 0.5.dp)
        assertNear("switch offset in row on", off.switch.top - off.row.top, on.switch.top - on.row.top, 0.5.dp)
        assertNear("switch x on", off.switch.left, on.switch.left, 0.5.dp)
        val date = bounds("deliveryOutcomeDateField")
        if (abs((on.rowInSheet - off.rowInSheet).value) > 0.5f) {
            // The row may only move when the body scrolled to reveal the new date field: then it moved up and the date
            // field is whole above the Cancel and Save row.
            assertTrue("row moved down (${off.rowInSheet} -> ${on.rowInSheet})", on.rowInSheet < off.rowInSheet)
            assertTrue("date field revealed whole above the buttons", date.bottom <= bounds("cancelDeliveryPlan").top + 0.5.dp)
        }
        assertTrue("date field below the row", date.top >= on.row.bottom - 0.5.dp)
        listOf("deliveryModeChip-caesarean", "deliveryOutcomeChip-live_birth").forEach { tag ->
            assertTrue("$tag below the row", bounds(tag).top >= on.row.bottom - 0.5.dp)
        }

        node("deliveryOutcomeSwitch").performScrollTo().performClick()
        compose.waitForIdle()
        val offAgain = rowGeometry()
        assertFalse(exists("deliveryOutcomeDateField"))
        assertNear("row height off again", off.height, offAgain.height, 0.5.dp)
        assertNear("hospital-to-row distance off again", off.hospitalToRow, offAgain.hospitalToRow, 0.5.dp)
        assertNear("switch x off again", off.switch.left, offAgain.switch.left, 0.5.dp)
    }

    @Test
    fun delivery_360dpAt200PercentText_labelWrapsWithoutOverlapAndRowGrowsOnlyAsNeeded() {
        narrowWindow(2.0f)
        openDeliveryPlan()
        assertSheetNarrowAndScaled("deliveryPlanSheet", text(str(R.string.edit_delivery_plan), "deliveryPlanSheet"), 2.0f)
        node("deliveryOutcomeSwitch").performScrollTo()
        val g = rowGeometry()
        assertRowAligned(g, rtl = false)
        compose.assertTextNotClipped("Delivered label", deliveredLabel)
        val labelHeight = g.label.bottom - g.label.top
        val needed = maxOf(56.dp, labelHeight, g.switch.bottom - g.switch.top)
        assertNear("row grows only as needed", needed, g.height, 1.dp)
        compose.assertAllTextWhole("deliveryPlanSheet")
        listOf("cancelDeliveryPlan", "saveDeliveryPlan").forEach { assertMinTouchTarget(it) }

        click("deliveryOutcomeSwitch")
        val on = rowGeometry()
        assertNear("row height at 200% text on", g.height, on.height, 0.5.dp)
        compose.assertAllTextWhole("deliveryPlanSheet")
    }

    @Test
    fun delivery_rtlMirrorsTheToggleRow() {
        openDeliveryPlan(layoutDirection = LayoutDirection.Rtl)
        node("deliveryOutcomeSwitch").performScrollTo()
        assertRowAligned(rowGeometry(), rtl = true)
        click("deliveryOutcomeSwitch")
        node("deliveryOutcomeSwitch").performScrollTo()
        assertRowAligned(rowGeometry(), rtl = true)
        val cancel = bounds("cancelDeliveryPlan")
        val save = bounds("saveDeliveryPlan")
        assertTrue("RTL: Cancel must be right of Save", cancel.left > save.left)
    }

    @Test
    fun delivery_saveGateAndDiscardAreUnchanged() {
        openDeliveryPlan()
        node("saveDeliveryPlan").assertIsNotEnabled()
        assertMinTouchTarget("cancelDeliveryPlan")
        assertMinTouchTarget("saveDeliveryPlan")
        node("deliveryPlanFeesField").performScrollTo().performTextReplacement("25000 EGP")
        compose.waitForIdle()
        node("saveDeliveryPlan").assertIsEnabled()
        click("cancelDeliveryPlan")
        node("deliveryPlanDiscardDialog").assertIsDisplayed()
        assertStyle("Discard", text(str(R.string.discard), "deliveryPlanDiscardConfirm"), 14f, FontWeight.SemiBold, errorColor)
        assertStyle("Keep editing", text(str(R.string.keep_editing), "deliveryPlanDiscardKeep"), 14f, FontWeight.SemiBold, primaryColor)
        click("deliveryPlanDiscardKeep")
        node("deliveryPlanSheet").assertIsDisplayed()
        click("cancelDeliveryPlan")
        click("deliveryPlanDiscardConfirm")
        assertFalse(exists("deliveryPlanSheet"))
        assertEquals(freeNotes, state.patient(planPatient.id)!!.obstetricNotes)
    }

    // ================= ChoiceGroup default callers =================

    @Test
    fun choiceGroup_defaultTitleKeepsTitleMedium() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    titleMediumStyle = MaterialTheme.typography.titleMedium
                    ChoiceGroup(
                        label = "Default group",
                        selected = "A",
                        choices = listOf("A", "B"),
                        enabled = true,
                        tagPrefix = "s11Choice",
                        onSelected = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        val style = layoutOf("default title", hasText("Default group")).layoutInput.style
        assertEquals("default title size", titleMediumStyle.fontSize, style.fontSize)
        assertEquals("default title weight", titleMediumStyle.fontWeight, style.fontWeight)
        assertTrue("unknown chip is still first by default", exists("s11Choice-0") && exists("s11Choice-2"))
    }
}
