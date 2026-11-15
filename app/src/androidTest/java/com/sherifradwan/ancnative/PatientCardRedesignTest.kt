package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patientdirectory.PatientDirectoryCard
import com.sherifradwan.ancnative.ui.projectDashboardRow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatientCardRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val today: LocalDate = LocalDate.now()
    private val base: DemoPatient = DemoFixtures.patients.first()

    private fun show(
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection? = null,
        content: @Composable () -> Unit,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    val direction = layoutDirection ?: LocalLayoutDirection.current
                    CompositionLocalProvider(
                        LocalDensity provides scaled,
                        LocalLayoutDirection provides direction,
                    ) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showListCard(
        p: DemoPatient,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection? = null,
    ) = show(widthDp, fontScale, layoutDirection) {
        Box(Modifier.padding(horizontal = 8.dp)) {
            PatientDirectoryCard(
                patient = p,
                row = projectDashboardRow(p),
                onEvent = {},
                onOpenMenu = {},
            )
        }
    }

    private fun nonDeliveredPatient(
        riskLevel: String = "low",
        latestBp: String? = "110/70",
        displayName: String = base.displayName,
        bloodGroupText: String? = "A+",
    ): DemoPatient {
        val lmp = today.minusDays(150)
        return base.copy(
            displayName = displayName,
            bloodGroupText = bloodGroupText,
            riskLevel = riskLevel,
            delivered = false,
            deliveryDate = null,
            status = "active",
            dating = DemoPatientDating(
                patientId = base.id,
                lmpDate = lmp.toString(),
                ultrasoundEdd = null,
                agreedEdd = lmp.plusDays(280).toString(),
                datingMethod = "LMP",
                notes = null,
            ),
            visits = listOf(
                DemoVisit(
                    id = "v-bp",
                    date = today.minusDays(7).toString(),
                    bloodPressure = latestBp,
                    weight = "70",
                    findings = null,
                    notes = null,
                ),
            ),
        )
    }

    private fun deliveredPatient(): DemoPatient {
        val lmp = today.minusDays(320)
        return base.copy(
            riskLevel = "low",
            delivered = true,
            deliveryDate = today.minusDays(10).toString(),
            status = "active",
            dating = DemoPatientDating(
                patientId = base.id,
                lmpDate = lmp.toString(),
                ultrasoundEdd = null,
                agreedEdd = lmp.plusDays(280).toString(),
                datingMethod = "LMP",
                notes = null,
            ),
            visits = listOf(
                DemoVisit(
                    id = "v-bp-delivered",
                    date = today.minusDays(5).toString(),
                    bloodPressure = "110/70",
                    weight = "70",
                    findings = null,
                    notes = null,
                ),
            ),
        )
    }

    private fun normalPatient() = nonDeliveredPatient()

    private fun moderateRiskPatient() =
        nonDeliveredPatient(riskLevel = "moderate")

    private fun highRiskPatient() =
        nonDeliveredPatient(riskLevel = "high")

    private fun incompletePatient(): DemoPatient {
        val lmp = today.minusDays(150)
        return base.copy(
            displayName = base.displayName,
            bloodGroupText = null,
            riskLevel = "low",
            delivered = false,
            deliveryDate = null,
            status = "active",
            gravida = null,
            parity = null,
            phone = "",
            dating = DemoPatientDating(
                patientId = base.id,
                lmpDate = lmp.toString(),
                ultrasoundEdd = null,
                agreedEdd = lmp.plusDays(280).toString(),
                datingMethod = "LMP",
                notes = null,
            ),
            visits = listOf(
                DemoVisit(
                    id = "v-bp-incomplete",
                    date = today.minusDays(7).toString(),
                    bloodPressure = "110/70",
                    weight = "70",
                    findings = null,
                    notes = null,
                ),
            ),
        )
    }

    private fun severeBpPatient() =
        nonDeliveredPatient(latestBp = "160/110")

    private fun incompletePlusHighPatient(): DemoPatient {
        return incompletePatient().copy(riskLevel = "high")
    }

    private fun assertCoreTagsPresent(p: DemoPatient) {
        compose.onNodeWithTag("patient-${p.caseId}", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("patientMenu-${p.caseId}", useUnmergedTree = true).assertExists()
    }

    private fun firstMetricLabelTop(): Float =
        listOf("GA", "Delivery outcome", "Parity").mapNotNull { label ->
            runCatching {
                compose.onNodeWithText(label, substring = false, useUnmergedTree = true)
                    .getUnclippedBoundsInRoot().top.value
            }.getOrNull()
        }.min()

    private fun assertTagsInsideNameRowAndAboveFirstMetric(p: DemoPatient, vararg tagTexts: String, requireSameRow: Boolean = true) {
        val nameBounds = compose.onNodeWithText(p.displayName, substring = false, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val menuBounds = compose.onNodeWithTag("patientMenu-${p.caseId}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val nameRowTop = minOf(nameBounds.top.value, menuBounds.top.value)
        val nameRowBottom = maxOf(nameBounds.bottom.value, menuBounds.bottom.value)
        val metricTop = firstMetricLabelTop()

        tagTexts.forEach { text ->
            val tagBounds = compose.onNode(hasText(text, substring = false), useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val centerY = (tagBounds.top.value + tagBounds.bottom.value) / 2f
            if (requireSameRow) {
                assertTrue(
                    "Tag '$text' center must lie inside the name row",
                    centerY >= nameRowTop && centerY <= nameRowBottom,
                )
            }
            assertTrue(
                "Tag '$text' must be above the first metric label",
                tagBounds.bottom.value <= metricTop,
            )
        }
    }

    @Test
    fun normalStateShowsNoTagAndCoreMetrics() {
        val p = normalPatient()
        showListCard(p)
        assertCoreTagsPresent(p)

        compose.onAllNodes(hasText("High risk", substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes().also { assertEquals(0, it.size) }
        compose.onAllNodes(hasText("Moderate risk", substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes().also { assertEquals(0, it.size) }
        compose.onAllNodes(hasText("Incomplete", substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes().also { assertEquals(0, it.size) }
        compose.onAllNodes(hasText("Delivered", substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes().also { assertEquals(0, it.size) }

        compose.onNodeWithText("GA", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Parity", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("BP", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Blood group", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Due date", substring = false, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun moderateRiskStateShowsModerateRiskTagInNameRow() {
        val p = moderateRiskPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("Moderate risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsInsideNameRowAndAboveFirstMetric(p, "Moderate risk")
    }

    @Test
    fun highRiskStateShowsHighRiskTagInNameRow() {
        val p = highRiskPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("High risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsInsideNameRowAndAboveFirstMetric(p, "High risk")
    }

    @Test
    fun incompleteStateShowsIncompleteTagInNameRow() {
        val p = incompletePatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithTag("incompleteChipAction", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("incompleteChip", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Incomplete", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsInsideNameRowAndAboveFirstMetric(p, "Incomplete")
    }

    @Test
    fun deliveredStateShowsDeliveredTagAndDeliveryOutcomeMetric() {
        val p = deliveredPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithTag("deliveredTag", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Delivered", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Delivery outcome", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsInsideNameRowAndAboveFirstMetric(p, "Delivered")
    }

    @Test
    fun severeBpStateShowsTagInNameRow() {
        val p = severeBpPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        val severeBpNode = compose.onAllNodes(hasText("Severe", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected a severe BP tag to be present", severeBpNode.isNotEmpty())
        val severeBounds = compose.onNode(hasText("Severe", substring = true), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val nameBounds = compose.onNodeWithText(p.displayName, substring = false, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val menuBounds = compose.onNodeWithTag("patientMenu-${p.caseId}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val nameRowTop = minOf(nameBounds.top.value, menuBounds.top.value)
        val nameRowBottom = maxOf(nameBounds.bottom.value, menuBounds.bottom.value)
        val centerY = (severeBounds.top.value + severeBounds.bottom.value) / 2f
        assertTrue(centerY >= nameRowTop && centerY <= nameRowBottom)
        assertTrue(severeBounds.bottom.value <= firstMetricLabelTop())
    }

    @Test
    fun incompletePlusHighStateShowsBothTagsInNameRow() {
        val p = incompletePlusHighPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("High risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Incomplete", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("incompleteChipAction", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("incompleteChip", useUnmergedTree = true).assertExists()
        assertTagsInsideNameRowAndAboveFirstMetric(p, "High risk", "Incomplete", requireSameRow = false)
    }

    @Test
    fun rtlLayoutPlacesNameRightOfMenuAndIsolatesBloodGroup() {
        val p = normalPatient().copy(bloodGroupText = "A+")
        showListCard(p, layoutDirection = LayoutDirection.Rtl)
        assertCoreTagsPresent(p)

        val nameBounds = compose.onNodeWithText(p.displayName, substring = false, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val menuBounds = compose.onNodeWithTag("patientMenu-${p.caseId}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Name must be to the right of the menu in RTL layout",
            nameBounds.left > menuBounds.right,
        )

        compose.onNodeWithText("⁦A+⁩", substring = false, useUnmergedTree = true).assertExists()
    }

    @Test
    fun narrowViewportFontScale13KeepsNameAndTagsUnclipped() {
        val longName = "A very long patient name that should wrap to two lines without being clipped"
        val p = incompletePlusHighPatient().copy(displayName = longName)
        showListCard(p, widthDp = 360, fontScale = 1.3f)
        assertCoreTagsPresent(p)

        val nameNode = compose.onNodeWithText(longName, substring = false, useUnmergedTree = true)
        nameNode.assertExists().assertIsDisplayed()
        val nameBounds = nameNode.getBoundsInRoot()
        assertTrue("Name height should allow wrapping to two lines", nameBounds.height >= 32.dp)

        compose.onNodeWithText("High risk", substring = false, useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
            .also { bounds ->
                assertTrue(bounds.right <= 360.dp)
                assertTrue(bounds.left >= 0.dp)
            }

        compose.onNodeWithText("Incomplete", substring = false, useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
            .also { bounds ->
                assertTrue(bounds.right <= 360.dp)
                assertTrue(bounds.left >= 0.dp)
            }
    }

    // ---- Name direction: Arabic names start at the right edge of the name area, others at the left. ----

    /** Where the name's glyphs actually sit, in root dp, next to the name node's own (full-width) box. */
    private class NameGlyphs(
        val nodeLeft: Float,
        val nodeRight: Float,
        val nodeTop: Float,
        val nodeBottom: Float,
        val inkLeft: Float,
        val inkRight: Float,
        val layout: TextLayoutResult,
    )

    private fun nameGlyphs(name: String): NameGlyphs {
        val node = compose.onNodeWithText(name, substring = false, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        val action = node.config[SemanticsActions.GetTextLayoutResult].action
        assertNotNull("Name text must expose its layout", action)
        assertTrue("Name text layout must be available", action!!.invoke(results) && results.isNotEmpty())
        val layout = results.first()
        val px = compose.density.density
        val box = node.boundsInRoot
        val lines = 0 until layout.lineCount
        val lineLeft = lines.minOf { layout.getLineLeft(it) }
        val lineRight = lines.maxOf { layout.getLineRight(it) }
        return NameGlyphs(
            nodeLeft = box.left / px,
            nodeRight = box.right / px,
            nodeTop = box.top / px,
            nodeBottom = box.bottom / px,
            inkLeft = (box.left + lineLeft) / px,
            inkRight = (box.left + lineRight) / px,
            layout = layout,
        )
    }

    private fun cardLeftDp(p: DemoPatient): Float =
        compose.onNodeWithTag("patient-${p.caseId}", useUnmergedTree = true).getUnclippedBoundsInRoot().left.value

    private fun tagLeftDp(text: String): Float =
        compose.onNode(hasText(text, substring = false), useUnmergedTree = true).getUnclippedBoundsInRoot().left.value

    private val arabicName = "فاطمة علي"
    private val longArabicName =
        "فاطمة عبد الرحمن محمد إبراهيم عبد العزيز السيد الشربيني المنصوري الحسيني القاهري الإسكندراني"

    @Test
    fun arabicNameStartsAtRightEdgeOfNameAreaLeftOfTags() {
        val p = highRiskPatient().copy(displayName = arabicName)
        showListCard(p, widthDp = 360)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(arabicName)
        assertEquals("Arabic name must be one line here", 1, g.layout.lineCount)
        assertTrue(
            "Arabic name ink right (${g.inkRight}) must sit at the name area's right edge (${g.nodeRight})",
            g.inkRight >= g.nodeRight - 2f && g.inkRight <= g.nodeRight + 0.5f,
        )
        val tagLeft = tagLeftDp("High risk")
        assertTrue(
            "Name area right (${g.nodeRight}) must end left of the tags (left $tagLeft)",
            g.nodeRight <= tagLeft + 0.5f,
        )
        // An English name would start at the card padding; the Arabic one must start well to the right of it.
        val englishStart = cardLeftDp(p) + 12f
        assertTrue(
            "Arabic name ink left (${g.inkLeft}) must be clearly right of an English start ($englishStart)",
            g.inkLeft > englishStart + 24f,
        )
    }

    @Test
    fun englishNameStartsAtCardPadding() {
        val name = "Fatma Ali"
        val p = highRiskPatient().copy(displayName = name)
        showListCard(p, widthDp = 360)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(name)
        val padStart = cardLeftDp(p) + 12f
        assertEquals("Name area must begin at the card start padding", padStart, g.nodeLeft, 0.5f)
        assertEquals("English name ink must begin at the card padding", padStart, g.inkLeft, 1f)
        assertTrue(
            "Short English name must not be pushed right (ink right ${g.inkRight}, area right ${g.nodeRight})",
            g.inkRight < g.nodeRight - 24f,
        )
        assertTrue("Name area must end left of the tags", g.nodeRight <= tagLeftDp("High risk") + 0.5f)
    }

    @Test
    fun mixedNameStartingWithLatinStaysLeft() {
        val name = "Mona منى"
        val p = highRiskPatient().copy(displayName = name)
        showListCard(p, widthDp = 360)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(name)
        val padStart = cardLeftDp(p) + 12f
        assertEquals("Latin-first mixed name ink must begin at the card padding", padStart, g.inkLeft, 1f)
        assertTrue("Latin-first mixed name must not be right-aligned", g.inkRight < g.nodeRight - 24f)
    }

    @Test
    fun arabicNameStaysRightAlignedInRtlLayout() {
        val p = highRiskPatient().copy(displayName = arabicName)
        showListCard(p, widthDp = 360, layoutDirection = LayoutDirection.Rtl)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(arabicName)
        assertTrue(
            "Arabic name ink right (${g.inkRight}) must sit at the name area's right edge (${g.nodeRight}) in RTL",
            g.inkRight >= g.nodeRight - 2f && g.inkRight <= g.nodeRight + 0.5f,
        )
    }

    @Test
    fun longArabicNameAt360dpFontScale2WrapsEllipsizesAndClearsTags() {
        val p = incompletePlusHighPatient().copy(displayName = longArabicName)
        showListCard(p, widthDp = 360, fontScale = 2.0f)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(longArabicName)
        assertEquals("Long Arabic name must use exactly two lines", 2, g.layout.lineCount)
        assertTrue("Long Arabic name must ellipsize on its last line", g.layout.isLineEllipsized(1))
        assertTrue(
            "Name ink right (${g.inkRight}) must sit at the name area's right edge (${g.nodeRight})",
            g.inkRight >= g.nodeRight - 2f && g.inkRight <= g.nodeRight + 0.5f,
        )
        // Known platform limit: at 200% font the ellipsized RTL (Arabic) last line comes out ~2dp wider than
        // its own layout box (Android places it at about -2.3dp), and Compose's Text clips to its box, so the
        // leftmost "…" dot loses ~2dp. Widening or offsetting the box only moves the overflow with it, so it is
        // accepted. The checks below make sure everything EXCEPT that ellipsis dot stays inside the name area.
        val px0 = compose.density.density
        assertTrue(
            "First (unellipsized) name line must stay inside its area (line left ${g.layout.getLineLeft(0) / px0}dp)",
            g.layout.getLineLeft(0) >= -0.5f * px0,
        )
        // Every real glyph kept on the ellipsized line lies inside the area; only the ellipsis may overflow.
        val keptEnd = g.layout.getLineEnd(1, visibleEnd = true)
        assertTrue("Ellipsized line must keep some letters", keptEnd > g.layout.getLineStart(1))
        (g.layout.getLineStart(1) until keptEnd).filterNot { longArabicName[it].isWhitespace() }.forEach { i ->
            val box = g.layout.getBoundingBox(i)
            assertTrue(
                "Kept letter #$i ('${longArabicName[i]}') must stay inside the name area (left ${box.left / px0}dp)",
                box.left >= -0.5f * px0 && box.right <= (g.nodeRight - g.nodeLeft + 0.5f) * px0,
            )
        }
        // Pixels: any ink touching the area's left edge must be the ellipsis dot alone, i.e. only on line 1
        // in a short band sitting on that line's baseline, never a letter's full height.
        val image = compose.onNodeWithText(longArabicName, substring = false, useUnmergedTree = true)
            .captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        val edgeCols = maxOf(1, px0.toInt()) // leftmost 1dp
        val reference = pixels[0] // top-left is blank: line 0 is right-aligned and starts well inside the area
        val edgeInkRows = (0 until image.height).filter { y ->
            (0 until edgeCols).any { x -> pixels[y * image.width + x] != reference }
        }
        val baseline1 = g.layout.getLineBaseline(1)
        val dotBandTop = baseline1 - 6f * px0
        val dotBandBottom = baseline1 + 1.5f * px0
        assertTrue(
            "Ink at the left edge must lie only in the ellipsis dot band on line 1 " +
                "(rows $edgeInkRows, band ${dotBandTop.toInt()}..${dotBandBottom.toInt()})",
            edgeInkRows.all { it >= dotBandTop && it <= dotBandBottom },
        )
        assertTrue(
            "Left-edge ink must be at most one dot tall (${edgeInkRows.size} rows)",
            edgeInkRows.size <= (5f * px0).toInt(),
        )
        val px = compose.density.density
        assertTrue(
            "Both name lines must fit inside the name box (text ${g.layout.size.height / px}dp, box ${g.nodeBottom - g.nodeTop}dp)",
            g.layout.size.height / px <= (g.nodeBottom - g.nodeTop) + 0.5f,
        )

        listOf("High risk", "Incomplete").forEach { tag ->
            val b = compose.onNode(hasText(tag, substring = false), useUnmergedTree = true)
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
            assertTrue(
                "Tag '$tag' (left ${b.left.value}) must not overlap the name ink (right ${g.inkRight})",
                b.left.value >= g.inkRight - 0.5f,
            )
            assertTrue("Tag '$tag' must stay on screen", b.right <= 360.dp && b.left >= 0.dp)
        }
    }
}
