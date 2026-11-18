package com.sherifradwan.ancnative

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import com.sherifradwan.ancnative.ui.report.PictureCandidate
import com.sherifradwan.ancnative.ui.report.ReportPicturePicker
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android UI tests for Slice S4 picture selection before print:
 * - Pictures button visibility depending on picture presence
 * - Opening and closing the picture picker sheet
 * - Default empty selection state
 * - Toggling candidate rows and badge counter updates
 * - Select all and Clear actions
 * - Done dismissal
 * - Native report reflection: "Fig N" outcome references and "Attached images" line
 * - Missing file thumbnail failure displaying "Image not available" without crash
 * - 48dp minimum row height and touch targets
 * - 360dp narrow viewport overflow verification
 */
@RunWith(AndroidJUnit4::class)
class ReportPicturePickerTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val basePatient: DemoPatient = DemoFixtures.patients.first()

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()

    private fun assertMinTouchTarget(tag: String) {
        val b = bounds(tag)
        assertTrue("$tag width < 48dp (${b.width})", b.width >= 47.99.dp)
        assertTrue("$tag height < 48dp (${b.height})", b.height >= 47.99.dp)
    }

    private fun assertNoClippingInContainer(tag: String) {
        val container = compose.onNode(hasTestTag(tag), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val nodes = compose.onAllNodes(
            (SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)) and inTagged(tag),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no text found in $tag", nodes.isNotEmpty())
        nodes.forEach { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val layout = layouts.firstOrNull() ?: return@forEach
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: ""
            if (text.isBlank()) return@forEach
            assertTrue("'$text' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
            for (line in 0 until layout.lineCount) {
                val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
                assertTrue(
                    "'$text' line $line wider than node ($lineWidth > ${layout.size.width})",
                    lineWidth <= layout.size.width + 1f,
                )
                assertFalse("'$text' ellipsized on line $line", layout.isLineEllipsized(line))
            }
            val byId = SemanticsMatcher("node ${n.id}") { it.id == n.id }
            val b = compose.onNode(byId, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("'$text' starts outside $tag (${b.left} < ${container.left - 1.dp})", b.left >= container.left - 1.dp)
            assertTrue("'$text' runs past $tag (${b.right} > ${container.right + 1.dp})", b.right <= container.right + 1.dp)
        }
    }

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

    @After
    fun tearDown() {
        restoreResources()
    }

    private fun show(
        patient: DemoPatient,
        securitySettings: SecuritySettingsState? = null,
        fontScale: Float = 1.0f,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onDismiss: () -> Unit = {},
    ) {
        narrowWindow(fontScale)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, fontScale),
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        PatientTwoPageReportDialog(
                            patient = patient,
                            securitySettings = securitySettings,
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun patientWithPictures(
        scanImages: List<String> = listOf("scan_photo_1.jpg"),
        attachments: List<DemoAttachment> = emptyList(),
    ): DemoPatient {
        val firstScan = basePatient.ultrasoundScans.first().copy(imageUris = scanImages)
        return basePatient.copy(
            ultrasoundScans = listOf(firstScan) + basePatient.ultrasoundScans.drop(1),
            attachments = attachments,
        )
    }

    // =========================================================================
    // 1. Pictures button visibility & picker opening
    // =========================================================================

    @Test
    fun picturesButton_hiddenWhenPatientHasNoPictures() {
        // basePatient has no scan imageUris and no attachments
        show(basePatient)
        node("reportPicturesButton").assertDoesNotExist()
    }

    @Test
    fun picturesButton_shownWhenPatientHasPictures_andOpensPicker() {
        val patient = patientWithPictures(scanImages = listOf("scan_test_1.jpg"))
        show(patient)

        node("reportPicturesButton").assertIsDisplayed().performClick()
        compose.waitForIdle()

        node("reportPicturePicker").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_picture_picker_title)).assertIsDisplayed()
    }

    // =========================================================================
    // 2. Default selection, toggle, badge counter updates
    // =========================================================================

    @Test
    fun picturePicker_nothingSelectedByDefault() {
        val patient = patientWithPictures(scanImages = listOf("scan_test_1.jpg"))
        show(patient)

        // Button has default "Pictures" description without count
        node("reportPicturesButton")
            .assertIsDisplayed()
            .assertContentDescriptionEquals(str(R.string.report_pictures_cd))

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        // Picker subtitle indicates 0 selected
        compose.onNodeWithText(str(R.string.report_picture_picker_subtitle, 0, 1)).assertIsDisplayed()

        // Candidate row checkbox is off
        val scanId = patient.ultrasoundScans.first().id
        node("reportPictureRow_scan:$scanId:0").assertIsDisplayed().assertIsOff()
    }

    @Test
    fun picturePicker_tappingRowTogglesSelection_andUpdatesBadgeCount() {
        val patient = patientWithPictures(scanImages = listOf("scan_test_1.jpg"))
        show(patient)

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        val scanId = patient.ultrasoundScans.first().id
        val rowTag = "reportPictureRow_scan:$scanId:0"

        // Tap row to toggle ON
        node(rowTag).performClick()
        compose.waitForIdle()

        node(rowTag).assertIsOn()
        compose.onNodeWithText(str(R.string.report_picture_picker_subtitle, 1, 1)).assertIsDisplayed()
        node("reportPicturesButton").assertContentDescriptionEquals(str(R.string.report_pictures_selected_cd, 1))

        // Tap row again to toggle OFF
        node(rowTag).performClick()
        compose.waitForIdle()

        node(rowTag).assertIsOff()
        compose.onNodeWithText(str(R.string.report_picture_picker_subtitle, 0, 1)).assertIsDisplayed()
        node("reportPicturesButton").assertContentDescriptionEquals(str(R.string.report_pictures_cd))
    }

    // =========================================================================
    // 3. Select all and Clear
    // =========================================================================

    @Test
    fun picturePicker_selectAllAndClear() {
        val patient = patientWithPictures(scanImages = listOf("photo_a.jpg", "photo_b.jpg"))
        show(patient)

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        val scanId = patient.ultrasoundScans.first().id
        val row0 = "reportPictureRow_scan:$scanId:0"
        val row1 = "reportPictureRow_scan:$scanId:1"

        // Click Select all
        node("reportPicturesSelectAll").performClick()
        compose.waitForIdle()

        node(row0).assertIsOn()
        node(row1).assertIsOn()
        compose.onNodeWithText(str(R.string.report_picture_picker_subtitle, 2, 2)).assertIsDisplayed()
        node("reportPicturesButton").assertContentDescriptionEquals(str(R.string.report_pictures_selected_cd, 2))

        // Click Clear
        node("reportPicturesClear").performClick()
        compose.waitForIdle()

        node(row0).assertIsOff()
        node(row1).assertIsOff()
        compose.onNodeWithText(str(R.string.report_picture_picker_subtitle, 0, 2)).assertIsDisplayed()
        node("reportPicturesButton").assertContentDescriptionEquals(str(R.string.report_pictures_cd))
    }

    // =========================================================================
    // 4. Done closes picker sheet
    // =========================================================================

    @Test
    fun picturePicker_doneCloses() {
        val patient = patientWithPictures(scanImages = listOf("photo_a.jpg"))
        show(patient)

        node("reportPicturesButton").performClick()
        compose.waitForIdle()
        node("reportPicturePicker").assertIsDisplayed()

        node("reportPicturesDone").performClick()
        compose.waitForIdle()

        node("reportPicturePicker").assertDoesNotExist()
    }

    // =========================================================================
    // 5. Native report reflection: "Fig N" & "Attached images" line
    // =========================================================================

    @Test
    fun pictureSelection_showsFigureReferencesAndAttachedImagesSummary() {
        val patient = patientWithPictures(scanImages = listOf("photo_fig.jpg"))
        show(patient)

        // Open picker, select photo, close with Done
        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        val scanId = patient.ultrasoundScans.first().id
        node("reportPictureRow_scan:$scanId:0").performClick()
        compose.waitForIdle()

        node("reportPicturesDone").performClick()
        compose.waitForIdle()

        // "Attached images: 1 selected" summary banner is displayed
        node("reportAttachedImagesSummary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.report_attached_images_selected, 1), substring = true)
            .assertIsDisplayed()

        // The visit linked to this scan shows the "Fig 1" reference in scan outcome
        val linkedVisitId = patient.ultrasoundScans.first().linkedVisitId
        if (linkedVisitId != null) {
            val outcomeNode = node("reportScanOutcome_$linkedVisitId").performScrollTo()
            outcomeNode.assertIsDisplayed()
            compose.onNode(
                hasText(str(R.string.report_figure_ref_format, 1), substring = true) and
                    inTagged("reportScanOutcome_$linkedVisitId"),
                useUnmergedTree = true,
            ).assertIsDisplayed()
        }
    }

    @Test
    fun pictureSelection_noneSelected_noPicturesLineAppears() {
        val patient = patientWithPictures(scanImages = listOf("photo_fig.jpg"))
        show(patient)

        // With nothing selected by default
        node("reportAttachedImagesSummary").assertDoesNotExist()

        val linkedVisitId = patient.ultrasoundScans.first().linkedVisitId
        if (linkedVisitId != null) {
            node("reportScanOutcome_$linkedVisitId").performScrollTo().assertIsDisplayed()
            compose.onAllNodes(
                hasText("Fig 1", substring = true) and inTagged("reportScanOutcome_$linkedVisitId"),
                useUnmergedTree = true,
            ).assertCountEquals(0)
        }
    }

    // =========================================================================
    // 6. Missing file shows "Image not available" without crash
    // =========================================================================

    @Test
    fun picturePicker_missingFile_showsImageNotAvailableTile_withoutCrashing() {
        // Candidate with non-existent vault path
        val patient = patientWithPictures(scanImages = listOf("missing_vault_scan_404.jpg"))
        show(patient)

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        // "Image not available" fallback text appears for the missing thumbnail without crashing
        compose.onNodeWithText(str(R.string.report_image_not_available), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // =========================================================================
    // 7. 48dp minimum row height and touch targets
    // =========================================================================

    @Test
    fun picturePicker_min48dpRowHeightAndTouchTargets() {
        val patient = patientWithPictures(scanImages = listOf("photo_target.jpg"))
        show(patient)

        assertMinTouchTarget("reportPicturesButton")

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        val scanId = patient.ultrasoundScans.first().id
        val rowTag = "reportPictureRow_scan:$scanId:0"

        // Candidate row minimum height is 64dp (>= 48dp touch target standard)
        val rowBounds = bounds(rowTag)
        assertTrue("candidate row height < 48dp (${rowBounds.height})", rowBounds.height >= 47.99.dp)

        // Action buttons meet 48dp minimum touch target
        assertMinTouchTarget("reportPicturesSelectAll")
        assertMinTouchTarget("reportPicturesClear")
        assertMinTouchTarget("reportPicturesDone")
    }

    // =========================================================================
    // 8. 360dp narrow window no horizontal overflow
    // =========================================================================

    @Test
    fun picturePicker_narrow360dp_noHorizontalOverflow() {
        val patient = patientWithPictures(scanImages = listOf("photo_narrow.jpg"))
        show(patient, fontScale = 1.3f)

        assertNoClippingInContainer("patientTwoPageReportDialog")

        node("reportPicturesButton").performClick()
        compose.waitForIdle()

        assertNoClippingInContainer("reportPicturePicker")
    }
}
