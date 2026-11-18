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
- Owner tried print v2 on the phone on 2026-11-19: the picture picker works, Share PDF works and the PDF is good, but the Print button shows "Printing is not available right now" (task rd47). Other findings are tasks rd48 to rd56 below. The headless emulator WebView renderer crashes, so `PrintHandoffTest` skips there: print can only be proven on the phone (ask the owner for a fresh wireless debugging address, install with `install_qa_build_on_device.sh`, capture `adb logcat` while the owner taps Print).

## 5. Open work (details are tasks in the roadmap)
### 5a. Owner-reported issues, 2026-11-19 (suggested order; each has a study with root cause, fix steps and tests in `docs/qa/study/STUDY_*.md`)
1. **rd47 BUG Print says "not available" on the phone.** Add `Log.w` with the throwable to every `onUnavailable` path in `PatientPrintManager.kt`, reproduce on the phone with logcat, fix (keep the WebView alive until the print adapter is done; fallback: print the exported PDF file). Study: STUDY_print.md.
2. **rd48 PDF patient strip**: add a "Patient" tag before the name, fix the alignment of age, blood group and risk. **rd49 PDF labs**: compact tables (auto layout, tight padding, nowrap dates and values), lab notes inline in the saved space instead of under the table. **rd50 PDF Arabic font**: `app/src/main/assets/fonts/ArslanWessamB.ttf` was supplied for the doctor name and title but no code references it; add `@font-face url(fonts/ArslanWessamB.ttf)` (HTML base URL is `file:///android_asset/`) with a fallback. All three edit the protected `PatientPrintHtmlGenerator.kt`: owner sign-off per task (the owner asked for these fixes). Study: STUDY_pdflayout.md.
3. **rd54 Lab values**: urine culture and similar tests accept only numbers; add result kinds (numeric, less-than or more-than with qualifier chips, free text), stored in the existing string column (schema frozen), `labResultFlag` parses `<`, `>`, `<=`, `>=` and gives UNKNOWN for text; same rules in `LabEditor.kt` and `LabBatchScreen.kt`; owner to list which tests are which. Study: STUDY_labvalue.md.
4. **rd52 Labs tab collapsible cards**, **rd53 Labs tab comparison view** (reuse `labReport` in `ui/report/ReportData.kt`, no second grouping rule), **rd51 attach photos or files to a lab result or group** (reuse `DemoAttachment.linkedRecordId`, category LAB, no schema change). Study: STUDY_labstab.md.
5. **rd55 DECISION, blood bank note.** Verified: the app has NO general decision engine, only fixed rules (BP alert thresholds 140/90 and 160/110 via `bpLevel`, severe BP attention, Rh-negative tag, dating discordance and redating hints, lab abnormal flags from reference ranges, delivery date validation, GA and EDD calculation; risk level and clinical attention are typed by the clinician). The "Blood bank precautions: Anti-D Prophylaxis + Matched Packed RBCs Cross-match" row is one hard-coded rule in `reportDeliveryRows` (since 2026-10-27): every Rh-negative blood group, in app, print and share, not editable, ignores antibodies, sensitisation and delivery already done. Needs the owner's decision (remove, editable per patient, or conditioned). Study: STUDY_rules.md.
6. **rd56 DECISION, Overview without active medications and labs.** It is intended: `docs/designs/redesign/PLAN.md` section 6 item 1 ("Identity and Medications are not added to Overview: accepted") and the Overview note "No cross-tab content" (labs and medications keep their own tabs). No change unless the owner reverses it. Study: STUDY_overview.md.
7. rd44 to rd46 (earlier): finish the phone check of Share text, confirm the `reports/` cache path, confirm what print layout B3 leaves out.

### 5b. Older open work
1. Owner approval still owed for the `reports/` cache path in `file_paths.xml`.
2. rd33 Meds stage 2 (edit the medication list in settings like lab tests), rd34 delivery follow-ups go into the postpartum note, rd35 deep clean, rd36 frontier review of the registration merge, rd37 and rd38 small accessibility items, rd42 refactor follow-ups.
3. Print follow-ups chosen by the owner: visit findings text and "Not recorded" for missing scan measurements are not printed in layout B3; medications in the short share text are names only.
4. Older roadmap items that may already be fixed by the redesign are marked "needs a look on the phone" (u06, u09, u11).
5. Go-live: signed release build, backup first, never uninstall the clinic app (see the checklist).

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
