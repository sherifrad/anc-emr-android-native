READ-ONLY REVIEW. Do not edit any file. Repo is the cwd (Android EMR, Kotlin/Compose, safety-critical). Review `git diff` (working tree vs HEAD).
Intent of the change: (a) repair stale static checks in .agents/checks/*.py after the UI moved from WorkspaceScreen/ProfileScreen to ui/patienthub/* and ui/patientdirectory/*, without weakening them; (b) replace raw LocalDate.parse in ui/LabEditor.kt and ui/LabBatchScreen.kt with parseIsoDateOrNull(...) ?: LocalDate.now(); (c) make the severe-BP badge render on the patient directory card (ui/patientdirectory/PatientDirectoryCard.kt, ui/ClinicalBadges.kt) instead of passing "≥160/110" to BpAlertLabel.
Judge:
1. For each changed check: does it still FAIL if the invariant it names is broken (not vacuous: it must read at least one real file and assert a real symbol)? Name any check that now passes vacuously (empty file list, regex that matches anything, assertion removed without a replacement).
2. LabEditor/LabBatchScreen: behaviour identical for valid, invalid, blank and padded date strings? Note parseIsoDateOrNull trims and treats blank as null; any clinical-date consequence (EDD, gestational age as-of date)?
3. BP badge: shown exactly when row.isSevereBp; BpAlertLabel callers unchanged in behaviour; fits on a 360dp row; testTag kept.
4. Anything else in the diff beyond this intent.
Output: PASS or FAIL, then findings `path:line | BLOCKER/SHOULD/NIT | problem`, max 20 lines. Only claim what you verified by reading.
