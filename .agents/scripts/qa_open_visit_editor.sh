#!/usr/bin/env bash
# Usage: qa_open_visit_editor.sh <emulator-serial>
# Emulator only, .qa package, demo data, debug PIN 121212. Launches the app, enters PIN,
# opens workspace Visits tab, and opens the new Visit editor. Needs 1080x2400 @ density 480 (360 dp).
set -euo pipefail
cd "$(dirname "$0")/../.."
S="${1:-emulator-5554}"; T=.agents/scripts/tap_text_on_device.sh
adb -s "$S" shell am force-stop com.sherifradwan.anc.qa
adb -s "$S" shell am start -n com.sherifradwan.anc.qa/com.sherifradwan.ancnative.MainActivity >/dev/null
sleep 4
onpin(){ adb -s "$S" shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb -s "$S" shell grep -q pinKey-1 /sdcard/u.xml; }
for round in 1 2; do
  if onpin; then
    for d in 1 2 1 2 1 2; do $T "$S" "pinKey-$d" >/dev/null; sleep 0.3; done
    sleep 3
  fi
done
$T "$S" "Workspace" >/dev/null 2>&1 || true; sleep 2
$T "$S" "Visits" >/dev/null 2>&1 || true; sleep 1
$T "$S" "addVisit" >/dev/null; sleep 2
echo "Visit editor opened on $S"
