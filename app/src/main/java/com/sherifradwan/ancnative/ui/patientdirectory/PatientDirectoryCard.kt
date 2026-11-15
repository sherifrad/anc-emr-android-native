package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.editPatientEvent
import com.sherifradwan.ancnative.ui.BpAlertChip
import com.sherifradwan.ancnative.ui.PatientDashboardRowProjection
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PatientNameText(
                    name = row.displayName,
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )

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

            PatientTagRow(
                patient = patient,
                row = row,
                onEvent = onEvent,
                modifier = Modifier.padding(end = RecordSpacing.S),
            )

            PatientDirectoryMetricsRow(
                patient = patient,
                row = row,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

/**
 * Status tags under the name on three fixed slots (risk, Incomplete, Delivered). The slots use the metric rows' gap
 * and end padding, so a tag type sits at the same x on every card. Not composed when there is no tag; otherwise at
 * least 48dp tall so the Incomplete target fits and every tagged card lines up.
 */
@Composable
private fun PatientTagRow(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTags = row.isHighRisk ||
        row.riskTier == PatientRiskTier.MEDIUM ||
        row.isSevereBp ||
        row.isIncomplete ||
        row.isDelivered
    if (!hasTags) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
    ) {
        TagSlot {
            Column(verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS)) {
                if (row.isHighRisk) {
                    StatusTag(StatusKind.HighRisk, wrap = true, horizontalPadding = CardTagPadding)
                } else if (row.riskTier == PatientRiskTier.MEDIUM) {
                    StatusTag(StatusKind.ModerateRisk, wrap = true, horizontalPadding = CardTagPadding)
                }
                if (row.isSevereBp) {
                    BpAlertChip(R.string.bp_badge_severe, isSevere = true)
                }
            }
        }
        TagSlot {
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
                    contentAlignment = Alignment.CenterStart,
                ) {
                    StatusTag(
                        kind = StatusKind.Incomplete,
                        wrap = true,
                        horizontalPadding = CardTagPadding,
                        modifier = Modifier.testTag("incompleteChip"),
                    )
                }
            }
        }
        TagSlot {
            if (row.isDelivered) {
                StatusTag(
                    kind = StatusKind.Delivered,
                    wrap = true,
                    horizontalPadding = CardTagPadding,
                    modifier = Modifier.testTag("deliveredTag"),
                )
            }
        }
    }
}

@Composable
private fun RowScope.TagSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

private val CardTagPadding = 6.dp
