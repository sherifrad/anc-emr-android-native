#!/usr/bin/env bash
# Usage: fix_tests_lane.sh <slice-number> <test-class-name>
# Purpose: send the failures of the last emulator run of <test-class-name> back to the Gemini Flash test author with fixed rules
#   (edit only that test file; real production defects must be reported, not hidden), then recompile and rerun the class.
set -eu
cd "$(git rev-parse --show-toplevel)"
N=$1; CLS=$2; P=docs/designs/redesign/out/prompt_tests_fix_s$N.md
{ cat <<EOT
Fix the failing tests listed below in app/src/androidTest/java/com/sherifradwan/ancnative/$CLS.kt ONLY (edit no other file, no git, no adb). Production code is reviewed; do not change it. Read docs/designs/redesign/out/build_s${N}_report.md and the production code to align each test with the real behaviour. Common causes: field/tab labels hidden from semantics (clearAndSetSemantics) so find nodes by testTag and contentDescription or use useUnmergedTree = true; a text appearing in more than one node (use onAllNodesWithText(...).onFirst() or the tagged node); content inside a scroll container needs performScrollTo before assertIsDisplayed, and only on nodes that exist at that step; a button whose text sits in a child node needs useUnmergedTree or assert on the contentDescription; collapsed sections must be opened first. Match strings with the exact string resources. If a failure shows a REAL production defect (a required control missing or behaving wrongly), do NOT weaken the test: describe it in the report. Keep a single setContent per test. Compile with ./gradlew --offline :app:compileDebugAndroidTestKotlin -q. Write a short note to docs/designs/redesign/out/tests_fix_s${N}_report.md.

FAILURES:
EOT
.agents/scripts/emulator_failures.py; } > $P
.agents/scripts/lane_build.sh gemini-3.8-flash-high $P docs/designs/redesign/out/tests_fix_s${N}_out.md "app/src/androidTest/java/com/sherifradwan/ancnative/$CLS\.kt|docs/designs/redesign/out/" 2>&1 | tail -1
./gradlew --offline :app:compileDebugAndroidTestKotlin -q 2>&1 | tail -4
timeout 540 .agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.$CLS 2>&1 | tail -1
.agents/scripts/emulator_failures.py | cut -c1-330
