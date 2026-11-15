package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.editPatientEvent
import com.sherifradwan.ancnative.state.firstNotDoneStep
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patientdirectory.PatientDirectoryCard
import com.sherifradwan.ancnative.ui.projectDashboardRow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        onEvent: (AncEvent) -> Unit = {},
    ) = showListCards(listOf(p), widthDp, fontScale, layoutDirection, onEvent)

    /** Several dashboard cards stacked as on the list (one setContent); the column scrolls so none is squeezed. */
    private fun showListCards(
        patients: List<DemoPatient>,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection? = null,
        onEvent: (AncEvent) -> Unit = {},
    ) = show(widthDp, fontScale, layoutDirection) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            patients.forEach { p ->
                Box(Modifier.padding(horizontal = 8.dp)) {
                    PatientDirectoryCard(
                        patient = p,
                        row = projectDashboardRow(p),
                        onEvent = onEvent,
                        onOpenMenu = {},
                    )
                }
            }
        }
    }

    /** A copy of [p] with its own ids and name, so several cards can sit in one list. */
    private fun asCase(p: DemoPatient, n: Int, name: String): DemoPatient =
        p.copy(id = "pt-slot-$n", caseId = "case-slot-$n", displayName = name)

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

    // ---- Geometry in root dp, per card (several cards can share one list). ----

    private data class Box4(val l: Float, val t: Float, val r: Float, val b: Float) {
        val w: Float get() = r - l
        val h: Float get() = b - t
        val cy: Float get() = (t + b) / 2f
        fun overlaps(o: Box4): Boolean = l < o.r - 0.5f && o.l < r - 0.5f && t < o.b - 0.5f && o.t < b - 0.5f
        fun inside(o: Box4, tol: Float = 0.5f): Boolean =
            l >= o.l - tol && r <= o.r + tol && t >= o.t - tol && b <= o.b + tol
        override fun toString() = "[l=$l t=$t r=$r b=$b]"
    }

    /** Unclipped bounds (layout position and size), so a card scrolled out of the viewport still measures. */
    private fun SemanticsNode.box(): Box4 {
        val px = compose.density.density
        val pos = positionInRoot
        return Box4(pos.x / px, pos.y / px, (pos.x + size.width) / px, (pos.y + size.height) / px)
    }

    private fun inCard(p: DemoPatient): SemanticsMatcher = hasAnyAncestor(hasTestTag("patient-${p.caseId}"))

    private fun textNodeIn(p: DemoPatient, text: String): SemanticsNode =
        compose.onNode(hasText(text, substring = false) and inCard(p), useUnmergedTree = true).fetchSemanticsNode()

    private fun tagNodeIn(p: DemoPatient, tag: String): SemanticsNode =
        compose.onNode(hasTestTag(tag) and inCard(p), useUnmergedTree = true).fetchSemanticsNode()

    private fun hasTagIn(p: DemoPatient, text: String): Boolean =
        compose.onAllNodes(hasText(text, substring = false) and inCard(p), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

    private fun cardBox(p: DemoPatient): Box4 =
        compose.onNodeWithTag("patient-${p.caseId}", useUnmergedTree = true).fetchSemanticsNode().box()

    private fun nameBox(p: DemoPatient): Box4 = textNodeIn(p, p.displayName).box()

    private fun nameRowBottom(p: DemoPatient): Float =
        maxOf(nameBox(p).b, tagNodeIn(p, "patientMenu-${p.caseId}").box().b)

    /**
     * The tag surface around a tag's text: the text node's semantics parent (the Surface; the text's own bounds
     * exclude the tag padding). Checked to be a tight wrapper, never the slot or the card.
     */
    private fun chip(p: DemoPatient, text: String): Box4 {
        val t = textNodeIn(p, text)
        val tb = t.box()
        val parent = t.parent
        assertNotNull("Tag '$text' text must have a parent surface", parent)
        val pb = parent!!.box()
        assertTrue(
            "Tag '$text' surface $pb must tightly wrap its text $tb",
            tb.inside(pb) && pb.w <= tb.w + 2 * 8f + 2f,
        )
        return pb
    }

    private fun bpChip(p: DemoPatient): Box4 = tagNodeIn(p, "bpAlertLabel").box()

    private fun metricLabel(p: DemoPatient, label: String): Box4 = textNodeIn(p, label).box()

    private fun firstMetricLabelTop(p: DemoPatient): Float =
        listOf("GA", "Delivery outcome", "Parity").filter { hasTagIn(p, it) }.minOf { metricLabel(p, it).t }

    /** The text layout of a tag (or any text) in card [p]. */
    private fun layoutOf(p: DemoPatient, text: String): TextLayoutResult {
        val node = textNodeIn(p, text)
        val results = mutableListOf<TextLayoutResult>()
        val action = node.config[SemanticsActions.GetTextLayoutResult].action
        assertTrue("'$text' must expose its layout", action != null && action.invoke(results) && results.isNotEmpty())
        return results.first()
    }

    /**
     * The three slot boxes of card [p], computed from the real card width: slot 1 starts at the card's 12dp start
     * padding, the row ends 4dp (card) + 8dp (row) before the card's end edge, slots are equal with 8dp gaps.
     * In RTL the order is mirrored (slot 1 at the right).
     */
    private fun expectedSlots(p: DemoPatient, rtl: Boolean = false): List<Box4> {
        val card = cardBox(p)
        val rowStart = if (rtl) card.r - 12f else card.l + 12f
        val rowWidth = card.w - 12f - 4f - 8f
        val slotWidth = (rowWidth - 2 * 8f) / 3f
        return (0 until 3).map { n ->
            if (rtl) {
                val right = rowStart - n * (slotWidth + 8f)
                Box4(right - slotWidth, card.t, right, card.b)
            } else {
                val left = rowStart + n * (slotWidth + 8f)
                Box4(left, card.t, left + slotWidth, card.b)
            }
        }
    }

    /** Asserts every tag of card [p] stays inside its slot horizontally, inside the card, and its text is not clipped. */
    private fun assertTagsFitTheirSlots(p: DemoPatient, rtl: Boolean = false) {
        val slots = expectedSlots(p, rtl)
        val card = cardBox(p)
        val present = mutableListOf<Pair<String, Box4>>()
        listOf("High risk" to 0, "Moderate risk" to 0, "Incomplete" to 1, "Delivered" to 2).forEach { (text, slot) ->
            if (!hasTagIn(p, text)) return@forEach
            val c = chip(p, text)
            present += text to c
            assertTrue(
                "Tag '$text' $c must stay inside slot ${slot + 1} ${slots[slot]} horizontally",
                c.l >= slots[slot].l - 0.5f && c.r <= slots[slot].r + 0.5f,
            )
            assertTrue("Tag '$text' $c must stay inside the card $card", c.inside(card))
            val layout = layoutOf(p, text)
            val px = compose.density.density
            val textBox = textNodeIn(p, text).box()
            // The semantics layout is a re-layout at the slot's full max width, so its didOverflowWidth is true for
            // any tag narrower than the slot. Clipping is checked on what is drawn: no line wider than the tag
            // text box, no height overflow.
            val widestLine = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
            assertTrue(
                "Tag '$text' widest line (${widestLine / px}dp) must fit its text box $textBox",
                widestLine / px <= textBox.w + 0.5f,
            )
            assertFalse("Tag '$text' text must not overflow its height", layout.didOverflowHeight)
            assertTrue(
                "Tag '$text' text (${layout.size.width / px}x${layout.size.height / px}) must fit its box $textBox",
                layout.size.width / px <= textBox.w + 0.5f && layout.size.height / px <= textBox.h + 0.5f,
            )
        }
        if (hasTagIn(p, "Severe BP")) {
            val bp = bpChip(p)
            present += "Severe BP" to bp
            assertTrue(
                "Severe BP chip $bp must stay inside slot 1 ${slots[0]}",
                bp.l >= slots[0].l - 0.5f && bp.r <= slots[0].r + 0.5f,
            )
            assertTrue("Severe BP chip $bp must stay inside the card $card", bp.inside(card))
        }
        for (i in present.indices) for (j in i + 1 until present.size) {
            assertFalse(
                "Tags '${present[i].first}' ${present[i].second} and '${present[j].first}' ${present[j].second} overlap",
                present[i].second.overlaps(present[j].second),
            )
        }
    }

    /**
     * Tag-row contract: every tag sits below the name row (name and menu), above the first metric label, and no
     * two tags overlap. Bounds are the tag surfaces (see [chip]).
     */
    private fun assertTagsBelowNameAndAboveFirstMetric(p: DemoPatient, vararg tagTexts: String) {
        val nameRowBottom = nameRowBottom(p)
        val metricTop = firstMetricLabelTop(p)
        val chips = tagTexts.map { text -> text to chip(p, text) }
        chips.forEach { (text, b) ->
            assertTrue(
                "Tag '$text' (top ${b.t}) must sit below the name row (bottom $nameRowBottom)",
                b.t >= nameRowBottom - 0.5f,
            )
            assertTrue(
                "Tag '$text' (bottom ${b.b}) must be above the first metric label (top $metricTop)",
                b.b <= metricTop + 0.5f,
            )
        }
        for (i in chips.indices) for (j in i + 1 until chips.size) {
            assertFalse(
                "Tags '${chips[i].first}' ${chips[i].second} and '${chips[j].first}' ${chips[j].second} must not overlap",
                chips[i].second.overlaps(chips[j].second),
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
    fun moderateRiskStateShowsModerateRiskTagUnderName() {
        val p = moderateRiskPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("Moderate risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsBelowNameAndAboveFirstMetric(p, "Moderate risk")
    }

    @Test
    fun highRiskStateShowsHighRiskTagUnderName() {
        val p = highRiskPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("High risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk")
    }

    @Test
    fun incompleteStateShowsIncompleteTagUnderName() {
        val p = incompletePatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithTag("incompleteChipAction", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("incompleteChip", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Incomplete", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsBelowNameAndAboveFirstMetric(p, "Incomplete")
    }

    @Test
    fun deliveredStateShowsDeliveredTagAndDeliveryOutcomeMetric() {
        val p = deliveredPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithTag("deliveredTag", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Delivered", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Delivery outcome", substring = false, useUnmergedTree = true).assertIsDisplayed()
        assertTagsBelowNameAndAboveFirstMetric(p, "Delivered")
    }

    @Test
    fun severeBpStateShowsChipUnderName() {
        val p = severeBpPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        val severeBpNode = compose.onAllNodes(hasText("Severe", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected a severe BP tag to be present", severeBpNode.isNotEmpty())
        val severe = bpChip(p)
        assertTrue(
            "Severe BP chip (top ${severe.t}) must sit below the name row (bottom ${nameRowBottom(p)})",
            severe.t >= nameRowBottom(p) - 0.5f,
        )
        assertTrue(
            "Severe BP chip (bottom ${severe.b}) must be above the first metric label",
            severe.b <= firstMetricLabelTop(p) + 0.5f,
        )
    }

    @Test
    fun incompletePlusHighStateShowsBothTagsUnderName() {
        val p = incompletePlusHighPatient()
        showListCard(p)
        assertCoreTagsPresent(p)
        compose.onNodeWithText("High risk", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Incomplete", substring = false, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("incompleteChipAction", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("incompleteChip", useUnmergedTree = true).assertExists()
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk", "Incomplete")
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
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk", "Incomplete")
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

    private fun menuLeftDp(p: DemoPatient): Float =
        compose.onNodeWithTag("patientMenu-${p.caseId}", useUnmergedTree = true).getUnclippedBoundsInRoot().left.value

    private val arabicName = "فاطمة علي"
    private val longArabicName =
        "فاطمة عبد الرحمن محمد إبراهيم عبد العزيز السيد الشربيني المنصوري الحسيني القاهري الإسكندراني"

    @Test
    fun arabicNameStartsAtRightEdgeOfNameAreaAboveTags() {
        val p = highRiskPatient().copy(displayName = arabicName)
        showListCard(p, widthDp = 360)
        assertCoreTagsPresent(p)

        val g = nameGlyphs(arabicName)
        assertEquals("Arabic name must be one line here", 1, g.layout.lineCount)
        assertTrue(
            "Arabic name ink right (${g.inkRight}) must sit at the name area's right edge (${g.nodeRight})",
            g.inkRight >= g.nodeRight - 2f && g.inkRight <= g.nodeRight + 0.5f,
        )
        val menuLeft = menuLeftDp(p)
        assertTrue(
            "Name area right (${g.nodeRight}) must end left of the menu (left $menuLeft)",
            g.nodeRight <= menuLeft + 0.5f,
        )
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk")
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
        assertTrue("Name area must end left of the menu", g.nodeRight <= menuLeftDp(p) + 0.5f)
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk")
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
    fun longArabicNameAt360dpFontScale2WrapsEllipsizesAboveTags() {
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
            compose.onNode(hasText(tag, substring = false), useUnmergedTree = true).assertIsDisplayed()
            val b = chip(p, tag)
            assertTrue(
                "Tag '$tag' (top ${b.t}) must sit below the name (bottom ${g.nodeBottom}), clear of its ink",
                b.t >= g.nodeBottom - 0.5f,
            )
            assertTrue("Tag '$tag' must stay on screen", b.r <= 360f && b.l >= 0f)
        }
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk", "Incomplete")
        assertTagsFitTheirSlots(p)
    }

    // ---- Tag slots: each tag type at the same x on every card. ----

    private fun slotPatients(): List<DemoPatient> = listOf(
        asCase(highRiskPatient(), 1, "Slot High Only"),
        asCase(incompletePatient().copy(riskLevel = "moderate"), 2, "Slot Moderate Incomplete"),
        asCase(
            deliveredPatient().copy(riskLevel = "high", gravida = null, parity = null, phone = "", bloodGroupText = null),
            3,
            "Slot Delivered High Incomplete",
        ),
        asCase(normalPatient(), 4, "Slot No Tags"),
        asCase(incompletePatient(), 5, "Slot Incomplete Only"),
    )

    @Test
    fun stackedCardsPutEachTagTypeAtTheSameX() {
        val ps = slotPatients()
        val (high, modInc, delHighInc, none, incOnly) = ps
        showListCards(ps, widthDp = 360)

        // Fixtures carry the tags they are meant to carry.
        assertTrue(hasTagIn(high, "High risk") && !hasTagIn(high, "Incomplete") && !hasTagIn(high, "Delivered"))
        assertTrue(hasTagIn(modInc, "Moderate risk") && hasTagIn(modInc, "Incomplete") && !hasTagIn(modInc, "Delivered"))
        assertTrue(hasTagIn(delHighInc, "High risk") && hasTagIn(delHighInc, "Incomplete") && hasTagIn(delHighInc, "Delivered"))
        listOf("High risk", "Moderate risk", "Incomplete", "Delivered").forEach {
            assertFalse("No-tag card must not show '$it'", hasTagIn(none, it))
        }
        assertTrue(hasTagIn(incOnly, "Incomplete") && !hasTagIn(incOnly, "High risk") && !hasTagIn(incOnly, "Moderate risk"))

        // The chip helper measures the tagged surface itself.
        assertEquals(chip(modInc, "Incomplete").l, tagNodeIn(modInc, "incompleteChip").box().l, 0.5f)
        assertEquals(chip(delHighInc, "Delivered").l, tagNodeIn(delHighInc, "deliveredTag").box().l, 0.5f)

        val riskLefts = listOf(chip(high, "High risk").l, chip(modInc, "Moderate risk").l, chip(delHighInc, "High risk").l)
        val incompleteLefts = listOf(chip(modInc, "Incomplete").l, chip(delHighInc, "Incomplete").l, chip(incOnly, "Incomplete").l)
        val deliveredLeft = chip(delHighInc, "Delivered").l
        riskLefts.forEach { assertEquals("Risk tags must share one left x ($riskLefts)", riskLefts[0], it, 0.5f) }
        incompleteLefts.forEach {
            assertEquals("Incomplete tags must share one left x ($incompleteLefts)", incompleteLefts[0], it, 0.5f)
        }
        assertTrue(
            "Slot order must be risk < Incomplete < Delivered (${riskLefts[0]}, ${incompleteLefts[0]}, $deliveredLeft)",
            riskLefts[0] < incompleteLefts[0] && incompleteLefts[0] < deliveredLeft,
        )

        // Slot 1 starts at the name's left edge; slots follow the formula from the real card width.
        ps.filter { it != none }.forEach { p ->
            assertEquals("Slot 1 must start at the name's left edge (${p.displayName})", nameBox(p).l, riskLefts[0], 0.5f)
            val slots = expectedSlots(p)
            assertEquals("Slot 1 left (${p.displayName})", slots[0].l, riskLefts[0], 1f)
            assertEquals("Slot 2 left (${p.displayName})", slots[1].l, incompleteLefts[0], 1f)
            assertEquals("Slot 3 left (${p.displayName})", slots[2].l, deliveredLeft, 1f)
            assertTagsFitTheirSlots(p)
        }

        // An empty slot keeps its width: Incomplete alone still sits at the slot 2 x, not at the start.
        val incOnlyLeft = chip(incOnly, "Incomplete").l
        assertEquals("Incomplete alone must stay in slot 2", incompleteLefts[0], incOnlyLeft, 0.5f)
        assertTrue("Incomplete alone must not move to the start", incOnlyLeft > nameBox(incOnly).l + 24f)

        // Non-delivered cards: slots sit over the GA / Parity / BP columns.
        listOf(high, modInc, incOnly).forEach { p ->
            assertEquals("Slot 1 over GA (${p.displayName})", metricLabel(p, "GA").l, riskLefts[0], 1f)
            assertEquals("Slot 2 over Parity (${p.displayName})", metricLabel(p, "Parity").l, incompleteLefts[0], 1f)
            assertEquals("Slot 3 over BP (${p.displayName})", metricLabel(p, "BP").l, deliveredLeft, 1f)
        }

        // Tag row: absent without tags (card is shorter), at least 48dp on every tagged card.
        val noTagGap = firstMetricLabelTop(none) - nameRowBottom(none)
        assertTrue("No-tag card must have no tag row (gap $noTagGap)", noTagGap < 8f)
        assertTrue(
            "No-tag card (${cardBox(none).h}) must be shorter than a tagged one (${cardBox(high).h})",
            cardBox(none).h < cardBox(high).h - 40f,
        )
        ps.filter { it != none }.forEach { p ->
            val gap = firstMetricLabelTop(p) - nameRowBottom(p)
            // gap = 2dp spacing + tag row + 2dp spacing
            assertTrue("Tag row of ${p.displayName} must be at least 48dp (gap $gap)", gap - 4f >= 48f - 0.5f)
        }

        // Slots share one height: Incomplete and Delivered centre on the same line.
        val incCy = tagNodeIn(delHighInc, "incompleteChipAction").box().cy
        assertEquals("Incomplete and Delivered must centre on the same line", incCy, chip(delHighInc, "Delivered").cy, 0.75f)
    }

    @Test
    fun severeBpChipStacksUnderRiskTagInSlotOne() {
        val p = asCase(nonDeliveredPatient(riskLevel = "high", latestBp = "160/110"), 1, "Severe Under Risk")
        val q = asCase(incompletePatient(), 2, "Incomplete Reference")
        showListCards(listOf(p, q), widthDp = 360)

        val risk = chip(p, "High risk")
        val bp = bpChip(p)
        assertTrue("Severe BP chip (top ${bp.t}) must be under the risk tag (bottom ${risk.b})", bp.t >= risk.b - 0.5f)
        assertEquals("Severe BP chip must start where the risk tag starts", risk.l, bp.l, 0.5f)
        assertEquals("Severe BP chip must start at the name's left edge", nameBox(p).l, bp.l, 0.5f)
        assertTrue("Severe BP chip must end before slot 2", bp.r <= chip(q, "Incomplete").l + 0.5f)
        assertTagsBelowNameAndAboveFirstMetric(p, "High risk")
        assertTrue("Severe BP chip must be above the first metric", bp.b <= firstMetricLabelTop(p) + 0.5f)
        assertTagsFitTheirSlots(p)
    }

    /** True when every line break of [text] in [layout] falls between words. */
    private fun breaksOnlyBetweenWords(text: String, layout: TextLayoutResult): Boolean =
        (1 until layout.lineCount).all { i ->
            val start = layout.getLineStart(i)
            start > 0 && (text[start - 1].isWhitespace() || text[start].isWhitespace())
        }

    @Test
    fun narrowViewportFontScale13KeepsSingleWordTagsWholeAndUnclipped() {
        val a = asCase(nonDeliveredPatient(riskLevel = "moderate"), 1, "Scale Moderate")
        val b = asCase(
            deliveredPatient().copy(riskLevel = "high", gravida = null, parity = null, phone = "", bloodGroupText = null),
            2,
            "Scale Delivered High Incomplete",
        )
        showListCards(listOf(a, b), widthDp = 360, fontScale = 1.3f)

        listOf("High risk", "Incomplete", "Delivered").forEach { tag ->
            val layout = layoutOf(b, tag)
            if (tag.contains(' ')) {
                assertTrue("'$tag' must not break mid-word at 1.3x", breaksOnlyBetweenWords(tag, layout))
            } else {
                assertEquals("Single-word tag '$tag' must stay on one line at 1.3x", 1, layout.lineCount)
            }
        }
        assertTrue(
            "'Moderate risk' may wrap only between words at 1.3x",
            breaksOnlyBetweenWords("Moderate risk", layoutOf(a, "Moderate risk")),
        )
        listOf(a, b).forEach { p ->
            assertTagsFitTheirSlots(p)
            assertTagsBelowNameAndAboveFirstMetric(
                p,
                *listOf("High risk", "Moderate risk", "Incomplete", "Delivered").filter { hasTagIn(p, it) }.toTypedArray(),
            )
        }
    }

    @Test
    fun fontScale2KeepsTagsInsideTheirSlotsWithoutOverlap() {
        val a = asCase(nonDeliveredPatient(riskLevel = "moderate", latestBp = "160/110"), 1, "Big Moderate Severe")
        val b = asCase(
            deliveredPatient().copy(riskLevel = "high", gravida = null, parity = null, phone = "", bloodGroupText = null),
            2,
            "Big Delivered High Incomplete",
        )
        showListCards(listOf(a, b), widthDp = 360, fontScale = 2.0f)

        // Mid-word breaks of 'Incomplete' / 'Delivered' are accepted here; clipping and overlap are not.
        listOf(a, b).forEach { p ->
            assertTagsFitTheirSlots(p)
            val tags = listOf("High risk", "Moderate risk", "Incomplete", "Delivered").filter { hasTagIn(p, it) }
            assertTagsBelowNameAndAboveFirstMetric(p, *tags.toTypedArray())
            val card = cardBox(p)
            assertTrue("Card must fit the 360dp viewport", card.l >= 0f && card.r <= 360f + 0.5f)
            val gap = firstMetricLabelTop(p) - nameRowBottom(p)
            assertTrue("Tag row of ${p.displayName} must be at least 48dp at 2x (gap $gap)", gap - 4f >= 48f - 0.5f)
        }
        val bp = bpChip(a)
        assertTrue("Severe BP chip must stay under the risk tag at 2x", bp.t >= chip(a, "Moderate risk").b - 0.5f)
        assertTrue("Severe BP chip must be above the first metric at 2x", bp.b <= firstMetricLabelTop(a) + 0.5f)
    }

    @Test
    fun rtlMirrorsTagSlots() {
        val ps = slotPatients()
        val (high, modInc, delHighInc, _, incOnly) = ps
        showListCards(ps, widthDp = 360, layoutDirection = LayoutDirection.Rtl)

        val riskRights = listOf(chip(high, "High risk").r, chip(modInc, "Moderate risk").r, chip(delHighInc, "High risk").r)
        val incompleteRights = listOf(chip(modInc, "Incomplete").r, chip(delHighInc, "Incomplete").r, chip(incOnly, "Incomplete").r)
        val deliveredRight = chip(delHighInc, "Delivered").r
        riskRights.forEach { assertEquals("RTL risk tags must share one right x ($riskRights)", riskRights[0], it, 0.5f) }
        incompleteRights.forEach {
            assertEquals("RTL Incomplete tags must share one right x ($incompleteRights)", incompleteRights[0], it, 0.5f)
        }
        assertTrue(
            "RTL slot order must be mirrored: risk > Incomplete > Delivered " +
                "(${riskRights[0]}, ${incompleteRights[0]}, $deliveredRight)",
            riskRights[0] > incompleteRights[0] && incompleteRights[0] > deliveredRight,
        )
        listOf(high, modInc, delHighInc, incOnly).forEach { p ->
            assertEquals("RTL slot 1 must end at the name's right edge", nameBox(p).r, riskRights[0], 0.5f)
            val slots = expectedSlots(p, rtl = true)
            assertEquals("RTL slot 1 right", slots[0].r, riskRights[0], 1f)
            assertEquals("RTL slot 2 right", slots[1].r, incompleteRights[0], 1f)
            assertEquals("RTL slot 3 right", slots[2].r, deliveredRight, 1f)
            assertTagsFitTheirSlots(p, rtl = true)
            assertTagsBelowNameAndAboveFirstMetric(
                p,
                *listOf("High risk", "Moderate risk", "Incomplete", "Delivered").filter { hasTagIn(p, it) }.toTypedArray(),
            )
        }
        listOf(high, modInc, incOnly).forEach { p ->
            assertEquals("RTL slot 1 over GA", metricLabel(p, "GA").r, riskRights[0], 1f)
            assertEquals("RTL slot 2 over Parity", metricLabel(p, "Parity").r, incompleteRights[0], 1f)
            assertEquals("RTL slot 3 over BP", metricLabel(p, "BP").r, deliveredRight, 1f)
        }
    }

    @Test
    fun incompleteTagIsA48dpTargetThatContinuesRegistration() {
        val p = incompletePatient()
        val events = mutableListOf<AncEvent>()
        showListCard(p, widthDp = 360, onEvent = { events += it })

        val action = compose.onNodeWithTag("incompleteChipAction", useUnmergedTree = true)
        val target = action.fetchSemanticsNode().box()
        assertTrue("Incomplete target must be at least 48dp wide (${target.w})", target.w >= 48f - 0.5f)
        assertTrue("Incomplete target must be at least 48dp tall (${target.h})", target.h >= 48f - 0.5f)
        assertTrue("Incomplete target must contain the tag", chip(p, "Incomplete").inside(target))
        val label = action.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.label
        assertEquals("Continue registration", label)

        action.performClick()
        compose.waitForIdle()
        assertEquals("One tap emits exactly one event, not the card's open-workspace event", 1, events.size)
        val ev = events.single()
        assertTrue("Incomplete tap must start the registration flow, got $ev", ev is AncEvent.Flow)
        ev as AncEvent.Flow
        assertEquals(editPatientEvent(p, ev.editorSessionId, ev.newPregnancyId), ev)
        assertEquals(FlowEvent.Begin(p.id, firstNotDoneStep(p) ?: RegistrationStep.IDENTITY), ev.event)
        assertTrue("Session ids must be fresh", ev.editorSessionId.isNotBlank() && ev.newPregnancyId.isNotBlank())
    }
}
