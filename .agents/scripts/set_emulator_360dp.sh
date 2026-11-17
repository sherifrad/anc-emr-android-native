#!/usr/bin/env bash
set -euo pipefail
SERIAL="${1:-emulator-5554}"
adb -s "$SERIAL" shell wm size 1080x2400
adb -s "$SERIAL" shell wm density 480
echo "Emulator $SERIAL configured for 360 dp (1080x2400 @ 480 dpi)"
# Test baseline for a freshly booted emulator: system font 1.0, no animations, no soft keyboard (the suite expects IME height 0).
adb -s "$SERIAL" shell settings put system font_scale 1.0
adb -s "$SERIAL" shell settings put global window_animation_scale 0
adb -s "$SERIAL" shell settings put global transition_animation_scale 0
adb -s "$SERIAL" shell settings put global animator_duration_scale 0
adb -s "$SERIAL" shell ime disable com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME >/dev/null 2>&1 || true
