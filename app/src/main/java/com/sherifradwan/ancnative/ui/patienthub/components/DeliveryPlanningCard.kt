package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.ui.DeliverySectionBox
import com.sherifradwan.ancnative.ui.DeliveryTone
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.model.parseDeliveryNotes

@Composable
fun DeliveryPlanningCard(
    patient: DemoPatient,
    canEdit: Boolean,
    modifier: Modifier = Modifier,
    onEditDeliveryPlan: () -> Unit,
) {
    val sections = remember(patient.obstetricNotes) { parseDeliveryNotes(patient.obstetricNotes) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("deliveryPlanningCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.delivery_planning),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (canEdit) {
                    IconButton(
                        onClick = onEditDeliveryPlan,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("editDeliveryPlan"),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_delivery_plan),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (patient.delivered) {
                DeliveredSummary(patient)
            }

            val defaultPlaceholder = stringResource(R.string.no_record)

            DeliverySectionBox(
                title = stringResource(R.string.delivery_hospital_title),
                value = sections.hospital,
                tone = DeliveryTone.HOSPITAL,
                placeholder = defaultPlaceholder,
                modifier = Modifier.testTag("deliveryHospital"),
            )
            DeliverySectionBox(
                title = stringResource(R.string.delivery_fees_title),
                value = sections.fees,
                tone = DeliveryTone.FEE,
                placeholder = defaultPlaceholder,
                modifier = Modifier.testTag("deliveryFees"),
            )
            DeliverySectionBox(
                title = stringResource(R.string.delivery_plan_title),
                value = sections.plan,
                tone = DeliveryTone.PLAN,
                placeholder = defaultPlaceholder,
                modifier = Modifier.testTag("deliveryPlan"),
            )
            DeliverySectionBox(
                title = stringResource(R.string.delivery_postpartum_title),
                value = sections.postpartum,
                tone = DeliveryTone.POSTPARTUM,
                placeholder = defaultPlaceholder,
                modifier = Modifier.testTag("deliveryPostpartum"),
            )
        }
    }
}
