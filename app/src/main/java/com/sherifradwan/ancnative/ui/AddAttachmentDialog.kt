package com.sherifradwan.ancnative.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.CameraCaptureHelper
import com.sherifradwan.ancnative.storage.PhotoQualityPreset
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

@Composable
fun AddAttachmentDialog(
    patient: DemoPatient,
    initialCategory: String = AttachmentCategories.ULTRASOUND,
    onDismiss: () -> Unit,
    onSave: (sourceUri: Uri, category: String, title: String?, preset: PhotoQualityPreset) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var titleText by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var chosenPreset by remember { mutableStateOf<PhotoQualityPreset?>(null) }
    val preset = chosenPreset ?: PhotoQualityPreset.defaultFor(selectedCategory)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri?.let { old -> if (old != uri) CameraCaptureHelper.deleteIfCaptureTemp(context, old) }
            selectedUri = uri
        }
    }

    // An unsent camera photo is deleted when the dialog goes away (Cancel, back, app lock);
    // deleteIfCaptureTemp ignores gallery URIs.
    var sent by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            // Delegated state is read directly so disposal sees the current values.
            val pending = selectedUri
            if (!sent && pending != null) CameraCaptureHelper.deleteIfCaptureTemp(context, pending)
        }
    }

    if (showCamera) {
        InAppCameraDialog(
            onCaptured = { uri ->
                selectedUri?.let { old -> if (old != uri) CameraCaptureHelper.deleteIfCaptureTemp(context, old) }
                selectedUri = uri
                showCamera = false
            },
            onDismiss = { showCamera = false },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 32).dp)
                .testTag("addAttachmentDialog"),
            shape = RecordShapes.Large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.add_attachment_title),
                            style = RecordType.SectionTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = patient.displayName,
                            style = RecordType.Action,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumTouchTarget()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FormFieldLabel(stringResource(R.string.attachment_type_label))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val categories = listOf(
                            AttachmentCategories.ULTRASOUND to R.string.attachment_type_ultrasound,
                            AttachmentCategories.LAB to R.string.attachment_type_labs,
                            AttachmentCategories.PRESCRIPTION to R.string.attachment_type_prescription,
                            AttachmentCategories.CLINICAL_PHOTO to R.string.attachment_type_photo,
                            AttachmentCategories.OTHER to R.string.attachment_type_other,
                        )
                        categories.forEach { (cat, label) ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FormFieldLabel(stringResource(R.string.photo_quality_label))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            Triple(PhotoQualityPreset.SMALL, "preset_small", R.string.photo_quality_small),
                            Triple(PhotoQualityPreset.STANDARD, "preset_standard", R.string.photo_quality_standard),
                            Triple(PhotoQualityPreset.DETAILED, "preset_detailed", R.string.photo_quality_detailed),
                        ).forEach { (option, tag, label) ->
                            FilterChip(
                                selected = preset == option,
                                onClick = { chosenPreset = option },
                                label = { Text(stringResource(label)) },
                                modifier = Modifier.testTag(tag),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.attachment_title_label)) },
                    placeholder = { Text(stringResource(R.string.attachment_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (selectedUri == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhotoSourceButton(
                            title = stringResource(R.string.attachment_take_photo),
                            hint = stringResource(R.string.attachment_take_photo_hint),
                            onClick = { showCamera = true },
                            modifier = Modifier.testTag("captureCameraBtn"),
                        )
                        PhotoSourceButton(
                            title = stringResource(R.string.attachment_gallery),
                            hint = stringResource(R.string.attachment_gallery_hint),
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            modifier = Modifier.testTag("pickGalleryBtn"),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp)
                            .clip(RecordShapes.Small)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RecordShapes.Small),
                        contentAlignment = Alignment.Center,
                    ) {
                        val previewBitmap = remember(selectedUri) {
                            runCatching {
                                BoundedBitmap.decodeStream({ context.contentResolver.openInputStream(selectedUri!!) }, 1024)?.asImageBitmap()
                            }.getOrNull()
                        }
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTextAction(
                            text = stringResource(R.string.attachment_retake),
                            contentDescription = stringResource(R.string.attachment_retake),
                            onClick = { showCamera = true },
                        )
                        SectionTextAction(
                            text = stringResource(R.string.attachment_gallery),
                            contentDescription = stringResource(R.string.attachment_gallery),
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                    }
                }

                SheetCancelSaveRow(
                    enabled = true,
                    saving = false,
                    saveEnabled = selectedUri != null,
                    savingLabel = stringResource(R.string.attachment_save),
                    onCancel = onDismiss,
                    onSave = {
                        val uri = selectedUri ?: return@SheetCancelSaveRow
                        sent = true
                        onSave(uri, selectedCategory, titleText.trim().ifEmpty { null }, preset)
                        onDismiss()
                    },
                    cancelTag = "cancelAttachmentBtn",
                    saveTag = "saveAttachmentConfirmBtn",
                    spinnerTag = "attachmentSaveSpinner",
                )
            }
        }
    }
}

@Composable
private fun PhotoSourceButton(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = RecordShapes.Small,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = RecordType.Action, color = MaterialTheme.colorScheme.primary)
            Text(text = hint, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
