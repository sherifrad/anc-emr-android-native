#!/usr/bin/env bash
# Runs every .agents/checks/check_r*.py. Prints FAIL lines only. Some "no logic file changed" checks
# fail while changes are uncommitted and pass after the commit.
cd "$(dirname "$0")/../.."
fail=0
for f in .agents/checks/check_r*.py; do
  python3 "$f" >/dev/null 2>&1 || { echo "FAIL $f"; fail=1; }
done
[ "$fail" = 0 ] && echo "STATIC CHECKS OK"
exit $fail
