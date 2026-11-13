#!/usr/bin/env bash
# Usage: push_gate.sh pre|post
# Purpose: pre: the checks that must pass before `git push origin main`. post: confirm the remote has exactly local HEAD.
#   pre runs the unit gate and the full emulator suite, checks protected files against the Stage 1 start (STAGE_BASE, default c9c1a82~1), and checks the tracked tree is clean.
#   Exit 0 only if every check passed. Prints one PASS/FAIL line per check.
set -u
REPO=/Users/sherifgamalradwan/anc-emr-android-native; cd "$REPO" || exit 1
fail=0; ok(){ echo "PASS $1"; }; bad(){ echo "FAIL $1"; fail=1; }
case "${1:-}" in
  pre)
    git diff --quiet && git diff --cached --quiet && ok "tracked tree clean" || { git status --short | grep -v '^??' | grep -v -E 'concepts_brief.md|design_draft.md' | grep -q . && bad "tracked tree has changes" || ok "tracked tree clean (two old drafts ignored)"; }
    echo "HEAD $(git rev-parse --short HEAD), origin/main $(git rev-parse --short origin/main), ahead $(git rev-list --count origin/main..HEAD)"
    base="${STAGE_BASE:-c9c1a82~1}"
    prot=$(git diff --name-only "$base"..HEAD -- app | grep -E "schemas|RecordsDatabase|RecordEntities|RecordsDao|RecordsRepository|UndoCoordinator|ClinicalModels|Generator" || true)
    [ -z "$prot" ] && ok "protected files unchanged since $base (Stage 1 start)" || { echo "$prot"; bad "protected files changed"; }
    ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks > /tmp/push_gate_unit.log 2>&1 && ok "assemble, unit tests, lint" || bad "assemble, unit tests, lint (see /tmp/push_gate_unit.log)"
    ./gradlew :app:compileDebugAndroidTestKotlin > /tmp/push_gate_compile.log 2>&1 && ok "androidTest compile" || bad "androidTest compile"
    .agents/scripts/run_emulator_all_tests.sh > /tmp/push_gate_emulator.log 2>&1; tail -3 /tmp/push_gate_emulator.log | grep TOTAL
    # Zero tests means no emulator was attached: that is a failed gate, not a pass.
    if grep -q "TOTAL tests=0 " /tmp/push_gate_emulator.log || ! grep -q "TOTAL tests=" /tmp/push_gate_emulator.log; then bad "emulator suite ran no tests (is emulator-5554 running?)"
    elif grep -q "failures=0" /tmp/push_gate_emulator.log; then ok "emulator suite, 0 failures"
    else bad "emulator suite (see /tmp/push_gate_emulator.log)"; fi;;
  post)
    git fetch origin > /dev/null 2>&1
    [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] && ok "origin/main equals local HEAD ($(git rev-parse --short HEAD))" || bad "origin/main differs from local HEAD"
    [ "$(git ls-remote origin refs/heads/main | cut -c1-40)" = "$(git rev-parse HEAD)" ] && ok "remote ref read back matches" || bad "remote ref differs"
    [ "$(git rev-list --count origin/main..HEAD)" = "0" ] && ok "nothing left unpushed" || bad "commits left unpushed";;
  *) echo "usage: push_gate.sh pre|post" >&2; exit 2;;
esac
exit $fail
