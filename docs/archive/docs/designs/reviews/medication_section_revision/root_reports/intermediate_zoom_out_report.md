# Stage 8 report: Intermediate Zoom Out

Target: `docs/designs/patient_workspace_overview_tab_optimization.md` (full file, lines 1..248)
Reviewer: `egm-zoom-out`, standard `zoom-out-standard.md`
Inputs: `zoom_out_report.md` (Stage 5), `comprehension_report.md` (Stage 6), `critic_report.md` (Stage 7). The design doc has not been edited since Stage 5, so no fixes were applied.

Verdict: not sound enough to start implementation planning. Every Stage 5 finding is still open. Stages 6 and 7 added safety-relevant Majors and one correction.

## Checks the reviewer ran against the code
- `minimumTouchTarget()` exists: `com.sherifradwan.ancnative.ui.minimumTouchTarget`, imported in `PatientDirectoryCard.kt:46` and used in `MedicationEditor`, `UltrasoundEditor`, `ClinicalPresets`. **This corrects `critic_report.md` C6 and Stage 6 finding 6**, which said the helper does not exist. The real gap is that the doc does not say it is an existing project helper. Downgrade to a Nit.
- `isRhNegative` exists at `ui/ClinicalFormat.kt:8` (`internal`, visible within the module). `previousDeliverySummary` exists at `model/ObstetricHistory.kt:73`.
- `DemoFixtures.kt:21` and `:428` store `"No known drug allergies (NKDA)"` as `allergyHistory`.
- The reviewer did not re-read the three context files in full. Beyond those greps, the remaining points are carried from earlier reports.

## Stage 5 findings: status
| Stage 5 finding | Status | Note |
|---|---|---|
| B1 Regex scraping of GA and weight | Open | Top item in every stage |
| B2 Prior-pregnancy fields dropped, no mapping | Open | Later stages added no mapping; none flagged `lossManagement`, `terminationMethod`, `pregnancyType` |
| M1 Reinvents `ObstetricHistory.kt`; `parsed.doctor` and `parsed.deliveryPlan` do not exist | Open | `parsed.*` confirmed undefined by Stages 6 and 7 |
| M2 Component layer too large; flat-variant not weighed | Open | Critic C9 adds missing alternatives; diagram components still lack files |
| M3 Altitude drops and unmeasured dp figures | Open | Repeated in Stages 6 and 7 |
| M4 Line cap, test tags, 40/48dp row, `canEdit`, string overlap | Open | |
| N1 Header duplication not verified | Open | Strengthened by Stages 6 and 7 |
| N2 Say `isRhNegative` is reused | Open | |
| N3 No rollout or revert; test-plan material in design | Open | Critic C1 raises missing Risks section |

## New findings beyond Stage 5
1. 🟡 **Major. Allergy rule is wrong against the fixtures.** §4.5 item 3 shows the red `errorContainer` banner whenever `allergyHistory` is non-blank, so seeded "NKDA" patients get a drug-allergy alarm. A blank value shows NKDA text, asserting a negative nobody entered. Needs three states (recorded, explicitly negative, not recorded) or a structured flag. `not_recorded` string exists.
2. 🟡 **Major. "Primigravida" is derived from an empty list, not `gravida`.** A G2 P0 patient would be labelled primigravida. Derive from `gravida` and reuse `ObstetricHistory.kt` helpers.
3. 🟡 **Major. `items(patient.previousPregnancies)` inside a `ColumnScope` slot is a crash risk.** It is `LazyColumn` syntax. `OverviewTab` is a `verticalScroll` Column, so a nested lazy list crashes on unbounded height. Use `forEach` and state the expected maximum N. Stage 5 missed this.
4. 🟡 **Major. The document is not a coherent design yet.** Section count is "four" at lines 29 and 80 and five elsewhere. The Documents footer has no owner file and a contradictory treatment (`AttachmentsSectionCard` reuse vs "replace six cards" vs "zero legacy baggage"). Old card files are neither deleted nor kept by 6.2. Stage 6 rated the count a Blocker; this reviewer rates it Major because it is a consistency fix.
5. 🟡 **Major. Undefined governance terms drive design decisions.** "SAMD", "Rule 2/3", "Rule of Reusability", "constitutional rule", "hash invalidation" justify rejecting the schema change and the accordion with no source. The schema freeze is asserted, not shown to be unavoidable. This reinforces B1.
6. 🟡 **Major. No data-flow and ownership spec.** Nothing on who builds edit and delete events, `canEdit` and archived-patient behaviour, or the existing ungated `onEditPatient` on `DeliveryPlanningCard`. An `OverviewActions` holder was suggested in the inventive review.
7. 🟢 **Nit.** The `Regex` is built per call. This goes away if the parser is dropped.
8. 🟢 **Nit. Documentation hygiene.** Unescaped `|` at line 210 breaks the table. The `datingMethod` list differs from `ClinicalModels.kt` (`LMP`, `Ultrasound`, `IVF_TRANSFER`, `Clinical`). The em-dash fallback conflicts with the recent em-dash removal commit.
9. 🟢 **Nit. `minimumTouchTarget` correction (see above).** Add a "reuse existing" note and the import location.

## Scope to resist (reviewer's YAGNI note)
Do not adopt every critic and inventive suggestion. These would grow the design:
- A canonical token (`GA:39+2 BW:3450g`) written by the editor into `otherDetails` (second write path, mixed-format corpus forever).
- A versioned parser with confidence scores and source spans.
- A shared `PatientSummary` state holder.
- A test matrix across font scales 1.0, 1.3, 2.0 plus an RTL and Arabic-digit review.

Simplest fix for B1: delete the parser, show `otherDetails` and `placeAndDoctor` verbatim on tier 2 (as `PreviousPregnancyCard` does), and treat structured biometrics as a separate signed-off change.

## Altitude and simplicity assessment
- The design is still too large for the root problem (card fatigue, duplicated GA and parity, long prior-pregnancy stack). Simpler options never considered: remove duplicated GA and parity fields from existing cards; add one shared flat section scaffold to existing cards, keeping their test tags and `canEdit` handling; render `PreviousPregnancyCard` content as a compact row using the `ObstetricHistory.kt` helpers.
- The doc still pastes composable and regex bodies. Keep interfaces and data flow only.
- The row has no width budget. About 230dp remains for tier 1. Use a min-height, let tier 2 wrap, and use one "More actions" control (the sheet already exists from the prior commit) instead of two 48dp icons.

## Is the design ready for implementation planning? No.
Before planning, the author should:
1. Drop the regex parser (recommended), or explicitly accept and test the misparse risk.
2. Add a field-by-field mapping for prior pregnancies, including where undisplayed fields go.
3. Fix allergy states and the primigravida derivation.
4. Choose `forEach` over `items` and state the expected maximum N.
5. Pick one section count and one footer treatment, and decide the fate of the old card files.
6. Reuse `ObstetricHistory.kt`, `isRhNegative`, `minimumTouchTarget` and `FactRow`; resolve `parsed.*` against the real `DeliveryNoteSections` (`hospital`, `plan`, `postpartum`, `fees`).
7. Add a short Risks and Rollback section, and a test-tag to node mapping with affected androidTests (`AncWorkflowTest`, `DeleteUndoUiTest`).
8. Weigh the flatten-existing-cards alternative in Section 5 and justify the rejection with evidence.
