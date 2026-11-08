package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
fun PreviousPregnancyCard(
    pregnancy: DemoPreviousPregnancy,
    canEdit: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pregnancy-${pregnancy.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val headerTitle = pregnancy.outcomeText ?: pregnancy.outcome ?: pregnancy.pregnancyType ?: "Previous Pregnancy"
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!pregnancy.dateText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = pregnancy.dateText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

            FactRow(R.string.entered_date_text, pregnancy.dateText)
            FactRow(R.string.entered_outcome_text, pregnancy.outcomeText)
            FactRow(R.string.pregnancy_type, pregnancy.pregnancyType)
            FactRow(R.string.structured_outcome, pregnancy.outcome)
            FactRow(R.string.historical_delivery_mode, pregnancy.deliveryBy)
            FactRow(R.string.other_details, pregnancy.otherDetails)
            FactRow(R.string.place_and_doctor, pregnancy.placeAndDoctor)
            FactRow(R.string.birth_method, pregnancy.birthMethod)
            FactRow(R.string.loss_management, pregnancy.lossManagement)
            FactRow(R.string.termination_method, pregnancy.terminationMethod)
            FactRow(R.string.clinician_id, pregnancy.clinicianId)
            if (canEdit) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, onDelete)
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("editPregnancy-${pregnancy.id}"),
                    ) {
                        Text(stringResource(R.string.correct_previous_pregnancy))
                    }
                }
            }
        }
    }
}
