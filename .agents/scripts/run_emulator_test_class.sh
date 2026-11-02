#!/usr/bin/env bash
# Usage: run_emulator_test_class.sh <fully.qualified.TestClass> [adb-serial, default emulator-5554]
# Runs one instrumented class on the QA package (.qa suffix, never the clinic app) and prints the counts.
set -uo pipefail
cd "$(dirname "$0")/../.."
cls="${1:?test class required}"; serial="${2:-emulator-5554}"
ANDROID_SERIAL="$serial" ./gradlew --offline -I .agents/scripts/qa_package_suffix.init.gradle \
  :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$cls" -q
code=$?
python3 - <<'PY'
import glob, re
for f in glob.glob('app/build/outputs/androidTest-results/connected/debug/TEST-*.xml'):
    m = re.search(r'tests="(\d+)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"', open(f).read(800))
    if m: print("tests=%s failures=%s errors=%s  (%s)" % (*m.groups(), f.split('/')[-1]))
PY
exit $code
