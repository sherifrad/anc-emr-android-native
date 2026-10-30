# Audit checkpoint 1 - results (orchestrator-verified where marked)
Completed: Claude Sonnet 5.5 (security), GPT-6 Luna (clinical), Nemotron 3 Ultra (concurrency).
No report (NVIDIA free tier gave no answer within 25 min): Mistral Large 4 (style), DeepSeek V4.1 (tests), GLM 5.3 Flash (UI, replaced Kimi K2.6 which is 404 on this account).
Verified by orchestrator against code: Claude #1 file_paths.xml missing cache/backups; #3 onBackgrounded(true) never closes vault, key not auth-bound;
#4 no FLAG_SECURE; #7 AutoLockTimeout picker unused; #9 javaScriptEnabled=true in report WebView. #2 code-read only (not run).
Refuted: Nemotron #1 and #2 (.value on collectAsStateWithLifecycle in composition is correct). Unchecked: GPT #1-2 (generic print text), Nemotron #5-#10.
Score (provisional): Claude ~ +60 | GPT ~ +10 | Nemotron ~ -10 (2 false blockers, rest unverified).

## Second pass: style and test-quality lenses (2026-10-30, Claude Sonnet 5.5)
Reports: REPORT_claude_style.md, REPORT_claude_tests.md (read-only; nothing was run). GPT-6 Luna returned nothing on both (instant fail), Claude took over via the fallback chain.
Orchestrator spot-checks (read the code): CONFIRMED - attachment save/delete errors only logged (AncViewModel.kt ~135, ~155) and delete order file-then-row; duplicate PatientPrintHtmlGeneratorTest in report/ and ui/report/; Rh "classification" test never calls production code (NegativeControlAndClinicalValidationTest.kt:204); SecuritySettingsRepository has no unit test.
NOT yet verified: unused-import count (55, text script), hard-coded string counts, the three "mutations that would still pass".
Note: the fallback's read-only check flagged Claude as failed because my own PHOTO_STORAGE_PLAN.md changed during the run (false positive; output used from stdout.txt).
