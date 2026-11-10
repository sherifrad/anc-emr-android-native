#!/usr/bin/env bash
# Usage: review_artifact.sh <lane> <out_file> <artifact> <requirements_file> [design_file]
# Purpose: read-only adversarial review of one artifact (for example an HTML mock) against a list of oracles.
#   lane  agy:<model> | opencode:<model> | alphacode
# agy reads the files itself (plan mode, read-only). opencode and alphacode get the text pasted into the prompt.
# Vendors only receive specs, mocks and code, never patient data. Exit 0 when a report was written.
set -u
LANE="$1"; OUT="$2"; ART="$3"; REQ="$4"; DESIGN="${5:-}"
REPO=/Users/sherifgamalradwan/anc-emr-android-native
cd "$REPO" || exit 1
mkdir -p "$(dirname "$OUT")"
TASK="You are an independent, adversarial reviewer. You did not build the artifact. Check the artifact against EVERY oracle in the requirements file by reading the code (you cannot run it). For each oracle ORACLE-1 to ORACLE-11 answer PASS, FAIL or CANNOT TELL with the exact lines or selectors that prove it, and the strongest reason it might still fail. Then list any other defects (bugs, accessibility, RTL, dark mode, keyboard handling, hardcoded colours outside CSS variables, hit areas under 48px, text that clips at 360px, logic that violates the design). Lead with failures. Plain markdown, under 900 words. Do not edit any file."
case "$LANE" in
  agy:*)
    timeout 1500 agy --mode plan --model "${LANE#agy:}" --print="$TASK Files: artifact $REPO/$ART ; requirements $REPO/$REQ ${DESIGN:+; design $REPO/$DESIGN (context only)}. Cite line numbers you actually read." > "$OUT" 2> "$OUT.err";;
  opencode:*|alphacode)
    BODY="$(mktemp)"
    { echo "$TASK You have no file access; everything is below. Do not call any tool; write the report as plain text now."
      echo; echo "=== REQUIREMENTS ==="; cat "$REQ"
      echo; echo "=== ARTIFACT ($ART), numbered lines ==="; cat -n "$ART"
      echo; echo "=== FINAL INSTRUCTION ==="; echo "Do not call any tool. Write the complete review report now."; } > "$BODY"
    if [ "$LANE" = alphacode ]; then timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$BODY")" > "$OUT" 2> "$OUT.err"
    else timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$BODY")" > "$OUT" 2> "$OUT.err"; fi
    rm -f "$BODY";;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
echo "$LANE -> $(wc -c < "$OUT") bytes"
