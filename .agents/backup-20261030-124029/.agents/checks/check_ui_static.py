#!/usr/bin/env python3
"""P10 static source check for the delivery card / parser / badges refactor (MACA round 2026-10-29).

Uses stdlib only. Reads Kotlin sources under app/src/main and prints PASS/FAIL for
sub-checks (a)-(f) of oracle P10 in .agents/pipeline/SPEC_FIX.md, then exits 1 if any
sub-check fails. Verifies nothing at runtime; textual/CSS-free source contract only.

Sub-checks:
  (a) R.string.delivery_planning_notice is referenced inside DeliveryPlanningCard.
  (b) R.string.agreed_edd, current_gestational_age, manual_risk, fetal_presentation and
      placental_site are used in WorkspaceScreen.kt (scoped to the DeliveryPlanningCard body,
      because the five facts belong to the delivery card; today they live in other cards as
      hard-coded English labels, which is exactly the defect P10 targets).
  (c) No contentDescription string literal on the delivery edit button; it must use
      stringResource(R.string.edit_delivery_plan).
  (d) ClinicalBadges.kt PatientAgeBadge calls clearAndSetSemantics (or mergeDescendants)
      and formatAgeYears.
  (e) WorkspaceScreen.kt has no inline endsWith("-") Rh logic and uses
      isRhNegative(...)/isHighRisk(...).
  (f) parseDeliveryNotes is not defined in WorkspaceScreen.kt.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
UI_DIR = REPO_ROOT / "app" / "src" / "main" / "java" / "com" / "sherifradwan" / "ancnative" / "ui"

WORKSPACE = UI_DIR / "WorkspaceScreen.kt"
BADGES = UI_DIR / "ClinicalBadges.kt"

FACT_STRING_IDS = [
    "agreed_edd",
    "current_gestational_age",
    "manual_risk",
    "fetal_presentation",
    "placental_site",
]


def read(path: Path) -> str:
    if not path.is_file():
        return ""
    return path.read_text(encoding="utf-8")


def composable_region(source: str, signature_regex: str) -> str:
    """Source slice from the composable's signature to the next top-level close brace
    (a '}' at column 0) or the next @Composable annotation, whichever comes first."""
    m = re.search(signature_regex, source)
    if not m:
        return ""
    rest = source[m.start():]
    candidates = []
    m2 = re.search(r"^}\s*$", rest, re.M)
    if m2:
        candidates.append(m2.start())
    m3 = re.search(r"\n@Composable\b", rest[1:])
    if m3:
        candidates.append(m3.start() + 1)
    end = min(candidates) if candidates else len(rest)
    return rest[:end]


def run() -> int:
    ws = read(WORKSPACE)
    badges = read(BADGES)
    relative_ws = WORKSPACE.relative_to(REPO_ROOT) if WORKSPACE.is_file() else "WorkspaceScreen.kt"
    relative_badges = BADGES.relative_to(REPO_ROOT) if BADGES.is_file() else "ClinicalBadges.kt"
    results = []
    any_fail = False
    ws_present = ws != ""
    badges_present = badges != ""

    # (a)
    card = composable_region(ws, r"fun\s+DeliveryPlanningCard\s*\(")
    if not ws_present:
        results.append((False, "a", f"{relative_ws} not found"))
    elif not card:
        results.append((False, "a", "DeliveryPlanningCard not found in WorkspaceScreen.kt"))
    elif "R.string.delivery_planning_notice" in card:
        results.append((True, "a", "delivery_planning_notice referenced inside DeliveryPlanningCard"))
    else:
        results.append((False, "a", "DeliveryPlanningCard does not reference R.string.delivery_planning_notice"))

    # (b)
    if not ws_present:
        results.append((False, "b", f"{relative_ws} not found"))
    elif not card:
        results.append((False, "b", "DeliveryPlanningCard not found in WorkspaceScreen.kt"))
    else:
        missing = [i for i in FACT_STRING_IDS if f"R.string.{i}" not in card]
        if missing:
            results.append((False, "b", "DeliveryPlanningCard missing R.string ids: " + ", ".join(missing)))
        else:
            results.append((True, "b", "all five fact string resources used in DeliveryPlanningCard"))

    # (c)
    if not ws_present:
        results.append((False, "c", f"{relative_ws} not found"))
    elif not card:
        results.append((False, "c", "DeliveryPlanningCard not found in WorkspaceScreen.kt"))
    else:
        literal_cd = re.findall(r'contentDescription\s*=\s*"', card)
        has_resource_desc = "R.string.edit_delivery_plan" in card
        if literal_cd:
            results.append((False, "c", "delivery edit button uses a contentDescription string literal: " + ", ".join(literal_cd)))
        elif not has_resource_desc:
            results.append((False, "c", "delivery edit button does not use stringResource(R.string.edit_delivery_plan)"))
        else:
            results.append((True, "c", "delivery edit button uses stringResource(R.string.edit_delivery_plan), no literal contentDescription"))

    # (d)
    if not badges_present:
        results.append((False, "d", f"{relative_badges} not found"))
    else:
        semantic = ("clearAndSetSemantics" in badges) or ("mergeDescendants" in badges)
        age_fn = "formatAgeYears" in badges
        if semantic and age_fn:
            results.append((True, "d", "PatientAgeBadge uses clearAndSetSemantics/mergeDescendants and formatAgeYears"))
        else:
            missing = []
            if not semantic:
                missing.append("clearAndSetSemantics/mergeDescendants")
            if not age_fn:
                missing.append("formatAgeYears")
            results.append((False, "d", "ClinicalBadges.kt missing: " + ", ".join(missing)))

    # (e)
    if not ws_present:
        results.append((False, "e", f"{relative_ws} not found"))
    else:
        inline_rh = re.search(r'\.endsWith\(\s*"\-"\s*\)', ws)
        uses_rh = re.search(r"\bisRhNegative\s*\(", ws)
        uses_risk = re.search(r"\bisHighRisk\s*\(", ws)
        if inline_rh:
            results.append((False, "e", f"inline endsWith(\"-\") Rh logic present in WorkspaceScreen.kt: {inline_rh.group(0)}"))
        elif not (uses_rh and uses_risk):
            missing = [n for n, u in (("isRhNegative", uses_rh), ("isHighRisk", uses_risk)) if not u]
            results.append((False, "e", "WorkspaceScreen.kt does not use: " + ", ".join(missing)))
        else:
            results.append((True, "e", "WorkspaceScreen.kt uses isRhNegative/isHighRisk, no inline endsWith(\"-\") logic"))

    # (f)
    if not ws_present:
        results.append((False, "f", f"{relative_ws} not found"))
    else:
        defined = re.search(r"\bfun\s+parseDeliveryNotes\s*\(", ws)
        defined_section = re.search(r"\bdata\s+class\s+DeliveryNoteSections\s*\(", ws)
        if defined or defined_section:
            found = []
            if defined:
                found.append("fun parseDeliveryNotes(")
            if defined_section:
                found.append("data class DeliveryNoteSections(")
            results.append((False, "f", "parser must live in ui/DeliveryNotesParser.kt, still defined in WorkspaceScreen.kt: " + ", ".join(found)))
        else:
            results.append((True, "f", "parseDeliveryNotes/DeliveryNoteSections not defined in WorkspaceScreen.kt"))

    for ok, sub, message in results:
        print(f"{'PASS' if ok else 'FAIL'} P10({sub}) {message}")
        any_fail = any_fail or not ok
    total_ok = sum(1 for ok, _, _ in results if ok)
    print(f"{'PASS' if not any_fail else 'FAIL'} P10 overall: {total_ok}/{len(results)} sub-checks passed")
    return 1 if any_fail else 0


if __name__ == "__main__":
    sys.exit(run())
