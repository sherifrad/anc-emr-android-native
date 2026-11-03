#!/usr/bin/env bash
set -euo pipefail
SERIAL="${1:-emulator-5554}"
adb -s "$SERIAL" shell wm size reset
adb -s "$SERIAL" shell wm density reset
echo "Emulator $SERIAL density and size reset to hardware default"
