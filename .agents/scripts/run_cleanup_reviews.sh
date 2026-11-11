#!/usr/bin/env bash
# Usage: run_cleanup_reviews.sh <skill> <batch_dir> <out_dir> <laneA> <laneB> [parallel]
# Purpose: review every batch patch in <batch_dir> against one antislop skill, alternating two lanes (read-only), and write
#   one report per batch to <out_dir>. Uses antislop_review.sh. A batch whose report is empty is retried once on the other lane.
set -u
SKILL="$1"; BD="$2"; OUT="$3"; LA="$4"; LB="$5"; PAR="${6:-4}"
cd /Users/sherifgamalradwan/anc-emr-android-native || exit 1
mkdir -p "$OUT"
one() { b="$1"; skill="$2"; bd="$3"; out="$4"; la="$5"; lb="$6"; n=${b##*b}; n=${n%.patch}
  case $((10#$n % 2)) in 0) l1="$la"; l2="$lb";; *) l1="$lb"; l2="$la";; esac
  f="$out/b$n.md"
  .agents/scripts/antislop_review.sh "$l1" "$skill" "$f" "$b" >/dev/null 2>&1
  [ "$(wc -c < "$f" 2>/dev/null || echo 0)" -gt 300 ] || .agents/scripts/antislop_review.sh "$l2" "$skill" "$f" "$b" >/dev/null 2>&1
  echo "b$n $(wc -c < "$f" 2>/dev/null || echo 0) bytes"; }
export -f one
ls "$BD"/b*.patch | xargs -P "$PAR" -I{} bash -c 'one "$@"' _ {} "$SKILL" "$BD" "$OUT" "$LA" "$LB"
echo "ALL DONE $SKILL"
