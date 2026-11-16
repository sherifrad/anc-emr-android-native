#!/usr/bin/env python3
"""Usage: emulator_failures.py
Purpose: print each failing test name and the first 380 characters of its failure from the last run_emulator_test_class.sh result."""
import glob, re
for f in glob.glob('app/build/outputs/androidTest-results/connected/debug/TEST-*.xml'):
    s = open(f).read()
    for m in re.finditer(r'<testcase name="([^"]+)"[^>]*?(?:/>|>(.*?)</testcase>)', s, re.S):
        if m.group(2) and 'failure' in m.group(2):
            print(m.group(1), '\n  ', re.sub(r'\s+', ' ', m.group(2))[:380])
