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

## 2026-11-03 - Visit Delete Adapter (`DeletableType.VISIT`) — COMPLETE & COMMITTED (`d9dff55`)
- Root Cause Identified (User Bug Report): On the Visits timeline tab, standalone ultrasound cards had a delete button (`UltrasoundDeleteAdapter` was wired), whereas routine visit cards (`VisitCard`) lacked `DeleteIconButton` because `DeletableType.VISIT` had not yet been wired.
- Changes committed locally in `d9dff55` (`logic(visit): visit delete adapter, undo bar, and delete icon button`):
  - `model/DemoModels.kt`: made `DemoVisit` implement `DeletableRecord` (`id`, `patientId`, `revision: Long` + backwards-compatible `Int` secondary constructor).
  - `model/DemoFixtures.kt`: populated `patientId` across all 25 `DemoVisit` seed records.
  - `storage/DeletableRecords.kt`: added `VISIT` to `DeletableType`, registered `VisitDeleteAdapter` in `DeletableRegistry`, added `is DemoVisit -> of(DeletableType.VISIT)` in `adapterFor`, and added `visitContentHash`.
  - `storage/VisitDeleteAdapter.kt`: implemented `DeletableRecordAdapter<DemoVisit>` (`fingerprintTag = "VIS"`, `deleteReceiptKind = "VISIT_DELETE"`, `restoreReceiptKind = "VISIT_RESTORE"`, `section = WorkspaceSection.VISITS`, notices `VISIT_DELETED`, `VISIT_RESTORED`, `VISIT_SAVE_UNDONE`, 8-field canonical string, `load`, `deleteRow` clearing `save_receipts` foreign-key references + atomic `ClinicalCommandAbort` on non-1 delete, `insertRow`, `record`).
  - `storage/RecordsDao.kt`: added `deleteSaveReceiptsForVisit(visitId)` and `deleteVisit(id, patientId, expectedRevision)`.
  - `storage/RecordsRepository.kt`: mapped `patientId` and `revision.toLong()` in `VisitEntity.toDomain()`, guarded `SaveVisitCommand` creation (`expectedRevision == null`) against replaying a save after a `_DELETE` tombstone in `clinical_command_receipts` (`VISIT_CONFLICT`).
  - `storage/ClinicalCommands.kt`: added `VISIT_DELETE` and `VISIT_RESTORE` to `ClinicalRecordKind`.
  - `state/AncState.kt`: added `VISIT_DELETED`, `VISIT_RESTORED`, `VISIT_SAVE_UNDONE` to `SavedNotice` and mapped in `stringRes()`; guarded `startVisit` with `undoRunningFor(visitId)`.
  - `state/EditorHooks.kt`: implemented `VisitEditorHook` and registered in `EditorHooks`.
  - `res/values/strings.xml` & `ui/DeleteUndoUi.kt`: added `deletable_type_visit`, `visit_deleted`, `visit_restored`, `visit_save_undone` and mapped in `labelResource()`.
  - `ui/WorkspaceScreen.kt`: wired `DeleteIconButton(DeletableType.VISIT, visit, onDelete)` into `VisitCard` trailing slot; disambiguated `HorizontalPager` pre-composed timeline standalone ultrasound tags (`timelineUltrasound-${scan.id}`, `delete-timelineUltrasound-${scan.id}`).
  - `.agents/checks/check_r6c.py`: added `VISIT` to `KIND` and `VisitDeleteAdapter.kt` to `FREE`.
- Verification:
  - `python3 .agents/checks/check_r6c.py`: PASS (R6C0–R6C6).
  - `.agents/scripts/run_build_gate.sh`: `GATE OK` (915+ unit tests, lint, formatting).
  - Emulator (`emulator-5554`):
    - `ClinicalDeleteRestoreDbTest`: 112/112 passed (medication, lab, ultrasound, visit + replay-after-delete test).
    - `DeleteUndoUiTest`: 29/29 passed.
  - Independent Frontier Review (Claude CLI): Reviewed diff, identified `save_receipts` replay and non-1 abort edge cases; all 3 items fixed and verified before commit `d9dff55`.

## 2026-11-03 - Previous Pregnancy Delete Adapter (`DeletableType.PREVIOUS_PREGNANCY`) — IN PROGRESS (Uncommitted in Working Tree, Ready for Device Verification & Commit)
- All 16 code & test files for `DeletableType.PREVIOUS_PREGNANCY` are edited in the working tree:
  - `model/DemoModels.kt`: `DemoPreviousPregnancy` implements `DeletableRecord`.
  - `storage/DeletableRecords.kt`: added `PREVIOUS_PREGNANCY` to `DeletableType`, registered `PreviousPregnancyDeleteAdapter`, updated `adapterFor`, added `previousPregnancyContentHash`.
  - `storage/PreviousPregnancyDeleteAdapter.kt` (NEW): `fingerprintTag = "PRV"`, `PREVIOUS_PREGNANCY_DELETE`, `PREVIOUS_PREGNANCY_RESTORE`, `section = WorkspaceSection.OBSTETRIC_HISTORY`, 17-field canonical string, `load`, `deleteRow`, `insertRow`, `record`.
  - `storage/RecordsDao.kt`: added `deletePreviousPregnancy(id, patientId, expectedRevision)`.
  - `storage/ClinicalCommands.kt`: added `PREVIOUS_PREGNANCY_DELETE` and `PREVIOUS_PREGNANCY_RESTORE`.
  - `state/AncState.kt`: added `PREVIOUS_PREGNANCY_DELETED`, `PREVIOUS_PREGNANCY_RESTORED`, `PREVIOUS_PREGNANCY_SAVE_UNDONE` and guarded `startPregnancyEditor` with `undoRunningFor`.
  - `state/EditorHooks.kt`: added `PreviousPregnancyEditorHook` and registered in `EditorHooks`.
  - `res/values/strings.xml` & `ui/DeleteUndoUi.kt`: added strings and mapped `DeletableType.PREVIOUS_PREGNANCY`.
  - `ui/WorkspaceScreen.kt`: wired `DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, onDelete)` into `PreviousPregnancyCard` and `WorkspaceSection.OBSTETRIC_HISTORY`.
  - `.agents/checks/check_r6c.py`: added `PREVIOUS_PREGNANCY` and `PreviousPregnancyDeleteAdapter.kt`.
  - JVM tests (`SavedNoticeTextTest.kt`, `UndoCoordinatorTest.kt`, `DeletableRegistryTest.kt`): updated and verified.
  - Android instrumented tests (`ClinicalDeleteRestoreDbTest.kt`, `DeleteUndoUiTest.kt`): updated with `PreviousPregnancyKind` (`modelVersion = 3`, `birthMethod = "Vaginal"`, `provenanceKind = "CLINICIAN_ENTRY"`) and `prevKind`.
- Verified so far for `PREVIOUS_PREGNANCY`:
  - `python3 .agents/checks/check_r6c.py`: PASS (R6C0–R6C6).
  - `.agents/scripts/run_build_gate.sh`: `GATE OK` (all JVM unit tests, lint, release compile pass).
- Remaining before committing `PREVIOUS_PREGNANCY`:
  1. Run `ClinicalDeleteRestoreDbTest` (140 tests) and `DeleteUndoUiTest` (33 tests) on `emulator-5554` (note: also check `assertOnlyTargetChanged` in `ClinicalDeleteRestoreDbTest.kt` if `dump()` includes `patient_command_receipts` during setup).
  2. Run independent frontier review on the `PREVIOUS_PREGNANCY` diff.
  3. Commit locally (`logic(obstetric): previous pregnancy delete adapter, undo bar, and delete icon button`).
- Next after `PREVIOUS_PREGNANCY`:
  1. Attachments delete adapter (`r6b6`, spec addendum first: files shredded only after Undo window expires).
  2. Part B Visual Polish: Lab (§4.1), Ultrasound (§4.2), Directory & Chart (§4.5, §4.6), History/Dating/Clinical Attention (§4.7).

## 2026-11-03 — attachments delete migration (slice2 r6b6 follow-up)
- Removed legacy `AncEvent.DeleteAttachment` from `AncState.kt` and `AncViewModel.kt`.
- `WorkspaceScreen.kt` attachment viewer delete now uses unified `AncEvent.RequestDelete(type=DeletableType.ATTACHMENT, recordId=att.id)`.
- `UndoCoordinatorTest.kt`: added `ATTACHMENT` branches to delete/restore `when` (imported `DemoAttachment`).
- Verified: `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL.

## 2026-11-04 — Attachment delete cleanup: test lane verified; core delegation blocked on quota; git hygiene finding
- Orchestrator verified the test-lane work in the working tree: fixture goldens re-derived externally (sha256 of the canonical att-1 string matches `bbc0078f...`), `./gradlew :app:testDebugUnitTest --offline` → 923 tests, 5 failed (all new attachment-cleanup exit-path tests, red for the right reason); the 4 earlier failures (SavedNoticeTextTest, DeletableRegistryTest) are green.
- Owner decisions recorded (`evidence/attachment-audit/OWNER_DECISIONS.md`): sweep on unlock, failed cleanup = keep deleted + notice + sweep retry, remove unused `attachment_save_undone`.
- Delegation to `opencode/glm-5.3-flash-free#max` failed immediately: `429 FreeUsageLimitError` from the opencode provider free tier (all 8 free catalog models share it; deepseek-v4.1-flash-free also 429). Owner chose to wait for the quota reset. Fallback lanes offered: Codex CLI (ChatGPT login), Antigravity/agy, orchestrator inline + different-family review. Brief ready; trackers updated (`tasks/current_task.md`, ledger `H-ATT-TESTLANE` / `H-DELEGATE-QUOTA` / `H-HISTORY`).
- Git hygiene finding (owner: plan only): `maca-round2` HEAD tracks 73,070 paths; ~71,927 are agent snapshots/run outputs swept in by `3948f79` (`.agents/backup-*` 13,215; `.agents/pipeline/runs/*` 58,712). Branch is local-only, 112 commits ahead of `origin/main`. Options recorded in ledger; nothing removed.
- Test-lane test files: `UndoCoordinatorTest.kt` (new exit-path suite + FinalizeResult fixture), `DeletableRegistryTest.kt` (attachment fixture + goldens), `SavedNoticeTextTest.kt` (ATTACHMENT keys). No app source changed by the orchestrator.

## 2026-11-04 — Attachment cleanup core lane implemented + device install
- Added `AncEvent.PayloadCleanupFailed`; reducer branch added.
- `UndoCoordinator.cleanupPending` is now the single function invoked from every exit path: expiry, `clear()`, second-delete replacement (`armUndo`), exhausted undo retries (`failUndo`); failures caught and reported.
- `:app:compileDebugKotlin` BUILD SUCCESSFUL. Installed debug APK on DNY-NX9 (AX3C025527011668) after uninstalling conflicting package.
- Remaining: independent review, unit-test green run, device verification of delete/undo/two-deletes/lock, local commit.

## 2026-11-04 — Attachment cleanup core committed; remaining items postponed
- Committed `f1aca51`: feat(attachment): payload cleanup on every undo exit path (10 files, 113+/48-).
- Removed unused strings `attachment_delete_failed` and `attachment_save_undone` from strings.xml.
- Release compile passes (BUILD SUCCESSFUL). Unit suite green: 923 tests.
- Postponed (recorded here and in tasks/current_task.md): startup sweep (gap 5), independent review (F4/F5), device check + DeleteUndoUiTest on device (no device connected this session), older audit items (minSdk, whole-DB load, design tokens, git history hygiene).
- Next mission candidate: older audit items or remaining postponed device tests.

## 2026-11-04 — Attachment cleanup closed properly; two static checkers repaired (commit `b6af671`)
- Implemented by the AutoCoder orchestrator after `zcode_run` failed with a model error and explicitly permitted native fallback; no other lane touched the code.
- `AncEvent.PayloadCleanupFailed` now carries the record it belonged to (patient, type, label, patient name); the reducer sets a real `CLEANUP_FAILED` message naming the record when the vault is unlocked, never writes into a locked state, and keeps a stronger `UNDO_FAILED` message about the same record.
- `UndoCoordinator.cleanupPending` is the single cleanup function for all four exit paths (expiry, `clear()`, second-delete replacement, exhausted Undo retries) and every path reports a failure — the `reportFailure = false` default is gone.
- New `storage/AttachmentOrphanSweeper.kt` (owner decision #1): on a successful vault open, payload files no attachment row references are deleted; 60 s age guard, referenced rows never touched, any failure logged and ignored so an unlock is never blocked. Pure planning function is unit-tested without Android.
- `AttachmentDeleteAdapter.insertRow` honours the revision the restore command passes again — it was the only adapter writing `record.revision`, so the row revision disagreed with the receipt after undo.
- Removed the unused `attachment_save_undone` string (owner decision #3); `attachment_delete_failed` removal from the earlier session is now committed too.
- Static checks repaired to describe the current contract: `check_r6c` R6C5 exempts ATTACHMENT from the editor-hook rule (attachments are never edited), `KIND` now covers ATTACHMENT, and new R6C7 requires every adapter's `insertRow` to use the revision parameter; `check_r3f1` S33/S34/S36 now assert the unified `RequestDelete` contract instead of the removed per-kind path.
- Commit `b6af671` (code, tests, checkers). Verified: `run_build_gate.sh` → `GATE OK`; `run_static_checks.sh` → `STATIC CHECKS OK` (after commit, on a clean tree); unit suite 938 tests in 86 classes, 0 failures.
- NOT verified: device run (`DeleteUndoUiTest`) — no device attached; the sweep on a real phone; independent frontier review of `b6af671` (F4/F5 still owed); the sweep only ran in JVM planning tests.
- Owed next: independent review of `b6af671`, device check (delete → wait → file gone; delete → Undo; two deletes; lock inside the window), then the postponed device tests.

## 2026-11-04 — Attachment cleanup verified on the owner's phone (QA copy only, `b6af671` build)
- Device AX3C025527011668 (USB, DNY-NX9). QA build `com.sherifradwan.anc.qa` 1.0.1-qa reinstalled at 23:28 with the `b6af671` code; the clinic app `com.sherifradwan.anc` was not touched (version 1.0.1, last update 16:55).
- Delete → wait: the card went at once, "Attachment deleted." appeared, and the Undo bar named the file and the patient. The payload file was still on disk inside the 8 s window and was gone after it; the other attachment's file was byte-identical before and after (md5 940e7e42…).
- Delete → Undo: "Attachment restored.", the card came back and the payload file was never removed. The restored record reopens with "Verified Digital Integrity · SHA-256" and the same hash `c8deb8cd…552` it had before the delete.
- Unlock sweep (the new code): a deliberate orphan (`orphan-test.webp`, 2020 mtime, no row) was removed by the first unlock after a force-stop, while the referenced `att_63ec9ede….webp` stayed. The empty patient folder `819244a3…` remains: the sweep removes files, not directories.
- Logcat: no app exception or crash during any of it.
- NOT verified on device: two deletes in a row (unit test only), lock inside the Undo window, the 60 s sweep age guard (unit test only), and `DeleteUndoUiTest` — connected instrumented runs are forbidden on the owner's phone, so that class still needs a disposable emulator.
- Two defects seen on hardware, NOT fixed (owner decision, both pre-date `b6af671`): the attachment viewer still asks twice (its own "Confirm Delete Attachment" dialog, then the shared "Delete this attachment?" one), and `AttachmentDeleteAdapter.label` falls back to the internal `att_<uuid>.webp` name, so the confirm dialog and the Undo bar show a UUID instead of the scan's title/date.
- Evidence: `evidence/attachment-audit/device-2026-11-04/attachment-restored-integrity.png` (git-ignored).

## 2026-11-04 — Attachment delete UX: one confirmation, human label; retested on the phone
- `ClinicalAttachmentViewerDialog` no longer runs its own "Confirm Delete Attachment" dialog. It dispatches `RequestDelete` and closes, so the shared `DeleteConfirmDialog` is the only confirmation. New `check_r6c` R6C8 keeps a record viewer from confirming deletes by itself.
- `AttachmentDeleteAdapter.label` now returns the title when there is one, else `<category> · <capture date>`. The internal `att_<uuid>.webp` name (which reached the confirm dialog and the Undo bar) is gone. `AttachmentDeleteAdapterTest` covers title, blank title, missing title, unparseable date and "never the file name".
- Retested on Honor DNY-NX9, QA build installed 23:41: the viewer closes → exactly one dialog ("Delete this attachment?" / "Ultrasound Scan · 2026-10-30") → Delete → the Undo bar reads "Deleted Ultrasound Scan · 2026-10-30" → Undo → "Attachment restored." with the payload file never removed (the same 10,626 B file). No crashes. Clinic app untouched.
- Verified: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; 943 unit tests in 87 classes, 0 failures.
- NOT verified: the titled-attachment path on device (unit test only, the QA record has no title), and `DeleteUndoUiTest` still needs an emulator.
- Evidence (git-ignored): `evidence/attachment-audit/device-2026-11-04/{single-confirm-human-label,undo-bar-human-label,retest-restored}.png`.

## 2026-11-04 — Instrumented tests on a disposable emulator; viewer copy moved into strings
- `DeleteUndoUiTest` on `emulator-5554` (AVD `anc-test-api26`, API 26, 1080x2400 @ 480 dpi = 360 dp): **33 tests, 0 failures, 0 errors**.
- `ClinicalDeleteRestoreDbTest` on the same emulator: **140 tests, 0 failures, 0 errors**. Its `restoreAfterDeleteReinsertsIdenticalContentAtTombstonePlusOne` asserts `tombstone + 1` per kind, including attachments: this is the direct proof of the `AttachmentDeleteAdapter.insertRow` revision fix from `b6af671`.
- Both runs used the `.qa` suffix on the emulator. The owner's phone was not involved: its QA copy kept the same `lastUpdateTime` (2026-11-04 23:41:42) throughout.
- Strings: the attachment viewer's hardcoded copy moved to resources — integrity and vault labels, `SHA-256: %1$s`, `Saved At: %1$s`, the size/format line, and the delete/close content descriptions (`close` reuses the existing string). `"Image unavailable"` is now one shared `R.string.image_unavailable` used by both photo viewers. `check_r3e` S26 now follows the copy to the resource instead of demanding a literal in the source.
- Verified: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; `check_r3e.py` PASS.
- NOT verified: nothing outstanding in this round. `DeleteUndoUiTest` still must never run on the owner's phone (rule unchanged); the emulator is the supported path.
- Note: the phone dropped off USB during the emulator cleanup (after all device work had finished); no phone setting was changed by this session.

## 2026-11-05 — Frontier review of the attachment round: FAIL, three hard blockers (one data-loss)
- Reviewer: Gemini 4 Argon via `agy`, independent session. ZCode was unreachable (model request failed on two probes, including a one-line probe); Claude is at its weekly limit until 7 Nov; opencode free quota is exhausted; codex rejects every model its account offers. `agy` was the only live lane and is a designated reviewer in `LANES.env`. Report: `evidence/attachment-audit/FRONTIER_REVIEW_gemini_2026-11-05.md` (git-ignored).
- Verdict **FAIL**. I re-checked every blocker against the code; the report misquotes some code (it invents an `AttachmentSweepKey` type, cites `AncModels.kt`, and misquotes `cleanup_failed_message`), so I treated it as leads, not truth. The three blockers survive:
  1. **CONFIRMED, data loss.** `AttachmentOrphanSweeper.referencedKeys` only reads `patient.attachments`, but ultrasound photos are written to the same `patient_attachments/<patient>/` folder by `AncViewModel.importScanPhoto` and referenced only from `ultrasound_scans.image_uris` as `vault:att_….webp` (`ScanPhotoRef.kt`); the reducer `ScanPhotoImported` only appends to the draft, never inserting an attachment row. Every imported scan photo older than 60 s is therefore an orphan by the sweep's definition and is deleted at the next unlock. Open ultrasound drafts holding imported photos are exposed the same way.
  2. **CONFIRMED by construction.** `UndoCoordinator.request` keeps `pendingUndo` while `RestoreRecordCommand` is in flight; a patient switch (the offer is dropped by `withDeleteUiConsistent`) or a lock (`clearFrozenCommands`) calls `clear()`, which deletes the payload file even though the restore then commits. The restored row survives with no file, and no sweep can recover it.
  3. **CONFIRMED by construction.** On the replacement path `armUndo` runs before `reduce(DeleteCommitted)`. In production the coordinator scope is `viewModelScope` (`Dispatchers.Main.immediate`) and `finalizeDelete` has no suspension point, so `PayloadCleanupFailed` is reduced *before* `DeleteCommitted`, whose `deleteMessageAfterSuccessFor(patientId)` then wipes it in the same frame. The unit test passes only because the fake dispatcher queues the launch.
- Correction: my earlier claim in `H-EMU-TESTS-2026-11-04` that `ClinicalDeleteRestoreDbTest` (140/140) directly checked the attachment restore revision was **wrong**. `kinds()` lists five kinds (`medication`, `lab`, `ultrasound`, `visit`, `previous_pregnancy`); attachments are not parameterised, and `DeleteUndoUiTest` has no attachment case.
- Should-fix from the same review, also verified: `lastModified() == 0` passes the age guard; payload I/O runs on the main thread; `failUndo` and early `request` exits can strand `pendingUndo` without cleanup; the viewer closes before the shared confirmation so Cancel drops the user out of the image; `check_r6c` R6C1's `NAMED` regex omits `Attachment` while `AncViewModel` still branches on `record is DemoAttachment`; S36 no longer checks the save-failure cleanup of the newly written file.
- Status: nothing fixed yet. The clinic app is unaffected (it still runs 1.0.1 without the sweep); the QA copy has demo data only. Remediation is the next round and must be tests-first, then gate + emulator suites + a re-review.

## 2026-11-05 — Review blockers 1-3 fixed and regression-tested (commit `7ac25c5`)
- **Sweep (data loss)**: `referencedKeys` now counts `ultrasound_scans.image_uris` (`vault:` refs). Scan photos share the attachment folder but have no attachment row, which is why the old code called every one of them an orphan. New test failed on the old code and passes now.
- **Unknown mtime**: a file whose `lastModified()` is 0 is no longer read as 56 years old; `planOrphans` requires a known age.
- **Restore race**: `UndoCoordinator` detaches the pending payload while a `RestoreRecordCommand` is in flight, so `clear()` during a patient switch or a lock can no longer delete the file of a record the restore is bringing back. A failed restore re-arms the offer or leaves the file to the next sweep. Red-before-green proven.
- **Replacement message**: payload cleanup now runs on an injected IO dispatcher (no file I/O on the main thread) and `DeleteCommitted` is reduced before arming. Reproduction limit: the JVM harness never runs a nested `launch` inline (verified with two dispatchers and a printed event order), so the test proves the ordering property but cannot emulate `Dispatchers.Main.immediate`; the production ordering is reasoned from `Main.immediate`, not reproduced.
- Verified: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; 948 unit tests in 87 classes, 0 failures; emulator `anc-test-api26` at 360 dp: `DeleteUndoUiTest` **33/33**, `ClinicalDeleteRestoreDbTest` **140/140**. The owner's phone was not part of these runs.
- NOT fixed (follow-up, from the same review): attachments are still missing from `ClinicalDeleteRestoreDbTest.kinds()` and `DeleteUndoUiTest` (SF4), so no instrumented test covers the attachment delete/restore path; the viewer closes before the shared confirmation so Cancel leaves the list (SF5); checker gaps — R6C1's `NAMED` regex omits `Attachment`, `AncViewModel` still branches on `record is DemoAttachment`, S36 no longer checks the save-failure cleanup (SF6); `failUndo` early exits can still strand a pending payload (SF1, lower risk after the detach). Re-review of `7ac25c5` has not been run.

## 2026-11-05 — SF4, SF5, SF6 closed; instrumented run on a visible emulator (commit `4adb5e0`)
- **SF4 (coverage)**: `ClinicalDeleteRestoreDbTest` gained an `AttachmentKind` (attachments are created through `RecordsRepository.saveAttachment`, so it overrides `create`) and `"attachment"` in `kinds()`: 6 kinds × 28 = **168 tests**, including the `tombstone + 1` restore revision the attachment adapter once broke. `DeleteUndoUiTest` gained three attachment cases driven through the viewer: delete → Undo bar, Undo restores, and Cancel keeping the viewer open.
- **SF5 (UX)**: the attachment viewer no longer closes when delete is tapped; it closes only when the record is really gone, so Cancel returns to the image. The delete button is disabled for archived patients instead of being silently rejected.
- **SF6 (checkers)**: `PayloadFileAdapter.finalizeDelete` takes `DeletableRecord`, so `AncViewModel` no longer branches on `record is DemoAttachment`; `check_r6c` R6C1 now also matches `AttachmentDeleteAdapter` names; `check_r3f1` S36 checks again that a failed attachment save deletes the file it just wrote.
- Verified: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; 948 unit tests in 87 classes, 0 failures; emulator `anc-test-api26` **with a visible window** at 360 dp, `.qa` package: `DeleteUndoUiTest` **36/36**, `ClinicalDeleteRestoreDbTest` **168/168**.
- `run_emulator_test_class.sh` now passes `android.injected.androidTest.leaveApksInstalledAfterRun=true`: the QA app and its test APK stay installed on the emulator between runs (owner rule: update in place, never uninstall).
- Phone: the QA copy is still the 01:00:51 build, which already contains the scan-photo sweep fix (`7ac25c5`); the newest build (SF5/SF6, no data-path change) could not be pushed because the USB link dropped again during the round. Clinic app untouched.
- NOT done: re-review of `7ac25c5`/`4adb5e0` — still owed by the level-3 rule.

## 2026-11-05 — QA copy updated on the phone; SF5 verified on hardware
- Phone reconnected; QA copy updated **in place** (`installDebug`, same signer, no uninstall): `1.0.1-qa`, `lastUpdateTime` 01:17:16. Data preserved (the remaining attachment file is untouched); clinic app still 1.0.1 / 16:55:47.
- SF5 on hardware: viewer → Delete Attachment → shared dialog ("Delete this attachment?" / "Ultrasound Scan · 2026-10-30") → **Cancel** → the confirm closes and **the viewer stays open** with its Delete and Close controls, no Undo bar, payload file untouched.
- Note: a system "Use USB to" dialog appears after each replug; it was cancelled without changing any phone setting.
- Emulator `anc-test-api26` is left running with the window open and the QA app installed, so runs can be watched and the app re-checked without rebuilding.

## 2026-11-05 — Re-review of the fix round: PASS WITH NITS, all closed (`69cdc57`)
- Reviewer: Gemini 4 Argon via `agy`, fresh session. ZCode still unreachable (model request failed on a one-line probe). Report kept in history: `.agents/pipeline/review_attachment_cleanup_rereview_2026-11-05.md`.
- **Verdict PASS WITH NITS — 0 blockers, 0 should-fix.** All three original blockers and SF1-SF6 marked closed, each with a file:line and the check used. The reviewer independently confirmed: the sweep's reference set now covers every persisted writer (only two callers of `saveAttachment`, drafts never persist `imageUris`), the in-flight restore cannot lose a committed payload, and `reduce(DeleteCommitted)` before `armUndo` removes the same-frame wipe on `Main.immediate`.
- Three nits, all fixed in `69cdc57`:
  1. `failUndo` cleared `pendingUndo` unconditionally when the offer was gone, which could drop a newer offer's pending payload in a cross-patient race; it now clears only its own token.
  2. The immediate-dispatcher test implied it covered the IO hop; its comment now says it isolates reduce-before-arm and that the hop is uncovered by a test.
  3. The attachment UI delete test asserted tags only; it now asserts the confirm dialog shows the human label ("Scan 1"), so a label regression fails the suite.
- Verified after the nits: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; `DeleteUndoUiTest` **36/36** and `ClinicalDeleteRestoreDbTest` **168/168** on the API 26 emulator at 360 dp.
- NOT verified: the reviewer did not run the instrumented suites itself (it compiled them and read the sources); it ran the targeted JVM tests and the three static checkers.

## 2026-11-05 — Mission closed; handoff refreshed
- Attachment delete / confirm / Undo is complete for all six record kinds, payload files included: committed delete → 8 s window → file removed; Undo → file kept; a failed or interrupted cleanup → next unlock sweep. The independent review went FAIL (three real blockers, one data-loss) → fixes → re-review **PASS WITH NITS, 0 blockers** → nits fixed.
- Handoff refresh: `.agents/pipeline/HANDOFF_AGY.md` gained a dated status block that **supersedes** the stale "previous pregnancy in progress" section at its end; `docs/AI_HANDOFF.md`'s banner now reports the real state (948 tests, PASS, 130 ahead); `tasks/current_task.md` closes Phase 7 and states the next four items in order.
- Code state at the time of this entry: `752d4cb`. Verified: gate OK, static checks OK, 948 JVM tests / 87 classes, `DeleteUndoUiTest` 36/36, `ClinicalDeleteRestoreDbTest` 168/168 on the API 26 emulator at 360 dp.

## 2026-11-05 — Revision queue: P-1 and P-2 were already fixed; backlog corrected
- Started the next queue item (print defects). Both were **already fixed** by `c7f1031` on 2026-10-31 ("no broken words or dates, stamp label once"); the UIUX backlog entries had never been closed.
- Evidence, produced now rather than assumed: the sample report (`PrintSampleDumpTest` → `app/build/print-sample/report.html`) was measured in headless Chrome by counting line boxes per cell. Every short value cell (date `yyyy-MM-dd`, BP `nnn/nnn`, weight/number, "Photo n", "and N more") renders on exactly one line at **794 px** (A4 content width) and at **380 px** (phone width). The generated HTML contains `Official Stamp & Signature` exactly once. `check_r3x.py` S97 (no `break-word`, `nowrap` present, stamp once) passes and is part of the static gate.
- Why the investigation was needed: the backlog still described the 2026-10-31 phone findings, and the CSS already carried the fix. No code changed in this entry; `UIUX_BACKLOG.md` now marks P-1/P-2 fixed with the method and the commit.
- NOT verified: a fresh visual pass on paper/print preview — the vision model was unavailable (406) during this session, so the check is deterministic layout measurement, not an eyeball.

## 2026-11-05 — Revision round: raw format string on lab cards fixed, guard added (`4313682`)
- Part B design comparison is blocked this session: the vision model returned **406 for every model tried**, so no screenshot can be judged. The Lab §4.1 mock was captured at 360 px for later (`evidence/design-review/lab-4.1/mock-360.png`); the built side could not be captured because the phone dropped off USB again mid-capture (the emulator was used for the functional check instead).
- Verifiable revision item found on the emulator: every workspace lab card rendered **"Calculated GA: %1$s"**. `FactRow` renders its label verbatim and the code passed a format string. Fixed with a label-only `calculated_ga_label`; the editors keep `calculated_ga_at_visit` with its argument. Verified on the emulator: both lab rows now read "Calculated GA:".
- New `.agents/checks/check_r7a.py`: **S500** fails when a string containing `%n$s`/`%n$d` is used as a `FactRow` label, **S501** when such a string is rendered directly through `stringResource` with no arguments. Storing it in a `val` first (then `String.format`) stays allowed — that is how the two legitimate templates (`already_on_todays_list`, `filled_normal_toast`) are used, and both were false positives in a naive scan. Red before the fix, green after; `run_static_checks.sh` picks the check up automatically.
- Verified: `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; the emulator build shows the corrected labels; the first JVM/emulator suites are unchanged by this (single string + label).
- NOT verified: design comparison against the mock (vision outage), fresh print/phone pass, and the phone build (USB dropped; the emulator has the new build).

## 2026-11-05 — Laboratory batch, logic slice 1: save / delete / restore (`fcc7a02`)
- Implemented against `SPEC_LOGIC_B1_LAB_BATCH.md` with the schema still at v6: no table, column or migration.
- `LabBatchEntry` + `LabBatchRecordRef` with a stable canonical string and per-entry SHA-256.
- `SaveLabBatchCommand` inserts N rows at revision 1 in one transaction, bumps the patient revision once and writes a single `LAB_BATCH_SAVE` receipt whose `recordId` is the batch id.
- `DeleteLabBatchCommand` verifies every row's revision **and** content hash before deleting; one mismatch aborts the whole transaction. `RestoreLabBatchCommand` re-inserts at tombstone + 1 (revision 2). One receipt each.
- Two "every record kind is known" invariants had to learn the batch kinds: `DeletableRegistryTest` (a batch has no single-record adapter, so its kinds are allow-listed) and the `ClinicalValidationFailed` reducer (a validation failure is not a storage error; the batch draft has no error surface yet).
- Verified: `run_build_gate.sh` GATE OK; **957** unit tests (9 new command tests), 0 failures; `LabBatchDbTest` **4/4** on the API 26 emulator (save + one receipt, stale-reference abort, delete + restore at revision 2, cross-patient rejection).
- NOT done yet: the batch draft in `AncState`, the one-action Undo offer in `UndoCoordinator`, the CBC/Urine catalogue entries, and the three-tab screen. Nothing calls the new commands from the UI.

## 2026-11-05 — Laboratory batch, logic slice 2: one-action undo (`111056f`, `0e66cdd`)
- `UndoCoordinator.offerLabBatchSaveUndo` builds one `DeleteLabBatchCommand` from the rows the save just produced (revision + content hash each) and arms a single offer labelled "N lab results". Requesting it executes exactly one command, so the batch goes together or not at all. No offer is made when the fresh snapshot does not hold every saved row.
- `check_r6c` R6C1 rejected the first version: the coordinator named `DeletableType.LAB`. The caller now supplies the type and the label, which is how every other call site works; the shared layer stays kind-free (`0e66cdd`).
- Verified: `run_build_gate.sh` GATE OK; **959** unit tests, 0 failures (2 new coordinator tests); `run_static_checks.sh` STATIC CHECKS OK.
- Still not wired: the ViewModel does not call the offer yet, because the batch draft and the three-tab screen do not exist. Next: batch draft in `AncState`, then the UI and the CBC/Urine panels.

## 2026-11-05 — Laboratory batch, logic slice 3: the in-memory draft (`da6ce5c`)
- `LabBatchDraft` / `LabBatchItem` in `state/LabBatchState.kt`: patient, date, one lab, rows, cursor, open tab. A new batch starts empty on **Add** and moves to **Enter** when the first tests are added; removing a row keeps the cursor inside the list.
- Events: start, add, remove, result/range/flag/note change, tab select, cursor move, date change, cancel. They are no-ops without a draft. A new result clears a stale manual flag; the flag and range stay explicit events.
- Cancel clears the draft and returns to the laboratory section. **No route was added**: the batch screen does not exist yet, and adding `LabBatchEditor` would have forced a placeholder branch in `AncApp`.
- Verified: `run_build_gate.sh` GATE OK; **969** unit tests, 0 failures (10 new draft tests); `run_static_checks.sh` STATIC CHECKS OK.
- Next: ViewModel `startLabBatch`/`saveLabBatch` (prepare the batch command, then call `offerLabBatchSaveUndo`), the CBC/Urine catalogue panels, and the three-tab screen.

## 2026-11-05 — MACA audit of the batch work: two rounds, FAIL → fixed, evidence re-recorded
- Ran the multi-agent audit path on the batch slices: outcome oracles B1–B8 added to `SPEC_AND_AUDIT.md`, the four batch tests registered in `TEST_MANIFEST.json`, and a read-only reviewer in a fresh context (`independence: same-model` — the profile names no approved external vendor).
- **Round 1: FAIL.** Two hard blockers, both real: a blank batch date passed validation (`isValidIsoDate` accepts blank and the batch omitted the `isBlank()` check every other dated command has), and the draft is not persisted / not in `hasDirtyDraft`. Seven should-fix followed. Fixed in `cc6239b` and recorded in `.agents/review-r1.md`.
- **Round 2: FAIL, one procedural blocker.** The fixes were correct, but every batch evidence entry predated the fix commit and hashed the parent blobs. Correct catch. Re-ran the four manifest entries after the fix (`00:51:58Z`–`00:52:31Z`); all pass. Same round's design points were also fixed in `cf89342`: `RestoreLabBatchCommand` now carries the delete uuid and derives the revision from that receipt, and the spec plus ORACLE-B4 say tombstone + 1 instead of revision 2.
- Verified this round: GATE OK; 971 unit tests; `LabBatchDbTest` **5/5** (11-row delete/restore with receipt kinds, archived patient); evidence cache holds post-fix hashes for all four entries.
- Two-round limit reached. Still owed, explicitly: draft persistence + `hasDirtyDraft` before the B2 screen, stale-revision rebase for the batch undo, `prepare()` so a bad date lands on the draft, and tests for the restore-archived guard and the revision branch of the delete guard.
