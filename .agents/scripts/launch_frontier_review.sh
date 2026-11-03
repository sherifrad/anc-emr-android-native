#!/usr/bin/env bash
# Usage: launch_frontier_review.sh <engine> <prompt.md> <output.txt> [timeout-seconds, default 1500]
# Supported engines:
#   claude-opus   : claude-opus-5-5 via claude -p (read-only)
#   claude-sonnet : claude-sonnet-5-5 via claude -p (read-only)
#   deepseek      : opencode/deepseek-v4.1-flash-free via opencode run
#   glm           : opencode/glm-5.3-flash-free via opencode run
#   codex-o3      : o3 via codex exec (read-only sandbox)
#   codex-gpt4o   : gpt-4o via codex exec (read-only sandbox)
#   gemini-argon  : gemini-4-argon-high via agy
set -euo pipefail

engine="${1:?engine: claude-opus | claude-sonnet | deepseek | glm | codex-o3 | codex-gpt4o | gemini-argon}"
prompt="${2:?prompt file}"
out="${3:?output file}"
secs="${4:-1500}"

case "$engine" in
  claude-opus)
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" claude -p --model claude-opus-5-5 --allowedTools 'Read,Grep,Glob' < "$prompt" > "$out" 2>&1 &
    ;;
  claude-sonnet)
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" claude -p --model claude-sonnet-5-5 --allowedTools 'Read,Grep,Glob' < "$prompt" > "$out" 2>&1 &
    ;;
  deepseek)
    prompt_text="$(cat "$prompt")"
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" opencode run -m "opencode/deepseek-v4.1-flash-free" --format default "$prompt_text" > "$out" 2>&1 &
    ;;
  glm)
    prompt_text="$(cat "$prompt")"
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" opencode run -m "opencode/glm-5.3-flash-free" --format default "$prompt_text" > "$out" 2>&1 &
    ;;
  codex-o3)
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" codex exec -m "o3" -s read-only < "$prompt" > "$out" 2>&1 &
    ;;
  codex-gpt4o)
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" codex exec -m "gpt-4o" -s read-only < "$prompt" > "$out" 2>&1 &
    ;;
  gemini-argon)
    prompt_text="$(cat "$prompt")"
    nohup perl -e 'alarm shift; exec @ARGV' "$secs" agy --model "gemini-4-argon-high" "$prompt_text" > "$out" 2>&1 &
    ;;
  *)
    echo "Unknown engine: $engine" >&2
    exit 2
    ;;
esac

pid=$!
echo "started $engine review (pid $pid); output -> $out"
