# Structural Review — `docs/designs/medication_section_revision.md` (Lines 1–214)

**Mode:** REVIEW
**Target Standard:** Feature Specification Standard
**Scope note:** Lines 1–214 only. The document continues past line 214 (`## Detailed Implementation Plan` begins at line 215); that content is outside this audit.

---

## 1. Structure Map (Lines 1–214)

```
H1  Medication Section Revision (Stage 1)                      (L1)
├── metadata bullets (Author / Status / Date)                  (L3–5)
├── HHR rule                                                   (L7)
├── H2  Overview                                               (L9)
├── H2  The Problem                                            (L17)
├── H2  Goals                                                  (L26)
├── H2  Non-Goals                                              (L43)
├── H2  Open Questions                                         (L52)
│   └── H3  Decisions made during drafting                     (L56)
├── H2  Critical User Journeys (CUJs)                          (L66)
│   ├── H3  CUJ 1                                              (L68)
│   ├── H3  CUJ 2                                              (L74)
│   └── H3  CUJ 3                                              (L80)
├── H2  Technical Plan (Architecture)                          (L86)
│   ├── H3  Summary                                            (L88)
│   └── H3  1–10 (State, Sheet, Dates, Stop/Change, Picker,
│           Enter, Deleted, Undo, Failed saves, Risks)         (L101–160)
└── H2  Alternatives Considered                                (L167)
    └── H3  Alternative A–H                                    (L169–209)
```

**Hierarchy check:** No skipped heading levels. H1 → H2 → H3 nesting is sequential and consistent throughout the scope. PASS.

**Blueprint coverage check (Standard requirements):**

| Required Area | Present? | Location |
|---|---|---|
| Problem Statement | Yes | `## The Problem` (L17) |
| Goals & Non-Goals | Yes | `## Goals` (L26), `## Non-Goals` (L43) |
| Proposed Architecture | Yes | `## Technical Plan (Architecture)` (L86) |
| Alternatives Considered | Yes | `## Alternatives Considered` (L167) |
| Risks & Mitigations | Partial | `### 10. Risks` (L160) — see Finding 1 |

---

## 2. Findings

### Finding 1
1. **Severity:** Major
2. **Affected Scope:** `### 10. Risks` (L160–165); absence of a standalone Risks & Mitigations section.
3. **Discrepancy Description & Rationale:** The Standard names **Risks & Mitigations** as a required area and requires that failure modes be identified "and how they will be mitigated." In this document, risks exist only as numbered subsection 10 inside the Technical Plan, and several entries state a hazard without a stated mitigation:
   * L163 (atomicity of the replace command + undo) — the requirement "must be atomic" is stated, but no mitigation or fallback if atomicity fails is given.
   * L164 (recovered drug must reopen the sheet without navigation) — hazard only, no mitigation described.
   * L165 ("Changed" mark loss from edited free-text notes) — the only "mitigation" is that the risk "was accepted by the author," which is an acceptance, not a mitigation.
   Additionally, no security implications of the new commands are discussed in the risks scope (e.g., the undo command touching two rows and carrying revisions). Recommend promoting risks to a top-level `## Risks & Mitigations` section and adding a mitigation (explicit or "accepted, rationale: …") per line item.

### Finding 2
1. **Severity:** Major
2. **Affected Scope:** `## Alternatives Considered` — L173, L179; attribution reference at L125.
3. **Discrepancy Description & Rationale:** The Standard's **Alternatives Rule** states rejection rationales "must state concrete technical or scope trade-offs and must not attribute a rejection to a maintainer without linking their issue, discussion, or PR."
   * For Alternative A (L173) and Alternative B (L179), the "Reason for Rejection" reads "Rejected by author: the author chose the bottom sheet (Option C)." This is a circular rationale — the alternative is rejected *because the other option was chosen* — with the concrete trade-off living only in the `Cons` bullets. The rationale should stand on its own trade-off (the cons already contain it: lost-tap risk vs. queue complexity for A; dual validation paths vs. level-3 churn for B) rather than on authorial preference.
   * L125 cites "as agreed when Option C was chosen" — an agreement attributed to a prior discussion with no link or pointer to that discussion. Technically an unlinked attribution under the Standard. (Mitigating context: the attributed party is the document's own author, not an external maintainer, which lowers the practical risk.)

### Finding 3
1. **Severity:** Nit
2. **Affected Scope:** L29, L30, L34 (×2), L58, plus additional occurrences at L84 and elsewhere in the CUJ/Technical plain text.
3. **Discrepancy Description & Rationale:** Prose consistently uses backslash-escaped quotes (`\"show past\"`, `\"changed\"`) instead of plain `"` or code spans. CommonMark renders `\"` as `"`, so output is not broken, but the escaping is non-idiomatic Markdown, inconsistent with the document's use of backticks elsewhere (e.g., `notes`, `medicationDraft`), and makes exact-string searches (e.g., for the `Changed from` line format) harder. Recommend plain quotes throughout.

### Finding 4
1. **Severity:** Nit
2. **Affected Scope:** `### Decisions made during drafting` (L56) nested under `## Open Questions` (L52).
3. **Discrepancy Description & Rationale:** The heading hierarchy is technically valid (H2 → H3), but the semantics are inverted: a list of seven settled decisions is filed as a subsection of "Open Questions" whose first bullet (L54) says "None open." The decisions are substantive design content, not sub-items of an empty questions list. Recommend promoting `Decisions` to its own H2 sibling section.

### Finding 5
1. **Severity:** Nit
2. **Affected Scope:** `### Alternative H` (L209–213).
3. **Discrepancy Description & Rationale:** Alternative H carries the field "Reason for Rejection: Not rejected." An entry in the *rejected alternatives* ledger that is explicitly not rejected breaks the ledger's symmetry and the Standard's framing ("Rejected options and rationale for rejection"). H is effectively a scope-stage description, not an alternative. Recommend either relocating its content into Non-Goals/Overview (where the Stage 2 boundary is already stated at L15 and L47) or renaming the H3 field to "Status: not an alternative — this is Stage 1 as scoped."

### Finding 6
1. **Severity:** Nit
2. **Affected Scope:** L125 vs. L181/183 ("Option C" vs. "Alternative C (chosen)").
3. **Discrepancy Description & Rationale:** Terminology inconsistency: the Technical Plan and Alternatives A/B refer to "Option C," while the Alternatives section titles the chosen design "Alternative C (chosen)." A fresh reader must infer these are the same item. Standardize on one label.

### Finding 7
1. **Severity:** Nit
2. **Affected Scope:** L50.
3. **Discrepancy Description & Rationale:** "A different route … is a separate drug and not a variant. This rule is recorded now and applied in Stage 2" is a design rule, not a non-goal in the same sense as the other bullets on L45–49. Minor categorization drift; consider labeling it explicitly as a forwarded Stage 2 rule to keep the Non-Goals list homogeneous.

---

## 3. Claims Requiring Code Verification (Reviewer Limitation)

Per the Goldfish protocol, the following in-scope claims reference the codebase and could not be checked (no file access): **not verified.**

* `MedicationTapToAddSection.kt` event flow (start-editor, five field-change events, submit) — L19. *not verified*
* `AncRoute.MedicationEditor` defined/used at `AncState.kt:1470`, `AncState.kt:1716`, `AncState.kt:357`, `AncState.kt` line 53 — L19, L103. *not verified*
* `AncApp.kt:238` hosts the editor full screen — L19, L103 context. *not verified*
* `medicationDraft?.medicationId == X` logic in `MedicationEditorHook` (`EditorHooks.kt`) — L104. *not verified*
* `RecordCommitted` sets `route = destination` — L106. *not verified*
* `keepSavedNotice` / "Save and add another" path exists — L107. *not verified*
* App already uses a `ModalBottomSheet` in `PatientListScreen.kt` — L111. *not verified*
* Existing undo built on `UndoCoordinator.offerSaveUndo` → `DeleteRecordCommand` — L150. *not verified*

---

## 4. Formatting Audit Summary

* **Lists:** Bullet style (`*   `) is uniform across the scope. PASS.
* **CUJ blocks:** Given/When/Then bold-label pattern is consistent across CUJs 1–3. PASS.
* **Code block (L92–99):** Opened with ```text, closed correctly, ASCII diagram intact. PASS.
* **Tables/links:** No tables; no external links present, and the Standard mandates none, so no link-compliance gap — though note the unlinked decision attribution flagged in Finding 2.
* **Headings:** No misspellings of standard-mandated section names; all present sections match their expected names (see coverage table).

---

## 5. Verdict

**No Blocker findings.** The document satisfies the Standard's structural layout: Problem, Goals/Non-Goals, Architecture, Alternatives, and CUJs are all present with a clean H1/H2/H3 hierarchy, and the Alternatives Rule is exercised with eight documented options (A–H).

**Conditionally passes, contingent on resolving the two Majors:**
1. Risk items must each carry an explicit mitigation (or documented acceptance with rationale), ideally under a dedicated Risks & Mitigations section (Finding 1).
2. Rejection rationales for A and B should be restated as standalone trade-offs, and the unlinked "as agreed when Option C was chosen" attribution at L125 should be linked or rephrased (Finding 2).
