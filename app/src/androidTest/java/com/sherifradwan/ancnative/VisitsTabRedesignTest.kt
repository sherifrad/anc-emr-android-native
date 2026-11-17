package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.buildVisitFindings
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.VisitsTab
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visits tab band card (redesign slice 6): band chips, plain vitals fields, actions, 360dp and RTL. */
@RunWith(AndroidJUnit4::class)
class VisitsTabRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val base: DemoPatient = DemoFixtures.patients.first()
    private val today: LocalDate = LocalDate.now()

    // LMP 191 days ago -> 27+2 wks on a visit dated today.
    private val ga27w2dLmp: LocalDate = today.minusDays(191)

    private fun dating() = DemoPatientDating(
        patientId = base.id,
        lmpDate = ga27w2dLmp.toString(),
        ultrasoundEdd = null,
        agreedEdd = null,
        datingMethod = "LMP",
        notes = null,
    )

    /** Findings use the canonical writer so the parser reads them exactly as saved visits are read. */
    private fun visit(
        id: String,
        bloodPressure: String? = "110/70",
        weight: String? = "70",
        sfhCm: String? = "27",
        fhr: String? = "140",
        urineProtein: String? = null,
        linkedUltrasoundId: String? = null,
    ) = DemoVisit(
        id = id,
        patientId = base.id,
        date = today.toString(),
        bloodPressure = bloodPressure,
        weight = weight,
        findings = buildVisitFindings(
            fundalHeight = sfhCm,
            oedema = null,
            fetalMovements = null,
            urineProtein = urineProtein,
            fetalHeartRate = fhr,
        ).ifBlank { null },
        notes = null,
        linkedUltrasoundId = linkedUltrasoundId,
    )

    private fun patientWith(vararg visits: DemoVisit, status: String? = "active") =
        base.copy(status = status, dating = dating(), visits = visits.toList(), ultrasoundScans = emptyList())

    private fun showVisits(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
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
                        Box(sized) {
                            VisitsTab(patient = patient, canEdit = canEdit, onStartVisit = { _, _ -> }, onEvent = {})
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun inCard(id: String): SemanticsMatcher = inTagged("visit-$id")

    private fun countInCard(id: String, matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inCard(id), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    @Test
    fun normalVisit_showsDateHeadingGaChipAndLabels_withoutFlagChip() {
        val v = visit("v-normal")
        showVisits(patientWith(v))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(v.date) and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasTestTag("gestationalAgeBadge") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("27+2 wks", substring = true) and inTagged("gestationalAgeBadge"), useUnmergedTree = true)
            .assertIsDisplayed()

        listOf("Blood pressure", "Weight", "Fetal heart rate", "Fundal height").forEach { label ->
            assertTrue("missing label $label", countInCard(v.id, hasText(label)) == 1)
        }
        listOf("Raised BP", "Severe BP", "Pre-eclampsia risk", "Raised, recheck", "Severe, urgent").forEach { flag ->
            assertTrue("unexpected '$flag' on a normal visit", countInCard(v.id, hasText(flag, substring = true)) == 0)
        }
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
    }

    @Test
    fun raisedBp_showsRaisedChipAndWord_notSevereOrPreEclampsia() {
        val v = visit("v-raised", bloodPressure = "145/92")
        showVisits(patientWith(v))

        compose.onNode(hasText("Raised BP") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Raised, recheck") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(countInCard(v.id, hasText("Severe", substring = true)) == 0)
        assertTrue(countInCard(v.id, hasText("Pre-eclampsia", substring = true)) == 0)
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
    }

    @Test
    fun severeBp_showsSevereChipAndWord_notRaised() {
        val v = visit("v-severe", bloodPressure = "165/110")
        showVisits(patientWith(v))

        compose.onNode(hasText("Severe BP") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Severe, urgent") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(countInCard(v.id, hasText("Raised", substring = true)) == 0)
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
    }

    @Test
    fun preEclampsiaRule_showsWarningBlockAndBandChip_notBpChips() {
        val v = visit("v-pe", bloodPressure = "145/95", urineProtein = "2+")
        showVisits(patientWith(v))

        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(
            hasText("Elevated BP (145/95) with proteinuria", substring = true) and inTagged("preEclampsiaWarning-${v.id}"),
            useUnmergedTree = true,
        ).assertExists()
        // One "Pre-eclampsia risk" in the band chip and one in the warning block.
        compose.onAllNodes(hasText("Pre-eclampsia risk") and inCard(v.id), useUnmergedTree = true).assertCountEquals(2)
        compose.onAllNodes(
            hasText("Pre-eclampsia risk") and inCard(v.id) and !inTagged("preEclampsiaWarning-${v.id}"),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        // The worst flag wins the band: no BP chip next to it.
        assertTrue(countInCard(v.id, hasText("Raised BP")) == 0)
        assertTrue(countInCard(v.id, hasText("Severe BP")) == 0)
    }

    @Test
    fun missingBpAndWeight_showNotRecorded() {
        val v = visit("v-missing", bloodPressure = null, weight = null)
        showVisits(patientWith(v))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasText("Not recorded") and inCard(v.id), useUnmergedTree = true).assertCountEquals(2)
        assertTrue(countInCard(v.id, hasText("mmHg", substring = true)) == 0)
    }

    @Test
    fun fundalHeightField_hiddenWhenSfhAbsent() {
        val v = visit("v-nosfh", sfhCm = null)
        showVisits(patientWith(v))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        assertTrue(countInCard(v.id, hasText("Fundal height")) == 0)
        assertTrue(countInCard(v.id, hasText("Fetal heart rate")) == 1)
        compose.onNode(hasText("⁦140 bpm⁩") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun linkedScan_showsScanLinkedChip() {
        val v = visit("v-linked", linkedUltrasoundId = "us-1")
        showVisits(patientWith(v))
        compose.onNode(hasText("Scan linked") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun editAndDeleteActions_haveTagsTouchTargetsAndDescription() {
        val v = visit("v-edit")
        showVisits(patientWith(v), canEdit = true)

        compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertMinTouchTarget("editVisit-${v.id}")
        compose.onNode(hasTestTag("editVisit-${v.id}") and hasContentDescription("Edit visit")).assertExists()

        compose.onNodeWithTag("delete-visit-${v.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("delete-visit-${v.id}")
    }

    @Test
    fun readOnly_hidesEditDeleteAndRecordButton() {
        val v = visit("v-ro")
        showVisits(patientWith(v), canEdit = false)

        compose.onNodeWithTag("visit-${v.id}").assertIsDisplayed()
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()
        compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("delete-visit-${v.id}", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun recordVisitButton_presentForEditableActivePatient() {
        showVisits(patientWith(visit("v-btn")), canEdit = true)
        compose.onNodeWithTag("recordNewVisitBtn").assertIsDisplayed()
        assertMinTouchTarget("recordNewVisitBtn")
    }

    @Test
    fun recordVisitButton_absentForArchivedPatient() {
        showVisits(patientWith(visit("v-arch"), status = "archived"), canEdit = true)
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()
    }

    @Test
    fun narrow360dpFontScale2_headingChipLabelsAndValuesNotClipped() {
        val v = visit("v-narrow", bloodPressure = "118/76", weight = "72")
        showVisits(patientWith(v), widthDp = 360, fontScale = 2.0f)

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        listOf(
            v.date,
            "27+2 wks",
            "Blood pressure",
            "⁦118/76 mmHg⁩",
            "Weight",
            "⁦72 kg⁩",
            "Fetal heart rate",
            "⁦140 bpm⁩",
        ).forEach { compose.assertNotEllipsized(it, "visit-${v.id}") }

        val card = compose.onNodeWithTag("visit-${v.id}").getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("editVisit-${v.id}")
        val edit = compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must stay inside the card (edit=$edit card=$card)", edit.right <= card.right + 1.dp)
    }

    @Test
    fun rtl_valuesStayLtrIsolated_andEditSitsAtLayoutEnd() {
        val v = visit("v-rtl", bloodPressure = "120/80", weight = "68")
        showVisits(patientWith(v), layoutDirection = LayoutDirection.Rtl)

        compose.onNode(hasText("⁦120/80 mmHg⁩") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦68 kg⁩") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("⁦27+2 wks⁩") and inCard(v.id), useUnmergedTree = true).assertIsDisplayed()

        // Layout end in RTL is the visual left: Edit (last action) is left of Delete, and both left of the title.
        val edit = compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithTag("delete-visit-${v.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = compose.onNode(hasText(v.date) and inCard(v.id), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must be left of Delete in RTL (edit=$edit delete=$delete)", edit.right <= delete.left + 1.dp)
        assertTrue("actions must be left of the title in RTL (delete=$delete title=$title)", delete.right <= title.left + 1.dp)
    }
}
