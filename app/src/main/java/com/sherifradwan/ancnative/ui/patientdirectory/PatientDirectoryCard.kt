package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.editPatientEvent
import com.sherifradwan.ancnative.ui.BpAlertChip
import com.sherifradwan.ancnative.ui.PatientDashboardRowProjection
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import java.util.UUID

/**
 * Standardized High-Density Patient Directory Card.
 * Adheres to 360dp narrow viewport first and the Rule of Reusability.
 */
@Composable
fun PatientDirectoryCard(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    onEvent: (AncEvent) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("patient-${patient.caseId}")
            .testTag("workspace-${patient.caseId}")
            .testTag("profile-${patient.caseId}")
            .clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) },
        shape = RecordShapes.Large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val isArabic = row.displayName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
            val hasTags = row.isHighRisk ||
                row.riskTier == PatientRiskTier.MEDIUM ||
                row.isSevereBp ||
                row.isIncomplete ||
                row.isDelivered

            // Name row: 48dp min touch target height, vertically centered
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = row.displayName,
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )

                if (hasTags) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (row.isHighRisk) {
                            StatusTag(StatusKind.HighRisk)
                        } else if (row.riskTier == PatientRiskTier.MEDIUM) {
                            StatusTag(StatusKind.ModerateRisk)
                        }

                        if (row.isSevereBp) {
                            BpAlertChip(R.string.bp_badge_severe, isSevere = true)
                        }

                        if (row.isIncomplete) {
                            Box(
                                modifier = Modifier
                                    .minimumTouchTarget()
                                    .clickable(
                                        onClickLabel = stringResource(R.string.registration_incomplete_chip_action),
                                    ) {
                                        editPatientEvent(
                                            patient,
                                            UUID.randomUUID().toString(),
                                            UUID.randomUUID().toString(),
                                        )?.let(onEvent)
                                    }
                                    .testTag("incompleteChipAction"),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                StatusTag(
                                    kind = StatusKind.Incomplete,
                                    modifier = Modifier.testTag("incompleteChip"),
                                )
                            }
                        }

                        if (row.isDelivered) {
                            StatusTag(
                                kind = StatusKind.Delivered,
                                modifier = Modifier.testTag("deliveredTag"),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("patientMenu-${patient.caseId}"),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            PatientDirectoryMetricsRow(
                patient = patient,
                row = row,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
