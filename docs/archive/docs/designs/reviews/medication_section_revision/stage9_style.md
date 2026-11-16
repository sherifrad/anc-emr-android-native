# Style Goldfish — REVIEW Report

**Document:** `docs/designs/medication_section_revision.md`
**Audited scope:** Lines 215–372 (Detailed Implementation Plan, Verification Plan, provenance footer)

## Overall Assessment

The audited range is dry, direct, and largely free of the vectors this audit targets: no hyperbole, no marketing fluff, and almost no buzzword usage. The section headings follow a consistent template, and most requirements are stated as concrete, checkable conditions ("stay under the 1,500-line ceiling", "confirm the Room schema file under `app/schemas` is unchanged"). The findings below are mostly precision and calibration issues rather than tone problems.

## Findings

### 1. Coverage requirement omits two of the new branching units it claims to cover

* **Severity**: Major
* **Affected Scope**: Line 362 ("Test Coverage Requirement"), measured against items 3 (line 244) and 20 in the Verification Plan.
* **Style Rationale**: The requirement states "Every new branch in `ReplaceMedicationCommand`, `replaceMedication`, `isOngoingMedication` and the change-note helpers is covered by a unit test." The list reads as exhaustive, but the plan itself introduces two additional branching units: `UndoMedicationChangeCommand` / `undoMedicationChange` (line 244, item 4) and `medicationCourseEndDate` (line 240), for which item 22 defines a test file in the former's case and no test at all in the latter's. A claim typed as "every … is covered" must list every unit it asserts coverage for, or the verification gate can be passed while the omission stands unchallenged.
* **Editorial action — CALIBRATE**: Either add `UndoMedicationChangeCommand`/`undoMedicationChange` and `medicationCourseEndDate` to the coverage list, or add a test item for `medicationCourseEndDate` and restate the list as explicitly exhaustive.

### 2. Claims about existing code are stated as fact but rest on evidence not shown

* **Severity**: Major
* **Affected Scope**: Lines 236, 249, 261, 293, 363.
* **Style Rationale**: Several sentences assert current repository behavior as established fact: "The old two-argument form is removed so every caller must pass the end date and today" (236); "In the command dispatch near line 353 add…" (249); "so two fast taps lose one of them" via "the existing `keepSavedNotice` path" framing (261); "follows `saveMedication`: check the patient exists…" implying the comparator's exact steps (249); "Full-screen screen, 396 lines" (293); "the project's whole-suite emulator script" (363). A goldfish with no file access cannot confirm any of these; each one is an assumption about current code, not verified from the document. Where verification is genuinely pending, the surrounding items already mark the file "Verify"; where they are load-bearing details of the plan, they are stated without qualification.
* **Editorial action — CALIBRATE**: Restate each as an verify-first step ("confirm the old two-argument form exists and find every caller before removing it") or mark with "not verified", and cite symbol names only for facts already evidenced.
* **Classification per claim**: each is `CALIBRATE` (narrow the claim to what the plan has actually observed), not `DROP` — the reader decision (what to check first) is concrete in every case.

### 3. Existing-behavior guarantee for a proposed change is stated in the past/present indicative

* **Severity**: Nit
* **Affected Scope**: Line 304 ("Existing callers keep working without changes").
* **Style Rationale**: This describes a *future* property of code that does not exist yet, in the indicative voice, as if already true. The correct register for a design requirement is either obligation ("must keep working without changes, verified by …") or a named test. As written, an uncritical reader inherits a verification-free assurance.
* **Editorial action — CALIBRATE**: Convert to a requirement plus the test that proves it (e.g., compile plus existing `ComponentsTest` coverage, if any — not verified).

### 4. Consecutive dense subroutine summaries — one bullet, one responsibility

* **Severity**: Nit
* **Affected Scope**: Lines 244 (`ClinicalCommands.kt`), 249 (`RecordsRepository.kt`), 269 (`AncViewModel.kt`).
* **Style Rationale**: These single bullets each pack two commands' worth of fields, validation rules, and transaction semantics into one 70–110-word sentence. Line 244 defines `ReplaceMedicationCommand` *and* `UndoMedicationChangeCommand` in one breath; an implementer cannot grep or assign work at that granularity. This violates the document's own one-item-per-bullet convention used everywhere else.
* **Editorial action**: Split into one sub-bullet per command, fields as a compact list, keeping the symbol names intact.

### 5. Fragile line-number references

* **Severity**: Nit
* **Affected Scope**: Lines 249 ("near line 353"), 260 ("line 53 … lines 357, 1483 and 1716"), 288–289 ("line 238").
* **Style Rationale**: Symbol names (`AncRoute.MedicationEditor`, its mapping in the back handler, `is AncRoute.MedicationEditor` in `AncApp.kt`) already identify every target uniquely; the parenthetical line numbers add no information and will silently rot at the first editorial edit, misleading a future implementer into the wrong region. Dry documentation should reference stable identifiers, not mutable offsets. (Where no stable symbol exists, e.g. the dispatch site, say so and mark "not verified" rather than pinning a line.)
* **Editorial action**: Drop the parenthetical line numbers; keep the symbol names.

### 6. Vague quantity: "a little JavaScript"

* **Severity**: Nit
* **Affected Scope**: Line 229.
* **Style Rationale**: "A little" is a subjective quantity judgment; it gives no acceptance criterion for a named deliverable.
* **Editorial action**: State the concrete bound (e.g., "reads-only mock: one 30-line script for sheet open/close and toggle state" or a plain count of behaviors wired).

### 7. Unexplained acronym, duplicated in two places

* **Severity**: Nit
* **Affected Scope**: Lines 370 and 372.
* **Style Rationale**: "EGM" appears twice with no expansion and no definition anywhere in the document ("Indicates initial structural drafting via EGM", "*Drafted with EGM*"). A fresh reader cannot act on, verify, or even care about this provenance mark without the expansion, and it is stated twice for a fact that needs stating once.
* **Editorial action — DROP**: If the provenance has value to some other workflow, name the tool once with its expansion; otherwise the comment and footer are noise for every reader of this design and can be removed.

### 8. Slightly metaphorical phrasing in an otherwise fat-free bullet

* **Severity**: Nit
* **Affected Scope**: Line 329 ("so the catalog frequency reaches the draft prefill").
* **Style Rationale**: "Reaches" is figurative where the rest of the document uses direct verb forms. Substituting a plain mechanism verb costs nothing and matches the surrounding register.
* **Editorial action**: "so the catalog frequency is carried into the draft prefill" or "so the prefill includes the catalog frequency".

## Defensive Caveats Audit (`DROP | CALIBRATE | RELOCATE | KEEP`)

No `RELOCATE` candidates found: the range contains no blanket operational disclaimer separated from the step that owns the decision. Specific classifications:

* **Line 361, "(use the project's existing lint and detekt tasks if present)"** — `KEEP`: a scoped, verifiable conditional with a concrete fallback, not a blanket hedge.
* **Lines 363–364, review-team instructions including the dated quota anecdote** — `KEEP`: the anecdote names the specific event and date and directly motivates a concrete operative decision (the one-line model re-check and the documented fallback to the Sonnet subagent). It earns its space.
* **Line 365, frontier-tier review requirement** — `KEEP` within this range, but note it repeats §10 Risks nearly verbatim (line 162). One pointer, not two restatements, would be tighter; flagging only as a Nit, not a caveat defect.
* **Line 219, "No code starts before that"** — `KEEP`: a hard gate with an owner (the author approves the mock), not a disclaimer.

## Compliance Notes

* **Objectivity**: No subjective quality judgments, no praise of the author's own proposal, no pejorative framing of the rejected alternatives. Good.
* **Requirement phrasing**: Most goals are stated as falsifiable conditions; the two exceptions are Finding 3 ("keep working") and Finding 6 ("a little").
* **Testability**: Item 22's test list ties tests to named behaviors and specific failure modes (stale revision, replayed operation id, rollback), which is the correct register for this document.

## Summary for the Orchestrator

| # | Severity | One-line fix |
|---|----------|--------------|
| 1 | Major | Add the undo command and the course-end-date function to the coverage requirement |
| 2 | Major | Convert current-code assertions into verify-first steps ("not verified") |
| 3 | Nit | Restate "existing callers keep working" as a tested requirement |
| 4 | Nit | Split the three dense command-definition bullets into one sub-bullet per command |
| 5 | Nit | Remove parenthetical source line numbers; reference symbols |
| 6 | Nit | Quantify "a little JavaScript" |
| 7 | Nit | Expand or delete the "EGM" provenance marks |
| 8 | Nit | Replace "reaches" with a plain mechanism verb |

No Blockers. The audited range is structurally sound; the two Major findings are calibration gaps in the verification gate, which is the single place where an imprecise sentence in this document would permit an otherwise preventable defect to pass.
