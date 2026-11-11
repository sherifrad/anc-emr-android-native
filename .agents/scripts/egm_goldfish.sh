#!/usr/bin/env bash
# Run one EGM Goldfish reviewer on a non-Claude (or any) lane, read-only, and save its report.
# usage: egm_goldfish.sh <lane> <persona> <out_file> <target> <first_line> <last_line> [standard] [context_file]
#   lane    agy:<model>       reads persona and repo itself (agy --mode plan, read-only)
#           opencode:<model>  document and persona are pasted into the prompt (--agent plan)
#           alphacode         document and persona are pasted into the prompt (--tool-profile none)
#   persona file name in the egm agents folder without .md (design_critic, zoom_out, ...)
#   standard (optional) name in resources/document_standards without .md
# Vendors only receive the design text and code, never patient data.
set -u
LANE="$1"; PERSONA="$2"; OUT="$3"; TARGET="$4"; FIRST="$5"; LAST="$6"; STD="${7:-}"; CTX="${8:-}"
EGM=/Users/sherifgamalradwan/.claude/plugins/cache/egm/egm/0.1.1
REPO=/Users/sherifgamalradwan/anc-emr-android-native
cd "$REPO" || exit 1
mkdir -p "$(dirname "$OUT")"
STD_PATH=""; [ -n "$STD" ] && STD_PATH="$EGM/resources/document_standards/$STD.md"

case "$LANE" in
  agy:*)
    MODEL="${LANE#agy:}"
    PROMPT="You are a read-only Goldfish reviewer. First read your persona file $EGM/agents/$PERSONA.md and follow it. Mode: REVIEW. Target document: $REPO/$TARGET, review lines $FIRST to $LAST (read the whole file for context). ${STD_PATH:+Apply the document standard $STD_PATH. }${CTX:+Also read this earlier report for context: $CTX. }Verify claims about the code against the repository at $REPO (cite file paths and line numbers you actually opened). Do not edit or create any file. Print your complete report as plain markdown to stdout."
    timeout 1500 agy --mode plan --model "$MODEL" --add-dir "$EGM" --print="$PROMPT" > "$OUT" 2> "$OUT.err"
    ;;
  opencode:*|alphacode)
    BODY="$(mktemp)"
    {
      echo "You are a Goldfish reviewer with no file access. Follow the persona below exactly. Mode: REVIEW. Review lines $FIRST to $LAST of the design document (the whole document is given for context, with line numbers). You cannot open the code: when a claim about the code needs checking, write 'not verified' instead of guessing. Print your complete report as plain markdown."
      echo; echo "=== PERSONA ==="; awk '/^---$/ && c<2 {c++; next} c>=2' "$EGM/agents/$PERSONA.md"
      [ -n "$STD_PATH" ] && { echo; echo "=== DOCUMENT STANDARD ==="; cat "$STD_PATH"; }
      [ -n "$CTX" ] && { echo; echo "=== EARLIER REPORT (context) ==="; cat "$CTX"; }
      echo; echo "=== DESIGN DOCUMENT ($TARGET) ==="; cat -n "$TARGET"
      echo; echo "=== FINAL INSTRUCTION ==="; echo "Do not call any tool and do not write tool-call markup. The document above is everything you can see. Write your complete review report now as plain markdown."
    } > "$BODY"
    if [ "$LANE" = alphacode ]; then
      timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$BODY")" > "$OUT" 2> "$OUT.err"
    else
      timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$BODY")" > "$OUT" 2> "$OUT.err"
    fi
    rm -f "$BODY"
    ;;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
echo "$LANE $PERSONA -> $(wc -c < "$OUT") bytes"
