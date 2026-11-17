# Slice 21 cleanup: shared brief for all four team members (READ-ONLY analysis phase)

Repo /Users/sherifgamalradwan/anc-emr-android-native, branch redesign-2026-11-15 (Android Kotlin Compose app, MVI). The whole-app redesign (slices 0 to 20) is finished and left old code behind. Goal of this task: find code, resources and tokens that are now dead, duplicated or slop, and prove each is safe to remove. Nothing is deleted in this phase.

## Hard safety rules (this phase)
- You may create or edit ONLY files under docs/cleanup/s21/. Never edit app code, tests, resources, scripts, git state. No git write commands, no gradle, no adb, no emulator, no phone.
- Protected, never propose removal or edits: Room schemas and app/schemas, RecordsDatabase, RecordEntities, RecordsDao, RecordsRepository, UndoCoordinator, ClinicalModels, any *Generator* file, state/ reducers (AncReducer, AncState, AncEvent), ui/DatingFieldVisibility.kt, security/keystore code, and anything under storage/ and model/. If dead code is there, list it in a separate "protected, report only" section.
- Level 3 files (auth, security, clinical, schema) may only be reported, not proposed for removal, unless the removal is a pure unused import: AncApp.kt, AncDatePicker.kt, InAppCameraDialog.kt, DatingEditor.kt, PreviousPregnancyEditor.kt, VisitEditor.kt, ClinicalPresets.kt, ui/report/*, SecuritySettingsDialog.kt, AppLockScreen.kt, VaultGateScreen.kt, LabEditor.kt.

## Skills to apply (read these files first and follow them)
- ~/.claude/skills/multi-agent-collaborative-audit/SKILL.md (separate builder and checker, evidence over claims, report what was not verified)
- ~/.claude/skills/antislop/SKILL.md, antislop-code/SKILL.md (comment hygiene: remove generic or restating comments, keep valuable ones, never touch code in a comment-only pass), antislop-ui/SKILL.md, antislop-layoutmobile/SKILL.md, antislop-human/SKILL.md, antislop-copywriting/SKILL.md (same folder ~/.claude/skills/<name>/SKILL.md)
- Design skills (Anthropic): design critique, design system, accessibility review, frontend-design: apply their questions to the redesigned UI system: one canonical component per entity, tokens used consistently, no parallel old and new styles, 48dp targets, 12sp minimum, no hardcoded user strings, no duplicate semantics.
- Project rules: AGENTS.md (reusability: one canonical implementation; zero legacy baggage; files under 1,500 lines) and docs/designs/redesign/PLAN.md.

## What to look for (categories)
1. Unused imports, unused private members, unreferenced declarations (start from docs/cleanup/s21/tool_unused.txt, produced by .agents/scripts/find_unused.py; it is a CANDIDATE list only: reflection, test tags, previews and framework callbacks can hide uses).
2. Old design-system leftovers the redesign replaced: FactRow (if no caller), ClinicalTagBadge, ClinicalRiskBadge, old Tag* colour tokens, DesignSystem.SectionCard once editors use RecordSectionCard, old ChoiceGroup/chip styles, local one-off card/chip/label implementations that duplicate ui/components (RecordSectionCard, FormFieldLabel, RecordFilterChip, StatusTag/StatusWord/BandChip, PlainField, SheetCancelSaveRow).
3. Duplicates (start from docs/cleanup/s21/tool_duplicates.txt from find_duplicates.py): near-identical functions that should call one canonical function.
4. Unused string resources in app/src/main/res/values (and unused colours, dimens, drawables): a resource is unused only if no R.string.NAME / R.color.NAME / @string/NAME reference exists in main, test, androidTest or any xml (check plurals and string arrays, format args, and name prefixes built dynamically).
5. Code-comment slop: generic restating comments, dead commented-out code, stale TODOs.
6. Dead tests or test helpers that reference removed things.

## Evidence required for EVERY finding (no claim without it)
Each finding row: id, file:line, what, category, the exact grep or command you ran and its result (zero references across app/src/main, app/src/test, app/src/androidTest, app/src/debug if present, res xml and AndroidManifest), risk (low, medium, high) and why, protected or level 3 flag, proposed action (delete / replace with canonical X / keep). Mark each CONFIRMED (grep proves zero references and you checked reflection, dynamic names, @Preview, tests) or UNCERTAIN. Do not claim something is unused from the candidate tool output alone. Prefer fewer certain findings to many doubtful ones.

## Output format
Markdown table plus a short "Not verified" section. Keep each report under 600 lines.
