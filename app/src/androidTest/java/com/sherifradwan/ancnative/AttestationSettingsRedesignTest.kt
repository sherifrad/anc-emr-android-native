package com.sherifradwan.ancnative

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.SecuritySettingsDialog
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Android UI tests for clinic attestation configuration in SecuritySettingsDialog and
 * verification in PatientTwoPageReportDialog (ReportAttestationCard).
 *
 * Follows [SecurityLockVaultRedesignTest] style:
 * - Single setContent per mount inside 360dp narrow window host
 * - Isolated test SharedPreferences for SecuritySettingsRepository
 * - >= 47.9dp touch target height assertions
 * - RTL right-aligned text layout assertions on Arabic fields
 * - Repository persistence and ReportAttestationCard presentation verification
 */
@RunWith(AndroidJUnit4::class)
class AttestationSettingsRedesignTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val basePatient: DemoPatient = DemoFixtures.patients.first()

    private var savedConfig: Configuration? = null
    private var savedMetrics: DisplayMetrics? = null

    @After
    fun tearDown() {
        restoreResources()
    }

    private fun narrowWindow(fontScale: Float = 1f) {
        compose.activityRule.scenario.onActivity { activity ->
            val res = activity.resources
            if (savedConfig == null) {
                savedConfig = Configuration(res.configuration)
                savedMetrics = DisplayMetrics().apply { setTo(res.displayMetrics) }
            }
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
        savedMetrics = null
    }

    private fun createIsolatedRepo(): SecuritySettingsRepository {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("test_sec_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        return SecuritySettingsRepository(prefs)
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    private fun bounds(tag: String): DpRect = node(tag).getUnclippedBoundsInRoot()

    private fun showSecurityDialog(repo: SecuritySettingsRepository) {
        narrowWindow(1f)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, 1f),
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag("host")) {
                            SecuritySettingsDialog(
                                securityRepo = repo,
                                onDismiss = {},
                                onLockNow = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun showReportDialog(patient: DemoPatient, securitySettings: SecuritySettingsState?) {
        narrowWindow(1f)
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                AncTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(base, 1f),
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag("host")) {
                            PatientTwoPageReportDialog(
                                patient = patient,
                                securitySettings = securitySettings,
                                onDismiss = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun textLayoutOfTag(tag: String): TextLayoutResult {
        val root = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        root.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        if (results.isNotEmpty()) return results.first()
        for (child in root.children) {
            val childResults = mutableListOf<TextLayoutResult>()
            child.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(childResults)
            if (childResults.isNotEmpty()) return childResults.first()
        }
        val allNodes = compose.onAllNodes(inTagged(tag), useUnmergedTree = true).fetchSemanticsNodes()
        for (n in allNodes) {
            val nResults = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(nResults)
            if (nResults.isNotEmpty()) return nResults.first()
        }
        error("No TextLayoutResult found for tag $tag")
    }

    @Test
    fun brandingSection_allFiveFieldsExist_withDefaultValuesAndAtLeast48dpHeight() {
        val repo = createIsolatedRepo()
        showSecurityDialog(repo)

        val fieldExpectations = listOf(
            "settingsDoctorNameEn" to SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN,
            "settingsDoctorSpecialtyEn" to SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_EN,
            "settingsDoctorNameAr" to SecuritySettingsState.DEFAULT_DOCTOR_NAME_AR,
            "settingsDoctorSpecialtyAr" to SecuritySettingsState.DEFAULT_DOCTOR_SPECIALTY_AR,
            "settingsClinicPhone" to SecuritySettingsState.DEFAULT_CLINIC_PHONE,
        )

        for ((tag, expectedDefault) in fieldExpectations) {
            // Scroll to field and assert displayed
            node(tag).performScrollTo().assertIsDisplayed()

            // Verify initial content equals canonical default
            val semanticsNode = node(tag).fetchSemanticsNode()
            val text = semanticsNode.config.getOrNull(SemanticsProperties.EditableText)?.text
                ?: semanticsNode.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
            assertEquals("Field $tag text matches default", expectedDefault, text)

            // Touch target ergonomics: must be at least 48dp tall
            val b = bounds(tag)
            assertTrue("$tag height must be >= 48dp (was ${b.height})", b.height >= 47.9.dp)
            assertTrue("$tag width must be >= 48dp (was ${b.width})", b.width >= 47.9.dp)
        }
    }

    @Test
    fun brandingSection_arabicFields_drawRtlRightAligned() {
        val repo = createIsolatedRepo()
        showSecurityDialog(repo)

        // Arabic doctor name field: right-aligned, RTL direction
        node("settingsDoctorNameAr").performScrollTo().assertIsDisplayed()
        val layoutNameAr = textLayoutOfTag("settingsDoctorNameAr")
        assertEquals("Arabic doctor name textAlign", TextAlign.Right, layoutNameAr.layoutInput.style.textAlign)
        assertEquals("Arabic doctor name textDirection", TextDirection.Rtl, layoutNameAr.layoutInput.style.textDirection)
        assertEquals("Arabic doctor name paragraph direction", ResolvedTextDirection.Rtl, layoutNameAr.getParagraphDirection(0))

        // Arabic doctor specialty field: right-aligned, RTL direction
        node("settingsDoctorSpecialtyAr").performScrollTo().assertIsDisplayed()
        val layoutSpecialtyAr = textLayoutOfTag("settingsDoctorSpecialtyAr")
        assertEquals("Arabic specialty textAlign", TextAlign.Right, layoutSpecialtyAr.layoutInput.style.textAlign)
        assertEquals("Arabic specialty textDirection", TextDirection.Rtl, layoutSpecialtyAr.layoutInput.style.textDirection)
        assertEquals("Arabic specialty paragraph direction", ResolvedTextDirection.Rtl, layoutSpecialtyAr.getParagraphDirection(0))

        // Contrast with LTR phone field
        node("settingsClinicPhone").performScrollTo().assertIsDisplayed()
        val layoutPhone = textLayoutOfTag("settingsClinicPhone")
        assertEquals("Clinic phone textDirection", TextDirection.Ltr, layoutPhone.layoutInput.style.textDirection)
    }

    @Test
    fun brandingSection_savingNewNameAndPhone_updatesRepositoryAndReportDialog() {
        val repo = createIsolatedRepo()
        showSecurityDialog(repo)

        val newDoctorName = "Dr. Katherine Pulaski"
        val newSpecialtyEn = "Chief Medical Officer"
        val newDoctorNameAr = "د. كاثرين بولاسكي"
        val newSpecialtyAr = "كبير الأطباء ورئيس قسم النساء والتوليد"
        val newClinicPhone = "01099887766"

        // Type new values into the branding fields
        node("settingsDoctorNameEn").performScrollTo().performTextReplacement(newDoctorName)
        node("settingsDoctorSpecialtyEn").performScrollTo().performTextReplacement(newSpecialtyEn)
        node("settingsDoctorNameAr").performScrollTo().performTextReplacement(newDoctorNameAr)
        node("settingsDoctorSpecialtyAr").performScrollTo().performTextReplacement(newSpecialtyAr)
        node("settingsClinicPhone").performScrollTo().performTextReplacement(newClinicPhone)

        // Press saveBrandingButton
        node("saveBrandingButton").performScrollTo().performClick()
        compose.waitForIdle()

        // Assert repository state is updated
        val updatedState = repo.settings.value
        assertEquals(newDoctorName, updatedState.doctorNameEn)
        assertEquals(newSpecialtyEn, updatedState.doctorSpecialtyEn)
        assertEquals(newDoctorNameAr, updatedState.doctorNameAr)
        assertEquals(newSpecialtyAr, updatedState.doctorSpecialtyAr)
        assertEquals(newClinicPhone, updatedState.clinicPhone)

        // Now mount the native report dialog with the updated settings state
        showReportDialog(basePatient, updatedState)

        // Switch to Page 2 where ReportAttestationCard is rendered
        node("reportPage2Chip").performClick()
        compose.waitForIdle()

        // Verify ReportAttestationCard renders the custom doctor name, title, and phone
        compose.onNodeWithText(newDoctorName, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(newSpecialtyEn, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(newDoctorNameAr, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(newSpecialtyAr, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(newClinicPhone, substring = true, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()

        // Default attestation name must no longer be rendered
        compose.onNodeWithText(SecuritySettingsState.DEFAULT_DOCTOR_NAME_EN, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
