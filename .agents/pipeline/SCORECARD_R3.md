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
reviewer: produced no review in 25 min (looped "plan" sessions) -> HALT after the gates; NOT fit as an opencode lane today.
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
