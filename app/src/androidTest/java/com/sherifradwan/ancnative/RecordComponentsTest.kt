package com.sherifradwan.ancnative

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LabeledValue
import com.sherifradwan.ancnative.ui.components.LabeledValueRow
import com.sherifradwan.ancnative.ui.components.RecordRow
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun density360FontScale13(): Density = Density(density = 1f, fontScale = 1.3f)

    // Checklist 5 & 7, Section 8: render at 360 dp and font scale 1.3 in light/dark,
    // assert sizes, missing-value text, 60-char value wraps and is not ellipsised,
    // merged semantics on LabeledValue, StatusTag words.
    @Test
    fun labeledValue_lightDark_missingMergedSemantics_sizesAndWrapping_light() {
        val longValue = "A".repeat(60)
        listOf(false).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            LabeledValue(
                                label = "Patient name",
                                value = longValue,
                                boxTag = "labeledValueBox"
                            )
                        }
                    }
                }
            }

            // Checklist 5: LabeledValue box is at least 44 dp tall.
            composeTestRule.onNodeWithTag("labeledValueBox", useUnmergedTree = true).assertHeightIsAtLeast(44.dp)

            // 60-character value wraps and is not ellipsised.
            val valueNode = composeTestRule.onNodeWithText(longValue, substring = false)
            valueNode.assertIsDisplayed()
            val overflow = valueNode.fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.Text)
                ?.any { annotated -> annotated.text == longValue }
            assertTrue("Expected the full 60-character value to be present", overflow == true)
            val bounds = valueNode.getBoundsInRoot()
            assertTrue(
                "Expected 60-character value to wrap (height > 22dp)",
                bounds.height > 22.dp
            )

            // Section 8: merged semantics on LabeledValue (one node holds both texts).
            composeTestRule.onNode(
                hasText("Patient name", substring = true) and hasText(longValue, substring = true),
                useUnmergedTree = false
            ).assertExists()

        }
    }

    @Test
    fun labeledValue_lightDark_missingMergedSemantics_sizesAndWrapping_dark() {
        val longValue = "A".repeat(60)
        listOf(true).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            LabeledValue(
                                label = "Patient name",
                                value = longValue,
                                boxTag = "labeledValueBox"
                            )
                        }
                    }
                }
            }

            // Checklist 5: LabeledValue box is at least 44 dp tall.
            composeTestRule.onNodeWithTag("labeledValueBox", useUnmergedTree = true).assertHeightIsAtLeast(44.dp)

            // 60-character value wraps and is not ellipsised.
            val valueNode = composeTestRule.onNodeWithText(longValue, substring = false)
            valueNode.assertIsDisplayed()
            val overflow = valueNode.fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.Text)
                ?.any { annotated -> annotated.text == longValue }
            assertTrue("Expected the full 60-character value to be present", overflow == true)
            val bounds = valueNode.getBoundsInRoot()
            assertTrue(
                "Expected 60-character value to wrap (height > 22dp)",
                bounds.height > 22.dp
            )

            // Section 8: merged semantics on LabeledValue (one node holds both texts).
            composeTestRule.onNode(
                hasText("Patient name", substring = true) and hasText(longValue, substring = true),
                useUnmergedTree = false
            ).assertExists()

        }
    }

    @Test
    fun labeledValueRow_equalHeightChildren_at360dpFontScale13_light() {
        listOf(false).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            LabeledValueRow {
                                LabeledValue(
                                    label = "G",
                                    value = "4",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxG",
                                    stretch = true
                                )
                                LabeledValue(
                                    label = "P",
                                    value = "1",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxP",
                                    stretch = true
                                )
                                LabeledValue(
                                    label = "A",
                                    value = "2 with long text to wrap",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxA",
                                    stretch = true
                                )
                            }
                        }
                    }
                }
            }
            val hG = composeTestRule.onNodeWithTag("boxG", useUnmergedTree = true).getBoundsInRoot().height
            val hP = composeTestRule.onNodeWithTag("boxP", useUnmergedTree = true).getBoundsInRoot().height
            val hA = composeTestRule.onNodeWithTag("boxA", useUnmergedTree = true).getBoundsInRoot().height
            assertEquals(hG, hP)
            assertEquals(hG, hA)
            composeTestRule.onNodeWithTag("boxG", useUnmergedTree = true).assertHeightIsAtLeast(44.dp)
        }
    }

    @Test
    fun labeledValueRow_equalHeightChildren_at360dpFontScale13_dark() {
        listOf(true).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            LabeledValueRow {
                                LabeledValue(
                                    label = "G",
                                    value = "4",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxG",
                                    stretch = true
                                )
                                LabeledValue(
                                    label = "P",
                                    value = "1",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxP",
                                    stretch = true
                                )
                                LabeledValue(
                                    label = "A",
                                    value = "2 with long text to wrap",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    boxTag = "boxA",
                                    stretch = true
                                )
                            }
                        }
                    }
                }
            }
            val hG = composeTestRule.onNodeWithTag("boxG", useUnmergedTree = true).getBoundsInRoot().height
            val hP = composeTestRule.onNodeWithTag("boxP", useUnmergedTree = true).getBoundsInRoot().height
            val hA = composeTestRule.onNodeWithTag("boxA", useUnmergedTree = true).getBoundsInRoot().height
            assertEquals(hG, hP)
            assertEquals(hG, hA)
            composeTestRule.onNodeWithTag("boxG", useUnmergedTree = true).assertHeightIsAtLeast(44.dp)
        }
    }

    @Test
    fun recordRow_and_sectionTextAction_minimumSizes_lightAndDark_light() {
        listOf(false).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            androidx.compose.foundation.layout.Column {
                                RecordRow(
                                    onClick = {},
                                    onClickLabel = "Open record",
                                    modifier = Modifier,
                                    clickTag = "recordRowClick"
                                ) {
                                    Text("Row content")
                                }
                                SectionTextAction(
                                    text = "Edit",
                                    contentDescription = "Edit section",
                                    onClick = {},
                                    modifier = Modifier.testTag("sectionAction")
                                )
                            }
                        }
                    }
                }
            }

            // Checklist 5: RecordRow is at least 56 dp tall.
            composeTestRule.onNodeWithTag("recordRowClick")
                .assertHeightIsAtLeast(56.dp)

            // Checklist 5: SectionTextAction is at least 48 by 48 dp.
            composeTestRule.onNodeWithTag("sectionAction")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    @Test
    fun recordRow_and_sectionTextAction_minimumSizes_lightAndDark_dark() {
        listOf(true).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            androidx.compose.foundation.layout.Column {
                                RecordRow(
                                    onClick = {},
                                    onClickLabel = "Open record",
                                    modifier = Modifier,
                                    clickTag = "recordRowClick"
                                ) {
                                    Text("Row content")
                                }
                                SectionTextAction(
                                    text = "Edit",
                                    contentDescription = "Edit section",
                                    onClick = {},
                                    modifier = Modifier.testTag("sectionAction")
                                )
                            }
                        }
                    }
                }
            }

            // Checklist 5: RecordRow is at least 56 dp tall.
            composeTestRule.onNodeWithTag("recordRowClick")
                .assertHeightIsAtLeast(56.dp)

            // Checklist 5: SectionTextAction is at least 48 by 48 dp.
            composeTestRule.onNodeWithTag("sectionAction")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    // Checklist 6 & 7, Section 8: RTL placement by bounds, heading() on section title.
    @Test
    fun recordSectionCard_rtlPlacement_heading_lightAndDark_light() {
        listOf(false).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            RecordSectionCard(
                                title = "Patient details",
                                family = FieldFamilies.patient(),
                                action = {
                                    SectionTextAction(
                                        text = "Edit",
                                        contentDescription = "Edit dating",
                                        onClick = {},
                                        modifier = Modifier.testTag("editDating")
                                    )
                                }
                            ) {
                                LabeledValue(
                                    label = "Name",
                                    value = "Amira",
                                    boxTag = "nameBox"
                                )
                            }
                        }
                    }
                }
            }

            // Section 8: heading() on the section title.
            composeTestRule.onNode(
                hasText("Patient details") and SemanticsMatcher.expectValue(
                    SemanticsProperties.Heading,
                    Unit
                ),
                useUnmergedTree = true
            ).assertExists()

            val titleBounds = composeTestRule.onNodeWithText("Patient details").getBoundsInRoot()
            val actionBounds = composeTestRule.onNodeWithTag("editDating").getBoundsInRoot()
            val labelBounds = composeTestRule.onNodeWithText("Name").getBoundsInRoot()
            val valueBounds = composeTestRule.onNodeWithText("Amira").getBoundsInRoot()

            // Checklist 6: in RTL the section title sits at the right and the action at the left.
            assertTrue(
                "Expected title to be to the right of action in RTL",
                titleBounds.left > actionBounds.left
            )

            // Checklist 6: label and value start at the right.
            // In a 360dp-wide container, "start" in RTL is the right edge.
            assertTrue(
                "Expected label to start near the right in RTL",
                labelBounds.right > 180.dp
            )
            assertTrue(
                "Expected value to start near the right in RTL",
                valueBounds.right > 180.dp
            )
        }
    }

    @Test
    fun recordSectionCard_rtlPlacement_heading_lightAndDark_dark() {
        listOf(true).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            RecordSectionCard(
                                title = "Patient details",
                                family = FieldFamilies.patient(),
                                action = {
                                    SectionTextAction(
                                        text = "Edit",
                                        contentDescription = "Edit dating",
                                        onClick = {},
                                        modifier = Modifier.testTag("editDating")
                                    )
                                }
                            ) {
                                LabeledValue(
                                    label = "Name",
                                    value = "Amira",
                                    boxTag = "nameBox"
                                )
                            }
                        }
                    }
                }
            }

            // Section 8: heading() on the section title.
            composeTestRule.onNode(
                hasText("Patient details") and SemanticsMatcher.expectValue(
                    SemanticsProperties.Heading,
                    Unit
                ),
                useUnmergedTree = true
            ).assertExists()

            val titleBounds = composeTestRule.onNodeWithText("Patient details").getBoundsInRoot()
            val actionBounds = composeTestRule.onNodeWithTag("editDating").getBoundsInRoot()
            val labelBounds = composeTestRule.onNodeWithText("Name").getBoundsInRoot()
            val valueBounds = composeTestRule.onNodeWithText("Amira").getBoundsInRoot()

            // Checklist 6: in RTL the section title sits at the right and the action at the left.
            assertTrue(
                "Expected title to be to the right of action in RTL",
                titleBounds.left > actionBounds.left
            )

            // Checklist 6: label and value start at the right.
            // In a 360dp-wide container, "start" in RTL is the right edge.
            assertTrue(
                "Expected label to start near the right in RTL",
                labelBounds.right > 180.dp
            )
            assertTrue(
                "Expected value to start near the right in RTL",
                valueBounds.right > 180.dp
            )
        }
    }

    // Section 8: StatusTag words for each kind; Checklist 7: light and dark render.
    @Test
    fun statusTag_wordsForEachKind_lightAndDark_light() {
        listOf(false).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            androidx.compose.foundation.layout.Column {
                                StatusTag(kind = StatusKind.HighRisk)
                                StatusTag(kind = StatusKind.ModerateRisk)
                                StatusTag(kind = StatusKind.Incomplete)
                                StatusTag(kind = StatusKind.Delivered)
                            }
                        }
                    }
                }
            }

            composeTestRule.onNodeWithText("High risk", ignoreCase = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("Moderate risk").assertIsDisplayed()
            composeTestRule.onNodeWithText("Incomplete").assertIsDisplayed()
            composeTestRule.onNodeWithText("Delivered").assertIsDisplayed()
        }
    }

    @Test
    fun statusTag_wordsForEachKind_lightAndDark_dark() {
        listOf(true).forEach { dark ->
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides density360FontScale13(),
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    AncTheme(darkTheme = dark) {
                        Box(modifier = Modifier.width(360.dp)) {
                            androidx.compose.foundation.layout.Column {
                                StatusTag(kind = StatusKind.HighRisk)
                                StatusTag(kind = StatusKind.ModerateRisk)
                                StatusTag(kind = StatusKind.Incomplete)
                                StatusTag(kind = StatusKind.Delivered)
                            }
                        }
                    }
                }
            }

            composeTestRule.onNodeWithText("High risk", ignoreCase = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("Moderate risk").assertIsDisplayed()
            composeTestRule.onNodeWithText("Incomplete").assertIsDisplayed()
            composeTestRule.onNodeWithText("Delivered").assertIsDisplayed()
        }
    }

    // Section 2: ltrIsolate keeps codes/numbers in left-to-right order inside RTL text.
    @Test
    fun ltrIsolate_wrapsWithDirectionalIsolates() {
        val isolated = ltrIsolate("A+")
        assertEquals("\u2066A+\u2069", isolated)
        val isolatedBp = ltrIsolate("110/70")
        assertEquals("\u2066110/70\u2069", isolatedBp)
    }

    @Test
    fun labeledValue_missingValueShowsNotRecorded() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides density360FontScale13(),
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                AncTheme(darkTheme = false) {
                    Box(modifier = Modifier.width(360.dp)) {
                        androidx.compose.foundation.layout.Column {
                            LabeledValue(label = "Null field", value = null, boxTag = "nullBox")
                            LabeledValue(label = "Blank field", value = "   ", boxTag = "blankBox")
                        }
                    }
                }
            }
        }
        composeTestRule.onNode(
            hasText("Null field", substring = true) and hasText("Not recorded", substring = true),
            useUnmergedTree = false
        ).assertExists()
        composeTestRule.onNode(
            hasText("Blank field", substring = true) and hasText("Not recorded", substring = true),
            useUnmergedTree = false
        ).assertExists()
    }

    @Test
    fun standaloneLabeledValueKeepsItsBoxInsideAScrollingColumn() {
        composeTestRule.setContent {
            AncTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.width(360.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                ) {
                    LabeledValue(label = "First", value = "One", boxTag = "scrollBox1")
                    LabeledValue(label = "Second", value = "Two", boxTag = "scrollBox2")
                }
            }
        }
        composeTestRule.onNodeWithTag("scrollBox1", useUnmergedTree = true).assertIsDisplayed().assertHeightIsAtLeast(44.dp)
        composeTestRule.onNodeWithTag("scrollBox2", useUnmergedTree = true).assertIsDisplayed().assertHeightIsAtLeast(44.dp)
    }
}
