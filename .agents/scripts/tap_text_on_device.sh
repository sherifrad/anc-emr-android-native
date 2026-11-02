#!/usr/bin/env bash
# Usage: tap_text_on_device.sh <adb-serial> <substring of text, content-desc or resource-id>
# A leading '=' means exact text/content-desc match.
# Taps the centre of the first matching node from a uiautomator dump. Emulator/QA use only.
set -euo pipefail
S="${1:?serial}"; Q="${2:?text}"
adb -s "$S" shell uiautomator dump /sdcard/ui.xml >/dev/null
adb -s "$S" pull /sdcard/ui.xml /tmp/ui_$$.xml >/dev/null
python3 -I - "$Q" /tmp/ui_$$.xml <<'PY' | xargs -r adb -s "$S" shell input tap
import sys,re,xml.etree.ElementTree as ET
q=sys.argv[1]
exact=q.startswith('=')
q=q.lstrip('=')
for n in ET.parse(sys.argv[2]).iter('node'):
    t,c,r=(n.get('text') or ''),(n.get('content-desc') or ''),(n.get('resource-id') or '')
    if (q in (t,c)) if exact else (q in t or q in c or q in r):
        m=re.findall(r'\d+',n.get('bounds'));x0,y0,x1,y1=map(int,m)
        print((x0+x1)//2,(y0+y1)//2);break
else:
    sys.exit("not found: "+q)
PY
rm -f /tmp/ui_$$.xml
