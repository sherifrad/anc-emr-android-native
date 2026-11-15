# Slice 8: Labs tab (band cards)

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s08_labs.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Depends on slice 6: `RecordBandCard`, `BandChip`, `StatusWord` / `StatusTone`, the `no_records` string. Read them as built.

**Owner direction for record lists (2026-11-15):** one container per record (`RecordBandCard`): a family-colour header band holding filled status-word chips (`BandChip`), the title and one-tap actions; a quiet white body with plain label-over-value text (`PlainField`). No tinted box per field, no box inside a box. Labs use the Background family (sand) band.

**The grouping rule stays exactly as it is** (lab fixes `2a7fbbb` and `9a0cc02` are in this branch): `partitionReportIntoCompositeGroups` makes a panel only when at least two member tests match; a lone member goes to "Individual Investigations"; `groupLabsIntoReports` groups by date and lab name. Do not edit `ui/patienthub/model/LabReportGrouping.kt`.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `LabReportGrouping.kt`, `LabEditor.kt`, `LabBatchScreen.kt` (slice 16), `ClinicalPresets.kt` (level 3).

**Level 3:** none.

**Visual only.** Same reports, groups, order, panel open state (`rememberSaveable(group.title)`, open by default), tap-to-edit (`clickable(enabled = canEdit)`), delete, add.

## Owner decisions that apply

- 9: GA in the report line reads "28+0 wks" (`shortWeeksText`).

## Lessons (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. One `setContent` per test; `performScrollTo()` before asserting below the fold.
4. `DemoLabInvestigation` fields: `testCategory`, `testName`, `result`, `unit`, `referenceRange`, `isAbnormal`, `notes`, `labName` (no `status`, no `category`). Labs are `DemoPatient.labInvestigations`.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/patienthub/tabs/LabsTab.kt` (128 lines) | band cards, header, empty state |
| `ui/patienthub/components/LabReportItem.kt` (383 lines) | restyle `LabReportItem` and `LabRowItem` (same signatures) |
| `app/src/main/res/values/strings.xml` | add strings and 2 plurals |

## Strings (add)

```xml
<string name="labs_tab_title">Laboratory reports (%1$d)</string>
<string name="add_test_action">Add test</string>
<string name="add_test_to_report">Add a test to this report</string>
<string name="lab_report_default_name">Laboratory report</string>
<string name="lab_all_normal">All normal</string>
<string name="lab_group_normal">Normal</string>
<string name="lab_abnormal">Abnormal</string>
<string name="lab_individual_investigations">Individual investigations</string>
<string name="lab_ref_line">%1$s, ref %2$s</string>
<string name="delete_test">Delete test</string>
<plurals name="lab_tests_count">
    <item quantity="one">%1$d test</item>
    <item quantity="other">%1$d tests</item>
</plurals>
<plurals name="lab_abnormal_count">
    <item quantity="one">%1$d abnormal</item>
    <item quantity="other">%1$d abnormal</item>
</plurals>
```
Panel titles come from the model, unchanged.

## 1. `LabsTab` (same signature)

- `LazyColumn(modifier.fillMaxSize().testTag("labsTabContent"), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`; keys `"${it.date}-${it.labName}"` as today.
- Header and empty state as slice 6: `labs_tab_title` with `reports.size`, button `add_test_action` with tag `addInvestigationBtn` and the same `onAddInvestigation` and gate.

## 2. `LabReportItem` (same signature)

```kotlin
RecordBandCard(
    title = report.date,
    family = FieldFamilies.background(),
    subtitle = subtitle,
    modifier = modifier.testTag("labReportCard-${report.date}"),
    chips = {
        if (report.isAnyAbnormal) BandChip(pluralStringResource(R.plurals.lab_abnormal_count, report.abnormalCount, report.abnormalCount), StatusTone.High)
        else BandChip(stringResource(R.string.lab_all_normal), StatusTone.Neutral)
    },
    actions = if (canEdit) {
        { SectionTextAction(stringResource(R.string.add_test_action), stringResource(R.string.add_test_to_report), onAddTestToReport, Modifier.testTag("addTestToReportBtn-${report.date}")) }
    } else null,
) {
    compositeGroups.forEach { group -> if (group.isComposite) PanelGroup(group, ...) else IndividualGroup(group, showHeading = compositeGroups.size > 1, ...) }
}
```
- `subtitle = listOfNotNull(report.labName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lab_report_default_name), pluralStringResource(R.plurals.lab_tests_count, report.totalCount, report.totalCount), gaShort?.let(::ltrIsolate)).joinToString(", ")`; `gaShort = patient.dating?.let { GestationalAgeEngine.calculateGestationalAgeAt(it, report.date) }?.shortWeeksText`.
- Removed: the `Card` and its elevation, the old surfaceVariant header band (replaced by the sand band), the `HorizontalDivider` under it, the "N ABNORMAL ▲" and "ALL NORMAL ✓" capsules, the footer "+ Add Another Test to this Report" (same handler as the header action), the `Add` icon.

### `PanelGroup` (private): a sub-heading, not a box

- Header `Row(Modifier.fillMaxWidth().minimumTouchTarget().clickable(role = Role.Button) { isExpanded = !isExpanded }.semantics { stateDescription = expanded or collapsed (reuse `state_expanded` / `state_collapsed`) }, verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(8.dp))`: `Text("${group.title} (${group.items.size})", RecordType.Action.copy(fontWeight = FontWeight.Bold), primary, Modifier.weight(1f))`; `StatusWord(plural lab_abnormal_count, High)` or `StatusWord(lab_group_normal, Neutral)`; chevron `KeyboardArrowUp` when open, `KeyboardArrowDown` when closed, 20 dp, primary, `contentDescription = null`.
- Open: the `LabRowItem`s directly below (no surface, no border, no extra indent).

### `IndividualGroup` (private)

When `showHeading`: `Text(stringResource(R.string.lab_individual_investigations), RecordType.Action.copy(fontWeight = FontWeight.Bold), primary, Modifier.padding(top = 4.dp))` (was uppercase "INDIVIDUAL INVESTIGATIONS"). Then the rows.

## 3. `LabRowItem(lab, canEdit, onEdit, onDelete, modifier, showDivider: Boolean = true)` (one new trailing parameter with a default; callers pass `false` for the first row of each group)

```kotlin
Column(modifier.fillMaxWidth()) {
    if (showDivider) HorizontalDivider(color = colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().testTag("lab-${lab.id}").clickable(enabled = canEdit, onClick = onEdit).heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(lab.testName, RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold), onSurface)
            Text(refLine, RecordType.Secondary, onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(ltrIsolate(resultText), RecordType.MetricValue.copy(fontWeight = FontWeight.Bold), color = if (lab.isAbnormal) colorScheme.error else colorScheme.onSurface)
            if (lab.isAbnormal) StatusWord(stringResource(R.string.lab_abnormal), StatusTone.High)
        }
        if (canEdit) IconButton(onDelete, Modifier.minimumTouchTarget().testTag("deleteLab-${lab.id}")) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_test), tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
```
- The `lab-<id>` tag stays on the clickable row (not on the outer column), so `DeleteUndoUiTest` finds the same node.
- `refLine = lab.referenceRange?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.lab_ref_line, lab.testCategory, it) } ?: lab.testCategory`; `resultText = lab.result + (lab.unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")`.
- Removed: "ABNORMAL ▲" pill, faded error delete tint, `labelSmall`.

## Acceptance checklist

1. Tags: `labsTabContent`, `addInvestigationBtn`, `labReportCard-<date>`, `addTestToReportBtn-<date>` (canEdit), `lab-<id>`, `deleteLab-<id>` (canEdit).
2. `git diff` shows no change to `LabReportGrouping.kt`; groups, titles, order and members are identical.
3. One `RecordBandCard` per report (sand band) with the status chip in the band; no `LabeledValue`, filled `Surface` or bordered panel inside its body (grep). Report status is the band chip ("All normal" white, "N abnormal" red); in the body status is only words ("N abnormal" on a panel, "Abnormal" on a row) and abnormal results are red. No "▲", "✓" or uppercase status.
4. "1 test" singular, "2 tests" plural.
5. A lone CBC member is a plain row; "Individual investigations" shows only when there is more than one group.
6. Panel header >= 47.9 dp, toggles, exposes its state.
7. Delete >= 47.9 dp; row tap edits only when `canEdit`.
8. 360 dp, font scale 1.3: "Oral Glucose Tolerance Test (75g) - 2h Post-Load" wraps; the result stays readable.
9. RTL: result left of the name; values isolated.
10. Dark: abnormal result `#FFB4AB` on `#1A202E` (9.59:1).

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. Unit `LabReportGroupingOwnerFixTest`, `PatientHubPureModelTest`, `AncStateTest` test the model. `DeleteUndoUiTest` uses `lab-` and `deleteLab-`. `HubHeaderRedesignTest` checks the header's "1▲" Labs badge, not this screen.

### Must pass unchanged

`DeleteUndoUiTest`, `AncWorkflowTest`, `LabEntryDesignTest`, `LabBatchUiTest`, `DraftRecoveryDialogTest`, `HubHeaderRedesignTest`, `PatientHubNavigation`, unit tests.

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/LabsTabRedesignTest.kt`

Fixtures (exact constructors; first two from `DemoFixtures`, panel names from `LabReportGroupingOwnerFixTest`; Hb, Hct, CRP results are test inputs):
```kotlin
private fun lab(id: String, category: String, name: String, result: String, unit: String? = null, ref: String? = null, abnormal: Boolean = false, labName: String? = "Al Borg Medical Labs", date: String = "2026-07-25") =
    DemoLabInvestigation(id = id, patientId = "p-labs", date = date, testCategory = category, testName = name, result = result, unit = unit, referenceRange = ref, isAbnormal = abnormal, notes = null, labName = labName)

private fun patient(vararg labs: DemoLabInvestigation) = DemoPatient(
    id = "p-labs", caseId = "L-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null, visits = emptyList(),
    dating = DemoPatientDating(patientId = "p-labs", lmpDate = "2026-01-10", ultrasoundEdd = null, agreedEdd = null, datingMethod = "LMP", notes = null),
    labInvestigations = labs.toList(),
)
val cbc = lab("l1", "Hematology", "Complete Blood Count", "11.6", "g/dL", "11.0 - 15.0")
val ogtt = lab("l2", "Biochemistry", "Oral Glucose Tolerance Test (75g) - 2h Post-Load", "112", "mg/dL", "< 140", labName = "Al Mokhtabar Labs")
val hb = lab("l3", "Hematology", "Haemoglobin (Hb)", "9.8", "g/dL", abnormal = true)
val hct = lab("l4", "Hematology", "Haematocrit (Hct)", "30", "%")
val crp = lab("l5", "Biochemistry", "C-Reactive Protein (CRP)", "4", "mg/L")
```
Render once: `compose.setContent { AncTheme { LabsTab(patient = p, canEdit = true, onEvent = {}, onAddInvestigation = {}) } }`.

Cases:
- `patient(cbc, ogtt)`: `onAllNodesWithTag("labReportCard-2026-07-25").assertCountEquals(2)`; "1 test" twice; "All normal" twice; `hasText("Al Borg Medical Labs", substring = true)`; `hasText("28+0 wks", substring = true)`.
- `patient(hb, hct, crp)`: "Complete Blood Count (CBC) (2)" exists; "1 abnormal" `assertCountEquals(2)` (report and panel); "Abnormal" in `lab-l3`; "Individual investigations" exists; `lab-l5` exists.
- `patient(cbc)`: no "Complete Blood Count (CBC)" (lone member, no panel); no "Individual investigations".
- panel toggle: click the panel header, `lab-l3` disappears; click again, it returns.
- no node text contains "▲", "✓" or "ABNORMAL".
- `deleteLab-l1` height >= 47.9 dp; `canEdit = false`: no `deleteLab-l1`, no `addTestToReportBtn-2026-07-25`.
- empty patient: "No records".

## Risk

Medium: the screen just received owner fixes; the reviewer confirms `LabReportGrouping.kt` is untouched.
