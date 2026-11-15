# Slice 9: Meds tab (flat rows, restyle only)

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s09_meds.html` (revised after the owner's "more elaborating than needed with all these squares"). Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Depends on slice 6: `RecordListDivider`, `PlainField`, `StatusWord` / `StatusTone`. Read them as built.

**Restyle only.** No Meds stage 2 (mission M1b), no new feature, no model change. Same events (`StopMedication`, `RequestDelete`, edit through `startEdit`, picker events), same gates (`canEdit`, `entryEnabled` / `actionsEnabled`), same lists (active, past), same picker behaviour.

**Owner rule:** a medication is ONE row: name and dose on the first line, route and frequency on the second, the status as a plain word at the end; rows split by a hairline; no box per field, no card per drug, no fill except the status word (non-compliant stays an error word). Tap-to-add and the stop bar stay compact and quick.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `ui/patienthub/model/MedicationTiles.kt` (picker rules), `MedicationSheet.kt` and `MedicationForm.kt` (slice 11), `ui/ClinicalPresets.kt` (level 3: `medicationDoseRouteFrequency`, `medicationFrequencyLabel`), `ui/ClinicalBadges.kt` (level 3: `MedicationStatusBadge` stays for other callers).

**Level 3:** none edited; two level-3 helpers are called unchanged.

## Lessons (apply all)

1. First (outermost) `testTag` wins; `medication-<id>` must stay the first tag on the row.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. One `setContent` per test; `performScrollTo()` before asserting below the fold.
4. `MedsTabSheetTest` already measures a lot of this screen: read it before changing any tag or text.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/patienthub/components/MedicationCard.kt` (229 lines) | redraw as a flat row (keep the function name and signature) |
| `ui/patienthub/tabs/MedsTab.kt` (326 lines) | surface background, dividers between rows, header, empty state, show-past row |
| `ui/patienthub/components/MedicationTapToAddSection.kt` (356 lines) | compact restyle: toggle row, tiles, radii |
| `ui/patienthub/components/MedicationStopBar.kt` (159 lines) | radii 10, one-line undo bar |
| `app/src/main/res/values/strings.xml` | change 1 plural, add 1 string |

## Strings

Change `active_prescriptions_title` to sentence case: one "Active prescription (%1$d)", other "Active prescriptions (%1$d)" (the test reads the resource, not a literal). Add `<string name="medication_route_frequency">%1$s, %2$s</string>`. Reuse `prescribe_medication`, `add_medication`, `stop_medication_short`, `stop_medication_description`, `edit_medication_description`, `dosage`, `route`, `frequency`, `start_date`, `adverse_effects`, `notes`, `show_details`, `hide_details`, `state_expanded`, `state_collapsed`, `action_edit`.

## 1. `MedicationCard` becomes a flat row (same signature)

```kotlin
Column(modifier.fillMaxWidth().testTag("medication-${med.id}")) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.weight(1f).minimumTouchTarget().testTag("medicationToggle-${med.id}")
                .semantics { stateDescription = toggleState }
                .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(med.name) }; if (med.dosage.isNotBlank()) append(" " + med.dosage.trim()) },
                 style = RecordType.FieldValue, color = onSurface, modifier = Modifier.testTag("medicationName-${med.id}"))
            if (routeFrequency.isNotBlank()) Text(routeFrequency, RecordType.Secondary, onSurfaceVariant, modifier = Modifier.testTag("medicationInstructions-${med.id}"))
            if (notes.isNotEmpty() && !isExpanded) Text(notes, RecordType.Secondary, onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("medicationNotes-${med.id}"))
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 6.dp).testTag("medicationActions-${med.id}")) {
            StatusWord(status, if (nonCompliant) StatusTone.High else StatusTone.Neutral, Modifier.testTag("medicationStatusBadge"))
            if (canEdit && isOngoingMedication(med.status, med.isNonCompliant) && onStop != null) StopControl(...)
        }
    }
    if (isExpanded) Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlainField(stringResource(R.string.start_date), med.startDate)
        if (!med.adverseNotes.isNullOrBlank()) PlainField(stringResource(R.string.adverse_effects), med.adverseNotes)
        PlainField(stringResource(R.string.notes), med.notes)
        if (canEdit) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) DeleteIconButton(DeletableType.MEDICATION, med, onDelete, enabled = actionsEnabled)
            if (onEdit != null) SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_medication_description, med.name), onEdit, Modifier.testTag("editMedication-${med.id}"), enabled = actionsEnabled)
        }
    }
}
```
- `routeFrequency = stringResource(R.string.medication_route_frequency, med.route.trim(), frequency)` when both are present, else whichever is present. `frequency = medicationFrequencyLabel(med.frequency)` (unchanged).
- `SectionTextAction` needs an `enabled: Boolean = true` parameter (add it in `RecordSectionCard.kt`, passed to the `TextButton`); existing callers unchanged.
- `StopControl`: keep its tag `stopMedication-<id>`, the label tag `stopMedicationLabel-<id>`, the "Stop <drug>" description, the error colour, `minimumTouchTarget()` and the disabled alpha. Drop the stop icon (the word "Stop" in red is the control); text `RecordType.Action.copy(fontWeight = Bold)`.
- Removed: the `Card` and its `surfaceContainerLow` fill, the expand chevron icon (the row itself toggles and exposes its state), the `MedicationStatusBadge` box, the pencil icon, the expanded `FactRow`s and their divider, the dose and route rows inside the opened row (dose is on line 1, route on line 2).
- Edit and delete now appear when the row is opened (one tap more, same events); Stop stays one tap on the closed row.

## 2. `MedsTab`

- `LazyColumn` background `colorScheme.surface`, no `verticalArrangement` spacing; `RecordListDivider()` drawn above every medication row except the first of each list; keys unchanged.
- Header: `Text(pluralStringResource(R.plurals.active_prescriptions_title, n, n), RecordType.SectionTitle, primary, Modifier.testTag("medsActiveTitle"))`; Prescribe `Button(shape = RecordShapes.Small, Modifier.heightIn(min = 48.dp).testTag("addCustomMedicationBtn"))` with the word only.
- Empty state (`medsEmptyState`): no tinted surface; title `RecordType.SectionTitle` onSurface, hint `RecordType.FieldValue` onSurfaceVariant, button `medEmptyPrescribe` 48 dp without the icon.
- `medsNoActive`: `RecordType.FieldValue`, onSurfaceVariant.
- Show past toggle (`medShowPastToggle`): a 48 dp row, text `RecordType.FieldValue.copy(fontWeight = Bold)`, primary; chevron kept (shows state).

## 3. `MedicationTapToAddSection` (compact)

- Toggle (`medPickerToggle`): a 48 dp row above a `RecordListDivider()`, no fill, no "+" icon; text `add_medication` in `RecordType.FieldValue.copy(SemiBold)` primary; chevron kept.
- Panel: search field `shape = RecordShapes.Small`; Common / Recent chips as today (tags `medTab-common`, `medTab-recent`); tiles `Surface(shape = RecordShapes.Small, border = BorderStroke(1.dp, outline), color = surface (active: surfaceContainerHigh))`, min 48 dp, name `RecordType.Action.copy(Bold)`, meta `RecordType.Tag` weight Normal; keep `medTile-<id>`, `medTileMeta-<id>`, `medTileCheck-<id>` (the check icon stays: a test finds it), `medPickerEmpty`, `medTilesGrid`.
- Radii 14 dp become 10 dp.

## 4. `MedicationStopBar`

- Undo (`medStopUndoBar`): `Surface(shape = RecordShapes.Small, color = surfaceContainerHigh)`, one row min 48 dp: message `RecordType.Secondary` onSurface, `TextButton` Undo (`medStopUndo`) 48 dp.
- Error (`medStopError`): `RecordShapes.Small`, errorContainer, title `RecordType.Action.copy(Bold)`, body `RecordType.Secondary`; dismiss `medStopErrorDismiss` 48 dp.
- Progress (`medStopProgress`) unchanged.

## Acceptance checklist

1. All tags in `MedsTabSheetTest`, `MedsTabRestorationTest`, `MedicationStopBarTest`, `DeleteUndoUiTest` and `AncWorkflowTest` still exist with the same meaning (list in the PR).
2. No `Card`, filled `Surface`, `LabeledValue` or `MedicationStatusBadge` inside a medication row (grep). The only coloured text in a row is "Stop" and a non-compliant status.
3. Closed row: line 1 "<name> <dose>", line 2 "<route>, <frequency>", status word at the end, Stop under it when active and editable.
4. Opened row: start date, side effects (if any), notes as plain fields; Edit and Delete at the end.
5. Stop target >= 47.9 dp wide and tall; Edit, Delete, Prescribe, picker toggle, show-past row, tiles, Undo >= 47.9 dp.
6. Stop label contrast >= 4.5:1 on the row in both themes (`#BA1A1A` on `#FFFFFF` 6.46; `#FFB4AB` on `#1A202E` 9.59).
7. 360 dp, font scale 1.3: name and dose wrap, never clipped; status and Stop stay visible.
8. Dark theme renders.

## Tests (written by a different lane from this spec)

### Existing tests that need edits (grep done 2026-11-15)

- `MedsTabSheetTest` line ~616 and ~981: `medicationInstructions-<id>` asserts the exact text "250 mg · Oral · <frequency>". It becomes "Oral, <frequency>" (the dose moved to line 1 inside `medicationName-<id>`). Update both assertions: name node `assertTextContains("250 mg", substring = true)`, instructions `assertTextEquals("Oral, $newFrequency")`.
- `MedsTabSheetTest` near line 1083 to 1092: checks name and instructions are not clipped and that a node is >= 48 dp; keep, switching the size check to `>= 47.9.dp` if it targets the toggle.
- Any test that clicks `editMedication-<id>` or a delete on a closed row must first click `medicationToggle-<id>` to open it (grep `editMedication-` in `MedsTabSheetTest` and `DeleteUndoUiTest`). This is the one interaction change; the events are the same.
- `stopMedicationLabel-<id>` keeps the text "Stop".

### Must pass unchanged

`MedsTabRestorationTest`, `MedicationStopBarTest`, `MedicationFormTest`, `AncWorkflowTest` (`addCustomMedicationBtn`), `PatientHubNavigation`, unit tests.

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabRedesignTest.kt`

Fixtures (exact constructors; values from `DemoFixtures`):
```kotlin
private fun med(id: String, name: String, dosage: String, frequency: String, status: String = "Active", nonCompliant: Boolean = false, notes: String? = null) =
    DemoMedicationRecord(id = id, patientId = "p-meds", name = name, dosage = dosage, route = "Oral", frequency = frequency,
        startDate = "2026-04-04", endDate = null, status = status, notes = notes, isProphylaxis = true, isNonCompliant = nonCompliant)

private fun patient(vararg meds: DemoMedicationRecord) = DemoPatient(
    id = "p-meds", caseId = "M-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null, visits = emptyList(),
    medications = meds.toList(),
)
val iron = med("m1", "Iron Polymaltose Complex", "100 mg", "Once daily", notes = "Routine antenatal iron prophylaxis; well tolerated")
val calcium = med("m2", "Calcium Carbonate + Vitamin D3", "600 mg / 400 IU", "Once daily with meals")
val folic = med("m3", "Folic Acid", "5 mg", "Once daily", status = "Completed")
```
Render `MedsTab` the way `MedsTabRestorationTest` does (copy its host setup, one `setContent`).

Cases:
- `medicationName-m1` text contains "Iron Polymaltose Complex" and "100 mg"; `medicationInstructions-m1` text contains "Oral" and "Once daily" and no "·";
- status "Active" shows as text in the `medicationStatusBadge` node under `medication-m1`, and that node is no taller than one 12 sp line (height <= 20 dp, so no padded box);
- `stopMedication-m1` exists on the closed row, width and height >= 47.9 dp; `editMedication-m1` does not exist until `medicationToggle-m1` is clicked, then exists and is >= 47.9 dp;
- a non-compliant fixture (`nonCompliant = true`, `status = "Patient non-compliant"`) shows "Patient non-compliant";
- no `Card` drawn: the gap between `medication-m1` bottom and `medication-m2` top is <= 2 dp (rows touch, a hairline separates them);
- show past: `medShowPastToggle` >= 47.9 dp; after clicking, `medication-m3` exists;
- 360 dp, font scale 1.3: `medicationName-m2` not clipped (`assertTextNotClipped` from the existing helper).

## Risk

Medium: heavily tested screen. The builder runs `MedsTabSheetTest`, `MedsTabRestorationTest` and `MedicationStopBarTest` first, then the full suite.
