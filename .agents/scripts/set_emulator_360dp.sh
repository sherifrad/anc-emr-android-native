#!/usr/bin/env bash
set -euo pipefail
SERIAL="${1:-emulator-5554}"
adb -s "$SERIAL" shell wm size 1080x2400
adb -s "$SERIAL" shell wm density 480
echo "Emulator $SERIAL configured for 360 dp (1080x2400 @ 480 dpi)"
