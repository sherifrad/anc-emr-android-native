#!/usr/bin/env bash
# Usage: device_qa_prepush.sh <adb-serial> <out_dir>
# Purpose: non-destructive pre-push smoke test of the redesigned UI for com.sherifradwan.anc.qa on a real phone
#   (1264x2736 @ 560 dpi, 1 dp = 3.5 px, 48 dp = 168 px). Writes one PASS/FAIL/SKIP line per check to
#   <out_dir>/results.txt and keeps UI dumps (NN_name.xml) and screenshots (NN_name.png) in <out_dir>.
#   Exit codes: 0 = all pass (no FAIL), 1 = any FAIL, 3 = device/app locked (PIN or fingerprint).
# Safety:
#   - Touches ONLY com.sherifradwan.anc.qa (activity com.sherifradwan.ancnative.MainActivity).
#   - Never changes phone or system settings (no wm size/density, no settings put, no rotation, no dark mode, no font scale).
#   - Never uninstalls or clears app data.
#   - Never enters a PIN or touches a fingerprint prompt (writes LOCKED and exits 3 instead).
#   - Never presses Save, Delete, Confirm, Archive, Submit, Print, Share, Export, Restore, Clear, Reset, Lock, Replace, or PIN buttons;
#     leaves editors with Back/Cancel (taps Discard only for an unsaved draft opened by this script).
#   - Never types into patient fields except the patient search box (and clears it).
#   - Never opens or screenshots any other app; brings com.sherifradwan.anc.qa forward before screencap and reports overlays.
set -u

S="${1:?Usage: device_qa_prepush.sh <adb-serial> <out_dir>}"
OUT="${2:?Usage: device_qa_prepush.sh <adb-serial> <out_dir>}"

PKG="com.sherifradwan.anc.qa"
ACT="com.sherifradwan.ancnative.MainActivity"
MIN_TARGET_PX=168      # 48 dp * 3.5 px/dp
ARABIC_PAD_MAX_PX=84   # 24 dp * 3.5 px/dp
REMOTE_XML="/sdcard/qa_prepush_ui.xml"

mkdir -p "$OUT"
: > "$OUT/results.txt"

ADB_TIMEOUT_SEC="${ADB_TIMEOUT_SEC:-20}"

N=0
FAILS=0
CUR=""
CUR_PNG=""
OVERLAY_PKGS=""
PATIENT_NAME=""
SCRIPT_OPENED_EDITOR=0
ON_PATIENT_LIST=0

run_with_timeout() {
  local sec="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "$sec" "$@"
  else
    perl -e 'alarm shift; exec @ARGV' "$sec" "$@"
  fi
}

A() { run_with_timeout "$ADB_TIMEOUT_SEC" adb -s "$S" "$@"; }

cleanup() {
  run_with_timeout 5 adb -s "$S" shell rm -f "$REMOTE_XML" >/dev/null 2>&1 || true
}
trap cleanup EXIT

pass_check() {
  local id="$1" msg="$2"
  echo "PASS $id $msg" | tee -a "$OUT/results.txt"
}

fail_check() {
  local id="$1" msg="$2"
  echo "FAIL $id $msg" | tee -a "$OUT/results.txt"
  FAILS=$((FAILS + 1))
}

skip_check() {
  local id="$1" msg="$2"
  echo "SKIP $id $msg" | tee -a "$OUT/results.txt"
}

locked_exit() {
  local reason="${1:-PIN, vault, or biometric lock screen detected}"
  echo "LOCKED $S: $reason. Unlock manually and leave on patient list, then re-run." | tee -a "$OUT/results.txt"
  exit 3
}

# Check dumpsys window for keyguard/lockscreen/bouncer flags or windows before any tap or launch.
check_dumpsys_lock() {
  local dw_out lock_reason
  dw_out=$(A shell dumpsys window 2>/dev/null || true)
  [ -n "$dw_out" ] || return 0
  lock_reason=$(printf '%s' "$dw_out" | python3 -I - <<'PY'
import re, sys

out = sys.stdin.read()
if re.search(r'\b(isKeyguardShowing|mDreamingLockscreen|mShowingLockscreen|isStatusBarKeyguard)\s*=\s*true\b', out, re.I):
    print("dumpsys window lockscreen flag is true")
    sys.exit(0)

lock_terms = ("com.android.keyguard", "keyguard", "lockscreen", "bouncer")
for line in out.splitlines():
    low = line.lower()
    if any(k in low for k in ("mcurrentfocus=", "mfocusedwindow=", "mfocusedapp=", "mtopfullscreenopaquewindowstate=")):
        if any(t in low for t in lock_terms):
            print(f"dumpsys focused lock window: {line.strip()[:120]}")
            sys.exit(0)

blocks = re.split(r'(?=^\s*Window #\d+)', out, flags=re.M)
for blk in blocks:
    first = blk.splitlines()[0] if blk.splitlines() else ""
    flow = first.lower()
    if any(t in flow for t in lock_terms):
        blow = blk.lower()
        hidden = (
            ("mviewvisibility=0x8" in blow) or
            ("mviewvisibility=0x4" in blow) or
            ("isvisible=false" in blow) or
            ("mhassurface=false" in blow and "isvisible=true" not in blow)
        )
        if not hidden:
            print(f"dumpsys visible lock window: {first.strip()[:120]}")
            sys.exit(0)
PY
)
  if [ -n "$lock_reason" ]; then
    locked_exit "$lock_reason"
  fi
}

# Inspect $CUR XML for foreground state, lock screen, and third-party overlays.
# Outputs one line: STATE|OVERLAY_LIST
inspect_window_state() {
  local xml_file="$1"
  python3 -I - "$xml_file" "$PKG" <<'PY'
import sys, xml.etree.ElementTree as ET

xml_path, pkg = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(xml_path).getroot()
except Exception:
    print("PARSE_ERROR|")
    sys.exit(0)

qa_nodes = []
lock_hit = False
other_apps = []

ime_hints = ("inputmethod", "keyboard", "gboard", "swiftkey", "honeyboard", "latinime")
sys_pkgs = ("com.android.systemui", "android")
lock_terms = ("keyguard", "lockscreen", "bouncer")

# Check any window/root metadata attributes as well as node packages
for elem in root.iter():
    for attr in ("title", "name", "package"):
        v = (elem.get(attr) or "").lower()
        if v == "com.android.keyguard" or any(t in v for t in lock_terms):
            lock_hit = True

for n in root.iter("node"):
    p = n.get("package") or ""
    plow = p.lower()
    rid = n.get("resource-id") or ""
    txt = n.get("text") or ""
    desc = n.get("content-desc") or ""
    cls = n.get("class") or ""
    low = (rid + " " + txt + " " + desc + " " + cls).lower()

    if p == "com.android.keyguard" or any(t in plow for t in lock_terms):
        lock_hit = True
    elif p == pkg:
        qa_nodes.append((rid, txt, desc))
        if rid in ("appLockScreen", "pinDotsRow", "vaultGate", "useBiometricButton") or rid.startswith("pinKey-"):
            lock_hit = True
    elif p in sys_pkgs or any(h in plow for h in ime_hints) or any(k in plow for k in ("biometric", "fingerprint", "faceunlock")):
        if any(k in low for k in ("keyguard", "lockscreen", "bouncer", "pin_container", "lock_icon", "biometric", "fingerprint")):
            lock_hit = True
    elif p:
        other_apps.append(p)

if lock_hit:
    print("LOCKED|")
    sys.exit(0)

if not qa_nodes:
    print("NOT_IN_FRONT|" + ",".join(sorted(set(other_apps))))
    sys.exit(0)

overlays = sorted(set(other_apps))
print("OK|" + ",".join(overlays))
PY
}

# Pull a fresh uiautomator --windows dump into $CUR, ensure QA app is foreground and unlocked, then screencap.
dump() {
  local name="$1"
  N=$((N + 1))
  local next_xml next_png
  next_xml="$OUT/$(printf '%02d' "$N")_${name}.xml"
  next_png="${next_xml%.xml}.png"
  CUR="$next_xml"
  CUR_PNG="$next_png"

  local attempt state_info state overlays
  for attempt in 1 2 3; do
    check_dumpsys_lock
    A shell uiautomator dump --windows "$REMOTE_XML" >/dev/null 2>&1 || true
    A pull "$REMOTE_XML" "$CUR" >/dev/null 2>&1 || true
    [ -s "$CUR" ] || { sleep 1; continue; }

    state_info=$(inspect_window_state "$CUR")
    state="${state_info%%|*}"
    overlays="${state_info#*|}"

    if [ "$state" = "LOCKED" ]; then
      rm -f "$CUR"
      CUR=""; CUR_PNG=""
      locked_exit "lock screen or biometric prompt visible"
    elif [ "$state" = "NOT_IN_FRONT" ]; then
      check_dumpsys_lock
      # Bring QA package forward before ever taking a screenshot
      A shell am start -n "$PKG/$ACT" >/dev/null 2>&1 || true
      sleep 1.5
      continue
    elif [ "$state" = "PARSE_ERROR" ]; then
      sleep 1
      continue
    else
      OVERLAY_PKGS="$overlays"
      A exec-out screencap -p > "$CUR_PNG" 2>/dev/null || true
      return 0
    fi
  done

  # Final check after retries
  check_dumpsys_lock
  if [ -s "$CUR" ]; then
    state_info=$(inspect_window_state "$CUR")
    state="${state_info%%|*}"
    [ "$state" = "LOCKED" ] && { rm -f "$CUR"; CUR=""; CUR_PNG=""; locked_exit "lock screen visible after retry"; }
    if [ "$state" = "NOT_IN_FRONT" ]; then
      rm -f "$CUR"
      CUR=""; CUR_PNG=""
      echo "WARN: $PKG could not be brought to foreground (front: ${state_info#*|})" | tee -a "$OUT/results.txt"
      return 1
    fi
  fi
  rm -f "$CUR"
  CUR=""; CUR_PNG=""
  return 1
}

# Query $CUR with a Python expression over `nodes` (and `qa_nodes`)
q() {
  [ -n "$CUR" ] && [ -s "$CUR" ] || return 1
  python3 -I - "$CUR" "$PKG" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg, expr = sys.argv[1], sys.argv[2], sys.argv[3]
nodes = []
try:
    root = ET.parse(xml_path).getroot()
except Exception:
    print("")
    sys.exit(1)

for n in root.iter("node"):
    b = list(map(int, re.findall(r"\d+", n.get("bounds") or "0 0 0 0")))
    if len(b) != 4:
        b = [0, 0, 0, 0]
    w = max(0, b[2] - b[0])
    h = max(0, b[3] - b[1])
    nodes.append({
        "id": n.get("resource-id") or "",
        "text": n.get("text") or "",
        "desc": n.get("content-desc") or "",
        "cls": (n.get("class") or "").split(".")[-1],
        "pkg": n.get("package") or "",
        "clickable": (n.get("clickable") == "true"),
        "enabled": (n.get("enabled") == "true"),
        "focused": (n.get("focused") == "true"),
        "scrollable": (n.get("scrollable") == "true"),
        "selected": (n.get("selected") == "true"),
        "b": b,
        "w": w,
        "h": h,
    })

qa_nodes = [n for n in nodes if n["pkg"] == pkg]
print(eval(expr, {"__builtins__": {"any": any, "all": all, "sum": sum, "len": len, "min": min, "max": max, "next": next, "abs": abs, "round": round, "sorted": sorted, "int": int, "float": float, "str": str, "bool": bool, "list": list, "dict": dict, "set": set, "tuple": tuple, "enumerate": enumerate, "re": re}}, {"nodes": nodes, "qa_nodes": qa_nodes}))
PY
}

has_id() { [ "$(q "any(n['id']=='$1' for n in qa_nodes)" 2>/dev/null)" = "True" ]; }
has_exact_text() { [ "$(q "any(n['text']=='$1' for n in qa_nodes)" 2>/dev/null)" = "True" ]; }
has_sub_text() { [ "$(q "any('$1' in n['text'] for n in qa_nodes)" 2>/dev/null)" = "True" ]; }
has_desc() { [ "$(q "any(n['desc']=='$1' for n in qa_nodes)" 2>/dev/null)" = "True" ]; }
has_ime() { [ "$(q "any(any(k in n['pkg'].lower() for k in ('inputmethod','keyboard','gboard','swiftkey','honeyboard')) for n in nodes)" 2>/dev/null)" = "True" ]; }

# Safely tap a node inside $PKG matching mode (id | text | subtext | desc | id_prefix), refusing destructive buttons.
# Usage: tap_qa <mode> <value> [sleep_sec] [allow_discard]
tap_qa() {
  local mode="$1" val="$2" wait_s="${3:-1.2}" allow_discard="${4:-0}"
  [ -n "$CUR" ] && [ -s "$CUR" ] || return 1
  local xy
  xy=$(python3 -I - "$CUR" "$PKG" "$mode" "$val" "$allow_discard" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg, mode, val, allow_discard = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5] == "1"
blocked = (
    "export",
    "restore",
    "clear",
    "reset",
    "lock",
    "replace",
    "share",
    "print",
    "delete",
    "save",
    "confirm",
    "archive",
    "submit",
    "discard-all",
)

root = ET.parse(xml_path).getroot()
for n in root.iter("node"):
    if (n.get("package") or "") != pkg:
        continue
    rid = n.get("resource-id") or ""
    txt = n.get("text") or ""
    desc = n.get("content-desc") or ""
    matched = (
        (mode == "id" and rid == val) or
        (mode == "id_prefix" and rid.startswith(val)) or
        (mode == "text" and txt == val) or
        (mode == "subtext" and val in txt) or
        (mode == "desc" and desc == val)
    )
    if not matched:
        continue
    raw_combined = rid + " " + txt + " " + desc
    combined = raw_combined.lower()
    tokens = re.findall(r"[A-Z]?[a-z]+|[A-Z]+(?=[A-Z][a-z]|\d|\b)|[0-9]+", raw_combined)
    has_pin_word = any(t.lower() == "pin" for t in tokens)
    if (
        has_pin_word
        or any(b in combined for b in blocked)
        or "discard_all" in combined
        or "discard all" in combined
    ):
        continue
    if ("discard" in combined) and not allow_discard:
        continue
    b = list(map(int, re.findall(r"\d+", n.get("bounds") or "")))
    if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
        print((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
        sys.exit(0)
sys.exit(1)
PY
) || return 1
  A shell input tap $xy >/dev/null 2>&1 || return 1
  sleep "$wait_s"
  return 0
}

swipe_node_id() {
  local node_id="$1" dir="$2"
  [ -n "$CUR" ] && [ -s "$CUR" ] || return 1
  local coords
  coords=$(python3 -I - "$CUR" "$PKG" "$node_id" "$dir" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg, node_id, dir_ = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
for n in ET.parse(xml_path).getroot().iter("node"):
    if (n.get("package") or "") == pkg and (n.get("resource-id") or "") == node_id:
        b = list(map(int, re.findall(r"\d+", n.get("bounds") or "")))
        if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
            cy = (b[1] + b[3]) // 2
            w = b[2] - b[0]
            x_a, x_b = (b[0] + int(w * 0.85), b[0] + int(w * 0.15)) if dir_ == "left" else (b[0] + int(w * 0.15), b[0] + int(w * 0.85))
            print(x_a, cy, x_b, cy, 250)
            sys.exit(0)
sys.exit(1)
PY
) || return 1
  A shell input swipe $coords >/dev/null 2>&1
  sleep 1
}

overlay_note() {
  if [ -n "$OVERLAY_PKGS" ]; then
    echo " (floating overlay present: $OVERLAY_PKGS — may be swallowing taps)"
  fi
}

# Handle discard dialog ONLY if it appears for an editor opened by this script; leaves via Cancel/Back safely.
leave_editor_safely() {
  local cancel_id="${1:-}" prefix="${2:-editor}"
  local rc=0
  if [ -n "$cancel_id" ] && has_id "$cancel_id"; then
    tap_qa id "$cancel_id" 1.2 || true
  elif has_exact_text "Cancel"; then
    tap_qa text "Cancel" 1.2 || true
  else
    A shell input keyevent 4 >/dev/null 2>&1
    sleep 1.2
  fi
  if ! dump "${prefix}_leave"; then
    SCRIPT_OPENED_EDITOR=0
    return 1
  fi

  if has_exact_text "Keep editing" || has_exact_text "Discard" || has_id "shellDiscardDialog" || has_id "discardDialog"; then
    if [ "${SCRIPT_OPENED_EDITOR:-0}" -eq 1 ]; then
      tap_qa text "Discard" 1.2 1 || tap_qa id "discardDialog" 1.2 1 || true
      dump "${prefix}_discarded" || rc=1
    else
      if has_exact_text "Keep editing" || has_exact_text "Continue"; then
        tap_qa text "Keep editing" 1.2 || tap_qa text "Continue" 1.2 || true
        dump "${prefix}_kept" || true
      fi
      echo "WARN: discard prompt shown for editor not opened by script (${prefix}); refusing to tap Discard" | tee -a "$OUT/results.txt"
      rc=1
    fi
  fi
  SCRIPT_OPENED_EDITOR=0
  return "$rc"
}

# Check lock screen via dumpsys before bringing QA app forward or tapping anything.
check_dumpsys_lock

# Bring QA app to front (never force-stop/restart to avoid locking).
A shell am start -n "$PKG/$ACT" >/dev/null 2>&1 || true
sleep 3

# -----------------------------------------------------------------------------
# CHECK 1: App launches to the patient list (text 'Patients', 'New patient')
# -----------------------------------------------------------------------------
DRAFT_PROMPT_ABORT=0
if dump launch; then
  if ! has_id patientListTable && (has_id workspaceScreen || ! has_exact_text "Patients"); then
    for _ in 1 2 3; do
      if has_exact_text "Keep editing" || has_exact_text "Discard" || has_exact_text "Continue" || has_id "shellDiscardDialog" || has_id "discardDialog"; then
        if has_exact_text "Keep editing" || has_exact_text "Continue"; then
          tap_qa text "Keep editing" 1.2 || tap_qa text "Continue" 1.2 || true
          dump launch_draft_kept || true
        fi
        echo "WARN 01: draft prompt present at launch (never tapping Discard)" | tee -a "$OUT/results.txt"
        DRAFT_PROMPT_ABORT=1
        break
      else
        A shell input keyevent 4 >/dev/null 2>&1
        sleep 1.2
      fi
      dump back_to_list || break
      has_id patientListTable && has_exact_text "Patients" && break
    done
  fi

  if [ "$DRAFT_PROMPT_ABORT" -eq 1 ]; then
    fail_check 01 "draft prompt present at launch; stopped recovery without discarding user draft (see ${CUR_PNG:-no screenshot})$(overlay_note)"
  elif has_exact_text "Patients" && has_exact_text "New patient"; then
    ON_PATIENT_LIST=1
    pass_check 01 "app launches to patient list ('Patients' and 'New patient' visible)"
  else
    fail_check 01 "patient list missing 'Patients' or 'New patient' (see ${CUR_PNG:-no screenshot})$(overlay_note)"
  fi
else
  fail_check 01 "could not capture foreground UI dump for $PKG$(overlay_note)"
fi

# -----------------------------------------------------------------------------
# CHECK 2: List touch targets (>= 168 px in width and height except text-scrolling chips)
# -----------------------------------------------------------------------------
if [ "$ON_PATIENT_LIST" -eq 1 ] && has_id patientListTable; then
  OFFENDERS=$(python3 -I - "$CUR" "$PKG" "$MIN_TARGET_PX" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg, min_px = sys.argv[1], sys.argv[2], int(sys.argv[3])
root = ET.parse(xml_path).getroot()
parent_map = {c: p for p in root.iter() for c in p}

def parse_bounds(elem):
    b = list(map(int, re.findall(r"\d+", elem.get("bounds") or "")))
    return b if len(b) == 4 else None

win_bounds = [b for w in root.iter("window") if (b := parse_bounds(w)) is not None]
if win_bounds:
    screen_top = min(b[1] for b in win_bounds)
    screen_bottom = max(b[3] for b in win_bounds)
else:
    all_bounds = [b for n in root.iter("node") if (b := parse_bounds(n)) is not None]
    screen_top = min((b[1] for b in all_bounds), default=0)
    screen_bottom = max((b[3] for b in all_bounds), default=2736)

def is_scrolling_chip(elem):
    rid = elem.get("resource-id") or ""
    if rid.startswith("riskFilter-"):
        return True
    cur = parent_map.get(elem)
    while cur is not None:
        if cur.get("scrollable") == "true" and (cur.get("resource-id") or "") != "patientListTable":
            # Horizontal scroll container holding filter chips
            return True
        cur = parent_map.get(cur)
    return False

offenders = []
for n in root.iter("node"):
    if (n.get("package") or "") != pkg:
        continue
    if n.get("clickable") != "true":
        continue
    if is_scrolling_chip(n):
        continue
    b = parse_bounds(n)
    if b is None:
        continue
    # Ignore nodes cut off by the top or bottom screen edge; judge only visible-in-full targets
    if b[1] <= screen_top or b[3] >= screen_bottom:
        continue
    w, h = b[2] - b[0], b[3] - b[1]
    if w < min_px or h < min_px:
        label = n.get("resource-id") or n.get("content-desc") or n.get("text") or n.get("class")
        offenders.append(f"{label}({w}x{h}@{b})")

print("; ".join(offenders))
PY
)
  if [ -z "$OFFENDERS" ]; then
    pass_check 02 "all visible-in-full non-chip clickable nodes on patient list >= ${MIN_TARGET_PX}x${MIN_TARGET_PX} px"
  else
    fail_check 02 "clickable nodes smaller than ${MIN_TARGET_PX} px on patient list: $OFFENDERS"
  fi
else
  skip_check 02 "patient list not open"
fi

# -----------------------------------------------------------------------------
# CHECK 3: Search box filters (type a letter, expect strictly fewer cards, clear it)
# -----------------------------------------------------------------------------
if [ "$ON_PATIENT_LIST" -eq 1 ] && has_id patientListTable; then
  BEFORE_CARDS=$(q "sum(1 for n in qa_nodes if n['id'].startswith('patient-'))" 2>/dev/null || echo 0)
  if [ "$BEFORE_CARDS" -eq 0 ]; then
    skip_check 03 "no patient cards visible on list to test search filtering"
  elif tap_qa id "patientSearch" 1.0; then
    dump search_focus || true
    if [ "$(q "any(n['id']=='patientSearch' and n['focused'] for n in qa_nodes)" 2>/dev/null)" != "True" ] && ! has_ime; then
      tap_qa id "patientSearch" 1.0 || true
    fi
    A shell input text "m" >/dev/null 2>&1 || true
    sleep 1.5
    TYPED_LEN=1
    if dump search_filtered; then
      SEARCH_ACCEPTED=0
      if has_desc "Clear search" || [ "$(q "any(n['id']=='patientSearch' and 'm' in n['text'].lower() for n in qa_nodes)" 2>/dev/null)" = "True" ]; then
        SEARCH_ACCEPTED=1
      fi
      AFTER_CARDS=$(q "sum(1 for n in qa_nodes if n['id'].startswith('patient-'))" 2>/dev/null || echo 9999)
      if [ "$SEARCH_ACCEPTED" -eq 1 ] && [ "$AFTER_CARDS" -eq "$BEFORE_CARDS" ]; then
        A shell input text "qx" >/dev/null 2>&1 || true
        sleep 1.5
        TYPED_LEN=3
        if dump search_filtered_narrow; then
          AFTER_CARDS=$(q "sum(1 for n in qa_nodes if n['id'].startswith('patient-'))" 2>/dev/null || echo 9999)
        fi
      fi
    else
      SEARCH_ACCEPTED=0
      AFTER_CARDS=9999
    fi

    # Clear search via focused DEL keyevents (never tapping 'Clear*' per blocked tap rules)
    tap_qa id "patientSearch" 0.6 || true
    A shell input keyevent 123 >/dev/null 2>&1 || true
    for _ in $(seq 1 $((TYPED_LEN + 1))); do
      A shell input keyevent 67 >/dev/null 2>&1 || true
    done
    sleep 1
    dump search_after_clear || true
    if has_desc "Clear search"; then
      tap_qa id "patientSearch" 0.6 || true
      A shell input keyevent 123 67 67 67 67 >/dev/null 2>&1 || true
      sleep 1
      dump search_after_clear_retry || true
    fi
    if has_ime; then
      A shell input keyevent 4 >/dev/null 2>&1 || true
      sleep 1
      dump search_cleared_ime || true
    fi
    RESTORED_CARDS=$(q "sum(1 for n in qa_nodes if n['id'].startswith('patient-'))" 2>/dev/null || echo 0)

    if [ "$SEARCH_ACCEPTED" -eq 1 ] && [ "$AFTER_CARDS" -lt "$BEFORE_CARDS" ] && [ "$RESTORED_CARDS" -ge "$BEFORE_CARDS" ]; then
      pass_check 03 "search filtered cards ($BEFORE_CARDS -> $AFTER_CARDS) and cleared back to $RESTORED_CARDS"
    else
      fail_check 03 "search filter check failed (accepted=$SEARCH_ACCEPTED before=$BEFORE_CARDS, filtered=$AFTER_CARDS, cleared=$RESTORED_CARDS)$(overlay_note)"
    fi
  else
    fail_check 03 "could not tap patientSearch field$(overlay_note)"
  fi
else
  skip_check 03 "patient list not open"
fi

# -----------------------------------------------------------------------------
# CHECK 4: Open a patient workspace and visit Overview, Visits, Scans, Labs, Meds, Plan
# -----------------------------------------------------------------------------
if [ "$ON_PATIENT_LIST" -eq 1 ]; then
  FIRST_PATIENT_ID=$(q "next((n['id'] for n in qa_nodes if n['id'].startswith('patient-')), '')" 2>/dev/null)
  if [ -n "$FIRST_PATIENT_ID" ] && tap_qa id "$FIRST_PATIENT_ID" 2.0; then
    dump workspace_open || true
    PATIENT_NAME=$(q "next((n['text'] for n in qa_nodes if n['id']=='hubPatientName'), '')" 2>/dev/null)
  else
    dump workspace_open_fail || true
  fi
fi

if [ "$ON_PATIENT_LIST" -eq 0 ]; then
  skip_check 04 "patient list not open"
elif ! has_id workspaceScreen; then
  fail_check 04 "could not open patient workspace from list$(overlay_note)"
else
  # Helper to locate and tap a workspace tab by its visible label in patientHubTabBar
  visit_tab() {
    local label="$1" content_id="$2" slug="$3"
    local found=0 try_swipe
    for try_swipe in 1 2 3 4; do
      if has_exact_text "$label"; then
        found=1
        break
      fi
      swipe_node_id patientHubTabBar left || break
      dump "tabbar_seek_${slug}_${try_swipe}" || break
    done
    if [ "$found" -eq 0 ]; then
      for try_swipe in 1 2 3 4; do
        if has_exact_text "$label"; then
          found=1
          break
        fi
        swipe_node_id patientHubTabBar right || break
        dump "tabbar_seek_r_${slug}_${try_swipe}" || break
      done
    fi
    if [ "$found" -eq 0 ]; then
      skip_check "04_${slug}" "tab label '$label' not found in patientHubTabBar dump"
      return 1
    fi
    tap_qa text "$label" 1.2 || true
    if ! dump "tab_${slug}"; then
      fail_check "04_${slug}" "failed to capture dump for tab '$label'"
      return 1
    fi
    local has_content text_cnt
    has_content=0
    has_id "$content_id" && has_content=1
    text_cnt=$(q "sum(1 for n in qa_nodes if n['text'].strip() != '' and n['b'][1] > 200)" 2>/dev/null || echo 0)
    if [ "$has_content" -eq 1 ] && [ "$text_cnt" -gt 0 ]; then
      pass_check "04_${slug}" "tab '$label' opened ($content_id present, $text_cnt text nodes)"
      return 0
    else
      fail_check "04_${slug}" "tab '$label' missing expected content ($content_id=$has_content, texts=$text_cnt)$(overlay_note)"
      return 1
    fi
  }

  visit_tab "Overview" "overviewTabContent" "overview" || true
  visit_tab "Visits"   "visitsTabContent"   "visits"   || true
  visit_tab "Scans"    "scansTabContent"    "scans"    || true
  visit_tab "Labs"     "labsTabContent"     "labs"     || true
  visit_tab "Meds"     "medsTabContent"     "meds"     || true
  visit_tab "Plan"     "planTabContent"     "plan"     || true
  skip_check "04_text_12sp" "12 sp minimum text size is not measurable via uiautomator XML; inspect NN_tab_*.png"
fi

# -----------------------------------------------------------------------------
# CHECK 5: Visits -> new-visit editor -> step header & patient banner -> date picker -> no pencil toggle -> Back -> Cancel/Discard
# -----------------------------------------------------------------------------
if has_id workspaceScreen; then
  # Navigate back to Visits tab
  for _ in 1 2 3 4; do
    has_exact_text "Visits" && break
    swipe_node_id patientHubTabBar right || break
    dump seek_visits_tab || break
  done
  tap_qa text "Visits" 1.2 || tap_qa id "tab-visits" 1.2 || true
  dump visits_ready || true

  if tap_qa id "recordNewVisitBtn" 1.5 || tap_qa text "Record visit" 1.5; then
    dump visit_editor || true
    STEP_OK=0
    if has_id "stepSegment-0" || has_sub_text "Step 1 of"; then STEP_OK=1; fi
    if has_id "visitEditor" || [ "$STEP_OK" -eq 1 ]; then
      SCRIPT_OPENED_EDITOR=1
    fi

    BANNER_OK=$(python3 -I - "$CUR" "$PKG" "$PATIENT_NAME" <<'PY'
import re, sys, xml.etree.ElementTree as ET
xml_path, pkg, pname = sys.argv[1], sys.argv[2], sys.argv[3]
nodes = []
for n in ET.parse(xml_path).getroot().iter("node"):
    if (n.get("package") or "") != pkg:
        continue
    b = list(map(int, re.findall(r"\d+", n.get("bounds") or "0 0 0 0")))
    nodes.append((n.get("resource-id") or "", n.get("text") or "", b))
step_y = next((b[1] for rid, _, b in nodes if rid == "stepSegment-0"), 99999)
parity = any(re.search(r"\bG\d+\s+P\d+\s+A\d+\b", t) and b[1] < step_y for _, t, b in nodes)
name_ok = bool(pname) and any(pname in t and b[1] < step_y for _, t, b in nodes)
print("1" if (parity or name_ok) else "0")
PY
)
    PICKER_OPEN=0
    NO_PENCIL=0
    PICKER_CLOSED=0

    if tap_qa id "visitDateCalendarButton" 1.5 || tap_qa id "visitDate" 1.5; then
      dump visit_date_picker || true
      if has_id "ancDatePickerDialog" || [ "$(q "any(n['desc'].startswith('Current selection:') or n['desc'].startswith('Change to next month') for n in qa_nodes)" 2>/dev/null)" = "True" ]; then
        PICKER_OPEN=1
        NO_PENCIL=$(python3 -I - "$CUR" "$PKG" <<'PY'
import sys, xml.etree.ElementTree as ET
xml_path, pkg = sys.argv[1], sys.argv[2]
root = ET.parse(xml_path).getroot()
# Find ancDatePickerDialog subtree or picker window if present, else scan all qa nodes
picker = None
for n in root.iter("node"):
    if (n.get("package") or "") == pkg and (n.get("resource-id") or "") == "ancDatePickerDialog":
        picker = n
        break
if picker is None:
    for w in root.iter("window"):
        if any(
            (n.get("package") or "") == pkg and (
                (n.get("content-desc") or "").startswith("Current selection:") or
                (n.get("content-desc") or "").startswith("Change to next month")
            )
            for n in w.iter("node")
        ):
            picker = w
            break
scope = [n for n in picker.iter("node") if (n.get("package") or "") == pkg] if picker is not None else [n for n in root.iter("node") if (n.get("package") or "") == pkg]
for n in scope:
    desc = (n.get("content-desc") or "").lower()
    cls = (n.get("class") or "").split(".")[-1]
    if any(k in desc for k in ("switch to text input", "text input", "input mode", "switch to text")) or cls == "EditText":
        print("0")
        sys.exit(0)
print("1")
PY
)
      fi
      A shell input keyevent 4 >/dev/null 2>&1
      sleep 1.2
      dump visit_after_picker || true
      if ! has_id "ancDatePickerDialog" && [ "$(q "any(n['desc'].startswith('Current selection:') or n['desc'].startswith('Change to next month') for n in qa_nodes)" 2>/dev/null)" != "True" ]; then
        PICKER_CLOSED=1
      fi
    fi

    leave_editor_safely "cancelVisit" "visit" || true
    LEFT_OK=0
    if ! has_id "visitEditor" && (has_id "visitsTabContent" || has_id "workspaceScreen"); then
      LEFT_OK=1
    fi

    if [ "$STEP_OK" -eq 1 ] && [ "$BANNER_OK" = "1" ] && [ "$PICKER_OPEN" -eq 1 ] && [ "$NO_PENCIL" = "1" ] && [ "$PICKER_CLOSED" -eq 1 ] && [ "$LEFT_OK" -eq 1 ]; then
      pass_check 05 "visit editor (step header, patient banner, date picker without pencil toggle, Back & Cancel/Discard)"
    else
      fail_check 05 "visit editor failed (step=$STEP_OK banner=$BANNER_OK picker=$PICKER_OPEN no_pencil=$NO_PENCIL picker_closed=$PICKER_CLOSED left=$LEFT_OK)$(overlay_note)"
    fi
  else
    fail_check 05 "could not open new-visit editor from Visits tab$(overlay_note)"
  fi
else
  skip_check 05 "workspace not open"
fi

# -----------------------------------------------------------------------------
# CHECK 6: New-scan editor and new-lab editor (and Lab batch if reachable) open/close
# -----------------------------------------------------------------------------
if has_id workspaceScreen; then
  # 6a: Scan editor
  SCAN_OK=0
  for _ in 1 2 3 4; do
    has_exact_text "Scans" && break
    swipe_node_id patientHubTabBar left || break
    dump seek_scans_tab || break
  done
  tap_qa text "Scans" 1.2 || tap_qa id "tab-scans" 1.2 || true
  dump scans_ready || true
  if tap_qa id "recordNewScanBtn" 1.5 || tap_qa text "Record scan" 1.5; then
    dump scan_editor || true
    if has_id "ultrasoundEditor" || has_sub_text "Ultrasound scan editor"; then
      SCRIPT_OPENED_EDITOR=1
      leave_editor_safely "cancelUltrasound" "scan" || true
      if ! has_id "ultrasoundEditor" && (has_id "scansTabContent" || has_id "workspaceScreen"); then
        SCAN_OK=1
      fi
    else
      leave_editor_safely "cancelUltrasound" "scan" || true
    fi
  fi
  if [ "$SCAN_OK" -eq 1 ]; then
    pass_check "06_scan" "new-scan editor opened and closed cleanly"
  else
    fail_check "06_scan" "new-scan editor open/close failed$(overlay_note)"
  fi

  # 6b: Lab batch & single Lab editor
  for _ in 1 2 3 4; do
    has_exact_text "Labs" && break
    swipe_node_id patientHubTabBar left || break
    dump seek_labs_tab || break
  done
  tap_qa text "Labs" 1.2 || tap_qa id "tab-labs" 1.2 || true
  dump labs_ready || true

  LAB_BATCH_OK=0
  if tap_qa id "addInvestigationBtn" 1.5 || tap_qa text "Add test" 1.5; then
    dump lab_batch_editor || true
    if has_id "labBatchScreen" || has_sub_text "Laboratory results" || has_id "labEditor"; then
      SCRIPT_OPENED_EDITOR=1
      leave_editor_safely "labBatchCancel" "lab_batch" || true
      if ! has_id "labBatchScreen" && ! has_id "labEditor" && (has_id "labsTabContent" || has_id "workspaceScreen"); then
        LAB_BATCH_OK=1
      fi
    else
      leave_editor_safely "labBatchCancel" "lab_batch" || true
    fi
  fi
  if [ "$LAB_BATCH_OK" -eq 1 ]; then
    pass_check "06_lab_batch" "lab batch screen opened and closed cleanly"
  else
    fail_check "06_lab_batch" "lab batch screen open/close failed$(overlay_note)"
  fi

  LAB_ROW_ID=$(q "next((n['id'] for n in qa_nodes if n['id'].startswith('lab-') and not n['id'].startswith('labReportCard-')), '')" 2>/dev/null)
  if [ -n "$LAB_ROW_ID" ]; then
    LAB_ED_OK=0
    if tap_qa id "$LAB_ROW_ID" 1.5; then
      dump lab_editor || true
      if has_id "labEditor" || has_sub_text "Laboratory investigation"; then
        SCRIPT_OPENED_EDITOR=1
        leave_editor_safely "" "lab_editor" || true
        if ! has_id "labEditor" && (has_id "labsTabContent" || has_id "workspaceScreen"); then
          LAB_ED_OK=1
        fi
      else
        leave_editor_safely "" "lab_editor" || true
      fi
    fi
    if [ "$LAB_ED_OK" -eq 1 ]; then
      pass_check "06_lab_editor" "lab editor ($LAB_ROW_ID) opened and closed cleanly"
    else
      fail_check "06_lab_editor" "lab editor ($LAB_ROW_ID) open/close failed$(overlay_note)"
    fi
  else
    skip_check "06_lab_editor" "no existing lab-* row visible on Labs tab to open single lab editor"
  fi
else
  skip_check "06_scan" "workspace not open"
  skip_check "06_lab_batch" "workspace not open"
  skip_check "06_lab_editor" "workspace not open"
fi

# -----------------------------------------------------------------------------
# CHECK 8 (run while in workspace): Report dialog (Page 1/2, Print PDF & Share present, do NOT tap them, close)
# -----------------------------------------------------------------------------
if has_id workspaceScreen; then
  if ! has_id "workspaceReportButton" && ! has_id "hubReportButton" && ! has_desc "Two-page report"; then
    # Expand hub header if collapsed
    A shell input swipe 600 800 600 1800 300 >/dev/null 2>&1
    sleep 1
    dump hub_expand_for_report || true
  fi

  if tap_qa id "workspaceReportButton" 1.5 || tap_qa id "hubReportButton" 1.5 || tap_qa desc "Two-page report" 1.5; then
    if dump report_page1; then
      HAS_P1=0; HAS_P2=0; HAS_PRINT=0; HAS_SHARE=0; SWITCHED=0; CLOSED=0
      (has_exact_text "Page 1" || has_id "reportPage1Chip") && HAS_P1=1
      (has_exact_text "Page 2" || has_id "reportPage2Chip") && HAS_P2=1
      if [ "$(q "any((n['id']=='printReportButton' or n['text']=='Print PDF') and n['enabled'] and n['w']>0 and n['h']>0 for n in qa_nodes)" 2>/dev/null)" = "True" ]; then
        HAS_PRINT=1
      fi
      if [ "$(q "any((n['id']=='shareReportButton' or n['desc']=='Share clinical record') and n['enabled'] and n['w']>0 and n['h']>0 for n in qa_nodes)" 2>/dev/null)" = "True" ]; then
        HAS_SHARE=1
      fi
      P1_TEXTS=$(q "'|'.join(n['text'] for n in qa_nodes if n['text'])" 2>/dev/null || echo "")

      # Switch to Page 2 (NEVER tap Print PDF or Share)
      if tap_qa id "reportPage2Chip" 1.2 || tap_qa text "Page 2" 1.2; then
        if dump report_page2; then
          P2_TEXTS=$(q "'|'.join(n['text'] for n in qa_nodes if n['text'])" 2>/dev/null || echo "")
          if [ "$(q "any((n['id']=='reportPage2Chip' or n['text']=='Page 2') and n['selected'] for n in qa_nodes)" 2>/dev/null)" = "True" ] || { [ -n "$P1_TEXTS" ] && [ -n "$P2_TEXTS" ] && [ "$P2_TEXTS" != "$P1_TEXTS" ]; }; then
            SWITCHED=1
          fi
        fi
      fi

      if tap_qa id "closeReportDialog" 1.2 || tap_qa desc "Close" 1.2; then
        :
      else
        A shell input keyevent 4 >/dev/null 2>&1; sleep 1.2
      fi
      if dump report_closed && ! has_id "patientTwoPageReportDialog"; then
        CLOSED=1
      fi

      if [ "$HAS_P1" -eq 1 ] && [ "$HAS_P2" -eq 1 ] && [ "$HAS_PRINT" -eq 1 ] && [ "$HAS_SHARE" -eq 1 ] && [ "$SWITCHED" -eq 1 ] && [ "$CLOSED" -eq 1 ]; then
        pass_check 08 "report dialog (Page 1 & Page 2 chips, switched pages, Print PDF & share enabled/visible without tapping, closed)"
      else
        fail_check 08 "report dialog check failed (p1=$HAS_P1 p2=$HAS_P2 print=$HAS_PRINT share=$HAS_SHARE switched=$SWITCHED closed=$CLOSED)$(overlay_note)"
      fi
    else
      fail_check 08 "failed to capture report dialog UI dump$(overlay_note)"
    fi
  else
    fail_check 08 "could not open patient report dialog from hub header$(overlay_note)"
  fi

  # Return from workspace to patient list
  if tap_qa id "hubBackButton" 1.5 || tap_qa id "stickyBackButton" 1.5; then
    :
  else
    A shell input keyevent 4 >/dev/null 2>&1; sleep 1.5
  fi
  dump returned_to_list || true
else
  skip_check 08 "workspace not open"
fi

# -----------------------------------------------------------------------------
# CHECK 7: Settings dialog (gear -> 'Security & clinic', 'Lab tests', 'Medications' visible without clipping, switch tabs, close)
# -----------------------------------------------------------------------------
has_settings_dialog() {
  has_id "securitySettingsDialog" || {
    has_exact_text "Clinic & System Settings" &&
    has_exact_text "Security & clinic" &&
    has_exact_text "Lab tests" &&
    has_exact_text "Medications"
  }
}

if [ "$ON_PATIENT_LIST" -eq 0 ] && ! has_id "openSecuritySettings" && ! has_desc "Security Settings"; then
  skip_check 07 "patient list / settings gear not reachable"
elif tap_qa id "openSecuritySettings" 1.5 || tap_qa desc "Security Settings" 1.5; then
  if dump settings_security; then
    TABS_CLIP_REPORT=$(python3 -I - "$CUR" "$PKG" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg = sys.argv[1], sys.argv[2]
root = ET.parse(xml_path).getroot()
qa = []
for n in root.iter("node"):
    if (n.get("package") or "") != pkg:
        continue
    b = list(map(int, re.findall(r"\d+", n.get("bounds") or "0 0 0 0")))
    qa.append((n.get("resource-id") or "", n.get("text") or "", b))

dlg = next((b for rid, _, b in qa if rid == "securitySettingsDialog"), None)
if dlg is None:
    has_title = any(txt == "Clinic & System Settings" for _, txt, _ in qa)
    if has_title:
        for win in root.iter("window"):
            if any((n.get("package") or "") == pkg and (n.get("text") or "") == "Clinic & System Settings" for n in win.iter("node")):
                wb = list(map(int, re.findall(r"\d+", win.get("bounds") or "")))
                if len(wb) == 4:
                    dlg = wb
                    break
        if dlg is None:
            dlg = next((b for rid, _, b in qa if rid == "android:id/content" and b[2] > b[0] and b[3] > b[1]), None)
if dlg is None:
    print("MISSING_DIALOG")
    sys.exit(0)

labels = ["Security & clinic", "Lab tests", "Medications"]
issues = []
for lbl in labels:
    hit = next(((rid, txt, b) for rid, txt, b in qa if txt == lbl), None)
    if hit is None:
        issues.append(f"missing:{lbl}")
        continue
    _, txt, b = hit
    w, h = b[2] - b[0], b[3] - b[1]
    if w <= 0 or h <= 0:
        issues.append(f"zero_size:{lbl}")
    if b[0] < dlg[0] or b[2] > dlg[2] or b[1] < dlg[1] or b[3] > dlg[3]:
        issues.append(f"clipped:{lbl}({b} outside {dlg})")
    if "..." in txt or "…" in txt:
        issues.append(f"ellipsized:{lbl}")

print("OK" if not issues else "; ".join(issues))
PY
)
    LAB_TAB_OK=0
    MED_TAB_OK=0
    SETTINGS_CLOSED=0

    if tap_qa id "settingsTabLabCatalog" 1.2 || tap_qa text "Lab tests" 1.2; then
      if dump settings_lab_tests && has_settings_dialog && ! has_id "themeSettingsCard" && ! has_exact_text "App theme"; then
        if [ "$(q "any((n['id']=='settingsTabLabCatalog' or n['text']=='Lab tests') and n['selected'] for n in qa_nodes)" 2>/dev/null)" = "True" ] || has_exact_text "Laboratory tests"; then
          LAB_TAB_OK=1
        fi
      fi
    fi
    if tap_qa id "settingsTabMedCatalog" 1.2 || tap_qa text "Medications" 1.2; then
      if dump settings_medications && has_settings_dialog && ! has_id "themeSettingsCard" && ! has_exact_text "App theme" && ! has_exact_text "Laboratory tests"; then
        if [ "$(q "any((n['id']=='settingsTabMedCatalog' or n['text']=='Medications') and n['selected'] for n in qa_nodes)" 2>/dev/null)" = "True" ] || has_exact_text "My medication list"; then
          MED_TAB_OK=1
        fi
      fi
    fi

    if tap_qa id "closeSettingsButton" 1.2 || tap_qa desc "Close" 1.2; then
      :
    else
      A shell input keyevent 4 >/dev/null 2>&1; sleep 1.2
    fi
    if dump settings_closed && ! has_settings_dialog; then
      SETTINGS_CLOSED=1
    fi

    if [ "$TABS_CLIP_REPORT" = "OK" ] && [ "$LAB_TAB_OK" -eq 1 ] && [ "$MED_TAB_OK" -eq 1 ] && [ "$SETTINGS_CLOSED" -eq 1 ]; then
      pass_check 07 "settings dialog tabs ('Security & clinic', 'Lab tests', 'Medications') unclipped, switched tabs, closed"
    else
      fail_check 07 "settings dialog failed (tabs=$TABS_CLIP_REPORT lab_tab=$LAB_TAB_OK med_tab=$MED_TAB_OK closed=$SETTINGS_CLOSED)$(overlay_note)"
    fi
  else
    fail_check 07 "failed to capture settings dialog UI dump$(overlay_note)"
  fi
else
  fail_check 07 "could not open settings dialog gear (openSecuritySettings)$(overlay_note)"
fi

# -----------------------------------------------------------------------------
# CHECK 9: Arabic patient name right-aligned (node right edge within 24 dp = 84 px of card's right padding / name area edge)
# -----------------------------------------------------------------------------
if [ "$ON_PATIENT_LIST" -eq 1 ] && ! has_id patientListTable; then
  for _ in 1 2; do A shell input keyevent 4 >/dev/null 2>&1; sleep 1.2; dump list_for_arabic || break; has_id patientListTable && break; done
fi

if ! has_id patientListTable; then
  skip_check 09 "patient list not open"
else
  ARABIC_CHECK=""
  for swipe_try in 0 1 2; do
    [ -n "$CUR" ] && [ -s "$CUR" ] || break
    ARABIC_CHECK=$(python3 -I - "$CUR" "$PKG" "$ARABIC_PAD_MAX_PX" <<'PY'
import re, sys, xml.etree.ElementTree as ET

xml_path, pkg, max_gap_px = sys.argv[1], sys.argv[2], int(sys.argv[3])
root = ET.parse(xml_path).getroot()
arabic_re = re.compile(r"[\u0600-\u06FF]")

def bounds_of(elem):
    b = list(map(int, re.findall(r"\d+", elem.get("bounds") or "")))
    return b if len(b) == 4 else [0, 0, 0, 0]

def find_arabic_in_card(card):
    cb = bounds_of(card)
    menu_b = None
    child_hit = None
    self_hit = None
    for n in card.iter("node"):
        if (n.get("package") or "") != pkg:
            continue
        rid = n.get("resource-id") or ""
        txt = n.get("text") or ""
        if rid.startswith("patientMenu-"):
            menu_b = bounds_of(n)
        elif txt and arabic_re.search(txt):
            if n is card:
                if self_hit is None:
                    self_hit = (txt, bounds_of(n), "card_merged")
            else:
                if child_hit is None:
                    child_hit = (txt, bounds_of(n), "name_node")
    return cb, menu_b, (child_hit or self_hit)

for n in root.iter("node"):
    if (n.get("package") or "") != pkg:
        continue
    rid = n.get("resource-id") or ""
    if rid.startswith("patient-"):
        cb, menu_b, arabic_hit = find_arabic_in_card(n)
        if arabic_hit is not None:
            txt, nb, source = arabic_hit
            # If the card header has patientMenu-* on the same row to the right of the name,
            # the name area's right edge sits against menu_b[0]; otherwise card right edge cb[2].
            if source == "name_node" and menu_b is not None and menu_b[0] >= nb[2] - 10 and menu_b[1] < nb[3] and menu_b[3] > nb[1]:
                right_ref = menu_b[0]
                ref_kind = "menu_left"
            else:
                right_ref = cb[2]
                ref_kind = "card_right"
            gap = right_ref - nb[2]
            status = "PASS" if (0 <= gap <= max_gap_px) else "FAIL"
            print(f"{status}|{rid}|{source}|{txt}|nx1={nb[2]}|{ref_kind}={right_ref}|card_x1={cb[2]}|gap_px={gap}|gap_dp={gap/3.5:.1f}")
            sys.exit(0)

print("NONE")
PY
)
    [ "$ARABIC_CHECK" != "NONE" ] && break
    [ "$swipe_try" -lt 2 ] && { A shell input swipe 600 1800 600 800 300 >/dev/null 2>&1; sleep 1.2; dump arabic_scroll_$swipe_try || break; }
  done

  if [ "$ARABIC_CHECK" = "NONE" ] || [ -z "$ARABIC_CHECK" ]; then
    skip_check 09 "no Arabic patient card found on patient list"
  else
    A_STATUS="${ARABIC_CHECK%%|*}"
    A_DETAIL="${ARABIC_CHECK#*|}"
    if [ "$A_STATUS" = "PASS" ]; then
      pass_check 09 "Arabic patient name right edge aligned ($A_DETAIL; visual glyph check: ${CUR_PNG:-no screenshot})"
    else
      fail_check 09 "Arabic patient name alignment out of ${ARABIC_PAD_MAX_PX}px (24dp) tolerance ($A_DETAIL; see ${CUR_PNG:-no screenshot})"
    fi
  fi
fi

# -----------------------------------------------------------------------------
# CHECK 10: Logcat tail filtered to QA pid (FATAL EXCEPTION, ANR, AndroidRuntime)
# -----------------------------------------------------------------------------
QA_PID=$(A shell pidof -s "$PKG" 2>/dev/null || A shell pidof "$PKG" 2>/dev/null | awk '{print $1}')
QA_PID=$(echo "$QA_PID" | tr -d '\r\n ')
LOGCAT_ERRS=$(A logcat -d -t 2000 2>/dev/null | python3 -I - "$QA_PID" "$PKG" <<'PY'
import re, sys

pid, pkg = sys.argv[1], sys.argv[2]
hits = []
pid_re = re.compile(rf"^\S+\s+\S+\s+{re.escape(pid)}\s+") if pid else None
bad_re = re.compile(r"(FATAL EXCEPTION|\bANR\b|AndroidRuntime)")

for line in sys.stdin:
    line = line.rstrip("\r\n")
    if not bad_re.search(line):
        continue
    if (pid_re and pid_re.match(line)) or (pkg in line):
        hits.append(line)

print("\n".join(hits[:10]))
PY
)
if [ -z "$QA_PID" ]; then
  fail_check 10 "cannot filter logcat: pidof $PKG is empty${LOGCAT_ERRS:+; matches: $LOGCAT_ERRS}"
elif [ -z "$LOGCAT_ERRS" ]; then
  pass_check 10 "logcat -d -t 2000 for pid $QA_PID has no FATAL EXCEPTION, ANR, or AndroidRuntime lines"
else
  fail_check 10 "logcat -d -t 2000 for pid $QA_PID found errors: $(echo "$LOGCAT_ERRS" | tr '\n' ' | ')"
fi

# -----------------------------------------------------------------------------
# CHECK 11: Final state (app still running via pidof and still on a normal screen)
# -----------------------------------------------------------------------------
FINAL_PID=$(A shell pidof -s "$PKG" 2>/dev/null || A shell pidof "$PKG" 2>/dev/null | awk '{print $1}')
FINAL_PID=$(echo "$FINAL_PID" | tr -d '\r\n ')
dump final_state || true

NORMAL_SCREEN=0
if has_id "patientListTable" || has_exact_text "Patients" || has_id "workspaceScreen"; then
  NORMAL_SCREEN=1
fi
CRASH_UI=0
if [ "$(q "any('keeps stopping' in n['text'].lower() or 'responding' in n['text'].lower() for n in nodes)" 2>/dev/null)" = "True" ]; then
  CRASH_UI=1
fi

if [ -n "$FINAL_PID" ] && [ "$NORMAL_SCREEN" -eq 1 ] && [ "$CRASH_UI" -eq 0 ]; then
  pass_check 11 "app running (pid $FINAL_PID) on a normal screen (${CUR_PNG:-no screenshot})"
else
  fail_check 11 "final state unhealthy (pid='${FINAL_PID:-none}', normal_screen=$NORMAL_SCREEN, crash_ui=$CRASH_UI)$(overlay_note)"
fi

PASSES=$(grep -c '^PASS ' "$OUT/results.txt" || true)
SKIPS=$(grep -c '^SKIP ' "$OUT/results.txt" || true)
SUMMARY="SUMMARY: ${PASSES} pass, ${FAILS} fail, ${SKIPS} skip -> $OUT/results.txt"
echo "$SUMMARY" | tee -a "$OUT/results.txt"

[ "$FAILS" -eq 0 ]
