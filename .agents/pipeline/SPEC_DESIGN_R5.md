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
