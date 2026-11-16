You reconcile a stale project task board against what really happened. Below are: (1) the current board rows (id, phase, status, title, detail) last updated 2026-11-02; (2) the roadmap status sections of the handoff file; (3) git commit subjects from 2026-11-01 to 2026-11-16 (date subject).
Statuses: done, active, next, todo, blocked. For EVERY board row decide the correct status as of 2026-11-16 using only evidence in (2) and (3). If no evidence changes it, keep the status. Output one line per row, tab separated: id<TAB>new_status<TAB>evidence (max 14 words, a commit date/subject keyword or handoff line) . Then, after a blank line, list up to 15 lines "NEW<TAB>phase<TAB>title<TAB>status" for significant completed or open work visible in (2)/(3) that has no board row (the whole-app redesign slices 0..13 done and 14..21 open belong to phase pRD; export/print v2 plan; Meds stage 2; delivery outcome follow-ups; full push gate before merge). No other text.

=== (1) BOARD
c01	p1	done	Delivery card reads the notes correctly	Hospital, fees, plan and postpartum notes are split from free text by tested rules; no text lost.
c02	p1	done	Your decisions D1–D5: Rh text marker, one hospital, no emoji, pencils	"O- (Rh negative)" on every screen; one shared Rh/risk rule on list, profile and workspace.
c03	p1	done	Hospital box separates previous and current deliveries	"Last CS Dar Al Fouad Hospital, plan Cleopatra Hospital" now shows only Cleopatra Hospital.
c04	p1	done	Printed report uses the same Rh, risk and age rules as the screens	"Not high risk" no longer prints High Risk; "29 years" prints "29 yrs".
c05	p1	done	Printed report never invents findings	Missing values print "Not recorded" instead of Cephalic, NKDA, AFI 14 cm, "Uncomplicated" and similar; the planned delivery mode is the patient's own plan.
c06	p1	done	Saving the profile never overwrites an ultrasound or IVF due date	Dating is written only for a new patient or a changed LMP-only date.
c07	p1	done	Visit checks: no future dates, read BP and weight, hypertension alerts	Round 3m, verified on the Honor 400. A visit now refuses a future date, a blood pressure that is not like 120/80 (systolic 60-260, diastolic 30-160, top above bottom) and a weight outside 30-250 kg, each with a message on the field. Blood pressure of 140/90 or above shows 'Raised blood pressure - recheck' and 160/110 or above sh
a08	p1	done	Device (instrumented) tests no longer compile	Fixed: the instrumented test folder compiles again (11 seed calls now pass the demo-patients flag; the removed 'never lock' option in SecurityWorkflowTest now selects 15 minutes). Built by the Gemini lane, diff read by me. The tests have NOT been run on a device (postponed): they need the phone.
c08	p1	done	Dating gaps: IVF day-6 embryos, USS vs LMP redating	Round 3n (unit-tested, two reviews). On the Honor 400: the Day 6 chip is there and wraps cleanly, and the redating hint appears at a scan GA 23 days off and not at 3 days off. Not hand-verified: the due date for a day-6 transfer (covered by unit tests only). IVF embryo days 3, 5 and 6 are accepted (day 6: due date transfer + 260
c09	p1	done	Lab, medication and scan saves can say "failed" after saving	Round 3l: the four clinical saves now roll back completely if the final patient-revision step fails, so a record can no longer be kept while the app says 'failed'. Honest note: inside one database transaction this step cannot fail after the earlier checks pass, so this is a safety net, not a bug you could have hit. Not proven by
a09	p1	active	Phone testing: Block A and B run by the agy lane (self-reported PASS); Block C, D and two hand checks still need you	Run on 2026-10-31 by the Gemini device lane on the QA copy (report: .agents/pipeline/devtest/REPORT.md). It reports PASS for: IVF Day 3/5/6 due dates and GA, ultrasound redating hint, raised and severe blood pressure hints, same risk badge on list and profile, singular 'Prior Miscarriage', the five medication frequencies, the dr
c10	p1	done	Real visits are labelled synthetic	Round 3l: visits you enter are saved as real, unit-tested and seen on the Honor 400 (new visit has no 'Synthetic edit' label). The message now reads 'Patient record saved'. Visits saved earlier keep their old flag (no real patients, so nothing to migrate).
c11	p1	done	Tell you when the due-date update after a profile save fails	Round 3l: after a profile save, if the due-date update fails or cannot be confirmed, the notice says 'Patient saved, but the due date was not updated. Open Dating to set it.' Success path seen on the Honor 400 (new patient with a last period saved, normal notice, gestational age 21w 5d correct). The failure message cannot be tri
a12	p1	done	Previous caesarean/vaginal counts now use one rule on the list, profile and print	Done and verified by tests and two reviews; NOT yet seen on the phone. I confirmed the audit's claim by reading the code: the print counted differently from the list and profile. New rule: either structured delivery field counts, a record is never counted twice, free-text notes count only when both fields are empty and only as w
c12	p1	done	Printed notes with "<" or "&" are cut off or mangled	Round 3o, now seen on the Honor 400: a visit note containing < > & prints in full in the preview. All record text goes through one escape function, once. The 'long lists cut at 7' part is c15.
c13	p1	done	Printed report: keep or change the fixed Rh-negative and "Special Recommendations" lines?	Kept as is: I took your 'go on' as accepting my advice that your earlier decisions stand ('Anti-D Prophylaxis + Matched RBCs' and 'Routine Obstetric Hospital Protocol' stay as fixed printed text). Tell me if you want any of the three lines changed.
a13	p1	done	Gestational-age countdown, term stage and display text now live in the engine only	Done, unit-tested (543 tests) and reviewed; NOT yet seen on the phone. The banner and two screens now read late-term, progress, 'W+D wks' text and days to due date from the engine, and the banner test calls production code. Review caught two problems that were fixed before commit: the test lane had copied the formulas into refle
a14	p1	done	Date checks differ: visits refuse future dates, scans, labs and medications do not	Decided D18: scan, lab and medication dates may be in the future; only visit dates refuse the future. Kept as is.
c14	p1	todo	LMP cycle length adjustment	Optional cycle length (days) in the dating editor; LMP due date = LMP + 280 + (cycle - 28). Needs a stored field and a database migration, so it is its own round. Decided 2026-10-31: not now.
c15	p1	done	Printed report: cut lists now say 'and N more not shown' (done)	Done with the layout fix, committed locally (c7f1031). The caps stay at 12 visits, 7 scans, 10 labs and 8 medications, but every cut list now ends with a row 'and N more not shown' with the right count (checked at exactly the cap, one over, and well over). Previous pregnancies were never cut.
c16	p1	active	Printed report does not show the blood-pressure alert	Decided yes (2026-10-31). Round 3q (in progress): the printed visits table shows 'Raised BP - recheck' or 'Severe-range BP - urgent assessment' under the blood pressure value.
c20	p1	active	Should a severe blood pressure also raise the patient's attention banner or list badge?	Decided yes on both (2026-10-31). Round 3q (in progress): when the latest visit with a readable blood pressure is severe (160/110 or above), the patient card on the list shows a 'Severe BP' badge and a line, and the workspace attention banner shows the same line. It is computed, never written into the stored attention note. Rais
w05	p1	done	Lab panels give wrong tests (preeclampsia panel gave only TSH)	Done and committed locally (3993cd6). The cause was not a wrong panel list: the lab form holds one test, and a panel chip only filtered the test chips. Picking a panel now queues all its tests (Test X of Y) and loads the next one after each save. A test checks that every default panel finds all its tests. Not seen on your phone 
s01	p2	done	Demo patients appear only in the QA copy	Verified: a release build starts with an empty vault.
s02	p2	done	One odd value no longer locks you out of every record	Only structural damage blocks the vault; odd values are noted and the patient opens.
s03	p2	done	No endless automatic retry when the vault is blocked	A blocked vault waits for the Retry button.
s04	p2	done	The app never deletes a vault that holds a database	Files and encryption keys are kept whenever records.db exists (three deletion paths guarded).
s05	p2	done	App lock: PIN on every start and after 10 minutes away	Locks at every start and after 10 minutes away. Round 3c-3 fixed the blocker (Settings dialog staying on top of the lock screen). Verified on the Android 8 emulator: Settings open, Home, 2 min away → no lock (correct); 11 min away → only the PIN screen; after the PIN, Settings is closed. The reviewer checked every dialog and pop
s06	p2	done	Your own PIN on first launch, growing wait after wrong tries	Own 6-digit PIN on first launch; weak PINs (111111, 123456) refused; strong salted hash. An old-version 123456 forces a new PIN, even after a restart (bug found and fixed in round 3c-2). 5 wrong PINs give a 30 s wait that survives a restart. Verified on the Android 8 emulator and on the Honor 400 QA copy.
s07	p2	done	Android 8–10 crash when opening Security Settings	Verified on the Android 8 emulator: Settings opens with no crash and no fingerprint option. Android 11+ (your Honor 400) keeps fingerprint unlock.
s08	p2	todo	Photo encryption and photos in backups (paused; storage plan first)	Screenshot hiding is done (round 3d). PAUSED by you: photo encryption. Storage plan written (PHOTO_STORAGE_PLAN.md): encryption adds ~28 bytes per photo, no reformatting; size now controlled by three presets (Small 1280 px, Standard 1600 px, Detailed 1920 px). Photos are not in backups yet. Next: decide migration of existing pho
s09	p2	done	The app lock now closes the vault	Round 3e: when the PIN lock fires, the database closes and stays closed until the PIN is entered, then reopens by itself. Verified on the emulator: open database handles 3 → 0 → 3, list back in under about 2 seconds. An open form is discarded when the lock fires. The key still does not need your phone unlock; that is a separate 
s10	p2	active	Backups lose data: ultrasound EDD merged, attachments missing	Round 3d fixed Export Backup (it could never open the share sheet) and added a confirm step before Restore; verified on the emulator for export. Still to do: photos are not in the backup, and the ultrasound EDD merge. The backup text now says "records only, not photos".
s23	p2	done	Backup Restore always failed (wrong table name) - fixed	Found by the new draft test on the emulator: Restore from backup crashed with 'no such table: medications' because the code deleted from a table that is really called 'medication_records'. So restoring a backup never worked since that code was written (the earlier confirm-step fix did not reach it). Fixed and every other table n
s11	p2	done	Ultrasound photos disappear; camera-denied crash	Done and verified on the Honor 400: the camera is inside the app (permission-denied shows a message, no crash) and new ultrasound scan photos are copied into the vault and open from there. Scans saved before round 3g keep temporary links; you decided no migration is needed because the app holds no real patients yet.
s12	p2	done	Attachment add and delete fail without telling you	Round 3f-1: a failed save or delete shows a banner; the database row is deleted first, then the file; a failed save removes the orphan file. Reviewer also fixed: a refused save (false result) is no longer silent.
s13	p2	todo	Restore or export racing with lock shows "unlocked" on a closed vault	
s14	p2	todo	Show records with odd values so you can correct them	The values are recorded since round 3b; a banner and fix flow are still needed. Also now relevant: since round 3m an old visit with free-text blood pressure or weight (for example 'BP high') cannot be re-saved until corrected, so the fix flow should list these visits.
s15	p2	todo	Stronger backup password protection (600k PBKDF2 iterations)	Needs the web app updated at the same time.
s16	p2	todo	Audit trail: who changed what, and when	Edits overwrite in place today; attachments are hard-deleted.
s17	p2	done	Round 3d: backup export works, timeout picker removed, no scripts in the report, weak PIN must be re-entered after a restart, honest labels, restore asks first	339/339 tests and all checks pass; export and the weak-PIN restart verified on the Android 8 emulator. Not yet run on a device: the restore confirm step, screenshot hiding in the release build, printing with scripts off.
c17	p2	done	Vault setup cancelled midway: clean rollback (done and tested)	Committed locally (5f39c46). The test now proves what you decided: after cancelling, the state is 'not set up', no files or key left behind, and setup can be started again and completes. Emulator: passes.
c18	p2	done	Lock button inside the visit editor: decided NO	Decided 2026-10-31: no Lock button in entry screens; locking stays on the patient list. Follow-up: the old instrumented test that expected a Lock button in the visit editor (AncWorkflowTest reviewedVisitEditorLayouts...) must drop that expectation (task d06).
s18	p2	done	Competitive audit of all rounds so far (security, clinical, concurrency, UI/UX)	Claude security 18 findings, Claude UI 19, GPT-6 Luna 2, Nemotron 10 (2 wrong). The style and test-quality lenses are not covered yet: NVIDIA's free tier did not respond. Reports are in .agents/pipeline/audit1/.
c19	p2	done	Draft notice no longer shown: replaced by the draft recovery feature (f01-f06)	You decided on 2026-10-31 not to just show a warning but to keep drafts and offer them back (D9). See f01-f06.
s19	p2	done	Biometric switch is ignored: with fingerprint off, the fingerprint key still unlocks	Round 3e: the fingerprint key does nothing when the switch is off.
s20	p2	done	Round 3e: vault closes with the lock, fingerprint switch, no fake scan screen, contrast, TalkBack, 48 dp buttons	342/342 tests, all checks pass, release compiles; device-checked on the Android 8 emulator. First live use of the automatic model fallback.
f01	p2	done	Draft recovery: all three steps built and committed locally (hand test on the phone still to do)	Storage (encrypted table, database version 6), auto-save every second and before lock, and the Continue / Discard alert are all done and committed locally. Remaining only: try it by hand on the phone (type, force-stop, reopen) and one end-to-end automated test (f05).
s21	p2	done	Round 3f-1: attachment errors shown, one helper per rule, bounded photo decode, real PIN and vault-guard tests	390/390 tests then. Shared helpers for hashing, photo decode and the vault clean-up guard (no more copies); the PIN lockout, upgrade and wrong-attempt rules and the vault-key guard now have real unit tests; the Rh test that tested nothing and a duplicate print test were removed; the test manifest was repaired.
s22	p2	done	In-app camera, photo size presets, upright photos, scan photos in the vault	CameraX camera inside the app (framed preview, capture, retake, use), so the app is never left and the lock cannot fire behind another app. Presets Small/Standard/Detailed (Ultrasound defaults to Detailed). EXIF rotation fixed in the one decoder. Add Attachment dialog scrolls. New scan photos are copied into the vault; leftover 
f02	p2	done	Draft store inside the encrypted database (done)	Done and committed locally (3fdc884). New encrypted 'drafts' table (one per patient per screen, newest replaces older, 64 KB cap, no link to patients so a brand-new patient works), database moved to version 6 with a migration from 5, save / load / delete functions, cleared on backup restore, not part of backups. Checked: 557 uni
f03	p2	done	Auto-save drafts from every entry screen (done; the Continue / Discard alert is the next task)	Done and committed locally (c8fd5ac). All nine entry screens now save their unsaved text as a draft about one second after typing stops, and once more just before the app locks or goes to the background (waits at most 2 seconds, then locks anyway). The draft is removed when the screen is saved, cancelled or discarded. A lock nev
f04	p2	done	'Unsaved draft found' alert: Continue or Discard and start fresh (done)	Done and committed locally (f5a05d1). Opening any of the nine entry screens with a stored draft shows a modal alert (cannot be dismissed by Back or tapping outside): which screen, how long ago it was started, and a warning if the saved record changed since. Continue puts the text back; Discard and start fresh deletes the draft. 
f05	p2	done	Draft recovery tests done (hand pass on the phone still on the postponed list)	Unit tests, six alert tests, and four database-level persistence tests (save, close and reopen, restore, discard) all pass on the emulator. The old lock-screen policy test was corrected. Remaining: the hand pass on the phone and a decision about the two unused draft-policy strings (they are no longer shown anywhere).
d20	p3	active	You: check the redone lab, ultrasound and medication screens on your phone	The redone lab and ultrasound screens and the simplified medication screens are installed as the QA copy (com.sherifradwan.anc.qa) on the phone and the emulator. Please look at them when you can. Claude cannot unlock the phone.
d01	p3	done	Device test pass 1 on Honor 400 and emulators	15 findings; "(Rh negative)" fits everywhere, even at large font.
d02	p3	done	Release build starts with an empty vault	
d03	p3	active	Device pass 2: PIN lock, TalkBack, Keystore, missing-pointer recovery	Checked on the Honor 400: PIN setup, forced change, weak-PIN refusal, cold-start lock; after 11 minutes away only the PIN screen shows and the open camera dialog is gone after unlock. Not yet done: TalkBack with a real screen reader, fingerprint on Android 11+, the restore confirmation (needs the file picker).
d04	p3	done	Run the instrumented test suite on an emulator	Instrumented suite now runs on an emulator: 39 tests, 23 pass, 16 fail (see d06).
d05	p3	active	Backup, restore, camera and print-to-PDF end to end on the phone	Camera end to end is done on the phone: capture, retake, use, small vs detailed photo sizes, scan photo opens from the vault. Still open: backup, restore and print-to-PDF end to end.
d06	p3	done	Instrumented tests: 56 of 56 pass on the emulator (twice in a row)	All green after the decided changes (clean rollback, no Lock button in the visit editor), a corrected settings-dialog test (it asserted while the dialog was still animating) and a corrected patient-identity expectation. The two end-to-end draft tests that could not work in this harness were replaced by four database-level tests 
w13	p3	todo	Parked until full device testing: settings dialog emulator test fails	The emulator test 'settings dialog shows the fixed auto-lock timeout and allows manual lock' fails on the emulator only (it cannot find the title 'Clinic & System Settings' after the wording change). You parked it until the full device testing. Emulator suite is 55 of 56 pass with this one failing; not a regression.
u01	p4	done	Emoji removed, pencil edit buttons, clear correction button	
u02	p4	done	Touch targets at least 48 dp	Round 3e: patient-card buttons are now 48 dp (measured 126 px on the emulator); no overflow. The photo chip height and other screens are still to be measured on the Honor 400.
u03	p4	done	Delivery tiles cut their labels ("AGREED CLINICAL")	Round 3k: tiles read "CURRENT GESTATIONAL AGE" and "AGREED CLINICAL EDD" in full (checked on the Honor 400).
u04	p4	done	Readable values in Obstetric History ("LiveBirth", "Entered date text")	No longer reproduces on the Honor 400: prior pregnancies show "Vaginal" and "Live female infant", not internal names.
u05	p4	done	Prior-pregnancy cards: text runs together, everything in red	Round 3k: the summary line sits above the outcome; the two no longer run together (checked on the phone).
u06	p4	active	Arabic-name cards mirror the whole card; chips touch	Round 3k: chips on Arabic-name cards now keep an 8 dp gap (verified). Still open: the action icons sit in a different place on Arabic and English cards.
u07	p4	todo	Profile header: risk badge crowds the name	
u08	p4	done	"1 photos" and other wording fixes	Round 3j: "1 photo" / "2 photos" (plural resource, 5 places) and the visit weight shows one "kg" (round 3k fixed "74.5 kg kg").
u09	p4	active	Large font: filter chip clipped; header subtitle truncated	Round 3k: the header subtitle shows in full over two lines (verified). Still open: at large font the "Normal Risk (2)" filter chip wraps and clips.
u10	p4	done	Print preview: duplicate headings, low-contrast Print button	Round 3p, seen on the Honor 400: each printed section title appears once and the dialog title 'Clinical Medical Record' is fully visible. The Print PDF button was never a contrast failure (white on rose measures about 6:1).
u11	p4	active	Light theme: high-risk card frame; Workspace opens with first tab cut	Round 3k: the light-theme high-risk card no longer shows a second inner frame (verified). Still open: the workspace opens scrolled so the first tab is cut, and its name repeats as the card title.
u12	p4	todo	Smooth first open of a patient profile	One ~200 ms frame on the first profile open; other transitions measured smooth.
u13	p4	done	Scan viewer draws a fake ultrasound screen when a photo fails to load	Round 3e: a photo that cannot load shows "Image unavailable"; the fake console and the default "Cephalic" are gone. Checked on the emulator.
u14	p4	done	High-risk tag contrast is below the readable standard; TalkBack gives no PIN feedback	Round 3e: High-risk tag text darker (3.95:1 → 5.3:1), every tag checked by the contrast test; PIN dots, errors and keys are labelled for TalkBack. Not yet tried with a real screen reader.
u15	p4	done	UI cluster 1: GA card layout, plurals, readable attachment titles, no emoji labels	The calculated-GA card is text over button (a third of its old height); attachment cards read "Ultrasound Scan · 2026-10-30"; Retake/Gallery buttons have no emoji. Both reviewers caught the builder gaming a wrong check; the check was fixed instead. 433/433 tests.
u16	p4	done	UI cluster 2: tile labels, chip gaps, prior-pregnancy summary, one weight unit, no inner frame, full header subtitle	All verified on the Honor 400. 437/437 tests. Remaining UI items are in UIUX_BACKLOG.md.
u17	p4	todo	More UI/UX items found on the phone	Camera: no flash, zoom or alignment guide. Add Attachment dialog is long and its size chips wrap. The number keyboard stays open and blocks scrolling in the scan editor. The photo viewer shows no size or preset. The GA weeks label wraps unevenly.
u18	p4	done	Printed report: broken words and dates fixed, stamp line once (done; phone check pending)	Done and committed locally (c7f1031). Table cells now break only at spaces; dates, numbers, GA, blood pressure and weight never wrap; the stamp label is printed once. I rendered the generated report in a browser: at A4 width no word or date is split; at phone width the tables keep a readable width and scroll sideways instead of 
w06	p4	done	Laboratory entry redesign: compact rows, no wasted space, no 'Codex' wording	Both parts done and committed locally: lab settings (279f095) and lab entry (3993cd6). Entry: Cancel, Add another and Save are pinned below the scrolling form; unit and range are one light line with an edit button; Normal/High/Low comes from the stored reference range only (text results leave your abnormal switch alone). The on-
w07	p4	done	Ultrasound entry redesign: one fast entry style for every field	Done (your choice B), committed locally (8c06c76). The old impression preset chips are gone; the impression is built from your choices (fetus, presentation, placenta, liquor) with a 'From my choices' button, a live 'Report so far' card, 'Fill normal scan' and 'Clear findings'. Unit-tested and reviewed. Not yet seen on the phone.
w08	p4	done	Medication entry redesign: fast and light to add a drug	Done (your choice B), committed locally (419ccc8). Four statuses (Active, Completed, Discontinued, Patient non-compliant); the Prophylaxis button is removed from the form (the stored field is kept but hidden); profile and print use the new status. Not yet seen on the phone. Open: medication editing in settings (only the wording 
a01	p5	active	Decode photos off the main thread; respect camera rotation	Done: every photo decode is memory-bounded (one shared decoder) and honours the camera rotation (EXIF), so a 12 MP photo can no longer exhaust memory and saved photos are upright. Still open: the viewers decode while drawing, on the main thread.
a02	p5	todo	Stop reloading the whole database after every save	Fine at 50 patients, slow at a few thousand.
a03	p5	todo	Split the 2,100-line screens; one shared GA calculator	
a04	p5	todo	Rename Demo* models and the "ANC Native Demo" project	
a05	p5	done	One due-date rule and one date parser, reused everywhere (your reuse principle)	The 280-day rule was written three times and dates were parsed in nine places with two different strictness rules; now one shared file. The editor's live "Current GA" preview follows the engine (up to 320 days). 428/428 tests; checked on the phone.
a06	p5	done	Model fleet reorganised and the fallback fixed	Providers probed with one-line tool tests; lanes: tests GLM, build DeepSeek, review GLM + Claude, fallback Claude then Big Pickle (LANES.env). GPT-6 Luna is out of quota, Antigravity was rate-limited. The fallback now detects real file changes and no longer waits on a model that never exits. Free opencode quota lasts about one r
a07	p5	todo	Load the clinical catalog off the main thread (low priority)	Of Nemotron's 10 concurrency findings, 9 were wrong or weak; this one is real but measured harmless on the Honor 400 (no skipped frames). Defer until a slow device shows it.
a10	p5	done	Reuse sweep: find rules written twice	Audit done by the Gemini lane (a single model's report: each finding is checked by the fixing round before any change). Full report: .agents/pipeline/a10/AUDIT_REUSE.md. 12 findings (R-1 to R-12) became tasks a12-a15. The recurring, bigger check is a11.
t01	p5	done	Team tooling changes not committed (needs your OK)	Done with your OK: removed the stale index.lock and its twin HEAD.lock (both empty, from Oct 29 02:10, no git running), then committed only lane_fallback.py and the agy relay change in the skill repo (ae8df92). The other pending edits in that repo were not mine and were left alone. Still true: gpt-oss-120b as a reviewer often re
a11	p5	todo	MAJOR (future): full duplicate check across design, functions and the whole codebase	You asked for a future major task: look for duplicates everywhere, not only rules: duplicated screens and components, repeated layouts and colours in the design, repeated functions, copied constants, copies inside tests, and the web app if shared. Run it as a full round of read-only audits (design, code, tests) followed by merge
a15	p5	done	Smaller duplicates from the reuse audit: all 7 done	Done and committed locally (787b77b): one function per rule for the print date, the save-error text, the 'FHR / EFW' line, the medication frequency list, the risk badge, the editor text field and the editor error card (the 'clinical editor shows patient error text' worry was only a naming copy: both cards showed the same texts).
w14	p5	todo	Review gap: independent test review and frontier-tier review for the ultrasound and lab settings commits	The project rule asks a frontier-tier review and an independent test review for edits under app/src. For the ultrasound commit and the lab settings commit (279f095) the reviews were Claude Sonnet and agy Gemini flash only. The OmniRoute review returned a gateway error (502) twice on 2026-11-01. To do: retry OmniRoute when it is 
g10	p5	todo	Review gap: frontier + independent test review of 33e63cd and the medication redo commits	Owed: frontier + independent test review of 33e63cd and of the medication commits 2fc5e1e, 4761f3f, e6642ca, 86f5fb9 (all touch app/src, level 3). Three Opus rounds ran on the earlier delete/Undo commits. Also not covered: repository.execute throwing, narrow-screen rendering of failure messages, system accessibility timeout for 
r01	p6	done	Lint errors fixed (17 to 0); unused-resource cleanup still optional	Done and committed locally (5f39c46). Fingerprint code now checks the Android version (below Android 11 it quietly falls back to the app PIN; your phone is Android 15), the splash-screen theme items moved to the Android 12+ theme folder, and the lock screen reads its texts the proper way. Lint now reports 0 errors (about 100 war
r02	p6	todo	Shrink the release (R8 on, 55 MB today); remove debug logging	
r03	p6	done	English only: Arabic resources removed and README fixed (done)	Arabic strings removed (earlier commit); README no longer promises an Arabic/RTL interface: it now says the app is English only and that Arabic patient names and notes are displayed and printed with correct direction (committed locally, ff88200).
r04	p6	todo	Release signing, version numbers, safe upgrade over the current clinic install	Signing key: found and checked. The key in the project's git-ignored keystore folder is byte-for-byte the same as the backup on the volume 'ePartogram Signing Backup' (identical checksum); passwords were not opened. Still to do: version number and version code, and a safe upgrade test over the installed clinic app (the app has n
r05	p6	todo	Final regression: unit, instrumented and device pass	
r06	p6	todo	Merge maca-round2 into main and push	25 local commits on maca-round2, not pushed (oldest to newest): c0e4917 2ff5051 64bb935 5fd691d 9f43f16 b53b9da 07bf565 587559d 9bd952a 9e87611 c3ffbb0 8a2313c 16e6ffd e419f4d 02688bd ae22f49 0d401e5 d3556e0 8529ab9 b58ec2d a5d8312 b155e39 16076cf 5321c38 2685feb. Check with: git log main..maca-round2 --oneline
r07	p6	active	Go-live checklist: draft written, needs your review	Draft in docs/GO_LIVE_CHECKLIST.md (committed locally): signed build only, export a backup first and keep it off the phone, write down patient and visit counts, install over the old app (never uninstall), check counts and a printed report after, export a fresh backup, what to do if something looks wrong. Open: you choose the ver
r08	p6	active	Screen-capture block removed in release builds (done); release-build device check still open	Done by your decision D15: the screen-capture block is gone from the app (all builds), the device test now expects no block, and the old check was updated. Two older reports in docs/ still described it as on; one was annotated. Still to do on a release build on the phone: no demo data, PIN setup on first start, no debug logging.
d21	pD	done	Ultrasound screen: guided findings, shared design-system cards, More section	One finding open at a time, the next blank opens after each answer, Fill normal scan never fills placenta, photos and visit link under More. 8 emulator tests pass. Not done: frontier-tier review, Clear findings test.
w11	pD	next	Design system: one shared card, section title, label/value, choice chips and action bar	First design round, no behaviour change. Replaces about 25 hand-written copies of the same card and title across the 8 editors, and the two separate Cancel/Save implementations (the old shared row and the new lab bar) with one action bar. Delivered as a mock-up sheet for your approval, then applied to lab, ultrasound and medicat
d10	pD	todo	Apply the design system to lab, ultrasound and medication editors	Pure refactor: same behaviour, same tags, before/after screenshots. The pinned bar from the lab screen becomes the shared action bar.
d22	pD	done	Lab screen: one-test card, edit-details, big result buttons, Flag and Note, Add tests	Design only. My review found and fixed two real defects (saved abnormal flag could be overwritten on open; open/closed sections not reset after Save and add another). 7 new tests, 9 workflow tests and 8 ultrasound tests pass on the emulator. Not seen at 160 percent text; no frontier-tier review yet. Left for a logic round: Revie
d11	pD	todo	One risk badge per patient card and compact two-line rows	From the pasted proposal: only the risk status gets a coloured badge; labels in a small muted style, values stronger; lab and medication rows already follow this, the lists and cards should too. Small labels on tinted strips need a darker grey (the proposed one measures 4.34:1, under the 4.5:1 text standard).
d23	pD	done	Approved mock-ups: lab/ultrasound/medication (B), five recommended screens, directory, chart, visit entry	Non-approved options removed. Sized for about 360 dp (Honor 400), light default, A- / A+ text size limited to 90-130 percent with one-line labels, '+ Add other' on every list, clinical attention entry field. Handoff for Antigravity and progress log written (.agents/pipeline/HANDOFF_AGY.md, PROGRESS_LOG.md).
d24	pD	active	Medication screen: multi-drug 'Today's list', one-tap add, Stop (next; two statuses only)	Part done: the simplified editor, card and print (g11). Still to build: 'Today's list' / one-tap grid / Stop with Undo for adding several drugs (design is Claude's choice, delegated by you); needs the custom-choices logic (l06) first for '+ Add other'. The four-status idea was dropped: only Active and Stopped.
d12	pD	blocked	Colour palette: keep Navy / Ivory / Rose / Teal, or switch to slate and teal? (your decision)	Recommendation: fix the competing badges first, judge the palette afterwards. A palette change is one theme file once the design system exists. The proposal's colours pass the readable-text check except the muted grey on the light tint.
d25	pD	todo	Text size A- / A+ (90-130 percent, hard limit) and one-line labels at 360 dp, applied to every screen	Small-screen rule from the owner: words never cut, never wrapped to two lines; the setting is bounded at both ends; light theme is the default. Needs the text-size preference logic first.
d14	pD	todo	Dating screen: the three due dates side by side with 'Use this one'	Only after the dating logic is done; the screen just shows and calls it.
d26	pD	todo	Patient directory and patient chart in the approved style	Only the risk badge coloured, alert preview two lines with '+N more', one sticky header, tabs, visit timeline with BP and weight on one line.
d15	pD	todo	Alert card shows the first alert plus '+ N more'	Layout only; the splitting and counting rule is the logic task.
d27	pD	todo	New patient with dating, obstetric history, previous pregnancies, clinical attention entry	Approved Recommended designs. Dating uses the existing due-date rule. Clinical attention entry: 2-line field, counter, quick phrases, preview.
w09	pD	todo	Previous pregnancies list: crowded, title and value look the same	The cards show many details and the label and its value beside it look alike. Plan: label in a small light style and the value in a stronger one, fewer lines per card (outcome and year first, details on tap), same on the printed report where it applies. Part of the same design pass as the other redesigns.
d28	pD	todo	Visit and vitals entry: guided steps, same as last, fill normal	Blood pressure of 140/90 or more turns rose and flags but never blocks saving. Fill normal never fills blood pressure or weight.
w10	pD	todo	Obstetric history screen: less crowded, two switchable designs	Confirmed it is the obstetric history screen. You want it less crowded and with less typing; plan: the fleet drafts two designs (for example a compact summary with tap-to-add chips for G/P/A and a card-per-pregnancy design) that you can switch between, you pick the default.
d16	pD	todo	The same entry style for visit, dating, other histories, patient form and delivery editor	One section at a time: mock-ups first, you approve, then build with the design system.
d17	pD	blocked	Lab on-screen number keypad: build it or keep the phone's number keyboard? (your decision)	Not built. Mock-up B had a keypad that stays on screen and moves to the next test by itself; the app now uses the normal number keyboard with the pinned bar.
d13	pD	blocked	Navigation redesign (merge Overview and Workspace into one chart, '+' button, no card icons): mock-up only, after the phone check	This changes how you move through the app, not just its look, and it removes actions from the card (you asked for pencil buttons earlier). Nothing is built until you have checked the current screens on your phone and approved a mock-up.
d18	pD	blocked	Ultrasound: biometry steppers and numbered collapsible sections (steppers need your reference values)	The steppers would start at typical values for the week; those reference values are a clinical decision for you, not for the models.
g04	pD	done	Shared delete UI: confirm dialog, Undo bar, delete icon (done)	One dialog, one Undo bar at app level, one 48 dp delete icon, strings stored once; the notice now says 'Medication deleted/restored' and 'Lab result deleted/restored'. Failures show a message (a failed Undo says the record is still deleted) that survives a patient switch. Seen on your phone: delete with confirm, Undo bar naming 
g05	pD	done	Delete and Undo wired into medications and lab results (done)	Delete icon on every medication and lab card (not on archived patients); Undo bar after 'Save and next' on the next blank medication form; long pre-saved medication lists collapse after 5 with Show all / Show fewer. Emulator UI tests 21 of 21; real-database cases 54. 'Save and next' Undo not tapped on the phone (you said not nee
g11	pD	done	Medication section simplified: name, dose, route, Active/Stopped, one note (done)	Your decisions 2026-11-02: tab reads 'Meds'; editor keeps only name (presaved list), dose, route, Active/Stopped and ONE 'Note / instructions'; end date, side effects, frequency and start date are no longer edited (stored fields kept, new entry starts today); card = bold name, Active/Stopped badge, lighter line 'dose · route · n
l01	pL	active	Remove duplicates found in the audit (removal only)	DONE (commit da90a9a): unused second copy of lab and medication catalogs removed; the lab-name list is now one list in the model; 'Active' is one constant. STILL OPEN: the scan viewer's own 'W+D wks' text (use the engine) and the unused import in the model file; the unused ultrasound impression list was not checked in this round
l02	pL	done	Lab panel queue survives screen rotation	DONE (commit da90a9a): the remaining-tests queue is saved across rotation, with one tested 'next test' rule. Checks, unit/lint/release gate and emulator 55 of 56 (parked test only) pass; both reviewers found no blockers. NOT verified: rotation itself has no automated test. Owner check on the phone: start a lab panel, rotate half
w01	pL	done	Dating logic: changing LMP or IVF dates must change the gestational age	Done in the logic (commit 346425b, local, not pushed). One rule now gives the due date for each method (LMP, IVF, scan); the gestational age and the estimated due date both use it. On the dating draft, changing LMP, IVF transfer date or embryo day, the scan due date, or the method updates the agreed due date, unless you typed th
w02	pL	done	New patient: choose how the pregnancy is dated, using the same dating rule (after the dating logic)	Done with limits: the new-patient data model now supports LMP, IVF, scan and clinical dating through the one due-date rule (commits 346425b, 14a24ff, unit tested). The new-patient screen that asks the dating question one step at a time is a design task (d27, approved mock) and is not built yet.
w03	pL	todo	Alert notes: one rule splits them into lines and counts '+ N more' (the card layout is a design task)	You typed three alerts, one per line (hypertension, hyperthyroidism, severe varicosity). The card at the top of the profile shows only the first line; tapping it shows all three. Plan: the card shows the first alert plus a count such as '+ 2 more' so nothing looks missing; one rule that splits the notes into lines, reused by the
w12	pL	todo	Hysterotomy deliveries counted as cesarean, with the difference shown	Decided: a hysterotomy counts as a cesarean in the totals and the text shows the difference, for example '4 previous CS (3 CS, 1 hysterotomy)', on the patient list, the profile and the printed report, and it is mentioned where the surgical history is shown. Reuses the single previous-delivery function.
l03	pL	todo	Edit medications in settings (name, dose, route, frequency), like lab tests	Same edit pattern as lab tests. The rename/duplicate-name/trim rule is shared with the lab edit, not copied. Only the wording of the medication list was changed so far.
l04	pL	blocked	Print title 'Active Medications': rename to 'Medications'? (waiting for your yes or no)	The printed list now includes completed, discontinued and non-compliant medications, so 'Active Medications' no longer describes it.
w04	pL	blocked	Delivery plan: its own data and editor (waiting for your list of fields)	The pencil in the delivery card opens the patient form because no delivery form exists. Step 1 (you): list the fields you want, next to the ones already stored. Step 2 (logic): one delivery-plan model with checked save, draft recovery and print. Step 3 (design): the editor in the new style.
l06	pL	todo	Custom choices: '+ Add other' values saved for later patients (per list, with Undo)	One store per list in the existing codex/settings store, in backup and restore, one validation function (trim, length, duplicate), rename or remove in Settings. Label only, never patient data. Presentation and other rule-driving findings: custom value is free text and triggers no rule. Must come before the screens that show the 
l05	pL	todo	Check that the blood-pressure alert on the print and the severe-BP badge are really finished	Two older tasks still read 'in progress'. Read the code and tests, then close them or list what is missing.
l07	pL	todo	Text size preference: five steps 90-130 percent, persisted, bounded	One setting applied once at the app root. A+ is not allowed past the largest size where short labels still fit.
l08	pL	todo	Visit entry rules: 'same as last' and 'fill normal' (one function each) and the BP flag	Unit tests first. Fill normal fills only blank oedema, movements and urine, never BP or weight.
g01	pL	done	Delete and restore with Undo for medications and lab results (done)	Delete asks for confirmation, removes the record at once, then an Undo bar for about 8 seconds (afterwards final). 'Save and next' offers Undo that removes only that new medication. Reviewed by Sonnet, agy and Opus; fixes: no phantom Undo, Undo never touches a different record, timer restart once, safe retry after a storage erro
g02	pL	done	One shared delete/restore storage layer for every record kind (done)	Owner request: delete/Undo must be one global mechanism, so what works for medications and lab applies to every other section. A new kind needs one small adapter and one registry line. Boundaries: a command carries exactly the confirmed type, patient and record; a delete or restore cannot touch another section or another patient
g03	pL	done	One shared Undo handler: timer, single retry, safe retry, clear on lock (done)	Built and committed locally (404a35a). One coordinator owns the 8 second timer, one restart, safe retry with the same command, replacing an older offer, and clearing on lock. Reviewed by Opus three times; all real findings fixed (dac3f46, 0b8e380, 0fba1b6, 33e63cd). Lock during the countdown: the delete is already final. Not ver
g06	pL	todo	Same delete and Undo for ultrasound, previous pregnancies and visits	One adapter each plus one test entry each in the real-database test. Dating is edit-only (not deletable).
g07	pL	todo	Delete and Undo for attachments (files removed for good only after the Undo window)	Needs a short written addendum and review first, because files are shredded.
g09	pL	done	Reuse kit for new deletable sections: guide, wiring check, registry test (done)	ADDING_A_DELETABLE_KIND.md lists the 10 steps; check_r6c.py fails if shared code names a kind; a test checks every kind has a full adapter. Repeated commands are now scripts in .agents/scripts (gate, static checks, emulator class run, QA install, screenshot, Opus review).
x01	pR	done	Rules agreed: logic and design are separate rounds; no duplicate functions	Written in .agents/pipeline/PLAN_DESIGN_VS_LOGIC.md. Each round's static check asserts that its rule is defined once and called from the screens; a copy found in review blocks the round.
x02	pR	done	Handoff for Antigravity and progress log	HANDOFF_AGY.md: rules, status, approved design language, small-screen and text-size rules, logic roadmap, pipeline steps, links. PROGRESS_LOG.md: append after every round until Claude returns.
g08	pR	done	Handoff for agy and progress log updated for the delete/Undo plan (done)	HANDOFF_AGY.md section 8 and the status line; PROGRESS_LOG.md entry.
x03	pR	done	Handoff to Gemini: report, start prompt and fixed redesign specs written	HANDOFF_AGY.md status section; .agents/pipeline/design/GEMINI_START_PROMPT.md and REDESIGN_SPECS.md (fixed rules and per-screen specs taken from the approved mock). Claude stops here; Gemini continues.
=== (2) HANDOFF
## B. Missions (status as of 2026-11-11)
| ID | Mission | Status | Next step |
| --- | --- | --- | --- |
| M1 | Meds tab revision, Stage 1: bottom sheet instead of the editor route, one-tap stop with 10 s Undo, collapsed picker, Show past, frequency required for new drugs, route-aware active check, header name at 200 percent text | DONE and pushed (`c9c1a82` to `fa01e98`). Design: `docs/designs/medication_section_revision.md` | Stage 2 list below (M1b) |
| M2 | Plan tab and delivery plan sheet: the Delivery Planning card shows only the four boxes (hospital, fees, plan, postpartum) and the attachments; the pencil opens a sheet that writes labelled lines into the obstetric notes; fetal presentation moved to the Scans tab | CODE DONE and pushed (`7923f8f` to `fa01e98`), **On the phone 2026-11-11 the pencil opens the sheet with the four fields (checked by screenshot); the owner's earlier report was not reproduced. One real defect found there: the Save button always read "Saving…". Fixed (it now reads "Save" until a save runs; test `theSaveButton
| M2b | Delivery outcome: whether the patient has delivered, and the delivery date (owner could not find it, 2026-11-11). Nothing like it exists in the app today: patient status is only active or archived | OWNER APPROVED 2026-11-12 (schema v6 to v7 and the protected files; four columns incl. mode and outcome; delivered patients leave the main dashboard). CODE DONE and PUSHED 2026-11-12 (origin/main 93da37d; push gate: full emulator suite 456 of 456 on the second run, the first run crashed in LabBatchUiTest and that class passed alone; the protected-files check failed only on the files the own
| M3 | Registration flow: the new patient entry form. The header pencil on the patient screen still opens the OLD form (`ui/PatientEditor.kt`) because the new flow is not merged | MERGED ON MAIN AND PUSHED 2026-11-13 (`0127086`); new patients use the new flow. Independent review of the merge still owed (section N1). The pencil for existing patients is mission M3b (section N2). MERGE PLAN: `docs/designs/merge_registration_plan.md` (alphacode draft, corrected by a Claude reviewer; strategy: integration branch in its own worktree, never touch main until green). Waiting on 5 owner questions. Work 
| M4 | Device QA on the phone | Plan `docs/designs/device_qa_plan.md`; driver `.agents/scripts/device_qa_meds.sh` passed 15 of 15 on 2026-11-11 for the Meds tab (phone QA copy) | Add the Plan sheet checks; keep to the phone safety rule in A3 |
| M5 | Git hygiene | DONE: `git_audit.sh`, `docs/BRANCHES.md`, rule in `AGENTS.md`, 11 old unreachable commits kept under `refs/archive/`, main worktree clean | Run the audit at the start of every session |
| M6 | Deep clean of code, directory and app (antislop skills by other models, duplicates, unused code and resources) | **POSTPONED to the end of M2 and M3.** Evidence gathered and committed under `docs/designs/reviews/cleanup/`: 10 compiler warnings, 126 unused resources (lint), 37 duplicate clusters, 172 unused imports (31 removed in `37ff23d`; 107 sit in files the registration branch changes; 6 in protected storage files), 38 review batches (`batches/`) and a few `antislop-code` reports (`reports_code/`, run stopped on purpose) | After M3 is merged: re-run `find_unused.py` and `find_duplica
| M7 | Older roadmap (section 2 and 3 below: design rounds, logic rounds L-1 to L-7, text-size control, custom choices) | UNKNOWN: much of it was overtaken by the patient hub work. Not re-triaged | After M3: go through sections 2 and 3, mark each item done, obsolete or open |
| M8 | Whole-app redesign in slices (Opus guided), branch `redesign-2026-11-15`, plan `docs/designs/redesign/PLAN.md` | Slices 0 to 8 committed locally (foundation, overview, delivery card, clinical attention, hub header, list chrome, visits, scans, labs; band-card style approved by owner 2026-11-15) plus Meds defect fixes (no flash under the sheet, stable row order, compact picker). Lab fixes (Hb to CBC grouping, panel editing) are on local main, not pushed. Builders and reviewers are Claude subagents because agy (Gemini) quota is out; tests by alphacode. Meds tune slice, dashboard card tag s

### M1b. Meds Stage 2 (not started)
Medication settings screen (favourites, variants, the saved list); course length; change history (needs a schema change and clinical sign-off); the "add to my medication list" banner and the preset chips removed with the old editor; frequency in the clinical summary and print output (protected helper `medicationInstructions`); a guard against a second active row through the blank Prescribe form; one shared expand/collapse row component; the picker tile "Active" badge reusing `MedicationStatusBadge`; translated route names; a lazy list for picker tiles; `otherMode` in `MedicationForm` possibly 

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
- 2026-11-15: redesign slices 6 to 8 (visits, scans, labs band cards) and Meds defect fixes committed on `redesign-2026-11-15`; shared RecordBandCard, BandChip, StatusWord, PlainField, RecordListHeader, RecordEmptyCard added; removed stray tracked file `ClinicalCatalogRepository.kt-E`.
- 2026-11-14 (owner fixes MERGED to main 817daa7, owner checked on phone and approved): notes bug, structured clinical attention (level dropdown, strip and Overview card), due date box, pinch zoom, dashboard sort and Archived-only filter. Review: Argon FAIL (8 findings) and antislop gate (6) fixed in round 2; unit green, 11 new emulator tests green; 4 `MedsTabSheetTest` keyboard tests fail on main too (pre-existing). Specs in `docs/designs/reviews/owner_fixes_2026-11-14/`. QUEUED (owner, 2026-11-14): (1) section heading/value styling like Delivery card across Overview, (2) dashboard card white
- 2026-11-14 (export pushed): owner checked the PDF on the phone and approved. origin/main is `3b5d208` (export rework, three fix rounds, audit evidence). Still owed: frontier-tier review of the registration merge and of the pencil change (Gemini 4 Argon is available again and can do it read-only: `agy --mode plan --model gemini-4-argon-high`), delivery outcome follow-ups (section N3), Meds Stage 2 (N4), deep clean (N5).
- 2026-11-14 (export QA install): QA build of main `e7f8aff` installed on the phone QA copy over wireless debugging (QA copy only, nothing tapped). Waiting for the owner to print one PDF from a patient and judge logo, Arabic signature, paging. Push only on the owner's word.
- 2026-11-14 (export rework DONE, merged to main locally, NOT pushed): three fix rounds run as a team. Reviewers: Gemini 4 Argon (frontier, read-only, review and re-review), GLM, DeepSeek. Builders: alphacode (round 1), Gemini 3.8 Flash with edit rights limited to the four files in `ui/report/` (rounds 2 and 3). Test designers: GLM, alphacode, DeepSeek (never the builder of the same round). Main fixes after the reviews: a long record was clipped in print (fixed height plus hidden overflow), now tables split between rows with repeated headers and margins on every sheet; weight unit printed twic
- 2026-11-13 (export rework, IN PROGRESS on branch `export-rework`, main untouched): owner signed off all four steps of `docs/designs/reviews/export_audit/FINDINGS.md`. Built: `ui/report/ReportSections.kt` (one set of rules, 45 tests by DeepSeek), the PDF rewired onto it, `ui/report/PatientShareText.kt` replacing `ClinicalSummaryGenerator`, owner choices (phone 01201720808, no fees, page 3 removed, Anti-D line for Rh negative only, Specialist not Consultant, Arabic signature name and title in the owner-supplied font `assets/fonts/ArslanWessamB.ttf`, full logo mark). Older report tests brought 
- 2026-11-13 (export audit): owner asked to examine the PDF export. Findings and a four-step fix plan in `docs/designs/reviews/export_audit/FINDINGS.md` (reviewers alphacode, GLM, DeepSeek; all agree on the main points: newest visits and scans are cut off, delivered patients still get a gestational age, medium risk prints as Normal, stopped medications under "Active", fixed advice and placeholders, missing clinical attention and other fields). Nothing changed yet: the print and summary generators are protected, waiting for the owner's sign-off and choices.
- 2026-11-13 (pushed): owner approved; origin/main is `5897def` (pencil and chip, audit tests, lane scripts). Next: frontier-tier review of the merge and of the audit triage when Antigravity has quota again (about 2026-11-14 evening for Gemini), then section N3.
- 2026-11-13 (audit): testing-mode audit of the registration merge and the pencil change. New `DeliveryThroughRegistrationFlowTest` (6 tests, by DeepSeek) passes. Reviews by GLM and alphacode both said FAIL, but no finding was confirmed when checked against the code (triage table in `docs/designs/reviews/audit_2026-11-13/REPORT.md`); the triage was done by the orchestrator, who wrote the merge port, so it is not independent. Push gate on `7af7a36`: 1473 unit tests, lint, emulator 470 of 470, protected files unchanged. The owner accepted the triage and approved the push. Frontier-tier review st
- 2026-11-13 (pencil and chip): built by alphacode, reviewed by opencode GLM and DeepSeek, tests by DeepSeek; details in section N2. `propose_excerpt_patch.py` now keeps every fenced block of a lane answer (alphacode fenced each block separately and only the first was kept). opencode lanes return nothing on prompts of about 40 KB or time out; keep review prompts near 20 KB.
- 2026-11-13 (lane probe): `.agents/scripts/probe_lanes.sh` asks every lane one line. Working: alphacode and the opencode free models (GLM `glm-5.3-flash-free`, DeepSeek `deepseek-v4.1-flash-free`, and 9 others; `iceman-free` failed). Out of quota: every agy model. Gemini 3.8 Flash, Gemini 4 Argon and Hydrogen reset in about 32 hours (about 2026-11-14 evening, Cairo); the Claude models and GPT-OSS inside agy reset in about 108 hours (about 2026-11-17). agy lists no model named Carbon. The opencode free quota is shared and small: re-run the probe before each round.
- 2026-11-13 (registration merge pushed): owner checked the merged QA build on the phone and approved. `main` fast-forwarded to `0127086`; gate on main passed (1456 unit tests, lint, emulator 466 of 466). The first gate run reported a pass with zero emulator tests because the emulator was not running; `push_gate.sh` now fails in that case and the suite was re-run for real. Integration worktree and branch removed; tags and the original branch kept. Independent review of the merge still owed.
- 2026-11-12 (merge): registration flow merged on `integration/registration-merge` in the worktree `/Users/sherifgamalradwan/anc-emr-integration` (merge `4208947`, port `c5a616c`). Conflicts resolved by the plan's rules; reducer edits R1 to R9 re-applied on the split state files; flow events added to `startsAnotherAction` and `clearsFrozenPatient`; the obsolete dating test replaced. Independent review NOT done (reviewer hit its weekly limit). Section N written as the runbook for the next model.
- 2026-11-12 (merge plan): registration flow merge plan written and reviewed; no merge attempted.
- 2026-11-12 (pushed): owner approved the push; origin/main is 93da37d.
- 2026-11-12 (end): review fixes committed; QA build installed on the phone.
- 2026-11-12 (later): built M2b in packages (model, storage, state, UI, print). Builders: alphacode proposals applied with `apply_blocks.py` and fixed by Claude where they slipped (null parse crash, wrong imports, a missed mapping). opencode and agy were out of quota. New script `.agents/scripts/propose_excerpt_patch.py`.
- 2026-11-12: owner chose real database fields for delivery outcome. alphacode drafted a plan, Claude corrected it (invalid CHECK, scope creep, backup is manual JSON, fingerprint needs the new fields); `docs/designs/delivery_outcome.md`.
- 2026-11-11 (late): owner asked for delivered yes/no and the delivery date; confirmed absent from the app, added as M2b. Notes rewrite kept, marked low priority.
- 2026-11-11 (late): the Delivery Plan sheet opens on the phone with the four fields. Fixed the Save label that always read "Saving…" and the frozen-patient event lists in `AncViewModel`. `DeliveryPlanSheetTest` 12 of 12 on the emulator.
- 2026-11-11: M1 and M2 code pushed. Owner tested the QA build on the phone and found no way to open the delivery sheet (M2 reopened). Owner approved: drop the six repeated tiles from the delivery card and show fetal presentation on the Scans tab (done); stop the cleanup and postpone it to the end (done); this file becomes the living roadmap.
- 2026-11-11: git audit found yesterday's registration flow work as 3,684 uncommitted lines in a side worktree; committed there as WIP, recorded in `docs/BRANCHES.md`.

---

=== (3) GIT
2026-11-16 redesign slice 13: history, previous pregnancy and dating editors (record styling, human outcome labels with stored codes unchanged, calculated dating tiles, W
2026-11-16 redesign slice 12: registration flow and patient editor (record styling, external labels with accessible names, equal Cancel/Save row, GPA stacks at large font
2026-11-15 redesign slice 11: editor sheets (medication, delivery plan), fixed aligned Delivered toggle, discard dialog; ChoiceGroup label params; delete unused PencilEdi
2026-11-15 redesign slice 10: plan tab and attachments (rows, add dialog, viewer chrome, scroll), shared RecordFilterChip and FormFieldLabel; tests
2026-11-15 docs: mocks s10 s11 approved, delivery toggle requirement
2026-11-15 redesign: Meds tune slice (type tokens, radii, spacing); MedsTuneTest; roadmap
2026-11-15 dashboard card: tags in fixed slots under the name, same x per tag type; tests
2026-11-15 dashboard card: Arabic names start from the right edge; tests
2026-11-15 docs: roadmap and branch list for the redesign
2026-11-15 redesign slice 8: labs band card; tests; separator fix
2026-11-15 redesign slice 7: scans band card, shared RecordListHeader/RecordEmptyCard; tests
2026-11-15 redesign slice 6: visits band card, shared RecordBandCard/BandChip/PlainField/StatusWord; tests
2026-11-15 meds: no flash under the sheet, stable row order, compact picker; tests; band-card mocks approved; remove stray backup file
2026-11-15 docs: owner direction, tune only
2026-11-15 redesign slice 4 round 2 fixes; hold slices 6 to 9 (owner: tune the current design only)
2026-11-15 redesign slice 4: patient hub header (facts row, risk tag, tabs); tests
2026-11-15 tests: list chrome scroll before click
2026-11-15 redesign slice 5 (list chrome) and slices 2-3 round 2 fixes; tests
2026-11-15 redesign slices 2 and 3 (delivery card, clinical attention) and GPA summary line restored on the card; tests
2026-11-15 docs: redesign owner decisions 7 to 11
2026-11-15 docs: redesign owner decisions
2026-11-15 redesign slices 0-1 round 2: review and antislop fixes (LabeledValue stretch, tag flow, contrast tokens, RTL action)
2026-11-14 redesign slice 1: Overview sections and dashboard card on the new record components; tests
2026-11-14 redesign slice 0: record tokens and shared components (no screen change), plan and slice briefs
2026-11-14 lab fixes round 2: HbA1c, RDW, urine culture, CRP grouping; catalog edit dialog hardening; tests
2026-11-14 lab fixes: Hb groups into CBC, urine RBC and FBS misgrouping, edit lab panels and medications, no duplicate on restart
2026-11-14 docs: owner fixes merged; queued fixes and export v2 in the roadmap
2026-11-14 owner fixes round 2: review and antislop findings (viewer heights, attention round trip, edit gate, zoom gesture, archived search, labels)
2026-11-14 owner fixes WIP: notes bug, structured clinical attention, due date box, pinch zoom, archived filter and dashboard sort
2026-11-14 docs: owner fixes specs, briefs and lane outputs (work in progress)
2026-11-14 docs: export v2 brief with the proven print crash cause; queued behind the owner fixes
2026-11-14 docs: export rework pushed; next steps in the roadmap
2026-11-14 docs: export QA build installed on the phone
2026-11-14 docs: export rework merged to main; roadmap and branch list updated
2026-11-14 export rework, fix round 3: printed tables split between rows with repeated headers, margins on every sheet, share text uses the shared ordering and weight rul
2026-11-14 export rework, fix round 2: long records no longer clipped in print, weight unit, one rule for delivery rows, share text gaps, preview refresh, contrast
2026-11-14 export rework: rows order by parsed date so an undated row is never hidden; blank lab result prints Not recorded
2026-11-13 WIP export rework: stamp text contrast, comment cleanup, roadmap state
2026-11-13 WIP export rework: older report tests brought to the new behaviour; frequency never printed; previous pregnancy type and both note fields printed
2026-11-13 WIP export rework: Specialist wording, signature block without the title line, full clinic logo mark on the page
2026-11-13 WIP export rework: signature font and Arabic name and title, free-text risk label, layout and escape tests updated
2026-11-13 WIP export rework: shared report rules, rewired PDF, share text on the same rules
2026-11-13 docs: audit of the PDF export and share text; findings and fix plan
2026-11-13 docs: pencil and chip pushed; next steps in the roadmap
2026-11-13 docs: audit result and gate numbers in the roadmap
2026-11-13 test: audit of the registration merge and pencil change; delivery values survive a save through the flow
2026-11-13 docs: pencil QA build installed on the phone
2026-11-13 feat: pencil and Incomplete chip open the registration flow for an unfinished registration
2026-11-13 scripts: probe_lanes.sh checks every lane with one line; lane state recorded in the roadmap
2026-11-13 docs: registration flow merge landed on main; push gate fails when the emulator suite runs no tests
2026-11-12 docs: merged QA build installed on the phone
2026-11-12 docs: the merged QA build is not on the phone yet
2026-11-12 docs: runbook for the next model (section N) and the merge state in the roadmap
2026-11-12 test: seed helpers give the new patient an age, now required on create
2026-11-12 merge port: registration flow reducer edits re-applied on main's split state files; all source sets compile
2026-11-12 WIP merge: registration flow branch into main, textual conflicts resolved, reducer port still to do (does not build)
2026-11-12 docs: plan the safe merge of the registration flow branch
2026-11-12 docs: record the delivery outcome push in the roadmap
2026-11-12 docs: roadmap after the delivery outcome review and full suite
2026-11-12 fix: check the delivery outcome only when it changed; keep it out of recovered profile drafts
2026-11-12 docs: record the delivery outcome build state in the roadmap
2026-11-12 feat: delivery outcome in print and summary; one source for the mode and result labels
2026-11-12 feat: delivery outcome in the plan sheet, card, header and dashboard; delivered patients leave the main list
2026-11-12 test: draft recovery now carries the four delivery fields
2026-11-12 feat: draft state, validation on save and draft recovery for the delivery outcome
2026-11-12 feat: store delivered flag, date, mode and outcome as real patient columns (schema v7)
2026-11-12 feat: delivery outcome model with the allowed values, validation and days since delivery
2026-11-12 docs: record the owner's approval and decisions for the delivery outcome
2026-11-12 docs: plan the delivered flag and delivery date as real database columns
2026-11-11 docs: record the missing delivery outcome as M2b and mark the notes rewrite low priority
2026-11-11 fix: delivery plan Save button reads Save until a save runs; freeze list covers the plan sheet events
2026-11-11 docs: make the handoff the living roadmap and record the Plan sheet failure on the phone
2026-11-11 chore: remove 31 unused imports in files the registration flow branch does not touch
2026-11-11 chore: record the deliberate branch and add a branch hygiene rule
2026-11-11 chore: clear the main worktree
2026-11-11 docs: save the latest working drafts of the medication brief and design
2026-11-11 wip: save the uncommitted registration flow work found by the git audit on 2026-11-11
2026-11-11 feat(ui): show the fetal presentation next to the placental site on the Scans tab
2026-11-11 refactor(ui): drop the facts the Delivery Plan card repeated from other screens
2026-11-11 feat(ui): edit the delivery plan in a bottom sheet instead of the full patient form
2026-11-11 feat(state): delivery plan sheet state that never changes the route
2026-11-11 feat(model): compose delivery notes from the four boxes and read labelled lines strictly
2026-11-11 refactor: move the delivery notes parser from ui to model
2026-11-11 docs: design for editing the delivery plan in place on the Plan tab
2026-11-11 test: device QA driver passes 15 of 15 on a real phone; log the judge findings in the plan
2026-11-11 test: device QA plan and an adb driver for the Meds tab
2026-11-11 docs: make three comments say only what the code does; add the push gate and antislop review scripts
2026-11-11 fix(ui): close the gaps found by the frontier-tier review
2026-11-11 fix(ui): route-aware already-active check, end-aligned wrapped actions, clipped ripples
2026-11-11 fix(ui): retry the name focus once after Save and add another
2026-11-11 fix(ui): keep the patient name and allergy line whole at 200% text on 360dp
2026-11-11 feat(storage): require a frequency when creating a medication
2026-11-11 docs: start prompt for zcode and the unfinished Stage B attempt for reference
2026-11-11 docs: add Gemini review findings to the zcode handoff
2026-11-11 docs: update zcode handoff after Phase 3 commit
2026-11-11 feat(ui): collapsed medication picker, Show past, compact rows and keyboard fixes
2026-11-11 docs: record dropped editor features for Stage 2 and mark state/ as a risk path
2026-11-11 refactor(state): split AncState.kt into state, events, reducer and domain helper files
2026-11-11 feat(ui): meds tab uses a bottom sheet and one-tap stop instead of the editor route
2026-11-10 feat(ui): add shared medication form and sheet beside the old editor
2026-11-10 feat(state): registration flow helpers, back-gesture order and step text mapping for the flow screens
2026-11-10 fix(state): Next on an untouched identity step runs the save so name and age errors show
2026-11-10 refactor(state): remove the eight unused dating fields from the patient draft
2026-11-10 refactor(state): remove the dead dating-at-registration path and its saved notice
2026-11-10 feat(state): registration flow driver, forced age in the identity step, pregnancy recovery matches its own pregnancy
2026-11-10 docs(design): registration flow driver design, revised after three independent reviews
2026-11-10 feat(model): require patient age on create, amber Incomplete chip, red Moderate risk badge
2026-11-10 feat(ui): split patient form into identity and obstetric pages, dating method dropdown, age rule in flow logic
2026-11-10 feat(state): registration step flow logic and Incomplete chip on the patient directory
2026-11-10 feat(ui): previous-pregnancy form with one outcome and one delivery mode, required place and doctor, default clinician
2026-11-10 fix(ui): show high and moderate risk badge before the patient name on the directory card
2026-11-09 test(infra): repoint stale static checks at the patient hub; 3 remaining failures are real defects
2026-11-09 fix(ui): show severe BP badge on the patient directory and parse lab as-of date through the shared rule
2026-11-09 fix(ui): drop exclamation marks, decorative emoji and em dashes from UI copy
2026-11-09 fix(ui): close the attachment viewer when its record is deleted
2026-11-09 test(ui): rewrite stale workspace suites against the patient hub
2026-11-09 test(infra): whole-suite emulator script and evidence for dating and backup round
2026-11-09 merge: keep agreed and scan EDD separate in backup (local, not pushed)
2026-11-09 fix(storage): keep agreed and scan EDD separate in encrypted backup export and restore
2026-11-09 fix(model): guard extreme dating dates, span limit, and add marker and span tests
2026-11-09 docs(design): round 2 review evidence and final amendments
2026-11-09 fix(ui): route delivery card and print target through shared dating rule, harden scan anchor, drop dead banner
2026-11-09 docs(design): record dating review findings and spec amendments
2026-11-09 feat(ui): dating section rewrite, GA discordance marker, shared EDD rule in print and share
2026-11-09 feat(model): shared dating summary rule and dd/mm/yyyy formatter
2026-11-09 docs(design): clarify dating rule oracles
2026-11-09 docs(design): approve Dating section mock and add spec and brief
2026-11-09 fix(ui): readable dating method label and neutral NKDA in print report
2026-11-09 test(ui): fix tagged-node lookup and overflow check in overview sections test
2026-11-09 feat(ui): replace overview cards with three clinical sections
2026-11-09 feat(ui): shared allergy state and canEdit gate on delivery plan card
2026-11-09 docs(design): publish patient workspace overview tab optimization design document
2026-11-09 feat(ui): implement audited high-density patient directory card and actions modal sheet
2026-11-09 docs(design): publish audited RFC for high-density patient directory card
2026-11-09 fix(ui): eliminate header crowding and patient name truncation on scroll
2026-11-09 fix(ui,a11y): eliminate theme contrast crash, em-dashes, and enforce 48dp touch targets
2026-11-09 fix(ui): apply antislop mobile layout reflow, touch targets, and sticky header fixes
2026-11-09 refactor(ui): eliminate dead monolith, unify canonical badges, and enforce antislop-human WCAG AA
2026-11-09 refactor(ui): content simplification and visual distinction design system
2026-11-09 feat(ui): unify workspace and profile into modular Patient Hub with smooth collapsing header
2026-11-08 chore: elevate app version to 1.0.4 (versionCode 5)
2026-11-08 feat(encounters): modernize bedside vitals grid, add FHR, remove fundal height entry, and pre-eclampsia safety alerts
2026-11-08 fix(labs): consolidate single unified Add Investigation entrypoint across workspace and profile
2026-11-08 fix(ui): unify global patient card and eliminate name truncation in lab/bedside views
2026-11-08 feat(bedside-workspace): collapsing header, composite lab reports, story-like entry & v1.0.3 QA release
2026-11-08 build(version): elevate version to 1.0.2 (versionCode 3) for mobile QA release
2026-11-08 docs(audit): complete MACA full path verification and emulator test certification
2026-11-08 refactor(ui): use semantic design tokens instead of raw hex colors
2026-11-08 test(ui): add WorkspaceScreenDesignTest for 5 fixed bedside tabs and header rhythm (step 6)
2026-11-08 refactor(ui): polish bedside tabs 3, 4, 5 (labs, meds, plan) with unified card styling (step 5)
2026-11-08 refactor(ui): unify encounters tab with filter chips, vitals grid, and inline ultrasound (step 4)
2026-11-08 refactor(ui): unify history tab with dating, obstetric parity, and medical histories (step 3)
2026-11-08 fix(ui): preserve standard ultrasound card and delete test tags on bedside timeline
2026-11-08 refactor(ui): consolidate workspace into 5 bedside tabs with pager sync (step 2)
2026-11-08 refactor(ui): modernize bedside header and clinical attention banner in workspace (step 1)
2026-11-07 refactor(ui): redesign patient chart to match approved mock (d26 phase 2)
2026-11-07 refactor(ui): redesign patient directory to match approved mock (d26 phase 1)
2026-11-07 refactor(ui): streamline single lab editor layout and remove redundant panel queuing
2026-11-07 chore(design): add QA device built screenshot for single lab investigation
2026-11-07 docs(audit): add MACA audit report for single lab redesign
2026-11-07 feat(ui): redesign single lab investigation to match story appearance and drop obsolete presets
2026-11-07 test(lab): add instrumented UI tests for laboratory batch flow
2026-11-07 fix(lab): pass pinnedBottomBar positionally to EditorPage
2026-11-07 feat(lab): align lab batch screen layout, grouping and components with approved mock
2026-11-07 docs(design): one-page spec sheet for laboratory batch screen
2026-11-07 fix(lab): save filled batch results and trim date during validation
2026-11-07 fix(codex): merge built-in default lab items and panels with user custom entries on load
2026-11-07 fix(lab): wire labBatchDraft to active editor session and revision, guard start against dirty drafts
2026-11-07 docs(design): approved direction, mock inventory and the batch gap list
2026-11-07 docs(handoff): consultant report for the independent reviewer
2026-11-07 feat(lab): the batch screen — Add, Enter, Review — over the split catalogue
2026-11-05 docs(handoff): record batch draft persistence and the blocked review
2026-11-05 feat(lab): persist the batch draft so a lock cannot lose entered results
2026-11-05 docs(handoff): record the batch view-model wiring
2026-11-05 feat(lab): wire the batch to the view model
2026-11-05 docs(audit): record the two review rounds and re-record batch evidence
2026-11-05 fix(lab): derive the restore revision from the delete receipt
2026-11-05 docs(review): record the batch review verdict, fixes and deferred items
2026-11-05 fix(lab): close the independent review's blockers on the batch commands
2026-11-05 docs(handoff): record the lab batch draft slice
2026-11-05 feat(lab): in-memory batch draft with tab, cursor and edit events
2026-11-05 docs(handoff): record the one-action batch undo slice
2026-11-05 fix(lab): keep the record kind out of the shared undo layer
2026-11-05 feat(lab): one Undo for a whole batch, executed as a single command
2026-11-05 docs(handoff): record the lab batch storage slice and what remains
2026-11-05 feat(lab): batch save, delete and restore as one transaction and one receipt
2026-11-05 spec(lab): batch entry and one-action batch undo without schema change
2026-11-05 design(lab): batch-flow slices with CBC and urine as element panels
2026-11-05 docs(handoff): record the raw-format-string fix and the design-round blocker
2026-11-05 fix(lab): stop printing the raw "Calculated GA: %1$s" template on lab cards
2026-11-05 docs(print): close P-1 and P-2 with layout measurements
2026-11-05 docs(handoff): refresh the handoff to the closed mission state
2026-11-05 docs(handoff): record the PASS re-review and its closed nits
2026-11-05 fix(attachment): close the three re-review nits
2026-11-05 docs(handoff): record the QA update and the SF5 check on the phone
2026-11-05 docs(review): keep the attachment-cleanup frontier review in history
2026-11-05 docs(handoff): record SF4-SF6 closure and the emulator run
2026-11-05 fix(attachment): cover attachments in both suites, keep the viewer on Cancel, close the checker gaps
2026-11-05 docs(handoff): record the fix for review blockers 1-3 and what stays open
2026-11-05 fix(attachment): never sweep scan photos; keep the payload of an in-flight Undo
2026-11-05 docs(handoff): record the FAILed frontier review and correct the DB-test claim
2026-11-04 docs(handoff): record emulator instrumented runs and the strings round
2026-11-04 fix(strings): share one fallback string between the two photo viewers
2026-11-04 fix(attachment): move the viewer's hardcoded copy into strings.xml
2026-11-04 docs(handoff): record the attachment UX fix and its device retest
2026-11-04 fix(attachment): one confirmation for delete, human label in the dialog and Undo bar
2026-11-04 docs(handoff): record the attachment cleanup device pass on the QA copy
2026-11-04 docs(handoff): record attachment cleanup completion and the repaired checks
2026-11-04 feat(attachment): surface cleanup failures, report every path, sweep orphans on unlock
2026-11-04 feat(attachment): payload cleanup on every undo exit path
2026-11-04 logic(attachments): migrate delete to unified RequestDelete flow + adapterFor fix + test fixtures
2026-11-04 slice2(r6b6): vault idempotency, revision-guarded DAO query, AttachmentDeleteAdapter, registry wiring
2026-11-04 groundwork(r6b6): ATTACHMENT enum, notices, strings, labelResource, DemoAttachment DeletableRecord
2026-11-04 spec(r6b6): attachments delete adapter spec addendum + groundwork (DemoAttachment DeletableRecord, ATTACHMENT enum)
2026-11-03 logic(obstetric): previous pregnancy delete adapter, undo bar, and delete icon button
2026-11-03 logic(prev-pregnancy): previous pregnancy delete adapter, undo bar, and delete icon button
2026-11-03 logic(visit): visit delete adapter, undo bar, and delete icon button
2026-11-03 docs(test): record physical phone deep testing audit on Honor DNY-NX9
2026-11-03 docs(handoff): record completion of ultrasound delete adapter
2026-11-03 logic(ultrasound): ultrasound delete adapter, undo bar, and delete icon button
2026-11-03 docs(handoff): record completion of design round d28
2026-11-03 design(visit): 4-step wizard, segmented progress, value steppers, findings normal fill, review summary (d28)
2026-11-03 docs(handoff): record completion of logic round l08
2026-11-03 logic(visit): visit entry rules, bp flagging, normal fill, and draft persistence (l08)
2026-11-03 scripts: add multi-model review launcher supporting opencode deepseek/glm, codex, agy, claude
2026-11-03 docs: update PROGRESS_LOG and HANDOFF for medication today's list design round
2026-11-03 design(medication): today's list with quick tiles, stop and restart controls
2026-11-03 docs(pipeline): record completion of logic round l06 custom choices store
2026-11-03 feat(codex): logic round l06 custom choices store for + Add other
2026-11-03 docs(pipeline): record medication review fixes, design nits, and emulator verification
2026-11-03 design(medication): cap instructions to 2 lines, single-line action bar button, remove uneditable end date row, separate tab label Meds
2026-11-03 fix(medication): address review findings on medication simplification
2026-11-02 docs(agents): Gemini start prompt and fixed redesign specs from the approved mocks
2026-11-02 design(medication): stop copying hidden frequency from presets; handoff for medication simplification
2026-11-02 design(medication): reduced editor (name, dose, route, Active/Stopped, one note), pinned action bar, instructions line on card and print, Meds tab label, one-l
2026-11-02 check(r6b3): medication card has one shared delete icon, expect 2 icons in WorkspaceScreen
2026-11-02 design(medication): redo medication list, notice card and editor visuals to the approved mock
2026-11-02 chore(agents): reusable scripts for gate, checks, emulator class run, QA install, screenshot, Opus review; update handoff
2026-11-02 fix(delete-undo): keep other patients' failure messages, disable Undo while a delete runs, type-safe delete icon, guard late Undo failures
2026-11-02 docs: progress log for shared delete/Undo UI, review fixes and phone test
2026-11-02 fix(delete-undo): keep the delete icon tag derivation the static check expects
2026-11-02 fix(delete-undo): keep delete/Undo failure messages through a patient switch, refuse Undo while a delete runs, block editing a record whose save is being undon
2026-11-02 refactor(delete-undo): kind-agnostic validation-failed branch, check_r6c wiring check, registry completeness test, guide for adding a deletable kind
2026-11-02 fix(delete-undo): visible delete/Undo failures, patient-scoped Undo bar and notices, running-delete guard, editor-safe Undo of a save, named delete icon
2026-11-02 feat(ui): shared delete confirm dialog, Undo bar and delete icon wired to medications and lab results; real delete/restore notices; collapsible presaved lists
2026-11-02 fix(logic): keep saved notice on Save and add another, rebase open lab draft after Undo, ignore delete while Undo runs
2026-11-02 chore(checks): retire the r6b1 'no state change / factories' rule, r6b2 removes them on purpose
2026-11-02 refactor(logic): one shared Undo coordinator (8 s timer, single restart, same-command retry, clear on lock) replaces per-kind delete/undo code in the ViewModel
2026-11-02 docs: update agy handoff and progress log (global delete/Undo mechanism plan)
2026-11-02 chore(checks): round 6a hash check follows the hashes to DeletableRecords.kt
2026-11-02 refactor(logic): one generic delete/restore command, repository path and rule for medications and lab results; isolation and DB-integrity tests (54 cases)
2026-11-02 docs(logic): scaffold rounds 6b1 and 6b2 (checks, briefs, isolation rules)
2026-11-02 docs(logic): round 6b design for one global delete/confirm/Undo mechanism
2026-11-02 fix(logic): delete/undo review fixes (rebase create draft, frozen retry, single restore, one restart) and real-database delete/restore test (46 cases)
2026-11-02 feat(logic): permanent delete with Undo for medications and lab results, Undo for Save and next (commands, repository, reducer, view model)
2026-11-02 docs(logic): round 6a spec for delete and undo of medications and lab results, checks and briefs
2026-11-02 feat(chart): collapsible medication list (collapsed rows with summary, collapse/expand all above three medications)
2026-11-02 feat(medication): guided dose/route/frequency/start rows, four-status big buttons, shared design-system cards
2026-11-01 docs(design): approved mock-ups (d4), handoff for the next agent, progress log
2026-11-01 feat(lab): one-test card with details, flag and note toggles, big preset buttons, add-tests section; shared BigChoiceButton
2026-11-01 feat(ultrasound): guided findings (one open at a time), shared design-system cards, photos and visit link under More
2026-11-01 chore(release): version 1.0.1 (code 2)
2026-11-01 feat(dating): new-patient form data model supports LMP, IVF, scan and clinical dating through the one due-date rule
2026-11-01 feat(dating): one due-date rule per method; editing LMP, IVF or scan inputs updates the agreed due date unless the doctor typed one
2026-11-01 refactor(logic): remove dead preset copies, one Active constant, one lab-name list, lab panel queue survives rotation
2026-11-01 feat(lab): lab entry has a pinned action bar, panels bring all their tests, result flag from the reference range
2026-11-01 feat(lab): lab tests can be edited in settings (name, range, unit, category); 'Codex' wording removed from the app
2026-11-01 docs(maca): park settings dialog emulator test until device testing (D23)
2026-11-01 feat(ultrasound): impression built from the doctor's choices, old preset chips removed
Do not call any tool. Write the full answer now as plain text.
