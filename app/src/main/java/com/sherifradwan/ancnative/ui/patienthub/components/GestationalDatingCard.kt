package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.ui.ClinicalCardTokens
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.isDarkTheme

@Composable
fun GestationalDatingCard(
    dating: DemoPatientDating?,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = isDarkTheme()
    val bg = if (isDark) ClinicalCardTokens.DatingDarkBg else ClinicalCardTokens.DatingBg
    val border = if (isDark) ClinicalCardTokens.DatingDarkBorder else ClinicalCardTokens.DatingBorder
    val accent = if (isDark) ClinicalCardTokens.DatingDarkAccent else ClinicalCardTokens.DatingAccent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("datingCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.5.dp, border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Title + Method badge + Edit affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Dating",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (!dating?.datingMethod.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = dating?.datingMethod.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget().testTag("editDating"),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_dating),
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (dating == null) {
                Text(
                    text = stringResource(R.string.no_dating_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val ga = dating.calculateGestationalAge()
            val edd = dating.agreedEdd ?: dating.ultrasoundEdd ?: dating.calculateEstimatedDueDate()?.toString()
            if (!ga.isNullOrBlank() || !edd.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, border.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = "GESTATIONAL AGE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = ga ?: "-",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (isDark) accent else ClinicalTeal,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = "ESTIMATED DUE DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = edd ?: "-",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Only underlying clinical parameters (LMP, IVF, or notes) - GA and Dating Method are already in hero & header
            when (dating.datingMethod?.uppercase()) {
                "ULTRASOUND", "US" -> {
                    if (!dating.ultrasoundEdd.isNullOrBlank() && dating.ultrasoundEdd != edd) {
                        FactRow(R.string.ultrasound_edd, dating.ultrasoundEdd)
                    }
                }
                "LMP" -> {
                    if (!dating.lmpDate.isNullOrBlank()) {
                        FactRow(R.string.lmp_date, dating.lmpDate)
                    }
                }
                "IVF_TRANSFER", "IVF" -> {
                    if (!dating.transferDate.isNullOrBlank()) {
                        FactRow(R.string.ivf_transfer_date, dating.transferDate)
                    }
                    dating.embryoAgeDays?.let { age ->
                        FactRow(R.string.embryo_age, "Day $age")
                    }
                }
                "CLINICAL" -> {
                    if (!dating.agreedEdd.isNullOrBlank() && dating.agreedEdd != edd) {
                        FactRow(R.string.agreed_edd, dating.agreedEdd)
                    }
                }
                else -> {
                    if (!dating.lmpDate.isNullOrBlank()) {
                        FactRow(R.string.lmp_date, dating.lmpDate)
                    }
                    if (!dating.transferDate.isNullOrBlank()) {
                        FactRow(R.string.ivf_transfer_date, dating.transferDate)
                    }
                    dating.embryoAgeDays?.let { age ->
                        FactRow(R.string.embryo_age, "Day $age")
                    }
                }
            }

            if (!dating.notes.isNullOrBlank()) {
                FactRow(R.string.notes, dating.notes)
            }
        }
    }
}
