#!/usr/bin/env python3
"""R6C static check: the shared delete/Undo layers name no deletable kind; every DeletableType is fully wired.
Stdlib only; textual contract only."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main/java/com/sherifradwan/ancnative"
RES = ROOT / "app/src/main/res"
results = []


def record(name, ok, detail=""):
    results.append(ok)
    print(("PASS" if ok else "FAIL") + " " + name + (": " + detail if detail and not ok else ""))


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return ""


KIND = r"(?:MEDICATION|LAB)"
# A kind is "named" by any of these forms.
NAMED = re.compile(r"DeletableType\.%s\b|SavedNotice\.%s_(?:DELETED|RESTORED|SAVE_UNDONE)\b|ClinicalRecordKind\.%s_(?:DELETE|RESTORE)\b|"
                   r"\b%s_(?:DELETE|RESTORE|DELETED|RESTORED|SAVE_UNDONE)\b|\b(?:Medication|Lab)DeleteAdapter\b" % ((KIND,) * 4))

# Files that may name a kind freely: the per-kind adapters, the registry, the receipt-kind enum, the editor-hook registry.
FREE = {"storage/MedicationDeleteAdapter.kt", "storage/LabDeleteAdapter.kt", "storage/DeletableRecords.kt",
        "storage/ClinicalCommands.kt", "state/EditorHooks.kt"}
# Files that may name a kind only on lines of the stated shape (strings table, call sites).
LINE_OK = {
    "state/AncState.kt": re.compile(r"^\s*(?:%s_(?:DELETED|RESTORED|SAVE_UNDONE),|SavedNotice\.%s_(?:DELETED|RESTORED|SAVE_UNDONE) -> R\.string\.\w+)\s*$" % (KIND, KIND)),
    "ui/DeleteUndoUi.kt": re.compile(r"^\s*DeletableType\.%s -> R\.string\.deletable_type_\w+\s*$" % KIND),
    "ui/WorkspaceScreen.kt": re.compile(r"(?:RequestDelete\(.*DeletableType\.%s|DeleteIconButton\(DeletableType\.%s)" % (KIND, KIND)),
    "state/AncViewModel.kt": re.compile(r"^\s*type = DeletableType\.%s,\s*$" % KIND),
}


def shared_layers_name_no_kind():
    bad = []
    for f in sorted(MAIN.rglob("*.kt")):
        rel = f.relative_to(MAIN).as_posix()
        if rel in FREE:
            continue
        ok_line = LINE_OK.get(rel)
        for n, line in enumerate(read(f).splitlines(), 1):
            if NAMED.search(line) and not (ok_line and ok_line.search(line)):
                bad.append("%s:%d" % (rel, n))
    return bad


def enum_values(src, name):
    m = re.search(r"enum class %s\s*\{(.*?)\n\}" % name, src, re.S)
    return re.findall(r"^\s*([A-Z][A-Z_]*)\s*,?\s*$", m.group(1), re.M) if m else []


def main():
    reg = read(MAIN / "storage/DeletableRecords.kt")
    types = enum_values(reg, "DeletableType")
    record("R6C0 DeletableType enum is readable and non-empty", bool(types), "no values parsed")

    bad = shared_layers_name_no_kind()
    record("R6C1 state, coordinator, UI and repository name no kind outside adapters, registry, strings and call sites",
           not bad, ", ".join(bad))

    # No second DeletableType.values() style per-kind 'when' in the coordinator or repository.
    shared = read(MAIN / "state/UndoCoordinator.kt") + read(MAIN / "storage/RecordsRepository.kt") + read(MAIN / "storage/ClinicalDeleteRule.kt")
    record("R6C2 coordinator, repository and delete rule never branch on DeletableType",
           not re.search(r"when\s*\(\s*\w*\.?type\s*\)|DeletableType\.\w+\s*->", shared), "found a when on type")

    registry_list = re.search(r"listOf\((.*?)\)\.associateBy", reg, re.S)
    registered = re.findall(r"(\w+DeleteAdapter)", registry_list.group(1)) if registry_list else []
    adapters = {}
    for f in sorted((MAIN / "storage").glob("*DeleteAdapter.kt")):
        src = read(f)
        m = re.search(r"object\s+(\w+)\s*:\s*DeletableRecordAdapter<", src)
        t = re.search(r"override val type: DeletableType = DeletableType\.([A-Z_]+)", src)
        if m and t:
            adapters[t.group(1)] = (m.group(1), src)

    missing = [t for t in types if t not in adapters]
    record("R6C3 every DeletableType has exactly one adapter object", not missing and len(adapters) == len(types),
           "no adapter for %s; adapters=%s" % (missing, sorted(adapters)))

    unregistered = [t for t, (obj, _) in adapters.items() if obj not in registered]
    record("R6C4 every adapter is listed in DeletableRegistry", not unregistered and len(registered) == len(set(registered)),
           "not registered: %s" % unregistered)

    kinds = enum_values(read(MAIN / "storage/ClinicalCommands.kt"), "ClinicalRecordKind")
    notices = enum_values(read(MAIN / "state/AncState.kt"), "SavedNotice")
    strings = read(RES / "values/strings.xml")
    arabic = read(RES / "values-ar/strings.xml") if (RES / "values-ar/strings.xml").exists() else None
    ui = read(MAIN / "ui/DeleteUndoUi.kt")
    hooks = read(MAIN / "state/EditorHooks.kt")
    problems, tags, allkinds = [], [], []
    for t, (obj, src) in adapters.items():
        dk = re.search(r'deleteReceiptKind: String = "(\w+)"', src)
        rk = re.search(r'restoreReceiptKind: String = "(\w+)"', src)
        tag = re.search(r'fingerprintTag: String = "(\w+)"', src)
        if not (dk and rk and tag):
            problems.append(t + " adapter lacks literal tag or receipt kinds")
            continue
        tags.append(tag.group(1))
        allkinds += [dk.group(1), rk.group(1)]
        for k in (dk.group(1), rk.group(1)):
            if k not in kinds:
                problems.append("%s: %s is not a ClinicalRecordKind" % (t, k))
        for n in re.findall(r"SavedNotice\.([A-Z_]+)", src):
            if n not in notices:
                problems.append("%s: SavedNotice.%s missing" % (t, n))
        low = t.lower()
        for scope, text in (("values", strings), ("values-ar", arabic)):
            if text is not None and 'name="deletable_type_%s"' % low not in text:
                problems.append("%s: string deletable_type_%s missing in %s" % (t, low, scope))
        if "DeletableType.%s ->" % t not in ui:
            problems.append("%s: no labelResource branch" % t)
        if "DeletableType.%s to" % t not in hooks:
            problems.append("%s: no EditorHooks entry" % t)
    if len(set(tags)) != len(tags):
        problems.append("fingerprintTag not distinct: %s" % tags)
    if len(set(allkinds)) != len(allkinds):
        problems.append("receipt kinds not distinct: %s" % allkinds)
    record("R6C5 every kind has distinct tag and receipt kinds (real ClinicalRecordKind values), real notices, label string and editor hook",
           not problems, "; ".join(problems))

    doc = read(ROOT / ".agents/pipeline/ADDING_A_DELETABLE_KIND.md")
    record("R6C6 ADDING_A_DELETABLE_KIND.md exists and names the registry, the adapter interface and the check",
           all(w in doc for w in ("DeletableRegistry", "DeletableRecordAdapter", "check_r6c.py")), "doc missing or incomplete")
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
