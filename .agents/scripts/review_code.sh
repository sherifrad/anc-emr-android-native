#!/usr/bin/env bash
# Usage: review_code.sh <lane> <out_file> <focus: code|tests> <design_file> <file>...
# Purpose: read-only adversarial review of new or changed source files against the design.
#   lane  agy:<model> | opencode:<model> | alphacode
# agy reads the files itself (plan mode). opencode and alphacode get the files pasted in. No patient data is sent.
set -u
LANE="$1"; OUT="$2"; FOCUS="$3"; DESIGN="$4"; shift 4
REPO=/Users/sherifgamalradwan/anc-emr-android-native; cd "$REPO" || exit 1
mkdir -p "$(dirname "$OUT")"
if [ "$FOCUS" = tests ]; then
  TASK="You are an independent TEST reviewer; you did not write the code or the tests. Judge whether the tests would catch real defects: for each behaviour in the design that the files should satisfy, say whether a test would FAIL if that behaviour broke (name the test), list behaviours with no test, tests that pass whatever the code does (weak or tautological asserts), flaky timing, and missing negative cases. Lead with gaps."
else
  TASK="You are an independent CODE reviewer; you did not write the code. Find correctness bugs, violations of the design and of AGENTS.md rules (one canonical component, 360dp narrow viewport, hit areas of 48dp, no hard-coded colours, no business logic in composables, files under 1,500 lines), focus and Enter-to-next defects, state that is lost on rotation or recomposition, accessibility, RTL, and strings that are hard-coded. Lead with failures; for each give file:line and the concrete failing scenario."
fi
TASK="$TASK Plain markdown, under 900 words, severity-ranked (Blocker, Major, Minor). Do not edit any file."
case "$LANE" in
  agy:*) timeout 1500 agy --mode plan --model "${LANE#agy:}" --print="$TASK Design: $REPO/$DESIGN (read Technical Plan and Detailed Implementation items for these files). Files: $* . Read them yourself and cite lines you read." > "$OUT" 2> "$OUT.err";;
  opencode:*|alphacode)
    B="$(mktemp)"; { echo "$TASK You have no file access; everything is pasted below. Do not call any tool."; echo "=== DESIGN (numbered) ==="; cat -n "$DESIGN"
      for f in "$@"; do echo; echo "=== FILE $f (numbered) ==="; cat -n "$f"; done; echo; echo "Do not call any tool. Write the complete report now."; } > "$B"
    if [ "$LANE" = alphacode ]; then timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$B")" > "$OUT" 2> "$OUT.err"
    else timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$B")" > "$OUT" 2> "$OUT.err"; fi; rm -f "$B";;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
echo "$LANE -> $(wc -c < "$OUT") bytes"
