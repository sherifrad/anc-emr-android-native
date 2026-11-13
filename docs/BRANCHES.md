# Branches that hold work not yet in main

`.agents/scripts/git_audit.sh` lists every branch, worktree, stash, stray file and unreachable commit that is not in `main`.
A branch is listed here only when it is deliberate. One line each: `- <branch>: <why it is kept and what decides its fate>`.
Remove the line when the branch is merged or deleted.

- export-rework: rework of the PDF export and share text (owner sign-off 2026-11-13). WIP: the page and the shared rules are built; older report tests are still being brought to the new behaviour. Merge to main when the full gate passes and the owner has seen the PDF.


Unreachable commits found on 2026-11-11 (old stashes and rewritten history) are kept alive under `refs/archive/unreachable-<hash>`; `git for-each-ref refs/archive` lists them. The visit entry work (df0bdba) is already in main as files.
