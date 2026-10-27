package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.sherifradwan.ancnative.storage.CameraCaptureHelper
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.UltrasoundDraftField

@Composable
fun UltrasoundEditor(
    state: AncState,
    route: AncRoute.UltrasoundEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.ultrasoundDraft?.takeIf { it.patientId == route.patientId && it.scanId == route.scanId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val context = LocalContext.current

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && pendingCameraUri != null) {
            val updated = draft.values.imageUris + pendingCameraUri.toString()
            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, updated.joinToString(",")))
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val updated = draft.values.imageUris + uri.toString()
            onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, updated.joinToString(",")))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val tempUri = CameraCaptureHelper.createCameraTempUri(context)
        pendingCameraUri = tempUri
        cameraLauncher.launch(tempUri)
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val tempUri = CameraCaptureHelper.createCameraTempUri(context)
            pendingCameraUri = tempUri
            cameraLauncher.launch(tempUri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    EditorPage(
        title = stringResource(R.string.ultrasound_editor),
        subtitle = "${patient.displayName} · Ultrasound Examination & Fetal Biometry",
        tag = "ultrasoundEditor",
    ) {
        ClinicalEditorErrorCard(state, draft.errors)

        // CARD 1: SCAN TIMING & GESTATIONAL AGE
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "1. Scan Timing & Gestational Age",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Date selection
                ClinicalDateField(
                    value = draft.values.date,
                    onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.DATE, it)) },
                    label = stringResource(R.string.scan_date),
                    tag = "scanDate",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
                        draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                    showTodayButton = true,
                )

                // Automatic Gestational Age calculation suggestion
                val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)
                if (calculatedGa != null && calculatedGa.isPostConception) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("scanCalculatedGaCard"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.calculated_ga_at_visit, calculatedGa.displayText),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            val isDifferent = draft.values.gaWeeks != calculatedGa.weeks.toString() ||
                                draft.values.gaDays != calculatedGa.days.toString()
                            if (isDifferent) {
                                TextButton(
                                    onClick = {
                                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, calculatedGa.weeks.toString()))
                                        onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, calculatedGa.days.toString()))
                                    },
                                    enabled = enabled,
                                    modifier = Modifier.testTag("applyScanGa"),
                                ) {
                                    Text(stringResource(R.string.apply_calculated_ga, calculatedGa.displayText))
                                }
                            }
                        }
                    }
                }

                // Side-by-side Gestational Age inputs (Weeks & Days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.gaWeeks,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_WEEKS, it)) },
                            label = stringResource(R.string.ga_weeks),
                            tag = "gaWeeks",
                            enabled = enabled,
                            error = draft.errors.contains(ClinicalValidationError.INVALID_GA_WEEKS),
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            value = draft.values.gaDays,
                            onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.GA_DAYS, it)) },
                            label = stringResource(R.string.ga_days),
                            tag = "gaDays",
                            enabled = enabled,
                            error = draft.errors.contains(ClinicalValidationError.INVALID_GA_DAYS),
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }

                // Link with Visit Section
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("linkedVisitCard"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.link_to_visit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (draft.values.linkedVisitId.isNotBlank()) {
                            val linkedVisit = patient.visits.firstOrNull { it.id == draft.values.linkedVisitId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.link_to_visit) + ": " + (linkedVisit?.date ?: draft.values.linkedVisitId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (linkedVisit?.bloodPressure != null || linkedVisit?.weight != null) {
                                        Text(
                                            text = "BP: ${linkedVisit.bloodPressure ?: "-"} · Wt: ${linkedVisit.weight ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "")) },
                                    enabled = enabled,
                                    modifier = Modifier.minimumTouchTarget().testTag("unlinkVisit"),
                                ) {
                                    Text(stringResource(R.string.unlink_visit))
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_linked_visit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (patient.visits.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    patient.visits.forEach { visit ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, visit.id)) },
                                            enabled = enabled,
                                            label = { Text(visit.date) },
                                            modifier = Modifier.minimumTouchTarget().testTag("linkVisit-${visit.id}"),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 2: FETUS, PLACENTA & LIQUOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "2. Fetus, Placenta & Amniotic Fluid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // 2-Column Space-Efficient Layout for Fetus & Placenta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceGroup(
                            label = stringResource(R.string.fetal_number),
                            selected = draft.values.fetalNumber,
                            choices = ClinicalVocabulary.fetalNumbers.toList(),
                            enabled = enabled,
                            tagPrefix = "fetalNumberChoice",
                            onSelected = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_NUMBER, it)) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceGroup(
                            label = stringResource(R.string.fetal_presentation),
                            selected = draft.values.presentation,
                            choices = ClinicalVocabulary.fetalPresentations.toList(),
                            enabled = enabled,
                            tagPrefix = "presentationChoice",
                            onSelected = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PRESENTATION, it)) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceGroup(
                            label = stringResource(R.string.placental_site),
                            selected = draft.values.placentalSite,
                            choices = ClinicalVocabulary.placentalSites.toList(),
                            enabled = enabled,
                            tagPrefix = "placentaChoice",
                            onSelected = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.PLACENTAL_SITE, it)) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceGroup(
                            label = stringResource(R.string.amniotic_fluid),
                            selected = draft.values.amnioticFluid,
                            choices = ClinicalVocabulary.amnioticFluids.toList(),
                            enabled = enabled,
                            tagPrefix = "liquorChoice",
                            onSelected = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.AMNIOTIC_FLUID, it)) },
                        )
                    }
                }
            }
        }

        // CARD 3: FETAL BIOMETRY
        val hasBiometry = draft.values.fetalHeartRate.isNotBlank() ||
            draft.values.biometryBpd.isNotBlank() ||
            draft.values.biometryHc.isNotBlank() ||
            draft.values.biometryAc.isNotBlank() ||
            draft.values.biometryFl.isNotBlank() ||
            draft.values.estimatedFetalWeight.isNotBlank()
        var biometryExpanded by remember { mutableStateOf(hasBiometry) }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("biometryCard"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "3. Fetal Biometry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "BPD, HC, AC, FL, FHR & EFW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { biometryExpanded = !biometryExpanded },
                        modifier = Modifier.minimumTouchTarget().testTag("toggleBiometry"),
                    ) {
                        Text(if (biometryExpanded) "▲ Collapse" else "▼ Expand Biometry")
                    }
                }
                if (biometryExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.fetalHeartRate,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.FETAL_HEART_RATE, it)) },
                                label = stringResource(R.string.fetal_heart_rate),
                                tag = "fetalHeartRate",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.estimatedFetalWeight,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.ESTIMATED_FETAL_WEIGHT, it)) },
                                label = stringResource(R.string.estimated_fetal_weight),
                                tag = "efw",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Number,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.biometryBpd,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_BPD, it)) },
                                label = stringResource(R.string.biometry_bpd),
                                tag = "biometryBpd",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.biometryHc,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_HC, it)) },
                                label = stringResource(R.string.biometry_hc),
                                tag = "biometryHc",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.biometryAc,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_AC, it)) },
                                label = stringResource(R.string.biometry_ac),
                                tag = "biometryAc",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditorField(
                                value = draft.values.biometryFl,
                                onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.BIOMETRY_FL, it)) },
                                label = stringResource(R.string.biometry_fl),
                                tag = "biometryFl",
                                enabled = enabled,
                                singleLine = true,
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                    }
                }
            }
        }

        // CARD 4: SCAN PHOTOS & IMPRESSION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "4. Scan Photos & Clinical Impression",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Attachments / Scan Photos Section
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("scanAttachmentsCard"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.scan_attachments),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (draft.values.imageUris.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_photos_attached),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                draft.values.imageUris.forEachIndexed { index, _ ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("attachmentItem-$index"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = "📷 Photo ${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updated = draft.values.imageUris.toMutableList().apply { removeAt(index) }
                                                    onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, updated.joinToString(",")))
                                                },
                                                enabled = enabled,
                                                modifier = Modifier.minimumTouchTarget().testTag("removePhoto-$index"),
                                            ) {
                                                Text("✕", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { launchCamera() },
                                enabled = enabled,
                                modifier = Modifier.minimumTouchTarget().testTag("captureScanPhotoCamera"),
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Photo", maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                enabled = enabled,
                                modifier = Modifier.minimumTouchTarget().testTag("addScanPhoto"),
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Image", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                EditorField(
                    value = draft.values.impression,
                    onValueChange = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, it)) },
                    label = stringResource(R.string.scan_impression),
                    tag = "impression",
                    enabled = enabled,
                    singleLine = false,
                )

                QuickPresetChips(
                    title = stringResource(R.string.scan_impression),
                    items = ClinicalPresets.commonUltrasoundImpressions,
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, it)) },
                    tagPrefix = "impressionPreset",
                )
            }
        }

        // Security Vault Indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Ultrasound records and scans encrypted locally (AES-256 GCM) · Hardware Vault Security",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EditorActions(
            enabled = enabled,
            saveTag = "saveUltrasound",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitUltrasound) },
        )
    }
}
