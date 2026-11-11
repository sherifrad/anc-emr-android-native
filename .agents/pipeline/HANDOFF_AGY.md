# ROADMAP AND HANDOFF (living file)
Last updated: 2026-11-11 (main at `37ff23d` plus local edits). Owner: Dr. Sherif Radwan. Repo: `/Users/sherifgamalradwan/anc-emr-android-native`, branch `main`.

**How to keep this file true.** Every feature, fix or decision updates this file IN THE SAME COMMIT: move the mission row, add the artifact, add the dated line to the log. `.agents/scripts/git_audit.sh` warns when this file is older than the newest app commit. If this file and the code disagree, fix this file first.

## A. Rules that apply now (they replace the matching lines of section 0 below)
1. **Push only when the owner says so, each time.** The 2026-11-11 pushes (`04338a6`, `fa01e98`) were approved in chat. Never force-push.
2. **Before any work and before any push run `.agents/scripts/git_audit.sh`.** It lists unmerged branches, dirty worktrees, stashes, stray files and unreachable commits. Deliberate branches are listed in `docs/BRANCHES.md`.
3. **Phone safety.** The phone is the owner's daily device (`AX3C025527011668`). Never touch the clinic app `com.sherifradwan.anc`, never change phone settings, never uninstall. Only the QA copy `com.sherifradwan.anc.qa` may be installed. `docs/AI_HANDOFF.md` says the phone gets same-signer installs and read-only observation only; on 2026-11-11 the owner asked for app-level QA taps on the QA copy and `.agents/scripts/device_qa_meds.sh` ran them. Default from now on: observe (UI dumps) and ask before tapping. The script never enters a PIN or touches a fingerprint prompt.
4. **Team roles (owner decision 2026-11-11).** Other models do the work, Claude orchestrates and verifies: lanes are opencode GLM, opencode DeepSeek, alphacode and agy models (when they have quota). They are read-only: they return text; edits are applied by `.agents/scripts/apply_blocks.py` from SEARCH/REPLACE blocks (`propose_patch.sh`), checked and revised by the orchestrator. Builder and reviewer are different model families. Level-3 paths (`state/`, `storage/`, `calculation/`, `PatientHubScreen.kt`, `PatientHubHeader.kt`) need a frontier-tier review plus an independent test review; say when none ran. Every change ends with the full gate (`.agents/scripts/push_gate.sh pre`).
5. **Antislop skills (all variants) are run by other models**, never as a reason to edit files near an open feature.
6. **Cleanup is postponed to the end of the main missions** (owner decision 2026-11-11), because it conflicts with files the registration flow changes. See mission M6.
7. Commit locally after every verified round; one change per commit; do not leave work only in a working tree.

## B. Missions (status as of 2026-11-11)
| ID | Mission | Status | Next step |
| --- | --- | --- | --- |
| M1 | Meds tab revision, Stage 1: bottom sheet instead of the editor route, one-tap stop with 10 s Undo, collapsed picker, Show past, frequency required for new drugs, route-aware active check, header name at 200 percent text | DONE and pushed (`c9c1a82` to `fa01e98`). Design: `docs/designs/medication_section_revision.md` | Stage 2 list below (M1b) |
| M2 | Plan tab and delivery plan sheet: the Delivery Planning card shows only the four boxes (hospital, fees, plan, postpartum) and the attachments; the pencil opens a sheet that writes labelled lines into the obstetric notes; fetal presentation moved to the Scans tab | CODE DONE and pushed (`7923f8f` to `fa01e98`), **On the phone 2026-11-11 the pencil opens the sheet with the four fields (checked by screenshot); the owner's earlier report was not reproduced. One real defect found there: the Save button always read "Saving…". Fixed (it now reads "Save" until a save runs; test `theSaveButtonSaysSaveUntilASaveRuns`); the new QA build must be installed to see it.** Passes on the emulator: `DeliveryPlanSheetTest` 11, `DeliveryPlanViewModelTest` 2 (real view model). Design: `docs/designs/plan_delivery_editor.md` | **NOW: install the fixed QA build, owner tries a real save on the phone.** Known suspects: the pencil is hidden while `canModifyData` is false (a dirty draft, a running save, a running delete); the QA app held old in-memory state; the QA build on the phone. `AncViewModel.clearsFrozenPatient()` now lists `StartDeliveryPlanEditor` and `DeliveryPlanFieldChanged` (fixed). Notes rewrite on first edit (label words such as "agreed" and "package" are dropped): owner said 2026-11-11 to keep it, LOW PRIORITY, revisit later |
| M2b | Delivery outcome: whether the patient has delivered, and the delivery date (owner could not find it, 2026-11-11). Nothing like it exists in the app today: patient status is only active or archived | OWNER APPROVED 2026-11-12 (schema v6 to v7 and the protected files; four columns incl. mode and outcome; delivered patients leave the main dashboard). CODE DONE locally (commits 19fef44 to 76c4fd9, not pushed): migration 6 to 7, entity, DAO, repository, command and fingerprint, backup, draft state and recovery, sheet, card, header badge, dashboard filter, print and summary. Unit tests all pass; `DeliveryPlanSheetTest` 17, `EncryptedRecordsTest` 23 (includes v1 to v7 chain) on the emulator. Full emulator suite 456 of 456 passed. Independent review done by a fresh Claude subagent (builder was alphacode): no serious storage finding; 3 low findings fixed (validate only a changed outcome, stale values normalised, delivery fields kept out of recovered profile drafts). `DeliveryPlanSheetTest` failed once and passed on rerun (flake, not diagnosed). NOW: owner checks the QA build on the phone, then push approval Design `docs/designs/delivery_outcome.md` (v6 to v7, two columns on `patients`). Waiting on the owner's sign-off (schema change plus protected files by name) and 5 questions. Independent model review owed (opencode and agy out of quota, agy resets about 2026-11-14) | After sign-off: work packages 1 to 5 in the design; builders and reviewers from different families; frontier review for the protected files |
| M3 | Registration flow: the new patient entry form. The header pencil on the patient screen still opens the OLD form (`ui/PatientEditor.kt`) because the new flow is not merged | NOT STARTED on main. Work exists on branch `claude/clever-goldwasser-e086ce` (15 commits, last one a WIP commit of previously uncommitted files), worktree `.claude/worktrees/clever-goldwasser-e086ce`. It changes 173 files (27 main Kotlin files, `strings.xml`, tests) and conflicts with the `AncState` split, the medication sheet and the delivery plan sheet | After M2: write a merge plan (design note), rebase or merge the branch onto main, resolve conflicts by lane proposals, review, full gate, device check. Replace the header pencil target |
| M4 | Device QA on the phone | Plan `docs/designs/device_qa_plan.md`; driver `.agents/scripts/device_qa_meds.sh` passed 15 of 15 on 2026-11-11 for the Meds tab (phone QA copy) | Add the Plan sheet checks; keep to the phone safety rule in A3 |
| M5 | Git hygiene | DONE: `git_audit.sh`, `docs/BRANCHES.md`, rule in `AGENTS.md`, 11 old unreachable commits kept under `refs/archive/`, main worktree clean | Run the audit at the start of every session |
| M6 | Deep clean of code, directory and app (antislop skills by other models, duplicates, unused code and resources) | **POSTPONED to the end of M2 and M3.** Evidence gathered and committed under `docs/designs/reviews/cleanup/`: 10 compiler warnings, 126 unused resources (lint), 37 duplicate clusters, 172 unused imports (31 removed in `37ff23d`; 107 sit in files the registration branch changes; 6 in protected storage files), 38 review batches (`batches/`) and a few `antislop-code` reports (`reports_code/`, run stopped on purpose) | After M3 is merged: re-run `find_unused.py` and `find_duplicates.py`, finish the antislop reviews (all six skills, other models), apply only compile-proven or test-covered cleanups in small commits, never touch the protected storage files without sign-off |
| M7 | Older roadmap (section 2 and 3 below: design rounds, logic rounds L-1 to L-7, text-size control, custom choices) | UNKNOWN: much of it was overtaken by the patient hub work. Not re-triaged | After M3: go through sections 2 and 3, mark each item done, obsolete or open |

### M1b. Meds Stage 2 (not started)
Medication settings screen (favourites, variants, the saved list); course length; change history (needs a schema change and clinical sign-off); the "add to my medication list" banner and the preset chips removed with the old editor; frequency in the clinical summary and print output (protected helper `medicationInstructions`); a guard against a second active row through the blank Prescribe form; one shared expand/collapse row component; the picker tile "Active" badge reusing `MedicationStatusBadge`; translated route names; a lazy list for picker tiles; `otherMode` in `MedicationForm` possibly not surviving recreation; rotation and 360 dp behaviour on a real phone.

## C. Open decisions for the owner
1. Notes rewrite: is it acceptable that the first Plan edit rewrites a patient's obstetric notes into labelled lines (nothing dropped, label words such as "agreed" and "package" lost)?
2. Push the local commits that are ahead of `origin/main` (list them with `git log origin/main..main`).
3. Registration flow: order and the exact scope of the merge (M3).

## D. Artifact index (what was created and where)
| Area | Path |
| --- | --- |
| Meds design, review reports, decisions | `docs/designs/medication_section_revision.md`, `docs/designs/reviews/medication_section_revision/` (stage reports, mock reviews, phase 1 to 3 reviews, device QA runs excluded from git), working drafts in `docs/designs/archive/` |
| Clickable Meds mock and device screenshots | `docs/mocks/medication-section.html`, `docs/mocks/screens/` |
| Plan and delivery design, brief, reviews | `docs/designs/plan_delivery_editor.md` (amendments A1 to A19 in section 11), `docs/designs/briefs/plan_delivery_editor_brief.md`, `docs/designs/reviews/plan_delivery_editor/` |
| Device QA | `docs/designs/device_qa_plan.md`, `.agents/scripts/device_qa_meds.sh` |
| Hand-off for the zcode lane (Meds Stage 1) | `docs/designs/handoff_zcode_medication_stage1.md`, `docs/designs/zcode_start_prompt.md` |
| Git hygiene | `.agents/scripts/git_audit.sh`, `docs/BRANCHES.md`, `refs/archive/` |
| Lane tooling | `lane_ask.sh`, `propose_patch.sh`, `apply_blocks.py`, `review_code.sh`, `review_artifact.sh`, `antislop_review.sh`, `run_cleanup_reviews.sh`, `egm_goldfish.sh`, `egm_review_agy.sh` (all under `.agents/scripts/`) |
| Gates | `push_gate.sh` (pre and post push), `run_emulator_all_tests.sh`, `run_emulator_test_class.sh`, `set_emulator_360dp.sh`, `install_qa_build_on_device.sh`, `verify_pure_move.py` |
| Cleanup evidence | `docs/designs/reviews/cleanup/`, `.agents/scripts/find_duplicates.py`, `.agents/scripts/find_unused.py` |
| Earlier ledger | `.agents/AI_HANDOFF_LEDGER.md` (provenance, approvals, reviews) |

## E. Log
- 2026-11-12 (end): review fixes committed; QA build installed on the phone.
- 2026-11-12 (later): built M2b in packages (model, storage, state, UI, print). Builders: alphacode proposals applied with `apply_blocks.py` and fixed by Claude where they slipped (null parse crash, wrong imports, a missed mapping). opencode and agy were out of quota. New script `.agents/scripts/propose_excerpt_patch.py`.
- 2026-11-12: owner chose real database fields for delivery outcome. alphacode drafted a plan, Claude corrected it (invalid CHECK, scope creep, backup is manual JSON, fingerprint needs the new fields); `docs/designs/delivery_outcome.md`.
- 2026-11-11 (late): owner asked for delivered yes/no and the delivery date; confirmed absent from the app, added as M2b. Notes rewrite kept, marked low priority.
- 2026-11-11 (late): the Delivery Plan sheet opens on the phone with the four fields. Fixed the Save label that always read "Saving…" and the frozen-patient event lists in `AncViewModel`. `DeliveryPlanSheetTest` 12 of 12 on the emulator.
- 2026-11-11: M1 and M2 code pushed. Owner tested the QA build on the phone and found no way to open the delivery sheet (M2 reopened). Owner approved: drop the six repeated tiles from the delivery card and show fetal presentation on the Scans tab (done); stop the cleanup and postpone it to the end (done); this file becomes the living roadmap.
- 2026-11-11: git audit found yesterday's registration flow work as 3,684 uncommitted lines in a side worktree; committed there as WIP, recorded in `docs/BRANCHES.md`.

---

# Older roadmap and handoff for Antigravity (written 2026-11-01, kept as history)
Sections 0 to 8 below were written for branch `maca-round2` and are partly out of date. Section A above wins where they differ.

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
- 2026-11-03: Logic round l08 (Visit entry rules, BP flagging, normal fill, and draft persistence, commit `a07d616`):
  - Created `VisitEntryLogic.kt` with pure clinical domain rules: BP flagging via canonical `bpLevel`, strict normal fill isolation, historical vitals resolution, GA-estimated fundal height, `" · "` token boundary parser/builder, flagged findings detection, and single-sided BP summary formatting.
  - Extended `VisitVitals.kt` with `bpLevel(systolic, diastolic)` preserving single threshold source of truth.
  - Added visit presets to `CustomChoiceModels.kt` (`VISIT_BP`, `VISIT_WEIGHT`, `VISIT_FUNDAL_HEIGHT`).
  - Extended `DraftValues`, `DraftField`, `AncReducer`, `SaveVisitCommand`, and `DraftMapping.kt` with the 6 structured fields (`systolic`, `diastolic`, `fundalHeight`, `oedema`, `fetalMovements`, `urineProtein`).
  - Frontier review: DeepSeek reviewed; remediated 4 Hard Blockers (HB-1 findings precedence, HB-2 blank BP clearing, HB-3 token boundary matching, HB-4 draft recovery mapping).
  - Verification: `VisitEntryLogicTest` (12/12), `VisitDraftStateAndCommandTest` (12/12), `CustomChoicesLogicTest` (11/11), `DraftMappingTest`, `AncWorkflowTest` (9/9 on emulator-5554), `run_build_gate.sh` (`GATE OK`), `run_static_checks.sh` (`STATIC CHECKS OK`).
- 2026-11-03: Design round d28 (Visit and vitals screen redesign, commit `ce296c7`):
  - Spec `SPEC_DESIGN_D28_VISIT_ENTRY.md` implemented adhering to REDESIGN_SPECS 4.4 & approved mock d4/d3 section 3.
  - Rebuilt `VisitEditor.kt` into a 4-step wizard with `SegmentedProgressBar` (`Role.Tab`), step progress header, and pinned action bar.
  - Step 0: Date chips ("Today", "Yesterday", picker), dual systolic/diastolic numeric inputs with `/` separator, "Same as last (sys/dia)", "Normal (110/70)", codex presets, "+ Add other", and high BP warning banner in `secondaryContainer` when BP $\ge 140/90$.
  - Step 1: Weight & Fundal Height with `ValueStepper` (- / + buttons with 48 dp targets; decimal for weight, integer for fundal height), "Same as last", "Matches GA", codex presets, "+ Add other".
  - Step 2: Findings with "Fill normal values" in `secondaryContainer` (fills only blank findings with None, Good, Negative), `BigChoiceButton` cards for Oedema, Fetal Movements, and Urine Protein (flagged choices highlighted in red), narrative findings field.
  - Step 3: Review & Save with summary card, red alert badges (`detectFlaggedFindings`), 7 review rows with direct step jumping, optional notes, and pinned "Save visit" button.
  - Closed palette compliance via `MaterialTheme.colorScheme` tokens, 48 dp touch targets, S70 compliance maintained.
  - Added reusable scripts `.agents/scripts/screenshot_mock_html.sh` and `qa_open_visit_editor.sh`.
  - Captured 360 dp screenshots: `visit-mock.png`, `visit-built-step0.png`, `visit-built-step1.png`, `visit-built-step2.png`, `visit-built-step3.png`.
  - Verification: `run_build_gate.sh` (`GATE OK`), `run_static_checks.sh` (`STATIC CHECKS OK`), `VisitEntryDesignTest` (5/5 passed on emulator-5554), `AncWorkflowTest` (9/9 passed on emulator-5554). Frontier review by DeepSeek.
- 2026-11-03: Ultrasound Delete Adapter (commit `fcff3bc`):
  - Added `DeletableType.ULTRASOUND` to the unified delete/confirm/Undo mechanism per `ADDING_A_DELETABLE_KIND.md`.
  - Implemented `UltrasoundDeleteAdapter` with unique fingerprint tag `"US"`, receipt kinds `ULTRASOUND_DELETE` and `ULTRASOUND_RESTORE`, 18-field canonical string with stable `ImageUriCodec.serialize`, `load`, `deleteRow`, `insertRow`, and `record`.
  - Added DAO query `deleteUltrasoundScan(id, patientId, expectedRevision)`.
  - Added `SavedNotice` notices and localized strings.
  - Added `UltrasoundEditorHook` and wired in `EditorHooks`.
  - Wired trailing `DeleteIconButton` in `UltrasoundCard` with 48 dp touch target and spoken description `"Delete ultrasound scan: <date>"`.
  - Updated `check_r6c.py` enforcing zero-leak invariants on `ULTRASOUND`.
  - Added `.agents/scripts/qa_open_ultrasound_tab.sh`.
  - Tests: `DeletableRegistryTest` (JVM), `ClinicalDeleteRestoreDbTest` (81/81 passed on emulator), `DeleteUndoUiTest` (25/25 passed on emulator).
  - Screenshots at 360 dp density: `ultrasound-tab-with-delete.png`, `ultrasound-delete-confirm-dialog.png`, `ultrasound-undo-bar.png`, `ultrasound-restored.png`.
  - Frontier review: Claude Sonnet independent review returned `PASS`.
- 2026-11-03: Physical Phone Deep Android Testing Audit (Honor DNY-NX9):
  - Safely installed QA build `com.sherifradwan.anc.qa` over wireless debugging.
  - Successfully validated Ultrasound delete affordance, confirmation dialog, 8-second countdown Undo bar, and restored state on hardware.
  - Successfully validated Visit Entry 4-step wizard: quick date chips, dual BP input, Normal BP preset ("110/70"), ValueStepper controls, "Same as last (68.0 kg)", "Fill normal values", live review summary banner, clickable review rows with direct step jumping, and clean persistence of new visit card with incremented revision in Room.
  - 10 screenshots captured in `.agents/pipeline/design/device-test/`.
  - Zero crashes, zero runtime exceptions in logcat.
- 2026-11-03: Visit Delete Adapter (`DeletableType.VISIT`) — COMPLETE & COMMITTED (`d9dff55`):
  - Fixed the user-reported bug where only ultrasound cards on the Visits tab had a delete icon while routine visit cards lacked one.
  - Implemented `VisitDeleteAdapter` (`tag = "VIS"`, `VISIT_DELETE`, `VISIT_RESTORE`, `section = WorkspaceSection.VISITS`), `VisitEditorHook`, DAO queries (`deleteVisit`, `deleteSaveReceiptsForVisit`), tombstone replay guard in `RecordsRepository.save(SaveVisitCommand)`, and `DeleteIconButton` on `VisitCard`.
  - Disambiguated `HorizontalPager` pre-composed timeline standalone ultrasound tags (`timelineUltrasound-${scan.id}`).
  - Verified: `check_r6c.py` PASS, `run_build_gate.sh` `GATE OK`, `ClinicalDeleteRestoreDbTest` 112/112 PASS on emulator, `DeleteUndoUiTest` 29/29 PASS on emulator, independent Claude CLI frontier review issues resolved.
- 2026-11-03: Previous Pregnancy Delete Adapter (`DeletableType.PREVIOUS_PREGNANCY`) — IN PROGRESS (Code & Unit Tests Complete in Working Tree):
  - All 16 files for `DeletableType.PREVIOUS_PREGNANCY` are implemented in the working tree (`PreviousPregnancyDeleteAdapter.kt` untracked + 15 modified files).
  - Verified: `python3 .agents/checks/check_r6c.py` PASS (R6C0–R6C6), `.agents/scripts/run_build_gate.sh` `GATE OK` (915+ JVM unit tests pass).
  - Fixed `PreviousPregnancyKind.create` in `ClinicalDeleteRestoreDbTest.kt` to use `modelVersion = 3`, `birthMethod = "Vaginal"`, `provenanceKind = "CLINICIAN_ENTRY"` per `PatientVocabulary` validation rules.
  - WHERE WE STOPPED (Immediate Next Steps for Incoming Agent):
    1. Run `ClinicalDeleteRestoreDbTest` (140 parameterized tests) and `DeleteUndoUiTest` (33 tests) on `emulator-5554` using `.agents/scripts/run_emulator_test_class.sh`.
    2. Run independent frontier review (`git diff HEAD app/src .agents/checks | claude -p ...`) on the `PREVIOUS_PREGNANCY` diff.
    3. Stage the 16 files and commit locally (`logic(obstetric): previous pregnancy delete adapter, undo bar, and delete icon button`).
    4. Proceed to Part B Visual Polish (Lab §4.1, Ultrasound §4.2, Directory & Chart §4.5–4.6, Obstetric History / Dating / Clinical Attention §4.7) and Attachments Delete Adapter (`r6b6`, spec addendum first).
  - Full Deletable Report & Continuation Prompt: `.agents/pipeline/DELETABLE_REPORT_AND_HANDOFF.md`.

### Status 2026-11-05 (AutoCoder): attachment delete/Undo mission CLOSED — read this before the sections above
- **Supersedes the "Previous Pregnancy in progress" block at the end of this file.** That adapter was committed on 2026-11-03 (`b5a416a`, then `3948f79`); nothing is left uncommitted from it.
- The global delete / confirm / Undo mechanism now covers **all six kinds**: medication, lab, ultrasound, visit, previous pregnancy, attachment. Attachment payload files are handled end to end: committed delete → 8 s window → file removed; Undo → file kept; app killed or deletion failed → next unlock sweeps orphans.
- Attachment round commits: `b6af671` (failure surfaced, every exit path reports, unlock sweep, restore revision), `cff3841`/`773131f`/`0b417c8` (single confirmation, human label, strings), `7ac25c5` (review blockers: scan-photo references, in-flight restore, off-main cleanup), `4adb5e0` (attachment in both suites, viewer Cancel, checker gaps), `69cdc57` (re-review nits).
- **Review trail:** independent review returned **FAIL** with three blockers (all real, one data-loss) → fixed → re-review **PASS WITH NITS** (0 blockers, 0 should-fix) → nits fixed. Reports: `.agents/pipeline/review_attachment_cleanup_2026-11-05.md` and `..._rereview_2026-11-05.md`.
- **Verified state (code at `752d4cb`):** 948 JVM tests in 87 classes, 0 failures; `run_build_gate.sh` GATE OK; `run_static_checks.sh` STATIC CHECKS OK; emulator `anc-test-api26` at 360 dp: `DeleteUndoUiTest` 36/36, `ClinicalDeleteRestoreDbTest` 168/168 (attachments parameterised). Phone QA copy updated in place (data preserved, clinic app untouched).
- **NOT verified on this mission:** nothing outstanding. The reviewer did not run the instrumented suites itself; those runs were the orchestrator's.
- **Next queue:** print defects P-1/P-2 (mid-word breaks in report tables, duplicated "Official Stamp & Signature"), then Part B design rounds (Lab §4.1, Ultrasound §4.2, Directory §4.5, Chart §4.6, History/Dating/Attention §4.7), then the postponed device tests in `POSTPONED_DEVICE_TESTS.md`, then the older audit items (`loadSnapshot()` whole-DB read, git history hygiene before any push, R8).
- **Hard rules unchanged (§0):** never push; QA copy only on the phone, never the clinic app; no patient data to vendors; a round is logic or design, never both.
