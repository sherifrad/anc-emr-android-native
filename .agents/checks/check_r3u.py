#!/usr/bin/env python3
"""S89-S91 static source check for round 3u (SPEC_FIX_R3.md, section "Round 3u").

Stdlib only. Prints PASS/FAIL per check, exits 1 if any FAIL.
Textual source contract only; verifies nothing at runtime.
  S89 RecordsDatabase.kt declares version = 6 and lists DraftEntity; RecordsMigration.kt
      defines from5To6; RecordsRepository's SCHEMA_VERSION is 6.
  S90 DraftEntity has primary key patient_id + screen_kind and no ForeignKey;
      EncryptedBackupManager.kt and VaultController.kt contain no "draft";
      restoreCanonicalBackup deletes drafts.
  S91 DraftCodec.kt, DraftRecord.kt and the draft methods of RecordsRepository.kt use no
      SharedPreferences, File write or Log call.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STORAGE = ROOT / "app/src/main/java/com/sherifradwan/ancnative/storage"
DATABASE = STORAGE / "RecordsDatabase.kt"
MIGRATION = STORAGE / "RecordsMigration.kt"
REPOSITORY = STORAGE / "RecordsRepository.kt"
ENTITIES = STORAGE / "RecordEntities.kt"
BACKUP = STORAGE / "EncryptedBackupManager.kt"
CONTROLLER = STORAGE / "VaultController.kt"
CODEC = STORAGE / "DraftCodec.kt"
RECORD = STORAGE / "DraftRecord.kt"

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


def code_of(path, problems):
    src = read(path)
    if src is None:
        problems.append(path.name + " missing")
        return None
    return strip_comments(src)


def function_body(code, name):
    """Text from 'fun name' up to the next declaration at the same indent."""
    m = re.search(r"^([ \t]*)(?:\w+\s+)*fun\s+" + re.escape(name) + r"\b", code, flags=re.M)
    if not m:
        return None
    rest = code[m.end():]
    nxt = re.search(r"^" + m.group(1) + r"(?:\w+\s+)*(?:fun|val|var|companion|class)\b", rest, flags=re.M)
    return rest[:nxt.start()] if nxt else rest


def check_s89():
    problems = []
    db = code_of(DATABASE, problems)
    if db is not None:
        if not re.search(r"\bversion\s*=\s*6\b", db):
            problems.append("RecordsDatabase.kt does not declare version = 6")
        if not re.search(r"\bDraftEntity\b", db):
            problems.append("RecordsDatabase.kt does not list DraftEntity")
    mig = code_of(MIGRATION, problems)
    if mig is not None and not re.search(r"\bfun\s+from5To6\s*\(", mig):
        problems.append("RecordsMigration.kt lacks fun from5To6(")
    repo = code_of(REPOSITORY, problems)
    if repo is not None and not re.search(r"\bconst\s+val\s+SCHEMA_VERSION\s*=\s*6\b", repo):
        problems.append("RecordsRepository SCHEMA_VERSION is not 6")
    record("S89 database version 6 lists DraftEntity, from5To6 exists, repository SCHEMA_VERSION is 6",
           not problems, "; ".join(problems))


def check_s90():
    problems = []
    ent = code_of(ENTITIES, problems)
    if ent is not None:
        m = re.search(r"class\s+DraftEntity\b", ent)
        if not m:
            problems.append("RecordEntities.kt lacks DraftEntity")
        else:
            start = ent.rfind("@Entity", 0, m.start())
            nxt = re.search(r"^@Entity", ent[m.end():], flags=re.M)
            whole = ent[start if start >= 0 else m.start():m.end() + (nxt.start() if nxt else len(ent))]
            if not re.search(r"primaryKeys\s*=\s*\[\s*\"patient_id\"\s*,\s*\"screen_kind\"\s*\]", whole):
                problems.append("DraftEntity primaryKeys is not [patient_id, screen_kind]")
            if "ForeignKey" in whole:
                problems.append("DraftEntity has a ForeignKey")
    for path in (BACKUP, CONTROLLER):
        code = code_of(path, problems)
        if code is not None and re.search(r"draft", code, flags=re.I):
            problems.append(path.name + " mentions draft")
    repo = code_of(REPOSITORY, problems)
    if repo is not None:
        body = function_body(repo, "restoreCanonicalBackup")
        if body is None:
            problems.append("restoreCanonicalBackup missing")
        elif not re.search(r"deleteAllDrafts\s*\(|DELETE\s+FROM\s+drafts\b", body):
            problems.append("restoreCanonicalBackup does not delete drafts")
    record("S90 DraftEntity key patient_id+screen_kind without ForeignKey, backup/controller draft-free, restore deletes drafts",
           not problems, "; ".join(problems))


FORBIDDEN = re.compile(
    r"SharedPreferences|\bLog\s*\.|\bandroid\.util\.Log\b|\bFile\s*\(|\bFileOutputStream\b"
    r"|\bwriteText\b|\bwriteBytes\b|\bFiles\s*\.\s*write|\bopenFileOutput\b"
)


def check_s91():
    problems = []
    for path in (CODEC, RECORD):
        code = code_of(path, problems)
        if code is not None:
            hit = FORBIDDEN.search(code)
            if hit:
                problems.append(path.name + " uses " + hit.group(0))
    repo = code_of(REPOSITORY, problems)
    if repo is not None:
        for name in ("saveDraft", "loadDraft", "loadDrafts", "deleteDraft", "deleteDrafts"):
            body = function_body(repo, name)
            if body is None:
                problems.append("RecordsRepository lacks " + name)
                continue
            hit = FORBIDDEN.search(body)
            if hit:
                problems.append(name + " uses " + hit.group(0))
    record("S91 DraftCodec, DraftRecord and repository draft methods use no SharedPreferences, File write or Log",
           not problems, "; ".join(problems))


def main():
    check_s89()
    check_s90()
    check_s91()
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
