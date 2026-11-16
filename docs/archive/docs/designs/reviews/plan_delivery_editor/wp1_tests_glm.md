# Independent test review — wp1 (parser/compose layer)

## Blockers

**B1. The pasted patch delivers only one of the three test suites the design promises (§9).** `DeliveryPlanReducerTest.kt` and `DeliveryPlanSheetTest.kt` are absent, so every state/UI behaviour is untested and would not fail if broken:
- Route preservation on open / on submit success / on Cancel (D1, Goal 1) — the actual bug this change fixes.
- Guards: archived patient, unknown patient, `blocksEditing()`, `hasDirtyDraft` (D10); field changes blocked while saving.
- Untouched Save writes nothing (D6); touched semantics per A6 (type-and-undo must not count as touched); failure keeps fields and sets `ERROR`/`saveError` (Goal 5).
- Draft cleared on route change and patient change (A8/amendment); process-death restore fallback (5.2) — an invisible dirty draft would silently block every other editor.
- Label constants equal the card titles in `strings.xml` (A8 explicitly requires this drift guard).
- 360 dp / font-scale 1.3 layout, keyboard visibility, discard dialog, disabled-while-saving, Undo footer, `delivery_plan_profile_invalid` message (A5).
- The shared `EditorSheetParts` refactor of `MedicationSheet`: "Meds tests still pass" is asserted nowhere in evidence.

**B2. The whitespace edge tests are mutually inconsistent, and fail against the pasted implementation.** On the JVM, `trim()` uses `Char.isWhitespace()`, which **does** treat U+0085 as whitespace and **does not** treat NBSP (U+00A0) as whitespace — the opposite of what the tests assume:
- `composeTrimsNonBreakingSpaceAtValueEdgesAndDropsNonBreakingSpaceOnlyLines` expects NBSP trimmed at edges and NBSP-only lines dropped. The pasted `composeDeliveryNotes` uses plain `trim()`, so it keeps NBSP → test fails.
- `nextLineCharacterAtValueEdgesIsNotTrimmedButStillRoundTrips` expects U+0085 to survive at value edges; its comment ("trim() does not treat U+0085 as whitespace") is factually wrong, and `parseDeliveryNotes` applies `group2.trim()`, which strips U+0085 anyway → test fails on both sides.
- Worse, no implementation can satisfy this pair *and* the property tests: `normalise()` uses plain `trim()`. If the implementation gains special NBSP handling, Theorem A fails on fields that are pure NBSP; if it uses plain `trim()`, the NBSP edge test fails. The suite is internally contradictory as written.

## Major

**M1. The moved existing parser test is not in the patch.** Design step 2 and §9 require the full existing suite to stay green with only justified updates. Without the moved file, I cannot verify any heuristic regression, and `theoremBHoldForEveryFixtureNoteOnFile` covers only 5 hand-picked fixtures, not "every fixture string in the existing parser tests" as §9 demands.

**M2. A1 re-rewrite scenario untested.** The amendment's key correction — a doctor adds one free (unlabelled) line in the patient form, so `isComposedDeliveryNotes` becomes false and the notice reappears on the next save — has no test. Only the trailing-newline variant is pinned; an appended heuristic line is the realistic trigger.

**M3. Theorem A's oracle is a mirror of the implementation.** `normalise()` re-implements compose's per-field logic line-for-line. A shared misconception (e.g., both treating a character class the same wrong way) passes Theorem A. Corollary C and Theorem B give partial independence, but no test pins compose output against an independently written expected string for pathological inputs beyond the fixed cases.

## Minor

- Blank-value strict fallback (`Delivery Plan:` → heuristic) is tested only for Plan; `Agreed Delivery Hospital:`, `Agreed Delivery Fees:`, `Postpartum Note:` with blank values are untested.
- Mixed-case labels beyond the one lowercase postpartum case (`Delivery plan:`, `AGREED...`) are untested for case sensitivity.
- `composedCheckIsTrueForNullBlankAndEveryComposeOutput` claims "every compose output" but samples one section set; the compose-output branch largely restates Corollary C — near-tautological for those inputs (null/blank are short-circuited by definition).
- A strict line with leading indentation (trimmed to strict by the parser's line-trim) is untested.
- No test that `isComposedDeliveryNotes` is false when any single labelled line is slightly perturbed (e.g., double space after the colon) — the exact-match notice trigger deserves one representative.

## Tests that would catch real breaks (positive findings)

The suite is strong where it exists: D4 counterexamples (a)–(e) are all pinned (`feesValueWithSemicolon...`, `planValueWithFeeWords...`, `quotedFeeValueStaysWhole...`, `clearedHospitalStaysNull...`, plus `aValueThatStartsWithAnotherFieldsLabelStaysInThePlanBox`, which also verifies the anchored-match claim of Lemma 2). The worked example, label-inside-value, 5,000-char values with `;`/sentence ends, hospital ordering across strict/heuristic lines, lowercase-label unchanged behaviour, and hand-typed exact-label before/after are all direct, discriminating assertions that fail on plausible regressions (e.g., dropping the `continue`, removing `DOT_MATCHES_ALL`, re-enabling derived hospitals on strict lines).

**Flaky timing:** none. Pure JVM tests, fixed seeds, no coroutines, no clocks — deterministic. The 2,000-iteration property tests are fast enough to keep.

## Verdict

The parser/compose unit layer is close to adequate — fix the two contradictory whitespace tests (decide the intended whitespace contract and align impl, tests, and the Lemma-1 prose), add the A1 re-rewrite case, and pull in the real fixture corpus. But the PR is not reviewable as "the tests for this design": the reducer and instrumented suites, which carry every behavioural guarantee the design exists to deliver (route stays put, no silent rewrite, guards, discard), are entirely missing. Blockers B1 and B2 must be resolved before merge.
