#!/usr/bin/env bash
# Usage: take_device_screenshot.sh <adb-serial> <output.png>
# Phone caveat: floating chat bubbles (Messenger etc.) sit on top of the app and swallow taps near the
# screen edge; ask the owner to remove them before tap tests, and never open or read other apps.
set -euo pipefail
adb -s "${1:?adb serial}" exec-out screencap -p > "${2:?output png}"
