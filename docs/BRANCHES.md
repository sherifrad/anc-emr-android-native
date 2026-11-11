# Branches that hold work not yet in main

`.agents/scripts/git_audit.sh` lists every branch, worktree, stash, stray file and unreachable commit that is not in `main`.
A branch is listed here only when it is deliberate. One line each: `- <branch>: <why it is kept and what decides its fate>`.
Remove the line when the branch is merged or deleted.

- claude/clever-goldwasser-e086ce: the registration flow redesign (new patient entry screens, flow scaffold, step header and footer, patient editor groups). 14 reviewed commits from 2026-11-09 and 2026-11-10 plus one WIP commit with the work that was still uncommitted on 2026-11-11. Not built or merged against the Stage 1 changes (AncState split, medication sheet, delivery plan sheet); merging it is a separate task that conflicts with the state files. Worktree: .claude/worktrees/clever-goldwasser-e086ce.

Unreachable commits found on 2026-11-11 (old stashes and rewritten history) are kept alive under `refs/archive/unreachable-<hash>`; `git for-each-ref refs/archive` lists them. The visit entry work (df0bdba) is already in main as files.
