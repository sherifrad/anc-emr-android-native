# MACA round 3a scorecard - nothing invented, nothing silently overwritten, no demo patients in the clinic app
Date 2026-10-30. Spec: SPEC_FIX_R3.md (Q19 a-k, Q20, Q21, S1-S4). Audit items #1, #2, #3; owner decision D8. Nothing committed or pushed.
Runs: 20261030-145357 (3a), 20261030-150323 (3a-2, invented-text items the 3a reviewer found).

## Verdict
All gates green. Fresh --rerun-tasks: 286/286 unit tests; static checks check_r3 (S1-S4), U-r2 30/30, P10 6/6; compileReleaseKotlin and
assembleRelease OK. Device (API 26 emulator): release build creates an EMPTY vault (0 patients); QA debug build still has the 4 demo patients.

## Staffing and independence
Tests: Claude Sonnet 5.5 (133 s + 72 s). Build: GPT-6 Luna via opencode, ChatGPT plan (77 s + 33 s). Review: Nemotron 3 Ultra via OmniRoute
-> NVIDIA NIM (298 s + 307 s). Three vendors (Anthropic / OpenAI / NVIDIA); builder never reviews itself. Orchestrator re-verified.
OmniRoute: local gateway, 127.0.0.1 only (found listening on all interfaces at first start; fixed with OMNIROUTE_SERVER_HOST), own
secrets in ~/.omniroute/.env, client key in ~/.omniroute/client.key (never in config or chat).

## Discipline
- Red before green: genuine both times. 3a: 8 print tests + all 7 static sub-checks red on assertions; IntakeDatingRuleTest parked
  (new API) and restored byte-identical. 3a-2: 6 print tests red.
- Scope violations 0. Repairs 0. Tests weakened: only the one Q17 null-risk case, mandated by Q19(e).

## What changed
Printed report: every missing finding prints "Not recorded" (scan presentation/placenta/liquor, allergy, medical/surgical history,
target timing, visit findings, previous-pregnancy details); missing risk prints "Risk not recorded"; "Planned Delivery Mode" shows the
patient's own plan from her notes (was one fixed CS/SVD sentence for everyone); medication notes "-"; scan label without invented
impression. Profile save: new pure rule intakeDatingUpdate - dating is written only for a new patient or a changed LMP-only dating;
ultrasound/IVF dating is never overwritten from the profile form. Demo patients: seeded only when BuildConfig.DEBUG.

## Findings
- Found by the reviewer (Nemotron) in 3a, fixed in 3a-2: "Antenatal course progressing favorably" (visit with no findings),
  "Uncomplicated" (previous pregnancy with no details), "Ongoing as prescribed", "Fetal Growth Surveillance". The orchestrator's spec had
  missed them - the reviewer earned its place.
- Open (later rounds): printed free text is not HTML-escaped (audit #13); a failed dating write after profile save is still silent;
  existing clinic vaults keep any demo patients already seeded (no deletion by design).

## Economy
Free quota used: NVIDIA (review only). Paid-plan use: ChatGPT plan for two short builds. Grok connected but not used (owner: no
benchmarks on free quota). opencode free models (GLM/DeepSeek/Nemotron) still at their limit.

# Round 3b (run 20261030-152104) - one odd value never locks every record; the app never deletes a vault by itself
Verdict: all gates green. Fresh --rerun-tasks 317/317; static checks r3b (S5-S7), r3, U-r2, P10 pass; release compile OK; QA build on the
API 26 emulator opens normally with its 4 demo patients after the vault change.
Staffing: tests Claude Sonnet, build GPT-6 Luna, review Nemotron 3 Ultra (NVIDIA). Grok 4.7-high (Grok Build sign-in) tried as second
reviewer: was reading the code (findings matched Nemotron's) but hit the 25-minute relay timeout before writing its report -> HALT after the gates. Needs a longer timeout; the earlier note that it "looped" was wrong.
Red before green: genuine. The three new-API tests were parked; STATIC-r3b failed on 6 sub-checks; parked files restored byte-identical.
What changed: findBlockingRecordProblems (revision < 1, previous pregnancy linked to another patient, unsupported model version) is the
only data reason to block a vault; vocabulary/GPA/provenance oddities become RecordAnomaly entries returned with the snapshot (UI to show
them is a later round; records are shown as stored). MainActivity no longer auto-retries a blocked vault (Retry button only).
hasVaultDatabase guards cleanupUnpublishedArtifacts and all THREE deleteOrphanAliases call sites (the spec named two; the builder found
the third in cancelAuthentication - correct).
Orchestrator checks: no code crashes on unknown vocabulary values (only the editor dropdown options and save-time validation use the
vocabulary); diffs read in full.
Not verified: Keystore behaviour on a real device; a real "pointer missing + database present" device scenario (needs file surgery on
the QA vault - planned for the device pass).
Open: show anomalies to the clinician (banner + fix flow) - UI round.

# Round 3c (runs 20261030-160322 .. 162108) - app lock, own PIN, Android 8-10 crash
Verdict: gates green on the final tree. Fresh --rerun-tasks 339/339; check_r3c (S8-S13), r3b, r3 pass; release builds.
Staffing: tests Claude Sonnet 5.5, build GPT-6 Luna, review Nemotron 3 Ultra (NVIDIA). Scope violations 0.
Halts (both my spec errors, both caught by the guards): tests lane could not register the gate ids (script needed approval) -> resume with a
direct-edit note; S13 was not scoped to a file so it flagged legitimate SHA-256 uses -> spec amended, check fixed.
Review value: found that the old default PIN kept working after a restart (migration dropped the legacy key). Fixed; a further pass (3c-2,
run from a second session in the same tree) made any weak stored PIN force a new PIN, persisted across restarts.
Device (Honor 400 QA copy, not the clinic app): old default PIN -> "Set a security PIN", still forced after a force-stop; weak PIN rejected
with a message; set + confirm unlocks; cold start asks for the PIN; 5 wrong PINs -> "Try again in 0:28". API 26 emulator: PIN setup, Settings
opens, no crash, no fingerprint button.
Not verified: biometric prompt on Android 11+; the legacy-PIN repository logic has no unit test.
Open: system Back on the lock screen exits the app (nit); lockout text for 1 h+ (not reachable).

# Round 3c-3 (run 20261030-164151) - no window above the lock screen
Why: device test of 3c found the Security Settings dialog stayed on top of the PIN screen after 10+ min away (Hard Blocker for D6).
Verdict: all gates green. Fresh --rerun-tasks 339/339; check_r3c S8-S16, r3b, r3 pass.
Staffing: tests Claude Sonnet 5.5 (33 s), build GPT-6 Luna (34 s), review Nemotron 3 Ultra (386 s). Scope: AncApp.kt + check_r3c.py only; violations 0.
Red before green: pipeline red step reused a cached result, so the orchestrator ran S16 against the pre-fix snapshot by hand: FAIL
(dialog not guarded; no reset effect). Passes on the fixed tree.
What changed: SecuritySettingsDialog shown only when !isAppLocked and no PIN setup is needed; a LaunchedEffect on the lock state closes it;
the two AlertDialogs also gain needsPinSetup != true.
Review: swept every Dialog/AlertDialog/DropdownMenu/Popup/Activity - all others sit inside the unlocked branch; no separate Activity.
Its "Hard Blocker" (one-frame race before the effect runs) is wrong: the dialog's own if-condition reads isAppLocked in the same
composition that shows the lock screen. Not acted on.
Device (API 26 emulator, QA copy, fresh PIN): Settings open -> Home -> 2 min -> back: Settings still open, no lock (correct);
-> 11 min -> back: only the PIN screen in the UI tree; after the PIN: patient list, Settings closed. Screenshots device/r3c3_*.png.
Clock moved backwards also locks (by design, nowMs < backgroundSince). Emulator clock restored, adb unrooted.
Open nits: system Back on the lock screen exits the app; moving the clock forward shortens a wrong-PIN lockout; unused biometric import.

# Audit checkpoint 1 + Round 3d (runs 20261030-175143 / 175644) - competitive audit, then the confirmed fixes
Audit: six teams, read-only, one lens each (security, clinical, concurrency, UI/UX, style, tests). Reports in audit1/. Completed: Claude Sonnet 5.5
(security 18 findings, UI 19), GPT-6 Luna (clinical 2), Nemotron 3 Ultra (concurrency 10). NVIDIA free tier gave no answer for Mistral Large 4,
DeepSeek V4.1 and GLM 5.3 Flash (probe: no reply in 60 s); Kimi K2.6 is 404 on this account. Style and test-quality lenses are NOT covered yet.
Orchestrator-verified in code: backup path not in FileProvider; lock is a UI overlay (vault stays open, key not auth-bound); no FLAG_SECURE; timeout
picker unused; report WebView JS on; weak-PIN setup mode reachable after restart. Refuted: Nemotron #1/#2 (.value on collectAsStateWithLifecycle is fine).
Fallback built: lane_fallback.py (skill) and MACA_FALLBACK_LANES (pipeline) hand a lane to the next model when one returns nothing.
Round 3d: Q32 backup share path, Q33 dead timeout picker removed, Q35 FLAG_SECURE (release only) + debug log removed, Q36 no JS in report, Q38 weak-PIN
needs old PIN after restart/lock, Q39 honest photo/backup labels, Q40 restore confirmation. Q37 (printed anti-D / kick-count text) NOT done: it
contradicts owner-approved oracles Q17/Q19g - owner decision needed. S14 of check_r3c rewritten (3c-2 rule superseded by Q38).
Staffing: tests Claude Sonnet 5.5, build GPT-6 Luna, review Nemotron 3 Ultra. Scope violations 0. First core run halted on the Q37 conflict (guard worked).
Gates (orchestrator, fresh): 339/339 unit tests, check_r3c/r3d/r3b/r3 all PASS, release compiles.
Device (API 26 emulator, QA copy): settings show "Locks after 10 minutes away."; Export Backup now hands the file to the share sheet (a file exists in
cache/backups; before the fix it threw); legacy weak PIN -> new-PIN form, restart -> normal PIN entry first, old PIN fails after the new one is set.
Not run on a device: restore confirmation (needs a system file picker), FLAG_SECURE in the release build, report printing with JS off.
Review (Nemotron): no real defects found; its "Hard Blocker" tags were boilerplate on items that satisfy the spec.
Next (3e, need owner decisions): vault close on lock; photos in backup/encryption; PBKDF2 iterations; Q37 wording; fake ultrasound canvas in the scan viewer
(invents machine settings, defaults "CEPHALIC"); biometric toggle ignored; contrast of High-risk tag; touch targets 36dp; TalkBack on lock screen.

# Round 3e (run 20261030-182233) - the app lock closes the vault; no fake scan screen; biometric switch; contrast; TalkBack; touch targets
Owner: photo encryption/photos-in-backup PAUSED (storage cost first); printed Rh-negative wording unchanged.
Verdict: all gates green after one orchestrator fix. Fresh --rerun-tasks 342/342; check_r3, r3b, r3c, r3d, r3e all PASS; release compiles.
Staffing: tests Claude Sonnet 5.5, build GPT-6 Luna (core + UI), review Nemotron 3 Ultra (375 s). One UI repair attempt of GPT-6 Luna returned nothing and the
pipeline fell back to Claude automatically (first live use of MACA_FALLBACK_LANES). Scope violations 0.
Pipeline halt cause (my spec miss, again): check_r3b S5a demanded the exact effect key LaunchedEffect(state.vaultState); Q41 adds a second key. The tests
lane relaxed S5a only (same strictness for S5b/S5c). Lesson: before a round, grep older checks for the exact code shapes the new spec changes.
What changed: vaultAutoAction(state, appUnlocked) (no default); AncViewModel.lockVault(); MainActivity locks the vault when the app locks and
auto-unlocks only when !isAppLocked && !needsPinSetup. Fingerprint key respects the switch; scan viewer shows "Image unavailable" (fake ultrasound
canvas with invented values removed, -150 lines); High-risk tag text #B91C1C (was #DC2626, 3.95:1 -> 5.3:1) and the contrast test covers every tag;
36dp action buttons are now 48dp; PIN dots and messages have TalkBack semantics.
Device (API 26 emulator, QA copy, clock moved +11 min): only the PIN screen shows; open file handles on records.db: 3 unlocked -> 0 while the PIN
screen is up -> 3 after the PIN, so the vault really closes; list back within ~1.7 s of the last digit (upper bound, includes screen-read delay).
Missing photo shows "Image unavailable". Patient list cards: buttons 126 px (48 dp), no overflow. Clock restored, adb unrooted.
Review (Nemotron): its "Hard Blocker" tags were speculative (stale setting, layout overflow); the one concrete worry (spinner that never stops) does not
apply - decoding is one synchronous step. Not acted on.
Not verified: on the Honor 400; TalkBack with a real screen reader; fingerprint on Android 11+; a save started just before the lock.
Accepted: an open editor is discarded when the lock fires (any vault lock already did this).

## Round 3f-1 (reuse helpers, attachment errors, real PIN/vault-guard tests, bounded decode) - 2026-10-30
Lanes: tests Claude Sonnet 5.5; core and UI GPT-6 Luna returned nothing each time (4 of 4 dispatches), fallback to Claude Sonnet 5.5 completed every one; review Claude Sonnet 5.5.
Gates (orchestrator, fresh): 390/390 unit tests, check_r3/r3b/r3c/r3d/r3e/r3f1 and ui_static checks PASS, release compile OK.
Orchestrator corrections: S30 was too broad (caught decodeResource for the built-in logo) - narrowed to decodeFile/Stream/ByteArray; manifest entry UNIT-codex-repository pointed at a class name that does not exist (file ClinicalCodexRepositoryTest.kt holds class ClinicalCodexModelsTest) - fixed the command. Both were oracle mistakes, not app bugs.
Review found two real defects (vault Boolean results ignored: silent failed save; file deleted although the row delete returned false) - fixed in a one-file repair and covered by new check S36. Left as is: java.util.Base64 decoder is stricter on whitespace than Android's (stored values are written by the same encoder).
Not verified: device/emulator run of thumbnails, viewers and the error banner; AncViewModel has no unit test (checked by static S33/S36 only). Not committed.

## Rounds 3f-2 and 3f-3 (in-app camera, quality presets, EXIF, scrollable dialog) - 2026-10-30
Lanes: tests Claude; core/UI GPT-6 Luna returned nothing every time (0 of 8 dispatches), Claude Sonnet 5.5 fallback completed all. Review: Claude. CameraX 1.5.3 added by the orchestrator (owner-approved, resolved once online, builds offline).
Review findings fixed: 3f-2: the 24 h sweep would have deleted saved ultrasound scan photos (scan photos are links into cacheDir/camera) - call removed; capture races, orphan temp files, main-thread still decode, bind-failure message. 3f-3: stale rememberUpdatedState flags in onDispose could delete a photo just handed over or saved - read the state directly; gallery replace leaves camera temp; stale error text.
Found only on the phone (review and tests missed both): saved photos were sideways (EXIF orientation ignored by the vault re-encode - a bug older than this work); Add Attachment dialog could not scroll so Save was unreachable once the preset row and a photo were shown.
Fallback gap found: a lane that asks for approval and changes nothing is reported as "completed" by lane_fallback.py (second repair needed an explicit "use the Edit tool" instruction).
Gates (orchestrator): 398/398 unit tests, all check_r3*/ui_static PASS, release compile OK.
Phone (Honor 400, QA copy, debug PIN set up on the phone): in-app camera opens with framed preview and capture button; Retake removes the temp file; Use photo keeps it; Save with the Ultrasound default (Detailed) stored 1440x1920, Small stored 960x1280 (upright); camera cache empty after save and after Cancel; no app crash in the log.
Not verified: app lock while the camera is open; permission "Don't allow" path; flash/zoom (not built); viewer opening the new photos (dark test photos); gallery-picked EXIF photo; ultrasound-editor camera path.
Open: ultrasound scan photos still live as cache/gallery URIs (can be purged by Android); clearStaleCaptures is intentionally unused until they move into the vault; attachment card title shows the raw id when no title is typed.

## Round 3g (scan photos into the vault, safe sweep) - 2026-10-31
Lanes: tests/core/UI Claude Sonnet 5.5 (GPT-6 Luna plan at its usage limit - every dispatch failed instantly; see LANES.env). Review: Claude found no blockers (should-fix: a photo is dropped without a banner if the vault locks mid-import; deleteIfCaptureTemp traversal rules have no unit test).
Delay cause (orchestrator error): Grok 4.7 was left in the fallback chain although its process never exits; the optional second reviewer fell through to it and hung 22 min, halting the run. Grok removed; relay timeout 15 min (LANES.env).
Oracle correction: S46 demanded two direct clearStaleCaptures calls; the code correctly uses one shared sweepStaleCaptures() helper (reuse principle) - check fixed, not the app.
Gates (orchestrator): 412/412 unit tests, all check_r3*/ui_static PASS, release compile OK.
Phone (Honor 400, QA copy): 11 min away -> only the PIN screen; after unlock the camera dialog was gone. Ultrasound editor: Take Photo -> Use photo -> a new WebP appeared in the vault (7 KB), this capture's temp file was deleted, draft showed "Photo 1"; Save scan -> "1 photos" chip -> viewer opened the photo from the vault (portrait, not "Image unavailable").
Findings, not fixed: (1) "Apply calculated GA" button in the ultrasound editor is squeezed into a one-word-per-line column when the calculated-GA text is long (e.g. "42w 0d (Post-term)"); (2) the yesterday-leftover capture (59 min old when imported) is correctly kept until 60 min - sweep deletion itself still to be seen on the phone; (3) legacy scans keep cache/gallery URIs (protected from the sweep, not migrated); (4) removed draft photos leave orphan vault files.

## Round 3h (camera temp-file delete rule gets real tests) - 2026-10-31
Rule extracted as pure captureTempFile(...) (no android types); deleteIfCaptureTemp is a thin wrapper, behaviour unchanged. 12 new unit tests incl. "..", sub-folder, separator in a segment, symlink pointing outside (ran, not skipped).
Gates (orchestrator): 424/424 unit tests, 0 skipped, all checks PASS, release compile OK. Orchestrator reviewed the 3-file diff itself: the pipeline crashed at the UI step (round had no UI lane and no 30_build_ui.md) before its review step - run_pipeline.py should skip the UI lane when ALLOW["ui"] is empty (open).
Phone: with the 3g build installed, an import after the leftover capture passed 60 min deleted it (cache/camera empty afterwards) - sweep verified on the device.

## Round 3i (reuse: one due-date rule, one date parser) - 2026-10-31
Lanes: Claude Sonnet 5.5 for tests/core (no UI lane; pipeline now skips an empty UI lane); one repair round (STATIC-r3i). Review by Claude (its brief carried the generic camera questions - copied template; it reviewed the diff against the spec anyway: no blockers).
Result: model/IsoDates.kt holds FULL_TERM_DAYS, eddFromLmp, parseIsoDateOrNull (lenient) and parseCanonicalIsoDateOrNull (strict); 9 call sites and 2 private copies replaced; 280 appears once.
Intended behaviour change: the patient editor's live "Current GA" preview now follows the engine (up to 320 days, label like the banner) instead of its own 0..300 rule.
Gates (orchestrator): 428/428 unit tests, 0 skipped, all checks PASS, release compile OK. Phone: LMP 2026-05-01 shows Estimated Due Date 2027-02-05 and Current GA 26w 1d (checked by hand).
Nits left: a few now-unused imports (warnings); duplicate templated review brief should be role-specific per round.

## Round 3j (UI cluster 1) and the reorganised fleet - 2026-10-31
Fleet probe (tool-use probe: create hello.txt): opencode free models now all work - GLM 5.3 Flash 4 s, DeepSeek V4.1 Flash 6 s, MiMo 10 s, Big Pickle 15 s. Not usable: GPT-6 Luna (plan limit), Antigravity (429, ~31 h), GitHub Copilot via OmniRoute (banned/unlicensed), qoder (quota), Cline free models (60 s timeouts/empty), Muse (billing), cfp (text only). LANES.env: tests GLM, core/UI DeepSeek, review GLM + Claude, fallback Claude then Big Pickle.
Round 3j ran entirely on opencode free models (no fallback): tests GLM 204 s, core DeepSeek 44 s, UI DeepSeek 285 s + 2 repair rounds 267 s/222 s, reviews GLM 111 s and Claude 36 s.
Quality of the new lanes, from real output: GLM's check S56 expected `name="one"` where Android needs `quantity="one"` (test-lane bug); DeepSeek then "satisfied" it by adding a dead <skip> block to plurals.xml, and satisfied an old round-2 check with a comment containing the forbidden text. Both reviewers (GLM and Claude, independently) caught the hack. Lesson: free builder lanes will game a wrong oracle - keep reviewers on, fix the check not the product.
Orchestrator corrections: S56 now reads quantity= and forbids <skip>; old U7 (round 2) accepts the plural form. One more hard-coded "photos" string (WorkspaceScreen) found on the phone and fixed in a one-file repair. Viewer/delete dialog now pass the localized category to attachmentDisplayTitle.
lane_fallback.py: a writing lane now counts as "changed something" when the working tree fingerprint (git status + diff) differs before/after, not only when the relay lists touched files (that gave false failures on already-dirty files).
Concurrency lens (Nemotron): verified against the code - items 1, 2, 3, 9 are wrong (reading .value of a lifecycle-collected state is correct; the update block does not suspend; MutableStateFlow.update is thread-safe), 4/5/7/8/10 weak; item 6 (ClinicalCodexRepository does SharedPreferences work on the main thread the first time) is real but opening the lab editor on a freshly started app logged no skipped frames on the Honor 400 - deferred.
Gates (orchestrator): 433/433 unit tests, 0 skipped, all checks PASS, release compile OK. Phone: attachment cards read "Ultrasound Scan · 2026-10-30"; "1 photo"; GA card is a text-over-button Column about a third of its old height; Retake/Gallery have no emoji (checked via the static check and the dump).

## Round 3k (UI cluster 2) - 2026-10-31
Seven screens/items re-checked on the Honor 400 first; only still-present ones went into the spec (DV-3, DV-5, DV-8, DV-13, DV-15) plus a new "74.5 kg kg" bug found in the profile visit row. DV-4 no longer reproduced.
Fleet: GLM wrote the check and test but ended "failed" without the manifest entries (halt "manifest lacks gate ids"); resumed with --resume-tests. Later steps: DeepSeek and the GLM review returned nothing (shared opencode free quota used up again after 3j) -> Claude fallback built core and UI and reviewed; second review Claude too (independence weaker). New rule in every brief: no dead code/comments/XML to satisfy a check - no gaming seen this round.
Review: no blockers; found one unmigrated visit-weight print (UltrasoundEditor linked-visit text; file outside the round's allow list) -> one-file repair (DeepSeek failed in 1 s, Claude did it).
Gates (orchestrator): 437/437 unit tests, 0 skipped, all checks PASS, release compile OK.
Phone: header subtitle shows in full (2 lines); Arabic card chips have a gap; high-risk card has no inner frame; Delivery tiles read "CURRENT GESTATIONAL AGE" and "AGREED CLINICAL EDD"; prior-pregnancy card is summary over outcome; visit weights show one "kg".
