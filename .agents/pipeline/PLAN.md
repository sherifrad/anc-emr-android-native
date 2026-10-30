# Orchestration plan (MACA, implementation + testing, round 1)
Orchestrator (Claude): reads, plans, dispatches, enforces ownership, verifies, reports. Writes no app code.

| Step | Mode | Role | Lane | May edit |
|---|---|---|---|---|
| 1 | testing | Test Designer (tests first) | opencode GLM-5.3-flash | the 3 test files, check_ui_static.py, TEST_MANIFEST.json |
| 1b | testing | Test Runner = script | run_pipeline.py | nothing (RED check must fail) |
| 2a | implementation | Core Logic builder | opencode DeepSeek-v4.1-flash | DeliveryNotesParser.kt, ClinicalFormat.kt, WorkspaceScreen.kt (parser + helper calls only) |
| 2a' | testing | selected tests, up to 2 repair rounds | script + cache | - |
| 2b | implementation | UI/UX builder | agy | ClinicalBadges.kt, Theme.kt, WorkspaceScreen.kt (card), ProfileScreen.kt, strings.xml |
| 2b' | testing | selected tests + static check + compile | script + cache | - |
| cache check | testing | nothing changed, expect REUSE (failed tests rerun by design) | script | - |
| 3 | testing | Independent review, read-only | GLM (did not write code) + optional gemini/codex | none |
| 4 | testing | Orchestrator's own Opus review of the final diff + scorecard | Claude | none |

Independence: tests author (GLM) != builders (DeepSeek, agy); reviewers != authors. Out-of-scope edits are reverted automatically. Backups: .agents/backup-<ts>/ before anything runs.
Scope fixed this round: findings confirmed by 2+ model families, plus the parser blockers I reproduced. Deferred to owner: Rh text marker, emoji, pencil consistency (unverified claim), localisation, touch targets.

## How the skill's performance will be judged (recorded in runs/<ts>/metrics.json)
1. Defects: findings still open after the fix (re-review) vs the 5 blockers + 11 should-fix before.
2. Discipline: scope violations per lane, red-before-green seen, repair rounds used (max 2).
3. Economy: tests run / reused / deferred; lanes needing repair; wall time per lane.
4. Honesty: anything not verified is stated (device tests, RTL, real data formats).
Limits: one run, free-tier models, token counts per vendor are not available, so cost is judged by tests avoided and rounds used, not dollars.
