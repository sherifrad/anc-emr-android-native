# Roadmap (generated from docs/PROJECT_STATE.json; do not edit by hand)

Updated 2026-11-18. 131 of 186 tasks done.

Working on now: Large font: filter chip clipped; header subtitle truncated; Phone testing: Block A and B run by the agy lane (self-reported PASS); Block C, D and two hand checks still need you; Remove duplicates found in the audit (removal only); Go-live checklist: draft written, needs your review; Arabic-name cards mirror the whole card; chips touch; Backup, restore, camera and print-to-PDF end to end on the phone; Screen-capture block removed in release builds (done); release-build device check still open; Medication screen: multi-drug 'Today's list', one-tap add, Stop (next; two statuses only); Light theme: high-risk card frame; Workspace opens with first tab cut; Device pass 2: PIN lock, TalkBack, Keystore, missing-pointer recovery; Backups lose data: ultrasound EDD merged, attachments missing; Decode photos off the main thread; respect camera rotation; Printed report does not show the blood-pressure alert; Should a severe blood pressure also raise the patient's attention banner or list badge?; Export and print v2 (print crash fix, new layout)

## Whole-app redesign (current work) (28/36)

22 slices on branch redesign-2026-11-15, mock first, owner approves, lane models build, review and test. Plan: docs/designs/redesign/PLAN.md. Nothing is pushed until the owner says so.

| Id | Status | Task | Ref |
|---|---|---|---|
| rd00 | done | Slice 0: Foundation: tokens and shared components |  |
| rd01 | done | Slice 1: Overview sections and dashboard card |  |
| rd02 | done | Slice 2: Delivery planning card and delivered state |  |
| rd03 | done | Slice 3: Clinical attention card, header strip and dialog |  |
| rd04 | done | Slice 4: Patient hub header and tab bar |  |
| rd05 | done | Slice 5: Patient list chrome and actions sheet |  |
| rd06 | done | Slice 6: Visits tab band cards |  |
| rd07 | done | Slice 7: Scans tab band cards |  |
| rd08 | done | Slice 8: Labs tab band cards |  |
| rd09 | done | Slice 9: Meds tab tune | 7b6bd67 |
| rd10 | done | Slice 10: Plan tab and attachments | 9416dab |
| rd11 | done | Slice 11: Editor sheets (medication, delivery plan) | 928b439 |
| rd12 | done | Slice 12: Registration flow and patient editor | dbeab1c |
| rd13 | done | Slice 13: History, previous pregnancy and dating editors | 91ec511 |
| rd14 | done | Slice 14: Visit editor | 89e4890 |
| rd15 | done | Slice 15: Ultrasound editor | 937be9f |
| rd16 | done | Slice 16: Lab editor and lab batch | 3bc4888 |
| rd17 | done | Slice 17: Catalog settings | slice 17 |
| rd18 | done | Slice 18: Security, lock and vault | slice 18 |
| rd19 | done | Slice 19: Shell and shared dialogs | slice 19 |
| rd20 | done | Slice 20: Report dialog | slice 20 |
| rd21 | done | Slice 21: Cleanup of unused code | slice 21 |
| rd30 | done | Full emulator push gate on the redesign branch (about 85 min) |  |
| rd31 | done | Merge and push the redesign |  |
| rd32 | active | Export and print v2 (print crash fix, new layout) |  |
| rd33 | todo | Meds stage 2 |  |
| rd34 | todo | Delivery outcome follow-ups |  |
| rd35 | todo | Deep clean: antislop, duplicates, unused code |  |
| rd36 | todo | Frontier review of the registration merge |  |
| rd37 | todo | MedicationStatusBadge: 12sp text and 10dp radius |  |
| rd38 | todo | Delivery sheet Switch has no accessible label |  |
| rd39 | done | Documentation cleanup and single source of truth |  |
| rd40 | done | Report attestation doctor name and clinic phone are hardcoded (print sheet and dialog); drive from clinic settings |  |
| rd41 | done | Lint errors to fix: LocalContext resource queries in LabBatchScreen and SecuritySettingsDialog (slices 16, 18), BidiSpoofing in PatientHubHeader |  |
| rd42 | todo | Cleanup follow-ups (refactors, not deletions): replace raw FilterChip with RecordFilterChip in attachment, delivery and medication forms; PatientNameText for raw name Text; extract shared patient banner, bottom-sheet shell and form dialog; remove dead useRecordStyle=false branches; 5 unreferenced launcher foreground PNGs need packaging proof |  |
| rd43 | done | Emulator environment: LabEditorRedesignTest.labBatchScreen_enterAndReviewFlow_markEmptyNormal fails on the freshly restarted emulator (also on the slice 19 commit where it passed before); find the emulator setting that differs before the push gate |  |

## Working rules for every round (4/4)

A round is logic OR design, never both. One function per rule, defined once and called everywhere. Shared screen pieces live in one design-system file. Every change under app/src needs a stronger-model review and an independent test review. Full plan: .agents/pipeline/PLAN_DESIGN_VS_LOGIC.md

| Id | Status | Task | Ref |
|---|---|---|---|
| x01 | done | Rules agreed: logic and design are separate rounds; no duplicate functions | plan 2026-11-01 |
| x02 | done | Handoff for Antigravity and progress log | commit c7ce850 |
| g08 | done | Handoff for agy and progress log updated for the delete/Undo plan (done) | commit 4d8f0fe |
| x03 | done | Handoff to Gemini: report, start prompt and fixed redesign specs written | 2026-11-02 |

## Logic and functions (no visual change) (8/19)

Model, state and storage code with unit tests written first. No layout, colour or wording changes. Screens only call the new function.

| Id | Status | Task | Ref |
|---|---|---|---|
| l01 | active | Remove duplicates found in the audit (removal only) | plan section D |
| l02 | done | Lab panel queue survives screen rotation | lab entry review |
| w01 | done | Dating logic: changing LMP or IVF dates must change the gestational age | your phone feedback 2026-11-01 |
| w02 | done | New patient: choose how the pregnancy is dated, using the same dating rule (after the dating logic) | your phone feedback 2026-11-01 |
| w03 | todo | Alert notes: one rule splits them into lines and counts '+ N more' (the card layout is a design task) | your phone feedback 2026-11-01 |
| w12 | todo | Hysterotomy deliveries counted as cesarean, with the difference shown | your decision D17 2026-11-01 |
| l03 | done | Edit medications in settings (name, dose, route, frequency), like lab tests | plan L7 |
| l04 | blocked | Print title 'Active Medications': rename to 'Medications'? (waiting for your yes or no) | owner decision |
| w04 | blocked | Delivery plan: its own data and editor (waiting for your list of fields) | your phone feedback 2026-11-01 |
| l05 | todo | Check that the blood-pressure alert on the print and the severe-BP badge are really finished | visit checks |
| l06 | todo | Custom choices: '+ Add other' values saved for later patients (per list, with Undo) | owner 2026-11-01 |
| l07 | todo | Text size preference: five steps 90-130 percent, persisted, bounded | owner 2026-11-01 |
| l08 | todo | Visit entry rules: 'same as last' and 'fill normal' (one function each) and the BP flag | approved d4/d3 |
| g01 | done | Delete and restore with Undo for medications and lab results (done) | 19c36fc · e8285ff |
| g02 | done | One shared delete/restore storage layer for every record kind (done) | 9ceef0d · SPEC_LOGIC_R6B.md |
| g03 | done | One shared Undo handler: timer, single retry, safe retry, clear on lock (done) | round r6b2 |
| g06 | todo | Same delete and Undo for ultrasound, previous pregnancies and visits | rounds r6b5a/b/c |
| g07 | todo | Delete and Undo for attachments (files removed for good only after the Undo window) | round r6b6 |
| g09 | done | Reuse kit for new deletable sections: guide, wiring check, registry test (done) | 7f26beb |

## Graphic design and UX (no logic change) (15/23)

Mock-up first, you approve, then Compose files only. No model, state or storage changes. Large changes are staged so each step can be checked on your phone.

| Id | Status | Task | Ref |
|---|---|---|---|
| d21 | done | Ultrasound screen: guided findings, shared design-system cards, More section | commit 80e6416 |
| w11 | done | Design system: one shared card, section title, label/value, choice chips and action bar | your phone feedback 2026-11-01 |
| d10 | done | Apply the design system to lab, ultrasound and medication editors | after the design system |
| d22 | done | Lab screen: one-test card, edit-details, big result buttons, Flag and Note, Add tests | commit 6ef0f9c |
| d11 | done | One risk badge per patient card and compact two-line rows | design proposal 2026-11-01 |
| d23 | done | Approved mock-ups: lab/ultrasound/medication (B), five recommended screens, directory, chart, visit entry | design/d4 and the Entry Mock-ups artifact |
| d12 | blocked | Colour palette: keep Navy / Ivory / Rose / Teal, or switch to slate and teal? (your decision) | design proposal 2026-11-01 |
| d24 | active | Medication screen: multi-drug 'Today's list', one-tap add, Stop (next; two statuses only) | after logic L-2 |
| d14 | todo | Dating screen: the three due dates side by side with 'Use this one' | after the dating logic |
| d25 | done | Text size A- / A+ (90-130 percent, hard limit) and one-line labels at 360 dp, applied to every screen | after logic L-2 |
| d15 | done | Alert card shows the first alert plus '+ N more' | after the alert-notes logic |
| d26 | done | Patient directory and patient chart in the approved style | approved d4/d3 |
| d27 | done | New patient with dating, obstetric history, previous pregnancies, clinical attention entry | approved d4/d2 |
| d28 | done | Visit and vitals entry: guided steps, same as last, fill normal | approved d4/d3 and logic L-3 |
| w09 | todo | Previous pregnancies list: crowded, title and value look the same | your phone feedback 2026-11-01 |
| w10 | todo | Obstetric history screen: less crowded, two switchable designs | your phone feedback 2026-11-01 |
| d16 | done | The same entry style for visit, dating, other histories, patient form and delivery editor | owner decision D20 |
| d17 | blocked | Lab on-screen number keypad: build it or keep the phone's number keyboard? (your decision) | lab mock-up B |
| d13 | blocked | Navigation redesign (merge Overview and Workspace into one chart, '+' button, no card icons): mock-up only, after the phone check | design proposal 2026-11-01 |
| d18 | blocked | Ultrasound: biometry steppers and numbered collapsible sections (steppers need your reference values) | ultrasound mock-up |
| g04 | done | Shared delete UI: confirm dialog, Undo bar, delete icon (done) | round r6b3 |
| g05 | done | Delete and Undo wired into medications and lab results (done) | round r6b4 |
| g11 | done | Medication section simplified: name, dose, route, Active/Stopped, one note (done) | commits 2fc5e1e, 4761f3f, e6642ca, 86f5fb9 |

## Clinical correctness (19/23)

What the app shows and prints must match what was recorded. Nothing invented, nothing silently changed.

| Id | Status | Task | Ref |
|---|---|---|---|
| c01 | done | Delivery card reads the notes correctly | R1 |
| c02 | done | Your decisions D1–D5: Rh text marker, one hospital, no emoji, pencils | R2 |
| c03 | done | Hospital box separates previous and current deliveries | R2b–R2e |
| c04 | done | Printed report uses the same Rh, risk and age rules as the screens | R2e–R2f |
| c05 | done | Printed report never invents findings | R3a · audit #1 |
| c06 | done | Saving the profile never overwrites an ultrasound or IVF due date | R3a · audit #2 |
| c07 | done | Visit checks: no future dates, read BP and weight, hypertension alerts | audit |
| a08 | done | Device (instrumented) tests no longer compile | 3l review |
| c08 | done | Dating gaps: IVF day-6 embryos, USS vs LMP redating | audit |
| a09 | active | Phone testing: Block A and B run by the agy lane (self-reported PASS); Block C, D and two hand checks still need you | devtests |
| c09 | done | Lab, medication and scan saves can say "failed" after saving | audit |
| c10 | done | Real visits are labelled synthetic | audit |
| c11 | done | Tell you when the due-date update after a profile save fails | R3a open |
| a12 | done | Previous caesarean/vaginal counts now use one rule on the list, profile and print | a10 R-4 |
| c12 | done | Printed notes with "<" or "&" are cut off or mangled | audit #13 |
| a13 | done | Gestational-age countdown, term stage and display text now live in the engine only | a10 R-1 R-7 R-12 |
| c13 | done | Printed report: keep or change the fixed Rh-negative and "Special Recommendations" lines? | your decision |
| a14 | done | Date checks differ: visits refuse future dates, scans, labs and medications do not | a10 R-3 |
| c14 | todo | LMP cycle length adjustment | c08 |
| c15 | done | Printed report: cut lists now say 'and N more not shown' (done) | c12 |
| c16 | active | Printed report does not show the blood-pressure alert | 3m review |
| c20 | active | Should a severe blood pressure also raise the patient's attention banner or list badge? | 3m review |
| w05 | done | Lab panels give wrong tests (preeclampsia panel gave only TSH) | your phone feedback 2026-11-01 |

## Data safety and security (25/31)

No record can be lost, locked away, or read by someone who picks up the phone.

| Id | Status | Task | Ref |
|---|---|---|---|
| s01 | done | Demo patients appear only in the QA copy | R3a · D8 · audit #3 |
| s02 | done | One odd value no longer locks you out of every record | R3b · audit #4 |
| s03 | done | No endless automatic retry when the vault is blocked | R3b · audit #4 |
| s04 | done | The app never deletes a vault that holds a database | R3b · audit #5 |
| s05 | done | App lock: PIN on every start and after 10 minutes away | R3c · D6 · audit #7 |
| s06 | done | Your own PIN on first launch, growing wait after wrong tries | R3c · D7 · audit #7 |
| s07 | done | Android 8–10 crash when opening Security Settings | R3c · audit #10 |
| s08 | todo | Photo encryption and photos in backups (paused; storage plan first) | audit #8 |
| s09 | done | The app lock now closes the vault | audit #7 |
| s10 | active | Backups lose data: ultrasound EDD merged, attachments missing | audit #6 |
| s11 | done | Ultrasound photos disappear; camera-denied crash | audit #9 · R3f-2 · R3g |
| s23 | done | Backup Restore always failed (wrong table name) - fixed | audit #6 |
| s12 | done | Attachment add and delete fail without telling you | audit #11 · R3f-1 |
| s13 | todo | Restore or export racing with lock shows "unlocked" on a closed vault | audit #12 |
| s14 | todo | Show records with odd values so you can correct them | R3b open |
| s15 | todo | Stronger backup password protection (600k PBKDF2 iterations) | audit |
| s16 | todo | Audit trail: who changed what, and when | audit |
| c17 | done | Vault setup cancelled midway: clean rollback (done and tested) | d06 test 15 |
| s17 | done | Round 3d: backup export works, timeout picker removed, no scripts in the report, weak PIN must be re-entered after a restart, honest labels, restore asks first | R3d · audit checkpoint 1 |
| c18 | done | Lock button inside the visit editor: decided NO | d06 |
| s18 | done | Competitive audit of all rounds so far (security, clinical, concurrency, UI/UX) | audit checkpoint 1 |
| c19 | done | Draft notice no longer shown: replaced by the draft recovery feature (f01-f06) | d06 |
| s19 | done | Biometric switch is ignored: with fingerprint off, the fingerprint key still unlocks | audit checkpoint 1 |
| s20 | done | Round 3e: vault closes with the lock, fingerprint switch, no fake scan screen, contrast, TalkBack, 48 dp buttons | R3e |
| f01 | done | Draft recovery: all three steps built and committed locally (hand test on the phone still to do) | D9 |
| s21 | done | Round 3f-1: attachment errors shown, one helper per rule, bounded photo decode, real PIN and vault-guard tests | R3f-1 |
| f02 | done | Draft store inside the encrypted database (done) | f01 |
| s22 | done | In-app camera, photo size presets, upright photos, scan photos in the vault | R3f-2 · R3f-3 · R3g · R3h |
| f03 | done | Auto-save drafts from every entry screen (done; the Continue / Discard alert is the next task) | f01 |
| f04 | done | 'Unsaved draft found' alert: Continue or Discard and start fresh (done) | f01 |
| f05 | done | Draft recovery tests done (hand pass on the phone still on the postponed list) | f01 |

## Device testing (6/8)

Checks on the Honor 400 and on emulators that unit tests cannot cover.

| Id | Status | Task | Ref |
|---|---|---|---|
| d20 | done | You: check the redone lab, ultrasound and medication screens on your phone | lab/ultrasound/medication rounds |
| d01 | done | Device test pass 1 on Honor 400 and emulators | D5 |
| d02 | done | Release build starts with an empty vault | R3a |
| d03 | active | Device pass 2: PIN lock, TalkBack, Keystore, missing-pointer recovery | after R3c |
| d04 | done | Run the instrumented test suite on an emulator | androidTest |
| d05 | active | Backup, restore, camera and print-to-PDF end to end on the phone |  |
| d06 | done | Instrumented tests: 56 of 56 pass on the emulator (twice in a row) | d04 |
| w13 | done | Parked until full device testing: settings dialog emulator test fails | D23 |

## UI and UX polish (16/21)

Findings from the device test: touch targets, labels, layout, themes, transitions.

| Id | Status | Task | Ref |
|---|---|---|---|
| u01 | done | Emoji removed, pencil edit buttons, clear correction button | R2 |
| u02 | done | Touch targets at least 48 dp | DV-6 |
| u03 | done | Delivery tiles cut their labels ("AGREED CLINICAL") | DV-3 |
| u04 | done | Readable values in Obstetric History ("LiveBirth", "Entered date text") | DV-4 |
| u05 | done | Prior-pregnancy cards: text runs together, everything in red | DV-5 |
| u06 | active | Arabic-name cards mirror the whole card; chips touch | DV-7 · DV-8 |
| u07 | done | Profile header: risk badge crowds the name | DV-9 |
| u08 | done | "1 photos" and other wording fixes | DV-10 |
| u09 | active | Large font: filter chip clipped; header subtitle truncated | DV-11 · DV-15 |
| u10 | done | Print preview: duplicate headings, low-contrast Print button | DV-12 |
| u11 | active | Light theme: high-risk card frame; Workspace opens with first tab cut | DV-13 · DV-14 |
| u12 | todo | Smooth first open of a patient profile | transitions |
| u13 | done | Scan viewer draws a fake ultrasound screen when a photo fails to load | audit checkpoint 1 |
| u14 | done | High-risk tag contrast is below the readable standard; TalkBack gives no PIN feedback | audit checkpoint 1 |
| u15 | done | UI cluster 1: GA card layout, plurals, readable attachment titles, no emoji labels | R3j |
| u16 | done | UI cluster 2: tile labels, chip gaps, prior-pregnancy summary, one weight unit, no inner frame, full header subtitle | R3k |
| u17 | todo | More UI/UX items found on the phone | UIUX_BACKLOG N-4 N-6 N-7 N-8 N-9 |
| u18 | done | Printed report: broken words and dates fixed, stamp line once (done; phone check pending) | UIUX_BACKLOG P-1 P-2 |
| w06 | done | Laboratory entry redesign: compact rows, no wasted space, no 'Codex' wording | your phone feedback 2026-11-01 |
| w07 | done | Ultrasound entry redesign: one fast entry style for every field | your phone feedback 2026-11-01 |
| w08 | done | Medication entry redesign: fast and light to add a drug | your phone feedback 2026-11-01 |

## Performance and architecture (7/13)

Keeps the app fast with thousands of records and easy to change safely.

| Id | Status | Task | Ref |
|---|---|---|---|
| a01 | active | Decode photos off the main thread; respect camera rotation | audit · R3f-1 · R3f-3 |
| a02 | todo | Stop reloading the whole database after every save | audit |
| a03 | todo | Split the 2,100-line screens; one shared GA calculator | audit |
| a04 | todo | Rename Demo* models and the "ANC Native Demo" project | audit |
| a05 | done | One due-date rule and one date parser, reused everywhere (your reuse principle) | R3i |
| a06 | done | Model fleet reorganised and the fallback fixed | fleet |
| a07 | todo | Load the clinical catalog off the main thread (low priority) | concurrency audit |
| a10 | done | Reuse sweep: find rules written twice | reuse rule |
| a11 | done | MAJOR (future): full duplicate check across design, functions and the whole codebase | owner 2026-10-31 |
| t01 | done | Team tooling changes not committed (needs your OK) | tooling |
| a15 | done | Smaller duplicates from the reuse audit: all 7 done | a10 R-2 R-5 R-6 R-8 R-9 R-10 R-11 |
| g10 | todo | Review gap: frontier + independent test review of 33e63cd and the medication redo commits | AGENTS.md level-3 rule |
| w14 | done | Review gap: independent test review and frontier-tier review for the ultrasound and lab settings commits | AGENTS.md level-3 rule |

## Release to production (3/8)

Build, sign and roll out without disturbing the records already on your phone.

| Id | Status | Task | Ref |
|---|---|---|---|
| r01 | done | Lint errors fixed (17 to 0); unused-resource cleanup still optional | lint |
| r02 | todo | Shrink the release (R8 on, 55 MB today); remove debug logging | audit |
| r03 | done | English only: Arabic resources removed and README fixed (done) | your decision |
| r04 | todo | Release signing, version numbers, safe upgrade over the current clinic install | release |
| r05 | todo | Final regression: unit, instrumented and device pass | release |
| r06 | done | Merge maca-round2 into main and push | you |
| r07 | active | Go-live checklist: draft written, needs your review | release |
| r08 | active | Screen-capture block removed in release builds (done); release-build device check still open | 3l/d06 review |

## Recent log

- 2026-11-18: Print v2 step 1: crash fix built, unit tested, reviewed (local commit, not pushed).
- 2026-11-18: Roadmap cleanup 2026-11-18: 16 tasks closed (covered by the redesign, or duplicates merged), 3 marked for a phone check, rd34 reworded per owner.
- 2026-11-18: Redesign merged and pushed: origin/main 4b905d1 (slices 0 to 21, polish, rd40).
- 2026-11-18: Polish batch and rd40 done (attestation from clinic settings). Final gate and merge next.
- 2026-11-18: Pre-push phone test: 18 pass, 0 fail, logcat clean (docs/qa/prepush/REPORT.md); 5 owner observations.
- 2026-11-18: QA copy (com.sherifradwan.anc.qa) with slices 14 to 21 installed on the phone at 192.168.1.4:43135.
- 2026-11-18: Push gate run: 931 of 933 emulator tests passed, the 2 failures were keyboard-sensitive tests; fixed (Meds date-field keyboard test updated for picker-only dates, lab batch tests made keyboard-proof) and re-run as classes, both green. Full gate to be repeated right before merge.
- 2026-11-17: Lint errors fixed (rd41). Emulator restarted; one lab batch test fails on the fresh emulator at older commits too (rd43).
- 2026-11-17: Slice 21 cleanup done by two competing teams: batches 1 to 3b committed, about 1,500 lines of dead code removed; refactor follow-ups logged (rd42), lint errors logged (rd41).
- 2026-11-17: Slice 20 report dialog done: native pages, 19 new emulator tests, review fixes (post-term marker, no-GA and no-EDD rows, fixed subtitle).
- 2026-11-16: Slice 19 shared dialogs done: all date fields picker-only, 35 new emulator tests, frontier review and alphacode clean after fixes.
- 2026-11-16: Slice 18 security, lock and vault done: 36 new emulator tests, SecurityWorkflow and catalog tests green.
- 2026-11-16: Slice 17 catalog settings done: 29 new emulator tests, CustomChoicesStore and SecurityWorkflow green.
- 2026-11-16: Slice 16 (lab editor and batch) built, reviewed, tested, committed locally.
- 2026-11-16: Slice 15 (ultrasound editor and photo viewer) built, reviewed, tested, committed locally.
- 2026-11-16: Slice 14 (visit editor) built, reviewed, tested, committed locally.
- 2026-11-16: Mocks for slices 14, 15, 16 approved by the owner as drawn.
- 2026-11-16: Docs cleanup done locally: 74,284 tracked files down to 1,512; progress now only in docs/PROJECT_STATE.json. Not pushed.
- 2026-11-16: Slices 12 and 13 built, reviewed, tested and committed locally; QA copy installed on the phone.
- 2026-11-16: Documentation cleanup started: single source of truth created.
