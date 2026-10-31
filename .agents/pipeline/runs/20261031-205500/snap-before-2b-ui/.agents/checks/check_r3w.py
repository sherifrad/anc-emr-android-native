#!/usr/bin/env python3
"""S94-S96 static source check for round 3w (SPEC_FIX_R3.md, section "Round 3w").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S94 state/DraftRecovery.kt defines DraftRecoveryPrompt, activeEditorSessionId, activeEditorBaseRevision,
      isDraftStale, DraftAge, draftAge and has no android import; AncState.kt defines draftRecoveryPrompt,
      DraftRecoveryFound, AcceptDraftRecovery, DiscardDraftRecovery; DraftAutosaver.kt defines holdSlot, releaseSlot.
  S95 AncViewModel.kt calls loadDraft, holdSlot, releaseSlot, deleteDraft; AncApp.kt has the testTags
      draftContinue and draftDiscard, dismissOnBackPress = false, dismissOnClickOutside = false and references
      draftRecoveryPrompt; no ui file other than AncApp.kt mentions DraftRecoveryPrompt.
  S96 no string in values/strings.xml says "memory-only" or "discarded on lock"; the five
      draft_recovery_* names and the nine draft_screen_* names exist there.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res"
RECOVERY = MAIN / "state/DraftRecovery.kt"
ANC_STATE = MAIN / "state/AncState.kt"
AUTOSAVER = MAIN / "state/DraftAutosaver.kt"
VIEWMODEL = MAIN / "state/AncViewModel.kt"
ANC_APP = MAIN / "ui/AncApp.kt"
STRINGS = (RES / "values/strings.xml",)

RECOVERY_STRINGS = (
    "draft_recovery_title", "draft_recovery_body", "draft_recovery_stale",
    "draft_recovery_continue", "draft_recovery_discard",
)
SCREEN_STRINGS = tuple(
    "draft_screen_" + k for k in (
        "visit", "patient_profile", "histories", "previous_pregnancy", "patient_status",
        "dating", "ultrasound", "lab", "medication",
    )
)

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


def strip_xml_comments(src):
    return re.sub(r"<!--.*?-->", "", src, flags=re.S)


def code_of(path, problems):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_comments(src)


def defines(code, kinds, name):
    return re.search(r"\b(?:" + kinds + r")\s+" + re.escape(name) + r"\b", code) is not None


def check_s94():
    problems = []
    rec = code_of(RECOVERY, problems)
    if rec is not None:
        for name in ("DraftRecoveryPrompt", "DraftAge"):
            if not defines(rec, "class|interface|object", name):
                problems.append("DraftRecovery.kt lacks " + name)
        for name in ("activeEditorSessionId", "activeEditorBaseRevision", "isDraftStale", "draftAge"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", rec):
                problems.append("DraftRecovery.kt lacks fun " + name)
        if re.search(r"\bimport\s+android\b", rec):
            problems.append("DraftRecovery.kt imports android")
    state = code_of(ANC_STATE, problems)
    if state is not None:
        if not re.search(r"\bdraftRecoveryPrompt\s*:", state):
            problems.append("AncState.kt lacks draftRecoveryPrompt")
        for name in ("DraftRecoveryFound", "AcceptDraftRecovery", "DiscardDraftRecovery"):
            if not defines(state, "class|object", name):
                problems.append("AncState.kt lacks event " + name)
    saver = code_of(AUTOSAVER, problems)
    if saver is not None:
        for name in ("holdSlot", "releaseSlot"):
            if not re.search(r"\bfun\s+" + name + r"\s*\(", saver):
                problems.append("DraftAutosaver.kt lacks fun " + name)
    record("S94 DraftRecovery.kt has the prompt, session/revision helpers, stale and age rules and no android import; "
           "AncState has the prompt field and three events; DraftAutosaver has holdSlot and releaseSlot",
           not problems, "; ".join(problems))


def check_s95():
    problems = []
    vm = code_of(VIEWMODEL, problems)
    if vm is not None:
        for name in ("loadDraft", "holdSlot", "releaseSlot", "deleteDraft"):
            if not re.search(r"\b" + name + r"\s*\(", vm):
                problems.append("AncViewModel does not call " + name)
    app = code_of(ANC_APP, problems)
    if app is not None:
        for tag in ("draftContinue", "draftDiscard"):
            if '"' + tag + '"' not in app:
                problems.append("AncApp.kt lacks testTag " + tag)
        for prop in ("dismissOnBackPress", "dismissOnClickOutside"):
            if not re.search(r"\b" + prop + r"\s*=\s*false\b", app):
                problems.append("AncApp.kt lacks " + prop + " = false")
        if "draftRecoveryPrompt" not in app:
            problems.append("AncApp.kt does not reference draftRecoveryPrompt")
    for path in sorted((MAIN / "ui").rglob("*.kt")):
        if path == ANC_APP:
            continue
        code = code_of(path, problems)
        if code is not None and "DraftRecoveryPrompt" in code:
            problems.append("ui/" + path.name + " mentions DraftRecoveryPrompt")
    record("S95 AncViewModel loads, holds, releases and deletes drafts; AncApp has the modal alert with both tags "
           "and no dismiss; no other ui file mentions DraftRecoveryPrompt",
           not problems, "; ".join(problems))


def check_s96():
    problems = []
    for path in STRINGS:
        label = path.parent.name + "/" + path.name
        src = read(path)
        if src is None:
            problems.append(label + " missing")
            continue
        xml = strip_xml_comments(src)
        for phrase in ("memory-only", "discarded on lock"):
            if re.search(re.escape(phrase), xml, flags=re.I):
                problems.append(label + " still says '" + phrase + "'")
        for name in RECOVERY_STRINGS + SCREEN_STRINGS:
            if not re.search(r'<string\s+name="' + name + r'"', xml):
                problems.append(label + " lacks " + name)
    record("S96 no memory-only / discarded-on-lock wording in the strings file; recovery and screen-name strings "
           "exist", not problems, "; ".join(problems))


def main():
    check_s94()
    check_s95()
    check_s96()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
