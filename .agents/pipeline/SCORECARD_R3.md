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
