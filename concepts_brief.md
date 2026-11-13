# Mac Companion App for the ANC EMR

* **Author:** sherifrad
* **Date:** 2026-11-13

## Overview

The clinic owner wants a Mac app to use during the patient visit, so cases are typed once on the Mac instead of on paper and then again on the phone. Opening a case on the Mac shows the same sections, field names and order as the Android app, laid out for a wide screen, with keyboard shortcuts to move between sections. It reuses the Android app's clinical rules instead of copying them.

Every save is written on the Mac first, then encrypted on the device with a key only the owner holds. It is sent to Firebase right away when online and queued when offline. The Android app, which has no Firebase today, will later read the same records, so a case entered on the Mac appears on the phone. There is one user. There are no live patients yet; later the owner will restore a phone backup onto the Mac and continue from there.

Success means: every case entered on the Mac appears on the phone, no case is lost, no case is duplicated. The target is "immediate", and a few minutes is acceptable on a bad connection.

## Analysis

**What the code confirms.**
- `model/` is almost free of Android code (1 of 21 files). The `state/` reducers are plain Kotlin. Only `AncViewModel`, `DraftAutosaver` and `UndoCoordinator` are tied to Android. A Mac app can run on the JVM and share this code.
- `storage/` is the hard part. SQLCipher is Android-only. The vault key is bound to the phone's hardware and cannot be exported. `RecordsRepository` mixes rules with database calls. The repo is one Gradle module with no shared module.
- Every record already has a client-made UUID, a revision counter and command receipts with an operation id. That is a base for retry-safe sync. Gaps: revision types differ (Int and Long), most tables have no updated-at time, no device id and no delete marker, deletes are hard deletes, and photo upload code is a stub.
- Case ids are built as ANC-year-(count+1) from the local list. Two offline devices can create the same case id for two different patients. This is the most realistic source of duplicates.
- The existing backup is AES-GCM with a passphrase, schema version 3 only, no photo bytes, and restore replaces everything and wipes receipts. A Mac can read a phone backup if it has the passphrase.

**Main objections from the reviewers.**
1. **Two products in one brief.** A desktop app and a sync system are separate pieces of work. The phone side of sync does not exist yet, so the Mac alone cannot meet the success test.
2. **Accepting key loss conflicts with "no case lost".** If the Mac holds the only key and the Mac fails, every record is unreadable. Cheap recovery is needed: a printed recovery phrase or a copy held on the phone, with a tested restore.
3. **Key words have no testable meaning.** "Case", "no duplicate", "same sections", "immediately" and "full app" need exact definitions. "Immediate" for the Mac upload and "phone sees it" are different clocks.
4. **One editor at a time is a habit, not a guarantee.** The phone and Mac may both be open during a visit, so one can overwrite the other.
5. **Sync direction is not settled.** Mac to phone is clear. Phone edits going back, and backup restore, are open.
6. **Client-side encryption limits Firebase.** The server cannot search or filter ciphertext, so search stays on the device and Firebase rules can only restrict access to the owner.
7. **Deletes.** With hard deletes, a deleted record can come back from the other device. Either ban Mac deletes in version 1 or use a deletion marker.
8. **Phone schema.** Adding sync columns to the Android database would break the frozen Room v7 and needs clinical sign-off. Keep sync data in a separate store.
9. **Review load.** Encryption, keys, the record format, backup and restore, and anything in `app/src` are level-3 under AGENTS.md. Each needs a frontier-tier review and an independent test review.
10. **Legal gate.** Whether patient data may be stored in Firebase, and under what consent, is not a design detail. No real patient data should leave a device until the owner confirms this.

**Risks.** Silent queue stalls (expired login, quota, crash with a full queue) are the main way a case gets lost. Photos are large and can leave a case showing without its images. Restore wipes receipts, so retry-safety does not survive a restore. Restoring a Mac backup into the phone would erase phone-only edits. Runaway retry loops can raise Firebase costs. Wrong-key, corrupt-file and version errors all show the same message today.

**Ideal Final Result.** A Mac app on the shared Kotlin clinical rules opens a case in the phone's section order, with keyboard navigation. Every save goes to local storage first, then into a versioned, encrypted, retry-safe record pushed whenever the network is up. The doctor sees only "waiting" or "safe in cloud". The phone pulls the same records and marks photos still pending. A reconciliation screen on both devices proves nothing is missing and nothing is duplicated. Conflicts never overwrite, deletes cannot come back, and the key sits in the Mac Keychain with a recovery phrase that has been tested. The server sees only ciphertext and minimal non-identifying routing data.

## Goals vs. Non-Goals

* **In-Scope Goals:**
  - Mac app with the phone's sections, fields, order and rules, a comfortable wide-screen layout and keyboard shortcuts between sections.
  - Offline use with an outbox that uploads when the connection returns.
  - Device-side encryption with a key only the owner holds, plus a drilled recovery path.
  - A record format (an "envelope") that carries one record: id, type, revision, device, time, schema version, deleted flag, key id, ciphertext.
  - Proof of "none lost, none duplicated" through a reconciliation check.
  - Import of a phone backup into the Mac as one more producer of records.
* **Out-of-Scope Non-Goals (for version 1):**
  - Other users, roles or sharing.
  - Server-side search over patient data.
  - Automatic merging of clinical fields.
  - Deleting records on the Mac, unless a deletion marker is built.
  - Changes to the frozen Room v7 schema without sign-off.
  - Photo sync (decided: photos stay on the phone).

## Alternatives Weighed

* **Compose Desktop on the JVM, sharing `model/`, `state/` and the pure parts of `storage/`.** Recommended by most reviewers. Keeps one copy of the clinical rules.
* **Full Kotlin Multiplatform common code.** Rejected for now: needs `java.time`, UUID and `org.json` removed, which is large churn with no stated benefit.
* **Rewrite in another language, or a web app.** Rejected: copies the clinical rules, which is the worst drift risk, and a browser is a weak place to hold the key.
* **Firebase as a "dumb mailbox" of opaque encrypted envelopes.** The original concept. Kept as the target transport.
* **Encrypted per-case file bundle in a synced folder, imported by the phone by id.** Raised by one reviewer: meets the success test with a free offline queue and removes most of the cloud-consent question. Compatible with Firebase if the envelope contains nothing Firebase-specific. Rejected by the owner: Firebase first, to avoid writing the sync code twice.
* **Conflict handling.** Last writer wins was weighed and leaned against. Preferred: reject stale writes by revision and keep both versions, plus an edit lease with a "being edited on another device" banner. Never merge clinical fields automatically.
* **Photos.** Either defer with an honest "photo not synced" state, or send them as separate encrypted blobs named by content hash, uploaded before the record that points to them.

## Key Decisions & Staging for Next Steps

**Decided by the owner on 2026-11-13:**
1. **Order of work.** Build and prove the Mac app first, working offline. Then connect the Mac to Firebase. Then connect the phone to Firebase so it meets the Mac's records there.
2. **Transport.** Firebase from the start. The synced-folder option is rejected, to avoid writing the sync code twice. The record format still must not depend on Firebase, so the transport can change without rewriting the records.
3. **Key safety.** The owner accepts any measure that keeps the key and the cases safe. Plan: a recovery phrase printed at setup, a restore drill passed before any real upload, and the Mac never being the only holder of key and data. A copy of the key held on the phone is a later option.
4. **Legal position.** The owner states that all cases may be stored in Firebase and gives consent for it. This is recorded as the owner's decision. Patient consent and local rules remain the owner's responsibility, and the data stays encrypted on the device before upload.
5. **Photos.** Not synced. Photos stay on the phone, taken per case as the Android app does today. The Mac case view should show a clear "photos are on the phone" note instead of an empty section. The phone's existing backup also carries no photo bytes, so restoring a backup to the Mac will not bring photos either.

**Confirmed by the owner (second round):**
- A "case" is the whole patient record with everything related to it: visits, labs, scans, medications and so on. Internally each part keeps its own version, so a small edit does not resend the whole case.
- Speed: the Mac uploads as soon as it is online, and the phone should show new records immediately when it is open and connected. The owner finds 5 minutes long. It is only a worst-case limit for a bad connection, not the target.
- No deleting records on the Mac in version 1.
- **Two-way sync is required.** The phone stays fully editable, because the owner is sometimes away from the Mac. Changes made on the phone must reach the Mac, and changes on the Mac must reach the phone. A read-only phone was proposed by mistake and is rejected.
- **Sync changes, not whole cases.** Each edit is sent as a small versioned change, not the full patient record in both directions.
- **A sync engine is a core part of the project.** It must be studied properly before it is built (see "Sync engine study").
- **Check on both sides.** Each device must be able to check Firebase for changes and for problems, and show the result.
- Photos stay on the phone for the early versions.

**The new problem two-way sync brings.** The owner is the only user, but the phone can be edited offline while the Mac edits the same case, so two versions of one case can exist. The engine must handle that without losing or overwriting clinical data.

**Sync engine study (to do before any sync code).** Compare the main methods, with how each fits this clinic:
- **Operation log:** every edit is saved as a small record (who, when, which field, old value, new value). Devices exchange only the records the other side has not seen. Gives per-change sync and a full history.
- **Optimistic concurrency by revision:** a change is accepted only if it was made on the latest revision. A stale change is held for review. Simple and safe, but needs a resolution step.
- **Per-field last-writer-wins with logical clocks:** the newest edit of each field wins. Simple, but can silently drop a clinical value, so it is risky for this data.
- **CRDTs:** data types that always merge without conflict. Strong for text and lists, but hard to apply to clinical fields where a wrong merge matters.
- **Three-way merge:** compare both versions against their common ancestor. Fields that changed on only one side merge on their own.
- **Existing services:** how far Firestore's own offline cache and change listeners can carry this, and what they cannot do with encrypted data.

**Conflict rule (accepted by the owner):** changes to different fields of one case merge automatically. If the same field was edited on both devices, the app never picks a winner. It shows both values, each with the date, the time and the source (Mac or Android), and the owner chooses. Nothing is overwritten without a saved copy of the other value.

**Suggested order of work:**
0. Decision record, record-format spec with sample JSON, and the sync engine study with a chosen method. The record format and change log are designed for two-way sync from the start, even though the Mac is built first.
1. One-week spike: extract `model/`, `state/` and the pure storage rules into a JVM module, build a Mac target, and run the existing unit tests. If it takes much longer, fall back to a rewrite with shared test fixtures.
2. Mac app, offline only: same sections, wide layout, keyboard shortcuts between sections. The owner tests it in real use before anything else is built.
3. Encryption and the key recovery drill, passed before any real upload.
4. Firebase upload from the Mac: outbox with a visible "N unsynced" count, plus a reconciliation report.
5. Phone two-way sync with Firebase, with sync data kept in a separate store, not in Room v7.
6. Backup restore into the Mac.

**Other decisions to carry into the draft:**
- Fix the case-id collision with a per-device prefix or by letting the Mac stage and reassign.
- Use the record id as the Firebase document id so retries change nothing. Normalise the revision to one type.
- Use a separate record-encryption key, not the phone vault key, wrapped by the Mac Keychain.
- Use multi-factor sign-in on the Firebase account and add PHI-free diagnostics.
- Treat the level-3 areas (encryption, keys, record format, backup and restore, anything in `app/src`) as milestone review gates and update `.agents/pipeline/HANDOFF_AGY.md` with each one.
