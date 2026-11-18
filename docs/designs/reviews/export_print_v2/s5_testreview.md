Defects:
- `app/src/test/java/.../ClinicalSummaryDatingTest.kt`: `datingBlock` now reads `datingSummaryOf`/`datingTextRows` instead of `patientShareText`, so deleting dating from the shared summary would not fail.
- `app/src/test/java/.../MedicationStatusTest.kt`: dropped assertions for dose/route/frequency and stopped-medication placement; only `Current medications: Methyldopa` plus `Aspirin` absence is checked, so a share text showing the wrong dose or misclassifying a stopped medication can pass.
[Tokens] upload: 11858 download: 1207 cache_read: 0 cache_write: 0

