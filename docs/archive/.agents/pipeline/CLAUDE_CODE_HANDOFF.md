# Handoff to Claude Code (from the Cowork chat, 2026-10-29)
You are the ORCHESTRATOR under the MACA skill (~/skills/multi-agent-collaborative-audit/SKILL.md - read it first). Your role: read, plan, dispatch lanes to vendor CLIs, enforce ownership, verify, report. You write NO app code; the builder lanes do.

## User and constraints
Dr. Sherif, private obstetrics clinic. Expects verified, accurate claims; never certify beyond evidence. Do not push to GitHub, commit, delete, or touch files outside the lanes' allowed lists without asking. Send code only (no patient data) to vendors. App is English-only (MainActivity forces Locale.ENGLISH, no values-ar).

## Situation
Project: ~/anc-emr-android-native (Kotlin, Compose). Uncommitted UI change under review (delivery card, age badge, pencil edit buttons). Audit result: NOT mergeable. Evidence: .agents/maca-run-2026-10-29/{REPORT,REVIEW,CROSS_VENDOR,SPEC}.md. Parser defects were reproduced (parse_port_repro.py). Four reviewers agreed on contrast, age text handling, a11y double announce, Rh/risk emphasis, weak author test.
Vendor CLIs on this Mac: opencode (models opencode/glm-5.3-flash-free, opencode/deepseek-v4.1-flash-free), agy, claude, codex (needs a model supported by the ChatGPT account; gpt-5.6-sol is rejected), gemini (needs auth: run `gemini` once or set GEMINI_API_KEY).
Known relay limits: vendored relays keep only the last 4000 chars in result.json (fixed: full stdout.txt now saved); default timeout 5 min (env MACA_RELAY_TIMEOUT_MS).

## What is ready (nothing of the fix has run yet)
- .agents/pipeline/SPEC_FIX.md : interface contract + oracles P1-P10 + out-of-scope list
- .agents/pipeline/00_common_prefix.md, 10_test_designer.md, 20_build_core.md, 30_build_ui.md, 40_review.md : briefs
- .agents/pipeline/run_pipeline.py : tests-first -> red check -> core builder -> selected tests (<=2 repairs) -> UI builder -> selected tests -> cache check -> read-only review; enforces file ownership (reverts out-of-scope edits), backups to .agents/backup-<ts>/, metrics.json. Tested only with a fake CLI. `python3 .agents/pipeline/run_pipeline.py --plan` shows lane availability.
- .agents/pipeline/PLAN.md : roles, models, scorecard criteria.

## Your tasks
1. Run `--plan`, report fleet table and independence level (multi-vendor if 2+ families run). Fix gemini/codex lanes only with the user's help (credentials/model).
2. Run the pipeline (python3 .agents/pipeline/run_pipeline.py). If opencode asks for edit permissions non-interactively and nothing changes, report it; do not widen permissions yourself.
3. When done: do your own independent review of the final diff against SPEC_FIX.md P1-P10, then write .agents/pipeline/SCORECARD.md: defects open vs 5 blockers + 11 should-fix before, scope violations, red-before-green, repair rounds, tests run/reused/deferred, wall time per lane, and what was NOT verified (device, RTL, real data).
4. Open owner decisions to ask the user: hospital line shown in two boxes (assumed yes), Rh text marker, emoji, pencil consistency (unverified claim), touch-target measurement on device.

## Skill repo (~/skills/multi-agent-collaborative-audit) state
Commit 34dd217 exists locally on main (v3 skill + redesigned README) but is NOT pushed (no GitHub credentials in the cloud sandbox: user runs `git push origin main`). Uncommitted since: scripts/fleet_review.sh, gemini-delegate relay, shared_relay.mjs (stdout.txt + timeout env), SKILL.md fleet-check rule, first_run_preflight.py, fleet.json gemini args, neutralised TEST_MANIFEST template. Ask before committing. Also pending: clinical sample data in scenario_and_callchain_engine.py and e2e_and_call_chain_testing.md should become project-supplied (packs carry directions only).
