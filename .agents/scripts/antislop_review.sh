#!/usr/bin/env bash
# Usage: antislop_review.sh <lane> <skill_name> <out_file> <patch_file>
# Purpose: read-only review of a patch against one antislop skill (antislop, antislop-code, antislop-copywriting,
#   antislop-human, antislop-layoutmobile, antislop-ui). The skill text and the patch are pasted into the prompt.
#   lane  opencode:<model> | alphacode | agy:<model>. The lane has no file access and edits nothing.
set -u
LANE="$1"; SKILL="$2"; OUT="$3"; PATCH="$4"
REPO=/Users/sherifgamalradwan/anc-emr-android-native; cd "$REPO" || exit 1
SK="$HOME/.claude/skills/$SKILL/SKILL.md"; [ -f "$SK" ] || { echo "no skill $SKILL" >&2; exit 2; }
mkdir -p "$(dirname "$OUT")"; B="$(mktemp)"
{ echo "You are an independent reviewer with no file access. Below are (1) the rules of the '$SKILL' skill and (2) a unified diff of an Android Kotlin/Compose app. Review ONLY the lines the diff adds (lines starting with +) against the skill's rules. Report each finding as: severity (critical, major or minor), file and the diff line, the quoted text, the rule it breaks, and the exact fix. Critical or major means a real defect a user or maintainer would hit (a misleading or wrong string, an unreadable or untappable control, a comment that is false or generic filler that hides intent, an em dash character in user-visible text). Minor is taste. Do not report style you cannot tie to a rule in the skill. Lead with critical and major. If you find none, say so plainly. Plain markdown under 700 words. Do not call any tool. Do not edit any file."
  echo; echo "=== SKILL RULES: $SKILL ==="; cat "$SK"
  echo; echo "=== DIFF ==="; cat "$PATCH"
  echo; echo "Write the report now."; } > "$B"
case "$LANE" in
  opencode:*) timeout 1500 opencode run -m "opencode/${LANE#opencode:}" --agent plan "$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  alphacode) timeout 1500 alphacode run --tool-profile none --quiet --no-update "$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  agy:*) timeout 1500 agy --mode plan --model "${LANE#agy:}" --print="$(cat "$B")" > "$OUT" 2> "$OUT.err";;
  *) echo "unknown lane $LANE" >&2; exit 2;;
esac
rm -f "$B"; echo "$LANE $SKILL -> $(wc -c < "$OUT") bytes"
