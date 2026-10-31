#!/usr/bin/env python3
"""U1-U6 static source check for round 2 (MACA R2, 2026-10-29).

Stdlib only. Reads Kotlin sources under app/src/main and prints PASS/FAIL per
sub-check of oracles U1-U6 in .agents/pipeline/SPEC_FIX_R2.md, then exits 1 if
any sub-check fails. Textual source contract only; verifies nothing at runtime.
Round-1 check .agents/checks/check_ui_static.py must keep passing AND this
checker must FAIL on U1-U6 on today's (pre-fix) code.

Scopes (only files named in the spec are read):
  U1 Emoji code points in ProfileScreen.kt, PatientListScreen.kt,
     WorkspaceScreen.kt, ClinicalBadges.kt.          -> one result per file
  U2 formatBloodGroup( called in PatientListScreen.kt, ProfileScreen.kt,
     WorkspaceScreen.kt.                             -> one result per file
  U3 No contains("-") / contains("+") / contains("High") and calls to
     isRhNegative( + isHighRisk( in PatientListScreen.kt, ProfileScreen.kt.
                                                     -> one result per file
  U4 Every reference to the nine R.string.edit_* ids in ProfileScreen.kt and
     WorkspaceScreen.kt is a pencil button contentDescription
     (PencilEditButton(contentDescription = ...) or
     Icon(Icons.Default.Edit, contentDescription = ...)), never inside Text(...)
     Existing testTags are not removed by this check. -> one result per (file,id)
  U5 R.string.correct_previous_pregnancy in WorkspaceScreen.kt is shown as Text
     inside a TextButton (not a pencil) and the tag prefix "editPregnancy-" is
     kept.                                             -> one result
  U6 WorkspaceScreen.kt has no contentDescription literal "Edit Clinical
     Alerts", uses R.string.edit_clinical_alerts, and the enclosing IconButton
     has no Modifier.size(32.dp).                      -> one result
   U7 (R2b addendum) ProfileScreen.kt scan photo chip shows
     stringResource(R.string.photos_count, n), never a bare number:
     no imageUris.size.toString() in ProfileScreen.kt AND the file uses
     stringResource(R.string.photos_count, ...) for the photo count.
                                                       -> one result

Technique: call nesting is resolved on a "masked" copy of the source in which
Kotlin string-literal interiors and comments are blanked (length preserving, so
indexes match the raw source). String-presence checks (emoji, test tags,
resource ids) run on the raw source.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
UI_DIR = REPO_ROOT / "app" / "src" / "main" / "java" / "com" / "sherifradwan" / "ancnative" / "ui"

PROFILE = UI_DIR / "ProfileScreen.kt"
PATIENT = UI_DIR / "PatientListScreen.kt"
WORKSPACE = UI_DIR / "WorkspaceScreen.kt"
BADGES = UI_DIR / "ClinicalBadges.kt"

EMOJI_RANGES = (
    (0x1F000, 0x1FAFF),
    (0x2600, 0x27BF),
    (0x2B00, 0x2BFF),
    (0xFE0F, 0xFE0F),
)

# U4: the nine edit ids that must be pencil-button contentDescriptions only.
EDIT_IDS = [
    "edit_dating",
    "edit_history",
    "edit_ultrasound",
    "edit_visit",
    "edit_other_histories",
    "edit_lab",
    "edit_medication",
    "edit_delivery_plan",
    "edit_clinical_alerts",
]

PENCIL_OR_ICON_RE = re.compile(r"\b(PencilEditButton|Icon)\s*\(")
TEXT_CALL_RE = re.compile(r"\bText\s*\(")
CONTAINS_BADGE_RE = re.compile(r'\.contains\(\s*"[-+]"')
CONTAINS_HIGH_RE = re.compile(r'\.contains\(\s*"High')


def read(path: Path):
    if not path.is_file():
        return None
    return path.read_text(encoding="utf-8")


def shorter(name: Path) -> str:
    return name.name


def mask(source: str) -> str:
    """Blank string-literal interiors and comments, preserving length/newlines."""
    # Block comments (keep newlines so line numbers/indexes stay aligned).
    source = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), source, flags=re.S)
    # Line comments.
    source = re.sub(r"//[^\n]*", lambda m: " " * len(m.group(0)), source)
    # String literals (interiors blanked; template braces and their contents too).
    source = re.sub(
        r'"(?:[^"\\\n]|\\.)*"',
        lambda m: re.sub(r"[^\n]", " ", m.group(0)),
        source,
    )
    return source


def line_of(source: str, idx: int) -> int:
    return source.count("\n", 0, idx) + 1


def enclosing_calls(masked: str, idx: int, limit: int = 4):
    """Innermost-first list of (call_name, open_paren_index) enclosing `idx`.

    Walks backwards tracking parenthesis depth; each '(' reached at depth 0 is the
    nearest enclosing call opener.
    """
    calls = []
    depth = 0
    j = idx
    while j >= 0 and len(calls) < limit:
        c = masked[j]
        if c == ")":
            depth += 1
        elif c == "(":
            if depth == 0:
                m = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\s*$", masked[:j])
                calls.append((m.group(1) if m else "", j))
            else:
                depth -= 1
        j -= 1
    return calls


def matching_paren(masked: str, open_idx: int):
    depth = 0
    k = open_idx
    while k < len(masked):
        c = masked[k]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return k
        k += 1
    return None


def is_pencil_desc(masked: str, idx: int):
    """True if R.string resource at idx sits in a pencil-button contentDescription.

    Returns (ok, detail): ok paths are PencilEditButton(contentDescription =
    stringResource(...)) or Icon(Icons.Default.Edit, contentDescription =
    stringResource(...)).
    """
    calls = enclosing_calls(masked, idx)
    if not calls:
        return False, "resource reference is not inside any call"
    inner_name, _ = calls[0]
    if inner_name == "Text":
        return False, "reference is inside Text(...)"
    if inner_name != "stringResource":
        return False, f"reference is directly inside {inner_name!r}, not stringResource(...)"
    if len(calls) < 2:
        return False, "stringResource(...) result is not a contentDescription argument"
    parent_name, parent_open = calls[1]
    if parent_name == "PencilEditButton":
        return True, "PencilEditButton(contentDescription = stringResource(...))"
    if parent_name == "Icon":
        window = masked[parent_open: parent_open + 70]
        if "Icons.Default.Edit" in window:
            return True, "Icon(Icons.Default.Edit, contentDescription = stringResource(...))"
        return False, "Icon(...) is not Icons.Default.Edit"
    return False, f"stringResource(...) sits inside {parent_name!r}, not PencilEditButton/Icon"


def lambda_host_near(masked: str, idx: int, host_re: str, max_lookback: int = 600):
    """Nearest call host of `host_re` whose trailing-lambda block contains idx.

    Checks `Regex(host_re)( ... ) { ... idx ... }`: for every host occurrence before
    idx, the host encloses idx only if its lambda brace opened and is not yet
    closed between the host's '(' and idx (braces are counted on masked source, so
    braces inside string literals never interfere).
    """
    best = None
    for m in re.finditer(host_re, masked):
        if m.start() >= idx:
            break
        span = masked[m.start(): idx]
        opens = span.count("{")
        closes = span.count("}")
        if opens >= 1 and opens > closes:
            best = m.start()
    return best


def run() -> int:
    results = []  # (ok, "U<n>(scope)", message)
    sources = {}
    for p in (PROFILE, PATIENT, WORKSPACE, BADGES):
        sources[p] = read(p)

    # ---- U1: emoji code points ----
    for p in (PROFILE, PATIENT, WORKSPACE, BADGES):
        tag = f"U1({shorter(p)})"
        src = sources.get(p)
        if src is None:
            results.append((False, tag, f"{shorter(p)} not found"))
            continue
        hits = []
        for i, ch in enumerate(src):
            code = ord(ch)
            if any(lo <= code <= hi for lo, hi in EMOJI_RANGES):
                hits.append((line_of(src, i), hex(code)))
        if hits:
            shown = ", ".join(f"line {ln}: {hx}" for ln, hx in hits[:8])
            more = f" (+{len(hits) - 8} more)" if len(hits) > 8 else ""
            results.append((False, tag, f"emoji/glyph code points present: {shown}{more}"))
        else:
            results.append((True, tag, "no emoji code points"))

    # ---- U2: formatBloodGroup( called where blood group values are displayed ----
    for p in (PROFILE, PATIENT, WORKSPACE):
        tag = f"U2({shorter(p)})"
        src = sources.get(p)
        if src is None:
            results.append((False, tag, f"{shorter(p)} not found"))
            continue
        if re.search(r"\bformatBloodGroup\s*\(", src):
            results.append((True, tag, "formatBloodGroup( is called"))
        else:
            results.append((False, tag, "formatBloodGroup( is never called"))

    # ---- U3: no inline Rh/risk contains(); helpers are the only source ----
    for p in (PROFILE, PATIENT):
        tag = f"U3({shorter(p)})"
        src = sources.get(p)
        if src is None:
            results.append((False, tag, f"{shorter(p)} not found"))
            continue
        bad = [
            (m.group(0), line_of(src, m.start()))
            for m in CONTAINS_BADGE_RE.finditer(src)
        ] + [
            (m.group(0), line_of(src, m.start()))
            for m in CONTAINS_HIGH_RE.finditer(src)
        ]
        if bad:
            shown = ", ".join(f"{pat!r} at line {ln}" for pat, ln in bad[:6])
            results.append((False, tag, f"inline contains() Rh/risk logic present: {shown}"))
            continue
        missing = [
            name
            for name, rx in (("isRhNegative", r"\bisRhNegative\s*\("),
                             ("isHighRisk", r"\bisHighRisk\s*\("))
            if not re.search(rx, src)
        ]
        if missing:
            results.append((False, tag, "does not call: " + ", ".join(missing)))
        else:
            results.append((True, tag, "no inline contains()-Rh/risk logic; uses isRhNegative/isHighRisk"))

    # ---- U4: nine edit ids are pencil contentDescriptions, never in Text(...) ----
    for p in (PROFILE, WORKSPACE):
        masked = mask(sources.get(p) or "")
        for res_id in EDIT_IDS:
            tag = f"U4({shorter(p)}/{res_id})"
            src = sources.get(p)
            if src is None:
                results.append((False, tag, f"{shorter(p)} not found"))
                continue
            refs = [m.start() for m in re.finditer(rf"\bR\.string\.{res_id}\b", src)]
            if not refs:
                results.append(
                    (True, tag, "no references (vacuous; resource must still exist for U6 where applicable)")
                )
                continue
            problems = []
            for idx in refs:
                ok, detail = is_pencil_desc(masked, idx)
                if not ok:
                    problems.append(f"line {line_of(src, idx)}: {detail}")
            if problems:
                results.append(
                    (False, tag, "; ".join(problems) + " (expected PencilEditButton/Icon(Icons.Default.Edit))")
                )
            else:
                results.append(
                    (True, tag, f"all {len(refs)} reference(s) are pencil-button contentDescriptions")
                )

    # ---- U5: correct_previous_pregnancy is Text inside TextButton, tag kept ----
    tag = "U5(WorkspaceScreen.kt/correct_previous_pregnancy)"
    src = sources.get(WORKSPACE)
    if src is None:
        results.append((False, tag, "WorkspaceScreen.kt not found"))
    else:
        refs = [m.start() for m in re.finditer(r"\bR\.string\.correct_previous_pregnancy\b", src)]
        masked = mask(src)
        ok_all = bool(refs)
        details = []
        for idx in refs:
            calls = enclosing_calls(masked, idx)
            names = [c[0] for c in calls]
            # Expected: stringResource( -> Text( and the Text call hosted in a
            # TextButton's trailing lambda (not a pencil button).
            if names[:2] == ["stringResource", "Text"]:
                tb = lambda_host_near(masked, idx, r"\bTextButton\s*\(")
                pencil = any(n == "PencilEditButton" for n in names)
                if tb is not None and not pencil:
                    details.append(f"line {line_of(src, idx)}: Text inside TextButton")
                elif pencil:
                    details.append(f"line {line_of(src, idx)}: still wrapped in PencilEditButton, chain {names[:3]}")
                    ok_all = False
                elif tb is None:
                    details.append(f"line {line_of(src, idx)}: chain {names[:3]}, no enclosing TextButton...")
                    ok_all = False
            else:
                details.append(
                    f"line {line_of(src, idx)}: chain {names[:3]}, expected [stringResource, Text, TextButton...]"
                )
                ok_all = False
        tag_kept = '"editPregnancy-' in src
        if not refs:
            ok_all = False
            details.append("R.string.correct_previous_pregnancy not referenced in WorkspaceScreen.kt")
        if not tag_kept:
            ok_all = False
            details.append('testTag prefix "editPregnancy-" missing')
        results.append((ok_all, tag, "; ".join(details) if details else "ok"))

    # ---- U6: clinical alerts edits: resource used, no literal, no size(32.dp) ----
    tag = "U6(WorkspaceScreen.kt/edit_clinical_alerts)"
    src = sources.get(WORKSPACE)
    if src is None:
        results.append((False, tag, "WorkspaceScreen.kt not found"))
    else:
        problems = []
        literal = re.search(r'contentDescription\s*=\s*"Edit Clinical Alerts"', src)
        if literal:
            problems.append(f'contentDescription literal "Edit Clinical Alerts" at line {line_of(src, literal.start())}')
        uses = [m.start() for m in re.finditer(r"\bR\.string\.edit_clinical_alerts\b", src)]
        if not uses:
            problems.append("R.string.edit_clinical_alerts is not used")
        masked = mask(src)
        for idx in uses:
            calls = enclosing_calls(masked, idx)
            ib = lambda_host_near(masked, idx, r"\bIconButton\s*\(")
            if ib is None and not any(n == "IconButton" for n, _ in calls):
                # Resource not hosted by an IconButton: still fine if it is a
                # pencil contentDescription (PencilEditButton or Icon with Edit).
                ok, detail = is_pencil_desc(masked, idx)
                if ok:
                    continue
                problems.append(f"line {line_of(src, idx)}: hosted at {calls[0][0] if calls else 'nothing'!r}, not an IconButton or pencil contentDescription")
                continue
            host_start = ib if ib is not None else next(o for n, o in calls if n == "IconButton")
            # Args of the IconButton end at the first '{' reached outside parens
            # (its trailing lambda). Mods after contentDescription stay in scope.
            cut = idx
            paren = 0
            for k in range(host_start, len(masked)):
                c = masked[k]
                if c == "(":
                    paren += 1
                elif c == ")":
                    paren -= 1
                elif c == "{" and paren == 0:
                    cut = k
                    break
                if k > idx + 400:
                    cut = max(cut, idx + 400)
                    break
            span = masked[host_start: max(cut, idx)]
            if re.search(r"Modifier\s*\.\s*size\s*\(\s*32\.dp", span):
                problems.append(
                    f"IconButton (contentDescription at line {line_of(src, idx)}) still has Modifier.size(32.dp)"
                )
        results.append(
            (not problems, tag, "; ".join(problems) if problems else "resource used, no literal, no size(32.dp)")
        )

    # ---- U7 (R2b): scan photo chip count is labelled, never a bare number ----
    tag = "U7(ProfileScreen.kt/photos_count)"
    src = sources.get(PROFILE)
    if src is None:
        results.append((False, tag, "ProfileScreen.kt not found"))
    else:
        problems = []
        bare = [m.start() for m in re.finditer(r"\bimageUris\.size\.toString\(\)", src)]
        if bare:
            shown = ", ".join(f"line {line_of(src, i)}" for i in bare)
            problems.append(
                f"photo count shown as a bare number via imageUris.size.toString() at {shown}; "
                "the scan photo chip must show stringResource(R.string.photos_count, n)"
            )
        if not re.search(r"\bstringResource\s*\(\s*R\.string\.photos_count\b", src):
            problems.append("R.string.photos_count is never shown via stringResource(R.string.photos_count, ...)")
        results.append(
            (not problems, tag, "; ".join(problems) if problems else "photo chip count labelled via stringResource(R.string.photos_count, n)")
        )

    for ok, sub, message in results:
        print(f"{'PASS' if ok else 'FAIL'} {sub} {message}")
    total_ok = sum(1 for ok, _, _ in results if ok)
    any_fail = total_ok != len(results)
    print(f"{'PASS' if not any_fail else 'FAIL'} U-r2 overall: {total_ok}/{len(results)} sub-checks passed")
    return 1 if any_fail else 0


if __name__ == "__main__":
    sys.exit(run())
