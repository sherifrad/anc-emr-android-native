package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.components.RecordEmptyCard
import com.sherifradwan.ancnative.ui.components.RecordListHeader
import com.sherifradwan.ancnative.ui.patienthub.components.LabReportItem
import com.sherifradwan.ancnative.ui.patienthub.model.groupLabsIntoReports
import java.util.UUID

@Composable
fun LabsTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onAddInvestigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reports = remember(patient.labInvestigations) {
        groupLabsIntoReports(patient.labInvestigations)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("labsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RecordListHeader(
                title = stringResource(R.string.labs_tab_title, reports.size),
                actionLabel = stringResource(R.string.add_test_action),
                actionTag = "addInvestigationBtn",
                onAction = onAddInvestigation,
                showAction = canEdit && patient.status != "archived",
            )
        }

        if (reports.isEmpty()) {
            item { RecordEmptyCard() }
        } else {
            items(
                items = reports,
                key = { "${it.date}-${it.labName}" },
            ) { report ->
                LabReportItem(
                    report = report,
                    patient = patient,
                    canEdit = canEdit,
                    onEditTest = { test ->
                        onEvent(
                            AncEvent.StartLabEditor(
                                patientId = patient.id,
                                labId = test.id,
                                newLabId = test.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    onDeleteTest = { testId ->
                        onEvent(AncEvent.RequestDelete(patient.id, DeletableType.LAB, testId))
                    },
                    onAddTestToReport = onAddInvestigation,
                )
            }
        }
    }
}
