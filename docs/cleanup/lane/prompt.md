You classify documentation files of an Android clinical EMR repo (ANC pregnancy app) for a cleanup. The repo has accumulated handoffs, task logs, run reports, review outputs, briefs and plans from many AI sessions. The owner wants ONE source of truth for progress and a clean tree. Git history keeps everything, so deleting from the tree loses nothing permanent.
Input below: tab-separated rows: path, bytes, last_commit date, mentions (how many other docs name this file), title line. Judge from path, title, date, size and mentions only.
Give each row exactly one class:
KEEP = living reference that people or tools still read (project constitution, profile, AGENTS-style rules, active plan/roadmap, redesign PLAN and slice briefs and approved mocks, BRANCHES, GO_LIVE_CHECKLIST, device test plan, owner decisions, specs still describing current behaviour).
MERGE = contains progress, decisions or status that must be folded into the single progress file before the file is removed (handoff ledgers, current_task, per-mission handoffs, decision logs).
ARCHIVE = finished-work record worth keeping for audit but not in the active tree (completed reviews, validation reports, design docs for shipped features, old plans).
DELETE = machine output, round prompts/briefs for finished rounds, duplicated or superseded files, raw lane outputs, scratch, backups.
Output ONLY lines: path<TAB>CLASS<TAB>short reason (max 12 words). One line per input row, same order, no header, no extra text.
