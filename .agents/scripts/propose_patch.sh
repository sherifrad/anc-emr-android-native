#!/usr/bin/env bash
# Usage: propose_patch.sh <lane> <out_patch> <task_file> <file>...
# Purpose: ask a read-only lane to propose SEARCH/REPLACE blocks for one bounded task (applied with apply_blocks.py). The lane gets the task text and the
#   numbered source files pasted into the prompt and has no file access, so it cannot edit anything.
#   Check the result with `apply_blocks.py --check <out>`, apply it, and run the gates.
#   lane  opencode:<model> | alphacode | agy:<model>
set -u
LANE="$1"; OUT="$2"; TASK="$3"; shift 3
REPO=/Users/sherifgamalradwan/anc-emr-android-native; cd "$REPO" || exit 1
mkdir -p "$(dirname "$OUT")"
B="$(mktemp)"
{ echo "You are a builder with no file access. Output ONLY edit blocks in this exact format, repeated as needed, inside one fenced block: a line FILE: <path relative to the repository root>, then a line <<<<<<< SEARCH, then the EXACT existing lines to replace copied character for character from the file below (enough lines that the text occurs exactly once in the file), then a line =======, then the replacement lines, then a line >>>>>>> REPLACE. To add new test functions use a SEARCH that quotes the last existing lines of the class and repeat them in the replacement before the new code. This is a written plan of exact edits, not an applied change. Do not call any tool. Plain words in code and comments, no em dash character, no generic comments."
  echo; echo "=== TASK ==="; cat "$TASK"
  for f in "$@"; do echo; echo "=== FILE $f ==="; cat "$f"; done
  echo; echo "Now output the edit blocks only."; } > "$B"
case "$LANE" in
  opencode:*) timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$B")" > "$OUT.raw" 2> "$OUT.err";;
  alphacode) timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$B")" > "$OUT.raw" 2> "$OUT.err";;
  agy:*) timeout 1500 agy --mode plan --model "${LANE#agy:}" --print="$(cat "$B")" > "$OUT.raw" 2> "$OUT.err";;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
rm -f "$B"
python3 - "$OUT.raw" "$OUT" <<'PY'
import re,sys
t=open(sys.argv[1],encoding='utf-8',errors='replace').read()
m=re.search(r"```[a-z]*\n(.*?)```",t,re.S)
d=m.group(1) if m else (t if "<<<<<<< SEARCH" in t else "")
open(sys.argv[2],"w",encoding="utf-8").write(d if d.endswith("\n") or not d else d+"\n")
print("block lines:",len(d.splitlines()))
PY
