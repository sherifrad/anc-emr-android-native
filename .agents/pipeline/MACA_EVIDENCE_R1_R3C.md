# MACA evidence report, rounds 1 to 3c (generated from runs/*/metrics.json, 2026-10-30)

How to read: every row is one pipeline run. "red" = tests that failed before the build (proof they test something). "repairs" = extra build
attempts after a failed gate. "scope" = files a lane touched outside its allowed list (reverted automatically).

| Run | Round | Tests lane | Build lane | Review lane | Red before green | Repairs | Scope violations |
|---|---|---|---|---|---|---|---|
| 20261029-025233 | r1 | GLM free | DeepSeek free | GLM free | UNIT-parse/format/contrast, STATIC-ui | 2 core, then halt | 0 |
| 20261029-233138/233755 | r1 | Claude Sonnet | Claude Haiku | GLM (1.5 s, no output) | UNIT-parse | 0 | 0 |
| 20261029-234348 | r2 | GLM free | DeepSeek free | GLM free | UNIT-parse/format, STATIC-ui-r2 | 0 | 0 |
| 20261030-110956, 114106 | r2b/r2c | GLM free | DeepSeek free | Nemotron Super + GLM | UNIT-parse, UNIT-print | 0 | 0 |
| 20261030-124141 | r2 | (reused) | GPT-6 Luna | Gemini 3.5 Flash | reused | 0 | 0 |
| 20261030-145357, 150323 | r3a | Claude Sonnet | GPT-6 Luna | Nemotron 3 Ultra | UNIT-print, STATIC-r3 | 0 | 0 |
| 20261030-152104 | r3b | Claude Sonnet | GPT-6 Luna | Nemotron 3 Ultra + Grok (timed out at 25 min) | STATIC-r3b | 0 | 0 |
| 20261030-160522 | r3c | Claude Sonnet | GPT-6 Luna | none (halted) | STATIC-r3c | 2 then halt (my spec error) | 0 |
| 20261030-160822, 161610, 162108 | r3c | Claude Sonnet | GPT-6 Luna | Nemotron 3 Ultra | reused / STATIC-r3c | 0 | 0 |

## What this proves
- No lane ever edited outside its allowed files (0 violations in every run).
- Tests were red before the code was written in every round that had new behaviour.
- The builder never reviewed its own work, and the review vendor differed from the build vendor from round 3a on.
- Guards halted three times (incomplete test list, unscoped check, unreachable check). None let wrong work through.

## What it does not prove
- That the reviewer found everything. Reviewers added real findings (invented print text in 3a; old PIN surviving a restart in 3c) and some
  false ones (stale lockout "blocker"), which I judged by reading the code.
- Behaviour on a real device unless a device line exists in the scorecard for that round.
- Early rounds (r1, r2) used free models whose runs were slower and needed repairs; they predate the current staffing.

## Process failures to learn from
1. Round 3c: my check S13 named no file; fixed by scoping it. Rule: every static check names the exact file it scans.
2. Grok was written off as "looped"; it had actually been cut at the 25-minute limit. Rule: read the log before diagnosing a lane.
3. A second session ran a pipeline round (3c-2) in the same working tree while I was verifying. Rule: one session per tree, or work in
   separate worktrees; re-run the full verification after any foreign change.
4. Legacy-PIN repository logic is only device-verified. Rule: pure decision logic goes in a unit-testable policy file.
