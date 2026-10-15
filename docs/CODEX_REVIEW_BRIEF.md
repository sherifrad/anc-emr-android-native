# BRIEF: Clinical Architecture & Review for ANC EMR Modernization

## Role & Goal
You are Codex, acting as the Senior Architectural Reviewer & Clinical Plan Strategist.
The lead implementer is Antigravity (Google DeepMind agent).
Your task is to:
1. Inspect and read the core files of this native Android ANC EMR project (`/Users/sherifgamalradwan/anc-emr-android-native`).
2. Review our proposed 6-slice technical & clinical plan.
3. Provide an in-depth architectural report: identify potential pitfalls, schema migration safety considerations (Room SQLCipher v3 -> v4), UX ergonomics improvements, and specific recommendations for each slice.

## Project Context
- Target device: HONOR 400 (`DNY-NX9`), Android 16 / API 36, arm64-v8a.
- Pure Kotlin + Jetpack Compose + Material 3 (Material You optimized).
- Encrypted storage: SQLCipher with Room (`RecordsDatabase`), currently version 3.
- Core UX principle: **Simplicity first, clinical speed, zero clutter, compact layouts (do not waste full-width lines on small inputs)**.

## Core Files to Inspect
- `docs/AI_HANDOFF.md`
- `app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt`
- `app/src/main/java/com/sherifradwan/ancnative/model/DemoModels.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsMigration.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/DatingEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt`

## Proposed 6-Slice Plan
- **Slice 1: Dating Engine & Unified Gestational Age (GA)**:
  - Progressive disclosure: Doctor chooses method first: LMP vs Ultrasound vs IVF Embryo Transfer (Day 3 vs Day 5).
  - IVF rules: Day 3 transfer adds 263 days to transfer date for EDD (initial GA at transfer = 17 days); Day 5 blastocyst adds 261 days (initial GA at transfer = 19 days).
  - Unified GA calculator: Call anywhere (Visits, Ultrasound, Labs, Workspace header) dynamically as of record date.
- **Slice 2: Two-Way Linking of Visits & Ultrasounds + Compact UI + Ultrasound Image Attachments**:
  - Visit <-> Ultrasound bi-directional linking.
  - Linked ultrasound shown inside visit card on timeline.
  - Compact ultrasound UI: GA weeks & days in a single Row, collapsible biometry (BPD, HC, AC, FL, EFW) & FHR, attachment section for ultrasound image photos.
- **Slice 3: Multi-Test Lab Engine**:
  - Add `lab_name` to lab investigations.
  - Multi-test entry per order/panel, floating (+) action button, quick presets with auto-filled units/ranges, instant auto-save.
- **Slice 4: Multi-Drug Rx & Drug Directory**:
  - Remove `end_date` and `route` fields. Keep `name`, `dosage`, `frequency`/instructions.
  - Add `is_non_compliant` and `adverse_notes`.
  - Multi-drug entry in one prescription session.
  - Directory in settings with autocomplete & auto-learning of newly typed drugs.
- **Slice 5: Clinical Attention Banner & Explicit Negative History**:
  - Banner for `CRITICAL` (red), `ALERT` (orange), `NOTE` (blue) items.
  - Explicit confirmation for negative history ("لا توجد سوابق مرضية / جراحية / حساسية") with 1-tap presets so fields are never left ambiguously blank.
- **Slice 6: Full Synthetic Longitudinal Case & E2E Verification**:
  - Comprehensive realistic case with 5 longitudinal visits across pregnancy, linked scans, multi-labs, multi-drugs, clinical attention.
  - Room v3 -> v4 migration test suite passing 100%.

## Output Request
Please provide a comprehensive, structured architectural evaluation covering:
1. Room Schema 3 -> 4 Migration safety (ensuring no regression with SQLCipher).
2. Data model & state management recommendations for multi-test labs & multi-drug prescriptions.
3. UI/UX ergonomics (Material You light/dark, compact inputs, keyboard IME actions).
4. Any edge cases or clinical calculation pitfalls in Gestational Age and IVF dating.
