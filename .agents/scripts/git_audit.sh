#!/usr/bin/env bash
# Usage: git_audit.sh [base-branch]   (default: main)
# Purpose: find work that exists in git but is not in the base branch, so a branch, a worktree edit, a stash or a stray
#   file is never forgotten. Read-only: it fetches and prints; it changes no branch, file or ref.
#   Lists: local and remote branches ahead of the base (by patch, so cherry-picked work counts as merged), every worktree
#   with its branch and uncommitted changes, stashes, untracked and modified files in the main tree, and unreachable commits.
#   Exit 0 only when nothing is found; exit 1 and a summary line otherwise.
set -u
BASE="${1:-main}"; cd "$(git rev-parse --show-toplevel)" || exit 1
issues=0; note(){ echo "$*"; issues=$((issues + 1)); }
git fetch --all --prune >/dev/null 2>&1 || echo "WARN fetch failed (offline?): remote data may be stale"
echo "== base $BASE: local $(git rev-parse --short "$BASE"), remote $(git rev-parse --short "origin/$BASE" 2>/dev/null || echo none)"
[ "$(git rev-list --count "origin/$BASE..$BASE" 2>/dev/null || echo 0)" = 0 ] || note "UNPUSHED: $BASE is ahead of origin/$BASE by $(git rev-list --count "origin/$BASE..$BASE")"
echo "== branches with work not in $BASE (by patch)"
for ref in $(git for-each-ref --format='%(refname:short)' refs/heads refs/remotes | grep -v -E "^(origin/HEAD|origin|$BASE|origin/$BASE)$"); do
  n=$(git cherry "$BASE" "$ref" 2>/dev/null | grep -c '^+')
  [ "$n" -gt 0 ] && note "UNMERGED $ref: $n commit(s) not in $BASE, last $(git log -1 --format='%ad %s' --date=short "$ref")"
done
echo "== worktrees"
git worktree list --porcelain | awk '/^worktree /{p=$2} /^branch /{b=$2} /^$/{if(p!="")print p" "b; p="";b=""} END{if(p!="")print p" "b}' | while read -r path br; do
  dirty=$(git -C "$path" status --porcelain 2>/dev/null | grep -v '^??' | wc -l | tr -d ' ')
  untr=$(git -C "$path" status --porcelain 2>/dev/null | grep -c '^??')
  echo "  $path ${br:-detached}  modified=$dirty untracked=$untr"
done
git worktree list --porcelain | awk '/^worktree /{print $2}' | while read -r path; do
  m=$(git -C "$path" status --porcelain 2>/dev/null | grep -v '^??' | wc -l | tr -d ' ')
  [ "$m" -gt 0 ] && echo "DIRTY worktree $path: $m modified file(s)"
done | tee /tmp/git_audit_dirty.$$ ; [ -s /tmp/git_audit_dirty.$$ ] && issues=$((issues + 1)); rm -f /tmp/git_audit_dirty.$$
echo "== stashes"; s=$(git stash list | wc -l | tr -d ' '); [ "$s" -gt 0 ] && { git stash list; note "STASH: $s stash(es)"; }
echo "== main tree: modified and untracked (ignored files excluded)"
st=$(git status --porcelain | grep -v '^$'); [ -n "$st" ] && { echo "$st"; note "UNCLEAN main tree: $(echo "$st" | wc -l | tr -d ' ') path(s)"; }
echo "== unreachable commits (not on any branch, tag or reflog)"
u=$(git fsck --no-reflogs --unreachable 2>/dev/null | awk '$2=="commit"{print $3}')
[ -n "$u" ] && { for c in $u; do git log -1 --format='  %h %ad %s' --date=short "$c"; done | head -20; note "UNREACHABLE: $(echo "$u" | wc -l | tr -d ' ') commit(s)"; }
[ "$issues" -eq 0 ] && echo "CLEAN: nothing outside $BASE" || echo "FOUND $issues issue group(s)"
[ "$issues" -eq 0 ]
