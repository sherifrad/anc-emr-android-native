#!/usr/bin/env python3
"""S76-S77 static source check for round 3o (SPEC_FIX_R3.md, section "Round 3o").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S76 ui/report/HtmlEscape.kt defines `internal fun escapeHtml(`;
      PatientPrintHtmlGenerator.kt calls escapeHtml( at least 25 times;
      no other file under app/src/main defines a function that replaces
      "<" with "&lt;" (textual heuristic: a file with `fun` bodies that both
      calls .replace( and contains the literal "&lt;").
  S77 in PatientPrintHtmlGenerator.kt every ${...} interpolation either starts
      with escapeHtml( or is on the short explicit allow-list below; offending
      expressions are printed.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"

ESCAPE_HTML_FILE = MAIN / "ui/report/HtmlEscape.kt"
GENERATOR_FILE = MAIN / "ui/report/PatientPrintHtmlGenerator.kt"

results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return None


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", src)


def check_s76():
    problems = []
    esc = read(ESCAPE_HTML_FILE)
    if esc is None:
        problems.append("ui/report/HtmlEscape.kt missing")
    else:
        code = strip_comments(esc)
        if not re.search(r"\binternal\s+fun\s+escapeHtml\s*\(", code):
            problems.append("HtmlEscape.kt lacks `internal fun escapeHtml(`")

    gen = read(GENERATOR_FILE)
    if gen is None:
        problems.append("ui/report/PatientPrintHtmlGenerator.kt missing")
    else:
        calls = len(re.findall(r"\bescapeHtml\s*\(", strip_comments(gen)))
        if calls < 25:
            problems.append("PatientPrintHtmlGenerator.kt calls escapeHtml( only %d times (need >= 25)" % calls)

    # Other escaping implementations. Textual heuristic: a function-bearing file
    # that both calls .replace( and contains the "&lt;" entity literal is a copy
    # of the escape rule; HtmlEscape.kt itself is the one allowed owner.
    dupes = []
    for f in sorted(MAIN.rglob("*.kt")):
        if f == ESCAPE_HTML_FILE:
            continue
        src = read(f)
        if src is None:
            continue
        code = strip_comments(src)
        if (
            re.search(r"\bfun\b", code)
            and re.search(r"\.replace\s*\(", code)
            and '"&lt;"' in code
        ):
            dupes.append(str(f.relative_to(MAIN)))
    if dupes:
        problems.append("escape rule duplicated in: " + ", ".join(dupes))
    record("S76 one escapeHtml rule: internal fun in HtmlEscape.kt, generator calls it, no copies elsewhere",
           not problems, "; ".join(problems))


# ---------------------------------------------------------------------------
# S77 allow-list, built by reading today's PatientPrintHtmlGenerator.kt.
# Anything not listed must start with escapeHtml( at the interpolation point.
# ---------------------------------------------------------------------------
ALLOW_PREFIXES = [
    # numbers / indexes
    ("idx + 1", "visit-table row index"),
    ("sIdx + 1", "scan-photo label index"),
    ("iIdx + 1", "scan-photo label index"),
    ("totalPages", "page count"),
    ("s.imageUris.size", "attached-photo count"),
    ("patient.previousPregnancies.size", "previous-pregnancy count"),
    ("s.gestationalAgeWeeks", "GA weeks (number)"),
    ("s.gestationalAgeDays", "GA days (number)"),
    # generator-built fragments (from numbers, badges, mounting, engine output)
    ("logoBase64", "generated PNG data URI"),
    ("logoImgTag", "generated logo <img> fragment"),
    ("watermarkImg", "generated CSS watermark background fragment"),
    ("vGa", "GA label from GestationalAgeEngine (numbers, not free text)"),
    ("scanGaStr", "GA label built from scan week/day numbers"),
    ("currentGaFormatted", "GA label from GestationalAgeEngine.formatted"),
    ("pastDeliverySummary", "summary built from CS/SVD counts (numbers only)"),
    ("resultHtml", "lab-result <span> fragment, escaped inside the builder"),
    ("typeBadge", "generated medication badge fragment"),
    ("outcomeBadge", "generated previous-pregnancy badge fragment"),
    ("visitsRowsHtml", "generated visits table-rows fragment"),
    ("scansRowsHtml", "generated scans table-rows fragment"),
    ("labsRowsHtml", "generated labs table-rows fragment"),
    ("medsRowsHtml", "generated medications table-rows fragment"),
    ("prevPregRows", "generated previous-pregnancies fragment"),
    ("page3Html", "generated page-3 appendix fragment"),
    # row/list builders written inline: the outer builder is a generated fragment
    # (spec Q93: row/list builders are not escaped again); every leaf value he
    # interpolates is still collected from the nested template and must carry
    # escapeHtml( at its own interpolation point.
    ("patient.previousPregnancies.map", "inline previous-pregnancy row builder fragment"),
    ("attachedUltrasoundImages.map", "inline page-3 photo-card row builder fragment"),
    # generator-owned markup branches (constants, never patient text)
    ('if (isRhNegative) "text-rose" else "text-navy"', "CSS class branch"),
    ('if (!patient.allergyHistory.isNullOrBlank()) "text-rose font-bold" else ""', "CSS class branch"),
    ('if (classifyAllergy(patient.allergyHistory) is AllergyState.Recorded) "text-rose font-bold" else ""', "CSS class branch (selects between two constant class names)"),
    ('if (isRhNegative) """<span class="badge badge-danger">Rh- Negative</span>""" else ""', "generated Rh badge branch"),
    ('if (isRhNegative) "Anti-D Prophylaxis + Matched Packed RBCs Cross-match" else "Routine Obstetric Hospital Protocol"', "generator-owned precaution wording branch (both constants)"),
    ("when {", "risk-badge selector block (generator markup only)"),
]


def interpolations(src):
    """Every ${...} expression, including nested ones inside triple-quoted
    templates that live inside another interpolation."""
    out = []
    for expr, nested_bodies in _top_level(src):
        out.append(expr)
        for body in nested_bodies:
            out.extend(interpolations(body))
    return out


def _top_level(src):
    """Scan src for ${...} spans (quote aware). For each span return
    (expression, [triple-quoted string bodies found inside the expression])."""
    spans = []
    triple_bodies_all = []
    i = 0
    n = len(src)
    while i < n:
        k = src.find("${", i)
        if k == -1:
            break
        j = k + 2
        depth = 1
        in_str = 0  # 0 none, 1 double quote, 3 triple quote
        str_open = -1
        triple_bodies = []
        e = j
        while e < n and depth > 0:
            if src.startswith('"""', e):
                if in_str == 3:
                    triple_bodies.append(src[str_open:e])
                    in_str = 0
                elif in_str == 0:
                    str_open = e + 3
                    in_str = 3
                e += 3
                continue
            c = src[e]
            if in_str == 1:
                if c == "\\":
                    e += 2
                    continue
                if c == '"':
                    in_str = 0
                    e += 1
                    continue
                e += 1
                continue
            if in_str == 3:
                if c == "\\":
                    e += 2
                    continue
                e += 1  # extra double quotes inside a triple body are literal
                continue
            if c == '"':
                # cannot happen here: startswith handled triple; a lone " opens
                # a plain string inside the expression
                in_str = 1
                e += 1
                continue
            if c == "{":
                depth += 1
                e += 1
                continue
            if c == "}":
                depth -= 1
                e += 1
                continue
            e += 1
        if depth == 0:
            spans.append((src[j:e - 1], triple_bodies))
            i = e
        else:
            break  # unterminated interpolation; stop scanning
    return spans


def check_s77():
    problems = []
    gen = read(GENERATOR_FILE)
    if gen is None:
        problems.append("ui/report/PatientPrintHtmlGenerator.kt missing")
        record("S77 every ${...} is escapeHtml( or an allow-listed generated fragment",
               False, "; ".join(problems))
        return
    exprs = interpolations(strip_comments(gen))
    offenders = []
    for raw in exprs:
        expr = raw.strip()
        if re.match(r"escapeHtml\s*\(", expr):
            continue
        if any(expr.startswith(prefix) for prefix, _ in ALLOW_PREFIXES):
            continue
        offenders.append(re.sub(r"\s+", " ", expr).strip())
    if offenders:
        uniq = []
        for o in offenders:
            if o not in uniq:
                uniq.append(o)
        shown = uniq[:40]
        detail = "%d unallowed ${...} expression(s): %s" % (
            len(offenders), "; ".join("`${%s}`" % s for s in shown))
        problems.append(detail if len(shown) < len(uniq) else detail)
    record("S77 every ${...} is escapeHtml( or an allow-listed generated fragment",
           not problems, "; ".join(problems))


def main():
    check_s76()
    check_s77()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
