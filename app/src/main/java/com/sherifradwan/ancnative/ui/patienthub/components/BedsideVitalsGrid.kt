package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.SfhConcordance
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.formatWeightKg
import com.sherifradwan.ancnative.ui.patienthub.model.VitalsSnapshot

@Composable
fun BedsideVitalsGrid(
    vitals: VitalsSnapshot,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            // Row 1: Maternal Vitals (BP + Weight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Blood Pressure Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blood Pressure",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = vitals.bpText?.let { "$it mmHg" } ?: "-",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = when (vitals.bpLevel) {
                            BpLevel.SEVERE -> MaterialTheme.colorScheme.error
                            BpLevel.RAISED -> MaterialTheme.colorScheme.tertiary
                            BpLevel.NORMAL -> MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (vitals.bpLevel) {
                            BpLevel.SEVERE -> MaterialTheme.colorScheme.errorContainer
                            BpLevel.RAISED -> MaterialTheme.colorScheme.tertiaryContainer
                            BpLevel.NORMAL -> ClinicalTeal.copy(alpha = 0.12f)
                        },
                    ) {
                        Text(
                            text = when (vitals.bpLevel) {
                                BpLevel.SEVERE -> "▲ Severe · Urgent ⚠️"
                                BpLevel.RAISED -> "▲ Raised · Recheck"
                                BpLevel.NORMAL -> "Normal ✓"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (vitals.bpLevel) {
                                BpLevel.SEVERE -> MaterialTheme.colorScheme.onErrorContainer
                                BpLevel.RAISED -> MaterialTheme.colorScheme.onTertiaryContainer
                                BpLevel.NORMAL -> ClinicalTeal
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }

                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                )

                Spacer(Modifier.width(12.dp))

                // Maternal Weight Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Maternal Weight",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatWeightKg(vitals.weightText) ?: "-",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (vitals.weightDeltaText != null) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = vitals.weightDeltaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }

            // Row 2: Fetal Assessment (FHR + optional legacy SFH)
            val hasFhr = !vitals.fhrText.isNullOrBlank()
            val hasLegacySfh = !vitals.sfhCm.isNullOrBlank()
            if (hasFhr || hasLegacySfh) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    if (hasFhr) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fetal Heart Rate",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${vitals.fhrText} bpm",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (vitals.fhrLevel != null) {
                                Spacer(Modifier.height(2.dp))
                                val (fhrText, fhrColor, fhrBg) = when (vitals.fhrLevel) {
                                    FhrLevel.NORMAL -> Triple("110-160 normal ✓", ClinicalTeal, ClinicalTeal.copy(alpha = 0.12f))
                                    FhrLevel.BRADYCARDIA -> Triple("Bradycardia (<110) ⚠️", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                                    FhrLevel.TACHYCARDIA -> Triple("Tachycardia (>160) ⚠️", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = fhrBg,
                                ) {
                                    Text(
                                        text = fhrText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = fhrColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (hasFhr && hasLegacySfh) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    if (hasLegacySfh) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fundal Height",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${vitals.sfhCm} cm",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (vitals.sfhConcordance != null) {
                                Spacer(Modifier.height(2.dp))
                                val (sfhText, sfhColor, sfhBg) = when (vitals.sfhConcordance) {
                                    SfhConcordance.CONCORDANT -> Triple("Concordant ✓", ClinicalTeal, ClinicalTeal.copy(alpha = 0.12f))
                                    SfhConcordance.DISCREPANT_LARGE -> Triple("Large for dates ▲", MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                                    SfhConcordance.DISCREPANT_SMALL -> Triple("Small for dates ▼", MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = sfhBg,
                                ) {
                                    Text(
                                        text = sfhText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = sfhColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
