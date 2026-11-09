#!/usr/bin/env python3
"""S59-S63 static source check for round 3k (SPEC_FIX_R3.md, section "Round 3k").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S59 ui/ClinicalBadges.kt: KeyFactTile's label Text has maxLines = 2 (not 1).
  S60 ui/PatientListScreen.kt: the chip FlowRow does not use bare Arrangement.End
      (it needs Arrangement.spacedBy(8.dp, Alignment.End), the real horizontal
      spacedBy overload) and the PatientCard background uses lerp( instead of a
      translucent errorContainer.copy(alpha = ...).
  S61 ui/ProfileScreen.kt Prior Pregnancies card: the summary Text and the outcome
      Text are not siblings of a Row with Arrangement.SpaceBetween (the block
      contains a Column and no Arrangement.SpaceBetween).
  S62 ui/ClinicalFormat.kt defines internal fun formatWeightKg; ProfileScreen.kt
      contains no "${visit.weight} kg" literal and calls formatWeightKg(.
  S63 ui/AncApp.kt: the subtitle Text ("Maternal-Fetal Medicine & Obstetrics")
      has maxLines = 2.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"

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


def match_end(src, start, open_ch, close_ch):
    """Index of the close_ch matching src[start] == open_ch, or -1."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == open_ch:
            depth += 1
        elif src[i] == close_ch:
            depth -= 1
            if depth == 0:
                return i
    return -1


def call_args(src, name_at):
    """(args_text, close_index) of the (...) of a call; name_at points at the name token
    (which may already include the opening parenthesis, e.g. "Text(" or "FlowRow(")."""
    paren = src.find("(", name_at)
    if paren == -1:
        return None, -1
    end = match_end(src, paren, "(", ")")
    if end == -1:
        return None, -1
    return src[paren + 1:end], end


def brace_body(src, open_at):
    """Source between the braces whose opening brace is at open_at (exclusive)."""
    end = match_end(src, open_at, "{", "}")
    if end == -1:
        return None
    return src[open_at + 1:end]


def check_s59():
    problems = []
    src = read(MAIN / "ui/ClinicalBadges.kt")
    if src is None:
        problems.append("ui/ClinicalBadges.kt missing")
    else:
        src = strip_comments(src)
        fun_at = src.find("fun KeyFactTile(")
        if fun_at == -1:
            problems.append("ClinicalBadges.kt lacks fun KeyFactTile(")
        else:
            body_open = src.find("{", fun_at)
            body = brace_body(src, body_open)
            if body is None:
                problems.append("fun KeyFactTile( body not closed")
            else:
                label_at = body.find("label.uppercase()")
                if label_at == -1:
                    problems.append("KeyFactTile no longer shows label.uppercase()")
                else:
                    text_at = body.rfind("Text(", 0, label_at)
                    args, _ = call_args(body, text_at)
                    if args is None:
                        problems.append("the label Text( of KeyFactTile not found")
                    else:
                        if "maxLines = 2" not in args:
                            problems.append("the KeyFactTile label Text has no maxLines = 2")
                        if "maxLines = 1" in args:
                            problems.append('the KeyFactTile label Text still has maxLines = 1 (label clipped)')
    record("S59 KeyFactTile label Text allows two lines (maxLines = 2)", not problems, "; ".join(problems))


def check_s60():
    # The clinical-tag chip FlowRow of the old patient list card no longer exists (the directory card was
    # redesigned into a header plus a three-column metrics row), so only the card background rule is kept.
    problems = []
    src = read(MAIN / "ui/patientdirectory/PatientDirectoryCard.kt")
    if src is None:
        problems.append("ui/patientdirectory/PatientDirectoryCard.kt missing")
    else:
        src = strip_comments(src)
        if not re.search(r"\blerp\s*\(", src):
            problems.append(
                "the PatientDirectoryCard high-risk background does not use lerp( "
                "(androidx.compose.ui.graphics.lerp(surface, errorContainer, fraction))"
            )
        if re.search(r"errorContainer\.copy\s*\(\s*alpha", src):
            problems.append("the card background is still a translucent errorContainer.copy(alpha = ...)")
    record("S60 directory card high-risk background uses lerp( (opaque blend, not a translucent errorContainer)", not problems, "; ".join(problems))


def check_s61():
    # The Prior Pregnancies card now renders each pregnancy with PastDeliveryMicroRow.kt.
    problems = []
    src = read(MAIN / "ui/patienthub/components/PastDeliveryMicroRow.kt")
    if src is None:
        problems.append("ui/patienthub/components/PastDeliveryMicroRow.kt missing")
    else:
        src = strip_comments(src)
        if "Arrangement.SpaceBetween" in src:
            problems.append("PastDeliveryMicroRow still uses Arrangement.SpaceBetween")
        if "Column(" not in src:
            problems.append("PastDeliveryMicroRow does not contain a Column")
        # The year (summary) Text and the outcome Text must wrap together in a FlowRow, not sit in a plain Row.
        flows = []
        for m in re.finditer(r"(?<![A-Za-z])FlowRow\(", src):
            _, args_end = call_args(src, m.start())
            inner_open = src.find("{", args_end + 1) if args_end != -1 else -1
            body = brace_body(src, inner_open) if inner_open != -1 else None
            if body is not None:
                flows.append(body)
        if not any("yearText" in f and "outcomeText" in f for f in flows):
            problems.append("the year Text and the outcome Text are not both inside a FlowRow")
        for m in re.finditer(r"(?<![A-Za-z])Row\(", src):
            _, args_end = call_args(src, m.start())
            inner_open = src.find("{", args_end + 1) if args_end != -1 else -1
            body = brace_body(src, inner_open) if inner_open != -1 else None
            if body is not None and "text = yearText" in body and "text = it" in body and "FlowRow(" not in body:
                problems.append("the year Text and the outcome Text are siblings of a plain Row")
                break
    record("S61 Prior Pregnancies year and outcome Texts wrap in a FlowRow, not a SpaceBetween Row", not problems, "; ".join(problems))


def check_s62():
    problems = []
    cf = read(MAIN / "ui/ClinicalFormat.kt")
    if cf is None:
        problems.append("ui/ClinicalFormat.kt missing")
    else:
        if not re.search(r"\binternal\s+fun\s+formatWeightKg\s*\(", strip_comments(cf)):
            problems.append("ClinicalFormat.kt lacks internal fun formatWeightKg(")

    # ProfileScreen.kt is gone; the weight is shown by the bedside vitals grid in the patient hub.
    hub = sorted((MAIN / "ui/patienthub").rglob("*.kt"))
    ps = "\n".join(strip_comments(read(f) or "") for f in hub) if hub else None
    if ps is None:
        problems.append("ui/patienthub missing")
    else:
        if "formatWeightKg(" not in ps:
            problems.append("patient hub does not call formatWeightKg(")
        if re.search(r"\$\{\s*visit\.weight\s*\}\s*kg", ps):
            problems.append('patient hub still contains the "${visit.weight} kg" literal ("kg kg" bug)')
    record("S62 formatWeightKg is the one weight rule; the patient hub uses it and no raw visit.weight kg literal", not problems, "; ".join(problems))


def check_s63():
    problems = []
    src = read(MAIN / "ui/AncApp.kt")
    if src is None:
        problems.append("ui/AncApp.kt missing")
    else:
        src = strip_comments(src)
        sub_at = src.find("Maternal-Fetal Medicine")
        if sub_at == -1:
            problems.append("AncApp.kt no longer shows the Maternal-Fetal Medicine & Obstetrics subtitle")
        else:
            text_at = src.rfind("Text(", 0, sub_at)
            args, _ = call_args(src, text_at)
            if args is None:
                problems.append("the subtitle Text( not found")
            else:
                if "maxLines = 2" not in args:
                    problems.append("the top bar subtitle Text has no maxLines = 2")
                if "maxLines = 1" in args:
                    problems.append("the top bar subtitle Text still has maxLines = 1 (subtitle cut)")
    record("S63 top bar subtitle Text allows two lines (maxLines = 2)", not problems, "; ".join(problems))


def main():
    check_s59()
    check_s60()
    check_s61()
    check_s62()
    check_s63()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
