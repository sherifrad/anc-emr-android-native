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
import androidx.compose.ui.test.assertIsDisplayed
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
}
