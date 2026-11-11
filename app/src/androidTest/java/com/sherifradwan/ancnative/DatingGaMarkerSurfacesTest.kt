package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ClinicalPatientCard
import com.sherifradwan.ancnative.ui.PatientCardVariant
import com.sherifradwan.ancnative.ui.patientdirectory.PatientDirectoryCard
import com.sherifradwan.ancnative.ui.projectDashboardRow
import com.sherifradwan.ancnative.ui.patienthub.PatientHubHeader
import com.sherifradwan.ancnative.ui.patienthub.PatientHubHeaderState
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import com.sherifradwan.ancnative.ui.patienthub.tabs.ScansTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.VisitsTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The red "??" after the current gestational age appears on the header card and the patient list row when the
 * dating is discordant, and never on the historical GA badges of visits and scans (U4, U12).
 * Dates are relative to now so the current GA is always inside the engine's 0 to 320 day window.
 */
@RunWith(AndroidJUnit4::class)
class DatingGaMarkerSurfacesTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val markerTag = "gaDiscordanceMarker"
    private val today: LocalDate = LocalDate.now()

    /** LMP 300 d ago: current GA 42w6d, post-term, so the list row also shows the "Post" chip beside the marker. */
    private val postTermLmp: LocalDate = today.minusDays(300)

    /** LMP 150 d ago, mid-pregnancy, for the surfaces that do not need post-term. */
    private val midLmp: LocalDate = today.minusDays(150)

    private val base: DemoPatient = DemoFixtures.patients.first()

    private fun scanOn(date: LocalDate) = DemoUltrasoundScan(
        id = "anchor-$date", patientId = base.id, date = date.toString(),
        gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
        presentation = null, placentalSite = null, amnioticFluid = null,
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = null, impression = null,
    )

    /** The 25 d gap between the LMP and scan due dates exceeds every tier limit (5 to 21 d) at any anchor. */
    private fun patient(lmp: LocalDate, discordant: Boolean): DemoPatient {
        val lmpEdd = lmp.plusDays(280)
        return base.copy(
            dating = DemoPatientDating(
                patientId = base.id, lmpDate = lmp.toString(),
                ultrasoundEdd = if (discordant) lmpEdd.plusDays(25).toString() else null,
                agreedEdd = null, datingMethod = "LMP", notes = null,
            ),
            ultrasoundScans = listOf(scanOn(lmp.plusDays(80))),
            visits = listOf(
                DemoVisit(
                    id = "v-ga", date = lmp.plusDays(100).toString(),
                    bloodPressure = "110/70", weight = "70", findings = null, notes = null,
                ),
            ),
        )
    }

    private fun show(widthDp: Int? = null, fontScale: Float? = null, content: @Composable () -> Unit) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    CompositionLocalProvider(LocalDensity provides scaled) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun markerCount() = compose.onAllNodes(hasTestTag(markerTag), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun badgeCount() =
        compose.onAllNodes(hasTestTag("gestationalAgeBadge"), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun headerCardShowsMarkerOnlyWhenDiscordant() {
        for (variant in listOf(PatientCardVariant.BEDSIDE_TOP_BAR, PatientCardVariant.EDITOR_SUMMARY)) {
            show { ClinicalPatientCard(patient(midLmp, discordant = true), variant, asOfDate = today) }
            assertEquals("$variant discordant", 1, markerCount())
            show { ClinicalPatientCard(patient(midLmp, discordant = false), variant, asOfDate = today) }
            assertEquals("$variant concordant", 0, markerCount())
            assertEquals("$variant keeps the GA badge", 1, badgeCount())
        }
    }

    @Test
    fun visitGaBadgesNeverShowTheMarkerEvenForADiscordantPatient() {
        show { VisitsTab(patient(midLmp, discordant = true), canEdit = true, onStartVisit = { _, _ -> }, onEvent = {}) }
        assertTrue("the visit must render a GA badge for this test to mean anything", badgeCount() >= 1)
        assertEquals(0, markerCount())
        compose.onAllNodes(hasText("??", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun scanGaBadgesNeverShowTheMarkerEvenForADiscordantPatient() {
        show { ScansTab(patient(midLmp, discordant = true), canEdit = true, onEvent = {}, onViewPhotos = { _, _ -> }) }
        assertTrue("the scan must render a GA badge for this test to mean anything", badgeCount() >= 1)
        assertEquals(0, markerCount())
        compose.onAllNodes(hasText("??", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    private fun showListCard(p: DemoPatient, widthDp: Int? = null, fontScale: Float? = null) =
        show(widthDp, fontScale) {
            Box(Modifier.padding(horizontal = 8.dp)) {
                PatientDirectoryCard(patient = p, row = projectDashboardRow(p), onEvent = {}, onOpenMenu = {})
            }
        }

    @Test
    fun listRowShowsMarkerOnlyWhenDiscordant() {
        showListCard(patient(midLmp, discordant = true))
        assertEquals(1, markerCount())
        showListCard(patient(midLmp, discordant = false))
        assertEquals(0, markerCount())
    }

    // U12: post-term and discordant is the widest first column: number, "??" and the "Post" chip share one row.
    @Test
    fun listRowMarkerIsNotClippedForAPostTermPatientAt360dpFontScale13() {
        showListCard(patient(postTermLmp, discordant = true), widthDp = 360, fontScale = 1.3f)
        compose.onNode(hasTestTag(markerTag), useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.assertNotEllipsized("??", markerTag)

        val marker = compose.onNode(hasTestTag(markerTag), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val parityLabel = compose.onNode(hasText("Parity"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val post = compose.onNode(hasText("Post"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(
            "marker must stay inside the GA column (right=${marker.right}, parity column starts at ${parityLabel.left})",
            marker.right <= parityLabel.left,
        )
        assertTrue("the Post chip must stay inside the GA column", post.right <= parityLabel.left)
        assertTrue("marker and Post chip must not overlap", marker.right <= post.left || post.right <= marker.left)
        assertTrue("marker must stay inside the 360dp viewport", marker.right <= 360.dp)
    }

    private fun showHeader(p: DemoPatient) = show {
        PatientHubHeader(
            patient = p, headerState = PatientHubHeaderState(), selectedTab = PatientHubTab.entries.first(),
            onTabSelected = {}, severeBp = null, onBack = {}, onEditPatient = {}, onOpenReport = {},
            onOpenAttentionDialog = {},
        )
    }

    // The expanded header only. The sticky bar needs the measured expanded height, so it is not driven here.
    @Test
    fun expandedPatientHubHeaderShowsMarkerOnlyWhenDiscordant() {
        showHeader(patient(midLmp, discordant = true))
        assertEquals(1, badgeCount())
        assertEquals(1, markerCount())
        showHeader(patient(midLmp, discordant = false))
        assertEquals(1, badgeCount())
        assertEquals(0, markerCount())
    }

    /** LMP 400 d ago: the current GA is past the engine's 320 d window, so there is no GA to flag. */
    private val outOfWindowLmp: LocalDate = today.minusDays(400)

    @Test
    fun discordantPatientWithGaOutsideTheWindowShowsNoMarkerInTheListRow() {
        val p = patient(outOfWindowLmp, discordant = true)
        assertEquals("-", projectDashboardRow(p).gestationalAgeText)
        assertTrue(projectDashboardRow(p).isDatingDiscordant)
        showListCard(p)
        assertEquals(0, markerCount())
        compose.onAllNodes(hasText("??", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun discordantPatientWithGaOutsideTheWindowShowsNoMarkerOnTheDeliveryPlanCard() {
        show { DeliveryPlanningCard(patient = patient(outOfWindowLmp, discordant = true), canEdit = true, onEditDeliveryPlan = {}) }
        compose.onNodeWithTag("deliveryPlanningCard", useUnmergedTree = true).assertExists()
        assertEquals(0, markerCount())
        compose.onAllNodes(hasText("??", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun discordantPatientWithGaOutsideTheWindowShowsNoMarkerInTheHeader() {
        showHeader(patient(outOfWindowLmp, discordant = true))
        assertEquals(0, markerCount())
    }
}
