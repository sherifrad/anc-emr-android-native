package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.ui.patienthub.components.AttachmentsSectionCard
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import java.util.UUID

@Composable
fun PlanTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onAddAttachment: () -> Unit,
    onViewAttachment: (DemoAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
            .testTag("planTabContent"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Full Delivery Planning Card
        DeliveryPlanningCard(
            patient = patient,
            canEdit = canEdit,
            onEditDeliveryPlan = {
                onEvent(AncEvent.StartDeliveryPlanEditor(patient.id, UUID.randomUUID().toString()))
            },
        )

        // Full Documents & Attachments Section
        AttachmentsSectionCard(
            patient = patient,
            onAddAttachment = onAddAttachment,
            onViewAttachment = onViewAttachment,
        )
    }
}
