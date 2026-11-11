#!/usr/bin/env bash
# Usage: lane_ask.sh <lane> <prompt_file> <out_file> [file...]
# Purpose: send one read-only prompt (plus numbered files pasted below it) to an external lane and save the answer.
#   lane  opencode:<model> | alphacode | agy:<model>. The lane has no file access and edits nothing.
set -u
LANE="$1"; PROMPT="$2"; OUT="$3"; shift 3
cd /Users/sherifgamalradwan/anc-emr-android-native || exit 1
mkdir -p "$(dirname "$OUT")"; B="$(mktemp)"
{ cat "$PROMPT"; for f in "$@"; do echo; echo "=== FILE $f (numbered) ==="; cat -n "$f"; done; echo; echo "Do not call any tool. Write the full answer now as plain text."; } > "$B"
case "$LANE" in
  opencode:*) timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  alphacode) timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  agy:*) timeout 1500 agy --mode plan --model "${LANE#agy:}" --print="$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
rm -f "$B"; echo "$LANE -> $(wc -c < "$OUT") bytes"
