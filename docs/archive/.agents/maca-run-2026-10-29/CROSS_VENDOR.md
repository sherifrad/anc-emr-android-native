# Cross-vendor merge (2026-10-29)
Lanes that produced a review: Claude subagent (Opus), agy, opencode GLM-5.3-flash-free, opencode DeepSeek-v4.1-flash-free.
Failed: codex (model gpt-5.6-sol not supported with the ChatGPT account), gemini (no auth configured).
Caveat: the relay kept only the LAST 4000 characters of each vendor review, so findings F1-F5 of the three vendors were cut off. Relay fixed (full stdout.txt now saved); rerun needed for those lanes.

## Agreed by 2+ model families (strong)
- Dark "AGE" label contrast 4.47:1 (<4.5): all 4 reviews; I computed it myself.
- Placeholder text at alpha 0.65 fails AA (~3.0-3.5:1): all 4.
- Age suffix handling case-sensitive ("29 Years" -> "29 Years yrs"): all 4.
- Age badge read twice by TalkBack (semantics not merged): Claude, agy, DeepSeek.
- Rh-negative detection misses Unicode minus / variants; risk emphasis uses English substring: all 4.
- Author's test too weak (weak asserts, no blank/history/mixed-line cases, nothing for Rh/age): all 4.
- Delivery edit icon has a hard-coded content description: all 4.
- Emoji inside clinical values read aloud / leak into text: Claude, GLM, DeepSeek.

## Single-family findings
- DeepSeek: leftover text edit buttons elsewhere (partial application of the pencil change) - NOT yet verified by me; I only saw OutlinedButton/TextButton still present at several lines of WorkspaceScreen.
- DeepSeek: "Risk" drops the "Manual" qualifier (Claude also noted this as part of S1).
- DeepSeek: testTag("patientAgeBadge") repeated on every list row -> ambiguous UI-test lookups.
- GLM: blank-but-non-null age adds a 6dp spacer (Claude also raised it).

## Disputed / corrected by checking the repo
- Localisation (O8): DeepSeek was right that the app has no values-ar and MainActivity forces Locale.ENGLISH. So hard-coded English is NOT an Arabic-breaking defect today. Severity drops from Should-fix to Nit/regression-of-consistency. The Claude reviewer's Arabic/RTL points (S1, S2, S10) are downgraded accordingly. Dropping existing string resources is still a code-consistency regression.
- GLM claimed "Group B pending typing" is wrongly flagged as Rh-negative because it contains "neg". That is FALSE ("pending" does not contain "neg"). Discard that example (the real Rh false positive is "A+, antibody screen negative").
- Unverified-by-Claude item resolved: PatientListScreen still calls PatientAgeBadge(ageText = ...) at lines 499 and 628, so the list badge works with the new public function.

## Parser blockers (HB1-HB5)
Only the Claude reviewer's version is on record (confirmed by my reproduction). Vendor confirmation pending the rerun.
