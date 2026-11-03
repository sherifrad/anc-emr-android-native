You are an independent frontier reviewer (read-only) for the ANC EMR Android native app repository (/Users/sherifgamalradwan/anc-emr-android-native).
This is a Level 3 (clinical safety, data schema, vital signs) review of Logic Round L08 (Visit and Vitals Entry Rules):

Review the working directory changes (uncommitted diff against HEAD):
- `app/src/main/java/com/sherifradwan/ancnative/model/VisitEntryLogic.kt` (new file: isHighBloodPressure, fillNormalVisitFindings, resolveLastVisitVitals, estimateFundalHeightForGa, buildVisitFindings, parseVisitFindings, detectFlaggedFindings, formatVisitSummary)
- `app/src/main/java/com/sherifradwan/ancnative/model/codex/CustomChoiceModels.kt` (added VISIT_BP, VISIT_WEIGHT, VISIT_FUNDAL_HEIGHT to CustomChoiceList)
- `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt` (added DraftField enum values, DraftValues structured fields, AncReducer.updateDraft sync, DemoVisit.toDraftValues parsing)
- `app/src/main/java/com/sherifradwan/ancnative/storage/SaveVisitCommand.kt` (supports systolic/diastolic and structured findings composition in prepare())
- `app/src/test/java/com/sherifradwan/ancnative/model/VisitEntryLogicTest.kt` (unit tests covering BP flagging, strict normal fill isolation, same-as-last resolver, GA fundal height estimation, findings roundtrip, flagged findings detection)
- `app/src/test/java/com/sherifradwan/ancnative/model/codex/CustomChoicesLogicTest.kt` (updated allowedLists check to verify dating methods invariance with new visit enums)
- `app/src/test/java/com/sherifradwan/ancnative/state/VisitDraftStateAndCommandTest.kt` (unit tests covering AncReducer bidirectional sync, SaveVisitCommand.prepare() with structured values, high BP non-blocking save)
- `.agents/pipeline/SPEC_LOGIC_L08_VISIT_ENTRY.md` (spec and outcome oracles)

Check the following critical areas:
1. Clinical Safety & Vital Signs Invariants:
   - Does `fillNormalVisitFindings` strictly isolate findings and NEVER overwrite blood pressure, weight, fundal height, visit date, or notes?
   - Does High BP (systolic >= 140 or diastolic >= 90) flag appropriately but NEVER block saving valid clinical records?
   - Does `estimateFundalHeightForGa` bound checks strictly within 10..50 cm?
   - Does `CustomChoiceList` continue to strictly exclude dating methods (LMP, IVF, Scan, Clinical)?
2. Data Schema & Persistence Safety:
   - Does the change leave Room database version (6) and schema completely untouched?
   - Are structured findings composed into `findings` and parsed back without data loss?
   - Can legacy free-text findings still be parsed or preserved gracefully?
3. Reducer & Command Integrity:
   - Is bidirectional synchronization between `DraftField.BLOOD_PRESSURE` and `DraftField.SYSTOLIC`/`DIASTOLIC` safe, idempotent, and non-recursive?
   - Does `SaveVisitCommand.prepare()` preserve existing error validations (implausible BP, implausible weight, future date, stale revision)?
4. Independent Test Review:
   - Review `VisitEntryLogicTest.kt` and `VisitDraftStateAndCommandTest.kt`. Do these tests adequately exercise the outcome oracles? Do they genuinely verify failure and success conditions?

Format your response:
1. Executive Summary & Verdict (PASS / PASS WITH NITS / FAIL)
2. Detailed Findings (categorized as Hard Blocker, Should-fix, or Nit, with file and line references)
3. Independent Test Review Verdict (Valid oracle tests / gaps identified)
Keep output concise and focused on high-assurance clinical and state safety.
