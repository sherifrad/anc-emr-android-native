package com.sherifradwan.ancnative.ui.report

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientTwoPageReportDialog(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareSubject = stringResource(R.string.report_share_subject_format, patient.displayName)
    val shareChooserTitle = stringResource(R.string.report_share_chooser_title)
    val pageSwitcherDescription = stringResource(R.string.report_pages_tab_group)
    var selectedPage by rememberSaveable(patient.id) { mutableIntStateOf(0) }
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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = {
                                        val summary = patientShareText(patient)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                            putExtra(Intent.EXTRA_TEXT, summary)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                                    },
                                    modifier = Modifier
                                        .minimumTouchTarget()
                                        .testTag("shareReportButton"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.report_share_cd),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                Button(
                                    onClick = {
                                        PatientPrintManager.printPatientReport(
                                            context = context,
                                            patient = patient,
                                            settings = securitySettings,
                                            onUnavailable = {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.report_print_unavailable),
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            },
                                        )
                                    },
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
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.print_pdf),
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
                        if (selectedPage == 0) {
                            ReportPageOneSections(
                                patient = patient,
                                securitySettings = securitySettings,
                                asOfDate = asOfDate,
                            )
                        } else {
                            ReportPageTwoSections(
                                patient = patient,
                                securitySettings = securitySettings,
                                asOfDate = asOfDate,
                            )
                        }
                    }
                }
            }
        }
    }
}
