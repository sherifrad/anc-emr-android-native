package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.editPatientEvent
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientActionsModalSheet(
    patient: DemoPatient,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArchived = patient.status == "archived"

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Patient Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.displayName,
                        style = RecordType.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = listOfNotNull(
                            stringResource(R.string.case_id_line, patient.caseId),
                            patient.ageText?.takeIf { it.isNotBlank() },
                        ).joinToString(", "),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }

            // Action 1: Fast-Track Record Visit (Active patients only)
            if (!isArchived) {
                SheetActionRow(
                    title = stringResource(R.string.record_visit),
                    tag = "actionNewVisit-${patient.caseId}",
                    onClick = {
                        onDismissRequest()
                        onEvent(
                            AncEvent.StartVisit(
                                patientId = patient.id,
                                visitId = null,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    primary = true,
                )
            }

            // Action 2: Open Chart & Workspace
            SheetActionRow(
                title = stringResource(R.string.open_workspace),
                tag = "workspace-${patient.caseId}",
                onClick = {
                    onDismissRequest()
                    onEvent(AncEvent.OpenWorkspace(patient.id))
                },
            )

            // Action 3: Edit Demographics (Active patients only)
            if (!isArchived) {
                SheetActionRow(
                    title = stringResource(R.string.edit_patient),
                    tag = "editPatient-${patient.caseId}",
                    onClick = {
                        onDismissRequest()
                        editPatientEvent(patient, UUID.randomUUID().toString(), UUID.randomUUID().toString())?.let(onEvent)
                    },
                )
            }

            // Action 4: Archive / Restore Patient
            SheetActionRow(
                title = stringResource(if (isArchived) R.string.unarchive_patient else R.string.archive_patient),
                subtitle = stringResource(if (isArchived) R.string.unarchive_patient_subtitle else R.string.archive_patient_subtitle),
                tag = "statusPatient-${patient.caseId}",
                onClick = {
                    onDismissRequest()
                    onEvent(
                        AncEvent.StartStatusEditor(
                            patientId = patient.id,
                            targetStatus = if (isArchived) "active" else "archived",
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                },
                danger = !isArchived,
            )

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = onDismissRequest,
                shape = RecordShapes.Small,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = RecordType.Action,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetActionRow(
    title: String,
    tag: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    primary: Boolean = false,
    danger: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RecordShapes.Small,
        color = if (primary) FieldFamilies.patient().container else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tag),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    danger -> MaterialTheme.colorScheme.error
                    primary -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
