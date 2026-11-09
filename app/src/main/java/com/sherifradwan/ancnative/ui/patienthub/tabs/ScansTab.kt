package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.GestationalAgeBadge
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.components.BiometryGrid
import com.sherifradwan.ancnative.ui.patienthub.model.extractBiometry

import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScansTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("scansTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Action: + Record Ultrasound Scan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ultrasound Scans (${patient.ultrasoundScans.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canEdit && patient.status != "archived") {
                    Button(
                        onClick = {
                            onEvent(
                                AncEvent.StartUltrasoundEditor(
                                    patientId = patient.id,
                                    scanId = null,
                                    newScanId = UUID.randomUUID().toString(),
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("recordNewScanBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Record Scan", fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
            }
        }

        if (patient.ultrasoundScans.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No records",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(
                items = patient.ultrasoundScans,
                key = { it.id },
            ) { scan ->
                val gaAtScan = remember(patient.dating, scan.date) {
                    GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, scan.date)
                }
                val gaDisplayText = gaAtScan?.displayText
                val biometry = remember(scan) { extractBiometry(scan) }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ultrasound-${scan.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Header Row: Date + GA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = scan.date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (!gaDisplayText.isNullOrBlank()) {
                                GestationalAgeBadge(gaDisplayText)
                            }
                        }

                        // Canonical Quantitative Biometry Grid
                        BiometryGrid(values = biometry)

                        if (!scan.impression.isNullOrBlank()) {
                            FactRow(R.string.scan_impression, scan.impression)
                        }

                        // Attachments Preview
                        if (scan.imageUris.isNotEmpty()) {
                            FactRow(R.string.scan_attachments, pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                scan.imageUris.forEachIndexed { idx, _ ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .testTag("wsScanPhotoChip-${scan.id}-$idx")
                                            .minimumTouchTarget()
                                            .clickable { onViewPhotos(scan, idx) },
                                    ) {
                                        Text(
                                            text = "Photo ${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Actions
                        if (canEdit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DeleteIconButton(
                                    type = DeletableType.ULTRASOUND,
                                    record = scan,
                                    onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, scan.id)) },
                                )
                                TextButton(
                                    onClick = {
                                        onEvent(
                                            AncEvent.StartUltrasoundEditor(
                                                patientId = patient.id,
                                                scanId = scan.id,
                                                newScanId = scan.id,
                                                editorSessionId = UUID.randomUUID().toString(),
                                            ),
                                        )
                                    },
                                    modifier = Modifier.minimumTouchTarget().testTag("editUltrasound-${scan.id}"),
                                ) {
                                    Text(stringResource(R.string.edit_ultrasound))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
