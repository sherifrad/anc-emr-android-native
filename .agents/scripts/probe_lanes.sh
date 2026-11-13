#!/usr/bin/env bash
# Usage: probe_lanes.sh [out_dir] [lane...]
# Purpose: one-line availability probe of every external lane, in parallel. No benchmark: each lane is asked to answer "LANE_OK 7".
#   Prints one line per lane: OK <seconds>s, or FAIL with the first error line. Read-only: lanes get no file access and edit nothing.
#   With no lane arguments it probes alphacode, every agy model and every opencode free model.
set -u
OUT="${1:-/tmp/lane_probe}"; [ $# -gt 0 ] && shift
mkdir -p "$OUT"; rm -f "$OUT"/*.out "$OUT"/*.err "$OUT"/*.res
Q='Do not call any tool. Reply with exactly this one line and nothing else: LANE_OK followed by a space and the sum of 3 and 4.'
if [ $# -gt 0 ]; then LANES="$*"; else
  LANES="alphacode"
  for m in $(agy models 2>/dev/null | awk -F'\t' 'NF>1{print $1}'); do LANES="$LANES agy:$m"; done
  for m in $(opencode models 2>/dev/null | grep '^opencode/' | sed 's#^opencode/##'); do LANES="$LANES opencode:$m"; done
fi
probe() {
  lane="$1"; f="$OUT/$(echo "$lane" | tr ':/' '__')"; t0=$(date +%s)
  case "$lane" in
    opencode:*) timeout 150 opencode run -m "opencode/${lane#opencode:}" --agent plan "$Q" > "$f.out" 2> "$f.err";;
    alphacode) timeout 150 alphacode run --tool-profile none --quiet --no-update "$Q" > "$f.out" 2> "$f.err";;
    agy:*) timeout 150 agy --mode plan --model "${lane#agy:}" --print="$Q" > "$f.out" 2> "$f.err";;
  esac
  t=$(( $(date +%s) - t0 ))
  if grep -q "LANE_OK 7" "$f.out"; then echo "OK   $lane ${t}s" > "$f.res"
  else echo "FAIL $lane ${t}s: $(cat "$f.out" "$f.err" | grep -v '^$' | grep -i -E 'error|quota|limit|429|exhaust|unavailable|denied|not found' | head -1 | cut -c1-160)" > "$f.res"; fi
}
for l in $LANES; do probe "$l" & done; wait
cat "$OUT"/*.res | sort
