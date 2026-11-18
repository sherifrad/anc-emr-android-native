# Handoff: start here (written 2026-11-19)

For any model or person who continues this project. Read this, then `AGENTS.md`, then `docs/ROADMAP.md`. Everything else is reference.

## 1. What this is
Native Android clinical antenatal EMR for one clinic (Kotlin, Jetpack Compose Material 3, MVI, Room with SQLCipher, schema v7 frozen). Package `com.sherifradwan.ancnative`. The owner is an obstetrician; patient data is real, so correctness comes before speed.

## 2. Where truth lives (do not duplicate it elsewhere)
| What | Where |
|---|---|
| Rules, architecture, protected files | `AGENTS.md` (top block is generated, do not hand edit) |
| Progress and open work | `docs/PROJECT_STATE.json`, rendered to `docs/ROADMAP.md` (edit only with `.agents/scripts/project_state.py set|add|log`, then `render`) |
| Live roadmap page | Claude artifact "ANC EMR Production Roadmap" https://claude.ai/artifact/PaLVQNoN83bLDb5HzZ2Nss, database doc `state/main`; sync with `project_state.py artifact-doc /tmp/state_main.json` then ArtifactData `set` with `if_version` |
| Branches that are deliberate | `docs/BRANCHES.md` (only `main` is expected) |
| Go-live steps | `docs/GO_LIVE_CHECKLIST.md` |
| Redesign contract (visual) | `docs/designs/redesign/PLAN.md` and `docs/designs/redesign/mocks/` |
| Print v2 plan and approved layout | `docs/designs/reviews/export_print_v2/BUILD_PLAN.md`, approved mock `docs/designs/mocks/export_v2/mock_b3.html` (+ `.pdf`) |
| Last gate and phone smoke results | `docs/qa/gate/`, `docs/qa/prepush/REPORT.md` |
Older work orders, prompts, patches and run logs were deleted on 2026-11-19; they are in git history (`git log --stat -- <path>`, commit "docs: deep clean").

## 3. Owner rules that must never be broken
- Push only when the owner says so in the current conversation; never force push.
- Never touch the clinic app `com.sherifradwan.anc` on the phone. QA copy only: `com.sherifradwan.anc.qa`. Never uninstall, never change phone settings, never enter a PIN.
- Protected files (no edit without explicit owner sign-off): schemas, `RecordsDatabase`, `RecordEntities`, `RecordsDao`, `RecordsRepository`, `UndoCoordinator`, `ClinicalModels`, `*Generator*` (the print generator was edited for the doctor-name work and print v2 with approval), `state/` reducers, `ui/DatingFieldVisibility.kt`.
- Every date field uses the date picker, never typed text.
- Level 3 files (auth, security, data, clinical, report generator) need a frontier-tier review plus an independent test review. The builder is never the test author and never the reviewer.
- 360 dp first: no truncation of name, GA, BP, risk. Arabic names right-aligned through `PatientNameText`.
- No hardcoded UI strings, sentence case, no em dashes, antislop. Reuse one canonical component, never copy.
- "No mistakes allowed": say plainly what was not verified.

## 4. State on 2026-11-19
- `origin/main` has the whole-app redesign (slices 0 to 21), polish, the doctor-name and clinic-phone settings, and print v2.
- Print v2 (task rd32, still marked active): crash fix (Activity unwrap), shared data layer `ui/report/ReportData.kt`, print HTML in layout B3, native report from the same data, picture picker before print (nothing selected by default), share as PDF and as a 15-line text, new `reports/` cache path in `res/xml/file_paths.xml`.
- Last full gate: unit 1707 pass, lint 0 errors, emulator 975 of 977 (the 2 failures pass alone: `DeliveryPlanSheetTest.aRejectedSaveKeepsTheSheetTheValuesAndShowsTheMessage` and `ScansTabRedesignTest.measurements_threePerRow_wordsTwoPerRow`, both order dependent).
- NOT verified: a real print and PDF share on a device (the headless emulator WebView renderer crashes, so `PrintHandoffTest` skips there). The QA build with print v2 was installed on the phone on 2026-11-18; the owner was to try Print and Share.

## 5. Open work (details are tasks in the roadmap)
1. Owner phone check of print v2 (Print, Share PDF, Share text, picture picker). Fix what the owner reports.
2. Owner approval still owed for the `reports/` cache path in `file_paths.xml`.
3. rd33 Meds stage 2 (edit the medication list in settings like lab tests), rd34 delivery follow-ups go into the postpartum note, rd35 deep clean, rd36 frontier review of the registration merge, rd37 and rd38 small accessibility items, rd42 refactor follow-ups.
4. Print follow-ups chosen by the owner: visit findings text and "Not recorded" for missing scan measurements are not printed in layout B3; medications in the short share text are names only.
5. Older roadmap items that may already be fixed by the redesign are marked "needs a look on the phone" (u06, u09, u11).
6. Go-live: signed release build, backup first, never uninstall the clinic app (see the checklist).

## 6. How to work (tools in `.agents/scripts`, see its README)
- Branch hygiene before and after work: `.agents/scripts/git_audit.sh`.
- Unit gate: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`; androidTest compile: `./gradlew :app:compileDebugAndroidTestKotlin`.
- Whole gate before a push: `.agents/scripts/push_gate.sh pre` (about 85 minutes, needs the emulator); after pushing: `push_gate.sh post`. The protected-files line flags files changed since `STAGE_BASE`; pass `STAGE_BASE=<last pushed commit>` to see only new ones.
- Emulator tests: one class `run_emulator_test_class.sh <fqcn> [serial]`, everything `run_emulator_all_tests.sh`, failures `emulator_failures.py`. Start the headless emulator: remove `~/.android/avd/anc-test-api36.avd/*.lock`, then `nohup emulator -avd anc-test-api36 -no-window -no-metrics -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 2048 -cores 2 &`, wait for `sys.boot_completed`, then `set_emulator_360dp.sh emulator-5554`.
- Phone: `install_qa_build_on_device.sh <serial>`; the wireless debugging address `192.168.1.4:PORT` changes, ask the owner for a fresh one. The auto-mode classifier blocks running instrumented tests on the phone; use the emulator.
- Lanes (other models): `lane_build.sh <agy-model> <prompt> <out> <allowed-path-regex>` writes code (scope guard reverts or deletes every changed or NEW file outside the regex, including untracked folders that git reports as one directory, so allow `.../java/android/` style folders explicitly; never run two writing lanes at once, never create files while one runs); `lane_ask.sh alphacode|agy:<model> <prompt> <out> [files]` is read only (it sees only the files you pass). Models used: Hydrogen (`gemini-4-hydrogen-xhigh`/`-high`) builds, Flash (`gemini-3.8-flash-high`) writes tests, alphacode reviews. agy can hang for an hour on a TLS or network error and on very large outputs: split big files into fragments, probe with a one-line prompt first (`probe_lanes.sh`).
- macOS: no `setsid`, no GNU `timeout`, use `sed -i ''`; zsh does not word-split variables and errors on unmatched globs (use a shell function or bash).
- `lintDebug` rejects `LocalContext.current.getString(...)`: use `LocalResources.current`.
- Project state is the only progress tracker: after any finished task run `project_state.py set|log`, then `render`, commit, and sync the artifact.
