# MACA round 1 scorecard - delivery card, notes parser, clinical helpers, age badge
Date 2026-10-29. Runs: runs/20261029-025233 (halted), runs/20261029-031001 (test repair + agy UI attempt), runs/20261029-031629 (UI on DeepSeek + review).
Nothing committed or pushed. Backups: .agents/backup-<ts>/ per run, plus snap-before-<step>/ in each run dir.

## Verdict
Round 1 scope (SPEC_FIX P1-P10) is met: every oracle green, confirmed by an independent reviewer (GLM) and by my own review.
**Not ready to commit.** Owner decisions D1-D4 (OWNER_DECISIONS.md) and the round-2 findings below are not yet applied, and nothing was checked on a device.

## Independence
Multi-vendor. Tests + review: opencode GLM-5.3. Core + UI builder: opencode DeepSeek-v4.1. Final review: Claude (orchestrator, wrote no app code).
agy (Gemini family) contributed nothing: headless write permission auto-denied, then quota exhausted (resets ~78h). The UI lane moved to DeepSeek, so one model built both slices (less builder diversity; tester/reviewer still differ from builder).

## Defects: before (prior audit) vs after
| Item | Before | After | Evidence |
|---|---|---|---|
| HB1 fee substring ("feels", "breastfeed") | blocker | CLOSED | ParseDeliveryNotesSpecTest; scratch probe |
| HB2 history/PPH lines -> Postpartum | blocker | CLOSED for history markers (see R2-1) | spec test |
| HB3 strip before ':' / quoted fee shown as agreed | blocker | CLOSED | spec test ("10:30", "Fee quoted (NOT agreed)") |
| HB4 hospital = "39 weeks"/sentence | blocker | CLOSED for spec cases (see R2-2) | spec test |
| HB5 planning notice removed | blocker | CLOSED | STATIC-ui (a); diff |
| S1 label ids replaced by literals | should-fix | CLOSED | STATIC-ui (b); diff |
| S2 English literals | should-fix | PARTIAL: edit icon uses string resource; titles/"AGE"/"yrs" accepted (app is English-only) | STATIC-ui (c) |
| S3 one-paragraph notes collapse into one box | should-fix | OPEN (not in P1-P10) | probe: whole note -> fees |
| S4 hospital shown twice | should-fix | RESOLVED by design, meets D2: hospital box holds the extracted name only | probe |
| S5 Rh detection | should-fix | CLOSED for spec cases (residual nit R2-5) | ClinicalFormatTest |
| S6 "Not high risk" emphasised | should-fix | CLOSED | ClinicalFormatTest |
| S7 age suffix handling | should-fix | CLOSED (new gap R2-4) | ClinicalFormatTest |
| S8 badge announced twice | should-fix | CLOSED in code (clearAndSetSemantics); TalkBack not run | STATIC-ui (d) |
| S9 contrast < 4.5 | should-fix | CLOSED (dark label #4F46E5; placeholder full opacity + italic) | ThemeContrastTest |
| S10 RTL badge / blank spacer | should-fix | PARTIAL: spacer fixed; RTL not applicable (English-only) | diff |
| S11 icon-only correction button | should-fix | OPEN -> owner decision D4 | - |
Summary: 5/5 blockers closed; should-fix 6 closed, 1 resolved by design, 2 partial, 2 open.

## New findings for round 2 (confirmed by compiling and running the real code, Claude only unless noted)
- R2-1 Should-fix (clinical misfile): future-tense plans go to Postpartum and leave Plan empty. "Will be delivered by elective CS at 39w", "Expected to be discharged day 2 after CS" -> postpartum. Spec gap in P2 (triggers on the word, not tense).
- R2-2 Should-fix: agreed hospital missed or mangled. "Hospital: Dar Al Fouad" -> null (card says "Hospital not agreed yet"); lowercase "Cleopatra hospital" -> null (also raised by GLM as nit); leading capitalised words glued: "Booked Cleopatra Maternity Hospital".
- R2-3 Should-fix: fee lines swallow clinical text. "Fee 15,000 EGP; penicillin allergy - avoid" -> allergy shown only under Fees. Same root as S3 (line-based).
- R2-4 Should-fix: formatAgeYears takes the first digit run with no range check. "1990-05-01" -> badge "AGE 1990 yrs"; "290" accepted.
- R2-5 Nit: isRhNegative / isHighRisk handle free text ("O- (partner A+)" -> false, "Pending - repeat" -> true), but stored values are validated enums (PatientVocabulary: A+..O-, Low/Moderate/High Risk). Simplest correct code: compare to the enum values.
- R2-6 Nit (GLM): "Postpartum note:" label outranks history markers.
- R2-7 Pre-existing (GLM): "Edit Clinical Alerts" icon has a literal contentDescription and size(32.dp).
- R2-8 Unverified: KeyFactTile labels now use longer strings ("Agreed clinical EDD", "Manual risk level") with maxLines = 1; truncation on narrow phones not checked.
- Emoji still in blood-group value (D3 not yet applied).
- GLM claim REJECTED as stated: "PencilEditButton below 48dp because minimumTouchTarget() was dropped". M3 IconButton enforces a 48dp minimum interactive size and the theme does not override LocalMinimumInteractiveComponentSize. Status: unverified until the device measurement (D5).

## Discipline
- Scope violations: 0 in every lane (machine-enforced snapshot diff).
- Red before green: UNIT-contrast genuinely red (compiled, assertion failure) before the UI lane. STATIC-ui red before. UNIT-parse/UNIT-format: the 1b "red" was a test-compile error, NOT an assertion failure, so red-before-green for these two was not demonstrated by JUnit (the prior audit's Python port reproduced the same defects, which is partial evidence).
- Repair rounds: core 2/2 used, both no-op (correctly: the blocker was a test file it may not edit). Tests lane 1 repair (2-line compile fix, no assertion weakened - diffed). UI 0 on DeepSeek; agy 1 run + 2 repairs, all no output.

## Economy and time
- Final: full unit suite 159/159 pass (113 existing + 46 new). COMPILE-app pass. Lint NOT re-run (17 pre-existing errors from the earlier audit).
- Cache: reuse 1/5 on the no-change check, because the Test Designer marked all four T1 tests "always": true. Cache logic works; the manifest disables it.
- Wall time (s): tests 377.5 + 45.6 repair; core 303.6 + 154.0 + 132.2; agy 94.0 + 3.7 + 3.3 (no output); UI DeepSeek 76.3; review GLM 187.8 (on red UI, superseded) + 117.9.

## Orchestration issues found and fixed (run_pipeline.py only, no app code)
1. Core gate ran UI-only checks -> core could never go green. Fixed: CORE_GATE = parse, format, compile.
2. A test-file compile error was counted as a behavioural failure and routed to the core builder. Fixed for this run with --resume-ui (repair goes to the tests lane); still worth detecting automatically.
3. Relay reported "completed" when agy produced nothing (permission auto-denied). Not fixed: the relay should treat empty output + no changes as failure.
4. Pipeline printed DONE with a red UI gate. Fixed: prints "UI GATE STILL RED".
5. GLM reviewer could not open .agents/checks/check_ui_static.py (read-only plan agent) and verified P10 by inspection. Not fixed.

## NOT verified
Device run, TalkBack, touch targets (D5: approved for the device-testing phase), visual layout/truncation, dark theme on device, real patient note formats, instrumented (androidTest) suite, Android lint.

## Owner decisions (OWNER_DECISIONS.md)
D1 Rh text marker: yes. D2 hospital in one box: yes (already met). D3 remove emoji: yes. D4 pencil everywhere except "Correct previous pregnancy" (text): yes. D5 touch targets: measure over USB during the device-testing phase.
