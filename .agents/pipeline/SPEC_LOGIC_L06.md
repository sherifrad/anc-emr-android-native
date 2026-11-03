# Specification: Logic Round L06 - Custom Choices Store for "+ Add other"

## 1. Purpose & Scope
Antenatal care entry screens include lists of predefined chips/choices (lab tests and result presets, ultrasound finding choices, medication dose/route/drug, previous pregnancy outcomes/modes/complications, clinical attention quick phrases, and visit entry choices such as oedema, movements, urine). Each list ends with a `"+ Add other"` chip.

This round implements the core logic and storage layer for custom choices prior to screen integration:
1. Type-safe list identification (`CustomChoiceList` enum with stable string keys).
2. Pure validation function (`validateCustomChoice`) enforcing trimming, maximum length, rejection of blank/newlines, and case-insensitive deduplication against existing items.
3. Storage in `ClinicalCodexRepository` backed by persistent SharedPreferences, providing reactive `StateFlow` streams per list.
4. Mutation operations: `addCustomChoice`, `renameCustomChoice`, `removeCustomChoice`, and `restoreCustomChoice` (supporting Undo).
5. Encrypted backup and restore inclusion in `EncryptedBackupManager` payload envelope without breaking backward compatibility (envelope schemaVersion remains 3).
6. Clinical safety constraints: Dating methods (LMP, IVF, Scan, Clinical) are strictly forbidden from having custom choices. Custom choice values are labels only, never patient health identifiers.

## 2. Outcome Oracles
- **O1 (Pure Validation)**:
  - Blank or whitespace-only strings return `Invalid(Reason.BLANK)`.
  - Strings exceeding max length (default 80 characters, attention phrases up to 140) return `Invalid(Reason.TOO_LONG)`.
  - Strings containing newlines or control characters are rejected or stripped to single-line text.
  - Case-insensitive duplicates against existing items return `Invalid(Reason.DUPLICATE)`.
  - Valid strings return `Valid(sanitized)`.
- **O2 (Dating Method Invariance)**:
  - Attempting to query or create custom choices for dating methods is forbidden or non-existent in `CustomChoiceList`.
- **O3 (Reactive Store Operations)**:
  - Adding a valid choice persists it and emits updated list to subscribers.
  - Removing a choice removes it from the store and returns the deleted `CustomChoiceItem` for Undo.
  - Calling `restoreCustomChoice` re-inserts the choice at its previous or correct place, notifying subscribers.
  - Renaming an existing choice updates its label and persists changes.
- **O4 (Backup & Restore Round-Trip)**:
  - `EncryptedBackupManager.createEncryptedBackup` serializes custom choices into the encrypted envelope.
  - `EncryptedBackupManager.decryptAndParseBackup` parses the custom choices alongside patient records.
  - Restoring a backup without `customChoices` defaults gracefully to empty/defaults without error.
  - Zero data loss: exported custom choices match restored custom choices exactly.
- **O5 (Isolation & Safety)**:
  - No patient data or clinical record kinds are touched.
  - Room database schema version remains 6; no database migrations or table edits.
