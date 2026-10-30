# MACA round 2 + 2b scorecard - owner decisions D1-D4, parser/age/Rh fixes
Date 2026-10-29. Spec: SPEC_FIX_R2.md (Q1-Q7, U1-U6, round 2b addendum Q8-Q12, U7). Nothing committed or pushed.
Runs: 20261029-232729 (tests lane timed out, halted by manifest guard), 20261029-234257 (stopped by orchestrator: U5 check defect),
20261029-234348 (round 2: all green + GLM review), 20261029-230415 (2b: tests red, DeepSeek quota -> halt),
20261029-233138 (2b: Claude Haiku builder, all green; GLM review unavailable - quota).

## Verdict
Round 2 and 2b oracles are all green: full unit suite 207/207 (fresh --rerun-tasks), STATIC-ui 6/6, STATIC-ui-r2 30/30, compile OK.
Round 2c (Q13/Q14) fixed R3-1: full unit suite 215/215 (fresh --rerun-tasks). Code is commit-ready on unit/static evidence, with one open
Should-fix (R3-2) and device checks still pending.

## Independence
Tests: opencode GLM-5.3 (both rounds). Build round 2: opencode DeepSeek-v4.1 (core + UI). Build round 2b: Claude Haiku 5.5 via claude CLI
(verified from its session logs: 24 + 63 Haiku turns; the single Opus line per log is a request-shape metadata record, not a model turn).
Review round 2: GLM (read-only). Review round 2b: Claude orchestrator only (GLM hit its free quota) -> same vendor as the 2b builder; weaker independence, stated.

## What changed (all verified by tests + orchestrator probe)
Parser: sentence/semicolon segments with exact rejoin; future tense stays in Plan; "Hospital: X" labels; leading verbs dropped; transport phrases
("Go To Hospital") rejected; all hospitals listed; "St. Mary's" not cut; pending/TBD = not agreed; lowercase-sentence split ("Fee ... . if bleeding call").
Clinical format: age 10-70 whole-text only; formatBloodGroup adds "(Rh negative)"; isRhPositive. One Rh/risk rule on list, profile and workspace
(list/profile flagged "Not high risk" as high before). UI: emoji removed from the 3 main screens + badges; pencils everywhere; "Correct previous
pregnancy" is a text button; clinical-alerts icon uses a string resource and 48dp minimum; photo chip says "3 photos".
testTags and click handlers preserved (diff read in full).

## Discipline
- Red before green: GENUINE this time. Round 2: UNIT-parse, UNIT-format, STATIC-ui-r2 failed on assertions with the new-API test parked
  (compile_error=false). Round 2b: 10 parser tests + U7 red on assertions.
- Scope violations: 0 in every lane. Tests weakened: none found (one round-1 case "0"->"0" changed to null, mandated by Q5).
- Repairs: core 0, UI 0 in both rounds.

## Findings
- R3-1 Should-fix (spec error, mine): Q8 drops every hospital in a segment containing a history marker, including "pregnancy".
  "Elective CS at Dar Al Fouad Hospital given previous CS x2" and "Planned delivery for this pregnancy at Cleopatra Hospital" -> hospital null,
  card says "Hospital not agreed yet". Text stays in Plan, but the card statement is false. Needs a narrower oracle (see proposal).
- Nit: free-text "O- (partner A+)" reads as Rh positive (values are validated on save; only a restored backup could carry it). Not taken.
- Nit: Q9 rejects any name containing the word "To"/"Go"/"If"; no real Egyptian hospital name found with these, accepted.

## Orchestration issues found and fixed (run_pipeline.py only)
1. Red check counted compile errors as red (round-1 blind spot) -> parks new-API tests, separates compile errors, routes them to the tests lane.
2. Missing manifest ids would silently ungate tests -> halts if a gate id is missing (caught GLM's timeout).
3. Lane quota/auth failure burned repair rounds -> halts at once when a lane fails with no output and no changes.
4. Added --round r2, --resume-tests, --from-core.
5. Test-check defect caught before build: U5 parsed parentheses only, so idiomatic `TextButton(...) { Text(...) }` would fail; sent back to GLM.
Skill bugs NOT fixed (skill repo, needs owner OK): claude-delegate relay ignores --model (would run the CLI default model; worked around
with ANTHROPIC_MODEL); relay reports "completed" on agy empty output (round 1).

## Economy and time
Cache: no-change re-check reused 9/9 (round 1: 1/5) after removing always:true. Wall time (s): tests 900 (timeout) + 269 + 333;
core DeepSeek 266, Haiku 129; UI DeepSeek 260, Haiku 25; review GLM 281. Free quotas exhausted: DeepSeek, GLM (opencode), agy.

## NOT verified
Device run, TalkBack, touch targets (D5), layout/truncation of "(Rh negative)" in the narrow blood-group tile and list badge, dark theme,
real patient note formats, androidTest suite, lint.

## Round 2c (run 20261029-233755) - fixes R3-1
Tests: Claude Sonnet 5.5 (36.7s, 8 tests, every Q13/Q14 example; no existing test line removed - diffed). Build: Claude Haiku 5.5 (91s,
DeliveryNotesParser.kt only). Review: Claude orchestrator only (GLM quota). All three Claude-family: lowest independence of the three rounds.
Red before green: exactly the 4 tests the spec predicted (3 Q13 cases that gave null + Q14), assertion failures, no compile error.
Gates: UNIT-parse, UNIT-parse-legacy, COMPILE-app, UNIT-all green; cache re-check 9/9 reused. Scope violations 0.
Orchestrator probe (own inputs, not in spec): "Previous baby born at El Shatby Hospital; this time Cleopatra Hospital" -> Cleopatra;
"Repeat CS at Cleopatra Hospital (previous CS at Al Salam Hospital 2020)" -> Cleopatra; "Last delivery at Al Salam Hospital..." -> null;
"G3P2, previous CS x2, booked Cleopatra Maternity Hospital" -> Cleopatra Maternity Hospital. All correct.
- R3-2 Should-fix (open): capitalised clinical words glue onto names and hide the past marker inside the phrase.
  "Last CS Dar Al Fouad Hospital, plan Cleopatra Hospital" -> hospital box "Last CS Dar Al Fouad Hospital" + "Cleopatra Hospital".
  Fix: add Last/Previous/Prior/Earlier/Repeat/Elective/Emergency/CS/LSCS/SVD/NVD to the leading-word drop list, and treat a dropped
  past-delivery word as a marker before the phrase. Text is never lost (line stays in plan); the box over-reports.
Orchestration: claude relay --model bug now worked around per lane in run_pipeline.py (ANTHROPIC_MODEL per dispatch); added --skip-ui.

## Model selection (2026-10-30, before round 2d)
Live probe: opencode DeepSeek/GLM quotas reset; also answering: MiMo, Flint, Beam, Big Pickle, Nemotron (free), Gemini 3.5 Flash (Google free
tier; 3.1 Pro/4 Argon quota 0), GPT-6 Luna (ChatGPT plan; other OpenAI models not allowed on that account). Public boards do not list these
exact versions, so an own synthetic benchmark (no app code, no patient data) was run:
  Code task (20 hidden tests): GPT-6 Luna, Beam, Big Pickle, DeepSeek, GLM, Nemotron 20/20; Flint compile errors; MiMo, Gemini timed out.
  Review task (5 planted bugs): GLM 5/5 (+1 real extra), Nemotron 5/5, Big Pickle 5/5 (+1), GPT-6 Luna 5/5 (7 s), DeepSeek 4/5, Gemini Flash 4/5,
  Beam/MiMo timed out. Note: several opencode sessions in parallel stall; run lanes one at a time.
Staffing 2d: tests GLM, build DeepSeek, review Nemotron (NVIDIA, third vendor) + GLM. Three vendors, builder independent of tests and review.

## Round 2d (run 20261030-110956) - fixes R3-2
Tests GLM (136 s, 9 tests: 7 spec + 2 own boundaries; no existing line removed - diffed). Red: 8 failed on assertions (all 7 spec
examples + "CS Hospital"); "cs Dar Al Fouad Hospital" already passed (lowercase "cs" was never part of the phrase). Build DeepSeek (113 s, DeliveryNotesParser.kt only; also added LSCS/SVD/NVD to the past-marker
delivery words, needed by the "Previous LSCS" example). Gates green, cache re-check 9/9 reused, scope violations 0.
Fresh --rerun-tasks: 224/224 unit tests, STATIC-ui 6/6, STATIC-ui-r2 30/30.
Orchestrator probe (11 own lines): R3-2 line -> "Cleopatra Hospital"; "Previous LSCS at Al Salam Hospital 2021. Now booked Cleopatra Hospital"
-> Cleopatra; "G2P1 last SVD El Shatby Hospital, this pregnancy Dar Al Fouad Hospital" -> Dar Al Fouad. 10/11 correct.
Reviews: Nemotron PASS Q1-Q15, ~23 own lines, 3 nits. GLM PASS Q1-Q15 (ran the suites and both static checks), 14/15 own lines, new findings:
- R4-1 Should-fix (confirmed by probe): a capitalised current word glues on: "Now Cleopatra Hospital" -> "Now Cleopatra Hospital".
  Fix: add now/current/currently/this to the leading-word drop list.
- R4-2 Should-fix, OUTSIDE round scope (confirmed by grep): ui/report/PatientPrintHtmlGenerator.kt:65-66 still uses
  riskLevel.contains("High") and bloodGroupText.contains("-"). Printed report says "High Risk" for "Not high risk", and misses "O neg",
  "O −" (U+2212), "AB Negative" as Rh negative, so the anti-D line (l.1203) is not printed for them. Same bug Q7 fixed on screen.
- Nit: "Transfer From Aswan General Hospital ..., deliver at Cleopatra Hospital" lists the origin hospital too.
