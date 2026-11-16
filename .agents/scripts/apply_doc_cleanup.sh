#!/usr/bin/env bash
# Usage: apply_doc_cleanup.sh [--apply]
# Purpose: apply docs/cleanup/classification.tsv to the working tree. Default is a dry run that only counts.
#   DELETE -> git rm; ARCHIVE -> git mv under docs/archive/<same path>; KEEP untouched.
#   Also untracks the machine run folders (.agents/backup-*, .agents/pipeline/runs) and ignores them.
#   Nothing is committed or pushed; git history keeps every removed file.
set -eu
cd "$(git rev-parse --show-toplevel)"
APPLY=0; [ "${1:-}" = "--apply" ] && APPLY=1
del=0; arc=0
while IFS=$'\t' read -r path cls _; do
  [ -e "$path" ] || continue
  case "$cls" in
    DELETE) del=$((del+1)); [ $APPLY = 1 ] && git rm -q -f -- "$path" ;;
    ARCHIVE) arc=$((arc+1)); if [ $APPLY = 1 ]; then mkdir -p "docs/archive/$(dirname "$path")"; git mv -k -- "$path" "docs/archive/$path"; fi ;;
  esac
done < docs/cleanup/classification.tsv
runs=$(git ls-files '.agents/backup-*' '.agents/pipeline/runs' | wc -l)
echo "docs: delete=$del archive=$arc; machine run files tracked: $runs"
if [ $APPLY = 1 ]; then
  git ls-files -z '.agents/backup-*' '.agents/pipeline/runs' | xargs -0 git rm -q -f --cached --
  rm -rf .agents/backup-* .agents/pipeline/runs
  grep -qx '.agents/backup-*/' .gitignore || printf '\n# machine run output, never tracked\n.agents/backup-*/\n.agents/pipeline/runs/\n' >> .gitignore
  echo applied
fi
