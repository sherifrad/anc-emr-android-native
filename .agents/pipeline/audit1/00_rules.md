# AUDIT CHECKPOINT 1 - competitive bug hunt (READ-ONLY)
You are one TEAM in a competition of independent AI models auditing an offline Android antenatal-care EMR (Kotlin, Jetpack Compose,
Room, encrypted vault). Rounds 1-3c-3 were written by several different models (GLM/DeepSeek, Gemini, GPT-6 Luna, Claude) and tested
by others. The owner wants to know: is it safe to continue, or are there hidden or old defects? Real clinic data will live in this app.

HARD RULES
1. READ-ONLY. Edit nothing, create nothing, run no build, no git write (no commit/checkout/stash/add). Allowed: reading files, grep/find,
   `git log`, `git diff`, `git blame`, `git show`. Your whole answer is your final message (text only).
2. Do NOT read: .agents/pipeline/audit1/ (other teams), .agents/pipeline/runs/, .agents/fleet-runs/, .agents/backup-*, SCORECARD*.md,
   MACA_EVIDENCE*.md. Independence is the point; the owner will compare teams.
3. Never invent. Every finding needs the exact file path, line number and a quote of the code. Run nothing you cannot read; say
   "not verified by running" when true. A finding you cannot support with a quote is a false positive and costs you points.
4. Scope: app/src/main/java, app/src/main/res, app/src/main/AndroidManifest.xml, app/build.gradle.kts, app/src/test, .agents/checks.
   Owner decisions are in .agents/pipeline/OWNER_DECISIONS.md and are NOT bugs (D1-D8). Known open items (do not re-report unless you
   find a worse variant): printed free text not HTML-escaped; Back on lock screen exits app; forward clock shortens lockout;
   anomalies not yet shown in UI; existing clinic vaults keep already-seeded demo patients.

TWO JOBS (both mandatory)
A. BUG TRACKER - find real defects in your LENS (below): wrong clinical output, data loss/corruption, security holes, crashes,
   lifecycle/state bugs, accessibility/UX failures, vacuous tests. Look at old code too, not only recent commits.
B. STYLE POLICE - the code came from several models. List where files deviate from ONE consistent standard: Kotlin official coding
   conventions + this project's dominant pattern (infer it from the majority of files; state the pattern in one line). Look at naming,
   file/package layout, error handling style, nullability idioms, state hoisting in Compose, string resources vs hard-coded text, comment
   density and language, test naming and structure, magic numbers, dead code/unused imports. Say which commit/author-model introduced
   each deviation when `git blame` shows it. Group deviations: give the rule once, then up to 5 example locations and the total count.

SCORING (the owner's referee verifies every claim against the code)
 confirmed Blocker +10 | confirmed Should-fix +5 | confirmed Nit +1 | first-to-find bonus +3 | wrong or unsupported claim -5 |
 claim another team also found: points shared. Quality beats quantity: max 20 bug findings + 10 style groups. Rank by severity.

OUTPUT FORMAT (final message, markdown, <= 220 lines)
## Team <your model> - lens <lens>
### Bugs
| # | Sev | file:line | quote (<=1 line) | failure scenario (input -> wrong result) | fix (1 line) | confidence H/M/L |
### Style groups
rule | deviation | count | examples file:line | introduced by (commit/model if known)
### Not covered / not verified
### Verdict: can the project continue to the next tasks? (YES / YES-after-fixes / NO) and the 3 things that must be fixed first.
