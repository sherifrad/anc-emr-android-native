# Slice 6: Visits tab (band cards) and the shared band-card pieces

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s06_visits.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt`, `RecordSectionCard.kt` (`SectionTextAction`), `StatusTag.kt`, `MetricCell.kt`. Read them; do not re-spec them.

**Owner direction for record lists (2026-11-15):** mock D's tinted box per field was "too many squares", the flat list "too plain". The middle (the "one card per medication" Option A the owner preferred on the withdrawn meds options page; the look is reproduced in `mocks/mock_s06_visits.html`): ONE container per record; a family-colour header band that holds filled status-word chips, the record title and the one-tap actions; a quiet white body with plain label-over-value text. No tinted box per field, no box inside a box. Tinted `LabeledValue` boxes stay only where the user edits or where a few key facts sit (Overview).

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `model/VisitEntryLogic.kt`, `ui/patienthub/model/BedsideVitalsUi.kt`, `ui/ClinicalFormat.kt` (level 3), `ui/ClinicalBadges.kt` (level 3), `VisitEditor.kt` (slice 14).

**Level 3:** no level-3 Kotlin file is edited. The `bp_alert_*` string change shows wherever `BpAlertLabel` (level 3 code) draws it: hub header, visit editor.

**Visual only.** Same parsing, levels, alerts, events, `canEdit` and archived gates, list order and keys.

## Owner decisions that apply

- 6: BP alert copy loses the triangle and middle dots.
- 9: GA reads "28+0 wks" everywhere (`shortWeeksText`).
- 11 (spirit): only what needs attention gets a coloured chip or word; normal values get none.

## Lessons from slices 0 to 5 (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. `ltrIsolate` wraps values in U+2066 / U+2069: match values with `substring = true`.
4. One `setContent` per test; `performScrollTo()` before asserting anything below the fold of a `LazyColumn`.
5. `strings.xml` values starting with `?` must be escaped (`\?`).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/components/RecordBandCard.kt` | new: `RecordBandCard` (shared by slices 6 to 8) |
| `ui/components/MetricCell.kt` | add `PlainField` |
| `ui/components/StatusTag.kt` | add `StatusTone`, `StatusWord` and `BandChip` |
| `ui/patienthub/tabs/VisitsTab.kt` (267 lines) | restyle to band cards |
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

### `BandChip` (in `StatusTag.kt`)

```kotlin
@Composable
fun BandChip(text: String, tone: StatusTone?, modifier: Modifier = Modifier, family: FieldFamily = currentFieldFamily()) {
    val dark = isDarkTheme()
    val (bg, border, fg) = when (tone) {
        null -> Triple(MaterialTheme.colorScheme.surface, Color.Transparent, family.label)            // info: GA, "Scan linked", "2 photos"
        StatusTone.Neutral -> Triple(MaterialTheme.colorScheme.surface, Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
        StatusTone.Moderate -> if (dark) Triple(StatusModerateDarkBg, StatusModerateDarkBorder, StatusModerateDarkText) else Triple(StatusModerateBg, StatusModerateBorder, StatusModerateText)
        StatusTone.High -> if (dark) Triple(StatusHighDarkBg, StatusHighDarkBorder, StatusHighDarkText) else Triple(StatusHighBg, StatusHighBorder, StatusHighText)
    }
    Surface(shape = RecordShapes.Small, color = bg, border = BorderStroke(1.dp, border), modifier = modifier.heightIn(min = 24.dp)) {
        Text(text, style = RecordType.Tag.copy(fontWeight = FontWeight.Bold), color = fg, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}
```
Contrast: info chip label on white 8.60 to 10.07 (dark 9.44 to 10.58), neutral 10.00 / 9.47, amber 7.19 / 9.44, red 7.40 / 10.19.

### `RecordBandCard` (new `ui/components/RecordBandCard.kt`)

```kotlin
@Composable
fun RecordBandCard(
    title: String,
    family: FieldFamily,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    chips: (@Composable FlowRowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(modifier.fillMaxWidth(), shape = RecordShapes.Large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        CompositionLocalProvider(LocalFieldFamily provides family) {
            Column(Modifier.fillMaxWidth().background(family.container).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                chips?.let { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = it) }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = RecordType.CardName, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).semantics { heading() })
                    actions?.invoke(this)
                }
                subtitle?.let { Text(it, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp)) }
            }
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}
```
One container; the band is the card's own top area (no border, no inset), the body is plain. Never put a `LabeledValue`, a filled `Surface` or another card inside `content`. Light band text on `family.container`: 14.19 to 14.54; dark 9.75 to 10.88.

## 2. Strings

Change (owner decision 6): `bp_alert_raised` to "Raised BP (≥140/90), recheck"; `bp_alert_severe` to "Severe BP (≥160/110), urgent". `PatientPrintHtmlGenerator.kt` keeps its own constants (`PrintBpAlertTest` unchanged).

Add:
```xml
<string name="visits_tab_title">Antenatal visits (%1$d)</string>
<string name="record_visit_action">Record visit</string>
<string name="no_records">No records</string>
<string name="scan_linked">Scan linked</string>
<string name="band_severe_bp">Severe BP</string>
<string name="band_raised_bp">Raised BP</string>
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

- `LazyColumn(modifier.fillMaxSize().testTag("visitsTabContent"), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp))` on the page background; keys unchanged.
- Header item: `Row(Modifier.fillMaxWidth(), SpaceBetween, CenterVertically)`: `Text(stringResource(R.string.visits_tab_title, patient.visits.size), RecordType.SectionTitle, primary, Modifier.weight(1f, fill = false))`; same gate `Button(onClick = { onStartVisit(patient.id, null) }, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("recordNewVisitBtn")) { Text(record_visit_action, RecordType.Action) }`.
- Empty: `OutlinedCard(shape = RecordShapes.Large, border = 1.dp outlineVariant)` with `Text(no_records, RecordType.FieldValue, onSurfaceVariant, Modifier.padding(16.dp))`.
- Visit item (keep every `remember`; GA text becomes `gaAtVisit?.shortWeeksText`):
```kotlin
RecordBandCard(
    title = visit.date,
    family = FieldFamilies.patient(),
    modifier = Modifier.testTag("visit-${visit.id}"),
    chips = if (gaText != null || linked || worstFlag != null) {
        {
            gaText?.let { BandChip(ltrIsolate(it), tone = null, modifier = Modifier.testTag("gestationalAgeBadge")) }
            if (visit.linkedUltrasoundId != null) BandChip(stringResource(R.string.scan_linked), tone = null)
            worstFlag?.let { (text, tone) -> BandChip(text, tone) }
        }
    } else null,
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
- `worstFlag` (display only, derived from values already computed): pre-eclampsia warning -> (`pre_eclampsia_risk_label`, High); else `vitals.bpLevel == SEVERE` -> (`band_severe_bp` "Severe BP", High); else `RAISED` -> (`band_raised_bp` "Raised BP", Moderate); else null. Add those two strings.
- The GA chip carries `gestationalAgeBadge`, so `DatingGaMarkerSurfacesTest.visitGaBadgesNeverShowTheMarkerEvenForADiscordantPatient` keeps counting it.
- Removed: the tinted `Card`, the header `BpAlertLabel`, the "Scan Linked" secondary pill, the bottom action row, the `Add` icon, the filled pre-eclampsia box and its icon.

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
9. Dark theme renders the chips and words in dark colours.
10. A visit with severe BP shows the red "Severe BP" chip in its band; a raised one the amber "Raised BP"; a normal visit no flag chip.

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
- `bp = "145/92"`: "Raised, recheck" in the body and "Raised BP" in the band; `bp = "165/115"`: "Severe, urgent" and "Severe BP"; the normal visit has neither band chip.
- `bp = "150/100"`, `findings = "Urine protein 2+"`: `preEclampsiaWarning-v1` displayed with "Pre-eclampsia risk".
- `findings = "FHR 100"`: "Bradycardia (<110)".
- `bp = null`: "Not recorded" under "Blood pressure".
- no node text contains "▲", "✓" or "⚠".
- `editVisit-v1` and `delete-visit-v1` height >= 47.9 dp; `canEdit = false`: neither exists.
- empty patient: "No records".
- RTL (inside the single `setContent`): `editVisit-v1` left of the date heading by bounds.

## Risk

Medium. Four new shared pieces (`RecordBandCard`, `BandChip`, `PlainField`, `StatusWord`) used by slices 7 and 8; review them first.
