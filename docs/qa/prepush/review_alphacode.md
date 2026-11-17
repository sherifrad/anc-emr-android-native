Verdict: does **not PASS** as written. Defects are listed below with line numbers and concrete failure scenarios, followed by per-rule notes, the tap-by-text inventory, weak checks, and state-change risks.

## Defects (with line numbers and failure scenarios)

### D1 — Rule 3 violation: system lock screen under `com.android.keyguard` is not detected; script will not exit 3
Lines 86–107, 141–143, 160–164.
`inspect_window_state` only treats `com.android.systemui` and `android` as system packages worth checking for keyguard/bouncer/biometric/fingerprint (lines 100–107). A real lock screen usually lives under `com.android.keyguard` (or an OEM lockscreen package). That package hits the `elif p:` branch at line 108, is added to `other_apps`, and never sets `lock_hit`. With no `qa_nodes`, the function prints `NOT_IN_FRONT|com.android.keyguard,...`.

Concrete scenario: the phone is locked at the PIN/pattern keyguard when the script starts. `dump()` sees `NOT_IN_FRONT`, retries `am start -n .../MainActivity` three times (line 146; keyguard blocks the launch), then writes a `WARN` and returns 1 (line 167) instead of `exit 3`. The script then continues checking against a keyguard/stale dump, and `input tap` coordinates from a stale dump could land on the lock screen. Rule 3 (stop with exit 3 on PIN/fingerprint lock) is violated.

### D2 — Rule 4: tap_qa blocked list is missing `export` and `restore`
Line 227: `blocked = ("save", "delete", "confirm", "archive", "submit", "print", "share", "replace", "reset", "lock")`.
`export` and `restore` are absent, so `tap_qa` will match and tap a node whose resource-id/text/content-desc contains "export" or "restore". The substring check (`any(b in combined ...)`) correctly catches `save` (so "Save PIN" is refused), but not these two.

Concrete scenario: if any current or future check calls `tap_qa id "exportReportButton"` or `tap_qa text "Export..."` / `"Restore backup"`, the tap is not refused and the script performs a write/export action it must never perform.

### D3 — Rule 4/6: unconditional `Discard` taps may destroy a saved or pre-existing unsaved draft
Lines 321–324 (check 1 recovery) and 305–308 (`leave_editor_safely`). Rule 6 allows `Discard` only for an unsaved draft the script itself opened. Check 1's recovery loop taps `Discard` whenever "Keep editing"/"Discard" is visible, even if the script never opened that editor.

Concrete scenario: the QA phone was left with an unsaved visit draft from a prior manual session. Check 1 sees the "Keep editing"/"Discard" dialog and taps `Discard` (line 323, `allow_discard=1`), deleting the user's unsaved draft data. `leave_editor_safely` has the same issue more mildly: it taps `Discard` whenever a discard dialog appears after an editor, without confirming the draft is the script-owned unsaved one.

### D4 — Rule 7: no timeouts on any adb/input/screencap/logcat call; script can hang indefinitely
Lines 133, 134, 154, 391, 402, 547, 664, 792, 854, 874, etc. Every `A shell ...`, `A pull`, `A exec-out screencap`, and `A logcat` runs with no `timeout` wrapper and no `adb` connect/response timeout.

Concrete scenario: the phone screen sleeps, USB suspends, or `uiautomator` wedges; `adb shell uiautomator dump --windows` blocks forever and the script never reaches any exit code (0/1/3), holding the phone and the harness hostage.

### D5 — Rule 7 reliability: `dump()` can return 1 and let later checks run on stale/empty XML
Lines 160–169. If the app cannot be brought to the front after retries, `dump()` writes `WARN` and returns 1. Callers guard with `|| true`, so the script proceeds. `has_*` then default to false (lines 212–216) and later checks run against a stale or missing `$CUR`.

Concrete scenario: the QA app crashed or isn't installed; `dump launch` fails, but the script continues into checks 02–11 on the previous dump (or none), producing false SKIPs/FAILs and confusing output rather than a clean abort.

## Per-rule summary

**Rule 1 (serial + qa package only):** PASS in practice. `A() { adb -s "$S" "$@"; }` always targets the serial; every package-scoped command uses `$PKG` = `com.sherifradwan.anc.qa` only; `com.sherifradwan.anc` (production) is never referenced. `uiautomator dump --windows` and `logcat -d` read system-wide UI/logs but do not write or target another app for modification.

**Rule 2 (no system/setting changes):** PASS. No `settings put`, `wm size/density`, `cmd uimode`, rotation, font scale, `pm clear/uninstall/disable`, `am force-stop`, `svc`. The only `input keyevent` values are 4 (BACK) and 67 (DEL); no POWER/SLEEP/HOME. Line 313 uses `am start` to bring the qa app forward (not force-stop/restart), consistent with the comment.

**Rule 3 (lock screen → exit 3, no PIN/fingerprint):** DEFECT D1. Lock detection misses `com.android.keyguard`/OEM lockscreen packages, so the script may not `exit 3`. It never enters a PIN or touches biometrics (good), but the failure mode is wrong.

**Rule 4 (no destructive taps):** DEFECTS D2, D3. The `tap_qa` refusal list covers save/delete/confirm/archive/submit/print/share/replace/reset/lock and blocks "save" substrings (so "Save PIN" is refused), but omits `export`/`restore`; and `Discard` is tapped outside script-owned drafts (D3). Print PDF / Share are checked for presence only and never tapped (check 08, lines 674–675) — compliant. `input text` goes only into the patient search box after tapping `patientSearch` (line 390) and is cleared via "Clear search" or `keyevent 67` (lines 397–408) — compliant with rule 5.

**Rule 5 (typing only into search box, cleared afterwards):** PASS (single typing site at line 391; cleared at 397–408; IME dismissed with BACK at 406–408). Weakness: no verification that `patientSearch` actually focused before `input text "m"`.

**Rule 6 (editor exits via Back/Cancel; Discard only for script-owned unsaved draft):** Partial PASS for editors the script opens (`leave_editor_safely` uses cancel id / "Cancel" / BACK, then Discard only on the confirmation dialog), DEFECT D3 for check 1's recovery and for the absence of draft-ownership verification.

**Rule 7 (robustness):** DEFECTS D4, D5. `set -u` is used and variables are initialized; no infinite loops (all loops bounded: retries 1–3, swipe tries 1–4); quoting is mostly intentional (`input tap $xy` / `input swipe $coords` unquoted by design); dump parse errors are caught and retried; exit codes are 0 (no FAILs), 1 (any FAIL), 3 (lock via `locked_exit`). Missing timeouts and the stale-XML continuation are real gaps.

## Tap-by-text inventory (rule 4)

Every `tap_qa` text-mode target uses **exact** match (`txt == val`), so substring collisions are not an issue for text mode; the blocked-list substring check covers ids/descs:

| Target | Line(s) | Could it also match a destructive button? |
|---|---|---|
| "Cancel" | 297 | No. |
| "Discard" (allow_discard=1) | 306, 323 | Yes-ish: it is a destructive-for-draft action; permitted only for an unsaved draft the script opened — see D3. |
| "Overview","Visits","Scans","Labs","Meds","Plan" | 461 | No; navigation labels. |
| "Record visit","Record scan","Add test" | 498, 586, 613 | No; they open an unsaved editor, but the script never taps Save/Submit inside. |
| "Page 2" | 678 | No. |
| "Lab tests","Medications" | 758, 764 | No; settings tabs. |
| desc "Clear search","Close","Two-page report","Security Settings" | 398, 685, 771, 716 | No. |

`tap_qa` subtext mode is defined but never actually invoked in a tap call (only `has_sub_text` read-checks), so fuzzy tap matching is limited to exact text plus the blocked-list substring screen.

## Weak checks (often PASS / weak signal)

- Line 482: check `04_text_12sp` is always `SKIP` — never actually verifies 12 sp minimum text.
- Lines 760, 766: tab-switch verification falls back to `|| ! has_id "themeSettingsCard"`, so it can PASS even when the Lab/Med tab did not actually switch.
- Line 412: `AFTER_CARDS -le BEFORE_CARDS` passes even if typing "m" filtered nothing (equal counts).
- Lines 674–675: verifies Print PDF / Share *presence* only; the script correctly never taps them, but the check does not prove they are disabled/untappable.
- Line 466: `text_cnt > 0` is a weak content proxy for "tab opened."
- Line 857–858: check 09 `SKIP`s when no Arabic card exists — appropriate, but alignment is then unverified.
- Line 895: check 10 scans only the last 2000 logcat lines for FATAL EXCEPTION/ANR/AndroidRuntime; caught exceptions and older crashes are missed.
- Line 908: `NORMAL_SCREEN` passes if any list/workspace marker is present even if a crash dialog is also visible (partially mitigated by the CRASH_UI check at 912).

## Anything that could leave the phone in a changed state

- **D3/Discard:** destruction of unsaved draft data (app/user state) — the only real data-loss risk.
- Nothing else mutates phone state: no settings changes, no package installs/removals/clears, no lock, no PIN entry, no force-stop. The app is left running; screenshots/UI dumps go to `<out_dir>` on the host; `/sdcard/qa_prepush_ui.xml` is removed by the `trap cleanup EXIT` handler, including on `exit 3`. After check 08 the script navigates back to the patient list (lines 703–708), and check 11 verifies a normal final screen.

## Required fixes (minimum to reach PASS)

1. Add lockscreen packages (e.g. `com.android.keyguard`, plus OEM variants) to lock detection in `inspect_window_state`, and `exit 3` whenever a lock screen is present instead of retrying `am start`.
2. Add `"export"` and `"restore"` to the `blocked` tuple on line 227.
3. Restrict `Discard` taps to script-owned unsaved drafts: remove the unconditional `Discard` from check 1's recovery (lines 321–324), or track which editor the script opened and only `leave_editor_safely` may tap `Discard`.
4. Add a timeout (e.g. `timeout 30` or `adb -t`) to every `adb`/`input`/`screencap`/`logcat` call so a wedged device fails fast rather than hanging.
5. When `dump()` cannot bring the app to the front after retries, abort (exit 1, or exit 3 if a lock is detected) instead of continuing on stale XML.

With D1–D5 fixed, the script's safety envelope matches the stated rules; the weak checks above remain honest limitations, not safety defects.
[Tokens] upload: 25549 download: 15993 cache_read: 0 cache_write: 0

