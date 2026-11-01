# SPEC_DESIGN_R5: entry screens, design B (design rounds only)
Written 2026-11-01 under `.agents/pipeline/PLAN_DESIGN_VS_LOGIC.md` (a round is logic OR design). Owner decisions D20-D22 choose design B for ultrasound, laboratory and medication; mock-ups are `.agents/pipeline/design/d1/index.html`.
Design rounds change Compose files, one shared design-system file and strings only. No model, state, storage or clinical-rule change. Every existing testTag stays.

## Round 5a: shared design-system file + ultrasound screen (mock-up section 2, option B)

### Design-system file (D1, first slice)
U200. New file `ui/DesignSystem.kt` defines, once:
  - `SectionCard(title: String, modifier: Modifier = Modifier, tag: String? = null, content: @Composable ColumnScope.() -> Unit)`: the rounded surface card with 1 dp outline-variant border, 16 dp padding, 12 dp spacing, bold primary `titleMedium` title (same look as today's hand-written cards).
  - `SummaryRow(label: String, value: String, tag: String, onClick: () -> Unit)`: one closed line "label  value" (value shown as `onSurfaceVariant` when it is the "not set" text), 48 dp minimum height, whole row clickable, a chevron at the end.
U201. `UltrasoundEditor.kt` has no direct `Card(` call: every card goes through `SectionCard`. Look, spacing and every existing testTag are unchanged (pure refactor).

### Ultrasound screen
U210. Order of cards: report card (unchanged), date strip, findings, measurements, impression, "More".
U211. Date strip = the existing scan-date field + Today chip + GA weeks + GA days + the calculated-GA / redating hint card, in one `SectionCard` (title: existing string "1. Scan Timing & Gestational Age"). Tags `scanCalculatedGaCard`, `redatingHint`, `applyScanGa` kept. The link-to-visit block leaves this card (see U214).
U212. Findings card (title: existing string "2. Fetus, Placenta & Amniotic Fluid", tag `ultrasoundFindingsCard`) is guided, one finding open at a time. Findings in this fixed order, key -> existing ChoiceGroup (tagPrefix kept): `fetalNumber` (fetalNumberChoice), `presentation` (presentationChoice), `placenta` (placentaChoice), `liquor` (liquorChoice).
  - The open finding shows its ChoiceGroup inside a column with tag `findingOpen-<key>`.
  - Every other finding shows a `SummaryRow` with tag `findingRow-<key>` (label = the finding's existing label string; value = the chosen value, or the new string "Not set").
  - Tapping a closed row opens it and closes the previous one. Exactly one finding is open at most.
  - Choosing a value sends the existing `UltrasoundDraftChanged`, then opens the next finding (in the fixed order, after the one just answered) whose value is blank; if none is blank, none is open.
  - On entry the open finding is the first one with a blank value, else none (state kept with `rememberSaveable`).
  - "Fill normal scan" keeps its existing behaviour (fills only blank fetal number, presentation and liquor; never placenta), then opens the first blank finding. "Clear findings" keeps its behaviour, then opens `fetalNumber`.
U213. Measurements card keeps `biometryCard`, `toggleBiometry` and all fields (FHR, EFW, BPD, HC, AC, FL) exactly as today. No steppers or sliders and no suggested values (reference values are an owner decision, D8).
U214. "More" (tag `ultrasoundMoreToggle`, a `SummaryRow`, label new string "More: photos and visit link", value "Open"/"Close" as new strings or the count of photos): when expanded it shows, in a container with tag `ultrasoundMoreSection`, the existing scan-photos card (`scanAttachmentsCard` with `captureScanPhotoCamera`, `addScanPhoto`, `attachmentItem-*`, `removePhoto-*`) and the existing link-to-visit card (`linkedVisitCard`, `linkVisit-*`, `unlinkVisit`). Initially expanded exactly when the draft already has an attachment or a linked visit; otherwise collapsed. State kept with `rememberSaveable`.
U215. Impression field, `impressionFromChoices` button and the security-vault indicator stay as they are, in their own `SectionCard`.
U216. Touch targets 48 dp; colours only from the theme; no hard-coded colours; no new library; works at 160% font scale (no text runs vertically).
U217. Strings: only add `finding_not_set` ("Not set"), `ultrasound_more` ("More: photos and visit link"), and what U214 needs for open/close. No string deleted. English only.

### Static check (S200-S203, `.agents/checks/check_r5a.py`)
S200 `DesignSystem.kt` defines `SectionCard` and `SummaryRow` once each; no other file defines either.
S201 `UltrasoundEditor.kt` calls `SectionCard(` and `SummaryRow(`, and has no direct `Card(` call.
S202 All tags named in U211-U215 appear in `UltrasoundEditor.kt` (or via a tagPrefix for the ChoiceGroups).
S203 No `.kt` file outside `ui/` changed (git diff names only `ui/DesignSystem.kt`, `ui/UltrasoundEditor.kt`, `res/values/strings.xml`, tests, checks, spec, pipeline files).

### Device/UI tests (independent author, from this spec, `androidTest`)
T200 Opening the editor with an empty draft: `findingOpen-fetalNumber` exists; the other three findings show `findingRow-*`.
T201 Choosing a fetal number opens `findingOpen-presentation` and shows `findingRow-fetalNumber` with the value.
T202 Fill normal scan on an empty draft: `findingOpen-placenta` is open, and fetal number, presentation and liquor rows show "Singleton", "Cephalic", "Normal".
T203 Tapping `findingRow-fetalNumber` after that opens it and `findingOpen-placenta` is gone.
T204 With a linked visit or a photo in the draft, `ultrasoundMoreSection` exists on entry; with neither it does not, and tapping `ultrasoundMoreToggle` shows it.
T205 Report card text still follows the choices (existing `ultrasoundLiveSummary`).

## Round 5b: laboratory entry screen (mock-up section 1, option B; decisions D20, D21)
Design only. Uses today's per-test draft and panel queue unchanged (no batch state). Assumption (owner has not decided D7): the phone's own keyboard is used for the result, not an on-screen keypad. NOT built here, because each needs logic first: a Review tab listing every pending test, "mark empty yes/no tests as normal", and a Back button between panel tests.

L300. `ui/DesignSystem.kt` gains one shared piece, defined once: `BigChoiceButton(label: String, selected: Boolean, tag: String, onClick: () -> Unit, enabled: Boolean = true)`: full-width outlined button, 56 dp minimum height, check mark and primary-tinted container when selected, theme colours only.
L301. `LabEditor.kt` has no direct `Card(` call: every card is `SectionCard`. Look, spacing and every existing testTag unchanged unless a spec item below says otherwise.
L302. Card order: header strip (lab name + date + GA hint; tags `pinnedLabHeaderTag`, `labCalculatedGaCard`, `labDate` kept), test card (tag `labTestCard`), add-tests card (tag `labAddTestsCard`), vault indicator. The pinned action bar (`cancelLab`, `addAnotherLab`, `saveLab`) is unchanged.
L303. Test card top: a progress line (tag `labTestProgress`): "Test {i} of {n} · {category}" while a panel queue is running (panel count above 1), otherwise just the category when it is not blank (nothing when blank). Then the test name as a large title (tag `labTestTitle`; "New test" string when the name is blank), then the existing light unit/reference-range summary line.
L304. Editing details: a toggle (tag `labEditDetailsToggle`, text button "Edit test details") reveals, together, the existing name (`labName`), unit (`labUnit`) and reference range (`labRefRange`) fields. Open on entry exactly when the name is blank or both unit and range are blank (today's rule), otherwise closed; `rememberSaveable`. The old per-field "Edit unit and range" button goes away.
L305. Result: the existing `labResult` field stays, full width, keyboard unchanged, with the existing flag pill shown directly under it when the flag is known. The four preset results (Negative, Non-reactive, Normal, Positive) become `BigChoiceButton`s, one per row, selected when the result equals the preset, tags unchanged in form (`labResultPreset-<index>`, same index order as today), tapping sends the existing `LabDraftChanged(RESULT, value)`.
L306. Below the result, two toggle chips in one row: `labFlagToggle` ("Flag", with "· abnormal" appended when isAbnormal) and `labNoteToggle` ("Note", with a check mark when notes are non-blank). Flag section (the existing Switch with tag `labAbnormal` and its label) is open on entry exactly when isAbnormal; note section (the existing `labNotes` field) is open on entry exactly when notes are non-blank. `rememberSaveable`. Automatic abnormal flag logic (the LaunchedEffect) is untouched.
L307. Add-tests card: title via a `SummaryRow` (tag `labAddTestsToggle`, label "Add tests", value Open/Close). Expanded: the existing panel chips, common tests, "Save to my list" banner and category field with every existing tag (`allLabTestsChip`, `labGroupChip-*`, `saveToLabCodexBanner`, `saveToLabCodexButton`). Open on entry exactly when the test name is blank and no panel queue is running; `rememberSaveable`.
L308. 48 dp touch targets (56 dp for `BigChoiceButton`); theme colours only; works at 160% font scale; no new library; English only.
L309. Strings: add only what L303-L307 need, each once; delete none.

### Static check (S210-S213, `.agents/checks/check_r5b.py`)
S210 `DesignSystem.kt` defines `BigChoiceButton` once; `SectionCard`/`SummaryRow` still defined once; no other file defines any of them.
S211 `LabEditor.kt` calls `SectionCard(`, `SummaryRow(` and `BigChoiceButton(`, has no direct `Card(`.
S212 All tags named in L302-L307 (and the kept ones `labName`, `labResult`, `labUnit`, `labRefRange`, `labNotes`, `labAbnormal`, `labResultPreset`) appear in `LabEditor.kt`.
S213 No file under model/, state/ or storage/ changed against HEAD.

### UI tests (independent author, `androidTest/UltrasoundGuidedFlow`-style, file `LabEntryDesignTest.kt`)
T300 New lab (blank name): `labEditDetailsToggle` exists, `labName` and `labResult` are displayed, `labAddTestsToggle` shows the add-tests content (`allLabTestsChip` exists).
T301 With a loaded test (name "Hemoglobin", unit "g/dL", range "11-14"): `labTestTitle` shows the name, details closed (`labName` does not exist), `labAddTestsCard` content closed.
T302 Tapping `labEditDetailsToggle` shows `labName`, `labUnit`, `labRefRange`.
T303 Tapping preset button `labResultPreset-<index of Positive>` sets the result (the `labResult` field contains "Positive").
T304 Result "9" with range "11-14" shows the flag as abnormal: the `labAbnormal` switch is on after opening `labFlagToggle`.
T305 `labNoteToggle` opens `labNotes`; text typed there reaches the draft.
T306 Existing: `saveLab`, `cancelLab` still displayed.

## Round 5c: medication entry screen (approved mock d4/d1 section 3, option B; decisions D20, D22)
Design only, on today's single-medication draft. NOT built here because it needs new state first (a later logic round): the session list "Today's list" with several drugs, Stop with Undo, and "Save N changes". Decision D22 overrides the mock: NO Prophylaxis button, and exactly four statuses (Active, Completed, Discontinued, Patient non-compliant). "+ Add other" waits for the custom-choices logic round.

M400. `MedicationEditor.kt` has no direct `Card(` call: every card is `SectionCard` (from `ui/DesignSystem.kt`; no new shared piece). Every existing testTag stays unless an item below says so.
M401. Card 1 "Medication" (tag `medPickCard`): the name field `medName` (typing a new drug allowed), the patient's previous medications (`patientPrevMed`, with its safety reset unchanged), the common-medication chips (`medPreset`; one tap still fills name, dose, route, frequency and start date today exactly as now) and the "add to my list" banner (`saveToMedCodexBanner`, `saveToMedCodexButton`).
M402. Card 2 "Dose and schedule" (tag `medDetailsCard`): four guided rows, one open at a time, fixed order: `dose`, `route`, `frequency`, `start`.
  - Closed row: `SummaryRow` with tag `medRow-<key>`, label = the field's existing label string, value = the current value or the string "Not set" (route defaults to "Oral" so it shows Oral).
  - Open row: a column with tag `medOpen-<key>` holding the existing controls: dose = `medDosage`; route = the existing `ChoiceGroup` (`medRouteChoice`); frequency = the frequency chips (`freqPreset`) and `medFrequency`; start = `medStartDate` with its Today button.
  - At most one row open. Tapping a closed row opens it and closes the other. On entry, and after choosing a preset, a route, a frequency chip or the Today button, the first row whose value is blank opens (none if all are filled). Typing in a text field never moves the open row. The "first blank row" rule is ONE small private function used by every case.
  - State kept with `rememberSaveable`.
M403. Card 3 "Status" (tag `medStatusCard`): exactly four `BigChoiceButton`s in this order: Active, Completed, Discontinued, Patient non-compliant (the existing `ClinicalVocabulary.medicationStatuses` and `effectiveMedicationStatus` decide selection; no new rule). Tags `medStatusChoice-<index>` with the same index order the old ChoiceGroup produced. Tapping sends the existing `MedicationDraftChanged(STATUS, value)`.
M404. "More" (tag `medMoreToggle`, a `SummaryRow`): expanded shows, in a container tagged `medMoreSection`, end date (`medEndDate`), adverse effects (`medAdverseNotes`) and notes (`medNotes`). Initially expanded exactly when any of the three is non-empty (today's rule); `rememberSaveable`.
M405. No Prophylaxis control, text or chip anywhere on the screen. The codex item created by "add to my list" keeps `isProphylaxis = false`.
M406. The security-vault indicator and the action row (`cancelMedication`, `addAnotherMedication`, `saveMedication`) stay as they are.
M407. 48 dp touch targets (56 dp for `BigChoiceButton`); theme colours only; works at 160% font; short labels stay on one line; no new library; English only.
M408. Strings: add only what M402-M404 need ("Not set" already exists as `finding_not_set`: reuse it), each once; delete none.

### Static check (S220-S223, `.agents/checks/check_r5c.py`)
S220 `MedicationEditor.kt` calls `SectionCard(`, `SummaryRow(` and `BigChoiceButton(` and has no direct `Card(`.
S221 It carries every tag in M401-M404 and the kept ones (`medName`, `medDosage`, `medFrequency`, `medStartDate`, `medEndDate`, `medAdverseNotes`, `medNotes`, `medRouteChoice`, `freqPreset`, `medPreset`, `patientPrevMed`, `saveToMedCodexBanner`, `saveToMedCodexButton`, `cancelMedication`, `addAnotherMedication`, `saveMedication`, `medStatusChoice`).
S222 The word "Prophylaxis" (any case) does not appear in `MedicationEditor.kt` outside the one `isProphylaxis = false` assignment.
S223 No file under model/, state/ or storage/ changed against HEAD.

### UI tests (independent author, file `MedicationEntryDesignTest.kt`)
T400 New medication (blank draft): `medOpen-dose` exists and `medDosage` is displayed; `medRow-route` shows "Oral"; `medRow-frequency` and `medRow-start` exist; `medPickCard` has `medName` displayed.
T401 Tapping a `medPreset` chip fills name and dose (the `medRow-*` values or fields reflect it) and opens the first blank row (not dose).
T402 Tapping `medRow-frequency` opens it (`medOpen-frequency` exists) and `medOpen-dose` is gone.
T403 Exactly four `medStatusChoice-*` buttons exist; none says Prophylaxis; tapping the fourth sets the draft status so that Patient non-compliant is selected.
T404 With an end date or adverse note in the draft, `medMoreSection` exists on entry; with none it does not, and tapping `medMoreToggle` shows it.
T405 `saveMedication`, `cancelMedication` and `addAnotherMedication` are displayed.
