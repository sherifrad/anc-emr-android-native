package com.sherifradwan.ancnative.ui.patienthub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.ui.ActionRose
import com.sherifradwan.ancnative.ui.BpAlertLabel
import com.sherifradwan.ancnative.ui.ClinicalRiskBadge
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.PatientAgeBadge
import com.sherifradwan.ancnative.ui.TagBloodNegativeBg
import com.sherifradwan.ancnative.ui.TagBloodNegativeBorder
import com.sherifradwan.ancnative.ui.TagBloodNegativeText
import com.sherifradwan.ancnative.ui.TagBloodPositiveBg
import com.sherifradwan.ancnative.ui.TagBloodPositiveBorder
import com.sherifradwan.ancnative.ui.TagBloodPositiveText
import com.sherifradwan.ancnative.ui.TagGestationalAgeBg
import com.sherifradwan.ancnative.ui.TagGestationalAgeBorder
import com.sherifradwan.ancnative.ui.TagGestationalAgeText
import com.sherifradwan.ancnative.ui.TagHighRiskBg
import com.sherifradwan.ancnative.ui.TagHighRiskText
import com.sherifradwan.ancnative.ui.formatBloodGroup
import com.sherifradwan.ancnative.ui.isHighRisk
import com.sherifradwan.ancnative.ui.isRhNegative
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import kotlin.math.roundToInt

@Stable
class PatientHubHeaderState(
    collapsedHeightPx: Float = 0f,
) {
    var offsetPx by mutableFloatStateOf(0f)
    var expandedHeightPx by mutableFloatStateOf(0f)
    var collapsedHeightPx by mutableFloatStateOf(collapsedHeightPx)

    val maxCollapsePx: Float
        get() = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(0f)

    val collapseFraction: Float
        get() {
            val max = maxCollapsePx
            return if (max <= 0f) 0f else (-offsetPx / max).coerceIn(0f, 1f)
        }

    val isStickyActive: Boolean
        get() = collapseFraction >= 0.75f

    // Hero layer fades during the initial 65% of the collapse gesture
    val heroAlpha: Float
        get() = (1f - (collapseFraction * 1.5f)).coerceIn(0f, 1f)

    // Sticky bar fades in during the latter 45% of the collapse gesture
    val stickyAlpha: Float
        get() = ((collapseFraction - 0.55f) / 0.45f).coerceIn(0f, 1f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientHubHeader(
    patient: DemoPatient,
    headerState: PatientHubHeaderState,
    selectedTab: PatientHubTab,
    onTabSelected: (PatientHubTab) -> Unit,
    severeBp: SevereBpAttention?,
    onBack: () -> Unit,
    onStartVisit: () -> Unit,
    onEditPatient: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenAttentionDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ga = patient.dating?.calculateGestationalAge()
    val isRhNeg = isRhNegative(patient.bloodGroupText)
    val hasAttention = !patient.clinicalAttention.isNullOrBlank() || severeBp != null

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (headerState.isStickyActive) 3.dp else 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsible Top Container: dynamically resized via Modifier.layout (zero recomposition)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val currentHeight = if (headerState.maxCollapsePx > 0f) {
                            (headerState.expandedHeightPx + headerState.offsetPx).roundToInt()
                                .coerceIn(
                                    headerState.collapsedHeightPx.roundToInt(),
                                    headerState.expandedHeightPx.roundToInt(),
                                )
                        } else {
                            placeable.height
                        }
                        layout(placeable.width, currentHeight) {
                            placeable.placeRelative(0, 0)
                        }
                    },
            ) {
                // Layer A: Expanded Hero Header (Provides stable intrinsic unconstrained measurement)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (size.height > 0 && size.height.toFloat() != headerState.expandedHeightPx) {
                                headerState.expandedHeightPx = size.height.toFloat()
                            }
                        }
                        .graphicsLayer {
                            alpha = headerState.heroAlpha
                            translationY = headerState.offsetPx * 0.35f
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Top Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumTouchTarget().testTag("hubBackButton"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (hasAttention) {
                                FilledTonalIconButton(
                                    onClick = onOpenAttentionDialog,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    modifier = Modifier.minimumTouchTarget().testTag("workspaceAttentionButton"),
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Clinical Attention",
                                        tint = ActionRose,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            OutlinedIconButton(
                                onClick = onOpenReport,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.minimumTouchTarget().testTag("workspaceReportButton").testTag("hubReportButton"),
                            ) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = "2-Page Report",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onEditPatient,
                                modifier = Modifier.minimumTouchTarget().testTag("editPatientButton"),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Patient",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Button(
                                onClick = onStartVisit,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.minimumTouchTarget().testTag("headerStartVisit"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Visit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Patient Identity & Primary Metrics Lockup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = patient.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        if (!ga.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TagGestationalAgeBg,
                                border = BorderStroke(1.dp, TagGestationalAgeBorder),
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.ga_prefix, ga),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TagGestationalAgeText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    // Standardized Clinical Badges FlowRow (24dp height, zero truncation on 360dp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ClinicalRiskBadge(isHighRisk = isHighRisk(patient.riskLevel))
                        patient.visits.firstOrNull()?.bloodPressure?.let { bp ->
                            BpAlertLabel(bp)
                        }
                        if (!patient.ageText.isNullOrBlank()) {
                            PatientAgeBadge(ageText = patient.ageText)
                        }
                        val g = patient.gravida ?: 1L
                        val p = patient.parity ?: 0L
                        val a = patient.abortion ?: 0L
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.height(24.dp),
                        ) {
                            Text(
                                text = "G$g P$p" + if (a > 0) " A$a" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                        if (!patient.bloodGroupText.isNullOrBlank()) {
                            val bg = formatBloodGroup(patient.bloodGroupText).orEmpty()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isRhNeg) TagBloodNegativeBg else TagBloodPositiveBg,
                                border = BorderStroke(1.dp, if (isRhNeg) TagBloodNegativeBorder else TagBloodPositiveBorder),
                                modifier = Modifier.height(24.dp),
                            ) {
                                Text(
                                    text = bg,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRhNeg) TagBloodNegativeText else TagBloodPositiveText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                        if (!patient.allergyHistory.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.height(24.dp),
                            ) {
                                Text(
                                    text = "Allergy: ${patient.allergyHistory}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }

                    // Compact Clinical Attention Strip (36dp min height)
                    if (hasAttention) {
                        CompactClinicalAttentionStrip(
                            attention = patient.clinicalAttention,
                            severeBp = severeBp,
                            onOpenDialog = onOpenAttentionDialog,
                        )
                    }
                }

                // Layer B: Collapsed Sticky Top Bar (Pinned at top, height 56dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp)
                        .graphicsLayer {
                            alpha = headerState.stickyAlpha
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.minimumTouchTarget()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = patient.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (!ga.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TagGestationalAgeBg,
                                border = BorderStroke(1.dp, TagGestationalAgeBorder),
                            ) {
                                Text(
                                    text = ga,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TagGestationalAgeText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (hasAttention) {
                            IconButton(onClick = onOpenAttentionDialog, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Attention Alert",
                                    tint = ActionRose,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        FilledTonalIconButton(
                            onClick = onStartVisit,
                            modifier = Modifier.size(40.dp).testTag("stickyStartVisit"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Start Visit", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Material 3 PrimaryScrollableTabRow: Always pinned below collapsible top container
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().testTag("patientHubTabBar"),
            ) {
                PatientHubTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount: Int? = when (tab) {
                        PatientHubTab.OVERVIEW -> null
                        PatientHubTab.VISITS -> patient.visits.size
                        PatientHubTab.SCANS -> patient.ultrasoundScans.size
                        PatientHubTab.LABS -> patient.labInvestigations.size
                        PatientHubTab.MEDS -> patient.medications.count { isOngoingMedication(it.status, it.isNonCompliant) }
                        PatientHubTab.PLAN -> patient.attachments.size
                    }
                    val hasTabAlert: Boolean = when (tab) {
                        PatientHubTab.OVERVIEW -> patient.dating == null
                        PatientHubTab.VISITS -> severeBp != null
                        PatientHubTab.SCANS -> false
                        PatientHubTab.LABS -> patient.labInvestigations.any { it.isAbnormal }
                        PatientHubTab.MEDS -> patient.medications.any { it.isNonCompliant }
                        PatientHubTab.PLAN -> false
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.minimumTouchTarget().testTag(tab.testTag),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                if (badgeCount != null && badgeCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (hasTabAlert) TagHighRiskBg else MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = if (hasTabAlert && tab == PatientHubTab.LABS) "$badgeCount▲" else "$badgeCount",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasTabAlert) TagHighRiskText else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        )
                                    }
                                } else if (hasTabAlert) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(ActionRose, RoundedCornerShape(3.dp)),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Compact, single-line/two-line interactive Clinical Alert Strip.
 * Replaces the heavy card with an elegant, non-disruptive signal that preserves vertical real estate.
 */
@Composable
fun CompactClinicalAttentionStrip(
    attention: String?,
    severeBp: SevereBpAttention?,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSevere = severeBp != null
    val containerColor = if (isSevere) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (isSevere) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    val iconTint = if (isSevere) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    val alertText = when {
        severeBp != null -> "Severe BP: ${severeBp.bloodPressure} (${severeBp.date})" +
            if (!attention.isNullOrBlank()) " • $attention" else ""
        !attention.isNullOrBlank() -> attention
        else -> ""
    }

    Surface(
        onClick = onOpenDialog,
        shape = RoundedCornerShape(8.dp),
        color = containerColor.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .testTag("clinicalAttentionBanner")
            .testTag("clinicalAttentionStrip"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert Icon",
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = alertText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit clinical alerts",
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
