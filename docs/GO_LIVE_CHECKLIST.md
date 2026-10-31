# Go-live checklist (clinic upgrade)

Written 2026-10-31 by the orchestrator. Draft for the owner to review; nothing here has been run on the clinic app.

## Before the upgrade
1. Use the signed release build only (key backup: volume "ePartogram Signing Backup", identical to `keystore/anc-v3-release.jks`; never commit the keystore or its passwords).
2. Confirm the version name and version code are higher than the installed clinic app (open question: owner picks the numbers).
3. In the OLD clinic app: Settings, Export Backup (encrypted backup file, with a passphrase you will remember). Save the file somewhere that is not the phone. Photos are not part of the backup.
4. Write down the number of patients and, for two or three patients, the number of visits and the latest visit date.
5. Do NOT uninstall the old app: install the new build OVER it (same package and same signing key keep the data).

## Upgrade
6. Install the new signed build over the old one. Open it: PIN screen first; enter the PIN.
7. The database moves to the new version on first unlock (schema 6, drafts table). If the app shows a storage problem instead of the patient list, stop: do not retry repeatedly, keep the phone as it is and use the backup file.

## Verify every record after
8. Patient count equals the number written down.
9. For the two or three patients written down: visit count and latest visit date match; open the profile, dating, ultrasound, lab and medication sections.
10. Open one printed report and compare it with a printout from before the upgrade (no broken words, stamp once, lists that were cut say how many are not shown).
11. Check that a new visit can be saved, and that locking and unlocking works (PIN, fingerprint).
12. Export a fresh backup from the new app and keep it.

## If something is wrong
13. Do not delete anything. Reinstall the previous build only with the backup file at hand; restore the backup (Settings, Restore Backup, confirmation dialog).
14. Report the symptom with the time; the logs of the QA copy are not on the clinic phone.

## Known limits at this date
- Photos are not in the backup. Backup restore had never worked before 2026-10-31 and has only been tested on an emulator and by code reading: do one restore test on the QA copy before relying on it (phone test item 17).
- R8 code shrinking is off in the release build until it is checked on a device.
