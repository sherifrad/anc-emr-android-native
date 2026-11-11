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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.BloodGroupBadge
import com.sherifradwan.ancnative.ui.GestationalAgeBadge
import com.sherifradwan.ancnative.ui.ObstetricParityBadge
import com.sherifradwan.ancnative.ui.TagHighRiskBg
import com.sherifradwan.ancnative.ui.TagHighRiskDarkBg
import com.sherifradwan.ancnative.ui.TagHighRiskText
import com.sherifradwan.ancnative.ui.TagHighRiskDarkText
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.isDarkTheme
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
import com.sherifradwan.ancnative.model.datingSummaryOf
import java.time.LocalDate
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
    onEditPatient: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenAttentionDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ga = patient.dating?.calculateGestationalAge()
    val gaDiscordant = remember(patient) { datingSummaryOf(patient, LocalDate.now()).discordance != null }
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
                        }
                    }

                    // Patient Identity & Primary Metrics Lockup
                    // Name owns the full row width so the GA badge stacks below instead of squeezing it
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = patient.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hubPatientName"),
                        )

                        if (!ga.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .testTag("hubGaBadge"),
                            ) {
                                GestationalAgeBadge(
                                    gaText = stringResource(R.string.ga_prefix, ga),
                                    discordant = gaDiscordant,
                                )
                            }
                        }
                    }

                    // Clinical badges flow row: pills wrap and grow with their text
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("hubRiskBadge")
                        ) {
                            ClinicalRiskBadge(isHighRisk = isHighRisk(patient.riskLevel))
                        }
                        patient.visits.firstOrNull()?.bloodPressure?.let { bp ->
                            BpAlertLabel(bp)
                        }
                        if (!patient.ageText.isNullOrBlank()) {
                            PatientAgeBadge(ageText = patient.ageText)
                        }
                        ObstetricParityBadge(patient.gravida, patient.parity, patient.abortion)
                        if (!patient.bloodGroupText.isNullOrBlank()) {
                            BloodGroupBadge(patient.bloodGroupText)
                        }
                        val allergy = classifyAllergy(patient.allergyHistory)
                        if (allergy !is AllergyState.NotRecorded) {
                            val isAlert = allergy is AllergyState.Recorded
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.testTag("hubAllergyPill"),
                            ) {
                                Text(
                                    text = if (allergy is AllergyState.Recorded) {
                                        "Allergy: ${allergy.text}"
                                    } else {
                                        stringResource(R.string.no_allergy_history)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .testTag("hubAllergyText"),
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

                // Layer B: Collapsed Sticky Top Bar (Composed only when active to eliminate ghost touch conflicts)
                val isStickyActive = headerState.stickyAlpha > 0.05f
                if (isStickyActive) {
                    val gaCompact = patient.dating?.calculateGestationalAgeResult()?.shortWeeksText ?: ga
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = headerState.stickyAlpha
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.minimumTouchTarget().testTag("stickyBackButton"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = patient.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (!gaCompact.isNullOrBlank()) {
                                GestationalAgeBadge(gaCompact, discordant = gaDiscordant)
                            }
                            if (hasAttention) {
                                IconButton(
                                    onClick = onOpenAttentionDialog,
                                    modifier = Modifier.minimumTouchTarget().testTag("stickyAttentionButton"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Attention Alert",
                                        tint = ActionRose,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
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
                                    val isDark = isDarkTheme()
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (hasTabAlert) (if (isDark) TagHighRiskDarkBg else TagHighRiskBg) else MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = if (hasTabAlert && tab == PatientHubTab.LABS) "$badgeCount▲" else "$badgeCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasTabAlert) (if (isDark) TagHighRiskDarkText else TagHighRiskText) else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View clinical alert details",
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
