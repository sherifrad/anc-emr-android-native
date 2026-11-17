**Verdict: PASS** — all four round‑1 defects are fixed, and the full re‑check is satisfied. No blocking defects remain; one minor, non‑blocking weakness is noted below with a suggested fix.

---

### D1 — Keyguard detected before any tap: FIXED

- **Lines 80–121** — `check_dumpsys_lock()` reads `dumpsys window`, flags `isKeyguardShowing`/`mShowingLockscreen`/etc. (line 89), and treats visible `keyguard`/`lockscreen`/`bouncer` windows as locked (lines 93–115).
- **Lines 424–425** — pre‑flight `check_dumpsys_lock` runs before `am start` (line 428) and before any tap.
- **Lines 197–200** — inside `dump()`, every dump re‑runs `check_dumpsys_lock` before the UI is captured.
- All taps flow through `tap_qa` (the only `input tap` is line 355), which requires `$CUR` from `dump`, so no tap can precede a lock check.

---

### D2 — Blocked tap list now includes export/restore: FIXED

- **Lines 302–317** — the `blocked` tuple now contains `export` (line 303), `restore` (line 304), and also `clear`, `reset`, `lock`, `replace`, `share`, `print`, `delete`, `save`, `confirm`, `archive`, `submit`, `discard-all` — matching the safety policy at line 12.
- Matching is a substring scan of `rid + text + desc` (lines 335–341), plus PIN‑word tokenization (lines 338–339) and discard gating (lines 346–347).

---

### D3 — Discard only for script‑opened editors: FIXED

- **Lines 407–418** — in `leave_editor_safely()`, `tap_qa text "Discard"` is executed only when `SCRIPT_OPENED_EDITOR -eq 1` (lines 408–409); for editors the script did not open it refuses to tap Discard and only warns (lines 416–418).
- **Lines 434–454** — at launch, a draft prompt is dismissed with "Keep editing"/"Continue" (lines 439–441), `DRAFT_PROMPT_ABORT=1` (line 444), and the script stops without discarding (fail at line 456).

---

### D4 — Timeouts on adb calls: FIXED

- **Line 30** — `ADB_TIMEOUT_SEC="${ADB_TIMEOUT_SEC:-20}"`.
- **Lines 41–49** — `run_with_timeout()` uses `timeout` or a `perl alarm` fallback.
- **Line 51** — `A()` wraps every `adb` call with that timeout.
- **Line 54** — the `cleanup` trap rm is also timeout‑wrapped (5 seconds).

---

### Re‑check results

**No tap reaches Save/Delete/Confirm/Archive/Submit/Print/Share/Export/Restore/Replace/Reset** — all eleven are in `blocked` at lines 302–317 (save 312, delete 311, confirm 313, archive 314, submit 315, print 310, share 309, export 303, restore 304, replace 308, reset 306). The only `input tap` in the script is line 355 inside `tap_qa`, which filters by package (lines 321–322) and skips blocked/PIN/discard nodes (lines 335–347). All other input events are keyevents or swipes (lines 447, 554–557, 562, 721, 841, 894, 966), none of which are those buttons.

**No system settings change** — no `settings put`, `wm size/density`, rotation, dark‑mode, or font‑scale call anywhere (policy stated at line 9; `am start` at lines 214/428 only launches the QA app).

**Only the qa package is touched** — `tap_qa` ignores non‑`$PKG` nodes (lines 321–322); `am start -n "$PKG/$ACT"` targets only `com.sherifradwan.anc.qa` (lines 214, 428); overlays from other packages are only reported via `OVERLAY_PKGS`/`overlay_note` (lines 181–182, 384–388), never tapped.

**Typing only in the search box, and cleared** — `input text` occurs only at lines 530 (`"m"`) and 540 (`"qx"`), both after `patientSearch` is tapped (line 525, with a focus re‑tap at 528). Clearing uses DEL keyevents (67) at lines 555–557 and 562, never tapping a `Clear*` button (line 552 comment).

**Exit codes 0/1/3** — `exit 3` via `locked_exit` (line 77); line 1128 `[ "$FAILS" -eq 0 ]` yields exit 0 (all pass) or 1 (any FAIL).

---

### Minor weakness (non‑blocking)

**Lines 530 and 540**: `input text` is sent without a final assertion that `patientSearch` still holds focus after the guard at lines 527–529. If `tap_qa` returned 0 but focus did not land, typed text could go to another focused field. Probability is low (no intervening events between tap and type), but for strict compliance with "typing only in the search box," add a focus check before each `input text` (or skip typing when unfocused).

---

**Bottom line:** D1–D4 verified fixed with line quotes; every re‑check constraint is satisfied; overall result is **PASS**, with the line 530/540 focus weakness logged for a follow‑up hardening patch.
[Tokens] upload: 29048 download: 8960 cache_read: 0 cache_write: 0

