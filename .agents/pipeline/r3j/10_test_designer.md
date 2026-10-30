ROLE: TEST DESIGNER for ROUND 3j (SPEC_FIX_R3.md section "Round 3j"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3j.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/AttachmentDisplayTitleTest.kt (NEW).
1. check_r3j.py: S55-S58 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; follow the style of .agents/checks/check_r3i.py; each S must FAIL on today's code. Also grep the older checks (.agents/checks/*.py) for shapes this round changes (photos_count, attachment.title, scanCalculatedGaCard, AddAttachmentDialog text) and list them in your final message.
2. AttachmentDisplayTitleTest per the "Unit" paragraph (JUnit4, only the API named in the spec).
3. TEST_MANIFEST.json: add STATIC-r3j and UNIT-attachment-title (cmd, deps incl. every file named in S55-S58, covers); edit directly; keep other entries unchanged.
Run check_r3j.py and report real results (the unit test will not compile until core is done).
