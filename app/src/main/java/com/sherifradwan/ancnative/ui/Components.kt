package com.sherifradwan.ancnative.ui

import java.time.LocalDate
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.stringRes

/**
 * Global Unified Clinical Patient Card Variants.
 *
 * Defines the presentation modes of the patient card across the application.
 * In all variants, the patient display name is prioritized with full width and never
 * squished or truncated by adjacent badge icons. The demo patient ID is strictly omitted.
 */
enum class PatientCardVariant {
    /**
     * Bedside workspace top bar: Dedicated Row 1 for navigation back button,
     * patient full display name (titleLarge, bold, weight(1f)), and action buttons.
     * Dedicated Row 2 FlowRow for clinical metadata (Risk, Age, Parity, Blood Group, GA, EDD, Allergy).
     */
    BEDSIDE_TOP_BAR,

    /**
     * Summary card for section/editor headers (e.g. Lab Batch, Lab Editor, Meds Editor).
     */
    EDITOR_SUMMARY,
}

/**
 * Global Unified Patient Card component.
 *
 * Exported globally and styled consistently for every section according to [variant].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalPatientCard(
    patient: DemoPatient,
    variant: PatientCardVariant,
    modifier: Modifier = Modifier,
    asOfDate: LocalDate = LocalDate.now(),
    onBack: (() -> Unit)? = null,
    onOpenReport: (() -> Unit)? = null,
    onOpenAttentionDialog: (() -> Unit)? = null,
) {
    when (variant) {
        PatientCardVariant.BEDSIDE_TOP_BAR -> {
            Card(
                modifier = modifier.fillMaxWidth().testTag("workspaceTopBar"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Row 1: Back + Patient Name (Full space, never truncated by badges) + Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.minimumTouchTarget(),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        Text(
                            text = patient.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() },
                        )

                        Spacer(Modifier.width(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val hasAttention = !patient.clinicalAttention.isNullOrBlank()
                            if (hasAttention && onOpenAttentionDialog != null) {
                                IconButton(
                                    onClick = onOpenAttentionDialog,
                                    modifier = Modifier.minimumTouchTarget().testTag("workspaceAttentionButton"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Clinical Attention & Alerts",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            if (onOpenReport != null) {
                                IconButton(
                                    onClick = onOpenReport,
                                    modifier = Modifier.minimumTouchTarget().testTag("workspaceReportButton"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = stringResource(R.string.print_patient_report),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Metadata Strip (Risk Badge, Age Badge, GPA, Blood Group, GA, EDD, Allergy)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // 1. Risk Level Badge
                        ClinicalRiskBadge(
                            isHighRisk = isHighRisk(patient.riskLevel),
                            isModerate = parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM,
                        )

                        // 2. Maternal Age Badge (in metadata row so it never squishes patient name)
                        if (formatAgeYears(patient.ageText) != null) {
                            PatientAgeBadge(ageText = patient.ageText)
                        }

                        // 3. Parity Pill (G/P/A)
                        val g = patient.gravida ?: 1L
                        val p = patient.parity ?: 0L
                        val a = patient.abortion ?: 0L
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "G$g P$p A$a",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }

                        // 4. Blood Group
                        BloodGroupBadge(patient.bloodGroupText)

                        // 5. Gestational Age
                        val datingSummary = datingSummaryOf(patient, asOfDate)
                        val gaResult = patient.dating?.calculateGestationalAgeResult()
                        val ga = patient.dating?.calculateGestationalAge(asOfDate)
                        val gaText = gaResult?.shortWeeksText ?: ga?.toString()
                        if (gaText != null) {
                            GestationalAgeBadge("GA $gaText", discordant = datingSummary.discordance != null)
                        }

                        // 6. EDD
                        val edd = datingSummary.edd?.let(::formatDmy)
                        if (!edd.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Text(
                                    text = "EDD $edd",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        // 7. Allergy Status
                        val allergy = classifyAllergy(patient.allergyHistory)
                        val isAlert = allergy is AllergyState.Recorded
                        val allergyLabel = when (allergy) {
                            is AllergyState.Recorded -> allergy.text
                            AllergyState.ExplicitlyNone -> stringResource(R.string.no_allergy_history)
                            AllergyState.NotRecorded -> stringResource(R.string.not_recorded)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = allergyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAlert) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        PatientCardVariant.EDITOR_SUMMARY -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.minimumTouchTarget(),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = patient.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        ClinicalRiskBadge(
                            isHighRisk = isHighRisk(patient.riskLevel),
                            isModerate = parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM,
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (formatAgeYears(patient.ageText) != null) {
                            PatientAgeBadge(ageText = patient.ageText)
                        }

                        val g = patient.gravida ?: 1L
                        val p = patient.parity ?: 0L
                        ObstetricParityBadge(g, p, 0L)

                        BloodGroupBadge(patient.bloodGroupText)

                        val gaResult = patient.dating?.calculateGestationalAgeResult()
                        val ga = patient.dating?.calculateGestationalAge(asOfDate)
                        val gaText = gaResult?.shortWeeksText ?: ga?.toString()
                        if (gaText != null) {
                            GestationalAgeBadge("GA $gaText", discordant = datingSummaryOf(patient, asOfDate).discordance != null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FactRow(
    @StringRes label: Int,
    value: String?,
    isEmphasized: Boolean = false,
    valueColor: Color? = null,
) {
    FactRow(
        label = stringResource(label),
        value = value,
        isEmphasized = isEmphasized,
        valueColor = valueColor,
    )
}

@Composable
fun FactRow(
    label: String,
    value: String?,
    isEmphasized: Boolean = false,
    valueColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value?.takeIf(String::isNotBlank) ?: stringResource(R.string.not_recorded),
            style = if (isEmphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor ?: if (value.isNullOrBlank()) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1.3f),
        )
    }
}

fun Modifier.minimumTouchTarget(): Modifier = sizeIn(minWidth = 48.dp, minHeight = 48.dp)

@Composable
internal fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val showError = error || errorMessage != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = if (showError) ({ Text(errorMessage ?: stringResource(R.string.correct_highlighted_field)) }) else null,
        isError = showError,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
internal fun GenericEditorErrorCard(hasErrors: Boolean, state: AncState) {
    if (!hasErrors && state.savePhase == SavePhase.IDLE) return
    Card(modifier = Modifier.fillMaxWidth().testTag("editorError")) {
        Text(
            text = when {
                hasErrors -> stringResource(R.string.correct_highlighted_fields)
                state.savePhase == SavePhase.PENDING -> stringResource(R.string.save_pending)
                state.savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
                state.savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
                else -> stringResource(state.saveError.stringRes())
            },
            color = if (hasErrors || state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}
