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
