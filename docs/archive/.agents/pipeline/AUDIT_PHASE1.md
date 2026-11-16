# ANC EMR Android audit

I read the security, storage, domain and state layers line by line and spot-checked the UI. I also ran the unit tests and Android Lint. Paths below start at `app/src/main/java/com/sherifradwan/ancnative/`.

**What the app is:** an offline, single-device EMR for antenatal care. The UI is Jetpack Compose, driven by one ViewModel and a reducer. It sits on a Room + SQLCipher database whose encryption key is protected by the Android Keystore, with every save going through a command/receipt layer that stops duplicate writes. It also exports AES-GCM backups that the web version of the app can read.

**Overall verdict:** the core engineering is good, much better than a typical prototype. But it is **not safe to use as your real clinic record yet**. Several bugs below can lose data, change a patient's dating without anyone noticing, or put made-up clinical values on a printed report. The project's own audit docs (`.agents/SPEC_AND_AUDIT.md`, `tasks/current_task.md`) mark some of these as fixed when the code shows they aren't.

**Checks I ran:** 113 of 113 unit tests pass. `lintDebug` **fails with 17 errors**. I did not run the app on a device.

---

## 🔴 Critical: patient safety and data integrity

1. **The printed report invents clinical findings.** `ui/report/PatientPrintHtmlGenerator.kt:143-145, 64, 99, 1038, 1042`
   - When a field is empty, the report prints a default value instead of leaving it blank. Defaults include presentation **"Cephalic"**, placenta **"Posterior Grade I"**, AFI **"Adequate (AFI 14cm)"**, risk **"Low Risk"**, allergies **"NKDA"**, and history **"None"**.
   - Example: a breech or placenta-previa patient whose field was left blank prints as cephalic with a posterior placenta.
   - A blank allergy field printed as "NKDA" is a medicolegal hazard. Empty fields must print as "—" or "Not recorded".

2. **Editing a patient's profile can silently re-date her pregnancy.** `state/AncViewModel.kt:285-305`
   - The profile editor is pre-filled with the stored LMP (`state/AncState.kt:1128`).
   - Every time the profile is saved, the app writes a new dating record: method = LMP, agreed EDD = LMP + 280. It sets the ultrasound EDD to null and clears the IVF transfer date, embryo age and notes.
   - So changing only a phone number throws away an ultrasound- or IVF-based EDD. Any failure of this second write is ignored without telling you.

3. **Every new vault is filled with demo patients.** `storage/VaultController.kt:587` → `storage/RecordsRepository.kt:27`
   - `seedInitialSyntheticFixtures()` still runs, so fictional patients (including realistic names such as "فريدة مصطفى أحمد") sit in the real clinic database next to real patients.
   - The task file says this was fixed. Only the `isSynthetic` check was removed; the seeding itself still happens.
   - The demo patients also throw off case-ID numbering, which uses `patients.size + 1`.

4. **One unexpected value can lock you out of every record, and the app then retries in an endless loop.**
   - On unlock, `RecordsRepository.kt:619` checks every patient against a fixed list of allowed values (blood group, risk level, `modelVersion == 3`, and so on). If any single row fails, the whole vault is blocked with `DATABASE_INTEGRITY_FAILED`.
   - `MainActivity.kt:73-79` then calls retry automatically on every `Blocked` state. The cycle Blocked → Inspecting → Locked → Unlock → Blocked repeats forever and no error is ever shown.
   - The easiest way to trigger it is restoring a backup from the web app: restore does no vocabulary validation (`storage/EncryptedBackupManager.kt`).

5. **The auto-retry path can wipe the whole vault.** `storage/VaultController.kt:274-288`
   - If the active-pointer file is missing, `retryInspection()` deletes the entire vault folder (`cleanupUnpublishedArtifacts`) and **every Keystore alias** the app owns (`deleteOrphanAliases`).
   - Because the retry fires automatically (#4), any situation where that file goes missing becomes silent, permanent loss of all records with no confirmation.

6. **Backups lose data.** `storage/EncryptedBackupManager.kt:433, 595-610, 671`
   - Ultrasound EDD and agreed EDD are merged into a single "edd" field, so after a restore the original ultrasound EDD is gone.
   - Attachment image files are never included (`dataUrl` is always null). After a restore, attachment rows point to files that don't exist (`filePath = ""`).
   - Ultrasound photo URIs in the backup point to temporary files on the old phone (see #9).

## 🔴 Critical: security

7. **The PIN lock does almost nothing.**
   - The app opens **unlocked** on every cold start (`security/SecuritySettingsRepository.kt:69`, `isAppLocked = false`).
   - `shouldLockOnBackground()` is never called, so the auto-lock setting is dead code. Only the manual lock button locks the app.
   - The default PIN is `123456` (`:155`) and it is never forced to change.
   - PIN attempts are unlimited, with no throttling (`ui/AppLockScreen.kt:100`).
   - The PIN is stored as SHA-256 with a fixed salt, so only 1 million combinations need to be tried (`:128`).
   - The vault key is created with `setUserAuthenticationRequired(false)` (`storage/AndroidVaultKeys.kt:79`), and `MainActivity.kt:66-71` completes vault "authentication" automatically. **Anyone who picks up the phone can read every patient record.**

8. **Patient photos are stored unencrypted** in `filesDir/patient_attachments` (`storage/PatientAttachmentVault.kt:171`), while the UI labels them "Locally Encrypted & Secured" / "Encrypted & Verified". There is also no `FLAG_SECURE`, so patient data shows in the recent-apps thumbnails and screenshots are allowed.

## 🟠 High

9. **Ultrasound photos will disappear.** `ui/UltrasoundEditor.kt:73-97`
   - The app stores the raw URIs: camera shots stay in `cacheDir`, which the OS clears, and picker URIs lose their read permission after a restart. Neither is copied into the vault.
   - If the camera permission is denied, the app launches the camera anyway, which will crash.
   - `pendingCameraUri` uses `remember` instead of `rememberSaveable`, so the photo is lost if Android recreates the activity while the camera is open.

10. **Crash on Android 8–10.**
    - `security/BiometricAuthManager.kt` uses framework biometric APIs that need API 28–30, but minSdk is 26. These are the 14 `NewApi` lint errors.
    - Opening Settings or the lock screen on those devices throws `NoClassDefFoundError`.
    - Fix: switch to `androidx.biometric`.

11. **Attachment add and delete fail with no message to you.** `state/AncViewModel.kt:111-159`
    - Errors are only written to the log.
    - On delete, the file is removed *before* the database row, so a failure leaves a record pointing at a missing image.
    - The database calls skip the epoch/session checks that every other write uses.

12. **Race between backup restore and lock.** `storage/VaultController.kt:524-527`: restore sets `Unlocked(gate.current())` even if a lock started meanwhile. The UI then shows "unlocked" with a closed database. Export (`:492`) has the same gap.

13. **Printed report drops text after a "<".** Notes are inserted into the HTML without escaping. A note such as "Hb <10, BP >140/90" loses text in print. Visits and scans are also cut off at `take(7)` without saying so.

## 🟡 Medium: architecture and performance

- **The whole database is reloaded after every save.** All patients, with all visits, labs, scans and attachments, are re-read and re-mapped after each write (`loadSnapshot()` after every command). This is fine at 50 patients and will become slow at a few thousand.
- **Edits overwrite in place with no history.** Receipts store only a fingerprint, and attachments are hard-deleted (`RecordsDao.kt:468`). A medical record should keep who changed what, and when.
- **Very large files and duplicated logic.**
  - `ProfileScreen.kt` and `WorkspaceScreen.kt` are about 2,100 lines each, and `AncState.kt` is about 1,200.
  - Gestational-age math is copied into `PatientEditor.kt:180-185` (valid range 0–300 days) and `AncViewModel.kt:290`, while the engine allows up to 320 days.
  - Domain models are still named `Demo*`.
- **Image decoding on the main thread.** Full-resolution bitmaps (`AddAttachmentDialog.kt:294`, `WorkspaceScreen.kt:1924`, `ClinicalAttachmentViewerDialog.kt:82`) and SHA-256 integrity checks run during composition. This causes jank and possible out-of-memory crashes with 12 MP photos. Camera EXIF rotation is also ignored.
- **One clinical command can report failure after it has already saved.** If the patient-revision bump fails, the lab, medication or ultrasound save *returns* `Rejected` inside the transaction instead of throwing. That commits the record but tells you it wasn't saved. `savePreviousPregnancy` does it correctly (it throws).
- **No validation on visits.** Dates can be in the future. Blood pressure and weight are free text and never parsed, so there are no alerts for hypertension or preeclampsia.
- **The real visits are labelled "synthetic".** Visits you enter are saved with `isSyntheticEdit = true` (`RecordsRepository.kt:687`).
- **Dating gaps.** IVF day-6 embryos return no GA with no message, LMP dating doesn't adjust for cycle length, and there is no rule for reconciling ultrasound and LMP dates (ACOG redating).

## 🟢 Low: build and hygiene

- `isMinifyEnabled = false`, so the release APK is 55 MB.
- `Log.d("VAULT_DEBUG")` runs on every recomposition (`MainActivity.kt:64`).
- The locale is forced to English (`MainActivity.kt:27`), although the README advertises Arabic/RTL.
- The backup uses PBKDF2 with 65k iterations; OWASP recommends 600k or more, which would need coordinating with the web app.
- The project is still named "ANC Native Demo".
- `strings.xml` is barely used.
- Lint reports 57 unused resources.

## ✅ What is genuinely good

- The vault state machine: epoch gating, atomic files, Keystore-wrapped passphrase, and bounded reads.
- Duplicate-safe commands that use operation-UUID receipts and fingerprints, with reconciliation when a save's outcome is unclear.
- Optimistic concurrency (revision checks), checks that block writes across patients, `RESTRICT` foreign keys, and exported schemas.
- Backup crypto (AES-GCM with the header authenticated) and the IVF day-3/5 math are correct.
- A solid suite of unit and instrumented tests.

## Suggested fix order

1. **Before any real patient is entered:** #1, #2, #3, #4, #5, #7. These are all small, contained fixes.
2. **Data durability:** #6, #8, #9, #11. Copy photos into encrypted storage and include attachments in backups.
3. **Device compatibility and correctness:** #10, #12, #13, and the clinical-command rollback.
4. **Architecture:** a per-patient query/paging layer, an audit trail, splitting the large screens, and one shared GA service.

I can start on the first group now; each fix would come with a regression test.