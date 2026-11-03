# HANDOFF FOR ANTIGRAVITY (agy): graphic and logic roadmap
Written 2026-11-01 by the Claude orchestrator. Read this whole file, then `PROGRESS_LOG.md`, then work. Owner: Dr. Sherif Radwan (consultant obstetrician). Repo: `/Users/sherifgamalradwan/anc-emr-android-native`, branch `maca-round2`. Pronouns for people: they/them unless stated. English-only UI.

## 0. Hard rules (never break)
1. NEVER `git push`. Commit locally only, one verified round per commit, ending with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` only if Claude wrote it; otherwise your own attribution.
2. NEVER touch the clinic app `com.sherifradwan.anc` on the phone (real patient data), nor `ancemr`, `ancemr.debug`, `ancnative.dev`. Test only the QA debug copy `com.sherifradwan.anc.qa` (PIN 121212, demo data) on the emulator `emulator-5554` (AVD `anc-test-api36`) or on the phone's QA copy. Never uninstall anything. Never change phone settings.
3. No patient data to any vendor. Demo data only.
4. A round is LOGIC or DESIGN, never both (`.agents/pipeline/PLAN_DESIGN_VS_LOGIC.md`). One function per rule: call the existing helper, never copy it. Shared UI lives in `ui/DesignSystem.kt`.
5. Every edit under a level-3 path (`app/src`, `evidence/`, `keystore/`, `app/schemas`) needs a review by a different model than the builder and an independent test author. Never certify beyond evidence: say what ran, what did not.
6. Owner decisions are in `.agents/pipeline/OWNER_DECISIONS.md` (D1-D23). Do not reverse them. Ask the owner (plain words, no round codes) when a choice is theirs.
7. Do not spend scarce free-model quota on benchmarks.

## 1. Where the work stands (2026-11-01)
- DONE, committed 80e6416: shared `ui/DesignSystem.kt` (`SectionCard`, `SummaryRow`) and the ultrasound screen with guided findings (one open at a time), "More" for photos and visit link. 8 emulator tests pass.
- DONE r5b lab screen, commit 6ef0f9c: gate green; 7 new lab tests, 9 workflow tests and 8 ultrasound tests pass on emulator API 36. NOT verified: a screenshot of the lab screen, 160 percent text, a frontier-tier review (two model reviews only read code), the full suite.
- NEXT: custom-choices logic round (section 2.3), then r5c medication screen (D22: one-tap add, list with Stop/Edit, four statuses Active/Completed/Discontinued/Patient non-compliant, no Prophylaxis button; wire the medication settings), then the sections in section 3 below.
- Owed: frontier-tier plus independent test reviews for the `app/src` commits (ultrasound 8c06c76 and 80e6416, lab settings, lab entry, r4e, r4f, r4g); full 56-test emulator suite on r5 rounds; "Clear findings" UI test.
- UPDATE 2026-11-02 (owner priority "A then B": A = logic first, B = visual redo to match the approved mock, checked by screenshots): DONE and committed locally: medication entry (f4b264a), collapsible medication list (a8e6235), delete/restore/Undo logic for medications and lab results (19c36fc, review fixes e8285ff, 46-case real-database test), and the generic storage layer (9ceef0d, 54 database cases, isolation tests). DONE after that (all local, see section 8 status): shared Undo handler, shared delete UI, medication and lab wiring, three Opus review rounds fixed. Owner requirement: delete/confirm/Undo must be ONE global mechanism for every entered record kind (medications, lab, ultrasound, previous pregnancy, visit, later attachments), with hard boundaries: a delete or restore can only touch the exact record, section and patient that was confirmed, never another section or patient, never break database rules. Full plan: section 8.
- The installed clinic app on the phone is still the old 1.0.1 release from before this design work. A new release build is made only if the owner asks.

## 2. Design language (graphic roadmap) - APPROVED by the owner 2026-11-01
Approved clickable mock-ups: `.agents/pipeline/design/d4/index.html` (also published: https://claude.ai/artifact/LUtQqawvTycThDsPd5652E). d4/d1 = Laboratory, Ultrasound, Medication (option B). d4/d2 = Obstetric history, Previous pregnancies, Dating, Clinical attention entry, Delivery plan (the options that were titled Recommended). d4/d3 = Patient directory, Patient chart, Visit and vitals entry. Non-approved options were deleted; do not bring them back. The ChatGPT teal palette proposal was REJECTED: keep the app's existing navy/rose theme (tokens are in the d4/d1 CSS).

### 2.1 Principles
- One thing open at a time; answering closes the row and opens the next blank one. Closed rows show "Label  value  >" (`SummaryRow`, 48 dp or more); open rows show big choices (`BigChoiceButton`, 56 dp).
- Choice before typing; keyboard only for numbers and free text. "Fill normal" never fills a finding the doctor must decide (never placenta, blood pressure, weight).
- Abnormal values turn rose and show a flag; they never block the doctor.
- Pinned primary action; one FAB for "add". No screen longer than about 1.5 screens.
- No hand-written `Card(` in screens: use `SectionCard`, `SummaryRow`, `BigChoiceButton` from `ui/DesignSystem.kt`; theme colours only.

### 2.2 Small-screen and text-size rules (owner requirements, apply to every design round)
- Target device: Honor 400 class, about 360 dp wide. Test at 360 dp.
- Light theme is the DEFAULT; dark is a switch, not automatic.
- In-app text size control A- / A+ (in Settings and reachable from the app bar): steps 90, 100, 110, 120, 130 percent, default 100, HARD LIMIT at both ends (never unbounded). A+ is disabled at 130 percent or earlier when a short label would no longer fit.
- Short labels (pills, chips, buttons, tab names, row labels, titles up to about 30 characters) stay on ONE line: never cut, never ellipsised, never wrapped to two lines. Method: maxLines = 1 plus shrink-to-fit down to 80 percent of the label size (Compose autoSize or one helper written ONCE in `ui/DesignSystem.kt`); shorten the wording if it still does not fit. Long paragraphs may wrap and are marked deliberately.
- Reference implementation: `.agents/pipeline/design/d4/shared.js` (bounded steps, fit pass, limit message). In the app the setting is stored in preferences and applied by overriding the font scale once at the app root.
- Tests: an instrumented test per screen at 90, 100 and 130 percent asserting that short-label tags stay single-line (node height equals one line).

### 2.3 Add-your-own values (a logic round that must come BEFORE the screens use it)
Every list of predefined choices (lab tests and result presets, categories, ultrasound finding choices, medication dose/route/frequency/drug, previous-pregnancy outcome/mode/complications, clinical-attention phrases, visit entry choices such as oedema, movements, urine) ends with a "+ Add other" chip. Approved behaviour: an inline one-line input and an Add button; the value is used for THIS patient at once; it is saved to a shared per-list store and appears as a normal chip for every later patient; a toast "Saved for next time" with Undo (undo removes both the selection and the stored value). Values are labels only, never patient data; the doctor can rename or remove saved values in Settings. Dating methods (LMP, IVF, Scan, Clinical) are fixed and have no "+ Add other". For findings that feed the impression text or risk rules (for example presentation), a custom value is stored as free text and never triggers a rule. Storage: extend the existing codex/settings store (see `model/codex/ClinicalCodexModels.kt` and the lab and medication codex), include it in backup and restore, one pure function for validation (trim, length limit, case-insensitive duplicate check), unit tests first.

### 2.4 Clinical attention entry (owner request: the field was missing from the mock)
A labelled field "Clinical attention" (2 visible lines, 140-character counter), quick-phrase chips that append (Planned vaginal delivery, Previous cesarean, Anaemia on iron, PIH on labetalol, GDM on diet, Rh negative, Twin pregnancy) plus "+ Add other", and a live preview "Shows on the patient list as:" (2 lines, then "+N more"). Reachable from the patient chart header (Edit) and from the new-patient flow. The patient directory card shows this text instead of a pile of badges. See d4/d2 screen 4 and d4/d3 chart.

### 2.5 Order of design rounds (each: spec, static check, independent tests, pipeline, review, emulator, screenshot, local commit)
1. DONE ultrasound (80e6416). 2. DONE lab (6ef0f9c). 3. Medication (r5c; D22: one-tap add, list with Stop/Edit, four statuses, no Prophylaxis button; wire the medication settings). 4. Text-size control and the small-screen fit helper (design-system round, then applied to every screen). 5. Patient directory and patient chart (d4/d3). 6. Visit and vitals entry (d4/d3). 7. Obstetric history, previous pregnancies, dating, clinical attention entry (d4/d2; dating uses the existing due-date rule). 8. Delivery plan (blocked until the owner lists the fields, w04). 9. Settings screens for the saved lists.

## 3. Logic roadmap (separate rounds, spec first, tests first)
- L-1 Custom choices store and rules (section 2.3). Must precede every screen that shows "+ Add other".
- L-2 Text-size preference (bounded steps, persisted).
- L-3 Visit entry rules: "same as last" and "fill normal" as ONE pure function each in `state/`, unit-tested; out-of-range blood pressure (140/90 or higher) flag.
- L-4 Lab Review tab (batch state), lab Back button, "mark empty yes/no tests as normal".
- L-5 Alert notes "+N more" model (w03) and hysterotomy counts as cesarean (w12).
- L-6 Edit medications in settings (l03); medication status of four values wired to history.
- L-7 Delivery plan fields (w04, waiting for the owner).
- Rule: if a design item needs state that does not exist, write, test and commit the logic round first; then the design round calls it.

## 4. How to run a round (the MACA pipeline)
1. Write the spec section in `.agents/pipeline/SPEC_DESIGN_R5.md` (items, static checks S###, UI tests T###).
2. Register: add the round to `--round` choices, the `BRIEFS` tuple, an `ALLOW` block (file allow-lists per lane) and `CORE_GATE |= {"UNIT-all"}` in `.agents/pipeline/run_pipeline.py`; add a `STATIC-rXX` entry to `.agents/TEST_MANIFEST.json` by editing the TEXT (json.dump reformats the whole file).
3. Write `.agents/checks/check_rXX.py` and briefs under `.agents/pipeline/rXX/` (copy `r5b`).
4. A different agent writes the androidTest from the spec BEFORE the pipeline runs. Never run a helper that writes outside the lane allow-list while the pipeline runs: its scope guard deletes such files.
5. Run: `cd repo && set -a && source .agents/pipeline/LANES.env && set +a && python3 .agents/pipeline/run_pipeline.py --round rXX --ui-only > .agents/pipeline/rXX-run.log 2>&1`.
6. Read the reviews yourself (reviewer outputs can be empty). Verify: `./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin -q` (empty output = green); `for f in .agents/checks/check_r*.py; do python3 $f >/dev/null 2>&1 || echo FAIL $f; done`.
7. Emulator tests: `ANDROID_SERIAL=emulator-5554 ./gradlew --offline -I <qa.init.gradle> :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>`; `qa.init.gradle` adds applicationIdSuffix `.qa` and versionNameSuffix `-qa` to debug (recreate it if missing).
8. Take a screenshot for the owner (emulator), commit locally, append to `PROGRESS_LOG.md`, update the roadmap site.
Shell: zsh on macOS (`sed -i ''`, no `PIPESTATUS`, no foreground `sleep`). Compose test fact: text inside a tagged Card needs `hasText(..) and hasAnyAncestor(hasTestTag(..))` with `useUnmergedTree = true`.

## 5. Roadmap site and progress history
- Roadmap artifact (tracker page): https://claude.ai/artifact/PaLVQNoN83bLDb5HzZ2Nss . Data lives in its database: collections `tasks` and `phases` (writes need `if_version`; fields are top-level). Pending updates: w02 done with limits; a design task for the new-patient dating choice controls; ultrasound design round done; add the rounds in section 1.
- If you cannot reach the artifact, keep the truth in `PROGRESS_LOG.md` and say so; Claude will sync it on return.
- `PROGRESS_LOG.md` (same folder): append one dated entry after EVERY round or decision, until Claude returns after its quota reset. Entry format: date and time, what changed (plain words), commit hash, what was verified (command and result), what was NOT verified, what is owed next. Never delete earlier entries.

## 6. Open owner decisions (ask, do not guess)
Lab: on-screen keypad vs the phone's keyboard (default used: phone keyboard). Ultrasound: reference values for steppers (default: none, no sliders). Rename "Active Medications". Delivery plan field list. (Palette is CLOSED: keep the app's navy/rose theme.)

## 7. Known debts
Stray `.agents/backup-*` folders and `retry/` (leave alone). Unreviewed `devtest_helper.py`. `scratchpad/pub/index.html` line 14 says "Recommended: A for all three" (stale). OmniRoute review lane to retry when its gateway is healthy.

## 8. Global delete / confirm / Undo mechanism (owner request 2026-11-02; spec `SPEC_LOGIC_R6B.md`, decisions D620-D654)
Owner decisions: delete asks for confirmation, then removes the record at once and shows an Undo bar for about 8 seconds (afterwards the delete is final, same for lab results). "Save and next" shows an Undo bar on the next blank form that removes only that new medication. One mechanism for every section, not copies.
- Storage (DONE, 9ceef0d): `storage/DeletableRecords.kt` registry; one `DeleteRecordCommand`/`RestoreRecordCommand` keyed by type; one `deleteRecord`/`restoreRecord` in `RecordsRepository.kt`; `MedicationDeleteAdapter.kt`, `LabDeleteAdapter.kt`. A new kind = one adapter plus one registry line plus one `kinds()` entry in `ClinicalDeleteRestoreDbTest.kt`. Never add a per-kind command class again.
- State (r6b2, running): `state/UndoCoordinator.kt` owns the 8 s timer, one restart, ambiguous-retry with the same command, replace-older-offer, clearing on app lock/epoch change; `state/EditorHooks.kt`; `AncViewModel` has no per-kind delete branches. After it lands the old check `check_r6a.py` (S300, S302 need old names) and `STATIC-r6a` must be relaxed on purpose.
- Shared UI (r6b3, design round, owner reviews): `DeleteConfirmDialog`, app-level `UndoBarHost` in `AncApp`, `DeleteIconButton` (48 dp minimum), strings once. Fix the known leftover: notice values `MEDICATION_DELETED/RESTORED` alias `MEDICATIONS` so the UI would say "Medication saved" after a delete; give them real enum values.
- Wiring: r6b4 medication and lab; r6b5a/b/c ultrasound, previous pregnancy, visit; r6b6 attachments (spec addendum first: files are shredded only after the Undo window). Dating is edit-only, not deletable.
- Boundary rules to test in every kind (D654): the command carries exactly the confirmed type, patient and record id; a restore of one kind or patient never changes another; snapshot-diff over all tables, `foreign_key_check` and `integrity_check` after every delete/restore.
- Also owed on screen: collapsible long lists in the medication entry (previous medications and presets: first few shown, "Show all / Show fewer", filter by typing); multi-drug "Today's list" with Stop/Undo; delivery-plan edit currently opens the wrong form (`WorkspaceScreen.kt` ~806-818, `editDeliveryPlan`) and waits for the owner's field list (w04).
- Part B after the logic: visual redo of lab, ultrasound, medication and other sections; compare emulator screenshots at 360 dp with the mock side by side, list gaps, then show the owner.
- Not yet verified: Undo-bar timing and lock clearing in a real ViewModel test (r6b2 adds a fake-clock test), retried committed restore with the same operation id, frontier-tier reviews listed in section 1, full emulator suite.

### Status 2026-11-02 (end of logic round A for delete/Undo) - supersedes the "running"/"owed" wording above
- DONE and committed locally (nothing pushed): coordinator 404a35a; shared dialog, Undo bar, delete icon wired to medications and lab results cdd0308; review fixes dac3f46, 0b8e380, 0fba1b6, 33e63cd; reuse clean-up and guide 7f26beb (`.agents/pipeline/ADDING_A_DELETABLE_KIND.md`, `check_r6c.py`, registry completeness test).
- Verified: gate green, all `check_r*.py` pass, `DeleteUndoUiTest` 21 of 21 on the emulator, 54 real-database storage cases. On the owner's phone (QA copy `com.sherifradwan.anc.qa` only): delete with confirm, Undo bar naming the patient, expiry, and Undo restoring the item (owner confirmed). Owner decisions: Undo window stays about 8-10 s; lock during the countdown = delete is final immediately; "Save and next" Undo needs no phone test.
- NOT verified / owed: frontier + independent test review of 33e63cd; coordinator-level late-Undo-failure-after-lock guard has no test; `repository.execute` throwing; narrow-screen rendering of the failure message; accessibility timeout; full emulator suite. Visual flaw seen on the phone: the Close button of the "Medication deleted." notice card floats mid-card (fix in Part B).
- Lab saves do not offer Undo of a save (medication only); add only if the owner asks.
- NEXT kinds (each: adapter + registry + `kinds()` + notice + string + call site, per the guide): ultrasound, previous pregnancy, visit, attachments (spec addendum first), then multi-drug "Today's list", delivery plan (needs field list w04).
- Phone testing: wireless debugging works; tap tests need the owner to remove floating chat bubbles first (a bubble swallowed a tap once and opened another app; never open or read other apps).
- Scripts: repeated commands live in `.agents/scripts/` (README lists them). Owner rule: anything run twice becomes a named script there.
- Part B (visual redo) is now the active work, starting with the medication section.

### Status 2026-11-02 (end of medication simplification; Claude stops here, Gemini continues)
- DONE, committed locally (nothing pushed): e6642ca (reduced medication editor: name, dose, route, Active/Stopped, ONE "Note / instructions"; pinned Cancel/Save/Save & add another bar; card = bold name, Active/Stopped badge, lighter line "dose · route · note"; same on print; tab label "Meds"; one-line Undo bar) plus a follow-up commit removing the hidden frequency copy from presets and previous prescriptions. Screenshots: `.agents/pipeline/design/medication-redo2/` (360 dp).
- Owner decisions: only Active and Stopped (Stopped stored as Discontinued; stored enum and DB untouched). No legacy or old-patient handling (app not in use). A note is NOT required (dose with no note prints "500 mg · Oral"). Frequency, end date, side effects and start date are no longer editable; stored fields stay in the model, a new entry gets today as start date. Aim: simplify each section.
- Verified: build gate OK (after the follow-up too), static checks OK after commit, subagent-reported emulator classes MedicationListCollapseTest 4, DeleteUndoUiTest 21, MedicationEntryDesignTest 7, AncWorkflowTest 9 (not re-run after the follow-up; re-run them).
- NOT verified: printed report not rendered; Save and "Save & add another" not tapped on a device; Stopped status screenshot; RTL, text-size steps, dark theme; no frontier or independent test review yet of 33e63cd, 2fc5e1e, 4761f3f, e6642ca or the follow-up (AGENTS.md level-3 rule: owed).
- Visual nits to fix: instructions line wraps to 3-4 lines for long notes; "Save & add another" wraps to two lines; expanded card still lists an uneditable End date row; the word "Meds" also replaced "Medications" on the "+ Meds" button and profile section (check wording).
- NEXT: (1) Opus review of the medication commits; (2) design and build "Today's list" / tap grid / Stop with Undo (Claude's choice of design, owner delegated); (3) Part B for lab and ultrasound: simplify each section, compare 360 dp screenshots to the mock `.agents/pipeline/design/d4/`; pending owner decisions lab keypad (d17), ultrasound reference values (d18), print title rename (l04), delivery-plan field list (w04).
- Tracker artifact https://claude.ai/artifact/PaLVQNoN83bLDb5HzZ2Nss still needs: medication redo status, the owed reviews, the decisions above (not updated by Claude).
- Reminder: anything run twice becomes a script in `.agents/scripts/` (README lists them).

### Gemini start (2026-11-02): read `.agents/pipeline/design/GEMINI_START_PROMPT.md` first, then `.agents/pipeline/design/REDESIGN_SPECS.md` (fixed specs from the approved mocks; owner overrides listed there beat the mock).

### Status 2026-11-03 (Gemini: medication review fixes, design nits, emulator verification)
- DONE and committed locally (nothing pushed):
  - Logic commit `8f8da08`: addresses Opus review findings on medication simplification (ClinicalSummaryGenerator aligned with card and print; preset / past prescription picking explicitly clears hidden fields; chained medication starts today; validation failure reducer fails fast on unhandled edit kinds without hardcoding deletable kinds; D24 recorded in OWNER_DECISIONS.md; PrintLayoutTest regex backtrack eliminated).
  - Design commit `675a4fa`: capped instructions line to 2 lines with ellipsis (full on expand); "Save & add another" button on single line with labelMedium and weight(1.8f); removed uneditable End date row from expanded card; separated `medications_tab` ("Meds") from `medications` ("Medications") so profile section header and overview button show "Medications" while tab displays "Meds".
  - Screenshot of Stopped state captured on emulator: `.agents/pipeline/design/medication-redo2/5-stopped-state.png`.
- Verified:
  - `run_build_gate.sh`: GATE OK.
  - `run_static_checks.sh`: STATIC CHECKS OK.
  - Re-run all 6 emulator test suites on `emulator-5554` (API 26, 360 dp / density 480):
    - `MedicationListCollapseTest`: 4/4 passed.
    - `DeleteUndoUiTest`: 21/21 passed.
    - `MedicationEntryDesignTest`: 7/7 passed.
    - `AncWorkflowTest`: 9/9 passed.
    - `LabEntryDesignTest`: 7/7 passed.
    - `UltrasoundGuidedFlowTest`: 8/8 passed.
    Total: 56/56 passed.
  - Rendered print report sample via `PrintSampleDumpTest` and verified medication section formatting.
- 2026-11-03 12:47 Logic round l06 completed (commit `fd8b7c5`):
  - Created `CustomChoiceModels.kt` with `CustomChoiceList` (17 lists, dating methods strictly excluded per safety invariant), pure `validateCustomChoice`, `CustomChoicesDelegate`.
  - Implemented thread-safe `CustomChoicesDelegate` in `ClinicalCodexRepository.kt` with SharedPreferences persistence, duplicate prevention on Undo, and validated import/export.
  - Extended `EncryptedBackupManager.kt` & `VaultController.kt` to serialize/restore custom choices with fault tolerance and schemaVersion 3 backwards compatibility.
  - Verification: `CustomChoicesLogicTest` unit tests (10/10 passed), `CustomChoicesStoreInstrumentedTest` on `emulator-5554` (1/1 passed), build gate `GATE OK`, static checks `STATIC CHECKS OK`. Frontier Opus review (`review_l06_custom_choices.out.txt`) verdict `PASS WITH NITS` (all should-fix resolved).
- NOT verified:
  - Physical phone (Honor 400) execution.
  - Dark mode and RTL visually.
- 2026-11-03 14:13 Design round Medication "Today's list" completed (commit `e772dff`):
  - Spec `SPEC_DESIGN_TODAYS_LIST.md` implemented adhering to REDESIGN_SPECS 4.3 & mock d1 section 3.
  - Quick-add tiles with live binding to `ClinicalCodexRepository.medicationCodexItems` + custom drug choices from `CustomChoiceList.MEDICATION_NAME`.
  - Recent tab filtering patient's real medication history.
  - Safe 1-tap add for standard drugs; custom / undosed drugs route to editor for explicit dose entry.
  - "+ Add other" integration with `FlowRow` and custom drug dialog validating via `addCustomChoice`.
  - "Today's list" section card with active count summary, 48 dp Stop button, and "Stopped in this session" section showing medications stopped today with 1-tap Restart (clears end date, preserves original course start date).
  - Draft dispatch safety: operations guarded by `canStartEditor` (`canEdit && !hasDirtyDraft && !isSaveBlocked && !deleteRunning && !undoRunning`).
  - Verification: `run_build_gate.sh` (`GATE OK`), `MedicationTodayListTest` (9/9 passed on `emulator-5554`), regressions passed (`MedicationListCollapseTest` 4/4, `DeleteUndoUiTest` 21/21, `MedicationEntryDesignTest` 7/7, `AncWorkflowTest` 9/9, `LabEntryDesignTest` 7/7, `UltrasoundGuidedFlowTest` 8/8; total 65/65). Frontier reviews via `launch_opus_review.sh` addressed all clinical blockers and should-fixes.
  - Screenshots at 360 dp density: `6-todays-list-top.png` and `7-todays-list-bottom.png`.
  - `run_static_checks.sh`: STATIC CHECKS OK.
- NOT verified:
  - Physical phone (Honor DNY_NX9) touch testing.
- NEXT:
  1. Logic l08 then design d28: visit entry rules and screen (4.4).
  2. Part B: lab and ultrasound polish (4.1, 4.2), directory and chart (4.5, 4.6), history/dating/attention (4.7).
  3. Delete adapters for ultrasound, previous pregnancy, visit, attachments (guide ADDING_A_DELETABLE_KIND.md).
