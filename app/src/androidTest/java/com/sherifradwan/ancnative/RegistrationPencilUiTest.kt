package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PENCIL_HOST_TAG = "pencilHostTag"

@RunWith(AndroidJUnit4::class)
class RegistrationPencilUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<AncEvent>()

    @Test
    fun chipTapStartsTheFlowAtIdentityAndDoesNotOpenTheWorkspace() {
        val p = DemoFixtures.patients.first().copy(ageText = null)
        show(
            AncState(
                patients = listOf(p),
                vaultState = VaultState.Unlocked(sessionEpoch = 1L),
                route = AncRoute.PatientList,
            ),
        )
        compose.onNodeWithTag("incompleteChipAction").performClick()
        val flow = events.single() as AncEvent.Flow
        assertEquals(FlowEvent.Begin(p.id, RegistrationStep.IDENTITY), flow.event)
    }

    @Test
    fun completePatientShowsNoChip() {
        val p = DemoFixtures.patients.first()
        show(
            AncState(
                patients = listOf(p),
                vaultState = VaultState.Unlocked(sessionEpoch = 1L),
                route = AncRoute.PatientList,
            ),
        )
        assertTrue(compose.onAllNodesWithTag("incompleteChip").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun pencilOnAnIncompletePatientStartsTheFlowAtTheFirstUnfinishedStep() {
        val p = DemoFixtures.patients.first().copy(dating = null)
        show(
            AncState(
                patients = listOf(p),
                vaultState = VaultState.Unlocked(sessionEpoch = 1L),
                route = AncRoute.Workspace(p.id),
            ),
        )
        compose.onNodeWithTag("editPatientButton").performClick()
        val flow = events.single() as AncEvent.Flow
        assertEquals(FlowEvent.Begin(p.id, RegistrationStep.DATING), flow.event)
    }

    @Test
    fun pencilOnACompletePatientOpensThePlainEditor() {
        val p = DemoFixtures.patients.first()
        show(
            AncState(
                patients = listOf(p),
                vaultState = VaultState.Unlocked(sessionEpoch = 1L),
                route = AncRoute.Workspace(p.id),
            ),
        )
        compose.onNodeWithTag("editPatientButton").performClick()
        val event = events.single() as AncEvent.StartPatientEditor
        assertEquals(p.id, event.patientId)
        assertEquals(p.id, event.newPatientId)
    }

    private fun show(state: AncState, fontScale: Float = 1f, darkTheme: Boolean = false) {
        events.clear()
        compose.activity.runOnUiThread {
            val base = compose.activity.resources.displayMetrics.density
            compose.activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(base, fontScale)) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds().testTag(PENCIL_HOST_TAG)) {
                        AncTheme(darkTheme = darkTheme) {
                            AncApp(
                                state = state,
                                onEvent = { events += it },
                                onStartVisit = { _, _ -> },
                                onSaveVisit = {},
                                onInitialize = {},
                                onUnlock = {},
                                onLock = {},
                                onRetryStorage = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
