# Start prompt for zcode

Paste this to zcode. It replaces the "no maca skill" limitation in your report and confirms the plan.

## 1. The maca skill is now installed for you

I copied the multi-agent-collaborative-audit skill to `~/.agents/skills/multi-agent-collaborative-audit/` (the same place your `antislop` skills live). The source is `~/skills/multi-agent-collaborative-audit`. Do this first:

1. Read `~/.agents/skills/multi-agent-collaborative-audit/SKILL.md`, then `references/full_path.md` (the Full path applies: this project is safety-critical).
2. Check it works: `python3 ~/.agents/skills/multi-agent-collaborative-audit/scripts/project_map.py --self-test` must exit 0. If it does not, stop and tell me.
3. Run `/antislop` and `/maca` as your working rules for everything that follows.

## 2. Plan: your report is approved

Execute your own consolidated plan, `~/Downloads/handoff_claude_medication_stage1.md`, with the stages in its order: A, B, C, C2, C3, C4, D. The detail for each stage is in that file and in `docs/designs/handoff_zcode_medication_stage1.md`. Tasks B and C were approved by the owner on 2026-11-11. Read `AGENTS.md` and `docs/designs/medication_section_revision.md` first.

Current repository state (checked by me just now): `HEAD` is `76e1337`, the Phase 3 commit is `ab74d27`, the working tree has only the two old drafts modified (`concepts_brief.md`, `design_draft.md`; never commit them). Earlier today I started Stage B with a Claude subagent and stopped it before it finished. I reverted its uncommitted edits so you start from a clean tree. Its unfinished and unreviewed attempt is saved as `docs/designs/reviews/medication_section_revision/stageB/claude_partial_attempt.patch` (171 lines: `ClinicalCommands.kt`, `MedicationForm.kt`, `ClinicalCommandsTest.kt`, the design file). You may read it as a reference. It was not verified and nothing in it is trusted: treat it as an untested idea, not as a fix.

## 3. Team and independence

You are the builder. A reviewer must not be the same model family as the author of the change, and the test reviewer must not have written the tests.

- **alphacode**: reviewer and test reviewer. It sometimes returns an empty response: retry once, and record a failed or empty review as not run.
- **agy**: use any model that answers. Try `claude-opus-5-5-medium` first (its quota ran out on 2026-11-11 and resets after a few hours), then `claude-sonnet-5-5-medium`, then `gemini-3.8-flash-medium`. Do not use `gpt-oss-120b-medium`: it only returns a plan.
- **opencode glm** (`opencode:glm-5.3-flash-free`): you are GLM-5.3 yourself, so it must not review code you wrote. Use it for roles that keep independence: reading and summarising context, and reviewing code written by someone else (the Claude-written Phase 3 work in Stage A, and the partial attempt patch). For your own changes in B, C, C2, C3 and C4 use alphacode and agy.
- Level-3 paths (everything under `state/`, `storage/`, `calculation/`, and `ui/patienthub/PatientHubScreen.kt`, `ui/patienthub/PatientHubHeader.kt`) need a frontier-tier code review plus an independent test review. Stage B and Stage C are level 3. If no frontier model answers, say so in the commit message and in the closing report; do not claim one ran.

Use the existing script for every external review, do not retype it:

```bash
.agents/scripts/review_code.sh <lane> <out> <code|tests> docs/designs/medication_section_revision.md <files...>
```

Lanes: `agy:<model>`, `opencode:<model>`, `alphacode`. Put each report in `docs/designs/reviews/medication_section_revision/<stage>/`.

## 4. Rules that do not change

- Local commits only, never push. One commit per stage (B, C, C2, C3, C4), each after its review. Every commit message ends with the attribution line your harness gives you. No unrelated file enters a commit (the old drafts, the report files at the repo root, `.agents/harness/`, `.agents/ROUTING_LOG.jsonl`, `.agents/scripts/egm_*.sh`).
- Protected files stay untouched, except `storage/ClinicalCommands.kt` in Stage B only. Run the protected-file check after each stage.
- Run every command yourself and report the real output. Report a failed or skipped step as failed or skipped. The emulator tests must be run through `.agents/scripts/run_emulator_test_class.sh` and `.agents/scripts/run_emulator_all_tests.sh`, never plain `connectedDebugAndroidTest`.
- Stage B design first, code second. In Stage B the rule is: frequency required when creating a medication, not when updating, so a stored drug with no frequency can still be stopped in one tap and its Undo works.
- Stage A must include the frontier-tier review of the final Phase 3 round that is still missing, and your independent re-check of the two triages marked "do not take on trust" in your report.

## 5. What to send back

When all stages are green, or if you stop early, send me one report with: the commits made (hash and subject), every review with the lane and model that actually ran and its result, the exact verification output (build, unit test count, emulator total), the protected-file check output, what you could not verify, and open items. I will re-run the full gate myself before accepting it.
