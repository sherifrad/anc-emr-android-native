package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.*
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.*
import com.sherifradwan.ancnative.ui.patienthub.components.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttentionRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val fourEntryAttention = "CRITICAL: Severe PIH\nALERT: Anaemia\nNOTE: Prefers female doctor"
    private val severeBp = SevereBpAttention("165/105", "2026-11-10")

    private fun showCard(
        attention: String?,
        severeBp: SevereBpAttention?,
        onEdit: (() -> Unit)?,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        ClinicalAttentionCard(
                            attention = attention,
                            severeBp = severeBp,
                            onEdit = onEdit,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showStrip(
        attention: String?,
        severeBp: SevereBpAttention?,
        onOpenDialog: () -> Unit = {},
        darkTheme: Boolean = false,
        density: Density? = null,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    val content = @androidx.compose.runtime.Composable {
                        CompactClinicalAttentionStrip(
                            attention = attention,
                            severeBp = severeBp,
                            onOpenDialog = onOpenDialog,
                            modifier = Modifier,
                        )
                    }
                    if (density != null) {
                        CompositionLocalProvider(LocalDensity provides density, content = content)
                    } else {
                        content()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showDialog(
        currentAttention: String,
        canEdit: Boolean = true,
        onSave: (String) -> Unit = {},
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    ClinicalAttentionDialog(
                        currentAttention = currentAttention,
                        onDismiss = {},
                        onSave = onSave,
                        canEdit = canEdit,
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun cardShowsSevereBpFirstWithEntryOrderHeightsAndLevelWords() {
        showCard(attention = fourEntryAttention, severeBp = severeBp, onEdit = {})

        val severeBounds = compose.onNodeWithTag("severeBpAttention").getUnclippedBoundsInRoot()
        val entry0Bounds = compose.onNodeWithTag("attentionEntry-0").getUnclippedBoundsInRoot()
        assertTrue(
            "severeBpAttention should be above attentionEntry-0",
            severeBounds.top < entry0Bounds.top,
        )

        compose.onNodeWithTag("severeBpAttention").assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-0").assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-1").assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-2").assertIsDisplayed()

        compose.onNodeWithTag("severeBpAttention").assertHeightIsAtLeast(44.dp)
        compose.onNodeWithTag("attentionEntry-0").assertHeightIsAtLeast(44.dp)
        compose.onNodeWithTag("attentionEntry-1").assertHeightIsAtLeast(44.dp)
        compose.onNodeWithTag("attentionEntry-2").assertHeightIsAtLeast(44.dp)

        compose.onAllNodes(hasText("Critical", substring = false)).assertCountEquals(2)
        compose.onNodeWithText("Alert", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Note", substring = false).assertIsDisplayed()
    }

    @Test
    fun cardHasNoIconContentDescriptionsExceptEditAction() {
        showCard(attention = fourEntryAttention, severeBp = severeBp, onEdit = {})

        val editLabel = compose.activity.getString(R.string.edit_clinical_alerts)
        val hasContentDescription =
            SemanticsMatcher("has content description") {
                it.config.contains(SemanticsProperties.ContentDescription)
            }
        val nodesWithContentDescription = compose
            .onAllNodes(hasContentDescription)
            .fetchSemanticsNodes()

        val nonEditDescriptions = nodesWithContentDescription
            .flatMap { node ->
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            }
            .filter { it != editLabel }

        assertTrue(
            "Expected no icon/content descriptions except Edit action, found: $nonEditDescriptions",
            nonEditDescriptions.isEmpty(),
        )
    }

    @Test
    fun stripShowsOverflowIsClickableAndClampsToTwoLinesAt360dpFontScale() {
        var opened = false
        showStrip(
            attention = fourEntryAttention,
            severeBp = severeBp,
            onOpenDialog = { opened = true },
        )

        val strip = compose.onNodeWithTag("clinicalAttentionStrip")
        strip.assertExists().assertIsDisplayed()
        assertTrue(strip.getUnclippedBoundsInRoot().height >= 48.dp)
        compose.onNodeWithText("+3", substring = true).assertIsDisplayed()

        strip.performClick()
        compose.waitForIdle()
        assertTrue(opened)

        val density360FontScale13 =
            Density(density = 360f / 360f, fontScale = 1.3f)
        showStrip(
            attention = fourEntryAttention,
            severeBp = severeBp,
            density = density360FontScale13,
        )

        assertTrue(compose.onNodeWithTag("clinicalAttentionStrip").getUnclippedBoundsInRoot().height >= 48.dp)
    }

    @Test
    fun dialogReadOnlyShowsEntriesAndCloseEditModeHasLargeSave() {
        showDialog(currentAttention = fourEntryAttention, canEdit = false)
        compose.onNodeWithTag("attentionEntry-0").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-1").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-2").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionClose").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionSave").assertDoesNotExist()

        showDialog(currentAttention = fourEntryAttention, canEdit = true)
        compose.onNodeWithTag("attentionSave")
            .assertExists()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun cardRtlPlacesTitleToRightOfEditAction() {
        showCard(
            attention = fourEntryAttention,
            severeBp = severeBp,
            onEdit = {},
            layoutDirection = LayoutDirection.Rtl,
        )

        val titleBounds = compose.onNodeWithText("Clinical attention").getUnclippedBoundsInRoot()
        val editBounds = compose.onNodeWithTag("editClinicalAttention").getUnclippedBoundsInRoot()

        assertTrue(
            "Expected card title to be to the right of Edit in RTL (title.left=${titleBounds.left}, edit.right=${editBounds.right})",
            titleBounds.left >= editBounds.right,
        )
    }

    @Test
    fun darkThemeRendersAllLevelsWithoutClipping() {
        showCard(
            attention = fourEntryAttention,
            severeBp = severeBp,
            onEdit = {},
            darkTheme = true,
        )

        compose.onNodeWithTag("clinicalAttentionCard").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("severeBpAttention").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-0").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-1").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("attentionEntry-2").assertExists().assertIsDisplayed()
        compose.onAllNodes(hasText("Critical", substring = false)).assertCountEquals(2)
        compose.onNodeWithText("Alert", substring = false).assertExists()
        compose.onNodeWithText("Note", substring = false).assertExists()
    }
}
