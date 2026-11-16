#!/usr/bin/env bash
# Usage: slice_checks.sh <slice-number> <slug> <test-class-name> <comma-list of level-3 files for the review>
# Purpose: after a lane build, write the diff, then start in parallel (a) the independent test author (Gemini Flash, writes
#   one androidTest class) and (b) the read-only alphacode diff review. Prints the diff path. The frontier review is started by Claude.
set -eu
cd "$(git rev-parse --show-toplevel)"
N=$1; SLUG=$2; CLS=$3; L3=$4
D=docs/designs/redesign/slice$N/out; mkdir -p $D
git add -N app docs 2>/dev/null || true
git diff HEAD -- app > $D/diff.patch
cat > $D/brief_review.md <<EOT
Read-only review of redesign slice $N (presentation-only restyle, brief docs/designs/redesign/slice_${N}_${SLUG}.md). Level-3 files: $L3. Diff below. Priorities: 1) clinical safety and behaviour parity: validation, save and cancel dispatch, calculations, stored codes, conditional rules, no field dropped or made uneditable, draft bindings; 2) regressions for other callers of shared components whose signatures changed (defaults must render as before); 3) accessibility (names, 48dp, 12sp, no duplicate reading); 4) hygiene (hardcoded strings, em dashes, dead code). Report only real defects with file:line and a concrete failure scenario, or PASS.
EOT
cat > docs/designs/redesign/out/prompt_tests_s$N.md <<EOT
You are the TEST AUTHOR (not the builder) for redesign slice $N in /Users/sherifgamalradwan/anc-emr-android-native. Edit ONLY app/src/androidTest/java/com/sherifradwan/ancnative/$CLS.kt (new) and, if a changed text or tag breaks an existing androidTest, only the minimal assertion in that existing test file. No production edits, no git, no adb/emulator.
Read first: docs/designs/redesign/slice_${N}_${SLUG}.md, docs/designs/redesign/out/build_s${N}_report.md (the "Test Author Reference" part is your oracle), and app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt and RegistrationRedesignTest.kt as the style template (single setContent per test, shared ComposeTextAsserts helpers, narrow 360dp window, fontScale overrides, useUnmergedTree where label text is cleared by clearAndSetSemantics, performScrollTo before assertIsDisplayed, select nodes by testTag and not by ambiguous text, >=47.99dp touch tolerance).
Cover: no text under 12sp; 48dp targets on every interactive element the build report lists; the new layout elements and strings; behaviour parity (same events dispatched, same stored values, conditional fields still appear/disappear, validation errors still show); no clipping at 360dp with fontScale 1.3 and 2.0; existing tags still present. Keep each test independent.
Compile ./gradlew --offline :app:compileDebugAndroidTestKotlin -q and fix compile errors. Write a short report to docs/designs/redesign/out/tests_s${N}_report.md.
EOT
.agents/scripts/lane_build.sh gemini-3.8-flash-high docs/designs/redesign/out/prompt_tests_s$N.md docs/designs/redesign/out/tests_s${N}_out.md "app/src/androidTest/java/com/sherifradwan/ancnative/|docs/designs/redesign/out/" > /tmp/s${N}_tests.log 2>&1 &
.agents/scripts/lane_ask.sh alphacode $D/brief_review.md $D/review_alphacode.md $D/diff.patch > /dev/null 2>&1 &
echo "diff: $D/diff.patch ($(wc -l < $D/diff.patch) lines)"
