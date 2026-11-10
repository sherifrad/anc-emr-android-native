package com.sherifradwan.ancnative

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationStopUndo
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationStopBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The one-tap stop's Undo bar keeps the window counted from the commit, on an injected clock and duration. */
@RunWith(AndroidJUnit4::class)
class MedicationStopBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<AncEvent>()
    private var now = 0L
    private var shown by mutableStateOf(true)

    private fun undo(committedAt: Long) = MedicationStopUndo(
        token = "t1", patientId = "p1", medicationId = "m1", medicationName = "Iron",
        previousStatus = "Active", previousEndDate = "", previousNonCompliant = false, committedAtMillis = committedAt,
    )

    private fun mount(committedAt: Long, windowMillis: Long) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AncTheme {
                if (shown) {
                    MedicationStopBar(
                        quickSave = null,
                        stopUndo = undo(committedAt),
                        savePhase = SavePhase.IDLE,
                        saveError = null,
                        undoEnabled = true,
                        onEvent = { events += it },
                        undoWindowMillis = windowMillis,
                        clock = { now },
                    )
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun advance(millis: Long) {
        now += millis
        compose.mainClock.advanceTimeBy(millis)
    }

    private val expired get() = events.filterIsInstance<AncEvent.MedicationStopUndoExpired>()

    @Test
    fun theBarExpiresAfterTheInjectedWindow() {
        mount(committedAt = 0L, windowMillis = 2_000L)
        compose.onNodeWithTag("medStopUndoBar", useUnmergedTree = true).assertIsDisplayed()
        advance(1_800L)
        assertTrue("not yet", expired.isEmpty())
        advance(400L)
        assertEquals(listOf(AncEvent.MedicationStopUndoExpired("t1")), expired)
    }

    @Test
    fun showingTheBarAgainDoesNotStartAFreshWindow() {
        mount(committedAt = 0L, windowMillis = 10_000L)
        advance(6_000L)
        // Leaving the tab (or rotating) removes the bar; coming back shows it again with the same commit time.
        shown = false
        compose.mainClock.advanceTimeByFrame()
        shown = true
        compose.mainClock.advanceTimeByFrame()
        advance(3_800L)
        assertTrue("4 s were left, not 10", expired.isEmpty())
        advance(400L)
        assertEquals(1, expired.size)
    }

    @Test
    fun aBarShownAfterItsWindowEndedGoesAtOnce() {
        now = 20_000L
        mount(committedAt = 0L, windowMillis = 10_000L)
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, expired.size)
    }
}
