package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.ScansTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.VisitsTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scans tab band card (redesign slice 7): band chips, biometry grid, photos, actions, 360dp and RTL; plus the shared
 * RecordListHeader / RecordEmptyCard gates as seen through both the Scans and the Visits tab.
 */
@RunWith(AndroidJUnit4::class)
class ScansTabRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val base: DemoPatient = DemoFixtures.patients.first()
    private val today: LocalDate = LocalDate.now()

    // LMP 140 days ago -> 20+0 wks on a scan dated today.
    private val ga20w0dLmp: LocalDate = today.minusDays(140)

    private fun dating() = DemoPatientDating(
        patientId = base.id,
        lmpDate = ga20w0dLmp.toString(),
        ultrasoundEdd = null,
        agreedEdd = null,
        datingMethod = "LMP",
        notes = null,
    )

    private fun scan(
        id: String,
        bpd: String? = null,
        hc: String? = null,
        ac: String? = null,
        fl: String? = null,
        efw: String? = null,
        fhr: String? = null,
        presentation: String? = null,
        placenta: String? = null,
        amnioticFluid: String? = null,
        impression: String? = null,
        imageUris: List<String> = emptyList(),
    ) = DemoUltrasoundScan(
        id = id,
        patientId = base.id,
        date = today.toString(),
        gestationalAgeWeeks = null,
        gestationalAgeDays = null,
        fetalNumber = null,
        presentation = presentation,
        placentalSite = placenta,
        amnioticFluid = amnioticFluid,
        fetalHeartRate = fhr,
        biometryBpd = bpd,
        biometryHc = hc,
        biometryAc = ac,
        biometryFl = fl,
        estimatedFetalWeight = efw,
        impression = impression,
        imageUris = imageUris,
    )

    private fun fullScan(id: String, imageUris: List<String> = emptyList()) = scan(
        id = id,
        bpd = "49",
        hc = "180",
        ac = "150",
        fl = "32",
        efw = "350 g",
        fhr = "148",
        presentation = "Cephalic",
        placenta = "Anterior",
        amnioticFluid = "Normal",
        impression = "Normal anatomy scan",
        imageUris = imageUris,
    )

    private fun patientWith(
        vararg scans: DemoUltrasoundScan,
        visits: List<DemoVisit> = emptyList(),
        status: String? = "active",
    ) = base.copy(status = status, dating = dating(), visits = visits, ultrasoundScans = scans.toList())

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

    private fun showScans(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onEvent: (AncEvent) -> Unit = {},
        onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
    ) = host(widthDp, fontScale, layoutDirection) {
        ScansTab(patient = patient, canEdit = canEdit, onEvent = onEvent, onViewPhotos = onViewPhotos)
    }

    private fun showVisits(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        onStartVisit: (String, String?) -> Unit = { _, _ -> },
    ) = host(widthDp, fontScale, LayoutDirection.Ltr) {
        VisitsTab(patient = patient, canEdit = canEdit, onStartVisit = onStartVisit, onEvent = {})
    }

    private fun inCard(id: String): SemanticsMatcher = inTagged("ultrasound-$id")

    private fun countInCard(id: String, matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inCard(id), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun boundsInCard(id: String, text: String): Rect =
        compose.onNode(hasText(text) and inCard(id), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun layoutOf(id: String, text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(hasText(text) and inCard(id), useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$text' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 0.01dp absorbs px-to-dp float rounding (a 48dp button can read back as 47.999985dp); a real shortfall is >= 1px.
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    private fun scrollToTag(tag: String) {
        compose.onNodeWithTag("scansTabContent").performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    // ---- Band, chips, body ----

    @Test
    fun normalScan_showsDateGaChipPhotoChipBiometryAndImpression() {
        val s = fullScan("s-normal", imageUris = listOf("uri://1", "uri://2"))
        showScans(patientWith(s))

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasTestTag("gestationalAgeBadge") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦20+0 wks⁩") and inTagged("gestationalAgeBadge"), useUnmergedTree = true)
            .assertIsDisplayed()

        // The photo count lives only in the band chip now; the old "Ultrasound scan images, N photos" line is gone.
        compose.onAllNodes(hasText("2 photos", substring = true) and inCard(s.id), useUnmergedTree = true)
            .assertCountEquals(1)
        assertEquals(0, countInCard(s.id, hasText("Ultrasound scan images", substring = true)))
        assertEquals(0, countInCard(s.id, hasText("Ultrasound Biometry", substring = true)))
        // The deleted "Presentation: %s" combined string must not reappear.
        assertEquals(0, countInCard(s.id, hasText("Presentation:", substring = true)))

        listOf("BPD", "HC", "AC", "FL", "EFW", "FHR", "Presentation", "Placenta", "Amniotic fluid", "Impression")
            .forEach { label -> assertEquals("label $label", 1, countInCard(s.id, hasText(label))) }

        listOf("⁦49 mm⁩", "⁦180 mm⁩", "⁦150 mm⁩", "⁦32 mm⁩", "⁦350 g⁩", "⁦148 bpm⁩").forEach { value ->
            compose.onNode(hasText(value) and inCard(s.id), useUnmergedTree = true).assertExists()
        }
        // Word values are plain text, not LTR-isolated.
        listOf("Cephalic", "Anterior", "Normal", "Normal anatomy scan").forEach { value ->
            compose.onNode(hasText(value) and inCard(s.id), useUnmergedTree = true).assertExists()
        }
        compose.onNode(hasTestTag("scanPresentation") and inCard(s.id)).assertExists()
        compose.onNode(hasText("Cephalic") and inTagged("scanPresentation"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun onePhoto_chipUsesSingular_andNoPhotoChipWithoutImages() {
        val one = scan("s-one", bpd = "49", imageUris = listOf("uri://1"))
        val none = scan("s-none", bpd = "50")
        showScans(patientWith(one, none))

        compose.onNode(hasText("1 photo") and inCard(one.id), useUnmergedTree = true).assertExists()
        assertEquals(0, countInCard(none.id, hasText("photo", substring = true)))
        assertEquals(0, countInCard(none.id, hasText("Photo", substring = true)))
    }

    @Test
    fun measurements_threePerRow_wordsTwoPerRow() {
        val s = fullScan("s-rows")
        showScans(patientWith(s))
        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo()

        val bpd = boundsInCard(s.id, "BPD")
        val hc = boundsInCard(s.id, "HC")
        val ac = boundsInCard(s.id, "AC")
        val fl = boundsInCard(s.id, "FL")
        assertEquals("BPD/HC same row", bpd.top, hc.top, 1f)
        assertEquals("BPD/AC same row", bpd.top, ac.top, 1f)
        assertTrue("FL starts a second row (fl=$fl bpd=$bpd)", fl.top > bpd.bottom)
        assertEquals("FL is in the first column", bpd.left, fl.left, 1f)

        val presentation = boundsInCard(s.id, "Presentation")
        val placenta = boundsInCard(s.id, "Placenta")
        val fluid = boundsInCard(s.id, "Amniotic fluid")
        assertEquals("Presentation/Placenta share a row", presentation.top, placenta.top, 1f)
        assertTrue("Placenta is the second column", placenta.left > presentation.right)
        assertTrue("Amniotic fluid wraps to the next row", fluid.top > presentation.bottom)
    }

    @Test
    fun emphasis_allSixMeasurementsSemiBold_wordValuesNormal() {
        val s = fullScan("s-weight")
        showScans(patientWith(s))
        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo()

        listOf("⁦49 mm⁩", "⁦180 mm⁩", "⁦150 mm⁩", "⁦32 mm⁩", "⁦350 g⁩", "⁦148 bpm⁩").forEach { value ->
            assertEquals("$value weight", FontWeight.SemiBold, layoutOf(s.id, value).layoutInput.style.fontWeight)
        }
        listOf("Cephalic", "Anterior", "Normal").forEach { value ->
            assertEquals("$value weight", FontWeight.Normal, layoutOf(s.id, value).layoutInput.style.fontWeight)
        }
    }

    @Test
    fun longWordValue_at360dpFontScale1_3_sitsOnItsOwnRowAndStaysOneLine() {
        val s = scan(
            id = "s-long",
            bpd = "49",
            presentation = "Cephalic",
            placenta = "Posterior",
            amnioticFluid = "Oligohydramnios",
        )
        showScans(patientWith(s), widthDp = 360, fontScale = 1.3f)
        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()

        // Any word value over 10 chars puts every word field on its own row.
        val presentation = boundsInCard(s.id, "Cephalic")
        val placenta = boundsInCard(s.id, "Posterior")
        val fluid = boundsInCard(s.id, "Oligohydramnios")
        assertTrue("Placenta below Presentation (p=$presentation pl=$placenta)", placenta.top >= presentation.bottom)
        assertTrue("Amniotic fluid below Placenta (pl=$placenta f=$fluid)", fluid.top >= placenta.bottom)
        assertEquals("one column: same start", presentation.left, fluid.left, 1f)

        val layout = layoutOf(s.id, "Oligohydramnios")
        assertEquals("'Oligohydramnios' must not be split across lines", 1, layout.lineCount)
        assertFalse(layout.isLineEllipsized(0))
        assertTrue(
            "'Oligohydramnios' wider than its field (${layout.getLineRight(0)} > ${layout.size.width})",
            layout.getLineRight(0) <= layout.size.width + 1f,
        )
        compose.assertNotEllipsized("Oligohydramnios", "ultrasound-${s.id}")
        val card = compose.onNodeWithTag("ultrasound-${s.id}").getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
    }

    @Test
    fun blankBiometryValues_areSkipped() {
        val s = scan("s-partial", bpd = "49", hc = null, ac = "  ", fl = "32")
        showScans(patientWith(s))

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        assertEquals(1, countInCard(s.id, hasText("BPD")))
        assertEquals(1, countInCard(s.id, hasText("FL")))
        assertEquals(0, countInCard(s.id, hasText("HC")))
        assertEquals(0, countInCard(s.id, hasText("AC")))
        assertEquals(0, countInCard(s.id, hasText("Not recorded")))
    }

    @Test
    fun scanWithNoBiometryAndNoImpression_stillRendersDateAndActions() {
        val s = scan("s-empty")
        showScans(patientWith(s), canEdit = true)

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        listOf("BPD", "HC", "AC", "FL", "EFW", "FHR", "Presentation", "Placenta", "Amniotic fluid", "Impression")
            .forEach { assertEquals("unexpected '$it' for empty scan", 0, countInCard(s.id, hasText(it))) }
    }

    // ---- Photos ----

    @Test
    fun photoButtons_tagsLabelsDescriptionsAndTouchTargets() {
        val s = scan("s-photos", bpd = "49", imageUris = listOf("uri://1", "uri://2"))
        showScans(patientWith(s))

        listOf(0, 1).forEach { idx ->
            val tag = "wsScanPhotoChip-${s.id}-$idx"
            scrollToTag(tag)
            compose.onNodeWithTag(tag).assertIsDisplayed()
            compose.onNode(hasText("Photo ${idx + 1}") and inTagged(tag), useUnmergedTree = true).assertExists()
            compose.onNode(hasTestTag(tag) and hasContentDescription("Open photo ${idx + 1} of 2, scan ${s.date}"))
                .assertExists()
            assertMinTouchTarget(tag)
        }
    }

    @Test
    fun photoButtonClick_callsOnViewPhotosWithScanAndIndex() {
        val s = scan("s-click", bpd = "49", imageUris = listOf("uri://1", "uri://2", "uri://3"))
        val calls = mutableListOf<Pair<String, Int>>()
        showScans(patientWith(s), onViewPhotos = { scan, idx -> calls += scan.id to idx })

        listOf(2, 0, 1).forEach { idx ->
            val tag = "wsScanPhotoChip-${s.id}-$idx"
            scrollToTag(tag)
            compose.onNodeWithTag(tag).performClick()
            compose.waitForIdle()
        }
        assertEquals(listOf(s.id to 2, s.id to 0, s.id to 1), calls)
    }

    // ---- Actions ----

    @Test
    fun editAndDeleteActions_haveTagsTouchTargetsDescriptionAndEmitEvents() {
        val s = fullScan("s-edit")
        val events = mutableListOf<AncEvent>()
        val patient = patientWith(s)
        showScans(patient, canEdit = true, onEvent = { events += it })

        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertMinTouchTarget("editUltrasound-${s.id}")
        compose.onNode(hasTestTag("editUltrasound-${s.id}") and hasContentDescription("Edit scan")).assertExists()
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("delete-ultrasound-${s.id}")

        compose.onNodeWithTag("editUltrasound-${s.id}").performClick()
        compose.onNodeWithTag("delete-ultrasound-${s.id}").performClick()
        compose.waitForIdle()

        val edit = events.filterIsInstance<AncEvent.StartUltrasoundEditor>().single()
        assertEquals(patient.id, edit.patientId)
        assertEquals(s.id, edit.scanId)
        assertEquals(s.id, edit.newScanId)
        assertEquals(
            AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, s.id),
            events.filterIsInstance<AncEvent.RequestDelete>().single(),
        )
    }

    @Test
    fun readOnly_hidesEditDeleteAndRecordButton_butKeepsPhotoButtons() {
        val s = scan("s-ro", bpd = "49", imageUris = listOf("uri://1"))
        showScans(patientWith(s), canEdit = false)

        compose.onNodeWithTag("ultrasound-${s.id}").assertIsDisplayed()
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()
        compose.onNodeWithTag("wsScanPhotoChip-${s.id}-0").assertExists()
    }

    // ---- Shared RecordListHeader / RecordEmptyCard via both tabs ----

    @Test
    fun scansHeader_editableActive_showsTitleCountAndRecordButtonThatStartsNewScan() {
        val events = mutableListOf<AncEvent>()
        val patient = patientWith(scan("s-h1", bpd = "49"), scan("s-h2", bpd = "50"))
        showScans(patient, canEdit = true, onEvent = { events += it })

        compose.onNode(hasText("Ultrasound scans (2)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("recordNewScanBtn").assertIsDisplayed()
        compose.onNode(hasText("Record scan") and inTagged("recordNewScanBtn"), useUnmergedTree = true).assertExists()
        assertMinTouchTarget("recordNewScanBtn")
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(0)

        compose.onNodeWithTag("recordNewScanBtn").performClick()
        compose.waitForIdle()
        val start = events.filterIsInstance<AncEvent.StartUltrasoundEditor>().single()
        assertEquals(patient.id, start.patientId)
        assertNull("a new scan has no scanId", start.scanId)
        assertTrue("new scan id must be fresh", start.newScanId.isNotBlank() && start.newScanId !in listOf("s-h1", "s-h2"))
    }

    @Test
    fun scansHeader_readOnly_keepsTitleHidesButton() {
        showScans(patientWith(scan("s-h3", bpd = "49")), canEdit = false)
        compose.onNode(hasText("Ultrasound scans (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()
    }

    @Test
    fun scansHeader_archived_keepsTitleHidesButton() {
        showScans(patientWith(scan("s-h4", bpd = "49"), status = "archived"), canEdit = true)
        compose.onNode(hasText("Ultrasound scans (1)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()
    }

    @Test
    fun scansEmpty_showsZeroTitleAndOneNoRecordsCard() {
        showScans(patientWith())
        compose.onNodeWithTag("scansTabContent").assertIsDisplayed()
        compose.onNode(hasText("Ultrasound scans (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("recordNewScanBtn").assertIsDisplayed()
    }

    @Test
    fun visitsHeader_editableActive_recordButtonCallsOnStartVisitForNewVisit() {
        val calls = mutableListOf<Pair<String, String?>>()
        val patient = patientWith()
        showVisits(patient, canEdit = true, onStartVisit = { id, visitId -> calls += id to visitId })

        compose.onNode(hasText("Antenatal visits (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(1)
        compose.onNode(hasText("Record visit") and inTagged("recordNewVisitBtn"), useUnmergedTree = true).assertExists()
        assertMinTouchTarget("recordNewVisitBtn")

        compose.onNodeWithTag("recordNewVisitBtn").performClick()
        compose.waitForIdle()
        assertEquals(listOf(patient.id to null), calls)
    }

    @Test
    fun visitsHeader_readOnlyAndArchived_hideButtonKeepTitle() {
        showVisits(patientWith(), canEdit = false)
        compose.onNode(hasText("Antenatal visits (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()
        compose.onAllNodes(hasText("No records"), useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun visitsHeader_archived_hidesButton() {
        showVisits(patientWith(status = "archived"), canEdit = true)
        compose.onNode(hasText("Antenatal visits (0)"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()
    }

    @Test
    fun header_at360dpFontScale2_titleWholeAndButtonInsideViewport_bothTabs() {
        showScans(patientWith(), widthDp = 360, fontScale = 2.0f)
        compose.assertTextNotClipped("Ultrasound scans (0)", hasText("Ultrasound scans (0)"))
        assertMinTouchTarget("recordNewScanBtn")
        val scanBtn = compose.onNodeWithTag("recordNewScanBtn").getUnclippedBoundsInRoot()
        assertTrue("Record scan inside 360dp (right=${scanBtn.right})", scanBtn.right <= 360.dp + 1.dp)

        showVisits(patientWith(), widthDp = 360, fontScale = 2.0f)
        compose.assertTextNotClipped("Antenatal visits (0)", hasText("Antenatal visits (0)"))
        assertMinTouchTarget("recordNewVisitBtn")
        val visitBtn = compose.onNodeWithTag("recordNewVisitBtn").getUnclippedBoundsInRoot()
        assertTrue("Record visit inside 360dp (right=${visitBtn.right})", visitBtn.right <= 360.dp + 1.dp)
    }

    // ---- Narrow and RTL ----

    @Test
    fun narrow360dpFontScale2_nothingEllipsizedAndActionsStayInsideCard() {
        val s = scan(
            id = "s-narrow",
            bpd = "49",
            hc = "180",
            ac = "150",
            fl = "32",
            fhr = "148",
            presentation = "Cephalic",
            placenta = "Posterior",
            amnioticFluid = "Oligohydramnios",
            impression = "Follow up recommended",
        )
        showScans(patientWith(s), widthDp = 360, fontScale = 2.0f)

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        listOf(
            s.date, "20+0 wks", "BPD", "⁦49 mm⁩", "HC", "⁦180 mm⁩", "AC", "⁦150 mm⁩", "FL", "⁦32 mm⁩",
            "FHR", "⁦148 bpm⁩", "Presentation", "Cephalic", "Placenta", "Posterior",
            "Amniotic fluid", "Oligohydramnios", "Impression", "Follow up recommended",
        ).forEach { compose.assertNotEllipsized(it, "ultrasound-${s.id}") }
        assertEquals("'Oligohydramnios' must stay whole", 1, layoutOf(s.id, "Oligohydramnios").lineCount)

        val card = compose.onNodeWithTag("ultrasound-${s.id}").getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("editUltrasound-${s.id}")
        val edit = compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must stay inside the card (edit=$edit card=$card)", edit.right <= card.right + 1.dp)
    }

    @Test
    fun rtl_valuesStayLtrIsolated_andActionsSitAtLayoutEnd() {
        val s = scan(id = "s-rtl", bpd = "49", fhr = "148", presentation = "Cephalic")
        showScans(patientWith(s), layoutDirection = LayoutDirection.Rtl)

        compose.onNode(hasText("⁦49 mm⁩") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦148 bpm⁩") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦20+0 wks⁩") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()

        // Layout end in RTL is the visual left: Edit (last action) is left of Delete, and both left of the title.
        val edit = compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must be left of Delete in RTL (edit=$edit delete=$delete)", edit.right <= delete.left + 1.dp)
        assertTrue("actions must be left of the title in RTL (delete=$delete title=$title)", delete.right <= title.left + 1.dp)
    }
}
