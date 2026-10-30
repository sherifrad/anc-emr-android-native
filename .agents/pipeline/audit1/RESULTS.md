# Audit checkpoint 1 - results (orchestrator-verified where marked)
Completed: Claude Sonnet 5.5 (security), GPT-6 Luna (clinical), Nemotron 3 Ultra (concurrency).
No report (NVIDIA free tier gave no answer within 25 min): Mistral Large 4 (style), DeepSeek V4.1 (tests), GLM 5.3 Flash (UI, replaced Kimi K2.6 which is 404 on this account).
Verified by orchestrator against code: Claude #1 file_paths.xml missing cache/backups; #3 onBackgrounded(true) never closes vault, key not auth-bound;
#4 no FLAG_SECURE; #7 AutoLockTimeout picker unused; #9 javaScriptEnabled=true in report WebView. #2 code-read only (not run).
Refuted: Nemotron #1 and #2 (.value on collectAsStateWithLifecycle in composition is correct). Unchecked: GPT #1-2 (generic print text), Nemotron #5-#10.
Score (provisional): Claude ~ +60 | GPT ~ +10 | Nemotron ~ -10 (2 false blockers, rest unverified).
