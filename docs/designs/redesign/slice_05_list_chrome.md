# Slice 5: Patient list chrome and the actions sheet

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s05_list_chrome.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0 and 2. Components as built: `ui/components/RecordTokens.kt`, `StatusTag.kt`; the patient card from slice 1 is not changed here.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`.

**Level 3:** `ui/PatientListScreen.kt` is level 3 (clinical_safety). Frontier-tier review plus an independent test review. **Keep its logic untouched:** `PatientDashboardRowProjection`, `projectDashboardRow`, `visiblePatients`, `dashboardPatients`, the `riskFilter` state and its six branches, every count (`allCount`, `highRiskCount`, `mediumRiskCount`, `lowRiskCount`, `deliveredCount`, `archivedCount`, `activeCount`), `sortDashboardPatients` and `DashboardSort`, the events (`StartPatientEditor`, `SearchChanged`, `ClearSearch`), the `BoxWithConstraints` width rule, the `LazyColumn` keys. Only the composables that draw the title row, notice, search field, chips, sort button and empty state change. `DashboardSort.kt` needs no edit (logic only) and is not touched.

## Lessons from slices 0 and 1

- First (outermost) `testTag` wins. The card is found by `patient-<caseId>`; the actions sheet's "Open Workspace" row also carries `workspace-<caseId>` (same tag as on the card, used by `AncWorkflowTest` after the sheet opens). Do not reorder tags on either.
- `LabeledValue(stretch = true)` only inside `LabeledValueRow`; not used here.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/PatientListScreen.kt` (517 lines, level 3) | restyle the drawing code inside `PatientListScreen` only |
| `ui/patientdirectory/PatientActionsModalSheet.kt` (259 lines) | restyle rows and header |
| `app/src/main/res/values/strings.xml` | change 3 values, add 3 strings |

## Strings

Change (owner decision 7, sentence case and "Moderate" to match the card's tag):
- `high_risk_filter` "High Risk" to "High risk"
- `medium_risk_filter` "Medium Risk" to "Moderate risk"
- `low_risk_filter` "Low Risk" to "Low risk"
- `record_visit` "Record Visit" stays; `open_workspace` stays (copy owner decision, not in this slice).

Before changing, grep: `high_risk_filter`, `medium_risk_filter`, `low_risk_filter` are used only by the chips today; no test asserts their text (tests use `riskFilter-*` tags).

Add (moving hard-coded text into resources, text unchanged):
```xml
<string name="no_matching_patients">No matching patients found</string>
<string name="no_patients_in_category">No patients in this category</string>
<string name="case_id_line">Case: %1$s</string>
```
Leave `active_patients_count` ("%1$d Patients") as is: the plural fix is owner decision 8.

## 1. Title row (first `item`)

```kotlin
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f, fill = false)) {
        Text(stringResource(R.string.patients), style = RecordType.HeaderName, color = colorScheme.primary, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.active_patients_count, activeCount), style = RecordType.Secondary, color = colorScheme.onSurfaceVariant, maxLines = 1)
    }
    Spacer(Modifier.width(12.dp))
    Button(onClick = { /* unchanged StartPatientEditor event */ }, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("newPatient")) {
        Text(stringResource(R.string.new_patient), style = RecordType.Action, maxLines = 1, softWrap = false)
    }
}
```
`RecordType.HeaderName` comes from slice 4; if slice 5 lands first, add it to `RecordTokens.kt` exactly as slice 4 specifies (22 sp / 28 sp, Bold) and slice 4 reuses it. Removed: the count pill (`primaryContainer`, 12 dp), the `Add` icon.

## 2. Notice (archived / unarchived)

`OutlinedCard(Modifier.fillMaxWidth(), shape = RecordShapes.Large, border = BorderStroke(1.dp, colorScheme.outlineVariant))` with the same text in `RecordType.Secondary`, `onSurface`, `padding(16.dp)`. Condition unchanged.

## 3. Search

Same `OutlinedTextField`, same tag `patientSearch`, events, label, leading search icon and clear button; only `shape = RecordShapes.Small` (was 24 dp). The clear `IconButton` gets `Modifier.minimumTouchTarget()` (it is an `IconButton`, already 48 dp; the modifier documents it).

## 4. Filter chips

One private composable in the same file:
```kotlin
@Composable
private fun ListFilterChip(selected: Boolean, label: String, onClick: () -> Unit, tag: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = RecordType.Action.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1, softWrap = false) },
        shape = RecordShapes.Small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colorScheme.surface, labelColor = colorScheme.onSurface,
            selectedContainerColor = colorScheme.primary, selectedLabelColor = colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = colorScheme.outline, selectedBorderColor = colorScheme.primary),
        modifier = Modifier.testTag(tag),
    )
}
```
Six calls in today's order with today's labels (`"${stringResource(...)} ($count)"`), selection and tags `riskFilter-all`, `-high`, `-medium`, `-low`, `-delivered`, `-archived`. The `Row` keeps `horizontalScroll` and `spacedBy(8.dp)`. M3 `FilterChip` already reserves a 48 dp touch target around its 32 dp body; do not shrink it. Removed: the per-filter colours (error, secondary, tertiary containers).

## 5. Sort

`TextButton(onClick = { sortMenuExpanded = true }, shape = RecordShapes.Small, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.minimumTouchTarget().testTag("dashboardSortButton"))` containing the same `Text(dashboard_sort_format)` in `RecordType.Action` and the same `KeyboardArrowDown` 16 dp icon (it marks a menu). `DropdownMenu` and its four items, actions and tags unchanged.

## 6. Empty state

`OutlinedCard(Modifier.fillMaxWidth(), shape = RecordShapes.Large, border = BorderStroke(1.dp, colorScheme.outlineVariant))`, inner `Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Center)`, `Text(stringResource(if (state.searchQuery.isNotBlank()) R.string.no_matching_patients else R.string.no_patients_in_category), style = RecordType.FieldValue, color = onSurfaceVariant, textAlign = TextAlign.Center)`. Condition unchanged. Removed: `surfaceVariant` fill.

## 7. List spacing

`LazyColumn` `verticalArrangement = Arrangement.spacedBy(12.dp)` (was 10 dp, matches the 12 dp record gap). Content padding unchanged.

## 8. `PatientActionsModalSheet` (same signature)

- `ModalBottomSheet` unchanged (container surface, default drag handle).
- Column `padding(horizontal = 16.dp, vertical = 8.dp)`, `spacedBy(8.dp)`.
- Header `Row`: `Column(weight(1f))` with `Text(patient.displayName, RecordType.SectionTitle, onSurface)` and `Text(listOfNotNull(stringResource(R.string.case_id_line, patient.caseId), patient.ageText?.takeIf { it.isNotBlank() }).joinToString(", "), RecordType.Secondary, onSurfaceVariant)`; close `IconButton(Modifier.minimumTouchTarget())` with `contentDescription = stringResource(R.string.close)` (was the hard-coded "Close"). This removes today's trailing " · " when age is missing.
- Remove the `HorizontalDivider`.
- One private row composable:
```kotlin
@Composable
private fun SheetActionRow(title: String, tag: String, onClick: () -> Unit, subtitle: String? = null, primary: Boolean = false, danger: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RecordShapes.Small,
        color = if (primary) FieldFamilies.patient().container else colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold), color = when { danger -> colorScheme.error; primary -> colorScheme.primary; else -> colorScheme.onSurface })
            subtitle?.let { Text(it, style = RecordType.Secondary, color = colorScheme.onSurfaceVariant) }
        }
    }
}
```
- Four rows, same order, same conditions, same click bodies (`onDismissRequest()` first, then the event), same tags: Record visit (`actionNewVisit-<caseId>`, primary, hidden when archived), Open workspace (`workspace-<caseId>`), Edit patient (`editPatient-<caseId>`, hidden when archived), Archive or Unarchive (`statusPatient-<caseId>`, `danger = !isArchived`, subtitle as today).
- Removed: leading icons, the "›" texts, the `.clip(...).clickable` pattern (the `Surface(onClick)` gives the ripple and the role), the red `TagHighRisk*` row fill.
- Cancel `TextButton` stays, `Modifier.fillMaxWidth().heightIn(min = 48.dp)`, `RecordType.Action`.

## Acceptance checklist

1. Logic diff: `git diff` of `PatientListScreen.kt` shows no change above `PatientListScreen(` and no change to any `remember { ... }` computation, `when (riskFilter)` branch, count, sort or event inside it.
2. Tags present: `patientListTable`, `newPatient`, `patientSearch`, `riskFilter-*` (6), `dashboardSortButton`, `dashboardSort-*` (4), `actionNewVisit-*`, `workspace-*`, `editPatient-*`, `statusPatient-*`.
3. Chips: one style; selected chip filled primary with onPrimary text; labels "All (n)", "High risk (n)", "Moderate risk (n)", "Low risk (n)", "Delivered (n)", "Archived (n)".
4. Radii in these two files: only 10 dp and 16 dp (search field 10, chips 10, buttons 10, rows 10, cards 16).
5. No icon left in the sheet rows or the New patient button; the search and sort icons remain.
6. Touch targets >= 48 dp: New patient, chips, sort, clear search, close, every sheet row (56 dp), Cancel.
7. Text sizes: nothing under 12 sp; title 22 sp; row titles 16 sp.
8. 360 dp, font scale 1.3: title row does not overlap the New patient button; chips scroll; the sheet header shows "Case: X" without a trailing separator when age is missing.
9. RTL: title at the right, New patient at the left; chip row starts at the right.
10. Dark theme: selected chip `#B4C5FF` with `#002A78` text (7.72:1), unselected readable.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `OwnerFixesUiTest.archivedFilterAndDashboardSort` uses `riskFilter-archived`, `riskFilter-all`, `dashboardSortButton` (asserts "Sort" text, kept), `dashboardSort-name` and card bounds. `AncWorkflowTest` uses `newPatient`, `patientSearch`, `workspace-*`, `patient-*`. `SecurityWorkflowTest` uses `patientListTable`. If any test matched the chip text "Medium Risk", it needs "Moderate risk"; grep found none.

### Must pass unchanged

`OwnerFixesUiTest`, `AncWorkflowTest`, `SecurityWorkflowTest`, `RegistrationPencilUiTest`, `RegistrationFlowStateTest`, `DraftMappingTest`, `DraftPersistenceIntegrationTest`, `EncryptedRecordsTest`, `NegativeControlAndClinicalValidationTest`, `PatientCardRedesignTest`, `DatingGaMarkerSurfacesTest`, unit `DashboardPatientsTest`, `OwnerFixesLogicTest`.

### New test

`app/src/androidTest/java/com/sherifradwan/ancnative/PatientListChromeRedesignTest.kt` (render `PatientListScreen` with an unlocked `AncState`, as `OwnerFixesUiTest` does):
- chip labels as in checklist 3, each chip node height >= 48 dp;
- clicking `riskFilter-medium` selects it and the list shows only Moderate patients (same filter as before: a fixture with one moderate and one high patient);
- empty search shows "No matching patients found"; archived filter with no archived patient shows "No patients in this category";
- `newPatient` >= 48 dp and has no icon child (no node with an icon role under it);
- actions sheet: open via `patientMenu-<caseId>`; rows `actionNewVisit-`, `workspace-`, `editPatient-`, `statusPatient-` each >= 56 dp; archived fixture hides `actionNewVisit-` and `editPatient-`; header text equals "Case: <id>" when `ageText` is null (no trailing separator);
- RTL: `newPatient` left of the "Patients" title by bounds.

## Risk

Medium-high: level-3 file on the home screen. The builder edits drawing code only; the reviewer diffs the logic region line by line (checklist 1). Full emulator suite before any push.
