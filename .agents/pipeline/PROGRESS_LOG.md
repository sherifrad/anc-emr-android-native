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
