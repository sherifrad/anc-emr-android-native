#!/usr/bin/env bash
# Usage: launch_opus_review.sh <prompt.md> <output.txt> [timeout-seconds, default 1500]
# Read-only frontier review through claude -p (Read/Grep/Glob only). Runs detached; output file is empty until done.
# macOS has no `timeout`, so perl alarm is the wrapper.
set -euo pipefail
prompt="${1:?prompt file}"; out="${2:?output file}"; secs="${3:-1500}"
nohup perl -e 'alarm shift; exec @ARGV' "$secs" claude -p --model claude-opus-5-5 --allowedTools 'Read,Grep,Glob' < "$prompt" > "$out" 2>&1 &
echo "started pid $!; read $out when 'pgrep -f \"claude -p\"' prints nothing"
