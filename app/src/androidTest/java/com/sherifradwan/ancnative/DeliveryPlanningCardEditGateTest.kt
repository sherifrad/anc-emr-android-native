package com.sherifradwan.ancnative

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ORACLE-A6: the editDeliveryPlan control exists only when canEdit is true. */
@RunWith(AndroidJUnit4::class)
class DeliveryPlanningCardEditGateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun canEditFalse_hidesEditDeliveryPlan() {
        val patient = DemoFixtures.patients.first()
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = false)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("editDeliveryPlan").assertDoesNotExist()
    }

    @Test
    fun canEditTrue_showsEditDeliveryPlanAndInvokesCallback() {
        val patient = DemoFixtures.patients.first()
        var clicks = 0
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = { clicks++ }, canEdit = true)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("editDeliveryPlan").assertIsDisplayed()
        compose.onNodeWithTag("editDeliveryPlan").performClick()
        assertEquals(1, clicks)
    }
}
