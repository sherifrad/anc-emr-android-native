package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.MedsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Meds tab tune (slice 9): type tokens, the 12 dp list gap, the header with no extra top space, the tighter empty
 * state, 48 dp targets, 360 dp at large text, RTL and dark theme.
 *
 * Corner radii (RecordShapes.Small/Large) are not checked: shape and clip are not exposed in semantics, and a pixel
 * probe of anti-aliased corners would be fragile. They are covered by code review of the four files.
 */
@RunWith(AndroidJUnit4::class)
class MedsTuneTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun med(
        id: String,
        name: String,
        dosage: String,
        frequency: String,
        status: String = MEDICATION_STATUS_ACTIVE,
        nonCompliant: Boolean = false,
    ) = DemoMedicationRecord(
        id = id, patientId = "p-meds", name = name, dosage = dosage, route = "Oral", frequency = frequency,
        startDate = "2026-04-04", endDate = null, status = status, notes = null, isProphylaxis = true, isNonCompliant = nonCompliant,
    )

    private fun patient(vararg meds: DemoMedicationRecord) = DemoPatient(
        id = "p-meds", caseId = "M-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null,
        visits = emptyList(), medications = meds.toList(), status = "active",
    )

    private val iron = med("m1", "Iron Polymaltose Complex", "100 mg", "Once daily")
    private val calcium = med("m2", "Calcium + Vitamin D3", "500 mg / 400 IU", "Twice daily", nonCompliant = true)

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun catalog(): List<MedicationCatalogItem> =
        ClinicalCatalogRepository.getInstance(compose.activity).medicationCatalogItems.value

    /** One setContent per test: MedsTab hosted directly, as MedsTabRestorationTest does. */
    private fun showMeds(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        darkTheme: Boolean = false,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    CompositionLocalProvider(LocalDensity provides scaled, LocalLayoutDirection provides layoutDirection) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized.testTag(HOST)) {
                            MedsTab(
                                patient = patient, canEdit = canEdit, actionsAllowed = true, medicationDraft = null,
                                savePhase = SavePhase.IDLE, saveError = null, quickSave = null, stopUndo = null, onEvent = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun scrollTo(tag: String) = compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag(tag))
    private fun bounds(tag: String) = node(tag).getUnclippedBoundsInRoot()

    private fun styleOf(label: String, matcher: SemanticsMatcher): TextStyle {
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.single().layoutInput.style
    }

    private fun assertFont(label: String, matcher: SemanticsMatcher, size: TextUnit, weight: FontWeight? = null) {
        val style = styleOf(label, matcher)
        assertEquals("'$label' font size", size, style.fontSize)
        if (weight != null) assertEquals("'$label' font weight", weight, style.fontWeight)
    }

    private fun assertFont(tag: String, size: TextUnit, weight: FontWeight? = null) = assertFont(tag, hasTestTag(tag), size, weight)

    private fun assertNear(label: String, expected: Dp, actual: Dp, tolerance: Dp = 0.6.dp) {
        assertTrue("$label: expected $expected, was $actual", actual >= expected - tolerance && actual <= expected + tolerance)
    }

    private fun assertTarget(tag: String, min: Dp = 48.dp) {
        scrollTo(tag)
        val b = bounds(tag)
        assertTrue("$tag height ${b.height} < $min", b.height >= min - 0.01.dp)
        assertTrue("$tag width ${b.width} < 48dp", b.width >= 47.99.dp)
    }

    private fun assertInside(inner: String, outer: String) {
        val i = bounds(inner)
        val o = bounds(outer)
        assertTrue("$inner sticks out of $outer (inner=$i outer=$o)", i.left >= o.left - 0.5.dp && i.right <= o.right + 0.5.dp)
    }

    // ---- Type tokens ----

    @Test
    fun cardTitleAndStopUseTheRecordTypeSizes() {
        showMeds(patient(iron.copy(notes = "Take two hours apart from calcium"), calcium))
        assertFont("medsActiveTitle", 18.sp, FontWeight.Bold)
        scrollTo("medication-m1")
        assertFont("medicationName-m1", 16.sp, FontWeight.Bold)
        assertFont("medicationInstructions-m1", 14.sp)
        assertFont("medicationNotes-m1", 14.sp)
        assertFont("stopMedicationLabel-m1", 12.sp, FontWeight.Bold)
        scrollTo("medication-m2")
        assertFont("medicationName-m2", 16.sp, FontWeight.Bold)
        assertFont("medicationInstructions-m2", 14.sp)
        assertFont("stopMedicationLabel-m2", 12.sp, FontWeight.Bold)
    }

    @Test
    fun pickerToggleTileNameAndActiveLabelUseTheRecordTypeSizes() {
        val item = catalog().first()
        val onList = med("m3", item.name, item.dosage.ifBlank { "1 mg" }, "Once daily").copy(route = item.route)
        showMeds(patient(iron, onList))
        scrollTo("medPickerToggle")
        assertFont("picker toggle", hasText(str(R.string.add_medication)) and inTagged("medPickerToggle"), 16.sp, FontWeight.SemiBold)
        node("medPickerToggle").performClick()
        compose.waitForIdle()
        node("medSearchField").performTextInput(item.name)
        compose.waitForIdle()
        scrollTo("medTile-${item.id}")
        node("medTileCheck-${item.id}").assertIsDisplayed()
        assertFont("tile name", hasText(item.name) and inTagged("medTile-${item.id}"), 16.sp, FontWeight.Bold)
        assertFont("tile Active label", hasText(str(R.string.medication_status_active)) and inTagged("medTile-${item.id}"), 12.sp)
        assertTarget("medTile-${item.id}")
        assertTarget("medTab-common")
        assertTarget("medTab-recent")
    }

    @Test
    fun emptyStateUsesCardNameAndFieldValueAndSixteenDpPadding() {
        showMeds(patient())
        node("medsEmptyState").assertIsDisplayed()
        val title = hasText(str(R.string.medications_empty_title)) and inTagged("medsEmptyState")
        val hint = hasText(str(R.string.medications_empty_hint)) and inTagged("medsEmptyState")
        assertFont("empty title", title, 16.sp, FontWeight.Bold)
        assertFont("empty hint", hint, 16.sp)
        val box = bounds("medsEmptyState")
        val titleTop = compose.onNode(title, useUnmergedTree = true).getUnclippedBoundsInRoot().top
        assertNear("empty state top padding", 16.dp, titleTop - box.top)
        assertNear("empty state bottom padding", 16.dp, box.bottom - bounds("medEmptyPrescribe").bottom)
        // Old padding (24 + 24) would make it 16 dp taller; the content is title, hint, button with 8 dp gaps.
        val hintBox = compose.onNode(hint, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val content = bounds("medEmptyPrescribe").bottom - titleTop
        assertNear("empty state height", content + 32.dp, box.height)
        assertTrue("hint sits between title and button", hintBox.top > titleTop && hintBox.bottom < bounds("medEmptyPrescribe").top)
    }

    @Test
    fun showPastRowUsesFieldValueBold() {
        showMeds(patient(iron, med("m9", "Methyldopa", "250 mg", "Twice daily", status = MEDICATION_STATUS_DISCONTINUED)))
        scrollTo("medShowPastToggle")
        assertFont(
            "Show past",
            hasText(pluralString(R.plurals.medication_show_past, 1)) and inTagged("medShowPastToggle"),
            16.sp,
            FontWeight.Bold,
        )
        assertTarget("medShowPastToggle")
    }

    // ---- Spacing ----

    @Test
    fun cardsSitTwelveDpApartAndTheHeaderStartsAtTheContentPadding() {
        showMeds(patient(iron, calcium))
        val content = bounds("medsTabContent")
        val title = bounds("medsActiveTitle")
        val button = bounds("addCustomMedicationBtn")
        // The header row is as tall as its tallest child (the 48 dp Prescribe); the title is centred in it.
        val headerTop = minOf(title.top, button.top)
        assertNear("header top below the list top", 16.dp, headerTop - content.top)
        assertTrue("title top within 18 dp of the row's own centring (title=$title button=$button)",
            title.top - headerTop <= (button.height - title.height) / 2 + 0.6.dp)
        val first = bounds("medication-m1")
        val second = bounds("medication-m2")
        assertNear("gap between cards", 12.dp, second.top - first.bottom)
        assertNear("gap between header and first card", 12.dp, first.top - maxOf(title.bottom, button.bottom))
    }

    @Test
    fun readOnlyHeaderTitleIsSixteenDpBelowTheListTop() {
        showMeds(patient(iron), canEdit = false)
        node("addCustomMedicationBtn").assertDoesNotExist()
        val gap = bounds("medsActiveTitle").top - bounds("medsTabContent").top
        assertTrue("title top $gap below the list top, expected <= 18 dp (16 dp padding, no extra 6 dp)", gap <= 18.dp)
        assertNear("title top", 16.dp, gap)
    }

    // ---- Touch targets ----

    @Test
    fun cardAndPickerControlsKeepTheirTouchTargets() {
        showMeds(patient(iron, calcium))
        assertTarget("addCustomMedicationBtn")
        assertTarget("medicationToggle-m1")
        assertTarget("stopMedication-m1")
        assertTarget("delete-medication-m1")
        assertTarget("editMedication-m1")
        assertTarget("medPickerToggle", min = 56.dp)
    }

    // ---- 360 dp at large text ----

    private fun assertCardsWholeAt360(fontScale: Float) {
        showMeds(patient(iron, calcium), widthDp = 360, fontScale = fontScale)
        assertTrue("held to 360 dp", bounds(HOST).width <= 360.dp + 0.5.dp)
        for (id in listOf("m1", "m2")) {
            scrollTo("medication-$id")
            compose.assertTextNotClipped("$id name", hasTestTag("medicationName-$id"))
            compose.assertTextNotClipped("$id dose, route and frequency", hasTestTag("medicationInstructions-$id"))
            assertInside("medicationName-$id", "medication-$id")
            assertInside("medicationInstructions-$id", "medication-$id")
            assertInside("medication-$id", HOST)
            scrollTo("stopMedication-$id")
            compose.assertTextNotClipped("$id Stop label", hasTestTag("stopMedicationLabel-$id"))
            for (tag in listOf("stopMedication-", "delete-medication-", "editMedication-")) assertTarget(tag + id)
        }
        compose.assertTextNotClipped("title", hasTestTag("medsActiveTitle"))
    }

    @Test
    fun cardsKeepTheirTextWholeAt360dpAnd130PercentText() = assertCardsWholeAt360(1.3f)

    @Test
    fun cardsKeepTheirTextWholeAt360dpAnd200PercentText() = assertCardsWholeAt360(2.0f)

    // ---- RTL and dark ----

    @Test
    fun rtlKeepsSizesAndPutsTheActionsAtTheLayoutEnd() {
        showMeds(patient(iron, calcium), layoutDirection = LayoutDirection.Rtl)
        scrollTo("medication-m1")
        assertFont("medicationName-m1", 16.sp, FontWeight.Bold)
        assertFont("stopMedicationLabel-m1", 12.sp, FontWeight.Bold)
        compose.assertTextNotClipped("m1 name", hasTestTag("medicationName-m1"))
        val card = bounds("medication-m1")
        val name = bounds("medicationName-m1")
        val edit = bounds("editMedication-m1")
        val stop = bounds("stopMedication-m1")
        val mid = (card.left + card.right) / 2
        assertTrue("name starts on the right in RTL (name=$name card=$card)", name.right > mid)
        assertTrue("edit sits at the layout end, the left, in RTL (edit=$edit card=$card)", edit.right < mid)
        assertTrue("edit is left of stop in RTL (edit=$edit stop=$stop)", edit.right <= stop.left + 0.5.dp)
        assertInside("editMedication-m1", "medication-m1")
    }

    @Test
    fun darkThemeRendersTheTunedCardAndPicker() {
        showMeds(patient(iron, calcium), darkTheme = true)
        node("medsActiveTitle").assertIsDisplayed()
        assertFont("medsActiveTitle", 18.sp, FontWeight.Bold)
        scrollTo("medication-m2")
        node("medicationName-m2").assertIsDisplayed()
        assertFont("medicationInstructions-m2", 14.sp)
        assertFont("stopMedicationLabel-m2", 12.sp, FontWeight.Bold)
        scrollTo("medPickerToggle")
        node("medPickerToggle").performClick()
        compose.waitForIdle()
        node("medSearchField").assertExists()
    }

    private fun pluralString(id: Int, count: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

    private companion object {
        const val HOST = "medsTuneHost"
    }
}
