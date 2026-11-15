# Slice 6: Visits tab (flat list) and the shared flat-list pieces

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s06_visits.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt`, `RecordSectionCard.kt` (`SectionTextAction`), `StatusTag.kt`, `MetricCell.kt`. Read them; do not re-spec them.

**Owner rule for lists (2026-11-15):** "more elaborating than needed with all these squares". Dense lists (visits, scans, labs, medications) are flat rows: a heading, label-over-value in plain text, hairline dividers. No card per record, no box inside a box, no tinted `LabeledValue` box per field, no coloured fill except a status word. Tinted `LabeledValue` boxes stay only where the user edits or where a few key facts sit (Overview).

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `model/VisitEntryLogic.kt`, `ui/patienthub/model/BedsideVitalsUi.kt`, `ui/ClinicalFormat.kt` (level 3), `ui/ClinicalBadges.kt` (level 3), `VisitEditor.kt` (slice 14).

**Level 3:** no level-3 Kotlin file is edited. The `bp_alert_*` string change shows wherever `BpAlertLabel` (level 3 code) draws it: hub header, visit editor.

**Visual only.** Same parsing, levels, alerts, events, `canEdit` and archived gates, list order and keys.

## Owner decisions that apply

- 6: BP alert copy loses the triangle and middle dots.
- 9: GA reads "28+0 wks" everywhere (`shortWeeksText`).
- 11 (spirit) and the list rule above: only what needs attention gets a coloured word; normal values get none.

## Lessons from slices 0 to 5 (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. `ltrIsolate` wraps values in U+2066 / U+2069: match values with `substring = true`.
4. One `setContent` per test; `performScrollTo()` before asserting anything below the fold of a `LazyColumn`.
5. `strings.xml` values starting with `?` must be escaped (`\?`).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/components/RecordList.kt` | new: `RecordListItem`, `RecordListDivider` (shared by slices 6 to 9) |
| `ui/components/MetricCell.kt` | add `PlainField` |
| `ui/components/StatusTag.kt` | add `StatusTone` and `StatusWord` |
| `ui/patienthub/tabs/VisitsTab.kt` (267 lines) | restyle to a flat list |
| `ui/patienthub/components/BedsideVitalsGrid.kt` (265 lines) | rewrite drawing on `PlainField` and `StatusWord` (same signature) |
| `app/src/main/res/values/strings.xml` | change 2 values, add strings |

## 1. Shared pieces

### `StatusWord` (in `StatusTag.kt`)

```kotlin
enum class StatusTone { High, Moderate, Neutral }

@Composable
fun StatusWord(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val dark = isDarkTheme()
    val color = when (tone) {
        StatusTone.High -> MaterialTheme.colorScheme.error
        StatusTone.Moderate -> if (dark) StatusModerateDarkText else StatusModerateText
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = RecordType.Tag.copy(fontWeight = if (tone == StatusTone.Neutral) FontWeight.SemiBold else FontWeight.Bold), color = color, modifier = modifier)
}
```
No fill, no border. Contrast on surface: error 6.46 / dark 9.59; amber 8.18 / dark 11.10; neutral 10.00 / dark 9.47.

### `PlainField` (in `MetricCell.kt`)

```kotlin
@Composable
fun PlainField(label: String, value: String?, modifier: Modifier = Modifier, emphasis: Boolean = false, missingText: String = stringResource(R.string.not_recorded), extra: (@Composable () -> Unit)? = null) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = RecordType.MetricLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (value.isNullOrBlank()) Text(missingText, style = RecordType.Missing, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Text(value, style = if (emphasis) RecordType.MetricValue else RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurface)
        extra?.invoke()
    }
}
```
Values wrap (no `maxLines`), unlike `MetricValue`. No box.

### `RecordListItem` and `RecordListDivider` (new `ui/components/RecordList.kt`)

```kotlin
@Composable
fun RecordListItem(title: String, modifier: Modifier = Modifier, subtitle: String? = null, actions: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
            actions?.invoke(this)
        }
        subtitle?.let { Text(it, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        content()
    }
}

@Composable
fun RecordListDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
```
The tab's `LazyColumn` draws `RecordListDivider()` between items (not inside items), so items stay lazy.

## 2. Strings

Change (owner decision 6): `bp_alert_raised` to "Raised BP (≥140/90), recheck"; `bp_alert_severe` to "Severe BP (≥160/110), urgent". `PatientPrintHtmlGenerator.kt` keeps its own constants (`PrintBpAlertTest` unchanged).

Add:
```xml
<string name="visits_tab_title">Antenatal visits (%1$d)</string>
<string name="record_visit_action">Record visit</string>
<string name="no_records">No records</string>
<string name="scan_linked">scan linked</string>
<string name="pre_eclampsia_risk_label">Pre-eclampsia risk</string>
<string name="pre_eclampsia_risk_text">Elevated BP (%1$s) with proteinuria (%2$s). Clinical evaluation required.</string>
<string name="vital_blood_pressure">Blood pressure</string>
<string name="vital_weight">Weight</string>
<string name="vital_fetal_heart_rate">Fetal heart rate</string>
<string name="vital_fundal_height">Fundal height</string>
<string name="vital_bp_raised">Raised, recheck</string>
<string name="vital_bp_severe">Severe, urgent</string>
<string name="vital_fhr_brady">Bradycardia (&lt;110)</string>
<string name="vital_fhr_tachy">Tachycardia (&gt;160)</string>
<string name="vital_sfh_large">Large for dates</string>
<string name="vital_sfh_small">Small for dates</string>
<string name="unit_mmhg">%1$s mmHg</string>
<string name="unit_bpm">%1$s bpm</string>
<string name="unit_cm">%1$s cm</string>
```
Reuse `action_edit`, `edit_visit`, `not_recorded`. Keep `record_visit` ("Record Visit", used by the actions sheet).

## 3. `VisitsTab` (same signature)

- `LazyColumn(modifier.fillMaxSize().background(colorScheme.surface).testTag("visitsTabContent"), contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 48.dp))`, no `verticalArrangement` spacing (dividers separate).
- Header item: `Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), SpaceBetween, CenterVertically)`: `Text(stringResource(R.string.visits_tab_title, patient.visits.size), RecordType.SectionTitle, primary, Modifier.weight(1f, fill = false))`; same gate `Button(onClick = { onStartVisit(patient.id, null) }, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("recordNewVisitBtn")) { Text(record_visit_action, RecordType.Action) }`.
- Empty: `Text(no_records, RecordType.FieldValue, onSurfaceVariant, Modifier.padding(vertical = 16.dp))`.
- Items: `itemsIndexed(patient.visits, key = { _, v -> v.id }) { index, visit -> if (index > 0) RecordListDivider(); VisitRow(...) }` (or `items` with a divider drawn above all but the first).
- `VisitRow` (keep every `remember` computation; GA text becomes `gaAtVisit?.shortWeeksText`):
```kotlin
RecordListItem(
    title = visit.date,
    subtitle = listOfNotNull(gaText?.let(::ltrIsolate), if (visit.linkedUltrasoundId != null) stringResource(R.string.scan_linked) else null).joinToString(", ").ifEmpty { null },
    modifier = Modifier.testTag("visit-${visit.id}"),
    actions = if (canEdit) {
        {
            DeleteIconButton(type = DeletableType.VISIT, record = visit, onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.VISIT, visit.id)) })
            SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_visit), { onStartVisit(patient.id, visit.id) }, Modifier.testTag("editVisit-${visit.id}"))
        }
    } else null,
) {
    if (vitals.isPreEclampsiaWarning) Column(Modifier.testTag("preEclampsiaWarning-${visit.id}").semantics(mergeDescendants = true) {}) {
        StatusWord(stringResource(R.string.pre_eclampsia_risk_label), StatusTone.High)
        Text(stringResource(R.string.pre_eclampsia_risk_text, visit.bloodPressure.orEmpty(), parsedFindings.urineProtein.orEmpty()), RecordType.FieldValue, onSurface)
    }
    BedsideVitalsGrid(vitals = vitals)
    if (!visit.notes.isNullOrBlank()) Text(visit.notes, RecordType.FieldValue, onSurface)
}
```
- GA tag for the marker test: the subtitle `Text` gets `Modifier.testTag("gestationalAgeBadge")` when GA is known (`RecordListItem` takes an optional `subtitleTag: String?`; add it). `DatingGaMarkerSurfacesTest.visitGaBadgesNeverShowTheMarkerEvenForADiscordantPatient` counts that tag >= 1 and markers == 0.
- Removed: the `Card`, the header `BpAlertLabel` (the vitals show the same level as a word), the "Scan Linked" pill, the bottom action row, the `Add` icon, the pre-eclampsia filled box and warning icon.

## 4. `BedsideVitalsGrid(vitals, modifier, compact = false)`

No surface, border or dividers.
```kotlin
Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PlainField(vital_blood_pressure, vitals.bpText?.let { ltrIsolate(unit_mmhg(it)) }, Modifier.weight(1f), emphasis = true, extra = bpWord)
        PlainField(vital_weight, formatWeightKg(vitals.weightText)?.let(::ltrIsolate), Modifier.weight(1f), emphasis = true, extra = vitals.weightDeltaText?.let { { Text(it, RecordType.Secondary, onSurfaceVariant) } })
    }
    if (hasFhr || hasLegacySfh) Row(... same ...) {
        if (hasFhr) PlainField(vital_fetal_heart_rate, ltrIsolate(unit_bpm(fhrText)), Modifier.weight(1f), emphasis = true, extra = fhrWord) else Spacer(Modifier.weight(1f))
        if (hasLegacySfh) PlainField(vital_fundal_height, ltrIsolate(unit_cm(sfhCm)), Modifier.weight(1f), emphasis = true, extra = sfhWord) else Spacer(Modifier.weight(1f))
    }
}
```
- Words: BP SEVERE `StatusWord(vital_bp_severe, High)`, RAISED `StatusWord(vital_bp_raised, Moderate)`, NORMAL none. FHR BRADYCARDIA / TACHYCARDIA `High`, NORMAL none. SFH DISCREPANT_LARGE `Moderate`, DISCREPANT_SMALL `High`, CONCORDANT none.
- Missing BP or weight shows "Not recorded" (was "-").
- Removed: "Normal ✓", "▲", "⚠️", "✓", "▼", filled level pills, `TagNormalRisk*` use, 11 sp labels.

## Acceptance checklist

1. Tags: `visitsTabContent`, `recordNewVisitBtn` (gate unchanged), `visit-<id>`, `editVisit-<id>` and `delete-visit-<id>` (canEdit), `preEclampsiaWarning-<id>` when the rule fires, `gestationalAgeBadge` when GA known.
2. No `Card`, `Surface` with a fill, or `LabeledValue` inside a visit row (code review: grep the two files).
3. No text containing "▲", "▼", "✓", "⚠" or "·"; the weight change keeps today's "Δ +x.x kg".
4. Flagged values show a coloured word; normal values none.
5. GA text equals `shortWeeksText`; no discordance marker in visits.
6. Edit, delete, Record visit >= 47.9 dp.
7. 360 dp, font scale 1.3: the two vitals columns do not overlap; words wrap inside their column.
8. RTL: title right, actions left; values isolated.
9. Dark theme renders the words in dark colours.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `DatingGaMarkerSurfacesTest` keeps working through the `gestationalAgeBadge` subtitle tag. If `VisitEntryDesignTest` asserts the old "▲ Raised BP" text, update it to the new string (list it in the PR).

### Must pass unchanged

`DatingGaMarkerSurfacesTest`, `DeleteUndoUiTest` (visit kind), `PatientHubNavigation`, `HubHeaderRedesignTest`, `AncWorkflowTest`, unit tests (`VisitEntryLogicTest`, `VisitVitalsTest`, `PatientHubPureModelTest`, `PrintBpAlertTest`).

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/VisitsTabRedesignTest.kt`

Fixtures (exact constructors):
```kotlin
private fun visit(id: String, date: String, bp: String?, weight: String?, findings: String?, notes: String? = null, linked: String? = null) =
    DemoVisit(id = id, patientId = "p-visits", date = date, bloodPressure = bp, weight = weight, findings = findings, notes = notes, linkedUltrasoundId = linked)

private fun patient(vararg visits: DemoVisit) = DemoPatient(
    id = "p-visits", caseId = "V-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null,
    visits = visits.toList(),
    dating = DemoPatientDating(patientId = "p-visits", lmpDate = "2026-01-10", ultrasoundEdd = null, agreedEdd = null, datingMethod = "LMP", notes = null),
)
```
Render once per test: `compose.setContent { AncTheme { VisitsTab(patient = p, canEdit = true, onStartVisit = { _, _ -> }, onEvent = {}) } }`.

Cases (thresholds from `bpLevel`: severe >= 160 or >= 110, raised >= 140 or >= 90; pre-eclampsia needs raised BP plus urine protein with "+" or "Trace"; FHR < 110 brady, > 160 tachy):
- `visit("v1", "2026-07-25", "120/78", "71.5 kg", "FH 28 cm")`: labels "Blood pressure", "Weight", "Fundal height"; `hasText("120/78 mmHg", substring = true)`; no "Raised, recheck" or "Severe, urgent"; `gestationalAgeBadge` text contains "28+0 wks".
- `bp = "145/92"`: "Raised, recheck"; `bp = "165/115"`: "Severe, urgent".
- `bp = "150/100"`, `findings = "Urine protein 2+"`: `preEclampsiaWarning-v1` displayed with "Pre-eclampsia risk".
- `findings = "FHR 100"`: "Bradycardia (<110)".
- `bp = null`: "Not recorded" under "Blood pressure".
- no node text contains "▲", "✓" or "⚠".
- `editVisit-v1` and `delete-visit-v1` height >= 47.9 dp; `canEdit = false`: neither exists.
- empty patient: "No records".
- RTL (inside the single `setContent`): `editVisit-v1` left of the date heading by bounds.

## Risk

Medium. Three new shared pieces used by slices 7 to 9; review them first.
