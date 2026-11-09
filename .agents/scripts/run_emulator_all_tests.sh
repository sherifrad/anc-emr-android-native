#!/usr/bin/env bash
# Usage: run_emulator_all_tests.sh [adb-serial, default emulator-5554]
# Purpose: runs the whole instrumented suite on the QA package (.qa suffix, never the clinic app) and prints per-class counts and failing test names.
set -uo pipefail
cd "$(dirname "$0")/../.."
serial="${1:-emulator-5554}"
rm -rf app/build/outputs/androidTest-results/connected
ANDROID_SERIAL="$serial" ./gradlew --offline -I .agents/scripts/qa_package_suffix.init.gradle \
  :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --continue -q
code=$?
python3 - <<'PY'
import glob, re, xml.etree.ElementTree as ET
tot = fail = 0
for f in sorted(glob.glob('app/build/outputs/androidTest-results/connected/**/TEST-*.xml', recursive=True)):
    root = ET.parse(f).getroot()
    for tc in root.iter('testcase'):
        tot += 1
        bad = tc.find('failure') is not None or tc.find('error') is not None
        if bad:
            fail += 1
            print("FAIL %s.%s" % (tc.get('classname').split('.')[-1], tc.get('name')))
print("TOTAL tests=%d failures=%d" % (tot, fail))
PY
exit $code
