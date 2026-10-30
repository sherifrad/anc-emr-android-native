ROLE: TEST DESIGNER for ROUND 3k (SPEC_FIX_R3.md section "Round 3k"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3k.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/ui/FormatWeightKgTest.kt (NEW).
1. check_r3k.py: S59-S63 exactly as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on FAIL; follow the style of .agents/checks/check_r3j.py; each S must FAIL on today's code. Check Android/Compose facts before encoding them in a regex (for example plurals need quantity=; Arrangement.spacedBy(8.dp, Alignment.End) is the real overload). Also grep the older checks (.agents/checks/*.py) for shapes this round changes (KeyFactTile, Arrangement.End, Prior Pregnancies, visit.weight, subtitle maxLines) and list them in your final message.
2. FormatWeightKgTest per the "Unit" paragraph.
3. TEST_MANIFEST.json: add STATIC-r3k and UNIT-format-weight (cmd, deps incl. every file named in S59-S63, covers); edit directly; keep other entries unchanged.
Run check_r3k.py and report real results (the unit test will not compile until core is done).
