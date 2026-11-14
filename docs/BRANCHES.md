# Branches that hold work not yet in main

`.agents/scripts/git_audit.sh` lists every branch, worktree, stash, stray file and unreachable commit that is not in `main`.
A branch is listed here only when it is deliberate. One line each: `- <branch>: <why it is kept and what decides its fate>`.
Remove the line when the branch is merged or deleted.



Unreachable commits found on 2026-11-11 (old stashes and rewritten history) are kept alive under `refs/archive/unreachable-<hash>`; `git for-each-ref refs/archive` lists them. The visit entry work (df0bdba) is already in main as files.
