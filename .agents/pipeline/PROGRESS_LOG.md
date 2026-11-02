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
