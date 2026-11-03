import subprocess
import re
import sys
import os
import time

SERIAL = "AX3C025527011668"
PACKAGE = "com.sherifradwan.anc.qa"
DEVTEST_DIR = os.path.dirname(os.path.abspath(__file__))
XML_LOCAL = os.path.join(DEVTEST_DIR, "w.xml")

def run_adb(args, check_focus=True):
    cmd = ["adb", "-s", SERIAL] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res

def get_focus():
    res = subprocess.run(["adb", "-s", SERIAL, "shell", "dumpsys", "window"], capture_output=True, text=True)
    for line in res.stdout.splitlines():
        if "mCurrentFocus" in line:
            return line.strip()
    return ""

def verify_qa_focus():
    focus = get_focus()
    if PACKAGE not in focus:
        print(f"WARNING: Focus is NOT {PACKAGE}: '{focus}'. Relaunching QA app...", file=sys.stderr)
        subprocess.run(["adb", "-s", SERIAL, "shell", "am", "start", "-n", f"{PACKAGE}/com.sherifradwan.ancnative.MainActivity"], capture_output=True)
        time.sleep(1)
        focus = get_focus()
        if PACKAGE not in focus:
            raise RuntimeError(f"CRITICAL SAFETY VIOLATION: Current focus is '{focus}' (not {PACKAGE})!")
    return focus

def post_tap_check():
    focus = get_focus()
    if PACKAGE not in focus:
        print(f"ALERT: Package left QA! Current focus: {focus}. Sending KEYCODE_BACK once.", file=sys.stderr)
        subprocess.run(["adb", "-s", SERIAL, "shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(1)
        new_focus = get_focus()
        raise RuntimeError(f"INCIDENT: Focus changed to '{focus}'. After KEYCODE_BACK: '{new_focus}'")

def dump_ui():
    verify_qa_focus()
    subprocess.run(["adb", "-s", SERIAL, "shell", "uiautomator", "dump", "/sdcard/qa_w.xml"], capture_output=True)
    subprocess.run(["adb", "-s", SERIAL, "pull", "/sdcard/qa_w.xml", XML_LOCAL], capture_output=True)
    if not os.path.exists(XML_LOCAL):
        return []
    with open(XML_LOCAL, "r", encoding="utf-8") as f:
        xml_content = f.read()

    elements = []
    for m in re.finditer(r'<node [^>]*>', xml_content):
        node = m.group(0)
        if f'package="{PACKAGE}"' not in node:
            continue
        def get_attr(k):
            match = re.search(k + r'="([^"]*)"', node)
            return match.group(1) if match else ""
        
        rid = get_attr("resource-id")
        text = get_attr("text")
        desc = get_attr("content-desc")
        bounds_str = get_attr("bounds")
        nums = list(map(int, re.findall(r'\d+', bounds_str)))
        if len(nums) == 4:
            elements.append((rid, text, desc, nums))
    return elements

FORBIDDEN_TEXT_PATTERNS = [
    r'^(\+?20)?[0-9]{10,11}$',
    r'^[0-9]{8,}$',
    r'(?i)\b(call|dial|share|email|open with)\b'
]

def is_forbidden_target(text, desc):
    comb = f"{text} {desc}".strip()
    for pat in FORBIDDEN_TEXT_PATTERNS:
        if re.search(pat, comb):
            return True
    return False

def tap_element(q, prefer_left=True):
    verify_qa_focus()
    elements = dump_ui()
    matched = []
    for rid, text, desc, b in elements:
        if q == rid or q == text or q == desc or (q in text and len(q) > 3) or (q in desc and len(q) > 3):
            matched.append((rid, text, desc, b))
    
    if not matched:
        print(f"FAILED to find element matching '{q}'")
        return False
    
    # Pick target
    target = matched[0]
    rid, text, desc, b = target
    
    if is_forbidden_target(text, desc):
        raise ValueError(f"CRITICAL SAFETY VIOLATION: Attempted tap on forbidden element text='{text}', desc='{desc}'")
    
    # Calculate tap point
    # Honor 400 screen width: 1264, rule 4: x < 632
    # If element bounds span across left half (b[0] < 632), tap within the left half!
    if b[0] >= 632:
        print(f"BLOCKED: Element '{q}' is entirely on the right half of the screen ({b}). Taps allowed only x < 632.")
        return False
    
    # Tap in the left portion of the element
    tap_x = min((b[0] + b[2]) // 2, 600)
    tap_y = (b[1] + b[3]) // 2
    
    print(f"Tapping '{q}' at ({tap_x}, {tap_y}) [bounds={b}, text='{text}', rid='{rid}']")
    subprocess.run(["adb", "-s", SERIAL, "shell", "input", "tap", str(tap_x), str(tap_y)])
    time.sleep(0.5)
    post_tap_check()
    return True

def tap_coords(x, y):
    verify_qa_focus()
    if x >= 632:
        print(f"BLOCKED: Tap coords ({x}, {y}) violate x < 632 rule.")
        return False
    subprocess.run(["adb", "-s", SERIAL, "shell", "input", "tap", str(x), str(y)])
    time.sleep(0.5)
    post_tap_check()
    return True

def take_screenshot(name):
    verify_qa_focus()
    shots_dir = os.path.join(DEVTEST_DIR, "shots")
    os.makedirs(shots_dir, exist_ok=True)
    out_path = os.path.join(shots_dir, name)
    with open(out_path, "wb") as f:
        res = subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=f)
    print(f"Saved screenshot: {out_path}")
    return out_path

def input_text(txt):
    verify_qa_focus()
    # Replace spaces with %s
    escaped = txt.replace(" ", "%s")
    subprocess.run(["adb", "-s", SERIAL, "shell", "input", "text", escaped])
    time.sleep(0.3)
    post_tap_check()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: devtest_helper.py [dump|tap <q>|tap_xy <x> <y>|shot <name>|text <txt>|focus]")
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "dump":
        elems = dump_ui()
        for rid, t, d, b in elems:
            if rid or t or d:
                print(f"{rid} | {t[:40]} | {d[:30]} | {b}")
    elif cmd == "tap":
        tap_element(sys.argv[2])
    elif cmd == "tap_xy":
        tap_coords(int(sys.argv[2]), int(sys.argv[3]))
    elif cmd == "shot":
        take_screenshot(sys.argv[2])
    elif cmd == "text":
        input_text(sys.argv[2])
    elif cmd == "focus":
        print(verify_qa_focus())
