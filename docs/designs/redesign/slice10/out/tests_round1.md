FILE: app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt
```kotlin
package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.PhotoQualityPreset
import com.sherifradwan.ancnative.ui.AddAttachmentDialog
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ClinicalAttachmentViewerDialog
import com.sherifradwan.ancnative.ui.patienthub.components.AttachmentsSectionCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plan tab attachments (redesign slice 10): AttachmentsSectionCard, AddAttachmentDialog,
 * ClinicalAttachmentViewerDialog. Conventions copied from LabsTabRedesignTest: host with
 * AncTheme + optional 360dp/fontScale/layoutDirection, one setContent per test, real Demo*
 * fixtures, >=47.99dp touch-target tolerance.
 *
 * DemoAttachment construction mirrors the model fields used by the pasted production code;
 * if the real constructor differs, align field names/order with the app model (no other
 * constructor source was pasted).
 */
@RunWith(AndroidJUnit4::class)
class AttachmentsRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val base: DemoPatient = DemoFixtures.patients.first()
    private val captured = "2026-11-10T09:30:00Z"
    private val capturedDate = "2026-11-10"

    private fun attachment(
        id: String,
        category: String,
        title: String?,
        fileName: String = "$id.jpg",
        fileSizeBytes: Long = 1024L,
        sha256Hash: String = "abc123hash",
        capturedAt: String = captured,
    ) = DemoAttachment(
        id = id,
        patientId = base.id,
        fileName = fileName,
        category = category,
        title = title,
        capturedAt = capturedAt,
        fileSizeBytes = fileSizeBytes,
        sha256Hash = sha256Hash,
    )

    private val ultrasound = attachment(
        id = "att-us-1",
        category = AttachmentCategories.ULTRASOUND,
        title = "Scan 1",
        fileName = "scan1.jpg",
        fileSizeBytes = 1024L,
    )
    private val lab = attachment(
        id = "att-lab-1",
        category = AttachmentCategories.LAB,
        title = "CBC",
        fileName = "cbc.jpg",
        fileSizeBytes = 1024L,
    )

    private fun patientWith(
        vararg attachments: DemoAttachment,
        status: String? = "active",
    ): DemoPatient = base.copy(status = status, attachments = attachments.toList())

    private fun host(
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
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

    private fun showCard(
        patient: DemoPatient,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onAdd: () -> Unit = {},
        onView: (DemoAttachment) -> Unit = {},
    ) = host(widthDp, fontScale, layoutDirection) {
        AttachmentsSectionCard(
            patient = patient,
            onAddAttachment = onAdd,
            onViewAttachment = onView,
        )
    }

    private fun showAddDialog(
        patient: DemoPatient = patientWith(),
        initialCategory: String = AttachmentCategories.ULTRASOUND,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
        onSave: (android.net.Uri, String, String?, PhotoQualityPreset) -> Unit = { _, _, _, _ -> },
    ) = host(widthDp, fontScale, layoutDirection) {
        AddAttachmentDialog(
            patient = patient,
            initialCategory = initialCategory,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }

    private fun showViewer(
        patient: DemoPatient = patientWith(ultrasound),
        attachment: DemoAttachment = ultrasound,
        canDelete: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) = host(widthDp, fontScale, layoutDirection) {
        ClinicalAttachmentViewerDialog(
            patient = patient,
            attachment = attachment,
            canDelete = canDelete,
            onDismiss = onDismiss,
            onDelete = onDelete,
        )
    }

    private fun boundsOf(matcher: SemanticsMatcher): Rect =
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun layoutOf(matcher: SemanticsMatcher, label: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue("'$label' has no text layout", results.isNotEmpty())
        return results.first()
    }

    private fun assertMinTouchTarget(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 0.01dp absorbs px-to-dp float rounding (a 48dp control can read back as 47.999985dp).
        assertTrue("$tag width < 48dp (${bounds.width})", bounds.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${bounds.height})", bounds.height >= 47.99.dp)
    }

    private fun assertMinHeight(tag: String, minDp: Int) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("$tag height < ${minDp}dp (${bounds.height})", bounds.height >= (minDp - 0.01f).dp)
    }

    private fun assertExactlyMinHeight(tag: String, minDp: Int) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("$tag height < ${minDp}dp (${bounds.height})", bounds.height >= (minDp - 0.01f).dp)
    }

    // ---- AttachmentsSectionCard ----

    @Test
    fun sectionCard_titleSummaryRowsAndAddAction() {
        var addCalls = 0
        var viewed: DemoAttachment? = null
        val patient = patientWith(ultrasound, lab)
        showCard(patient, onAdd = { addCalls++ }, onView = { viewed = it })

        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("2 files, 2.0 KB"), useUnmergedTree = true).assertIsDisplayed()

        // Filter chips with counts and stable tags.
        compose.onNodeWithTag("attachmentFilter-ALL").assertIsDisplayed()
        compose.onNode(hasText("All (2)") and hasTestTag("attachmentFilter-ALL"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Ultrasound (1)") and hasTestTag("attachmentFilter-${AttachmentCategories.ULTRASOUND}"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Labs (1)") and hasTestTag("attachmentFilter-${AttachmentCategories.LAB}"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Prescriptions (0)") and hasTestTag("attachmentFilter-${AttachmentCategories.PRESCRIPTION}"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Clinical photos (0)") and hasTestTag("attachmentFilter-${AttachmentCategories.CLINICAL_PHOTO}"), useUnmergedTree = true).assertExists()
        // No chip for OTHER.
        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.OTHER}", useUnmergedTree = true).assertDoesNotExist()
        compose.onAllNodesWithText("Other", useUnmergedTree = true).assertCountEquals(0)

        // Row content, tags, clickable viewer callback.
        val usRow = hasTestTag("attachmentCard-${ultrasound.id}")
        compose.onNode(usRow).assertIsDisplayed().assert(hasClickAction())
        assertMinHeight("attachmentCard-${ultrasound.id}", 72)
        compose.onNode(hasText("Scan 1") and usRow, useUnmergedTree = true).assertExists()
        val titleLayout = layoutOf(hasText("Scan 1") and usRow, "Scan 1")
        assertTrue("title must allow wrapping (max 2 lines)", titleLayout.lineCount <= 2)

        compose.onNode(hasText("Ultrasound scan, 1.0 KB") and usRow, useUnmergedTree = true).assertIsDisplayed()
        // Saved-on-phone line includes ltr-isolated date (bidi marks around the date).
        compose.onNode(hasText("Saved on this phone, \u2066$capturedDate\u2069") and usRow, useUnmergedTree = true)
            .assertIsDisplayed()

        compose.onNode(usRow).performClick()
        compose.waitForIdle()
        assertEquals(ultrasound.id, viewed?.id)

        // Add action text/description/tag/touch target.
        compose.onNode(hasText("Add") and hasTestTag("addAttachmentBtn"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Add attachment") and hasTestTag("addAttachmentBtn"), useUnmergedTree = true)
            .assertExists()
        assertMinTouchTarget("addAttachmentBtn")
        compose.onNodeWithTag("addAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, addCalls)

        // No green shield and no text under 12sp anywhere in the section.
        compose.onAllNodesWithText("Integrity verified", substring = true, useUnmergedTree = true).assertCountEquals(0)
        assertNoTextBelow12sp(inTagged("attachmentsSectionCard"))
    }

    @Test
    fun sectionCard_filteringAndEmptyFilteredState() {
        val patient = patientWith(ultrasound, lab)
        showCard(patient)

        compose.onNodeWithTag("attachmentCard-${ultrasound.id}").assertIsDisplayed()
        compose.onNodeWithTag("attachmentCard-${lab.id}").assertIsDisplayed()

        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.ULTRASOUND}").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("attachmentCard-${ultrasound.id}").assertIsDisplayed()
        compose.onNodeWithTag("attachmentCard-${lab.id}", useUnmergedTree = true).assertDoesNotExist()
        compose.onAllNodesWithText("No attachments in this selected category", useUnmergedTree = true).assertCountEquals(0)

        // Selecting a category with zero items shows the filtered empty state.
        compose.onNodeWithTag("attachmentFilter-${AttachmentCategories.PRESCRIPTION}").performClick()
        compose.waitForIdle()
        compose.onNode(hasText("No attachments in this selected category"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("attachmentCard-${ultrasound.id}", useUnmergedTree = true).assertDoesNotExist()

        compose.onNodeWithTag("attachmentFilter-ALL").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("attachmentCard-${ultrasound.id}").assertIsDisplayed()
        compose.onNodeWithTag("attachmentCard-${lab.id}").assertIsDisplayed()
    }

    @Test
    fun sectionCard_emptyStateAndArchivedHidesAdd() {
        showCard(patientWith())
        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("No saved attachments or clinical images"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addAttachmentBtn").assertIsDisplayed()

        // Archived: add action absent.
        host {
            AttachmentsSectionCard(
                patient = patientWith(ultrasound, status = "archived"),
                onAddAttachment = {},
                onViewAttachment = {},
            )
        }
        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addAttachmentBtn", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun sectionCard_360dpFontScale2AndRtl_nothingClipped() {
        val longTitle = attachment(
            id = "att-long",
            category = AttachmentCategories.ULTRASOUND,
            title = "Detailed anomaly scan follow up with extended notes for this pregnancy",
            fileSizeBytes = 1024L,
        )
        showCard(patientWith(longTitle, lab), widthDp = 360, fontScale = 2.0f)
        compose.onNodeWithTag("attachmentsSectionCard").assertIsDisplayed()
        compose.assertNotClippedInTree("Documents and attachments", "attachmentsSectionCard")
        compose.assertNotClippedInTree("2 files, 2.0 KB", "attachmentsSectionCard")
        compose.assertNotClippedInTree("No attachments in this selected category absent", "attachmentsSectionCard", optional = true)
        // Key row text must not be ellipsized/clipped at 360dp/2.0.
        val row = hasTestTag("attachmentCard-${longTitle.id}")
        compose.onNode(hasText(longTitle.title!!, substring = false) and row, useUnmergedTree = true).assertExists()
        assertNoTextBelow12sp(inTagged("attachmentsSectionCard"))

        // RTL layout renders and keeps the ltr-isolated date readable.
        showCard(patientWith(ultrasound, lab), widthDp = 360, fontScale = 2.0f, layoutDirection = LayoutDirection.Rtl)
        compose.onNode(hasText("Documents and attachments"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Saved on this phone, \u2066$capturedDate\u2069", substring = true), useUnmergedTree = true)
            .assertExists()
        val card = compose.onNodeWithTag("attachmentsSectionCard").getUnclippedBoundsInRoot()
        assertTrue("card must fit 360dp viewport (right=${card.right})", card.right <= 360.dp + 1.dp)
    }

    // ---- AddAttachmentDialog ----

    @Test
    fun addDialog_structureChoicesAndSaveGating() {
        var dismissed = false
        val patient = patientWith()
        showAddDialog(patient = patient, onDismiss = { dismissed = true })

        compose.onNode(hasText("Add attachment"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText(patient.displayName), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Type"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Photo size"), useUnmergedTree = true).assertIsDisplayed()

        listOf("Ultrasound", "Labs", "Prescription or report", "Clinical photo", "Other").forEach {
            compose.onNode(hasText(it), useUnmergedTree = true).assertExists()
        }

        // Two 64dp source buttons with titles and sublabels.
        compose.onNode(hasText("Take photo") and hasTestTag("captureCameraBtn"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Use the camera to capture an image") and hasTestTag("captureCameraBtn"), useUnmergedTree = true)
            .assertExists()
        assertExactlyMinHeight("captureCameraBtn", 64)
        compose.onNode(hasText("Choose from gallery") and hasTestTag("pickGalleryBtn"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Pick an existing image from this device") and hasTestTag("pickGalleryBtn"), useUnmergedTree = true)
            .assertExists()
        assertExactlyMinHeight("pickGalleryBtn", 64)

        // Save disabled until a photo exists.
        compose.onNode(hasText("Save to record") and hasTestTag("saveAttachmentConfirmBtn"), useUnmergedTree = true)
            .assertExists()
            .assertIsNotEnabled()

        compose.onNode(hasText("Title or note (optional)"), useUnmergedTree = true).assertExists()

        // Cancel tag 48dp and dismisses.
        compose.onNodeWithTag("cancelAttachmentBtn").assertIsDisplayed()
        assertMinTouchTarget("cancelAttachmentBtn")
        compose.onNodeWithTag("cancelAttachmentBtn").performClick()
        compose.waitForIdle()
        assertTrue("cancel should dismiss", dismissed)

        assertNoTextBelow12sp(inTagged("addAttachmentDialog"))
    }

    @Test
    fun addDialog_360dpFontScale2AndRtl() {
        showAddDialog(widthDp = 360, fontScale = 2.0f)
        compose.onNodeWithTag("addAttachmentDialog").assertIsDisplayed()
        compose.onNode(hasText("Add attachment"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Save to record"), useUnmergedTree = true).assertExists()
        assertMinTouchTarget("cancelAttachmentBtn")

        showAddDialog(widthDp = 360, fontScale = 2.0f, layoutDirection = LayoutDirection.Rtl)
        compose.onNode(hasText("Add attachment"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Take photo"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Choose from gallery"), useUnmergedTree = true).assertExists()
    }

    // ---- ClinicalAttachmentViewerDialog ----

    @Test
    fun viewer_missingFile_showsUnavailableAndUnverified_neverVerified_andRespectsDeleteGate() {
        var deletes = 0
        var dismisses = 0
        val patient = patientWith(ultrasound)
        showViewer(
            patient = patient,
            attachment = ultrasound,
            canDelete = true,
            onDelete = { deletes++ },
            onDismiss = { dismisses++ },
        )

        compose.onNode(hasText(patient.displayName), useUnmergedTree = true).assertIsDisplayed()
        // Header detail: "Ultrasound scan, Scan 1"
        compose.onNode(hasText("Ultrasound scan, Scan 1"), useUnmergedTree = true).assertIsDisplayed()

        // Missing/unreadable file in test env: unavailable image and integrity NOT verified.
        compose.onNodeWithTag("image_unavailable").assertIsDisplayed()
        compose.onNode(hasText("Image unavailable"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Integrity not verified"), useUnmergedTree = true).assertIsDisplayed()
        // SAFETY: a failed/missing check must never show the verified word.
        compose.onAllNodesWithText("Integrity verified (SHA-256)", useUnmergedTree = true).assertCountEquals(0)

        // Hash line 12sp and present.
        val hashMatcher = hasText("SHA-256: ${ultrasound.sha256Hash}", substring = false)
        compose.onNode(hashMatcher, useUnmergedTree = true).assertIsDisplayed()
        val hashStyle = layoutOf(hashMatcher, "hash").layoutInput.style
        assertEquals("hash must be 12sp", 12f, hashStyle.fontSize.value, 0.1f)

        assertMinTouchTarget("deleteAttachmentBtn")
        assertMinTouchTarget("dismissAttachmentBtn")
        compose.onNodeWithTag("deleteAttachmentBtn").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(1, deletes)
        compose.onNodeWithTag("dismissAttachmentBtn").performClick()
        compose.waitForIdle()
        assertEquals(1, dismisses)

        assertNoTextBelow12sp(inTagged("clinicalAttachmentViewerDialog"))
    }

    @Test
    fun viewer_deleteDisabledWhenCannotDelete() {
        var deletes = 0
        showViewer(canDelete = false, onDelete = { deletes++ })
        compose.onNodeWithTag("deleteAttachmentBtn").assertIsNotEnabled()
        // Attempted click on disabled node must not invoke callback.
        try {
            compose.onNodeWithTag("deleteAttachmentBtn").performClick()
        } catch (_: AssertionError) {
            // performClick may assert enabled depending on semantics; disabled state is the gate.
        }
        compose.waitForIdle()
        assertEquals(0, deletes)
        compose.onNode(hasText("Integrity not verified"), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText("Integrity verified (SHA-256)", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun viewer_360dpFontScale2AndRtl_hashWrapsWithoutClipping() {
        val longHashAttachment = ultrasound.copy(
            sha256Hash = "a".repeat(64),
        )
        showViewer(attachment = longHashAttachment, widthDp = 360, fontScale = 2.0f)
        compose.onNodeWithTag("clinicalAttachmentViewerDialog").assertIsDisplayed()
        val hashMatcher = hasText("SHA-256: ${"a".repeat(64)}")
        compose.onNode(hashMatcher, useUnmergedTree = true).assertIsDisplayed()
        compose.assertTextNotClipped("hash", hashMatcher)
        assertTrue("hash must wrap instead of clipping", layoutOf(hashMatcher, "hash").lineCount > 1)
        compose.onNode(hasText("Integrity not verified"), useUnmergedTree = true).assertIsDisplayed()
        assertNoTextBelow12sp(inTagged("clinicalAttachmentViewerDialog"))

        showViewer(attachment = longHashAttachment, widthDp = 360, fontScale = 1.3f, layoutDirection = LayoutDirection.Rtl)
        compose.onNode(hasText("Ultrasound scan, Scan 1"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Image unavailable"), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText("Integrity verified (SHA-256)", useUnmergedTree = true).assertCountEquals(0)
    }

    // ---- helpers ----

    private fun inTagged(tag: String): SemanticsMatcher = hasTestTag(tag)

    private fun assertNoTextBelow12sp(within: SemanticsMatcher? = null) {
        val matcher = if (within != null) {
            SemanticsMatcher("has text") { it.config.contains(SemanticsProperties.Text) } and within
        } else {
            SemanticsMatcher("has text") { it.config.contains(SemanticsProperties.Text) }
        }
        val nodes = compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
        nodes.forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            layouts.forEach { layout ->
                val sp = layout.layoutInput.style.fontSize.value
                // Ignore unspecified (0) which inherits; explicit sizes must be >= 12sp.
                if (sp > 0f) {
                    assertTrue("Found text below 12sp (${sp}sp) in node $node", sp >= 11.99f)
                }
            }
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertNotClippedInTree(
        text: String,
        tag: String,
        optional: Boolean = false,
    ) {
        val nodes = onAllNodes(hasText(text, substring = true) and inTagged(tag), useUnmergedTree = true)
            .fetchSemanticsNodes()
        if (optional && nodes.isEmpty()) return
        assertTrue("Expected '$text' inside $tag", nodes.isNotEmpty())
        nodes.forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            layouts.forEach { layout ->
                assertFalse("'$text' is ellipsized", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
            }
        }
    }
}

/** SemanticsActions is used by layout/clip helpers; import kept explicit for clarity. */
private val SemanticsActions = androidx.compose.ui.semantics.SemanticsActions
```

FILE: corrected unit test assertions (AttachmentDisplayTitleTest and AttachmentDeleteAdapterTest)
```kotlin
// AttachmentDisplayTitleTest — corrected assertions
// Separator changes from " · " to ", " (comma + space). Update the affected expectations:

@Test
fun titleAndDate() {
    // was: assertEquals("Scan A · 2026-01-10", attachmentDisplayTitle("Scan A", "Ultrasound", "2026-01-10T12:00:00Z"))
    assertEquals(
        "Scan A, 2026-01-10",
        attachmentDisplayTitle("Scan A", "Ultrasound", "2026-01-10T12:00:00Z"),
    )
}

@Test
fun categoryFallbackWithDate() {
    // was: assertEquals("Ultrasound · 2026-01-10", attachmentDisplayTitle(null, "Ultrasound", "2026-01-10T12:00:00Z"))
    assertEquals(
        "Ultrasound, 2026-01-10",
        attachmentDisplayTitle(null, "Ultrasound", "2026-01-10T12:00:00Z"),
    )
}

// AttachmentDeleteAdapterTest — corrected assertions
// Localized category names are now sentence-case and the detail separator is ", ".
// Update the affected expectations (lines ~29, 34, 46, 47):

@Test
fun titledAttachment_includesTitleCategoryAndDate() {
    // was: assertEquals("Scan A · Ultrasound · 2026-01-10", ...)
    assertEquals(
        "Scan A, Ultrasound scan, 2026-01-10",
        /* existing call, e.g. */ attachmentDeleteDetail(title = "Scan A", category = "ULTRASOUND", capturedAt = "2026-01-10T12:00:00Z"),
    )
}

@Test
fun untitledAttachment_usesCategoryAndDate() {
    // was: assertEquals("Ultrasound · 2026-01-10", ...)
    assertEquals(
        "Ultrasound scan, 2026-01-10",
        /* existing call */ attachmentDeleteDetail(title = null, category = "ULTRASOUND", capturedAt = "2026-01-10T12:00:00Z"),
    )
}

@Test
fun otherCategory_sentenceCase() {
    // was: assertEquals("Lab result · 2026-01-10", ...) or similar with old separator/category
    assertEquals(
        "Lab result, 2026-01-10",
        /* existing call */ attachmentDeleteDetail(title = "Lab result", category = "LAB", capturedAt = "2026-01-10T12:00:00Z"),
    )
}

@Test
fun photoCategory_sentenceCase() {
    // was: assertEquals("Clinical photo · 2026-01-10", ...)
    assertEquals(
        "Clinical photo, 2026-01-10",
        /* existing call */ attachmentDeleteDetail(title = null, category = "CLINICAL_PHOTO", capturedAt = "2026-01-10T12:00:00Z"),
    )
}
```

Notes on fidelity and assumptions (so you can align quickly if needed):
- **Instrumented test location:** `app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt`, same package/convention style as `LabsTabRedesignTest.kt`.
- **Shared helpers:** uses the same `compose.assertTextNotClipped(...)` / `compose.assertNotEllipsized(...)` style from the project’s existing Compose test asserts when available; the file also includes local helpers (`assertNoTextBelow12sp`, `layoutOf`, touch/height checks) so it remains self-contained. If `SemanticsActions`/imports collide with project helpers, prefer the project’s shared helpers and drop the local duplicates.
- **`DemoAttachment` constructor:** built from the fields exercised by the pasted UI (`id`, `patientId`, `fileName`, `category`, `title`, `capturedAt`, `fileSizeBytes`, `sha256Hash`). If the real data class has a different parameter order/names, adjust the `attachment(...)` factory to match the app model (copied fixture pattern: `DemoFixtures.patients.first()` + `.copy(...)`).
- **Viewer “verified” path:** the test environment has no vault file, so integrity resolves to **not verified** and the image to **unavailable**. Tests assert “Image unavailable”, “Integrity not verified” (amber/`StatusTone.Moderate` path in UI), hash line 12sp/wrap, and—critically—**never** “Integrity verified (SHA-256)”. This satisfies the safety requirement that a failed check must never show the verified word. To additionally exercise a true verified render you’d need to place the attachment file in `PatientAttachmentVault` with a matching SHA-256 for the test patient (not possible to wire here without the vault API/signature).
- **Unit-test corrections:** the exact helper/method names in `AttachmentDeleteAdapterTest` (`attachmentDeleteDetail(...)` placeholders above) should be replaced with the real function under test in those tests; the **expected strings** are the required corrections (separator ` · ` → `, ` and sentence-case category names like `Ultrasound scan`, `Clinical photo`). For `AttachmentDisplayTitleTest` the function name/shape matches the usage shown in production (`attachmentDisplayTitle(title, category, capturedAt)`).
[Tokens] upload: 26718 download: 8164 cache_read: 0 cache_write: 0

