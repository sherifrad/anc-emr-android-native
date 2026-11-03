package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.NORMAL_AMNIOTIC_FLUID
import com.sherifradwan.ancnative.model.NORMAL_FETAL_NUMBER
import com.sherifradwan.ancnative.model.NORMAL_PRESENTATION
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.UltrasoundDraftField
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.UltrasoundEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Oracles T200-T205 of SPEC_DESIGN_R5.md (Round 5a), written from the spec only. */
@RunWith(AndroidJUnit4::class)
class UltrasoundGuidedFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private var state by mutableStateOf(AncState(patients = emptyList()))

    /** Opens a new scan with the real reducer; events from the screen go through the real reducer. */
    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 5001L),
        )
        s = AncReducer.reduce(
            s,
            AncEvent.StartUltrasoundEditor(
                patientId = patient.id,
                scanId = null,
                newScanId = "guided-scan-1",
                editorSessionId = "guided-session-1",
            ),
        )
        state = setup(s)
        val route = state.route as AncRoute.UltrasoundEditor
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    UltrasoundEditor(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun change(s: AncState, field: UltrasoundDraftField, value: String): AncState =
        AncReducer.reduce(s, AncEvent.UltrasoundDraftChanged(field, value))

    /** ChoiceGroup tags are "$tagPrefix-$index", index 0 being the "Unknown" chip. */
    private fun choose(tagPrefix: String, choices: Collection<String>, value: String) {
        val index = choices.toList().indexOf(value) + 1
        assertTrue("unknown vocabulary value $value", index > 0)
        compose.onNodeWithTag("$tagPrefix-$index", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun assertExists(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    }

    private fun assertMissing(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun assertRowShows(key: String, value: String) {
        compose.onNodeWithTag("findingRow-$key").performScrollTo().assertTextContains(value, substring = true)
    }

    @Test
    fun t200_emptyDraftOpensFetalNumberAndSummarisesTheOtherThree() {
        host()
        assertExists("findingOpen-fetalNumber")
        assertMissing("findingRow-fetalNumber")
        listOf("presentation", "placenta", "liquor").forEach { key ->
            assertExists("findingRow-$key")
            assertMissing("findingOpen-$key")
        }
    }

    @Test
    fun t201_choosingFetalNumberOpensPresentationAndShowsTheValue() {
        host()
        choose("fetalNumberChoice", ClinicalVocabulary.fetalNumbers, "Singleton")
        assertEquals("Singleton", state.ultrasoundDraft?.values?.fetalNumber)
        assertExists("findingOpen-presentation")
        assertMissing("findingOpen-fetalNumber")
        assertExists("findingRow-fetalNumber")
        assertRowShows("fetalNumber", "Singleton")
    }

    @Test
    fun t202_fillNormalScanOpensPlacentaAndFillsTheOtherThreeRows() {
        host()
        compose.onNodeWithTag("fillNormalScan").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(NORMAL_FETAL_NUMBER, state.ultrasoundDraft?.values?.fetalNumber)
        assertEquals("", state.ultrasoundDraft?.values?.placentalSite)
        assertExists("findingOpen-placenta")
        assertRowShows("fetalNumber", NORMAL_FETAL_NUMBER)
        assertRowShows("presentation", NORMAL_PRESENTATION)
        assertRowShows("liquor", NORMAL_AMNIOTIC_FLUID)
        assertEquals("Singleton", NORMAL_FETAL_NUMBER)
        assertEquals("Cephalic", NORMAL_PRESENTATION)
        assertEquals("Normal", NORMAL_AMNIOTIC_FLUID)
    }

    @Test
    fun t203_tappingAClosedRowOpensItAndClosesPlacenta() {
        host()
        compose.onNodeWithTag("fillNormalScan").performScrollTo().performClick()
        compose.waitForIdle()
        assertExists("findingOpen-placenta")
        compose.onNodeWithTag("findingRow-fetalNumber").performScrollTo().performClick()
        compose.waitForIdle()
        assertExists("findingOpen-fetalNumber")
        assertMissing("findingOpen-placenta")
        assertExists("findingRow-placenta")
    }

    @Test
    fun t204a_moreSectionIsOpenOnEntryWhenAVisitIsLinked() {
        val visitId = patient.visits.first().id
        host { change(it, UltrasoundDraftField.LINKED_VISIT, visitId) }
        assertExists("ultrasoundMoreSection")
    }

    @Test
    fun t204b_moreSectionIsOpenOnEntryWhenAPhotoIsAttached() {
        host { change(it, UltrasoundDraftField.IMAGE_URIS, "content://demo.scan/photo-1.jpg") }
        assertExists("ultrasoundMoreSection")
    }

    @Test
    fun t204c_moreSectionIsClosedWithoutVisitOrPhotoAndToggleShowsIt() {
        host()
        assertMissing("ultrasoundMoreSection")
        compose.onNodeWithTag("ultrasoundMoreToggle").performScrollTo().performClick()
        compose.waitForIdle()
        assertExists("ultrasoundMoreSection")
    }

    @Test
    fun t205_reportCardFollowsTheChoices() {
        host()
        choose("fetalNumberChoice", ClinicalVocabulary.fetalNumbers, "Singleton")
        choose("presentationChoice", ClinicalVocabulary.fetalPresentations, "Cephalic")
        compose.onNodeWithTag("ultrasoundLiveSummary").performScrollTo()
        compose.onNode(hasText("Singleton pregnancy", substring = true) and hasAnyAncestor(hasTestTag("ultrasoundLiveSummary")), useUnmergedTree = true)
            .assertExists()
        compose.onNode(hasText("cephalic presentation", substring = true, ignoreCase = true) and hasAnyAncestor(hasTestTag("ultrasoundLiveSummary")), useUnmergedTree = true)
            .assertExists()
    }
}
