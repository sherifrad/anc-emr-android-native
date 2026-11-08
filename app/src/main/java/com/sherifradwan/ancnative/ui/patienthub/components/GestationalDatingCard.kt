package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.FactRow

@Composable
fun GestationalDatingCard(
    dating: DemoPatientDating?,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("datingCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Title + Method badge + Edit affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Pregnancy Dating",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!dating?.datingMethod.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = dating?.datingMethod.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("editDating"),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_dating),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (dating == null) {
                Text(
                    text = stringResource(R.string.no_dating_recorded),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                return@Column
            }

            val ga = dating.calculateGestationalAge()
            val edd = dating.agreedEdd ?: dating.ultrasoundEdd ?: dating.calculateEstimatedDueDate()?.toString()
            if (!ga.isNullOrBlank() || !edd.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GESTATIONAL AGE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = ga ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ClinicalTeal,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ESTIMATED DUE DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = edd ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (!ga.isNullOrBlank()) {
                FactRow(R.string.current_gestational_age, ga)
            }

            if (!dating.datingMethod.isNullOrBlank()) {
                FactRow(R.string.dating_method, dating.datingMethod)
            }

            when (dating.datingMethod?.uppercase()) {
                "ULTRASOUND", "US" -> {
                    if (!dating.ultrasoundEdd.isNullOrBlank()) {
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
                    if (!dating.agreedEdd.isNullOrBlank()) {
                        FactRow(R.string.agreed_edd, dating.agreedEdd)
                    }
                }
                else -> {
                    if (!dating.ultrasoundEdd.isNullOrBlank()) {
                        FactRow(R.string.ultrasound_edd, dating.ultrasoundEdd)
                    }
                    if (!dating.lmpDate.isNullOrBlank()) {
                        FactRow(R.string.lmp_date, dating.lmpDate)
                    }
                    if (!dating.transferDate.isNullOrBlank()) {
                        FactRow(R.string.ivf_transfer_date, dating.transferDate)
                    }
                    dating.embryoAgeDays?.let { age ->
                        FactRow(R.string.embryo_age, "Day $age")
                    }
                    if (!dating.agreedEdd.isNullOrBlank()) {
                        FactRow(R.string.agreed_edd, dating.agreedEdd)
                    }
                }
            }

            if (!dating.notes.isNullOrBlank()) {
                FactRow(R.string.notes, dating.notes)
            }
        }
    }
}
