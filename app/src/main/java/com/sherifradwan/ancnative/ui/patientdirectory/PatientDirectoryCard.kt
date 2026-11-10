package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.ui.BpAlertChip
import com.sherifradwan.ancnative.ui.ClinicalRiskBadge
import com.sherifradwan.ancnative.ui.ClinicalTagBadge
import com.sherifradwan.ancnative.ui.PatientDashboardRowProjection
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.ActionRose
import com.sherifradwan.ancnative.ui.ClinicalCardTokens

/**
 * Standardized High-Density Patient Directory Card.
 * Adheres to 360dp narrow viewport first and the Rule of Reusability.
 * Replaces bloated legacy cards (~220dp) with a clean, un-truncated ~108-112dp resting height.
 */
@Composable
fun PatientDirectoryCard(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    onEvent: (AncEvent) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArabic = row.displayName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }

    // Ambient 3.5dp leading severity accent color
    val accentColor: Color = when {
        row.isSevereBp || row.isHighRisk -> ActionRose
        row.riskTier == PatientRiskTier.MEDIUM -> ClinicalCardTokens.WarningAccent
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    val cardBgColor = if (row.isHighRisk) {
        lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.errorContainer,
            0.04f,
        )
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .testTag("patient-${patient.caseId}")
            .testTag("workspace-${patient.caseId}")
            .testTag("profile-${patient.caseId}")
            .clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Ambient 3.5dp leading-edge severity indicator strip
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Header: leading Risk badge (High / Moderate) + Patient Name + Severe BP chip + MoreVert Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (row.isHighRisk) {
                        ClinicalRiskBadge(isHighRisk = true)
                        Spacer(Modifier.width(6.dp))
                    } else if (row.riskTier == PatientRiskTier.MEDIUM) {
                        ClinicalRiskBadge(isHighRisk = false, isModerate = true)
                        Spacer(Modifier.width(6.dp))
                    }

                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (row.isSevereBp) {
                            BpAlertChip(R.string.bp_badge_severe, isSevere = true)
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
                }

                if (row.isIncomplete) {
                    ClinicalTagBadge(
                        text = stringResource(R.string.registration_incomplete_chip),
                        isError = false,
                        isWarning = true,
                        modifier = Modifier.testTag("incompleteChip"),
                    )
                }

                // Body: Structured 3-Column Clinical Micro-Grid (GA & EDD, Parity, Vitals)
                PatientDirectoryMetricsRow(
                    patient = patient,
                    row = row,
                )
            }
        }
    }
}
