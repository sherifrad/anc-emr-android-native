# Whole-app redesign plan (from mock D)

Status: plan, written 2026-11-14 by the design lead (Claude Opus). Source of truth for the look: `docs/designs/reviews/owner_fixes_2026-11-15/mock_D_opus.html`, accepted by the owner. This file is read-only guidance for builder lanes. Builders edit app code only inside the slice they were given.

antislop: DURING, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (motion: pressed and focus states only, no new animation).

## 0. Rules for every slice

1. **Visual only.** A slice changes how things look and where they sit. It does not change what a tap does, what is saved, what is shown or hidden for a given patient state, or the order of clinical rules. If a mock asks for new content (a new section, a new action), it is a separate owner decision, listed in section 5, never slipped into a visual slice.
2. **Never touch these files:** `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, any `*Generator*` (today `ui/report/PatientPrintHtmlGenerator.kt`). Room schema v7 is frozen. A slice whose diff shows any of these is rejected.
3. **Keep test tags.** Every `testTag` that a test uses stays, on an element with the same meaning. Before renaming or removing any tag, grep `app/src/androidTest` and `app/src/test`. Each slice brief lists the tests that need edits; those edits are written by a different lane from the spec, not by the builder.
4. **One canonical component per entity** (AGENTS.md section 1). Shared pieces live in `app/src/main/java/com/sherifradwan/ancnative/ui/components/`. A slice that needs a variant adds a parameter to the shared component; it never copies it.
5. **360 dp first.** Every slice is checked at 360 dp width and font scale 1.3. No clipped patient name, GA, BP or risk word.
6. **48 dp** for anything tappable (`Modifier.minimumTouchTarget()` in `ui/Components.kt`, or `minimumInteractiveComponentSize()`). Read-only value boxes do not get forced to 48 dp.
7. **Contrast** 4.5:1 for all text, 3:1 for tag borders and focus. New colours go into `Theme.kt` as `val Name = Color(0xFF......)` so the existing `ThemeContrastTest` style can parse them.
8. **Status never by colour alone.** Every risk or state shows a word.
9. **No em dashes, no all-caps labels, no decorative icons, no gradients or glow, two radii only (10 dp, 16 dp).**
10. **File size ceiling 1,500 lines.** If a slice would push a file past it, split first in a separate commit.
11. **Branch hygiene** (AGENTS.md): one branch per slice, `.agents/scripts/git_audit.sh` before work and before push, and `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit (mission row, artifact, log line). This plan does not edit HANDOFF; the first builder commit must.
12. **Lessons from slices 0 and 1:** `LabeledValue(stretch = true)` only inside `LabeledValueRow` with `Modifier.weight(1f).fillMaxHeight()` (weight in an unbounded scrolling column collapsed boxes to 0 px; tests must assert height or `assertIsDisplayed`, not only existence). With several `testTag` modifiers on one node, the first (outermost) wins in tests.
13. **Review:** an edit to a level-3 path (see `.agents/PROJECT_MAP.json` via the project's tools: `ClinicalBadges.kt`, `PatientListScreen.kt`, `PatientEditor.kt`, `PreviousPregnancyEditor.kt`, `DatingEditor.kt`, `ClinicalFormat.kt`, `ClinicalPresets.kt`, `ClinicalAttachmentViewerDialog.kt`, `SecuritySettingsDialog.kt`, `VaultGateScreen.kt`, `PatientTwoPageReportDialog.kt`, `PatientPrintManager.kt`) needs a frontier-tier review and an independent test review.

## 1. Screen inventory (surveyed 2026-11-14, `ui/**`, 23,004 lines)

| Area | Screens, tabs, dialogs, sheets | Files |
|---|---|---|
| Shell | App scaffold, route switch, new-patient info dialog, two more `AlertDialog`s (draft recovery and similar) | `AncApp.kt` |
| Lock and vault | App lock screen, vault gate | `AppLockScreen.kt`, `VaultGateScreen.kt` (auth, level 3) |
| Settings | Security settings dialog with change PIN, export backup, restore passphrase dialogs; catalog settings with add lab test, add lab category group, add medication dialogs | `SecuritySettingsDialog.kt` (level 3), `CatalogSettingsSections.kt` |
| Patient list | Patient list screen (search, filters, sort), patient card, metrics row, patient actions sheet | `PatientListScreen.kt` (level 3), `DashboardSort.kt`, `patientdirectory/PatientDirectoryCard.kt`, `PatientDirectoryMetricsRow.kt`, `PatientActionsModalSheet.kt` |
| Patient hub | Hub screen, header with GA, risk, allergy pills, sticky attention, tab bar (Overview, Visits, Scans, Labs, Meds, Plan) | `patienthub/PatientHubScreen.kt`, `PatientHubHeader.kt`, `PatientHubTab.kt` |
| Overview tab | Clinical attention card, dating, previous pregnancies with G P A, medical history, delivery planning card, attachments | `tabs/OverviewTab.kt`, `components/OverviewSections.kt`, `PastDeliveryMicroRow.kt`, `ClinicalAttentionCard.kt`, `ClinicalAttentionEntries.kt`, `ClinicalGroupSection.kt`, `DeliveryPlanningCard.kt`, `DeliveryOutcomeUi.kt`, `AttachmentsSectionCard.kt`, `ui/components/DueDateBox.kt` |
| Attention | Attention banner, attention dialog | `components/ClinicalAttentionBanner.kt` |
| Visits tab | Visit list on a timeline, bedside vitals grid | `tabs/VisitsTab.kt`, `components/BedsideVitalsGrid.kt`, `model/BedsideVitalsUi.kt`, `TimelineSpine.kt` |
| Scans tab | Scan list, biometry grid, photo viewer dialog | `tabs/ScansTab.kt`, `components/BiometryGrid.kt`, `model/BiometryUi.kt`, `UltrasoundPhotoViewerDialog.kt` |
| Labs tab | Lab report list, report items | `tabs/LabsTab.kt`, `components/LabReportItem.kt`, `model/LabReportGrouping.kt` |
| Meds tab | Medication cards, tap-to-add, stop bar, medication sheet and form | `tabs/MedsTab.kt`, `components/MedicationCard.kt`, `MedicationTapToAddSection.kt`, `MedicationStopBar.kt`, `MedicationSheet.kt`, `MedicationForm.kt`, `model/MedicationTiles.kt` |
| Plan tab | Delivery plan card, delivery plan sheet | `tabs/PlanTab.kt`, `components/DeliveryPlanSheet.kt` |
| Editors | Patient editor and status editor, other histories, previous pregnancy, dating, visit, ultrasound, lab editor, lab batch screen (add, choose panel dialog, enter, review tabs) | `PatientEditor.kt`, `PatientEditorGroups.kt`, `OtherHistoriesEditor.kt`, `PreviousPregnancyEditor.kt`, `DatingEditor.kt`, `DatingFieldVisibility.kt`, `VisitEditor.kt`, `UltrasoundEditor.kt`, `LabEditor.kt`, `LabBatchScreen.kt` |
| Registration flow | Scaffold, step header, step footer, step text | `ui/components/Registration*.kt` |
| Shared dialogs and parts | Delete confirm and undo, discard changes, date picker, in-app camera, add attachment, attachment viewer, zoomable image, editor sheet parts | `DeleteUndoUi.kt`, `components/EditorSheetParts.kt`, `AncDatePicker.kt`, `InAppCameraDialog.kt`, `AddAttachmentDialog.kt`, `ClinicalAttachmentViewerDialog.kt` (level 3), `ui/components/ZoomableImage.kt`, `components/PencilEditButton.kt` |
| Report | Two-page report dialog, share text, report sections (print HTML generator is protected) | `report/PatientTwoPageReportDialog.kt` (level 3), `PatientShareText.kt`, `ReportSections.kt` |
| Shared look today | `DesignSystem.kt` (SectionCard, SummaryRow, BigChoiceButton, NoticeCard, SegmentedProgressBar, ValueStepper, SectionHeader, batch bars), `Components.kt` (FactRow, `minimumTouchTarget`, EditorField), `ClinicalBadges.kt` (tags, badges, DeliverySectionBox), `Theme.kt` (scheme, typography, tokens) | |

Screens with a mock today: Overview sections and the dashboard card (mock D). Every other screen needs a mock before its slice is built (section 4 says which).

## 2. Design system (Kotlin-ready, from mock D)

### 2.1 Colour

Mock D's base inks map to the app's existing colour scheme roles, so no second navy or grey is introduced. Contrast computed with the WCAG formula against the real scheme values in `Theme.kt`.

| Role | Light | Dark | Used for |
|---|---|---|---|
| `colorScheme.primary` | #0D1B3D | #B4C5FF | section headings, text actions (Edit, Add), Delivered tag |
| `colorScheme.onSurface` | #0D1B3D | #E2E2EA | values |
| `colorScheme.onSurfaceVariant` | #4C3F43 | #CDC3C6 | card metric labels, secondary lines, "Not recorded" |
| `colorScheme.surface` | #FFFFFF | #1A202E | section cards, patient cards |
| `colorScheme.background` | #FBF8F5 | #0E121B | page behind cards |
| `colorScheme.outlineVariant` | #CAC4D0 | #3A4459 | card outline 1 dp |

New tokens for `Theme.kt` (names final; builders use them as written):

| Token | Light | Dark | Meaning |
|---|---|---|---|
| `FieldPatientBg` / `FieldPatientLabel` | #E9EEF8 / #23407A | #22304A / #AFC6F2 | Patient family: identity, dating |
| `FieldObstetricBg` / `FieldObstetricLabel` | #E3F0EE / #1D5149 | #1D3836 / #9FD6CC | Obstetric family: G P A, previous pregnancies |
| `FieldBackgroundBg` / `FieldBackgroundLabel` | #F1EDE4 / #594A2C | #36301F / #E0CFA6 | Background family: medical, surgical, family history, allergy, medications |
| `FieldDeliveryBg` / `FieldDeliveryLabel` | #EEE9F5 / #4E3A78 | #2E2842 / #CDBDF0 | Delivery family: plan, hospital, fees, postpartum, outcome |
| `StatusIncompleteBg` / `Border` / `Text` | #EEF0F4 / #8C95A5 / #333B4A | #2A3140 / #5B6578 / #D5DAE3 | Incomplete |
| `StatusModerateBg` / `Border` / `Text` | #FCEFD6 / #B9862C / #6E4708 | #3A2A0E / #A87A2A / #F3D18E | Moderate risk |
| `StatusHighBg` / `Border` / `Text` | #FBE3E3 / #C26666 / #8E1B1B | #4A1616 / #B45A5A / #FFC9C9 | High risk |
| Delivered | surface / primary / primary | surface / primary / primary | Delivered (no new token) |

Dark tokens get the `Dark` infix like the existing ones: `FieldPatientDarkBg`, `StatusHighDarkText`, and so on.

Field families never use red, amber or green; those stay reserved for risk, attention and alerts. Clinical attention levels and the allergy banner keep their own error and warning colours.

Computed contrast (text pairs need 4.5, borders 3.0):

| Pair | Light | Dark |
|---|---|---|
| Heading (primary) on surface | 16.92 | 9.59 |
| Value (onSurface) on Patient / Obstetric / Background / Delivery box | 14.54 / 14.47 / 14.48 / 14.19 | 10.25 / 9.75 / 10.19 / 10.88 |
| Not recorded (onSurfaceVariant) on the four boxes | 8.60 / 8.56 / 8.56 / 8.39 | 7.69 / 7.31 / 7.64 / 8.15 |
| Family label on its box | 8.65 / 7.73 / 7.36 / 8.00 | 7.66 / 7.76 / 8.53 / 8.10 |
| Family label on surface | 10.07 / 9.03 / 8.60 / 9.54 | 9.44 / 10.05 / 10.58 / 9.41 |
| Metric label (onSurfaceVariant) on surface | 10.00 | 9.47 |
| Action (primary) on Obstetric box | 14.47 | 7.40 |
| Tag text Incomplete / Moderate / High / Delivered | 9.87 / 7.19 / 7.40 / 16.92 | 9.29 / 9.44 / 10.19 / 9.59 |
| Tag border Incomplete / Moderate / High | 3.02 / 3.23 / 3.90 | not required on dark fills (text carries it) |

### 2.2 Type

The app's `ClinicalTypography` has `bodyLarge` 15 sp, `labelMedium` 11 sp and `titleMedium` 15 sp, so mock D sizes cannot come from those roles. Do not change `ClinicalTypography` (it would restyle every screen at once). Add explicit styles in `ui/components/RecordTokens.kt`:

| Style | Size / line | Weight | Colour |
|---|---|---|---|
| `RecordType.SectionTitle` | 18 / 24 sp | Bold | primary |
| `RecordType.FieldLabel` | 12 / 16 sp | SemiBold, letterSpacing 0 | family label |
| `RecordType.FieldValue` | 16 / 22 sp | Normal | onSurface |
| `RecordType.KeyValue` | 16 / 22 sp | SemiBold | onSurface (EDD, G P A, blood group, GA) |
| `RecordType.Missing` | 16 / 22 sp | Normal, Italic | onSurfaceVariant |
| `RecordType.Secondary` | 14 / 20 sp | Normal | onSurfaceVariant |
| `RecordType.Action` | 14 / 20 sp | SemiBold | primary |
| `RecordType.Tag` | 12 / 16 sp | SemiBold (Bold for High risk) | tag text |
| `RecordType.CardName` | 16 / 22 sp | Bold | onSurface |
| `RecordType.MetricLabel` | 12 / 16 sp | Medium | onSurfaceVariant |
| `RecordType.MetricValue` | 16 / 22 sp | SemiBold | onSurface |

`FontFamily.Default` (Roboto on Android). Minimum anywhere in redesigned screens: 12 sp.

### 2.3 Shape and spacing

- `RecordShapes.Small = RoundedCornerShape(10.dp)`: value boxes, tags, buttons, record rows.
- `RecordShapes.Large = RoundedCornerShape(16.dp)`: section cards, patient cards, sheets' top corners.
- `RecordSpacing`: `XS = 4.dp` (label to box, tag stack), `S = 8.dp` (columns, metric gaps), `M = 12.dp` (field rows, card list gap, card start padding), `L = 16.dp` (card padding, screen gutter).
- Elevation 0 everywhere in redesigned cards; the 1 dp outline separates.

### 2.4 Components (all in `ui/components/`, built in slice 0)

- `FieldFamily` (data class: `container`, `label`) and `FieldFamilies.patient() / obstetric() / background() / delivery()` (composable getters that pick light or dark through `isDarkTheme()`), plus `LocalFieldFamily`.
- `LabeledValue(label, value, modifier, emphasis = false, family = LocalFieldFamily.current, missingText = stringResource(R.string.not_recorded))`: label above a tinted 10 dp box, min height 44 dp, read-only. Null or blank value renders `missingText` in `RecordType.Missing`.
- `LabeledValueRow(...)`: `Row(spacedBy(8.dp))` that gives each child `weight(1f)`; used for pairs and the G P A triple.
- `RecordSectionCard(title, modifier, action: (@Composable () -> Unit)? = null, family, content)`: outlined card, header row min 48 dp, provides `LocalFieldFamily`.
- `SectionTextAction(text, contentDescription, onClick, modifier)`: `TextButton`, 48 by 48 dp min.
- `RecordRow(onClick, onClickLabel, modifier, trailing, content)`: tappable tinted row, min 56 dp, for repeatable records (previous pregnancies now; visits, scans, labs later).
- `StatusTag(kind: StatusKind, modifier, text = default word)`: `StatusKind { HighRisk, ModerateRisk, Incomplete, Delivered }`. No tag for normal.

## 3. Slices (in build order)

Each slice is one builder lane, one branch, at most about five main files. "Mock first" means a mock must be made and accepted before the slice is built.

### Slice 0. Foundation: tokens and shared components
- Scope: add tokens to `Theme.kt`; add `ui/components/RecordTokens.kt`, `LabeledValue.kt`, `RecordSectionCard.kt`, `StatusTag.kt`. Nothing on screen changes; nothing calls the new components yet except tests.
- Files: `ui/Theme.kt` (exists, append only), four new files in `ui/components/`.
- Deleted: nothing. Reused: `minimumTouchTarget()`, `isDarkTheme()`, `R.string.not_recorded`.
- Acceptance: compiles; unit contrast test passes for every pair in 2.1; emulator component test at 360 dp, font scale 1.3, light, dark and RTL passes; no existing test changes.
- Depends on: nothing. Risk: low (append only, no level-3 file). Mock: none needed.
- Brief: `slice_00_foundation.md`.

### Slice 1. Overview sections and dashboard card (mock D)
- Two lanes, one branch: 1A Overview sections, 1B dashboard card.
- 1A files: `patienthub/components/OverviewSections.kt`, `PastDeliveryMicroRow.kt`, `patienthub/tabs/OverviewTab.kt`, `ui/components/DueDateBox.kt` (deleted), `res/values/strings.xml`.
- 1B files: `patientdirectory/PatientDirectoryCard.kt`, `PatientDirectoryMetricsRow.kt`, `res/values/strings.xml`.
- Deleted: `DueDateBox.kt`; in Overview, the icon tiles and `SectionBanner` wrapper stay only where an alert needs them; the shared `Surface` plus `SectionDivider`; in the card, the 3.5 dp strip, the 4% error tint, the separate Incomplete row, the forced-LTR metrics, the 10 sp sub-lines.
- Reused: slice 0 components, `GaDiscordanceMarker`, `BpAlertChip`, `DeleteIconButton`, `editPatientEvent`.
- Tests needing edits: `OverviewTabSectionsTest` (dating rows helper, EDD pairing), possibly `OwnerFixesUiTest` (tag kept; check only). Tests that must pass unchanged: `DatingGaMarkerSurfacesTest`, `RegistrationPencilUiTest`, `AncWorkflowTest`, `DeleteUndoUiTest`, `ClinicalAttentionUiTest`.
- Depends on: slice 0. Risk: medium (no level-3 file; card used on the main list). Mock: done (mock D).
- Brief: `slice_01_overview_and_card.md`.

### Slice 2. Delivery planning card and delivered state
- Brief: `slice_02_delivery.md`. Mock: `mocks/mock_s02_delivery.html` (done).
- Scope: `DeliveryPlanningCard` on `RecordSectionCard`, Delivery family, four full-width `LabeledValue`s; `DeliveredSummary` as a "Delivered" tag, a "Delivery date" field ("???" kept when unknown), mode and outcome fields when present. New sentence-case display strings; `delivery_*_title` untouched (sheet labels, note keys).
- Files: `components/DeliveryPlanningCard.kt`, `components/DeliveryOutcomeUi.kt`, `ui/ClinicalBadges.kt` (level 3, deletions only), `ui/Theme.kt` (delete 24 `Delivery*` colours), `strings.xml`.
- Tests needing edits: `ThemeContrastTest` (its `Delivery*` pairs move to `FieldDelivery*`). Depends on: 1A. Risk: medium.

### Slice 3. Clinical attention card, header strip and dialog
- Brief: `slice_03_attention.md`. Mock: `mocks/mock_s03_attention.html` (done).
- Scope: entries as level word over a 16 sp sentence (`AttentionEntryBox`); Alert moves to the amber Moderate tokens (4.84:1 to 7.19:1); icons removed (the word carries the level); strip and dialog restyled; unused `ClinicalAttentionBanner` composable and `ClinicalGroupSection.kt` deleted.
- Files: `components/ClinicalAttentionBanner.kt`, `ClinicalAttentionEntries.kt`, `ClinicalAttentionCard.kt`, `ClinicalGroupSection.kt` (delete), `patienthub/PatientHubHeader.kt` (`CompactClinicalAttentionStrip` only), `strings.xml`.
- Tests needing edits: none expected. Depends on: 0, 1A. Risk: medium (clinical meaning; frontier review advised).

### Slice 4. Patient hub header and tab bar
- Brief: `slice_04_hub_header.md`. Mock: `mocks/mock_s04_hub_header.html` (done).
- Scope: name 22 sp; risk as `StatusTag` (new `NormalRisk` kind); GA, Parity, Blood group, Age as metric cells shared with the list card (`ui/components/MetricCell.kt`, moved out of `PatientDirectoryMetricsRow`); "Rh negative" as a word; tab labels without icons, count badges radius 10, "!" instead of a colour-only dot. Collapse logic untouched.
- Files: `ui/components/MetricCell.kt` (new), `patientdirectory/PatientDirectoryMetricsRow.kt` (move only), `patienthub/PatientHubHeader.kt`, `ui/components/StatusTag.kt`, `ui/components/RecordTokens.kt`, `strings.xml`. Reads level-3 `ClinicalBadges.kt` and `ClinicalFormat.kt` without editing them.
- Tests needing edits: none expected. Depends on: 3. Risk: medium-high.

### Slice 5. Patient list chrome and actions sheet
- Brief: `slice_05_list_chrome.md`. Mock: `mocks/mock_s05_list_chrome.html` (done).
- Scope: one chip style (selected filled primary), "Moderate risk" filter label, 10 dp radii, count as text, New patient without icon, sort as a text button, empty states from strings, sheet rows 56 dp without icons or chevrons, no trailing separator in the sheet header.
- Files: `ui/PatientListScreen.kt` (level 3, drawing code only, logic untouched), `patientdirectory/PatientActionsModalSheet.kt`, `strings.xml`. `DashboardSort.kt` is not touched.
- Tests needing edits: none expected. Depends on: 1B (and `RecordType.HeaderName` from 4, or adds it). Risk: medium-high.

### Slice 6. Visits tab (flat list) and the shared flat-list pieces
- Brief: `slice_06_visits.md`. Mock: `mocks/mock_s06_visits.html` (done, flat-list version).
- Scope: visits as flat rows (date heading with Delete and Edit, grey GA line, vitals as plain label-over-value, hairline dividers); flags as coloured words; pre-eclampsia as a red label over its sentence; BP alert copy without "▲" and "·" (owner decision 6). Adds the shared `RecordListItem`, `RecordListDivider` (`ui/components/RecordList.kt`), `PlainField` (`MetricCell.kt`) and `StatusWord` / `StatusTone` (`StatusTag.kt`).
- Files: the three shared files above, `tabs/VisitsTab.kt`, `components/BedsideVitalsGrid.kt`, `strings.xml`. Risk: medium. Depends on: 0 to 5.

### Slice 7. Scans tab (flat list)
- Brief: `slice_07_scans.md`. Mock: `mocks/mock_s07_scans.html` (done).
- Scope: scans as flat rows; biometry and anatomy as plain fields in three columns; impression as a plain field; photo buttons kept.
- Files: `tabs/ScansTab.kt`, `components/BiometryGrid.kt`, `strings.xml`. Risk: low-medium. Depends on: 6.

### Slice 8. Labs tab (flat list)
- Brief: `slice_08_labs.md`. Mock: `mocks/mock_s08_labs.html` (done).
- Scope: reports as flat rows with a grey line (lab, "n tests", GA) and a status word; panels as sub-headings that open and close, not boxes; test rows split by hairlines; abnormal results red with the word "Abnormal". Grouping rule untouched (a panel needs at least two members).
- Files: `tabs/LabsTab.kt`, `components/LabReportItem.kt`, `strings.xml`. Risk: medium. Depends on: 6.

### Slice 9. Meds tab (flat rows, restyle only)
- Brief: `slice_09_meds.md`. Mock: `mocks/mock_s09_meds.html` (done, revised after the owner's feedback).
- Scope: one row per drug (name and dose; route and frequency; status word at the end; Stop one tap); Edit and Delete in the opened row; compact picker and undo bar. No Meds stage 2.
- Files: `components/MedicationCard.kt`, `tabs/MedsTab.kt`, `components/MedicationTapToAddSection.kt`, `components/MedicationStopBar.kt`, `strings.xml`. Tests needing edits: `MedsTabSheetTest` (instructions text, open-before-edit). Risk: medium. Depends on: 6.

### Slice 10. Plan tab and attachments
- Files: `tabs/PlanTab.kt`, `components/AttachmentsSectionCard.kt`, `ui/AddAttachmentDialog.kt`, `ui/ClinicalAttachmentViewerDialog.kt` (level 3).
- Tests: `DeliveryPlanTabsEditGateTest`, attachment tags in `AncWorkflowTest`.
- Depends on: 2. Risk: medium. Mock first: plan tab, attachments list with two files and empty state, add dialog, viewer chrome.

### Slice 11. Editor sheets
- Files: `components/EditorSheetParts.kt`, `MedicationSheet.kt`, `MedicationForm.kt`, `DeliveryPlanSheet.kt`, `PencilEditButton.kt`.
- Scope: sheet header, field labels at 12 sp, save and discard buttons, discard dialog.
- Tests: `MedicationFormTest`, `MedsTabSheetTest`, `DeliveryPlanSheetTest` (known flaky once).
- Depends on: 0. Risk: medium (save paths must not move). Mock first: medication sheet and delivery plan sheet, empty and filled, discard dialog.

### Slice 12. Registration flow and patient editor
- Files: `ui/components/RegistrationFlowScaffold.kt`, `RegistrationStepHeader.kt`, `RegistrationStepFooter.kt`, `RegistrationStepText.kt`, `ui/PatientEditor.kt` (level 3).
- Tests: `RegistrationFlowUiTest`, `RegistrationPencilUiTest`, `RegistrationStepTextTest` (unit), `PatientEditorGroupsTest` (unit).
- Depends on: 0. Risk: high (level 3, registration recently merged). Mock first: step header and footer, identity step, status editor; English and Arabic name input.

### Slice 13. History editors
- Files: `ui/OtherHistoriesEditor.kt`, `PreviousPregnancyEditor.kt` (level 3), `DatingEditor.kt` (level 3), `DatingFieldVisibility.kt`.
- Tests: `RegistrationFlowUiTest` steps, `DatingFieldVisibilityTest`, `DatingDraftRuleTest` (unit).
- Depends on: 12. Risk: high (level 3). Mock first: one screen per editor at 360 dp.

### Slice 14. Visit editor
- Files: `ui/VisitEditor.kt` (1,299 lines; split it first if the slice would pass 1,500), `ui/DesignSystem.kt` (`SummaryRow`, `ValueStepper` only if shared).
- Tests: `VisitEntryDesignTest`.
- Depends on: 0, 6. Risk: medium-high. Mock first: visit editor top, vitals entry, summary.

### Slice 15. Ultrasound editor
- Files: `ui/UltrasoundEditor.kt`, `ui/components/ZoomableImage.kt`.
- Tests: `UltrasoundGuidedFlowTest`. Depends on: 7. Risk: medium. Mock first: guided steps and summary.

### Slice 16. Lab editor and lab batch
- Files: `ui/LabEditor.kt`, `ui/LabBatchScreen.kt`, `ui/ClinicalPresets.kt` (level 3, display only).
- Tests: `LabEntryDesignTest`, `LabBatchUiTest`, `ClinicalPresetsTest`. Depends on: 8. Risk: high. Mock first: batch add, choose panel, enter, review.

### Slice 17. Catalog settings
- Files: `ui/CatalogSettingsSections.kt` (1,269 lines).
- Tests: `CustomChoicesStoreInstrumentedTest`. Depends on: 0. Risk: low-medium. Mock first: settings list and the three add dialogs.

### Slice 18. Security, lock and vault
- Files: `ui/SecuritySettingsDialog.kt`, `ui/AppLockScreen.kt`, `ui/VaultGateScreen.kt` (auth, level 3).
- Tests: `SecurityWorkflowTest`. Depends on: 0. Risk: high (auth_security; frontier review plus independent test review; no change to PIN, backup or restore logic). Mock first: lock screen, PIN entry, security settings, change PIN, export and restore dialogs.

### Slice 19. Shell and shared dialogs
- Files: `ui/AncApp.kt` (dialogs only), `ui/DeleteUndoUi.kt`, `ui/AncDatePicker.kt`, `ui/InAppCameraDialog.kt`.
- Tests: `DeleteUndoUiTest`, `DraftRecoveryDialogTest`, `AncWorkflowTest`. Depends on: 0. Risk: medium. Mock first: delete confirm, undo bar, draft recovery, new patient info, date picker chrome.

### Slice 20. Report dialog
- Files: `ui/report/PatientTwoPageReportDialog.kt` (level 3), `ReportSections.kt`, `PatientShareText.kt` (text only if copy changes; owner). The print HTML generator is protected and out of scope.
- Tests: report unit tests under `test/.../ui/report/`. Depends on: 0. Risk: high (level 3). Mock first: report dialog on 360 dp.

### Slice 21. Cleanup
- Scope: delete what slices left unused: `FactRow` (if no caller), `ClinicalTagBadge`, `ClinicalRiskBadge`, old `Tag*` tokens, `DesignSystem.SectionCard` once editors moved to `RecordSectionCard`. Run `.agents/scripts/find_unused.py` and `find_duplicates.py`. Only compile-proven removals.
- Depends on: all. Risk: low-medium.

## 4. Mock queue (before the slice that needs it)

Every mock follows mock D's rules: one self-contained HTML file, 360 px frames, before and after, light plus one dark frame, computed contrast table, Compose mapping, real sample data only (Mona Ragab / منى رجب, 27+2 wks, G4 P1 A2, 110/70, A+, EDD 11/02/2027, Mothercare, 12000) and the app's own empty strings. Done: slices 2 to 9 (`docs/designs/redesign/mocks/`). Next: 10 plan, 11 sheets, then editors. Mocks for 12 to 20 can wait until slices 2 to 11 land.

## 5. Owner decisions found while planning

1. **Identity and Medications on Overview.** Mock D drew them, but `OverviewTab` has no Identity or Medications section today (identity is in the hub header, medications in the Meds tab). Slice 1 does not add them. Adding them is a content change; say yes or no.
2. **Previous delivery summary on the card.** Today the card shows a 10 sp line such as "1 CS, 1 SVD" under parity. Mock D drops it. Slice 1B drops it as accepted; say if it must stay (it would come back as the Parity cell's second line).
3. **Arabic labels.** The app has no `values-ar`. Slice 1B lets the card mirror in an Arabic locale but adds no Arabic strings; the Arabic labels in mock D need a localisation decision and the owner's wording review.
4. **"HIGH RISK" in caps.** Slice 1B changes `R.string.high_risk` to "High risk", which also changes the hub header badge text. No test asserts the caps text.
5. **Blank history fields.** Mock D showed "Not recorded" for every blank history field. Today blank fields are hidden. Slice 1A keeps today's rule; showing them is a content change.
6. **BP alert copy** ("▲ Raised BP (≥140/90) · Recheck"): the triangle and middle dot break the antislop copy rules. Copy change, owner, slice 6.

## 6. Owner decisions (2026-11-15)
1. Identity and Medications are not added to Overview: accepted.
2. The card keeps the GPA summary line under Parity (previous delivery summary, "Nulliparous" when none): the owner said it is important. Mock D's removal is overruled. Slice 1 follow-up restores it as the second line of the Parity cell.
3. The app is English by default; no Arabic strings.
4. "High risk" sentence case (also on the hub header): accepted.
5. Blank history fields stay hidden: accepted.
6. BP alert copy loses the triangle and middle dots (slice 6): accepted.

7. **Filter wording.** The list filter says "Medium Risk", the card tag "Moderate risk". Slice 5 changes the filter to "Moderate risk" and sentence case. Say if "Medium" must stay.
8. **Plural.** `active_patients_count` reads "1 Patients". A plurals resource would fix it; slice 5 leaves the text as is.
9. **GA text in the header.** The expanded header prints "27w 2d" (`calculateGestationalAge()`), the sticky bar and the card "27+2 wks" (`shortWeeksText`). Slice 4 keeps both; say if the header should match the card.
10. **Unknown delivery date.** The card shows "???" (a test asserts it). Slice 2 keeps it; "Date not recorded" would match the rest of the record.
11. **Normal risk in the header.** Slice 4 shows a neutral "Normal risk" tag in the header (the list shows none). The existing `normal_risk` string reads "STANDARD RISK" and is left alone.

## 7. Owner decisions 7 to 11 (2026-11-15)
7. Filter wording: "Moderate risk" (not "Medium").
8. "1 patient" singular fixed.
9. Gestational age format everywhere (header, sticky bar, card, reports): "27+2 wks" (weeks+days, the RCOG, NICE and FIGO convention; ACOG's "27w2d" and "27 2/7" are equivalent). Slice 4 changes the header's "27w 2d" to "27+2 wks".
10. Unknown delivery date keeps "???".
11. No "Normal risk" tag in the header for low-risk patients: the header shows a risk tag only for Moderate and High risk. The `NormalRisk` StatusKind is not needed; remove it from the slice 4 brief.


## 8. Owner rule for lists (2026-11-15)

Feedback on the first meds mock: "more elaborating than needed with all these squares". From now on:
- Tinted `LabeledValue` boxes only where the user edits or where a few key facts sit (the Overview sections, the delivery card).
- Dense lists (visits, scans, labs, medications, and later lists) are flat: rows on the tab surface, a heading per record, label-over-value in plain text, hairline dividers between records. No card per record, no box inside a box, no filled pill per value.
- Status in lists is a coloured word (`StatusWord`), not a filled tag; only what needs attention is coloured; non-compliant stays an error word.
- Entry stays fast and low-fatigue: Stop one tap on the row, compact picker and undo bar.
