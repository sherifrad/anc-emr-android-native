#!/usr/bin/env bash
# Usage: run_emulator_all_tests.sh [adb-serial, default emulator-5554]
# Purpose: applies 360 dp (restored on exit), runs the whole instrumented suite on the QA package (.qa suffix, never the clinic app) and prints per-class counts and failing test names.
set -uo pipefail
cd "$(dirname "$0")/../.."
serial="${1:-emulator-5554}"
# The narrow-screen tests need 360 dp: apply it for the run, then put back the size and density the emulator had.
old_size=$(adb -s "$serial" shell wm size | sed -n 's/^Override size: //p' | tr -d '\r')
old_density=$(adb -s "$serial" shell wm density | sed -n 's/^Override density: //p' | tr -d '\r')
restore() {
  if [ -n "$old_size" ]; then adb -s "$serial" shell wm size "$old_size"; else adb -s "$serial" shell wm size reset; fi
  if [ -n "$old_density" ]; then adb -s "$serial" shell wm density "$old_density"; else adb -s "$serial" shell wm density reset; fi
}
trap restore EXIT
.agents/scripts/set_emulator_360dp.sh "$serial" >/dev/null
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
