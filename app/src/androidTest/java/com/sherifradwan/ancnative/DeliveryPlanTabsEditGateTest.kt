package com.sherifradwan.ancnative

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.OverviewTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.PlanTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ORACLE-A7: PlanTab and OverviewTab pass canEdit through to the Delivery Plan card. */
@RunWith(AndroidJUnit4::class)
class DeliveryPlanTabsEditGateTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(plan: Boolean, canEdit: Boolean) {
        val patient = DemoFixtures.patients.first()
        compose.setContent {
            AncTheme {
                if (plan) {
                    PlanTab(patient = patient, canEdit = canEdit, onEvent = {}, onAddAttachment = {}, onViewAttachment = {})
                } else {
                    OverviewTab(patient = patient, canEdit = canEdit, onEvent = {}, onAddAttachment = {}, onViewAttachment = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun planTab_readOnly_hasNoEditDeliveryPlan() {
        show(plan = true, canEdit = false)
        compose.onNodeWithTag("editDeliveryPlan").assertDoesNotExist()
    }

    @Test fun planTab_editable_hasEditDeliveryPlan() {
        show(plan = true, canEdit = true)
        compose.onNodeWithTag("editDeliveryPlan").assertExists()
    }

    @Test fun overviewTab_readOnly_hasNoEditDeliveryPlan() {
        show(plan = false, canEdit = false)
        compose.onNodeWithTag("editDeliveryPlan").assertDoesNotExist()
    }

    @Test fun overviewTab_editable_hasEditDeliveryPlan() {
        show(plan = false, canEdit = true)
        compose.onNodeWithTag("editDeliveryPlan").assertExists()
    }
}
