package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.model.parseWeightKg
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordBandCard
import com.sherifradwan.ancnative.ui.components.RecordEmptyCard
import com.sherifradwan.ancnative.ui.components.RecordListHeader
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.components.BedsideVitalsGrid
import com.sherifradwan.ancnative.ui.patienthub.model.deriveVitalsSnapshot

@Composable
fun VisitsTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onStartVisit: (String, String?) -> Unit,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookingWeightKg = remember(patient.visits) {
        patient.visits
            .mapNotNull { parseWeightKg(it.weight) }
            .lastOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("visitsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RecordListHeader(
                title = stringResource(R.string.visits_tab_title, patient.visits.size),
                actionLabel = stringResource(R.string.record_visit_action),
                actionTag = "recordNewVisitBtn",
                onAction = { onStartVisit(patient.id, null) },
                showAction = canEdit && patient.status != "archived",
            )
        }

        if (patient.visits.isEmpty()) {
            item { RecordEmptyCard() }
        } else {
            items(
                items = patient.visits,
                key = { it.id },
            ) { visit ->
                val gaAtVisit = remember(patient.dating, visit.date) {
                    GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, visit.date)
                }
                val gaText = gaAtVisit?.shortWeeksText
                val gaWeeks = gaAtVisit?.weeks
                val parsedFindings = remember(visit.findings) { parseVisitFindings(visit.findings) }

                val vitals = remember(visit.bloodPressure, visit.weight, visit.findings, gaWeeks, bookingWeightKg) {
                    deriveVitalsSnapshot(
                        bloodPressure = visit.bloodPressure,
                        weight = visit.weight,
                        findings = visit.findings,
                        gaWeeks = gaWeeks,
                        bookingWeightKg = bookingWeightKg,
                    )
                }
                val preEclampsiaLabel = stringResource(R.string.pre_eclampsia_risk_label)
                val severeBpLabel = stringResource(R.string.band_severe_bp)
                val raisedBpLabel = stringResource(R.string.band_raised_bp)
                val worstFlag: Pair<String, StatusTone>? = when {
                    vitals.isPreEclampsiaWarning -> preEclampsiaLabel to StatusTone.High
                    vitals.bpLevel == BpLevel.SEVERE -> severeBpLabel to StatusTone.High
                    vitals.bpLevel == BpLevel.RAISED -> raisedBpLabel to StatusTone.Moderate
                    else -> null
                }
                val linked = visit.linkedUltrasoundId != null

                RecordBandCard(
                    title = visit.date,
                    family = FieldFamilies.patient(),
                    modifier = Modifier.testTag("visit-${visit.id}"),
                    chips = if (gaText != null || linked || worstFlag != null) {
                        {
                            gaText?.let {
                                BandChip(ltrIsolate(it), tone = null, modifier = Modifier.testTag("gestationalAgeBadge"))
                            }
                            if (linked) BandChip(stringResource(R.string.scan_linked), tone = null)
                            worstFlag?.let { (text, tone) -> BandChip(text, tone) }
                        }
                    } else {
                        null
                    },
                    actions = if (canEdit) {
                        {
                            DeleteIconButton(
                                type = DeletableType.VISIT,
                                record = visit,
                                onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.VISIT, visit.id)) },
                            )
                            SectionTextAction(
                                text = stringResource(R.string.action_edit),
                                contentDescription = stringResource(R.string.edit_visit),
                                onClick = { onStartVisit(patient.id, visit.id) },
                                modifier = Modifier.testTag("editVisit-${visit.id}"),
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    if (vitals.isPreEclampsiaWarning) {
                        Column(
                            modifier = Modifier
                                .testTag("preEclampsiaWarning-${visit.id}")
                                .semantics(mergeDescendants = true) {},
                        ) {
                            StatusWord(preEclampsiaLabel, StatusTone.High)
                            Text(
                                text = stringResource(
                                    R.string.pre_eclampsia_risk_text,
                                    visit.bloodPressure.orEmpty(),
                                    parsedFindings.urineProtein.orEmpty(),
                                ),
                                style = RecordType.FieldValue,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    BedsideVitalsGrid(vitals = vitals)
                    if (!visit.notes.isNullOrBlank()) {
                        Text(
                            text = visit.notes,
                            style = RecordType.FieldValue,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
