FILE: app/src/androidTest/java/com/sherifradwan/ancnative/ScansTabRedesignTest.kt
```kotlin
package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.text.AnnotatedString
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
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.ScansTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Scans tab band card (redesign slice 7): band chips, biometry grid, photos, actions, 360dp and RTL. */
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
        date: String = today.toString(),
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
        date = date,
        bpd = bpd,
        hc = hc,
        ac = ac,
        fl = fl,
        efw = efw,
        fhr = fhr,
        presentation = presentation,
        placenta = placenta,
        amnioticFluid = amnioticFluid,
        impression = impression,
        imageUris = imageUris,
    )

    private fun patientWith(vararg scans: DemoUltrasoundScan, status: String? = "active") =
        base.copy(status = status, dating = dating(), visits = emptyList(), ultrasoundScans = scans.toList())

    private fun showScans(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
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
                            ScansTab(
                                patient = patient,
                                canEdit = canEdit,
                                onEvent = {},
                                onViewPhotos = onViewPhotos,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun inTagged(tag: String): SemanticsMatcher =
        hasAnyAncestor(hasTestTag(tag))

    private fun hasAnyAncestor(matcher: SemanticsMatcher): SemanticsMatcher =
        SemanticsMatcher("has any ancestor matching $matcher") { node ->
            generateSequence(node.parent) { it.parent }.any { matcher.matches(it) }
        }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.assertNotEllipsized(
        text: String,
        containerTag: String? = null,
    ) {
        val baseMatcher = hasText(text, substring = true)
        val matcher = if (containerTag != null) baseMatcher and inTagged(containerTag) else baseMatcher
        val nodes = onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("No node found for text '$text'", nodes.isNotEmpty())
        val ellipsized = nodes.any { node ->
            val texts = node.config.getOrNull(SemanticsProperties.Text) ?: emptyList<AnnotatedString>()
            texts.any { it.text.contains("…") || it.text.contains('\u2026') }
        }
        assertTrue("Text '$text' is ellipsized", !ellipsized)
    }

    private fun inCard(id: String): SemanticsMatcher = inTagged("ultrasound-$id")

    private fun countInCard(id: String, matcher: SemanticsMatcher) =
        compose.onAllNodes(matcher and inCard(id), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 48.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 48.dp)
    }

    @Test
    fun normalScan_showsDateGaChipPhotoCountBiometryImpressionPhotosAndActions() {
        val s = scan(
            id = "s-normal",
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
            imageUris = listOf("uri://1", "uri://2"),
        )
        var viewedScan: DemoUltrasoundScan? = null
        var viewedIdx: Int = -1
        showScans(patientWith(s)) { scan, idx ->
            viewedScan = scan
            viewedIdx = idx
        }

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()

        // GA chip (ltrIsolate) tagged gestationalAgeBadge
        compose.onNode(hasTestTag("gestationalAgeBadge") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("20+0 wks", substring = true) and inTagged("gestationalAgeBadge"), useUnmergedTree = true)
            .assertIsDisplayed()

        // Photo-count chip and line both show "2 photos" -> appears twice.
        compose.onAllNodes(hasText("2 photos", substring = true) and inCard(s.id), useUnmergedTree = true)
            .assertCountEquals(2)

        // Biometry labels
        listOf(
            "BPD", "HC", "AC", "FL", "EFW", "FHR",
            "Presentation", "Placenta", "Amniotic fluid",
        ).forEach { label ->
            assertTrue("missing label $label", countInCard(s.id, hasText(label, substring = true)) >= 1)
        }

        // Measurement values (ltrIsolated, match by substring)
        compose.onNode(hasText("49 mm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("180 mm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("150 mm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("32 mm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("148 bpm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("350 g", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()

        // Presentation field tagged
        compose.onNodeWithTag("scanPresentation", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Cephalic") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()

        // Impression and no biometry caption
        compose.onNode(hasText("Impression") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Normal anatomy scan") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(countInCard(s.id, hasText("Ultrasound Biometry", substring = true)) == 0)

        // Photo buttons with tags, 48dp, and click callback
        compose.onNodeWithTag("wsScanPhotoChip-${s.id}-0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Photo 1") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("wsScanPhotoChip-${s.id}-0")
        compose.onNodeWithTag("wsScanPhotoChip-${s.id}-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Photo 2") and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("wsScanPhotoChip-${s.id}-1")

        compose.onNodeWithTag("wsScanPhotoChip-${s.id}-0", useUnmergedTree = true).performClick()
        assertEquals(s.id, viewedScan?.id)
        assertEquals(0, viewedIdx)
        compose.onNodeWithTag("wsScanPhotoChip-${s.id}-1", useUnmergedTree = true).performClick()
        assertEquals(1, viewedIdx)

        // Actions with tags, descriptions, and 48dp touch targets
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasTestTag("editUltrasound-${s.id}") and hasContentDescription("Edit scan")).assertExists()
        assertMinTouchTarget("editUltrasound-${s.id}")
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("delete-ultrasound-${s.id}")
    }

    @Test
    fun recordNewScanButton_presentOnlyWhenCanEditAndNotArchived() {
        val s = scan("s-btn")
        showScans(patientWith(s), canEdit = true)
        compose.onNodeWithTag("recordNewScanBtn").assertIsDisplayed()

        showScans(patientWith(s), canEdit = false)
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()

        showScans(patientWith(s, status = "archived"), canEdit = true)
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()
    }

    @Test
    fun readOnly_hidesEditAndDeleteActions() {
        val s = scan("s-ro", bpd = "49")
        showScans(patientWith(s), canEdit = false)

        compose.onNodeWithTag("ultrasound-${s.id}").assertIsDisplayed()
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("recordNewScanBtn").assertDoesNotExist()
    }

    @Test
    fun emptyState_showsNoRecords() {
        showScans(patientWith())
        compose.onNodeWithTag("scansTabContent").assertIsDisplayed()
        compose.onNode(hasText("No records"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun scanWithNoBiometryAndNoImpression_stillRendersDateAndActions() {
        val s = scan("s-empty")
        showScans(patientWith(s), canEdit = true)

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()

        listOf("BPD", "HC", "AC", "FL", "EFW", "FHR", "Presentation", "Placenta", "Amniotic fluid", "Impression").forEach {
            assertTrue("unexpected '$it' for empty scan", countInCard(s.id, hasText(it, substring = true)) == 0)
        }
    }

    @Test
    fun blankBiometryValues_areSkipped() {
        val s = scan("s-partial", bpd = "49", hc = null, ac = "", fl = "32")
        showScans(patientWith(s))

        compose.onNodeWithTag("ultrasound-${s.id}").performScrollTo().assertIsDisplayed()
        assertTrue(countInCard(s.id, hasText("BPD", substring = true)) == 1)
        assertTrue(countInCard(s.id, hasText("FL", substring = true)) == 1)
        assertTrue(countInCard(s.id, hasText("HC", substring = true)) == 0)
        assertTrue(countInCard(s.id, hasText("AC", substring = true)) == 0)
    }

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
            s.date,
            "20+0 wks",
            "BPD",
            "49 mm",
            "HC",
            "180 mm",
            "FHR",
            "148 bpm",
            "Presentation",
            "Cephalic",
            "Amniotic fluid",
            "Oligohydramnios",
            "Impression",
            "Follow up recommended",
        ).forEach { compose.assertNotEllipsized(it, "ultrasound-${s.id}") }

        val card = compose.onNodeWithTag("ultrasound-${s.id}").getUnclippedBoundsInRoot()
        assertTrue("card must fit the 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
        compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).assertIsDisplayed()
        assertMinTouchTarget("editUltrasound-${s.id}")
        val edit = compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must stay inside the card (edit=$edit card=$card)", edit.right <= card.right + 1.dp)
    }

    @Test
    fun rtl_valuesStayLtrIsolated_andActionsSitAtLayoutEnd() {
        val s = scan(
            id = "s-rtl",
            bpd = "49",
            fhr = "148",
            presentation = "Cephalic",
        )
        showScans(patientWith(s), layoutDirection = LayoutDirection.Rtl)

        // LTR-isolated measurement values and GA chip.
        compose.onNode(hasText("49 mm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("148 bpm", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("20+0 wks", substring = true) and inCard(s.id), useUnmergedTree = true).assertIsDisplayed()

        // Layout end in RTL is the visual left: Edit (last action, rendered after Delete in Row) is left of Delete,
        // and both left of the title.
        val edit = compose.onNodeWithTag("editUltrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val delete = compose.onNodeWithTag("delete-ultrasound-${s.id}", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = compose.onNode(hasText(s.date) and inCard(s.id), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Edit must be left of Delete in RTL (edit=$edit delete=$delete)", edit.right <= delete.left + 1.dp)
        assertTrue("actions must be left of the title in RTL (delete=$delete title=$title)", delete.right <= title.left + 1.dp)
    }
}
```
[Tokens] upload: 19297 download: 5609 cache_read: 4096 cache_write: 0

