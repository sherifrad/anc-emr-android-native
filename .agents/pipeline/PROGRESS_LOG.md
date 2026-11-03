# PROGRESS LOG (append only; newest at the bottom)
Keeps the history from the handoff until Claude returns after its quota reset. Format: date time | change | commit | verified | NOT verified | owed next.

## 2026-10-31 to 2026-11-01 (Claude, summarised at handoff)
- Release 1.0.1 (code 2) built and installed over the clinic app (commit 28f7950). The owner then saw the old screens: the earlier rounds delivered logic and a thin UI, not the option B layout and flow. Corrected; plan split into logic rounds and design rounds (PLAN_DESIGN_VS_LOGIC.md).
- Dating rules (346425b, 14a24ff): one due-date rule per method. Verified by unit tests.
- Ultrasound design round (80e6416): shared design-system file, guided findings, More section. Verified: unit, lint, release compile, device-test compile green; 8 new tests pass on emulator API 36; screenshot checked. NOT verified: full suite, frontier and independent reviews, "Clear findings" test.
- Lab round r5b (6ef0f9c): pipeline passed; the orchestrator's own review found and fixed (a) the auto abnormal-flag effect could overwrite a saved flag on open, (b) open-on-entry flags were not re-derived after Save and add another, (c) chips and banner could wrap vertically, (d) new strings were hard-coded. One test (T304) was corrected by the orchestrator because it asserted the unsafe behaviour. Verified: gate green; 7 lab + 9 workflow + 8 ultrasound emulator tests pass. NOT verified: screenshot, 160 percent text, frontier-tier review, full suite.
- Incident: a mistyped shell heredoc accidentally launched the r5b pipeline once more (failed UI lane on an opencode free model, no tracked file changed, HEAD still 6ef0f9c). The r5b brief (code instructions only, no patient data) went to that model. Lesson: always quote heredocs that contain backticks.

## 2026-11-01 (Claude) design approval
- Owner rejected the ChatGPT-style palette mock. Owner approved: option B (lab, ultrasound, medication); the options titled Recommended in the second mock set (history, previous pregnancies, dating, clinical attention, delivery plan); my directory, chart and visit entry mocks. Overlapping screens in my set were dropped in favour of the Recommended ones. Final clickable set: design/d4 and artifact https://claude.ai/artifact/LUtQqawvTycThDsPd5652E (version 3).
- New owner requirements recorded in HANDOFF_AGY.md: small-screen (Honor 400) sizing, bounded A-/A+ text size in the app and the mocks (90-130 percent, one-line labels never cut), light default, add-your-own values saved for later patients, clinical attention entry field.
- Mocks verified in a browser at 360 px at 90-130 percent text (no wrapped labels, no console errors). NOT verified: a real device, dark mode visually.
- Owed next: custom-choices logic round, medication round r5c, text-size control, then the screens in HANDOFF_AGY.md section 2.5. Also owed: reviews listed in section 1.

## 2026-11-02 (stop point)
- Medication entry design (r5c) built, reviewed, fixed, emulator-tested (7 new tests + workflow tests pass), committed f4b264a.
- Collapsible medication list in the chart (r5d, owner request): spec, check, briefs, independent tests written; agy build was still running when the owner stopped. Uncommitted partial edits in WorkspaceScreen.kt, strings.xml and MedicationListCollapseTest.kt. Next: if the run did not finish, re-run `python3 .agents/pipeline/run_pipeline.py --round r5d --ui-only` (after `set -a; source .agents/pipeline/LANES.env; set +a`), then read reviewer outputs, run the gate and the emulator tests, commit.

## 2026-11-02 (Claude) delete / Undo logic and the global mechanism
- Delete, restore and Undo for medications and lab results (19c36fc), reviewed by Sonnet, agy and Opus; fixes in e8285ff (create draft rebase, frozen retry, single restore, one restart); real-database emulator test 46 cases. Verified: gate green, DB test pass. NOT verified: Undo timer and lock clearing end to end, retried committed restore with the same operation id.
- Owner asked for one global mechanism for all sections with strict boundaries between sections and patients. Spec `SPEC_LOGIC_R6B.md` (1afc2a3). Storage layer generalised (9ceef0d): one registry, generic commands and repository, 54 emulator DB cases incl. isolation and integrity checks. Check script for r6a updated (645bbfc).
- Round r6b2 (shared state + Undo coordinator) launched, tests lane done (2 files, 104 checks ran); core lane running. Owed next: read reviews, gate, commit; r6b3 shared UI; r6b4 wire medication and lab; r6b5 other kinds; r6b6 attachments; then visual redo against the mock. Still emulator only, nothing on the phone, nothing pushed.

## 2026-11-02 (Claude) delete / Undo: shared UI, review fixes, phone test (disclosed: log edit by the orchestrator)
- Shared Undo coordinator (404a35a), shared confirm dialog, Undo bar and delete icon wired to medications and lab results (cdd0308), collapsible long lists. Reviews by Opus found: failures not shown, Undo bar on other patients, Undo of a save vs an open editor, notices on the wrong patient, second delete while one runs. Fixed in dac3f46, then a second Opus review found five more (failure after patient switch, Undo during a running delete, editor during a running Undo, icon name copy, empty patient clause); fixed in 0b8e380 and 0fba1b6. Reuse clean-up and guide ADDING_A_DELETABLE_KIND.md, check_r6c.py, registry completeness test in 7f26beb.
- Verified: gate green, all check_r*.py pass, DeleteUndoUiTest 20 of 20 on the emulator (re-run on the final commit), 54 real-database storage cases earlier. On the owner's phone (QA copy only): delete with confirm removes the card and shows the Undo bar naming the patient, the bar expires, the owner confirmed Undo restored the item.
- NOT verified: lock during the countdown on the phone (owner: delete is final immediately), "Save and next" Undo on the phone (owner: not needed), Undo window vs system accessibility timeout, whether repository.execute can throw, rendering of the new failure message on a narrow screen. Opus re-review and independent test review of 0b8e380/0fba1b6 pending.
- Lab saves do not offer Undo of a save (medication only), as requested. Next: finish review fixes, then Part B visual redo against the approved mock. Nothing pushed.

## 2026-11-03 (Gemini) Medication review fixes, design nits, full verification
- Level-3 review of medication commits (33e63cd, 2fc5e1e, 4761f3f, e6642ca, 86f5fb9): detached Opus review completed (`launch_opus_review.sh`), verdict PASS WITH NITS.
- Independent test review confirmed: unit tests added in `MedicationStatusTest` and `AncDeleteUndoStateTest` fail without the fixes.
- Logic fixes committed in `8f8da08`:
  - `ClinicalSummaryGenerator`: aligned with card and print (`medicationInstructions` and `medicationStatusLabel`).
  - `MedicationEditor`: cleared hidden fields (frequency, end date, adverse notes) on picking presets and past prescriptions.
  - `AncViewModel`: chained medication starts today by default.
  - `AncState`: fail-fast on unhandled edit kinds without hardcoding deletable kinds (satisfying static check R6C).
  - Recorded owner decision D24 in `OWNER_DECISIONS.md`.
  - Optimized `PrintLayoutTest` CSS regex to eliminate catastrophic backtracking.
- Design nits committed in `675a4fa`:
  - `WorkspaceScreen`: capped collapsed card instructions to 2 lines with ellipsis (full on expand).
  - `WorkspaceScreen`: removed uneditable End date row from expanded card.
  - `WorkspaceScreen` & `strings.xml`: separated `medications_tab` ("Meds") from `medications` ("Medications") so profile section header and overview button show "Medications" while tab displays "Meds".
  - `MedicationEditor`: single-line action bar "Save & add another" button (`softWrap = false`, `maxLines = 1`, `labelMedium`, `weight(1.8f)`).
- Verification:
  - `run_build_gate.sh`: GATE OK.
  - `run_static_checks.sh`: STATIC CHECKS OK.
  - Emulator (`emulator-5554` API 26, 360 dp / density 480):
    - `MedicationListCollapseTest`: 4/4 passed.
    - `DeleteUndoUiTest`: 21/21 passed.
    - `MedicationEntryDesignTest`: 7/7 passed.
    - `AncWorkflowTest`: 9/9 passed.
    - `LabEntryDesignTest`: 7/7 passed.
    - `UltrasoundGuidedFlowTest`: 8/8 passed.
    Total: 56/56 passed.
  - Rendered report HTML via `PrintSampleDumpTest` and verified medication section formatting.
  - Captured emulator screenshot of Stopped state (`.agents/pipeline/design/medication-redo2/5-stopped-state.png`).
- NOT verified:
  - Real device (Honor 400) tap testing (owner must unlock and clear chat bubbles).
  - Dark mode and RTL visually.
- Next: Logic round l06 (custom choices store for "+ Add other"), then design round for medication "Today's list" (REDESIGN_SPECS 4.3).

## 2026-11-03 12:47 Logic round l06 (custom choices store for "+ Add other")
- Commit: `fd8b7c5`
- Implemented core custom choices store and validation per Section 2.3 & REDESIGN_SPECS:
  - `CustomChoiceModels.kt`:
    - `CustomChoiceList` enum (17 lists covering medications, labs, ultrasound findings, previous pregnancy, clinical attention, and visit vitals).
    - Clinical safety invariant: dating methods (LMP, IVF, Scan, Clinical) strictly excluded.
    - Pure validation function `validateCustomChoice`: rejects control characters (C0, C1, NUL, tabs, newlines, Unicode separators), rejects blank/whitespace, enforces length limits (140 for attention phrases, 80 default), case-insensitive duplicate check.
    - `CustomChoicesDelegate` interface with `addCustomChoice`, `renameCustomChoice`, `removeCustomChoice`, `restoreCustomChoice` (returning `RemovedCustomChoice` with `originalIndex` for precise Undo).
  - `ClinicalCodexRepository.kt`:
    - Implemented `CustomChoicesDelegate` backed by SharedPreferences per list.
    - Full synchronization on `customChoicesLock` preventing lost-update races between background restore and main-thread UI mutations.
    - Duplicate protection on Undo: prevents duplicate entry if matching label was re-added before tapping Undo.
    - Export / restore functions: `exportCustomChoicesForBackup()` and `restoreCustomChoicesFromBackup()` with validation on import.
  - `EncryptedBackupManager.kt` & `VaultController.kt`:
    - Backup envelope carries optional `customChoices` payload while preserving `schemaVersion = 3`.
    - Non-fatal custom choice parsing: custom choice parse error never fails or blocks patient record restore.
    - Legacy backups without `customChoices` continue to parse cleanly without error.
  - Reusable scripts added to `.agents/scripts/`:
    - `run_unit_test.sh`: run individual test classes offline.
    - `set_emulator_360dp.sh` & `reset_emulator_density.sh`: configure and restore 360 dp display density.
- Verification:
  - Unit tests: `CustomChoicesLogicTest` (10/10 passed, testing validation, dating safety, store reload from FakeSharedPreferences, undo duplicate prevention, backup roundtrip).
  - Independent test review: confirmed tests fail without implementation.
  - Frontier review: Opus review (`.agents/pipeline/review_l06_custom_choices.out.txt`) verdict `PASS WITH NITS`; all should-fix items addressed (lock synchronization, undo duplicate check, control-character validation, real repository tests).
  - Real Android OS test on `emulator-5554`: `CustomChoicesStoreInstrumentedTest` (1/1 passed).
  - `run_build_gate.sh`: GATE OK.
  - `run_static_checks.sh`: STATIC CHECKS OK.
- NOT verified:
  - Physical phone (Honor 400) execution.
- Next: Design round for medication "Today's list" (REDESIGN_SPECS 4.3).

## 2026-11-03: Design round: Medication "Today's list" (REDESIGN_SPECS 4.3, commit e772dff)
- Built & verified:
  - `SPEC_DESIGN_TODAYS_LIST.md` documenting UI blocks M420 ("Tap to add" tiles, search filter, Common/Recent tabs, "+ Add other" integration), M421 ("Today's list" active cards with Stop control), M422 ("Stopped in this session" section with Restart control), and outcome oracles O1-O6.
  - `WorkspaceScreen.kt`:
    - `MedicationTapToAddSection`: dynamic tiles from `ClinicalCodexRepository.medicationCodexItems` + custom choices, Recent tab showing real medication history, one-tap add for standard drugs, safe editor routing for custom/undosed drugs, "+ Add other" dialog using `addCustomChoice` validation.
    - `FlowRow` for "+ Add other" and "+ Add medication" buttons to prevent clipping at 360 dp or high font scales.
    - Guarded draft dispatch via `canStartEditor` ensuring no operations corrupt active drafts when editing is blocked.
    - `MedicationsListContent`: "Today's list" section card with active count summary, accessible 48 dp Stop button, and "Stopped in this session" section showing medications stopped today with 1-tap Restart (clearing end date and preserving original start date).
    - `MedicationCard`: 48 dp minimum touch target on edit button, 2-line ellipsis on instructions line, full text on expansion.
  - Strings in `strings.xml`: `tap_to_add`, `tap_to_add_sub`, `todays_list`, `search_drugs_placeholder`, `tab_common`, `tab_recent`, `stopped_in_this_session`, `stop_medication`, `restart_medication`, `already_on_todays_list`, `add_other_choice`, `custom_drug_title`, `custom_drug_label`, `active_count_summary`, `no_recent_medications`, `set_dose_in_editor`, custom drug validation errors.
- Verification:
  - `run_build_gate.sh`: GATE OK.
  - `MedicationTodayListTest`: 9/9 passed on `emulator-5554` (verifying rendering, 1-tap add, stop dispatch, restart date preservation, session filtering, active tile prevention, custom drug editor routing, and 48 dp minimum touch targets).
  - Regressions: `MedicationListCollapseTest` (4/4 passed), `DeleteUndoUiTest` (21/21 passed), `MedicationEntryDesignTest` (7/7 passed), `AncWorkflowTest` (9/9 passed), `LabEntryDesignTest` (7/7 passed), `UltrasoundGuidedFlowTest` (8/8 passed).
  - Frontier review: Two rounds through `launch_opus_review.sh`. All hard blockers (codex dynamic binding, safe custom dosage, history preservation on restart, session filtering) and should-fixes addressed.
  - Captured screenshots at 360 dp density: `6-todays-list-top.png` and `7-todays-list-bottom.png`.
  - `run_static_checks.sh`: STATIC CHECKS OK.
- NOT verified:
  - Physical phone (Honor DNY_NX9) touch testing.
- Next: Logic round l08 then design d28: visit entry rules and the visit screen (4.4).

## 2026-11-03: Logic round l08 (Visit entry rules, BP flagging, normal fill, and draft persistence)
- Commit: `a07d616`
- Implemented core domain logic and state/draft persistence per Section 4.4, mock d4/d3, and `SPEC_LOGIC_L08_VISIT_ENTRY.md`:
  - `VisitEntryLogic.kt`:
    - Blood pressure evaluation: `isHighBloodPressure` delegates to canonical `bpLevel` in `VisitVitals.kt`, flagging $\ge 140/90$ (RAISED/SEVERE) and single-sided elevations.
    - Normal fill rule: `fillNormalVisitFindings` strictly sets oedema (`None`), fetal movements (`Good`), urine protein (`Negative`), while leaving vitals untouched.
    - Historical vitals: `resolveLastVisitVitals` pulls latest visit chronologically by ISO date (excluding active visit id when editing).
    - Fundal height estimation: `estimateFundalHeightForGa` clamps values to 10..50 cm.
    - Findings builder & tokenizer: `buildVisitFindings` and `parseVisitFindings` use strict `" · "` token boundaries, cleanly separating structured tokens (FH, Oedema, Movements, Urine) from narrative `additionalFindings`.
    - Flagged findings detection: `detectFlaggedFindings` flags High BP, Marked/Mild oedema, Reduced/Absent movements, and proteinuria (Trace, +, ++).
    - Summary formatting: `formatVisitSummary` formats vitals and findings with fallback placeholders for single-sided BP.
  - `VisitVitals.kt`:
    - Added `bpLevel(systolic: Int?, diastolic: Int?)` overload in the canonical vitals location, preserving single threshold source of truth (respecting check S80 and "one function per rule" principle).
  - `CustomChoiceModels.kt`:
    - Added `CustomChoiceList.VISIT_BP`, `VISIT_WEIGHT`, and `VISIT_FUNDAL_HEIGHT`.
  - `AncState.kt`:
    - Added `DraftField` entries: `SYSTOLIC`, `DIASTOLIC`, `FUNDAL_HEIGHT`, `OEDEMA`, `FETAL_MOVEMENTS`, `URINE_PROTEIN`.
    - Added 6 structured fields to `DraftValues`.
    - Bidirectional non-sticky sync in `AncReducer.updateDraft` between combined BP string and separate systolic/diastolic fields; clearing BP clears both components.
    - Updated `DemoVisit.toDraftValues()` to populate structured fields via `parseVisitFindings`.
  - `SaveVisitCommand.kt`:
    - Structured fields are authoritative when present; composes `buildVisitFindings` while preserving `additionalFindings`.
    - Validates partial BP (single-sided systolic/diastolic) as `BLOOD_PRESSURE_INVALID`.
  - `DraftMapping.kt`:
    - Encodes and decodes the 6 structured visit fields in `DraftRecord.fields` with backwards-compatible string fallback parsing.
- Verification:
  - Unit tests:
    - `VisitEntryLogicTest`: 12/12 passed (testing BP flagging, normal fill, history lookup, FH estimation, token parsing, flagged findings, summaries).
    - `VisitDraftStateAndCommandTest`: 12/12 passed (testing sync, clearing, partial BP validation, structured findings override, draft persistence).
    - `CustomChoicesLogicTest`: 11/11 passed.
    - `DraftMappingTest`: updated and passed.
  - Frontier review: DeepSeek frontier review (`review_l08_visit_entry.out.txt`) verified 907 tests across 84 classes; identified 4 Hard Blockers (HB-1 findings override, HB-2 cleared BP resurrection, HB-3 regex tokenizer greediness, HB-4 draft recovery field omission) and Should-Fixes, all remediated with verified tests.
  - `run_build_gate.sh`: GATE OK.
  - `run_static_checks.sh`: STATIC CHECKS OK.
  - Emulator (`emulator-5554` API 26): `AncWorkflowTest` 9/9 passed.
- NOT verified:
  - Physical phone (Honor DNY_NX9) execution.
- Next: Design round d28: Visit and vitals screen redesign (REDESIGN_SPECS 4.4 and mock d4/d3).

## 2026-11-03: Design round d28 (Visit and vitals screen redesign)
- Commit: `ce296c7`
- Built & verified per Section 4.4, approved mock `d4/d3/index.html` section 3, and `SPEC_DESIGN_D28_VISIT_ENTRY.md`:
  - `DesignSystem.kt`:
    - `SegmentedProgressBar`: horizontal segmented step indicator with 48 dp min touch target, 4 dp height bars, primary color for current/completed steps, and `Role.Tab` accessibility semantics (`stepSegment-0`..`stepSegment-3`).
    - `ValueStepper`: centered row with 48 dp minus/plus icon buttons, styled numeric text field supporting `keyboardType` (`Decimal` for weight, `Number` for fundal height).
  - `VisitEditor.kt`:
    - Rebuilt into a 4-step guided wizard (`rememberSaveable` state):
      - Step 0 (Date & Blood Pressure): "Today" / "Yesterday" / other date picker; "Same as last (sys/dia)", "Normal (110/70)", codex presets, "+ Add other"; dual inputs (`visitSystolic`, `/`, `visitDiastolic`); high BP warning banner in `secondaryContainer` (`#FFD9DE` / `#934F5B`) when BP $\ge 140/90$.
      - Step 1 (Weight & Fundal Height): "Same as last", codex presets, "+ Add other"; `ValueStepper` for weight (- / + 0.1 kg) and fundal height (- / + 1 cm).
      - Step 2 (Findings): "Fill normal values" button in `secondaryContainer` with caption note; `BigChoiceButton` cards for Oedema (Marked highlighted), Fetal Movements (Reduced/Absent highlighted), Urine Protein (Trace/+/++ highlighted), codex presets, "+ Add other".
      - Step 3 (Review & Save): Summary card (`visitSummaryCard`) displaying `formatVisitSummary`, red alert badges (`detectFlaggedFindings`), and green pill ("Nothing flagged"); 7 clickable review rows jumping directly to their wizard step; optional notes field; pinned "Save visit" button.
    - Pinned bottom bar using `EditorPage` slot: Cancel + Next on step 0; Back + Next on steps 1-2; Back + Save visit on step 3.
    - Closed color palette adherence using `MaterialTheme.colorScheme` tokens (`secondary`, `secondaryContainer`, `tertiary`, `tertiaryContainer`, `primary`).
    - S70 compliance preserved (`R.string.visit_date_in_future`, `R.string.blood_pressure_invalid`, `R.string.weight_invalid`, calling `bpLevel`).
  - Added reusable scripts in `.agents/scripts/`:
    - `screenshot_mock_html.sh`: captures HTML mock files at specified resolutions via headless Chrome.
    - `qa_open_visit_editor.sh`: unlocks QA app on emulator, enters PIN, and navigates to the new visit editor at 360 dp.
  - Screenshots captured at 360 dp density:
    - `visit-mock.png`: mock reference screenshot.
    - `visit-built-step0.png` (Date & BP).
    - `visit-built-step1.png` (Weight & Fundal Height).
    - `visit-built-step2.png` (Findings & Normal Fill).
    - `visit-built-step3.png` (Review & Save).
- Verification:
  - Unit tests: 915 tests across 84 suites passed.
  - Gate: `run_build_gate.sh` (`GATE OK`).
  - Static checks: `run_static_checks.sh` (`STATIC CHECKS OK`).
  - Emulator (`emulator-5554` API 26):
    - `VisitEntryDesignTest`: 5/5 passed (testing step advancement, segments jump, high BP warning box, "Same as last" presets, "Fill normal values" non-overwrite invariant, review summary and flagged alerts).
    - `AncWorkflowTest`: 9/9 passed.
  - Frontier review: DeepSeek reviewed; addressed all color token hardcodings, step state persistence (`rememberSaveable`), integer fundal height stepper, CTA copy alignment (`save_visit`), and unused imports.
- NOT verified:
  - Physical phone (Honor DNY_NX9) execution.
- Next: Part B: Lab and ultrasound polish (REDESIGN_SPECS 4.1 and 4.2), or next roadmap item.

## 2026-11-03 - Ultrasound Delete Adapter (committed locally as fcff3bc)
- Scope: Implemented Ultrasound Delete Adapter according to `.agents/pipeline/ADDING_A_DELETABLE_KIND.md`.
- Changes made:
  - `model/ClinicalModels.kt`: `DemoUltrasoundScan` implements `DeletableRecord` (`override val id`, `override val patientId`, `override val revision = 1`).
  - `storage/DeletableRecords.kt`: added `DeletableType.ULTRASOUND`, registered `UltrasoundDeleteAdapter` in `DeletableRegistry`, wired `is DemoUltrasoundScan` in `adapterFor`, added `ultrasoundContentHash()`.
  - `storage/UltrasoundDeleteAdapter.kt`: implemented `DeletableRecordAdapter<DemoUltrasoundScan>` (`type = ULTRASOUND`, `fingerprintTag = "US"`, `deleteReceiptKind = "ULTRASOUND_DELETE"`, `restoreReceiptKind = "ULTRASOUND_RESTORE"`, `section = ULTRASOUND`, `canonical`, `load`, `deleteRow`, `insertRow`, `record`).
  - `storage/RecordsDao.kt`: added query `deleteUltrasoundScan(id, patientId, expectedRevision)`.
  - `storage/ClinicalCommands.kt`: added `ULTRASOUND_DELETE` and `ULTRASOUND_RESTORE` to `ClinicalRecordKind`.
  - `state/AncState.kt`: added `ULTRASOUND_DELETED`, `ULTRASOUND_RESTORED`, `ULTRASOUND_SAVE_UNDONE` to `SavedNotice` and mapped in `stringRes()`; guarded `startUltrasoundEditor` with `undoRunningFor(scanId)`.
  - `state/EditorHooks.kt`: implemented `UltrasoundEditorHook` and wired in `EditorHooks`.
  - `values/strings.xml`: added `deletable_type_ultrasound`, `ultrasound_deleted`, `ultrasound_restored`, `ultrasound_save_undone`.
  - `ui/DeleteUndoUi.kt`: wired `DeletableType.ULTRASOUND -> R.string.deletable_type_ultrasound` in `labelResource()`.
  - `ui/WorkspaceScreen.kt`: updated `UltrasoundCard` with `DeleteIconButton` trailing affordance and wired `onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, scan.id)) }` in ultrasound section and timeline standalone scan.
  - `.agents/checks/check_r6c.py`: updated `KIND` regex and allowed `UltrasoundDeleteAdapter.kt` in `FREE`.
  - Scripts: added `.agents/scripts/qa_open_ultrasound_tab.sh`.
- Verification:
  - `python3 .agents/checks/check_r6c.py`: PASS (all 7 rules R6C0-R6C6).
  - Gate: `.agents/scripts/run_build_gate.sh` (`GATE OK`, all 915 unit tests passed).
  - Static checks: `.agents/scripts/run_static_checks.sh` (`STATIC CHECKS OK`).
  - Emulator (`emulator-5554` API 26):
    - `ClinicalDeleteRestoreDbTest`: 81/81 passed (covering medication, lab, and ultrasound across all 14 integration test scenarios).
    - `DeleteUndoUiTest`: 25/25 passed (including 4 new tests for ultrasound delete, cancel, undo restore, and 8s countdown).
    - Visual QA at 360 dp density: captured screenshots showing `DeleteIconButton` on ultrasound card (`ultrasound-tab-with-delete.png`), confirmation dialog (`ultrasound-delete-confirm-dialog.png`), undo bar (`ultrasound-undo-bar.png`), and restored state (`ultrasound-restored.png`).
  - Frontier review: Claude Sonnet independent review returned `PASS`.
- Physical phone verification: completed in deep testing session below.
- Next: Next deletable adapter (Previous Pregnancy per `ADDING_A_DELETABLE_KIND.md`) or Part B polish.

## 2026-11-03 - Physical Phone Deep Android Testing Audit (Honor DNY-NX9)
- Target device: Honor DNY-NX9 (wireless debugging `adb-AX3C025527011668-bPKEZm._adb-tls-connect._tcp`), physical resolution 1264x2736 @ density 560 (effective layout width: 361 dp ~ 360 dp standard).
- Scope: Deep end-to-end testing of newly implemented features on hardware:
  - QA build installation (`com.sherifradwan.anc.qa` installed safely side-by-side via `.agents/scripts/install_qa_build_on_device.sh`).
  - Ultrasound Delete / Confirm / Undo mechanism (`fcff3bc`).
  - Visit Entry 4-step wizard with value steppers, findings normal fill, and live review summary (`ce296c7`).
  - System gesture safety & 8s timer expiration.
- Evidence captured in `.agents/pipeline/design/device-test/`:
  - `01-workspace-overview.png`: patient directory to workspace navigation, tab row rendering.
  - `02-phone-ultrasound-tab.png`: ultrasound section showing scan cards, 48 dp touch target `DeleteIconButton` adjacent to pencil edit button.
  - `03-phone-ultrasound-confirm-dialog.png`: confirmation dialog with exact wording ("Delete this ultrasound scan?", "will be removed... You can undo for a few seconds afterwards").
  - `04-phone-ultrasound-undo-bar.png`: Undo countdown bar ("Deleted 2026-11-02", "Undo" button).
  - `05-phone-ultrasound-restored.png`: Undo interaction within timer window and status notice.
  - `06-phone-visit-step0.png`: Visit Wizard Step 0 (Date & BP) with quick date chips, dual BP input, presets ("Same as last (120/80)", "Normal (110/70)"), "+ Add other".
  - `07-phone-visit-step1.png`: Visit Wizard Step 1 (Weight & Fundal Height) with `ValueStepper` (+ / - 48 dp targets) and "Same as last (68.0 kg)".
  - `08-phone-visit-step2.png`: Visit Wizard Step 2 (Findings) with "Fill normal values" banner, oedema/fetal movement/urine choices.
  - `09-phone-visit-step3.png`: Visit Wizard Step 3 (Review & Save) with summary banner (" · BP 110/70 · Wt 68.0 kg · Oedema none · Movements good · Urine negative"), "Nothing flagged", clickable review rows for direct step jumping, and pinned "Save visit" button.
  - `10-phone-visit-saved.png`: Post-save state in workspace showing visit count incremented from 3 to 4, newly added visit card with exact recorded vitals, and "Visit saved to encrypted clinical records." notice.
- Hardware & Invariant Findings:
  - Timer expiration: When the 8s Undo window expires without user intervention, the Undo bar cleanly transitions to the permanent status banner with "Close" button, and the deletion commits to the encrypted Room DB.
  - Gesture navigation clearance: System navigation insets (Honor system gesture area frame `[0,2596][1264,2736]`) leave the action bars and Undo buttons completely unoccluded.
  - Logcat: 0 fatal exceptions, 0 runtime errors, 0 strict mode crashes.
- Local commits: Documentation updated, zero push rule strictly maintained.
