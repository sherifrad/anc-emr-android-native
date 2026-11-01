# Project profile (intake, 2026-10-29)
- Purpose: Android EMR for antenatal care in a private obstetrics clinic (Kotlin, Compose, Room/SQLCipher). Users: clinician (owner) and staff.
- Failure severity: HIGH - wrong data could affect care. Owner (Dr. Sherif) approves clinical values.
- Packs: core, clinical (directions only)
- Scope of this run: uncommitted changes only (4 modified UI files, 2 new files). Mode: testing, read-only on source.
- Test runs: Gradle unit tests on the user's Mac. Not runnable from the sandbox (no Android SDK there).
- Weight: Full. Model tiers: strong model for review, mid model for test design.
- Downgrade paths: evidence/**=0, retry/**=0, docs/**=0 (human-set 2026-11-01: run output and prose; a secret-shaped value still escalates; UI paths deliberately not downgraded)
