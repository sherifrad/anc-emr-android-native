#!/usr/bin/env bash
# Usage: qa_emulator_delete_first_medication.sh <emulator-serial> <out.png>
# Emulator only (1080x2400 @ 480 = 360 dp), .qa package. Opens the Meds tab, taps the first card's trash icon,
# confirms the dialog and screenshots the Undo bar. Coordinates match the collapsed first card of the demo patient.
set -euo pipefail
cd "$(dirname "$0")/../.."
S="${1:?serial}"; OUT="${2:?png}"
.agents/scripts/qa_open_medication_tab.sh "$S" >/dev/null 2>&1
adb -s "$S" shell input tap 816 1396; sleep 1.5
adb -s "$S" shell input tap 802 1482; sleep 1.2
.agents/scripts/take_device_screenshot.sh "$S" "$OUT"
