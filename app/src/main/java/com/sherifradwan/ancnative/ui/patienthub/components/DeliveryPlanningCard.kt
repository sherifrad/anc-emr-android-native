package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.parseDeliveryNotes
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LabeledValue
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.SectionTextAction

@Composable
fun DeliveryPlanningCard(
    patient: DemoPatient,
    canEdit: Boolean,
    modifier: Modifier = Modifier,
    onEditDeliveryPlan: () -> Unit,
) {
    val sections = remember(patient.obstetricNotes) { parseDeliveryNotes(patient.obstetricNotes) }
    RecordSectionCard(
        title = stringResource(R.string.delivery_planning),
        family = FieldFamilies.delivery(),
        modifier = modifier.testTag("deliveryPlanningCard"),
        action = if (canEdit) {
            {
                SectionTextAction(
                    text = stringResource(R.string.action_edit),
                    contentDescription = stringResource(R.string.edit_delivery_plan),
                    onClick = onEditDeliveryPlan,
                    modifier = Modifier.testTag("editDeliveryPlan"),
                )
            }
        } else null,
    ) {
        if (patient.delivered) DeliveredSummary(patient)
        LabeledValue(
            label = stringResource(R.string.delivery_hospital_label),
            value = sections.hospital,
            modifier = Modifier.testTag("deliveryHospital"),
        )
        LabeledValue(
            label = stringResource(R.string.delivery_fees_label),
            value = sections.fees,
            modifier = Modifier.testTag("deliveryFees"),
        )
        LabeledValue(
            label = stringResource(R.string.delivery_plan_label),
            value = sections.plan,
            modifier = Modifier.testTag("deliveryPlan"),
        )
        LabeledValue(
            label = stringResource(R.string.delivery_postpartum_label),
            value = sections.postpartum,
            modifier = Modifier.testTag("deliveryPostpartum"),
        )
    }
}
