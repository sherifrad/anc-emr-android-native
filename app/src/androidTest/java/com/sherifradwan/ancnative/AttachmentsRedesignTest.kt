package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.ui.AddAttachmentDialog
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ClinicalAttachmentViewerDialog
import com.sherifradwan.ancnative.ui.StatusModerateDarkText
import com.sherifradwan.ancnative.ui.StatusModerateText
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.patienthub.components.AttachmentsSectionCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plan tab attachments (redesign slice 10): the section card (title, summary, filter chips, rows, empty states, Add),
 * the add dialog (type and photo size chips, source buttons, Save gate, Cancel) and the viewer (integrity word proven
 * against a real file and its SHA-256, hash line, buttons, delete gate), at 360dp with font scale 1.3 and 2.0 and RTL.
 *
 * The dialogs live in their own window, which re-reads density from the activity resources, so their narrow and
 * large-text runs hold the activity resources at 360dp wide and the given font scale for that test (restored after).
 */
@RunWith(AndroidJUnit4::class)
class AttachmentsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    // A patient id of its own, so the viewer files written here never sit in a fixture patient's folder.
    private val base: DemoPatient = DemoFixtures.patients.first().copy(id = "slice10-attachments-test")
    private val capturedAt = "2026-11-10T09:30:00Z"
    private val capturedDay = "2026-11-10"

    private fun attachment(
        id: String,
        category: String,
        title: String?,
        fileName: String = "att_$id.webp",
        fileSizeBytes: Long = 1024,
        sha256Hash: String = "00",
    ) = DemoAttachment(
        id = id,
        patientId = base.id,
        fileName = fileName,
        filePath = "patient_attachments/${base.id}/$fileName",
        category = category,
        fileSizeBytes = fileSizeBytes,
        sha256Hash = sha256Hash,
        title = title,
        capturedAt = capturedAt,
        revision = 1,
    )

    private val ultrasound = attachment("us-1", AttachmentCategories.ULTRASOUND, "Scan 1")
    private val lab = attachment("lab-1", AttachmentCategories.LAB, "CBC report")
    private val untitledUs = attachment("us-2", AttachmentCategories.ULTRASOUND, title = null)
    private val other = attachment("other-1", AttachmentCategories.OTHER, "Referral letter")

    // Fits two lines at 360dp with font scale 2.0, so "nothing clipped" is a real check and not an ellipsis by design.
    private val growthScan = attachment("us-g", AttachmentCategories.ULTRASOUND, "Growth scan at 28 weeks")

    private fun patientWith(vararg attachments: DemoAttachment, status: String? = "active") =
        base.copy(status = status, attachments = attachments.toList())

    /** Theme values read inside the composition, so colour checks compare with the real scheme. */
    private var onSurfaceVariantColor: Color = Color.Unspecified
    private var moderateColor: Color = Color.Unspecified

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null
    private val writtenFiles = mutableListOf<File>()

    @After
    fun tearDown() {
        restoreResources()
        writtenFiles.forEach { it.delete() }
        if (writtenFiles.isNotEmpty()) {
            PatientAttachmentVault.getPatientDirectory(context(), base.id).delete()
        }
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun host(
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    moderateColor = if (isDarkTheme()) StatusModerateDarkText else StatusModerateText
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    CompositionLocalProvider(
                        LocalDensity provides scaled,
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        // The card sits in the scrolling Plan tab in the app; the host scrolls the same way, so a tall
                        // card at large text is measured whole instead of being cut by the window.
                        Box(sized.verticalScroll(rememberScrollState())) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * Holds the activity resources at a 360dp-wide window and [fontScale] for this test. A Dialog window builds its own
     * density from these resources, so a LocalDensity override in the host never reaches dialog content.
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

    private fun showCard(
        patient: DemoPatient,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onAdd: () -> Unit = {},
        onView: (DemoAttachment) -> Unit = {},
    ) = host(widthDp, fontScale, layoutDirection) {
        AttachmentsSectionCard(patient = patient, onAddAttachment = onAdd, onViewAttachment = onView)
    }

    private fun showAddDialog(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        initialCategory: String = AttachmentCategories.ULTRASOUND,
        onDismiss: () -> Unit = {},
        onSave: () -> Unit = {},
    ) = host(layoutDirection = layoutDirection) {
        AddAttachmentDialog(
            patient = patientWith(),
            initialCategory = initialCategory,
            onDismiss = onDismiss,
            onSave = { _, _, _, _ -> onSave() },
        )
    }

    private fun showViewer(
        attachment: DemoAttachment = ultrasound,
        canDelete: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) = host(layoutDirection = layoutDirection) {
        ClinicalAttachmentViewerDialog(
            patient = patientWith(attachment),
            attachment = attachment,
            canDelete = canDelete,
            onDismiss = onDismiss,
            onDelete = onDelete,
        )
    }

    // ---- shared measuring helpers ----

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    private fun boundsOf(matcher: SemanticsMatcher): Rect =
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun dpBounds(matcher: SemanticsMatcher) = compose.onNode(matcher, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun layoutOf(matcher: SemanticsMatcher, label: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun assertMinTouchTarget(tag: String) = assertMinTouchTarget(tag, hasTestTag(tag))

    private fun assertMinTouchTarget(label: String, matcher: SemanticsMatcher, merged: Boolean = false) {
        val bounds = if (merged) compose.onNode(matcher).getUnclippedBoundsInRoot() else dpBounds(matcher)
        // 0.01dp absorbs px-to-dp float rounding (a 48dp control can read back as 47.999985dp); a real shortfall is >= 1px.
        assertTrue("$label width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$label height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    private fun assertMinHeight(tag: String, min: Dp) {
        val height = node(tag).getUnclippedBoundsInRoot().height
        assertTrue("$tag height < $min ($height)", height >= min - 0.01.dp)
    }

    private fun textNodes(tag: String): List<SemanticsNode> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and inTagged(tag), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun nodeText(n: SemanticsNode) = n.config[SemanticsProperties.Text].joinToString(" ")

    private fun layoutsOf(n: SemanticsNode): List<TextLayoutResult> {
        val results = mutableListOf<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results
    }

    /** No text in [tag] is drawn below 12sp (the slice floor). */
    private fun assertNoTextBelow12sp(tag: String) {
        val nodes = textNodes(tag)
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.forEach { n ->
            layoutsOf(n).forEach { layout ->
                val size = layout.layoutInput.style.fontSize
                assertTrue("'${nodeText(n)}' is ${size} (< 12sp)", size.value >= 11.99f)
            }
        }
    }

    /**
     * Every text in [tag] is whole (shared assertTextNotClipped: no vertical cut, no line past its node, no ellipsis)
     * and sits inside the [tag] container horizontally.
     */
    private fun assertAllTextWhole(tag: String) {
        val container = node(tag).getUnclippedBoundsInRoot()
        val nodes = textNodes(tag)
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.filter { layoutsOf(it).isNotEmpty() }.forEach { n ->
            val text = nodeText(n)
            val layout = layoutsOf(n).first()
            if (layout.layoutInput.style.textAlign == TextAlign.Center || layout.layoutInput.style.textAlign == TextAlign.End) {
                assertAlignedTextNotClipped(text, layout)
            } else {
                compose.assertTextNotClipped(text, SemanticsMatcher("node ${n.id}") { it.id == n.id })
            }
            val b = compose.onNode(SemanticsMatcher("node ${n.id}") { it.id == n.id }, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue("'$text' starts outside $tag (${b.left} < ${container.left})", b.left >= container.left - 1.dp)
            assertTrue("'$text' runs past $tag (${b.right} > ${container.right})", b.right <= container.right + 1.dp)
        }
    }

    /**
     * Clip check for centred or end-aligned text (LabsTabRedesignTest precedent): a shrink-wrapped aligned line is laid
     * out against the full paragraph width, so getLineRight can pass size.width while the line is drawn whole. Compare
     * each line's own width with the node width instead.
     */
    private fun assertAlignedTextNotClipped(label: String, layout: TextLayoutResult) {
        assertTrue("'$label' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
        for (line in 0 until layout.lineCount) {
            val width = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("'$label' line $line wider than its node ($width > ${layout.size.width})", width <= layout.size.width + 1f)
            assertTrue("'$label' ellipsized on line $line", !layout.isLineEllipsized(line))
        }
    }

    /** Proves a dialog really rendered with [fontScale] and within 360dp: the text density and the card width. */
    private fun assertDialogNarrowAndScaled(dialogTag: String, textMatcher: SemanticsMatcher, fontScale: Float) {
        val layout = layoutOf(textMatcher, "dialog title")
        assertEquals("dialog text font scale", fontScale, layout.layoutInput.density.fontScale, 0.01f)
        val width = node(dialogTag).getUnclippedBoundsInRoot().width
        assertTrue("$dialogTag must fit 360dp (width=$width)", width <= 360.dp)
    }

    // ---- section card ----

    @Test
    fun card_titleSummaryAndChipsWithCounts_noChipForOther() {
        showCard(patientWith(ultrasound, lab, untitledUs, other))

        compose.onNode(hasText("Documents and attachments") and inTagged("attachmentsSectionCard"), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(hasText("4 files, 4.0 KB") and inTagged("attachmentsSectionCard"), useUnmergedTree = true)
            .assertIsDisplayed()

        mapOf(
            "attachmentFilter-ALL" to "All (4)",
            "attachmentFilter-${AttachmentCategories.ULTRASOUND}" to "Ultrasound (2)",
            "attachmentFilter-${AttachmentCategories.LAB}" to "Labs (1)",
            "attachmentFilter-${AttachmentCategories.PRESCRIPTION}" to "Prescriptions (0)",
            "attachmentFilter-${AttachmentCategories.CLINICAL_PHOTO}" to "Clinical photos (0)",
        ).forEach { (tag, label) ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assert(hasText(label))
        }
        compose.onNodeWithTag("attachmentFilter-ALL").assertIsSelected()
        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.ULTRASOUND}").assertIsNotSelected()
        node("attachmentFilter-${AttachmentCategories.OTHER}").assertDoesNotExist()
        // The OTHER file is still listed under All.
        node("attachmentCard-${other.id}").assertIsDisplayed()
    }

    @Test
    fun card_oneFileSummaryIsSingular() {
        showCard(patientWith(ultrasound))
        compose.onNode(hasText("1 file, 1.0 KB") and inTagged("attachmentsSectionCard"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun card_chipSelectionFiltersRows_andEmptyCategoryShowsFilteredEmptyState() {
        showCard(patientWith(ultrasound, lab, untitledUs))
        val emptyText = "No attachments in this selected category"

        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.ULTRASOUND}").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.ULTRASOUND}").assertIsSelected()
        compose.onNodeWithTag("attachmentFilter-ALL").assertIsNotSelected()
        node("attachmentCard-${ultrasound.id}").assertIsDisplayed()
        node("attachmentCard-${untitledUs.id}").assertIsDisplayed()
        node("attachmentCard-${lab.id}").assertDoesNotExist()
        compose.onAllNodes(hasText(emptyText), useUnmergedTree = true).assertCountEquals(0)

        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.LAB}").performClick()
        compose.waitForIdle()
        node("attachmentCard-${lab.id}").assertIsDisplayed()
        node("attachmentCard-${ultrasound.id}").assertDoesNotExist()

        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.PRESCRIPTION}").performClick()
        compose.waitForIdle()
        compose.onNode(hasText(emptyText) and inTagged("attachmentsSectionCard"), useUnmergedTree = true).assertIsDisplayed()
        listOf(ultrasound, lab, untitledUs).forEach { node("attachmentCard-${it.id}").assertDoesNotExist() }
        // The summary and the chips stay: the clinician can always get back to All.
        compose.onNode(hasText("3 files, 3.0 KB"), useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithTag("attachmentFilter-ALL").performClick()
        compose.waitForIdle()
        listOf(ultrasound, lab, untitledUs).forEach { node("attachmentCard-${it.id}").assertIsDisplayed() }
        compose.onAllNodes(hasText(emptyText), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun card_noAttachments_showsEmptyState_noSummaryNoChips_addStillOffered() {
        var adds = 0
        showCard(patientWith(), onAdd = { adds++ })

        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("No saved attachments or clinical images") and inTagged("attachmentsSectionCard"), useUnmergedTree = true)
            .assertIsDisplayed()
        node("attachmentFilter-ALL").assertDoesNotExist()
        compose.onAllNodes(hasText("file", substring = true) and inTagged("attachmentsSectionCard"), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onNodeWithTag("addAttachmentBtn").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertEquals(1, adds)
    }

    @Test
    fun card_archivedPatient_hasNoAdd_butKeepsRows() {
        showCard(patientWith(ultrasound, status = "archived"))
        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        node("addAttachmentBtn").assertDoesNotExist()
        node("attachmentCard-${ultrasound.id}").assertIsDisplayed()
    }

    @Test
    fun card_archivedPatientWithoutFiles_hasNoAdd() {
        showCard(patientWith(status = "archived"))
        compose.onNode(hasText("No saved attachments or clinical images"), useUnmergedTree = true).assertIsDisplayed()
        node("addAttachmentBtn").assertDoesNotExist()
    }

    @Test
    fun card_addAction_isAddWithDescription_48dp_andCallsBack() {
        var adds = 0
        showCard(patientWith(ultrasound), onAdd = { adds++ })
        compose.onNodeWithTag("addAttachmentBtn")
            .assertIsDisplayed()
            .assert(hasText("Add"))
            .assert(hasContentDescription("Add attachment"))
            .assert(hasClickAction())
        assertMinTouchTarget("addAttachmentBtn")
        compose.onNodeWithTag("addAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, adds)
    }

    @Test
    fun card_rowGeometry_72dpRow_56dpThumbnail_titleWrapsToTwoLines_detailLines_andOpensViewer() {
        val longTitle = attachment(
            "us-long",
            AttachmentCategories.ULTRASOUND,
            "Detailed anomaly scan with cervical length and placental position review for this pregnancy",
        )
        var viewed: DemoAttachment? = null
        showCard(patientWith(ultrasound, longTitle), onView = { viewed = it })

        val rowTag = "attachmentCard-${ultrasound.id}"
        assertMinHeight(rowTag, 72.dp)
        compose.onNodeWithTag(rowTag).assert(hasClickAction())

        // Thumbnail: the text column starts after the 56dp thumbnail and the 12dp gap, so 68dp from the row edge.
        val row = node(rowTag).getUnclippedBoundsInRoot()
        val title = dpBounds(hasText("Scan 1") and inTagged(rowTag))
        val start = title.left - row.left
        assertTrue("text starts ${start} from the row edge, expected 68dp (56dp thumb + 12dp)", abs(start.value - 68f) <= 0.5f)

        compose.onNode(hasText("Ultrasound scan, 1.0 KB") and inTagged(rowTag), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(
            hasText("Saved on this phone, ⁦$capturedDay⁩") and inTagged(rowTag),
            useUnmergedTree = true,
        ).assertIsDisplayed()

        // A long title wraps to a second line instead of being cut on one.
        val longLayout = layoutOf(hasText(longTitle.title!!) and inTagged("attachmentCard-${longTitle.id}"), "long title")
        assertEquals("long title wraps to 2 lines", 2, longLayout.lineCount)
        assertMinHeight("attachmentCard-${longTitle.id}", 72.dp)

        compose.onNodeWithTag(rowTag).performClick()
        compose.waitForIdle()
        assertEquals(ultrasound.id, viewed?.id)
    }

    @Test
    fun card_untitledRow_usesCategoryCommaDateTitle() {
        showCard(patientWith(untitledUs))
        val rowTag = "attachmentCard-${untitledUs.id}"
        compose.onNode(hasText("Ultrasound scan, $capturedDay") and inTagged(rowTag), useUnmergedTree = true).assertIsDisplayed()
        // The internal file name is never shown.
        compose.onAllNodes(hasText(untitledUs.fileName, substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun card_noVerifiedShieldWord_andNoTextBelow12sp() {
        showCard(patientWith(ultrasound, lab, other))
        compose.onAllNodes(
            hasText("verified", substring = true, ignoreCase = true) and inTagged("attachmentsSectionCard"),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        compose.onAllNodes(hasText("vault", substring = true, ignoreCase = true), useUnmergedTree = true).assertCountEquals(0)
        assertNoTextBelow12sp("attachmentsSectionCard")
    }

    @Test
    fun card_360dpFontScale1_3_nothingClipped() {
        showCard(patientWith(growthScan, lab, untitledUs), widthDp = 360, fontScale = 1.3f)
        assertCardWholeAt360()
    }

    @Test
    fun card_360dpFontScale2_0_nothingClipped() {
        showCard(patientWith(growthScan, lab, untitledUs), widthDp = 360, fontScale = 2.0f)
        assertCardWholeAt360()
    }

    private fun assertCardWholeAt360() {
        val card = node("attachmentsSectionCard").getUnclippedBoundsInRoot()
        assertTrue("card must fit 360dp (right=${card.right})", card.right <= 360.dp + 0.5.dp)
        assertAllTextWhole("attachmentsSectionCard")
        listOf(growthScan, lab, untitledUs).forEach { assertMinHeight("attachmentCard-${it.id}", 72.dp) }
        assertMinTouchTarget("addAttachmentBtn")
        val add = node("addAttachmentBtn").getUnclippedBoundsInRoot()
        assertTrue("Add sticks out of the card", add.right <= card.right + 0.5.dp && add.left >= card.left - 0.5.dp)
    }

    @Test
    fun card_rtl360dp_mirrorsRow_keepsDateIsolated_andNothingClipped() {
        showCard(patientWith(ultrasound, lab), widthDp = 360, fontScale = 1.3f, layoutDirection = LayoutDirection.Rtl)
        val rowTag = "attachmentCard-${ultrasound.id}"
        val row = node(rowTag).getUnclippedBoundsInRoot()
        val title = dpBounds(hasText("Scan 1") and inTagged(rowTag))
        val start = row.right - title.right
        assertTrue("RTL: text starts ${start} from the right row edge, expected 68dp", abs(start.value - 68f) <= 0.5f)
        compose.onNode(
            hasText("Saved on this phone, ⁦$capturedDay⁩") and inTagged(rowTag),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        // Chips flow from the right: All sits right of Ultrasound.
        val all = node("attachmentFilter-ALL").getUnclippedBoundsInRoot()
        val us = node("attachmentFilter-${AttachmentCategories.ULTRASOUND}").getUnclippedBoundsInRoot()
        assertTrue("RTL: All chip should be right of Ultrasound", all.left > us.left)
        assertAllTextWhole("attachmentsSectionCard")
    }

    // ---- add dialog ----

    @Test
    fun add_titlePatientLabelsAndChoices() {
        showAddDialog()
        val inDialog = inTagged("addAttachmentDialog")
        compose.onNodeWithTag("addAttachmentDialog").assertIsDisplayed()
        compose.onNode(hasText("Add attachment") and inDialog, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText(base.displayName) and inDialog, useUnmergedTree = true).assertIsDisplayed()

        // Group labels are 12sp FormFieldLabels.
        listOf("Type", "Photo size").forEach { label ->
            val style = layoutOf(hasText(label) and inDialog, label).layoutInput.style
            assertEquals("$label must be 12sp", 12.sp, style.fontSize)
        }
        listOf("Ultrasound", "Labs", "Prescription or report", "Clinical photo", "Other").forEach {
            compose.onNode(hasText(it) and hasClickAction() and inDialog).assertIsDisplayed()
        }
        mapOf(
            "preset_small" to "Small (documents)",
            "preset_standard" to "Standard (reports)",
            "preset_detailed" to "Detailed (scans)",
        ).forEach { (tag, label) -> compose.onNodeWithTag(tag).assertIsDisplayed().assert(hasText(label)) }
        compose.onNode(hasText("Title or note (optional)") and inDialog, useUnmergedTree = true).assertExists()
    }

    @Test
    fun add_typeChipSelection_drivesDefaultPhotoSize_untilChosen() {
        showAddDialog()
        val inDialog = inTagged("addAttachmentDialog")
        fun chip(label: String) = compose.onNode(hasText(label) and hasClickAction() and inDialog)

        chip("Ultrasound").assertIsSelected()
        chip("Labs").assertIsNotSelected()
        // Ultrasound defaults to the detailed size.
        compose.onNodeWithTag("preset_detailed").assertIsSelected()

        chip("Labs").performClick()
        compose.waitForIdle()
        chip("Labs").assertIsSelected()
        chip("Ultrasound").assertIsNotSelected()
        compose.onNodeWithTag("preset_small").assertIsSelected()
        compose.onNodeWithTag("preset_detailed").assertIsNotSelected()

        // An explicit choice sticks when the type changes.
        compose.onNodeWithTag("preset_standard").performClick()
        compose.waitForIdle()
        chip("Ultrasound").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("preset_standard").assertIsSelected()
        compose.onNodeWithTag("preset_detailed").assertIsNotSelected()
    }

    @Test
    fun add_sourceButtons_areAtLeast64dp_withTitleAndHint() {
        showAddDialog()
        mapOf(
            "captureCameraBtn" to ("Take photo" to "Capture a scan or document"),
            "pickGalleryBtn" to ("Choose from gallery" to "An existing file or photo"),
        ).forEach { (tag, texts) ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assert(hasClickAction())
            compose.onNode(hasText(texts.first) and inTagged(tag), useUnmergedTree = true).assertIsDisplayed()
            compose.onNode(hasText(texts.second) and inTagged(tag), useUnmergedTree = true).assertIsDisplayed()
            assertMinHeight(tag, 64.dp)
        }
    }

    @Test
    fun add_saveDisabledWithoutPhoto_cancelAndClose48dp_cancelDismisses() {
        var dismissed = 0
        var saved = 0
        showAddDialog(onDismiss = { dismissed++ }, onSave = { saved++ })

        compose.onNodeWithTag("saveAttachmentConfirmBtn").assert(hasText("Save to record")).assertIsNotEnabled()
        compose.onNodeWithTag("saveAttachmentConfirmBtn").performClick()
        compose.waitForIdle()
        assertEquals("Save must not fire without a photo", 0, saved)
        assertEquals("a disabled Save must not close the dialog", 0, dismissed)

        assertMinTouchTarget("saveAttachmentConfirmBtn")
        assertMinTouchTarget("cancelAttachmentBtn")
        compose.onNodeWithTag("cancelAttachmentBtn").assert(hasText("Cancel")).assertIsEnabled()
        val close = hasContentDescription("Cancel") and hasClickAction() and inTagged("addAttachmentDialog")
        // The icon's description merges into the clickable IconButton, so the close button is found in the merged tree.
        assertMinTouchTarget("close icon", close, merged = true)

        compose.onNodeWithTag("cancelAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertEquals(0, saved)
    }

    @Test
    fun add_closeIconDismisses() {
        var dismissed = 0
        showAddDialog(onDismiss = { dismissed++ })
        compose.onNode(hasContentDescription("Cancel") and hasClickAction() and inTagged("addAttachmentDialog")).performClick()
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test
    fun add_noTextBelow12sp() {
        showAddDialog()
        assertNoTextBelow12sp("addAttachmentDialog")
    }

    @Test
    fun add_360dpFontScale1_3_nothingClipped() {
        narrowWindow(1.3f)
        showAddDialog()
        assertAddDialogWhole(1.3f)
    }

    @Test
    fun add_360dpFontScale2_0_nothingClipped() {
        narrowWindow(2.0f)
        showAddDialog()
        assertAddDialogWhole(2.0f)
    }

    private fun assertAddDialogWhole(fontScale: Float) {
        assertDialogNarrowAndScaled("addAttachmentDialog", hasText("Add attachment") and inTagged("addAttachmentDialog"), fontScale)
        assertAllTextWhole("addAttachmentDialog")
        listOf("captureCameraBtn", "pickGalleryBtn").forEach { assertMinHeight(it, 64.dp) }
        listOf("cancelAttachmentBtn", "saveAttachmentConfirmBtn").forEach { assertMinTouchTarget(it) }
    }

    @Test
    fun add_rtl360dp_mirrorsHeader_andNothingClipped() {
        narrowWindow(1.3f)
        showAddDialog(layoutDirection = LayoutDirection.Rtl)
        val inDialog = inTagged("addAttachmentDialog")
        val title = dpBounds(hasText("Add attachment") and inDialog)
        val close = compose.onNode(hasContentDescription("Cancel") and hasClickAction() and inDialog).getUnclippedBoundsInRoot()
        assertTrue("RTL: close icon must sit left of the title", close.right <= title.left + 0.5.dp)
        // Cancel comes first, so in RTL it is on the right of Save.
        val cancel = node("cancelAttachmentBtn").getUnclippedBoundsInRoot()
        val save = node("saveAttachmentConfirmBtn").getUnclippedBoundsInRoot()
        assertTrue("RTL: Cancel must be right of Save", cancel.left > save.left)
        assertAllTextWhole("addAttachmentDialog")
    }

    // ---- viewer ----

    /** Writes a real PNG for [base] in the vault and returns its bytes' SHA-256 (lower-case hex), computed here. */
    private fun writeVaultImage(fileName: String): String {
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GRAY) }
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        val file = File(PatientAttachmentVault.getPatientDirectory(context(), base.id), fileName)
        file.writeBytes(bytes)
        writtenFiles += file
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun integrityStyle(text: String) =
        layoutOf(hasText(text) and inTagged("clinicalAttachmentViewerDialog"), text).layoutInput.style

    @Test
    fun viewer_missingFile_showsImageUnavailable_andAmberNotVerified_neverVerified() {
        showViewer(ultrasound)
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        compose.onNodeWithTag("image_unavailable").assertIsDisplayed().assert(hasText("Image unavailable"))
        compose.onNode(hasText("Integrity not verified") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        assertEquals("not verified must be amber", moderateColor, integrityStyle("Integrity not verified").color)
        compose.onAllNodes(hasText("Integrity verified", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun viewer_fileWithMatchingHash_showsVerified_andTheImage() {
        val fileName = "att_slice10_ok.png"
        val hash = writeVaultImage(fileName)
        showViewer(ultrasound.copy(fileName = fileName, sha256Hash = hash))
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        compose.onNode(hasText("Integrity verified (SHA-256)") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        assertEquals(onSurfaceVariantColor, integrityStyle("Integrity verified (SHA-256)").color)
        compose.onAllNodes(hasText("Integrity not verified"), useUnmergedTree = true).assertCountEquals(0)
        node("image_unavailable").assertDoesNotExist()
        compose.onNode(hasContentDescription("Scan 1") and inViewer, useUnmergedTree = true).assertExists()
        compose.onNode(hasText("SHA-256: $hash") and inViewer, useUnmergedTree = true).assertExists()
    }

    @Test
    fun viewer_tamperedFile_showsNotVerified_neverVerified() {
        val fileName = "att_slice10_tampered.png"
        val hash = writeVaultImage(fileName)
        // The stored hash belongs to the original bytes; the file on disk changes afterwards.
        writtenFiles.last().appendBytes(byteArrayOf(1, 2, 3))
        showViewer(ultrasound.copy(fileName = fileName, sha256Hash = hash))
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        compose.onNode(hasText("Integrity not verified") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        assertEquals(moderateColor, integrityStyle("Integrity not verified").color)
        compose.onAllNodes(hasText("Integrity verified", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun viewer_headerAndDetails_patientCategoryTitleSizeSavedAt() {
        showViewer(ultrasound)
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        compose.onNode(hasText(base.displayName) and inViewer, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Ultrasound scan, Scan 1") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("1.0 KB, WebP") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Saved at ⁦$capturedAt⁩") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp("clinicalAttachmentViewerDialog")
    }

    @Test
    fun viewer_untitledHeader_isCategoryOnly() {
        showViewer(untitledUs)
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        compose.onNode(hasText("Ultrasound scan") and inViewer, useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasText("Ultrasound scan,", substring = true) and inViewer, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun viewer_hashLine_is12sp_andWrapsWithoutClipping() {
        val hash = "ab".repeat(32)
        showViewer(ultrasound.copy(sha256Hash = hash))
        val matcher = hasText("SHA-256: $hash") and inTagged("clinicalAttachmentViewerDialog")
        val layout = layoutOf(matcher, "hash")
        assertEquals("hash line must be 12sp", 12.sp, layout.layoutInput.style.fontSize)
        assertTrue("64-char hash must wrap (lines=${layout.lineCount})", layout.lineCount > 1)
        compose.assertTextNotClipped("hash", matcher)
    }

    @Test
    fun viewer_deleteAndClose_are48dp_andCallBack() {
        var deletes = 0
        var dismisses = 0
        showViewer(canDelete = true, onDelete = { deletes++ }, onDismiss = { dismisses++ })
        assertMinTouchTarget("deleteAttachmentBtn")
        assertMinTouchTarget("dismissAttachmentBtn")
        compose.onNodeWithTag("deleteAttachmentBtn").assert(hasContentDescription("Delete attachment")).assertIsEnabled()
        compose.onNodeWithTag("dismissAttachmentBtn").assert(hasContentDescription("Close"))
        compose.onNodeWithTag("deleteAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, deletes)
        assertEquals("delete must not close the viewer by itself", 0, dismisses)
        compose.onNodeWithTag("dismissAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, dismisses)
    }

    @Test
    fun viewer_cannotDelete_deleteDisabledAndInert() {
        var deletes = 0
        showViewer(canDelete = false, onDelete = { deletes++ })
        compose.onNodeWithTag("deleteAttachmentBtn").assertIsNotEnabled()
        compose.onNodeWithTag("deleteAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(0, deletes)
        compose.onNodeWithTag("dismissAttachmentBtn").assertIsEnabled()
    }

    @Test
    fun viewer_360dpFontScale1_3_nothingClipped() {
        narrowWindow(1.3f)
        showViewer(ultrasound.copy(sha256Hash = "cd".repeat(32)))
        assertViewerWhole(1.3f)
    }

    @Test
    fun viewer_360dpFontScale2_0_nothingClipped() {
        narrowWindow(2.0f)
        showViewer(ultrasound.copy(sha256Hash = "cd".repeat(32)))
        assertViewerWhole(2.0f)
        // The details below the image are reachable: scrolling brings the integrity word and the hash into view.
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        listOf("Integrity not verified", "SHA-256: ${"cd".repeat(32)}").forEach { text ->
            compose.onNode(hasText(text) and inViewer, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        }
    }

    /** The viewer's own vertical scroll position (the scrolling Column of the dialog). */
    private fun viewerScroll(): Float =
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and inTagged("clinicalAttachmentViewerDialog"),
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test
    fun viewer_pinchZoomPansTheImage_notTheDialog_andAt1xASwipeScrollsTheDialog() {
        // 360dp at font scale 2.0 makes the viewer taller than the window, so the dialog really can scroll.
        narrowWindow(2.0f)
        val fileName = "att_slice10_zoom.png"
        val hash = writeVaultImage(fileName)
        showViewer(ultrasound.copy(fileName = fileName, sha256Hash = hash))
        val image = compose.onNode(hasContentDescription("Scan 1") and inTagged("clinicalAttachmentViewerDialog"), useUnmergedTree = true)
        image.assertIsDisplayed()
        node("zoomResetButton").assertDoesNotExist()
        val maxScroll = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and inTagged("clinicalAttachmentViewerDialog"),
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        assertTrue("the viewer must be scrollable in this fixture (max=$maxScroll)", maxScroll > 0f)
        assertEquals(0f, viewerScroll(), 0.5f)

        // Pinch out from the centre: the image zooms and offers the 1x reset.
        image.performTouchInput {
            pinch(
                start0 = center - Offset(20f, 0f),
                end0 = center - Offset(width * 0.4f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(width * 0.4f, 0f),
                durationMillis = 400,
            )
        }
        compose.waitForIdle()
        node("zoomResetButton").assertExists()

        // Zoomed: a one-finger swipe pans the image and leaves the dialog where it is.
        image.performTouchInput { swipeUp(startY = centerY + height * 0.3f, endY = centerY - height * 0.3f, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals("a zoomed pan must not scroll the dialog", 0f, viewerScroll(), 0.5f)
        node("zoomResetButton").assertExists()

        // Back to 1x: the same swipe on the image now scrolls the dialog.
        compose.onNodeWithTag("zoomResetButton", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        node("zoomResetButton").assertDoesNotExist()
        image.performTouchInput { swipeUp(startY = centerY + height * 0.3f, endY = centerY - height * 0.3f, durationMillis = 300) }
        compose.waitForIdle()
        assertTrue("at 1x a swipe on the image must scroll the dialog (scroll=${viewerScroll()})", viewerScroll() > 0.5f)
    }

    private fun assertViewerWhole(fontScale: Float) {
        assertDialogNarrowAndScaled(
            "clinicalAttachmentViewerDialog",
            hasText(base.displayName) and inTagged("clinicalAttachmentViewerDialog"),
            fontScale,
        )
        assertAllTextWhole("clinicalAttachmentViewerDialog")
        assertMinTouchTarget("deleteAttachmentBtn")
        assertMinTouchTarget("dismissAttachmentBtn")
        compose.onNode(hasText("Integrity not verified"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun viewer_rtl360dp_buttonsOnTheLeft_andNothingClipped() {
        narrowWindow(1.3f)
        showViewer(ultrasound.copy(sha256Hash = "ef".repeat(32)), layoutDirection = LayoutDirection.Rtl)
        val inViewer = inTagged("clinicalAttachmentViewerDialog")
        val name = dpBounds(hasText(base.displayName) and inViewer)
        val close = node("dismissAttachmentBtn").getUnclippedBoundsInRoot()
        val delete = node("deleteAttachmentBtn").getUnclippedBoundsInRoot()
        assertTrue("RTL: buttons must sit left of the patient name", close.right <= name.left + 0.5.dp && delete.right <= name.left + 0.5.dp)
        assertTrue("RTL: delete comes first, so it is right of close", delete.left > close.left)
        compose.onNode(hasText("Saved at ⁦$capturedAt⁩") and inViewer, useUnmergedTree = true).assertExists()
        assertAllTextWhole("clinicalAttachmentViewerDialog")
    }

    // ---- tags kept ----

    @Test
    fun stableTagsArePresent() {
        showCard(patientWith(ultrasound))
        listOf(
            "attachmentsSectionCard",
            "addAttachmentBtn",
            "attachmentCard-${ultrasound.id}",
            "attachmentFilter-ALL",
            "attachmentFilter-${AttachmentCategories.ULTRASOUND}",
            "attachmentFilter-${AttachmentCategories.LAB}",
            "attachmentFilter-${AttachmentCategories.PRESCRIPTION}",
            "attachmentFilter-${AttachmentCategories.CLINICAL_PHOTO}",
        ).forEach { node(it).assertExists() }
        assertTrue(boundsOf(hasTestTag("attachmentCard-${ultrasound.id}")).height > 0f)
    }

    @Test
    fun stableTagsArePresent_addDialog() {
        showAddDialog()
        listOf(
            "addAttachmentDialog", "captureCameraBtn", "pickGalleryBtn", "preset_small", "preset_standard",
            "preset_detailed", "cancelAttachmentBtn", "saveAttachmentConfirmBtn",
        ).forEach { node(it).assertExists() }
    }

    @Test
    fun stableTagsArePresent_viewer() {
        showViewer()
        listOf("clinicalAttachmentViewerDialog", "deleteAttachmentBtn", "dismissAttachmentBtn", "image_unavailable")
            .forEach { node(it).assertExists() }
    }
}
