package com.sherifradwan.ancnative.ui.patienthub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.ui.ActionRose
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.BpAlertLabel
import com.sherifradwan.ancnative.ui.FieldPatientBg
import com.sherifradwan.ancnative.ui.FieldPatientDarkBg
import com.sherifradwan.ancnative.ui.GaDiscordanceMarker
import com.sherifradwan.ancnative.ui.PatientRiskTier
import com.sherifradwan.ancnative.ui.StatusHighBg
import com.sherifradwan.ancnative.ui.StatusHighDarkBg
import com.sherifradwan.ancnative.ui.StatusHighDarkText
import com.sherifradwan.ancnative.ui.StatusHighText
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.components.MetricCell
import com.sherifradwan.ancnative.ui.components.MetricValue
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.components.parityText
import com.sherifradwan.ancnative.ui.formatAgeYears
import com.sherifradwan.ancnative.ui.formatBloodGroup
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.isHighRisk
import com.sherifradwan.ancnative.ui.isRhNegative
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.parseRiskTier
import com.sherifradwan.ancnative.ui.patienthub.components.ClinicalAttentionLevel
import com.sherifradwan.ancnative.ui.patienthub.components.attentionColors
import com.sherifradwan.ancnative.ui.patienthub.components.clinicalAttentionLevel
import com.sherifradwan.ancnative.ui.patienthub.components.deliveredHeadline
import com.sherifradwan.ancnative.ui.patienthub.components.levelWord
import com.sherifradwan.ancnative.ui.patienthub.components.parseAttentionEntries
import com.sherifradwan.ancnative.ui.patienthub.components.sortedForDisplay
import java.time.LocalDate
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
    val ga = patient.dating?.calculateGestationalAgeResult()?.shortWeeksText ?: patient.dating?.calculateGestationalAge()
    val gaDiscordant = remember(patient) { datingSummaryOf(patient, LocalDate.now()).discordance != null }
    val isRhNeg = isRhNegative(patient.bloodGroupText)
    val attentionEntries = remember(patient.clinicalAttention) { parseAttentionEntries(patient.clinicalAttention) }
    val hasAttention = attentionEntries.isNotEmpty() || severeBp != null

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
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
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
                                        contentDescription = stringResource(R.string.clinical_attention),
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
                                    contentDescription = stringResource(R.string.action_two_page_report),
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
                                    contentDescription = stringResource(R.string.edit_patient),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // Patient Name
                    Text(
                        text = patient.displayName,
                        style = RecordType.HeaderName,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hubPatientName"),
                    )

                    // Clinical badges / tags flow row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val isHigh = isHighRisk(patient.riskLevel)
                        val isModerate = !isHigh && parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM
                        if (isHigh) {
                            Box(modifier = Modifier.testTag("hubRiskBadge")) {
                                StatusTag(StatusKind.HighRisk)
                            }
                        } else if (isModerate) {
                            Box(modifier = Modifier.testTag("hubRiskBadge")) {
                                StatusTag(StatusKind.ModerateRisk)
                            }
                        }

                        patient.visits.firstOrNull()?.bloodPressure?.let { bp ->
                            BpAlertLabel(bp)
                        }

                        val allergy = classifyAllergy(patient.allergyHistory)
                        if (allergy !is AllergyState.NotRecorded) {
                            val isAlert = allergy is AllergyState.Recorded
                            Surface(
                                shape = RecordShapes.Small,
                                color = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isAlert) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null,
                                modifier = Modifier.testTag("hubAllergyPill"),
                            ) {
                                Text(
                                    text = if (isAlert) {
                                        stringResource(R.string.allergy_banner, allergy.text)
                                    } else {
                                        stringResource(R.string.no_allergy_history)
                                    },
                                    style = RecordType.Tag.copy(fontWeight = FontWeight.Bold),
                                    color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("hubAllergyText"),
                                )
                            }
                        }
                    }

                    // Facts row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (patient.delivered) {
                            Box(modifier = Modifier.weight(1f).testTag("hubDeliveredBadge")) {
                                MetricCell(stringResource(R.string.delivery)) {
                                    MetricValue(deliveredHeadline(patient.deliveryDate))
                                }
                            }
                        } else if (!ga.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f).testTag("hubGaBadge")) {
                                MetricCell(stringResource(R.string.ga_label)) {
                                    Row(
                                        modifier = Modifier.testTag("gestationalAgeBadge"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        MetricValue(ltrIsolate(ga))
                                        if (gaDiscordant) {
                                            GaDiscordanceMarker(style = RecordType.MetricValue)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        MetricCell(
                            label = stringResource(R.string.parity),
                            modifier = Modifier.weight(1f),
                        ) {
                            MetricValue(
                                value = ltrIsolate(parityText(patient.gravida, patient.parity, patient.abortion)),
                            )
                        }

                        val bgFormatted = formatBloodGroup(patient.bloodGroupText)
                        MetricCell(
                            label = stringResource(R.string.blood_group),
                            modifier = Modifier.weight(1f),
                        ) {
                            MetricValue(
                                value = ltrIsolate(bgFormatted ?: "--"),
                                isMissing = bgFormatted == null,
                            )
                            if (isRhNeg) {
                                StatusTag(
                                    kind = StatusKind.HighRisk,
                                    text = stringResource(R.string.rh_negative_tag),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }

                    val ageYears = formatAgeYears(patient.ageText)
                    if (ageYears != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetricCell(
                                label = stringResource(R.string.age_label),
                                modifier = Modifier.weight(1f),
                            ) {
                                MetricValue(ltrIsolate(ageYears))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.weight(1f))
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                            Text(
                                text = patient.displayName,
                                style = RecordType.CardName,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (patient.delivered) {
                                Text(
                                    text = ltrIsolate(deliveredHeadline(patient.deliveryDate)),
                                    style = RecordType.Action.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            } else if (!gaCompact.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = ltrIsolate(gaCompact),
                                        style = RecordType.Action.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                    )
                                    if (gaDiscordant) {
                                        GaDiscordanceMarker(style = RecordType.Action.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                            if (hasAttention) {
                                IconButton(
                                    onClick = onOpenAttentionDialog,
                                    modifier = Modifier.minimumTouchTarget().testTag("stickyAttentionButton"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.clinical_attention),
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
                val dark = isDarkTheme()
                val needsAttention = stringResource(R.string.needs_attention)
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
                                Text(
                                    text = tab.title,
                                    style = RecordType.Action.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                if (badgeCount != null && badgeCount > 0) {
                                    Surface(
                                        shape = RecordShapes.Small,
                                        color = if (hasTabAlert) {
                                            if (dark) StatusHighDarkBg else StatusHighBg
                                        } else {
                                            if (dark) FieldPatientDarkBg else FieldPatientBg
                                        },
                                    ) {
                                        Text(
                                            text = if (hasTabAlert && tab == PatientHubTab.LABS) "$badgeCount▲" else "$badgeCount",
                                            style = RecordType.Tag,
                                            color = if (hasTabAlert) {
                                                if (dark) StatusHighDarkText else StatusHighText
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        )
                                    }
                                } else if (hasTabAlert) {
                                    Surface(
                                        shape = RecordShapes.Small,
                                        color = if (dark) StatusHighDarkBg else StatusHighBg,
                                        modifier = Modifier.semantics { contentDescription = needsAttention },
                                    ) {
                                        Text(
                                            text = "!",
                                            style = RecordType.Tag.copy(fontWeight = FontWeight.Bold),
                                            color = if (dark) StatusHighDarkText else StatusHighText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        )
                                    }
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
    val entries = remember(attention) { parseAttentionEntries(attention) }
    if (entries.isEmpty() && severeBp == null) return

    val level = clinicalAttentionLevel(attention, severeBp)
    val colors = attentionColors(level)
    val sorted = remember(entries) { sortedForDisplay(entries) }
    val firstEntry = sorted.firstOrNull()

    val itemLevel = if (severeBp != null) ClinicalAttentionLevel.CRITICAL else (firstEntry?.level ?: ClinicalAttentionLevel.NOTE)
    val shownText = if (severeBp != null) {
        stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date)
    } else {
        firstEntry?.text.orEmpty()
    }
    val remainingCount = if (severeBp != null) entries.size else (entries.size - 1).coerceAtLeast(0)

    val prefix = ""
    val headline = shownText
    val moreSuffix = if (remainingCount > 0) stringResource(R.string.attention_remaining_count, remainingCount) else ""
    @Suppress("UNUSED_VARIABLE")
    val bodyText = "$prefix⁨$headline⁩⁨$moreSuffix⁩"

    Surface(
        onClick = onOpenDialog,
        shape = RecordShapes.Small,
        color = colors.container,
        border = BorderStroke(colors.borderWidth, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("clinicalAttentionStrip")
            .testTag("clinicalAttentionBanner"),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = levelWord(itemLevel),
                    style = RecordType.FieldLabel.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent,
                )
                Text(
                    text = "⁨$headline⁩",
                    style = RecordType.Secondary,
                    color = colors.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (remainingCount > 0) {
                Surface(
                    shape = RecordShapes.Small,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, colors.accent),
                ) {
                    Text(
                        text = "⁨$moreSuffix⁩",
                        style = RecordType.Tag,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
