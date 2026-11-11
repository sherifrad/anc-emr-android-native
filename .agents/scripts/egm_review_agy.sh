#!/usr/bin/env bash
# Run EGM Goldfish reviewer personas on a non-Claude model via agy (read-only), one process per lens, in parallel.
# usage: egm_review_agy.sh <hypothesis.md> <out_dir> [model]
set -u
HYP="$1"; OUT="$2"; MODEL="${3:-gemini-3.8-flash-medium}"
PERSONAS=/Users/sherifgamalradwan/.claude/plugins/cache/egm/egm/0.1.1/agents
REPO=/Users/sherifgamalradwan/anc-emr-android-native
mkdir -p "$OUT"
cd "$REPO" || exit 1
for lens in design_critic codebase_details zoom_out comprehension implementation_readiness structural inventive_catalyst; do
  PROMPT="You are a read-only Goldfish reviewer. First read your persona file $PERSONAS/$lens.md and follow it. Mode: BRAINSTORM. Review this feature concept: read $HYP (it names the repo files to verify; read them). Do not edit any file. Print your findings as plain-English markdown to stdout: lead with what is wrong, missing or risky, give the strongest counter-argument, cite file paths you actually checked, and keep it under 600 words."
  ( agy --mode plan --model "$MODEL" --add-dir "$(dirname "$HYP")" --add-dir "$PERSONAS" --print="$PROMPT" > "$OUT/$lens.md" 2> "$OUT/$lens.err" ) &
done
wait
wc -c "$OUT"/*.md
