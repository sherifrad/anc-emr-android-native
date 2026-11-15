#!/usr/bin/env bash
# Usage: lane_build.sh <agy-model> <prompt_file> <out_file> <allowed_path_regex>
# Purpose: run one agy WRITING lane in the repo (owner approved --dangerously-skip-permissions for agy writing lanes only, 2026-10-31),
#   then revert every changed or new file whose path does not match <allowed_path_regex> (scope guard) and report what is left.
#   The prompt must carry the safety rules (no phone, no other apps, no git writes, no protected files). Output text goes to <out_file>.
set -u
MODEL="$1"; PROMPT="$2"; OUT="$3"; ALLOW="$4"
cd /Users/sherifgamalradwan/anc-emr-android-native || exit 1
mkdir -p "$(dirname "$OUT")"
before=$(git status --porcelain | sort)
timeout 3000 agy --dangerously-skip-permissions --model "$MODEL" --print="$(cat "$PROMPT")" > "$OUT" 2> "$OUT.err"
after=$(git status --porcelain | sort)
git status --porcelain | awk '{print $2}' | while read -r f; do
  if ! echo "$f" | grep -Eq "$ALLOW"; then
    if echo "$before" | grep -q " $f\$"; then continue; fi
    if git ls-files --error-unmatch "$f" >/dev/null 2>&1; then git checkout -- "$f"; echo "REVERTED out of scope: $f"; else rm -rf "$f"; echo "REMOVED out of scope: $f"; fi
  fi
done
echo "--- changed files kept"; git status --porcelain
echo "$MODEL -> $(wc -c < "$OUT") bytes"
