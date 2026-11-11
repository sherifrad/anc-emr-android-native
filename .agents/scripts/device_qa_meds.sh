#!/usr/bin/env bash
# Usage: device_qa_meds.sh <adb-serial> <out_dir> [--system-checks]
# Purpose: drive the QA build (com.sherifradwan.anc.qa, fictional demo data) through the Meds tab flows with adb and
#   uiautomator, and write one PASS/FAIL/SKIP line per check to <out_dir>/results.txt. UI dumps (NN_name.xml) and
#   screenshots (NN_name.png) stay in <out_dir> on this machine.
# The script never enters a PIN and never touches a fingerprint prompt; it stops with exit 3 and a LOCKED line instead.
# Safety: without --system-checks only app-level actions run (taps, text typed into the app, back, swipes, dumps,
#   screenshots). With --system-checks rotation, dark mode and font scale are changed, only on an 'emulator-' serial,
#   and every setting is restored on exit. A real device is never given a system change.
set -u
S="${1:?adb serial}"; OUT="${2:?out dir}"; SYS="${3:-}"
cd "$(dirname "$0")/../.." || exit 1
PKG=com.sherifradwan.anc.qa; ACT=com.sherifradwan.ancnative.MainActivity
mkdir -p "$OUT"; : > "$OUT/results.txt"; N=0; FAILS=0
A() { adb -s "$S" "$@"; }

dump() {  # dump <name>: UI dump of all windows to $OUT/NN_name.xml, screenshot beside it
  N=$((N + 1)); CUR="$OUT/$(printf '%02d' "$N")_$1.xml"
  A shell uiautomator dump --windows /sdcard/qa.xml >/dev/null 2>&1; A pull /sdcard/qa.xml "$CUR" >/dev/null 2>&1
  A exec-out screencap -p > "${CUR%.xml}.png" 2>/dev/null
}
q() {  # q <python expr over list nodes (dicts with id,text,desc,bounds)> : prints result of the expression on $CUR
  python3 -I - "$CUR" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes = []
for n in ET.parse(sys.argv[1]).iter("node"):
    b = list(map(int, re.findall(r"\d+", n.get("bounds") or "0 0 0 0")))
    nodes.append({"id": n.get("resource-id") or "", "text": n.get("text") or "", "desc": n.get("content-desc") or "", "cls": (n.get("class") or "").split(".")[-1], "b": b})
print(eval(sys.argv[2], {"nodes": nodes}))
PY
}
has_text() { [ "$(q "any(n['text']=='$1' for n in nodes)")" = True ]; }   # the sheet window exposes text but no test tags
has_id() { [ "$(q "any(n['id']=='$1' for n in nodes)")" = True ]; }
tap_edit() {  # tap_edit <n> <text> : tap the nth EditText of the dump and type
  local xy; xy=$(q "[((n['b'][0]+n['b'][2])//2,(n['b'][1]+n['b'][3])//2) for n in nodes if n['cls']=='EditText'][$1]" 2>/dev/null) || return 1
  A shell input tap $(echo "$xy" | tr -d '(),') ; sleep 1; A shell input text "$2"; sleep 1; }
count_prefix() { q "sum(1 for n in nodes if n['id'].startswith('$1'))"; }
tap() { .agents/scripts/tap_text_on_device.sh "$S" "$1" >/dev/null 2>&1; local rc=$?; sleep "${2:-1.2}"; return $rc; }
type_into() { tap "$1" 1 && A shell input text "$2"; }  # types only when the field was found and tapped
swipe_node() {  # swipe_node <id> left|right : swipe along the middle of a node, any screen size
  local b; b=$(q "next(((n['b'][0]+n['b'][2])//2, (n['b'][1]+n['b'][3])//2, n['b'][0], n['b'][2]) for n in nodes if n['id']=='$1')" 2>/dev/null) || return 1
  [ "$b" = None ] && return 1
  python3 -I - "$b" "$2" <<'PY' | xargs -r adb -s "$S" shell input swipe
import sys, ast
cx, cy, x0, x1 = ast.literal_eval(sys.argv[1]); w = x1 - x0
a, b = (x0 + int(w * .85), x0 + int(w * .1)) if sys.argv[2] == "left" else (x0 + int(w * .1), x0 + int(w * .85))
print(a, cy, b, cy, 250)
PY
  sleep 1; }
check() { local id="$1" desc="$2"; shift 2; if "$@"; then echo "PASS $id $desc" >> "$OUT/results.txt"; else echo "FAIL $id $desc" >> "$OUT/results.txt"; FAILS=$((FAILS + 1)); fi; }
skip() { echo "SKIP $1 $2" >> "$OUT/results.txt"; }
screen_h() { A shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2; }
ime_top() { A shell dumpsys window windows 2>/dev/null | awk '/Window #.*InputMethod/{f=1} f&&/mFrame=/{print; exit}' | grep -oE '\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' | head -1 | grep -oE '[0-9]+' | sed -n 2p; }

# restore system settings on exit, only ever set on an emulator
restore() { [ "$SYS" = "--system-checks" ] && [[ "$S" == emulator-* ]] && { A shell settings put system accelerometer_rotation 1; A shell settings put system user_rotation 0; A shell cmd uimode night no; A shell settings put system font_scale 1.0; } >/dev/null 2>&1; }
trap restore EXIT

# Never restart the app: a restart locks it, and the unlock (PIN or fingerprint) is a person's step, not the script's.
A shell am start -n "$PKG/$ACT" >/dev/null; sleep 4
dump launch
if ! has_id patientListTable && has_id workspaceScreen; then   # already inside a workspace: go back to the list
  for i in 1 2 3; do A shell input keyevent 4; sleep 1.5; dump back_to_list; has_id patientListTable && break; done
fi
if ! has_id patientListTable; then
  echo "LOCKED $S: the QA app is not on the patient list (PIN or fingerprint screen). Unlock it by hand, leave it on the patient list, then run this script again." | tee -a "$OUT/results.txt"
  exit 3
fi
echo "PASS D00 app is on the patient list" >> "$OUT/results.txt"
tap patient-ANC-0001 3; dump workspace
for try in 1 2 3 4; do
  tap tab-meds 2 && break
  dump tabbar_try; swipe_node patientHubTabBar left
done
dump meds_tab
if has_id medSearchField; then tap medPickerToggle 1.5; dump meds_tab_collapsed; fi   # start from a collapsed picker
check D01 "Meds tab shows its content and the active title" bash -c "$(declare -f has_id q); CUR='$CUR'; has_id medsTabContent && has_id medsActiveTitle"
check D03 "picker is collapsed by default" bash -c "$(declare -f has_id q); CUR='$CUR'; has_id medPickerToggle && ! has_id medSearchField"
ACTIVE_BEFORE=$(count_prefix medication-)

tap medPickerToggle 1.5; dump picker_open
check D04 "expanding the picker shows the search field" bash -c "$(declare -f has_id q); CUR='$CUR'; has_id medSearchField"
type_into medSearchField fer; sleep 1.5; dump picker_search
TOP=$(q "next((n['b'][1] for n in nodes if ('keyboard_holder' in n['id'] or 'inputmethod' in n['id'].lower()) and n['b'][1] > 800), 0)" 2>/dev/null); [ -z "$TOP" -o "$TOP" = 0 ] && TOP=$(ime_top); H=$(screen_h)
if [ -n "$TOP" ] && [ "$TOP" -gt 0 ] 2>/dev/null; then
  check D05 "first matching tile is above the keyboard (keyboard top $TOP)" bash -c "$(declare -f q); CUR='$CUR'; [ \"\$(q \"min([n['b'][3] for n in nodes if n['id'].startswith('medTile-') and not n['id'].startswith('medTileCheck') and not n['id'].startswith('medTileMeta')] or [999999])\")\" -le $TOP ]"
else skip D05 "keyboard top could not be read; look at ${CUR%.xml}.png"; fi
A shell input keyevent 4; sleep 1

# add a tile that is not already active
TILE=$(q "next((n['id'] for n in nodes if n['id'].startswith('medTile-') and not n['id'].startswith(('medTileCheck','medTileMeta'))), '')")
if [ -n "$TILE" ]; then
  tap "$TILE" 1.8; dump sheet_open
  check D06 "tapping a tile opens the sheet with its values" bash -c "$(declare -f has_text q); CUR='$CUR'; has_text 'Save & add another' && has_text 'Once daily'"
  tap =Save 2.5; dump after_add
  AFTER=$(count_prefix medication-)
  check D07 "saving adds one row ($ACTIVE_BEFORE to $AFTER) and closes the sheet" bash -c "$(declare -f has_text q); CUR='$CUR'; ! has_text 'Save & add another' && [ $AFTER -gt $ACTIVE_BEFORE ]"
else skip D06 "no tile outside the already-active ones was found"; fi

# one-tap stop and undo
STOP=$(q "next((n['id'] for n in nodes if n['id'].startswith('stopMedication-')), '')")
if [ -n "$STOP" ]; then
  tap "$STOP" 1.5; dump stopped
  check D08 "one-tap stop shows the Undo bar" bash -c "$(declare -f has_id q); CUR='$CUR'; has_id medStopUndoBar"
  tap =Undo 2; dump undone   # exact text: the substring medStopUndo would hit the bar container, not its button
  check D09 "Undo removes the bar and restores the row" bash -c "$(declare -f has_id q); CUR='$CUR'; ! has_id medStopUndoBar && has_id '$STOP'"
  tap "$STOP" 1.5 || echo "FAIL D10 stop control missing after Undo" >> "$OUT/results.txt"; dump stopped_again
  BAR_SEEN=0; has_id medStopUndoBar && BAR_SEEN=1
  sleep 11; dump expired
  check D10 "the Undo bar was shown after the second stop and is gone after about 11 seconds" bash -c "$(declare -f has_id q); CUR='$CUR'; [ $BAR_SEEN = 1 ] && ! has_id medStopUndoBar"
else skip D08 "no active row with a stop control"; fi

# Show past keeps its state through an action
tap medShowPastToggle 1.5; dump past_open
check D11 "Show past toggle is present after opening" bash -c "$(declare -f has_id q); CUR='$CUR'; has_id medShowPastToggle"

# rejected save: custom drug without a frequency, then the touched Cancel dialog
A shell input swipe 540 600 540 1800 300; sleep 1
if tap addCustomMedicationBtn 1.8; then
  dump custom_sheet
  tap_edit 0 QaDrug; tap_edit 1 1mg; A shell input keyevent 4; sleep 1
  tap =Save 2; dump rejected
  check D12 "a rejected save keeps the sheet and shows the frequency error" bash -c "$(declare -f has_text q); CUR='$CUR'; has_text 'Save' && has_text 'Choose a frequency.'"
  check D13 "the frequency error is inside the screen" bash -c "$(declare -f q); CUR='$CUR'; [ \"\$(q \"next((n['b'][3] for n in nodes if n['text']=='Choose a frequency.'), 99999)\")\" -le $(screen_h) ]"
  tap =Cancel 1.5; dump discard
  check D14 "Cancel on a touched form asks before discarding" bash -c "$(declare -f has_text q); CUR='$CUR'; has_text 'Discard draft changes?'"
  tap =Discard 1.5; dump discarded
  check D15 "discarding closes the sheet and stays on the Meds tab" bash -c "$(declare -f has_text has_id q); CUR='$CUR'; ! has_text 'Save & add another' && has_id medsTabContent"
fi

if [ "$SYS" = "--system-checks" ]; then
  if [[ "$S" == emulator-* ]]; then
    tap addCustomMedicationBtn 1.8; tap medName 1; A shell input text QaRotate; sleep 1
    A shell settings put system accelerometer_rotation 0; A shell settings put system user_rotation 1; sleep 3; dump landscape
    check D22 "typed name survives rotation" bash -c "$(declare -f q); CUR='$CUR'; [ \"\$(q \"any('QaRotate' in n['text'] for n in nodes)\")\" = True ]"
    A shell settings put system user_rotation 0; sleep 2; A shell input keyevent 4; sleep 1; tap cancelMedication 1; tap medDiscardConfirm 1
    A shell cmd uimode night yes; sleep 3; dump dark; skip D23 "dark mode screenshot saved: ${CUR%.xml}.png (look at it)"; A shell cmd uimode night no
    A shell settings put system font_scale 2.0; sleep 3; tap patientHub 1 2>/dev/null; dump large_text
    check D24 "patient name is wide enough at 200 percent text" bash -c "$(declare -f q); CUR='$CUR'; [ \"\$(q \"next((n['b'][2]-n['b'][0] for n in nodes if n['id']=='hubPatientName'), 0)\")\" -ge 500 ]"
  else echo "REFUSED system checks on a real device ($S)" | tee -a "$OUT/results.txt"; fi
fi

echo "done: $(grep -c ^PASS "$OUT/results.txt") pass, $(grep -c ^FAIL "$OUT/results.txt") fail, $(grep -c ^SKIP "$OUT/results.txt") skip -> $OUT/results.txt"
[ "$FAILS" -eq 0 ]
