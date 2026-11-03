#!/usr/bin/env bash
# Screenshots an HTML mock file at a given window size using headless Chrome.
# Usage: .agents/scripts/screenshot_mock_html.sh <html_path> <output_png_path> [width] [height]
set -euo pipefail

HTML_PATH="${1:-}"
OUTPUT_PNG="${2:-}"
WIDTH="${3:-1200}"
HEIGHT="${4:-900}"

if [ -z "$HTML_PATH" ] || [ -z "$OUTPUT_PNG" ]; then
    echo "Usage: $0 <html_path> <output_png_path> [width] [height]" >&2
    exit 1
fi

CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
if [ ! -x "$CHROME" ]; then
    echo "Chrome binary not found at $CHROME" >&2
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT_PNG")"
"$CHROME" --headless --disable-gpu --screenshot="$OUTPUT_PNG" --window-size="${WIDTH},${HEIGHT}" "file://$HTML_PATH"
echo "Screenshot saved to $OUTPUT_PNG"
