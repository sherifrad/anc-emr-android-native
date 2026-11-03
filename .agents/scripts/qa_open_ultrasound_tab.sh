#!/usr/bin/env bash
# Usage: qa_open_ultrasound_tab.sh <emulator-serial>
# Emulator only, .qa package, demo data, debug PIN 121212. Launches the app, enters PIN if asked,
# opens workspace and taps Ultrasound tab.
set -euo pipefail
cd "$(dirname "$0")/../.."
S="${1:?serial}"; T=.agents/scripts/tap_text_on_device.sh
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
# Tab row is at Y=500. Swipe left once to bring Ultrasound into view.
adb -s "$S" shell input swipe 900 500 200 500 250
sleep 1
# Tap Ultrasound section tab
adb -s "$S" shell input tap 158 516
sleep 2
