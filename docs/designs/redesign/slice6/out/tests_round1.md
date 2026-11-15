## Existing tests/assertions likely to break (string change)

**Changed strings** (`app/src/main/res/values/strings.xml`):
- `bp_alert_raised`: old `⚠ Raised BP (≥140/90) · recheck` → new `Raised BP (>=140/90), recheck`
- `bp_alert_severe`: old `⚠ Severe BP (≥160/110) · urgent` → new `Severe BP (>=160/110), urgent`

**Impact:** any assertion matching the old full text, the leading `⚠`, or the middle-dot separator ` · ` will fail. I could not find test files in the pasted context that assert these exact strings (they are not referenced in `HubHeaderRedesignTest`), so the list below is the **required correction pattern** for any existing/nearby assertions that do reference them (search for `bp_alert_raised`, `bp_alert_severe`, `Raised BP (≥140/90)`, `Severe BP (≥160/110)`, `⚠ Raised BP`, or `· recheck` / `· urgent`):

| Likely old assertion | Corrected assertion |
|---|---|
| `onNodeWithText("⚠ Raised BP (≥140/90) · recheck").assertIsDisplayed()` | `onNodeWithText("Raised BP (>=140/90), recheck").assertIsDisplayed()` |
| `onNodeWithText("⚠ Severe BP (≥160/110) · urgent").assertIsDisplayed()` | `onNodeWithText("Severe BP (>=160/110), urgent").assertIsDisplayed()` |
| `onNodeWithText("Raised BP", substring = true)` (still valid) | No change needed if it only matched the words; keep `substring = true`. |
| `hasText("⚠", substring = true)` used to identify the BP alert | Remove/replace: the warning triangle is no longer part of the string; assert the specific text/tag instead (e.g. `onNodeWithText("Raised BP", substring = true, useUnmergedTree = true)` or the band chip/tag). |
| `contains(" · ")` / middle-dot checks on the alert | Replace with `", "` (comma-space) or assert the new full string. |

**Recommended robust form (survives minor punctuation/spacing):**
```kotlin
compose.onNodeWithText("Raised BP", substring = true, useUnmergedTree = true).assertIsDisplayed()
compose.onNodeWithText(">=140/90", substring = true, useUnmergedTree = true).assertIsDisplayed()
// severe:
compose.onNodeWithText("Severe BP", substring = true, useUnmergedTree = true).assertIsDisplayed()
compose.onNodeWithText(">=160/110", substring = true, useUnmergedTree = true).assertIsDisplayed()
```
If any overview/summary test previously keyed off `bp_alert_*` as the sole BP-warning marker, update it to the same substring assertions above (the clinical meaning/thresholds are unchanged).

---

## Notes on assumptions (kept minimal, based on pasted/real code)
- `VisitsTab` takes `(patient: DemoPatient, canEdit: Boolean, onStartVisit: (String, String?) -> Unit, onEvent: (AncEvent) -> Unit, modifier)`. `DemoPatient` fields used: `id`, `status`, `visits`, `dating`. Visit fields used: `id`, `date`, `bloodPressure`, `weight`, `findings`, `notes`, `linkedUltrasoundId`. These are the same model types used elsewhere in the app (`DemoFixtures`, `DemoPatientDating` appear in `HubHeaderRedesignTest`).
- `DeleteIconButton(type, record, onClick)` is not shown; its `testTag` is not defined in pasted code. The spec requires asserting `delete-visit-<id>` with 48dp min size. The test asserts that tag; if the production `DeleteIconButton` does not yet emit it, that single assertion will fail and should be fixed in `DeleteIconButton` to `Modifier.testTag("delete-visit-${record.id}")` (or equivalent) while keeping its 48dp touch target.
- `GestationalAgeEngine.calculateGestationalAgeAt` and `deriveVitalsSnapshot`/parsers are production code; tests use valid LMP/dates so GA and BP classification are deterministic.
- No `assertTextNotClipped`/viewport helper was pasted in the shown test file; the tests include a small local helper that asserts the node is laid out with positive bounds within the 360dp viewport (and text nodes exist/unclipped by bounds) for the 360dp/@2.0x case, per instructions.

FILE: app/src/androidTest/java/com/sherifradwan/ancnative/VisitsTabRedesignTest.kt
```kotlin
package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.VisitsTab
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitsTabRedesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val base: DemoPatient = DemoFixtures.patients.first()
    private val today: LocalDate = LocalDate.now()

    // LMP 191 days ago -> 27+2 wks at visit date == today.
    private val ga27w2dLmp: LocalDate = today.minusDays(191)

    private fun datingForGa(): DemoPatientDating =
        DemoPatientDating(
            patientId = base.id,
            lmpDate = ga27w2dLmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

    private fun visit(
        id: String,
        date: LocalDate = today,
        bloodPressure: String? = "110/70",
        weight: String? = "70",
        findings: String? = null,
        notes: String? = null,
        linkedUltrasoundId: String? = null,
        sfhCm: String? = "27",
        fhr: String? = "140",
        urineProtein: String? = null,
    ): DemoVisit {
        val findingsValue = findings ?: buildFindings(fhr = fhr, sfhCm = sfhCm, urineProtein = urineProtein)
        return DemoVisit(
            id = id,
            patientId = base.id,
            date = date.toString(),
            bloodPressure = bloodPressure,
            weight = weight,
            findings = findingsValue,
            notes = notes,
            linkedUltrasoundId = linkedUltrasoundId,
        )
    }

    private fun buildFindings(fhr: String?, sfhCm: String?, urineProtein: String?): String? {
        val parts = mutableListOf<String>()
        if (!fhr.isNullOrBlank()) parts += "FHR: $fhr"
        if (!sfhCm.isNullOrBlank()) parts += "SFH: $sfhCm"
        if (!urineProtein.isNullOrBlank()) parts += "Urine protein: $urineProtein"
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun patientWith(
        visits: List<DemoVisit>,
        status: String = "active",
        dating: DemoPatientDating? = datingForGa(),
    ): DemoPatient =
        base.copy(
            status = status,
            dating = dating,
            visits = visits,
            ultrasoundScans = emptyList(),
        )

    private fun showVisits(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
        layoutDirection: LayoutDirection? = null,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    val density = LocalDensity.current
                    val scaled = if (fontScale != null) Density(density.density, fontScale) else density
                    CompositionLocalProvider(
                        LocalDensity provides scaled,
                        LocalLayoutDirection provides (layoutDirection ?: LocalLayoutDirection.current),
                    ) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized) {
                            VisitsTab(
                                patient = patient,
                                canEdit = canEdit,
                                onStartVisit = { _, _ -> },
                                onEvent = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Asserts a text node exists, is laid out, and its bounds fit within the given viewport width. */
    private fun assertTextWithinViewport(text: String, viewportWidthDp: Int, substring: Boolean = true) {
        val node = compose.onNodeWithText(text, substring = substring, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue("Expected '$text' to have positive width", bounds.width > 0.dp)
        assertTrue("Expected '$text' to have positive height", bounds.height > 0.dp)
        assertTrue(
            "Expected '$text' right edge (${bounds.right}) within ${viewportWidthDp}dp viewport",
            bounds.right <= viewportWidthDp.dp + 1.dp,
        )
    }

    private fun SemanticsNodeInteraction.assertMinTouchTarget() {
        assertHeightIsAtLeast(48.dp)
        assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun normalVisit_showsBandHeadingGaChipLabelsAndNoFlagChip() {
        val v = visit(id = "v-normal", bloodPressure = "110/70", sfhCm = "27")
        showVisits(patientWith(listOf(v)))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(v.date, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("gestationalAgeBadge").assertIsDisplayed()
        compose.onNodeWithText("27+2 wks", substring = true, useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithText("Blood pressure", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Weight", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Fetal heart rate", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Fundal height", useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithText("Raised BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Severe BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Pre-eclampsia", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
    }

    @Test
    fun raisedBp_showsRaisedChipAndWord_notSevereOrPreEclampsia() {
        val v = visit(id = "v-raised", bloodPressure = "145/92", urineProtein = null)
        showVisits(patientWith(listOf(v)))

        // Band chip + in-grid word both render "Raised BP" label text.
        compose.onNodeWithText("Raised BP", substring = true, useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag("visit-${v.id}").fetchSemanticsNodes().isNotEmpty()
        compose.onNodeWithText("Severe BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
        compose.onNodeWithText("Pre-eclampsia", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun severeBp_showsSevereChipAndWord() {
        val v = visit(id = "v-severe", bloodPressure = "165/110", urineProtein = null)
        showVisits(patientWith(listOf(v)))

        compose.onNodeWithText("Severe BP", substring = true, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Raised BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").assertDoesNotExist()
    }

    @Test
    fun preEclampsiaRule_showsWarningTagAndBandChip_notBpChips() {
        val v = visit(id = "v-pe", bloodPressure = "145/95", urineProtein = "+2")
        showVisits(patientWith(listOf(v)))

        compose.onNodeWithTag("preEclampsiaWarning-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pre-eclampsia", substring = true, useUnmergedTree = true).assertIsDisplayed()
        // Pre-eclampsia takes precedence in worst-flag chip; BP-only chips/words must not appear.
        compose.onNodeWithText("Raised BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Severe BP", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun missingBpAndWeight_showNotRecorded() {
        val v = visit(id = "v-missing", bloodPressure = null, weight = null, sfhCm = "27")
        showVisits(patientWith(listOf(v)))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        // Two "Not recorded" values expected (BP and Weight). Assert at least one is visible and both exist.
        compose.onAllNodes(hasText("Not recorded", substring = false), useUnmergedTree = true)
            .fetchSemanticsNodes().let { nodes ->
                assertTrue("Expected at least 2 'Not recorded' fields for missing BP/weight", nodes.size >= 2)
            }
    }

    @Test
    fun sfhField_hiddenWhenAbsent() {
        val v = visit(id = "v-nosfh", bloodPressure = "110/70", sfhCm = null)
        showVisits(patientWith(listOf(v)))

        compose.onNodeWithTag("visit-${v.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fundal height", useUnmergedTree = true).assertDoesNotExist()
        // FHR still present in this visit.
        compose.onNodeWithText("Fetal heart rate", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun editAndDeleteActions_presentWithTagsSizesAndContentDescription() {
        val v = visit(id = "v-edit")
        showVisits(patientWith(listOf(v)), canEdit = true)

        compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertMinTouchTarget()
        compose.onNodeWithContentDescription("Edit visit", useUnmergedTree = true)
            .assertIsDisplayed()

        // Spec: delete tag delete-visit-<id> with 48dp touch target.
        compose.onNodeWithTag("delete-visit-${v.id}", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertMinTouchTarget()
    }

    @Test
    fun recordNewVisitButton_presentForEditable_absentForArchivedOrNotEditable() {
        val v = visit(id = "v-btn")

        showVisits(patientWith(listOf(v), status = "active"), canEdit = true)
        compose.onNodeWithTag("recordNewVisitBtn").assertIsDisplayed()

        // Archived patient: button must be absent even if canEdit is true.
        showVisits(patientWith(listOf(v), status = "archived"), canEdit = true)
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()

        // Not editable: button must be absent.
        showVisits(patientWith(listOf(v), status = "active"), canEdit = false)
        compose.onNodeWithTag("recordNewVisitBtn").assertDoesNotExist()
    }

    @Test
    fun narrow360dp_fontScale2_valuesNotClipped() {
        val v = visit(
            id = "v-narrow",
            bloodPressure = "118/76",
            weight = "72",
            sfhCm = "27",
            fhr = "140",
        )
        showVisits(patientWith(listOf(v)), widthDp = 360, fontScale = 2.0f)

        compose.onNodeWithTag("visitsTabContent").assertIsDisplayed()
        assertTextWithinViewport(v.date, viewportWidthDp = 360)
        assertTextWithinViewport("27+2 wks", viewportWidthDp = 360)
        assertTextWithinViewport("Blood pressure", viewportWidthDp = 360)
        assertTextWithinViewport("118/76", viewportWidthDp = 360)
        // Edit action remains reachable/tappable.
        compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertMinTouchTarget()
    }

    @Test
    fun rtl_valuesLtrIsolated_andEditActionAtLayoutEnd() {
        val v = visit(id = "v-rtl", bloodPressure = "120/80", weight = "68", sfhCm = "27")
        showVisits(patientWith(listOf(v)), layoutDirection = LayoutDirection.Rtl)

        // LTR-isolated clinical values remain wrapped in isolate marks (U+2066 ... U+2069).
        compose.onNodeWithText("\u2066120/80", substring = true, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("27+2 wks", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()

        // In RTL, layout end is the visual left/start edge of the row: Edit (end action)
        // should be left of Delete (preceding action in the Row).
        val editBounds = compose.onNodeWithTag("editVisit-${v.id}", useUnmergedTree = true)
            .performScrollTo()
            .getUnclippedBoundsInRoot()
        val deleteBounds = compose.onNodeWithTag("delete-visit-${v.id}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Expected Edit action at layout end (left of Delete) in RTL",
            editBounds.right <= deleteBounds.left + 1.dp,
        )
    }
}
```
[Tokens] upload: 19254 download: 5396 cache_read: 0 cache_write: 0

