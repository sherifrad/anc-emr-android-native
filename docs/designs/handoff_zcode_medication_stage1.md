# Handoff to zcode: Meds tab revision, Stage 1

Date: 2026-11-11. Repository: `/Users/sherifgamalradwan/anc-emr-android-native` (Android, Kotlin, Jetpack Compose, MVI, Room schema v6 frozen). Branch: `main`. Strictly local commits, never push.

## How to work

1. Run `/antislop` first and keep its rules for every line you write: code, comments, strings and your reports. Plain words, no filler, no generic comments.
2. Run `/maca` (multi-agent-collaborative-audit) for the rest. Use the Full path. The project is safety-critical (see `.agents/PROJECT_PROFILE.md`).
   - The builder must not review its own work. A reviewer from a different model family reads each change, and a test reviewer who did not write the tests checks them.
   - Level-3 paths need a frontier-tier review plus an independent test review (`AGENTS.md`). Level-3 here means everything under `app/src/main/java/com/sherifradwan/ancnative/state/`, `storage/`, `calculation/`, and `ui/patienthub/PatientHubScreen.kt`.
   - Approved vendors for reviews: agy, codex, gemini, opencode, alphacode. They read only; they may not edit files.
3. Read `AGENTS.md` before any edit. Its rules that matter here: one canonical component per clinical entity, no UI file over 1,500 lines, nothing critical truncated at 360dp, Room schema v6 frozen, no legacy shims.
4. Report what ran and what did not. Never claim a check you did not run.

## What was asked and why

The doctor reported that the Meds tab refreshes on every tap, and that the add-medication list is too long. Stage 1 fixes both. The route change that opened a full-screen editor was the cause of the refresh. The design is `docs/designs/medication_section_revision.md` (read it fully). The approved clickable mock is `docs/mocks/medication-section.html`. Decisions made with the doctor:

- Adding and stopping happen in place on the Meds tab, in a bottom sheet. No navigation.
- Stop is one tap with a 10 second Undo bar. A different stop date is set by editing the row.
- The picker is one collapsed row. First add takes 3 taps, later adds 2 while the picker stays open.
- Cancel on an untouched sheet closes at once; the discard prompt shows only after an edit.
- The doctor approved: frequency becomes required for new prescriptions, and the patient name must not be cut off at 200% text. Both are still to do (see "Work for you").
- Medication settings, course length, change history and the "add to my medication list" banner are Stage 2.

## State of the repository

Committed locally (newest first):

| Commit | What |
| --- | --- |
| `a4137ee` | Design note about the features dropped with the old editor; `state/*` added to profile Risk paths |
| `c32eb57` | `AncState.kt` split into `AncState.kt`, `AncEvent.kt`, `AncReducer.kt`, `MedicationReducer.kt`, `ClinicalDraftReducers.kt`, `ReducerSupport.kt` (pure move, proven by `.agents/scripts/verify_pure_move.py --ignore-visibility`) |
| `3bd55b3` | Phase 2: sheet hosted in the patient hub, one-tap stop with Undo, old editor and `AncRoute.MedicationEditor` deleted |
| `c9c1a82` | Phase 1: `MedicationForm.kt`, `MedicationSheet.kt` |

Not committed yet: Phase 3 (collapsed picker, Show past, compact rows, strings, keyboard fixes). It is in the working tree and passes everything below. First job: confirm and commit it.

Phase 3 files in the working tree (all under `app/src/`):

- `main/java/com/sherifradwan/ancnative/state/AncState.kt` (+5: `MedicationDraft.chainedAdd`) and `state/MedicationReducer.kt` (+4). Level 3.
- `main/.../ui/ClinicalPresets.kt`, `ui/patienthub/PatientHubScreen.kt` (level 3), `ui/patienthub/components/MedicationCard.kt`, `MedicationForm.kt`, `MedicationSheet.kt`, `MedicationTapToAddSection.kt`, `ui/patienthub/model/MedicationTiles.kt`, `ui/patienthub/tabs/MedsTab.kt`, `main/res/values/strings.xml`.
- Tests: `androidTest/.../MedicationEntryDesignTest.kt`, `MedicationFormTest.kt`, `MedsTabSheetTest.kt`, new `MedsTabRestorationTest.kt`; `test/.../state/MedicationSheetStateTest.kt`, `test/.../ui/ClinicalPresetsTest.kt`, `test/.../ui/patienthub/model/PatientHubPureModelTest.kt`.
- Scripts and docs: `.agents/scripts/run_emulator_all_tests.sh` (now sets the emulator to 360dp and restores it), `.agents/scripts/README.md`, screenshots in `docs/mocks/screens/`.
- Unrelated modified files you must not commit with this work: `concepts_brief.md`, `design_draft.md` (earlier drafts), and untracked `.agents/ROUTING_LOG.jsonl`, `.agents/harness/`, `.agents/scripts/egm_*.sh`, `comprehension_report.md`, `critic_report.md`, `goldfish_review_report*.md` and similar report files at the repo root.

Last verified result (run by me, 2026-11-11): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks` BUILD SUCCESSFUL, 1194 unit tests, 0 failures. `.agents/scripts/run_emulator_all_tests.sh`: `TOTAL tests=435 failures=0`. No file from the protected list changed (see below).

Review status of Phase 3: Opus (frontier) reviewed round 1 and round 1 fixes, no blockers. The last small round (focus flag, one-line notes preview, picker keyboard fix, tab-sync callback) was NOT reviewed by a frontier model, because the Opus lane hit its quota. The GLM review of it is in `docs/designs/reviews/medication_section_revision/phase3r3/code_glm.md`. I checked its findings: the claim that opening a card hides the notes is wrong (`MedicationCard.kt` line 181 shows them in the expanded rows); the double-tap on Stop concern is already covered by the reducer, which ignores a second stop while one saves (test `aSecondStopTapOnTheSameRowWhileItSavesIsIgnored`); its route finding became task C2 and its focus finding became task C3. The Gemini review of this round returned nothing. Get a frontier-tier review of that last round. The files to review are the ones listed above, mainly `MedicationForm.kt`, `MedicationSheet.kt`, `MedicationCard.kt`, `MedsTab.kt`, `MedicationTapToAddSection.kt`, `PatientHubScreen.kt`.

## Work for you

### A. Confirm and commit Phase 3

1. Run `git status`, then re-run the build and the full emulator suite yourself (commands below).
2. Read the Gemini and GLM reports in `phase3r3/` if present. Fix real findings, reject false ones with a reason.
3. Get a frontier-tier review of the final Phase 3 round. Get an independent test review of `MedsTabSheetTest.kt` and `MedicationFormTest.kt`.
4. Commit only the Phase 3 files listed above, locally. Commit message ends with the attribution line the harness gives you.

### B. Frequency required for new prescriptions

Problem: `MedicationForm.kt` (around line 206) shows an error for `ClinicalValidationError.REQUIRED_FREQUENCY`, but `SaveMedicationCommand.prepare` in `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt` (around lines 440-480) never produces that error. A drug can be saved with no frequency.

Constraints you must keep:

- `ClinicalCommands.kt` was on the "must not change" list for Stage 1. The doctor approved this one change. Update the design first: add a Decisions line and change the "must not change" list in `docs/designs/medication_section_revision.md` (Non-Goals, Technical Plan section 7, the Detailed Implementation "must not change" paragraph).
- Require frequency only when creating a medication, not when updating an existing row. Reason: the one-tap stop and its Undo save the stored row through the same `prepare`. A stored row that has no frequency must still be stoppable and its Undo must work, otherwise the doctor cannot stop a drug.
- Do not touch the schema, the fingerprint format, `RecordsRepository.kt`, `RecordsDao.kt` or `UndoCoordinator.kt`. Backup export and restore and any other save path must not change behaviour.
- The message must show next to the frequency field in the sheet (the form already renders `medFreqError`); the "Other" choice with empty text is also an error.

Tests required: unit tests in `app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalCommandsTest.kt` for create without frequency (rejected), create with each fixed frequency and with Other text (accepted), update of an existing row without frequency (accepted), one-tap stop and Undo of a legacy row without frequency (accepted); reducer/state tests in `MedicationSheetStateTest.kt` for the error reaching the draft; an instrumented sheet test that Save with no frequency shows the error and keeps the sheet open (`MedsTabSheetTest.kt`). Rewrite any existing test that saved a new drug without frequency; do not weaken any assertion.

### C. Patient name cut off at large text

At 200% font scale at 360dp the patient header cuts the name to "Ma ya…" and clips "No known drug allergies" (see `docs/mocks/screens/meds-tab-360dp-200pct-text.png`). `AGENTS.md` says the patient name, gestational age, blood pressure and the high-risk alert must not truncate critically at 360dp.

File: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt` (level 3 path). Let the name wrap to two lines or allow the header to grow; keep gestational age, risk badge and allergy line fully visible; keep it one canonical component. Test: add to `app/src/androidTest/java/com/sherifradwan/ancnative/PatientHubDesignTest.kt` a 360dp, fontScale 2.0 case with a long name asserting the name is not ellipsized (check text layout, not only existence) and the other fields are displayed. Capture a new screenshot.

Commit B, C, C2 and C3 separately, each after review.

### C2. Already-active check should include the route

The picker marks a tile as "already on today's list" by drug name only (`activeMedicationNameKeys` and `isAlreadyActive` in `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/MedicationTiles.kt`, used by `MedicationTapToAddSection.kt`). The design says a different route is a separate drug. A patient on oral ferrous sulfate therefore cannot get the intramuscular iron tile opened. Key the check on trimmed, case-insensitive name plus route, keep the pure function and its unit tests in `PatientHubPureModelTest.kt`, and add a test for same name with a different route (tile stays openable). Check the stop and picker UI tests still pass.

### C3. Possible silent failure of name focus after "Save and add another"

`MedicationForm.kt` waits one frame and then calls `requestFocus()` on the name field, while the sheet may still be animating in. If the field is not attached yet, the request fails and is not retried. The emulator tests pass, so this may only show on a slower phone. Check the result of the focus request and retry once after layout, and keep the existing chained-add tests green.

### D. Final pass

1. Check the three journeys on the emulator at 360dp (review and stop one drug; add a known drug; change a dose), plus dark mode.
2. Run the complete verification (below) once more on the final tree.
3. Write the closing report: what was done, every review and its model, what was not verified, and open items.

## Protected files (do not change unless this document says so)

`app/schemas/*`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `storage/ClinicalCommands.kt` (only task B may change it), `UndoCoordinator.kt`, `ClinicalModels.kt`, the clinical summary and print generators. Check with:

```bash
git diff --name-only -- app | grep -E "schemas|Records|ClinicalCommands|UndoCoordinator|ClinicalModels|Generator"
```

## Verification commands

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks
./gradlew :app:compileDebugAndroidTestKotlin
.agents/scripts/run_emulator_test_class.sh <fully.qualified.TestClass>   # one class at a time
.agents/scripts/run_emulator_all_tests.sh                                 # about 10 minutes, expect failures=0
```

Notes: the emulator is `anc-test-api36`. Use `run_emulator_test_class.sh`, not plain `connectedDebugAndroidTest`: the plain one installs over and then removes the clinic app. The QA app's PIN no longer matches `.agents/scripts/qa_open_medication_tab.sh`, so screenshots were taken with a temporary instrumented test, deleted afterwards. `DraftAutosaverTest.slotClosedWhileSaveWaitedOnSaveMutexDoesNotResurrectDraft` failed once and passed 5 of 5 alone; treat it as a timing flake and report it if it fails again.

Review scripts (reuse, do not retype): `.agents/scripts/review_code.sh <lane> <out> <code|tests> <design> <files...>` with lanes `agy:<model>`, `opencode:<model>`, `alphacode`; `.agents/scripts/verify_pure_move.py` for pure moves. Known lane problems: AlphaCode sometimes returns an empty response; `agy:gpt-oss-120b-medium` only returns a plan; `agy:claude-opus-5-5-medium` hit its quota on 2026-11-11 and resets in a few hours.

## Reviewed findings that were rejected (do not re-raise without new evidence)

- Stacked discard prompts on Back: a test proves one prompt only.
- Cancel never prompts: the sheet asks first (touched flag).
- Enter on Notes inserts a newline and Next skips the route and frequency chips: approved in the mock.
- Dimmed active tiles: they already show a check and do nothing.
- Splitting `AncState.kt` was out of scope: the doctor asked for it.

## Follow-ups recorded for later (Stage 2 or small tasks)

Medication settings, course length, change history, "add to my medication list" banner and preset chips, frequency in the clinical summary and print output (its shared helper `medicationInstructions` is on the protected list), one shared expand/collapse row component, the picker tile "Active" badge reusing `MedicationStatusBadge`, translated route names, a lazy list for picker tiles, `otherMode` in `MedicationForm` possibly not surviving recreation, tap misses next to the collapsing hub header seen in some emulator tests, and the 360dp and rotation behaviour on a real phone.

## Not verified at all

Behaviour on a real phone: real keyboard, real finger swipe, real rotation, Arabic and right-to-left (there is no Arabic resource folder). The doctor should try the final build on a phone.
