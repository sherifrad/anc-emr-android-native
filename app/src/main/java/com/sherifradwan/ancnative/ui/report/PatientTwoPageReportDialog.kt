package com.sherifradwan.ancnative.ui.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private suspend fun encodeSelectedPictures(
    context: Context,
    patientId: String,
    figures: List<Figure>,
): Map<String, String> {
    if (figures.isEmpty()) return emptyMap()
    return encodePicturesForPrint(
        context = context,
        patientId = patientId,
        figures = figures,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientTwoPageReportDialog(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        try {
            purgeStaleReportExports(context.cacheDir)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Cleanup is best-effort; never block opening the report dialog.
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val shareSubject = stringResource(R.string.report_share_subject_format, patient.displayName)
    val shareChooserTitle = stringResource(R.string.report_share_chooser_title)
    val pageSwitcherDescription = stringResource(R.string.report_pages_tab_group)
    val candidates = remember(patient) { pictureCandidates(patient) }
    var selectedPage by rememberSaveable(patient.id) { mutableIntStateOf(0) }
    var selectedPictureKeys by rememberSaveable(patient.id, stateSaver = StringSetSaver) {
        mutableStateOf(emptySet())
    }
    var showPicturePicker by rememberSaveable(patient.id) { mutableStateOf(false) }
    var isPreparingPrint by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }
    val validSelectedKeys = remember(candidates, selectedPictureKeys) {
        val candidateKeys = candidates.mapTo(HashSet(candidates.size)) { it.key }
        selectedPictureKeys.intersect(candidateKeys)
    }
    val figures = remember(candidates, validSelectedKeys) {
        figuresFor(candidates, validSelectedKeys)
    }
    val showPrintUnavailable: () -> Unit = {
        Toast.makeText(
            context,
            resources.getString(R.string.report_print_unavailable),
            Toast.LENGTH_LONG,
        ).show()
    }
    val showPdfExportFailed: () -> Unit = {
        Toast.makeText(
            context,
            resources.getString(R.string.report_share_pdf_failed),
            Toast.LENGTH_LONG,
        ).show()
    }
    val page1Scroll = rememberScrollState()
    val page2Scroll = rememberScrollState()
    val asOfDate = remember { LocalDate.now() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("patientTwoPageReportDialog"),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumTouchTarget()
                                    .testTag("closeReportDialog"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }

                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (candidates.isNotEmpty()) {
                                    val selectedCount = validSelectedKeys.size
                                    val picturesCd = if (selectedCount > 0) {
                                        stringResource(R.string.report_pictures_selected_cd, selectedCount)
                                    } else {
                                        stringResource(R.string.report_pictures_cd)
                                    }
                                    OutlinedButton(
                                        onClick = { showPicturePicker = true },
                                        enabled = !isPreparingPrint && !isExportingPdf,
                                        shape = RecordShapes.Small,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedCount > 0) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selectedCount > 0) {
                                                FieldFamilies.patient().container
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = if (selectedCount > 0) 10.dp else 0.dp,
                                            vertical = 0.dp,
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .minimumTouchTarget()
                                            .semantics { contentDescription = picturesCd }
                                            .testTag("reportPicturesButton"),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            if (selectedCount > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ) {
                                                    Text(
                                                        text = ltrIsolate(selectedCount.toString()),
                                                        style = RecordType.Tag,
                                                        modifier = Modifier.padding(
                                                            horizontal = 6.dp,
                                                            vertical = 2.dp,
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val shareCd = stringResource(R.string.report_share_cd)
                                val sharePreparingCd = stringResource(R.string.report_share_pdf_preparing)
                                Box {
                                    IconButton(
                                        onClick = {
                                            if (!isExportingPdf) {
                                                showShareMenu = true
                                            }
                                        },
                                        enabled = !isExportingPdf,
                                        modifier = Modifier
                                            .minimumTouchTarget()
                                            .semantics {
                                                contentDescription = if (isExportingPdf) {
                                                    sharePreparingCd
                                                } else {
                                                    shareCd
                                                }
                                            }
                                            .testTag("shareReportButton"),
                                    ) {
                                        if (isExportingPdf) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.report_share_pdf),
                                                    style = RecordType.Action,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            },
                                            onClick = {
                                                showShareMenu = false
                                                if (isExportingPdf) return@DropdownMenuItem
                                                isExportingPdf = true
                                                coroutineScope.launch {
                                                    try {
                                                        val shared = sharePatientReportPdf(
                                                            context = context,
                                                            patient = patient,
                                                            securitySettings = securitySettings,
                                                            selectedPictureKeys = validSelectedKeys,
                                                            figures = figures,
                                                            shareSubject = shareSubject,
                                                            shareChooserTitle = shareChooserTitle,
                                                        )
                                                        if (!shared) {
                                                            showPdfExportFailed()
                                                        }
                                                    } finally {
                                                        isExportingPdf = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .minimumTouchTarget()
                                                .testTag("shareReportPdfItem"),
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.report_share_short_text),
                                                    style = RecordType.Action,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            },
                                            onClick = {
                                                showShareMenu = false
                                                sharePatientShortText(
                                                    context = context,
                                                    patient = patient,
                                                    shareSubject = shareSubject,
                                                    shareChooserTitle = shareChooserTitle,
                                                )
                                            },
                                            modifier = Modifier
                                                .minimumTouchTarget()
                                                .testTag("shareReportTextItem"),
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (isPreparingPrint) return@Button
                                        if (validSelectedKeys.isEmpty()) {
                                            PatientPrintManager.printPatientReport(
                                                context = context,
                                                patient = patient,
                                                settings = securitySettings,
                                                onUnavailable = showPrintUnavailable,
                                            )
                                        } else {
                                            isPreparingPrint = true
                                            coroutineScope.launch {
                                                try {
                                                    val encodedUris = encodeSelectedPictures(
                                                        context = context,
                                                        patientId = patient.id,
                                                        figures = figures,
                                                    )
                                                    PatientPrintManager.printPatientReport(
                                                        context = context,
                                                        patient = patient,
                                                        settings = securitySettings,
                                                        selectedPictureKeys = validSelectedKeys,
                                                        pictureDataUris = encodedUris,
                                                        onUnavailable = showPrintUnavailable,
                                                    )
                                                } finally {
                                                    isPreparingPrint = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isPreparingPrint,
                                    shape = RecordShapes.Small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .minimumTouchTarget()
                                        .testTag("printReportButton"),
                                ) {
                                    if (isPreparingPrint) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = LocalContentColor.current,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(
                                            if (isPreparingPrint) R.string.report_preparing_print else R.string.print_pdf,
                                        ),
                                        style = RecordType.Action,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.report_dialog_kicker),
                                style = RecordType.FieldLabel,
                                color = FieldFamilies.patient().label,
                            )
                            PatientNameText(
                                name = patient.displayName,
                                style = RecordType.HeaderName,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .semantics { contentDescription = pageSwitcherDescription }
                                .testTag("reportPageSwitcher"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            RecordFilterChip(
                                selected = selectedPage == 0,
                                label = stringResource(R.string.report_page_1),
                                onClick = { selectedPage = 0 },
                                tag = "reportPage1Chip",
                                wrap = true,
                                minTouchTarget = true,
                            )
                            RecordFilterChip(
                                selected = selectedPage == 1,
                                label = stringResource(R.string.report_page_2),
                                onClick = { selectedPage = 1 },
                                tag = "reportPage2Chip",
                                wrap = true,
                                minTouchTarget = true,
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(if (selectedPage == 0) page1Scroll else page2Scroll)
                            .padding(RecordSpacing.L)
                            .testTag("reportPageContent"),
                        verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                    ) {
                        if (figures.isNotEmpty()) {
                            Surface(
                                shape = RecordShapes.Small,
                                color = FieldFamilies.patient().container,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reportAttachedImagesSummary"),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.report_attached_images_selected,
                                        figures.size,
                                    ),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                        if (selectedPage == 0) {
                            ReportPageOneSections(
                                patient = patient,
                                securitySettings = securitySettings,
                                asOfDate = asOfDate,
                                figures = figures,
                            )
                        } else {
                            ReportPageTwoSections(
                                patient = patient,
                                securitySettings = securitySettings,
                                asOfDate = asOfDate,
                                figures = figures,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicturePicker && candidates.isNotEmpty()) {
        ReportPicturePicker(
            patientId = patient.id,
            candidates = candidates,
            selectedKeys = validSelectedKeys,
            onToggleKey = { key ->
                selectedPictureKeys = if (key in validSelectedKeys) {
                    validSelectedKeys - key
                } else {
                    validSelectedKeys + key
                }
            },
            onSelectAll = {
                selectedPictureKeys = candidates.mapTo(LinkedHashSet(candidates.size)) { it.key }
            },
            onClear = { selectedPictureKeys = emptySet() },
            onDismiss = { showPicturePicker = false },
        )
    }
}

private fun sharePatientShortText(
    context: Context,
    patient: DemoPatient,
    shareSubject: String,
    shareChooserTitle: String,
) {
    val summary = patientShareText(patient)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
}

private suspend fun sharePatientReportPdf(
    context: Context,
    patient: DemoPatient,
    securitySettings: SecuritySettingsState?,
    selectedPictureKeys: Set<String>,
    figures: List<Figure>,
    shareSubject: String,
    shareChooserTitle: String,
): Boolean {
    val activity = context.findActivity() ?: return false
    return try {
        val encodedUris = encodeSelectedPictures(
            context = context,
            patientId = patient.id,
            figures = figures,
        )
        val pdfFile = exportReportPdf(
            activity = activity,
            patient = patient,
            settings = securitySettings,
            selectedPictureKeys = selectedPictureKeys,
            pictureDataUris = encodedUris,
        ) ?: return false

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, shareSubject, uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
        true
    } catch (ce: CancellationException) {
        throw ce
    } catch (_: Throwable) {
        false
    }
}
