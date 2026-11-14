# Slice 1: Overview sections and dashboard card (mock D)

Builder brief, self-contained. Look: `docs/designs/reviews/owner_fixes_2026-11-15/mock_D_opus.html` (accepted by the owner). System: `docs/designs/redesign/PLAN.md` section 2. Components: slice 0 (`docs/designs/redesign/slice_00_foundation.md`), which must be merged first.

Two lanes on one branch `redesign/slice-01-overview-card`, in this order: **1A Overview** then **1B card**. Only one lane edits `strings.xml` at a time. Each lane commits separately, each commit updates `.agents/pipeline/HANDOFF_AGY.md`.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also out of scope here: `ClinicalBadges.kt`, `PatientListScreen.kt` (both level 3), `DeliveryPlanningCard.kt` (slice 2), `ClinicalAttentionCard.kt` (slice 3).

**Visual only.** Same data, same taps, same events, same show and hide rules as today. Where mock D drew content the app does not have (an Identity section, a Medications section on Overview, "Not recorded" rows for blank history fields), this slice does not add it: see PLAN.md section 5, decisions 1 and 5.

Base path below: `app/src/main/java/com/sherifradwan/ancnative/`.

---

## Lane 1A: Overview sections

### Files

| File | Action |
|---|---|
| `ui/patienthub/components/OverviewSections.kt` (228 lines) | rewrite the three sections, add `ObstetricHistorySection` |
| `ui/patienthub/components/PastDeliveryMicroRow.kt` (129 lines) | restyle on `RecordRow` |
| `ui/patienthub/tabs/OverviewTab.kt` (136 lines) | drop the shared `Surface` and `SectionDivider`, add the obstetric history card |
| `ui/components/DueDateBox.kt` (72 lines) | delete (only caller is `OverviewSections.kt`) |
| `app/src/main/res/values/strings.xml` | add 3 strings |

### Strings to add

```xml
<string name="action_edit">Edit</string>
<string name="action_add">Add</string>
<string name="obstetric_history_title">Obstetric history</string>
```
Reuse: `pregnancy_dating_title`, `previous_pregnancies`, `medical_histories_title`, `gravida`, `parity`, `abortion`, `allergy_history`, `no_allergy_history`, `not_recorded`, `no_dating_recorded`, `notes`, `medical_history`, `surgical_history`, `family_history`, `edit_dating`, `add_previous_pregnancy`, `edit_patient`, `allergy_banner`, `correct_previous_pregnancy`. Do not change `obstetric_history` (used elsewhere in Title Case).

### Section order in `OverviewTab`

`Column(... .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp).testTag("overviewTabContent"), verticalArrangement = Arrangement.spacedBy(12.dp))` (was 14 dp):

1. `ClinicalAttentionCard` (unchanged)
2. `DatingSection`
3. `ObstetricHistorySection` (new)
4. `ParityPregnanciesSection`
5. `MedicalHistoriesSection`
6. `DeliveryPlanningCard` (unchanged until slice 2)
7. `AttachmentsSectionCard` (unchanged)

Delete the `Surface(color = surface) { Column { ... SectionDivider() ... } }` wrapper and the private `SectionDivider()` function. Each section is now its own card. Keep every lambda and event exactly as today (`startPatientEditor`, `startPregnancyEditor`, `AncEvent.StartDatingEditor`, `AncEvent.RequestDelete`).

### `DatingSection(patient, onEdit, modifier)`

- `RecordSectionCard(title = stringResource(R.string.pregnancy_dating_title), family = FieldFamilies.patient(), modifier = modifier.testTag("datingCard"), action = onEdit?.let { { SectionTextAction(text = stringResource(R.string.action_edit), contentDescription = stringResource(R.string.edit_dating), onClick = it, modifier = Modifier.testTag("editDating")) } })`.
- `dating == null`: `Text(stringResource(R.string.no_dating_recorded), style = RecordType.Missing, color = colorScheme.onSurfaceVariant)`.
- Else take `datingTextRows(summary)` (unchanged function). Render in this layout:
  - The origin row (label "LMP" or "ET") and the EDD row side by side in one `LabeledValueRow`, each `Modifier.weight(1f).fillMaxHeight()`. If there is no origin row, EDD takes the full width.
  - EDD: `LabeledValue(label = row.label, value = row.value, emphasis = true, modifier = Modifier.testTag("dueDateBox"))`, wrapped in `Box(Modifier.testTag("datingField-${row.label}"))`. Null EDD shows "Not recorded" (today `DueDateBox` did the same).
  - Origin: `LabeledValue(row.label, row.value, emphasis = true)` wrapped in `Box(Modifier.testTag("datingField-${row.label}"))`.
  - Method row: full width `LabeledValue`, tag wrapper `datingField-Method`.
  - Notes (only when not blank, as today): full width `LabeledValue(stringResource(R.string.notes), dating.notes)`.
- Discordance alert: keep the private `SectionBanner` and its strings and colours (it is an alert, not a field). Restyle only: shape `RecordShapes.Small`, text style `RecordType.Secondary.copy(fontWeight = FontWeight.Bold)`, keep the warning icon (it marks an alert, it is not decoration), keep tag `datingDiscordanceAlert`. Place it after the EDD row, as today.

### `ObstetricHistorySection(patient, modifier)` (new, same file)

- `RecordSectionCard(title = stringResource(R.string.obstetric_history_title), family = FieldFamilies.obstetric(), modifier = modifier.testTag("obstetricHistoryCard"))`, no action (G P A are edited in the patient editor; adding an entry point here would be a behaviour change).
- One `LabeledValueRow(Modifier.testTag("obstetricParityBadge"))` with three `LabeledValue(..., emphasis = true, modifier = Modifier.weight(1f).fillMaxHeight())`: Gravida `R.string.gravida` value `(patient.gravida ?: 1L).toString()`, Parity `R.string.parity` value `(patient.parity ?: 0L)`, Abortion `R.string.abortion` value `(patient.abortion ?: 0L)`. The fallbacks are the ones `ObstetricParityBadge` uses today; do not change them.
- The tag `obstetricParityBadge` moves from the old summary chip to this row. G P A appears once on the screen (no chip above fields).

### `ParityPregnanciesSection(...)` (same signature)

- `RecordSectionCard(title = stringResource(R.string.previous_pregnancies), family = FieldFamilies.obstetric(), modifier = modifier.testTag("obstetricParityCard"), action = onAddPregnancy?.let { { SectionTextAction(stringResource(R.string.action_add), stringResource(R.string.add_previous_pregnancy), it, Modifier.testTag("addPregnancyButton")) } })`.
- Remove the `ObstetricParityBadge(...)` call (now in `ObstetricHistorySection`).
- Empty list: `Text(previousDeliverySummary(patient), style = RecordType.Secondary, color = colorScheme.onSurfaceVariant)` (text unchanged, so "Primigravida" tests keep passing).
- Non-empty: `Column(verticalArrangement = Arrangement.spacedBy(8.dp))` of `PastDeliveryMicroRow` as today.

### `PastDeliveryMicroRow(pregnancy, onEdit, onDelete, modifier)` (same signature)

Keep every computed value (`pastDeliveryRow`, `yearText`, `outcomeText`, tone colours, `line2`, `line3`, `editLabel`). Change only the frame:

- `RecordRow(onClick = onEdit, onClickLabel = editLabel, modifier = modifier.testTag("pregnancy-${pregnancy.id}"), clickTag = if (onEdit != null) "editPregnancy-${pregnancy.id}" else null, trailing = onDelete?.let { { DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, it) } })`.
- Line 1, `FlowRow(horizontalArrangement = spacedBy(6.dp), verticalArrangement = spacedBy(2.dp), itemVerticalAlignment = CenterVertically)`:
  - year: `Text(yearText, style = RecordType.KeyValue.copy(fontFeatureSettings = "tnum"), color = onSurface)`;
  - outcome chip: keep the tone mapping (`Severe` errorContainer, `Caution` tertiaryContainer, `Neutral` surfaceContainerHigh) because it carries clinical severity; restyle to `Surface(shape = RecordShapes.Small, color = chipBg)` with `Text(it, style = RecordType.Tag, color = chipFg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))`;
  - mode: `Text(it, style = RecordType.FieldValue, color = onSurface)`.
- Line 2: `Text(line2, style = RecordType.Secondary, color = onSurfaceVariant, maxLines = 2, overflow = Ellipsis)`.
- Line 3: `Text(line3, style = RecordType.Secondary, color = onSurfaceVariant, modifier = Modifier.testTag("pregnancyExtras-${pregnancy.id}"))`.
- Family comes from the card (`LocalFieldFamily`), so the row is teal.

### `MedicalHistoriesSection(patient, onEdit, modifier)`

- `RecordSectionCard(title = stringResource(R.string.medical_histories_title), family = FieldFamilies.background(), modifier = modifier.testTag("medicalHistoriesCard"), action = onEdit?.let { { SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_patient), it, Modifier.testTag("editMedicalHistoryButton")) } })`.
- Allergy, by `classifyAllergy(patient.allergyHistory)`:
  - `Recorded`: keep `SectionBanner` with `allergy_banner`, error colours and icon, restyled like the dating banner. An allergy is a safety alert, so it keeps red and does not use the family tint.
  - `ExplicitlyNone`: `LabeledValue(stringResource(R.string.allergy_history), stringResource(R.string.no_allergy_history))`.
  - `NotRecorded`: `LabeledValue(stringResource(R.string.allergy_history), null)` (shows "Not recorded").
- Medical, surgical, family: full-width `LabeledValue` each, shown only when not blank (today's rule).

### Delete

- `ui/components/DueDateBox.kt` and its import in `OverviewSections.kt`.
- In `OverviewSections.kt`: `SectionIconAction`, the `ClinicalGroupSection` calls and their icon imports (`DateRange`, `Person`, `Healing`, `Edit`, `Add`). Do not delete `ClinicalGroupSection.kt` (still used by `ClinicalAttentionCard` until slice 3).
- `FactRow` stays in `Components.kt` (still used by `ScansTab` and `MedicationCard`).

### 1A acceptance checklist

1. Tags still present for a multiparous patient: `overviewTabContent`, `datingCard`, `editDating`, `obstetricParityCard`, `obstetricParityBadge`, `addPregnancyButton`, `medicalHistoriesCard`, `editMedicalHistoryButton`, `deliveryPlanningCard`, `editDeliveryPlan`, `deliveryHospital`, `deliveryFees`, `dueDateBox`, `datingDiscordanceAlert` (discordant fixture), `pregnancy-<id>`, `editPregnancy-<id>`, `pregnancyExtras-<id>`, `delete-previous_pregnancy-<id>`. New: `obstetricHistoryCard`, `datingField-<label>`.
2. Each section is its own outlined card, 16 dp radius, 1 dp `outlineVariant`, no shadow; 12 dp gaps.
3. Heading 18 sp bold primary; field label 12 sp semibold family colour; value 16 sp; key values (EDD, LMP or ET, G P A) semibold.
4. `dueDateBox` node height >= 48 dp and width <= 360 dp at 360 dp (test `OwnerFixesUiTest.dueDateBoxIsDisplayedTallAndWithinNarrowViewport`).
5. Edit and Add actions >= 48 by 48 dp; previous-pregnancy rows >= 56 dp; delete button unchanged.
6. 360 dp, font scale 1.3: no section title ellipsised; long outcome and mode text not ellipsised and left of the delete button (existing test).
7. RTL: titles right, actions left, fields start right.
8. Dark theme renders with the dark family tokens.
9. No `uppercase()`, no icon tiles, no em dash in the touched files.
10. Recorded allergy still shows the red banner with the allergy text.

---

## Lane 1B: dashboard patient card

### Files

| File | Action |
|---|---|
| `ui/patientdirectory/PatientDirectoryCard.kt` (200 lines) | restyle |
| `ui/patientdirectory/PatientDirectoryMetricsRow.kt` (156 lines) | rewrite layout |
| `app/src/main/res/values/strings.xml` | change 1 value |

`PatientListScreen.kt` (`projectDashboardRow`, `PatientDashboardRowProjection`) is not touched: every field the card needs already exists (`displayName`, `gestationalAgeText`, `eddText`, `isDatingDiscordant`, `bloodGroup`, `isHighRisk`, `riskTier`, `latestVitalsBp`, `isSevereBp`, `isLateOrPostTerm`, `isDelivered`, `deliveryDate`, `isIncomplete`).

### String change

`high_risk`: "HIGH RISK" becomes "High risk" (sentence case; it also changes the hub header badge text, PLAN.md decision 4). No test asserts the caps text (`ClinicalFormatTest` uses its own literal). Slice 0 already added `status_delivered`, `field_label_due_date`, `field_label_bp`.

### `PatientDirectoryCard`

Keep the signature, the `clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) }`, and the tags `patient-<caseId>`, `workspace-<caseId>`, `profile-<caseId>`, `patientMenu-<caseId>`, `incompleteChipAction`, `incompleteChip`.

- Container: `Card(modifier.fillMaxWidth().testTag(...)x3.clickable{...}, shape = RecordShapes.Large, colors = CardDefaults.cardColors(containerColor = colorScheme.surface), border = BorderStroke(1.dp, colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp))`. Remove `heightIn(min = 104.dp)`.
- Remove: the 3.5 dp severity strip `Box`, `accentColor`, the `lerp` high-risk tint (`cardBgColor`), `height(IntrinsicSize.Min)`, the separate Incomplete `Box` row.
- Body: `Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp))`. Start and end padding mirror in RTL.
- Name row: `Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp))`:
  1. Name: `Text(row.displayName, style = RecordType.CardName, color = colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = if (isArabic) TextAlign.End else TextAlign.Start, modifier = Modifier.weight(1f))`. Keep the `isArabic` rule as is (behaviour).
  2. Tag stack: `Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp))`, shown only when at least one tag applies, in this order:
     - `row.isHighRisk`: `StatusTag(StatusKind.HighRisk)`; else `row.riskTier == PatientRiskTier.MEDIUM`: `StatusTag(StatusKind.ModerateRisk)`. Same conditions as today's `ClinicalRiskBadge` calls.
     - `row.isSevereBp`: existing `BpAlertChip(R.string.bp_badge_severe, isSevere = true)` (unchanged component).
     - `row.isIncomplete`: the existing clickable `Box` with `minimumTouchTarget()`, `onClickLabel = registration_incomplete_chip_action`, the same `editPatientEvent(...)` call and tag `incompleteChipAction`, now holding `StatusTag(StatusKind.Incomplete, Modifier.testTag("incompleteChip"))`. `contentAlignment = Alignment.CenterEnd`.
     - `row.isDelivered`: `StatusTag(StatusKind.Delivered, Modifier.testTag("deliveredTag"))`. Display only: the delivered state already exists and was shown in the GA column.
  3. Menu: the existing `IconButton` with `minimumTouchTarget()`, `MoreVert` 20 dp, `contentDescription = more_actions`, tint `onSurfaceVariant`, tag `patientMenu-<caseId>`.
  Normal risk shows no tag (as today).
- Then `PatientDirectoryMetricsRow(patient, row, modifier = Modifier.padding(end = 8.dp))`.

### `PatientDirectoryMetricsRow`

- Remove `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`. The grid mirrors with the layout direction (mock D). Wrap coded values with `ltrIsolate(...)` from slice 0: GA text, parity text, BP, blood group, EDD.
- A private `MetricCell(label: String, modifier: Modifier, content: @Composable () -> Unit)`: `Column(modifier)`, label `Text(label, style = RecordType.MetricLabel, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = Ellipsis)`, then content. Value helper: `Text(value, style = RecordType.MetricValue, color = ..., maxLines = 1, overflow = TextOverflow.Ellipsis)`; missing values use `RecordType.Missing` size 16 with `onSurfaceVariant`.
- Layout, not delivered: `Column(verticalArrangement = spacedBy(8.dp))` of
  - `Row(spacedBy(8.dp))`: GA `weight(1f)`, Parity `weight(1f)`, BP `weight(1f)`;
  - `Row(spacedBy(8.dp))`: Blood group `weight(1f)`, Due date `weight(2f)`.
- Layout, delivered: first row Delivery `weight(2f)` and Parity `weight(1f)`; second row BP `weight(1f)`, Blood group `weight(1f)`, `Spacer(Modifier.weight(1f))`.
- Cells:
  - GA (label `R.string.ga_label`): `FlowRow(horizontalArrangement = spacedBy(4.dp), verticalArrangement = spacedBy(2.dp), itemVerticalAlignment = CenterVertically)` holding the value (`row.gestationalAgeText` when present and not "-", else `R.string.pending` in missing style), then `GaDiscordanceMarker(style = RecordType.MetricValue)` when `hasGa && row.isDatingDiscordant` (unchanged component and tag `gaDiscordanceMarker`), then the post-term chip when `row.isLateOrPostTerm`: `Surface(shape = RecordShapes.Small, color = errorContainer) { Text("Post", style = RecordType.Tag, color = onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) }`. The text stays "Post" (a test finds it). FlowRow lets the marker and chip wrap inside the GA column instead of overflowing.
  - Parity (label `R.string.parity`): same text rule as today (`G$g P$p A$a` when a > 0, else `G$g P$p`; fallbacks 1 and 0 as today).
  - BP (label `R.string.field_label_bp`): `row.latestVitalsBp` with `ActionRose` colour when `row.isSevereBp`; missing shows "--/--" in missing style (text unchanged).
  - Blood group (label `R.string.blood_group`): `row.bloodGroup` or "--" in missing style.
  - Due date (label `R.string.field_label_due_date`): `row.eddText` or `R.string.pending` in missing style.
  - Delivery (label `R.string.delivery_outcome_section`): value `deliveredHeadline(row.deliveryDate)` (unchanged function), then `deliveredDaysAgo(row.deliveryDate)` when not null as `Text(style = RecordType.MetricLabel, color = onSurfaceVariant)`.
- Removed from the card: the 10 sp sub-lines "Due ...", "Blood: ..." (both now have cells), and the previous-delivery summary line (PLAN.md decision 2, accepted with mock D). Removed hard-coded strings "Parity", "Vitals / BP", "Due ", "Blood: ".

### 1B acceptance checklist

1. Tags unchanged and present: `patient-<caseId>`, `workspace-<caseId>`, `patientMenu-<caseId>`, `incompleteChipAction`, `incompleteChip`, `gaDiscordanceMarker`. New: `deliveredTag`.
2. No row of its own for any tag: every visible tag's vertical centre lies within the 48 dp name row (tags stack inside it; the row may grow only when two or more tags apply).
3. Labels 12 sp, values 16 sp; no text under 12 sp in the card.
4. Five metrics present for a non-delivered patient: GA, Parity, BP, Blood group, Due date; the label "Parity" exists (used by `DatingGaMarkerSurfacesTest`).
5. 360 dp, font scale 1.3, post-term discordant fixture: marker and "Post" chip inside the GA column, left of the Parity label, not overlapping, not clipped (existing `DatingGaMarkerSurfacesTest.listRowMarkerIsNotClippedForAPostTermPatientAt360dpFontScale13`).
6. Menu button and Incomplete target >= 48 by 48 dp.
7. RTL layout: name at the right, menu at the left, GA cell rightmost; `A+` renders as `A+` (isolated).
8. High risk shows the word "High risk" (bold), Moderate "Moderate risk", Incomplete "Incomplete", Delivered "Delivered"; normal shows none.
9. No strip, no tinted card, no elevation shadow.
10. Dark theme: status tags use the dark tokens.

---

## Tests (written by a different lane from this spec)

### Existing tests that need edits (grep done 2026-11-14)

- `app/src/androidTest/java/com/sherifradwan/ancnative/OverviewTabSectionsTest.kt`: the `datingRows()` helper groups texts by vertical position, which breaks when the label sits above the value. Rewrite it to read pairs from the `datingField-<label>` tags: for each tagged node collect texts with `hasAnyAncestor(hasTestTag(tag))`, giving `listOf(label, value)`. Assertions such as `datingRows().any { it == listOf("EDD", "15/03/2027") }` then keep their meaning. 10 call sites use the helper. Add `obstetricHistoryCard` to the tag list in `legacyTagsResolveForMultiparousPatient` (and keep `obstetricParityBadge`).
- `OwnerFixesUiTest.dueDateBoxIsDisplayedTallAndWithinNarrowViewport`: no edit expected (tag kept on a node of at least 64 dp); confirm it passes.

### Existing tests that must pass unchanged

`DatingGaMarkerSurfacesTest`, `RegistrationPencilUiTest`, `AncWorkflowTest`, `DeleteUndoUiTest`, `ClinicalAttentionUiTest`, `DeliveryPlanningCardEditGateTest`, `DeliveryPlanTabsEditGateTest`, `DeliveryPlanSheetTest`, `PatientHubDesignTest`, and all unit tests.

### New tests

- `app/src/androidTest/java/com/sherifradwan/ancnative/OverviewRedesignTest.kt`:
  - every Overview section card has `heading()` semantics on its title and sits in its own card (bounds do not overlap);
  - G P A appears once: exactly one node with text "Gravida" under `overviewTabContent`, inside `obstetricHistoryCard`;
  - `editDating`, `addPregnancyButton`, `editMedicalHistoryButton` each >= 48 by 48 dp; `pregnancy-<id>` >= 56 dp;
  - blank medical history is still hidden; `NotRecorded` allergy shows "Not recorded" under the "Allergy history" label; recorded allergy shows the banner text;
  - 360 dp, font scale 1.3: titles `assertNotEllipsized`; RTL: title right of action by bounds.
- `app/src/androidTest/java/com/sherifradwan/ancnative/PatientCardRedesignTest.kt` (render `PatientDirectoryCard` like `DatingGaMarkerSurfacesTest.showListCard`):
  - for each state (normal, moderate, high, incomplete, delivered, severe BP, incomplete plus high) the tag centres lie inside the name row and above the first metric label;
  - normal shows no tag; word per state as in checklist 8;
  - labels GA, Parity, BP, Blood group, Due date present for a non-delivered fixture; Delivery outcome present for a delivered one;
  - RTL (`CompositionLocalProvider(LocalLayoutDirection provides Rtl)`): name right of menu by bounds; blood group text equals `"⁦A+⁩"`;
  - 360 dp, font scale 1.3: name not clipped beyond two lines, tags not clipped.
- Run each class with `.agents/scripts/run_emulator_test_class.sh <fqcn>`; full gate with `.agents/scripts/run_emulator_all_tests.sh` and `./gradlew --offline :app:testDebugUnitTest -q`.

## Risk

Medium. No level-3 file, no storage, no state change. The card is on the main list, so the full emulator suite runs before any push. Review by a model family different from the builder.
