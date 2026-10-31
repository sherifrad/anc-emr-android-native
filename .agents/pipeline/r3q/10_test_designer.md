ROLE: TEST DESIGNER for ROUND 3q (SPEC_FIX_R3.md section "Round 3q"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3q.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/model/SevereBpAttentionTest.kt (NEW), app/src/test/java/com/sherifradwan/ancnative/ui/report/PrintBpAlertTest.kt (NEW).
1. check_r3q.py: S80-S82 exactly as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; style of .agents/checks/check_r3p.py; each S must FAIL on today's code. For S80 check the threshold literals by grepping app/src/main for the BP threshold numbers in BP context only (avoid false hits on unrelated numbers: restrict to lines mentioning systolic/diastolic/bloodPressure/bpLevel).
2. The two unit tests per the "Unit" paragraph; assert exact values. Read PatientPrintHtmlGeneratorTest.kt for the builders (do not edit it) and VisitVitalsTest.kt for style. DemoVisit/DemoPatient builders: copy their shapes.
3. TEST_MANIFEST.json: add STATIC-r3q, UNIT-severe-bp, UNIT-print-bp-alert (cmd, deps incl. every file named in S80-S82, covers). Keep others unchanged.
Run check_r3q.py and report real results.
