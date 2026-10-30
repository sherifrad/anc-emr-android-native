#!/bin/zsh
# usage: tapid.sh SERIAL PATTERN  - taps the first node whose resource-id, text or content-desc contains PATTERN
A=/opt/homebrew/share/android-commandlinetools/platform-tools/adb; $A -s $1 shell uiautomator dump /sdcard/ui.xml >/dev/null; $A -s $1 pull /sdcard/ui.xml /tmp/claude-501/tap.xml >/dev/null 2>&1
xy=$(python3 -c "
import re,sys,xml.etree.ElementTree as ET
for n in ET.parse('/tmp/claude-501/tap.xml').iter('node'):
    if sys.argv[1] in (n.get('resource-id','')+'|'+n.get('text','')+'|'+n.get('content-desc','')):
        b=list(map(int,re.findall(r'\d+',n.get('bounds')))); print((b[0]+b[2])//2,(b[1]+b[3])//2); break
" "$2"); [ -n "$xy" ] && $A -s $1 shell input tap ${=xy} && echo "tapped $2 at $xy" || echo "NOT FOUND $2"
