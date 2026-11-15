package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.RecordBandCard
import com.sherifradwan.ancnative.ui.components.RecordEmptyCard
import com.sherifradwan.ancnative.ui.components.RecordListHeader
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.components.BiometryGrid
import com.sherifradwan.ancnative.ui.patienthub.model.extractBiometry
import java.util.UUID

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RecordListHeader(
                title = stringResource(R.string.scans_tab_title, patient.ultrasoundScans.size),
                actionLabel = stringResource(R.string.record_scan_action),
                actionTag = "recordNewScanBtn",
                onAction = {
                    onEvent(
                        AncEvent.StartUltrasoundEditor(
                            patientId = patient.id,
                            scanId = null,
                            newScanId = UUID.randomUUID().toString(),
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                },
                showAction = canEdit && patient.status != "archived",
            )
        }

        if (patient.ultrasoundScans.isEmpty()) {
            item { RecordEmptyCard() }
        } else {
            items(
                items = patient.ultrasoundScans,
                key = { it.id },
            ) { scan ->
                val gaText = remember(patient.dating, scan.date) {
                    GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, scan.date)?.shortWeeksText
                }
                val biometry = remember(scan) { extractBiometry(scan) }

                RecordBandCard(
                    title = scan.date,
                    family = FieldFamilies.obstetric(),
                    modifier = Modifier.testTag("ultrasound-${scan.id}"),
                    chips = if (gaText != null || scan.imageUris.isNotEmpty()) {
                        {
                            gaText?.let {
                                BandChip(ltrIsolate(it), tone = null, modifier = Modifier.testTag("gestationalAgeBadge"))
                            }
                            if (scan.imageUris.isNotEmpty()) {
                                BandChip(
                                    pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size),
                                    tone = null,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    actions = if (canEdit) {
                        {
                            DeleteIconButton(
                                type = DeletableType.ULTRASOUND,
                                record = scan,
                                onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, scan.id)) },
                            )
                            SectionTextAction(
                                text = stringResource(R.string.action_edit),
                                contentDescription = stringResource(R.string.edit_ultrasound),
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
                                modifier = Modifier.testTag("editUltrasound-${scan.id}"),
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    BiometryGrid(values = biometry)
                    if (!scan.impression.isNullOrBlank()) {
                        PlainField(stringResource(R.string.scan_impression), scan.impression)
                    }
                    if (scan.imageUris.isNotEmpty()) ScanPhotos(scan, onViewPhotos)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScanPhotos(
    scan: DemoUltrasoundScan,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
) {
    val count = scan.imageUris.size
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        scan.imageUris.forEachIndexed { idx, _ ->
            val openLabel = stringResource(R.string.scan_photo_open, idx + 1, count, scan.date)
            OutlinedButton(
                onClick = { onViewPhotos(scan, idx) },
                shape = RecordShapes.Small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = openLabel }
                    .testTag("wsScanPhotoChip-${scan.id}-$idx"),
            ) {
                Text(
                    text = stringResource(R.string.scan_photo_n, idx + 1),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
