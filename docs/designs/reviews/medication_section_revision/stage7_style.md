# Goldfish Style Review — Medication Section Revision Design Document

## Summary

The document is technically detailed and well-structured, but contains several instances of wordiness, defensive hedging, and subjective framing that dilute the engineering signal. Below are the findings.

---

## Findings

### 1. Defensive Caveat — Uncalibrated "Level-3" Claim
**Severity:** Major  
**Affected Scope:** Lines 162–165 (Risks section), also echoed in Verification Plan line 365  
**Style Rationale:** The document asserts that certain files are "level-3 paths" and that the change "needs a frontier-tier review and an independent test review." This is presented as a fact about the codebase, but as a Goldfish reviewer I cannot verify the project map (`PROJECT_MAP.json`) or the risk classification. The claim outruns observable evidence in this document.  
**Action:** **CALIBRATE** — Restate as: "The author classifies `AncState.kt`, `ClinicalCommands.kt`, `RecordsRepository.kt`, and `EditorHooks.kt` as level-3 paths per the project risk map. Per project rules, this change requires a frontier-tier review and an independent test review before acceptance."

---

### 2. Defensive Caveat — Unverified Test Coverage Claim
**Severity:** Major  
**Affected Scope:** Line 364 (Verification Plan)  
**Style Rationale:** The plan mandates a specific multi-vendor review process ("use the MACA skill and distribute the roles: a Claude Sonnet subagent plus Argon, AlphaCode and GLM through opencode...") and a pre-flight one-line test for each model because "on 2026-10-31 Argon was out of quota and the opencode free models failed instantly." This is operational detail about tool availability, not a verification criterion for the design. It belongs in an execution runbook, not the design doc.  
**Action:** **RELOCATE** — Move to an implementation runbook or CI checklist. The design doc should state only: "Changes to level-3 paths require frontier-tier review and independent test review before merge."

---

### 3. Filler & Wordiness — "The author was offered X and chose Y"
**Severity:** Nit  
**Affected Scope:** Lines 58–63 (Decisions made during drafting)  
**Style Rationale:** Phrases like "The author was offered a schema change and chose to store..." and "Rejected save: the sheet stays open..." repeat the decision record format without adding signal. The bullet points already convey the decision; the narrative wrapper is redundant.  
**Action:** **DROP** the narrative wrappers. Keep only the decision bullets:
- Schema stays frozen; "changed" info stored as a text line in `notes`.
- Rejected save: sheet stays open with doctor's values; shows specific cause; no automatic retry.
- Course end evaluated at draw time; one shared `isOngoingMedication` check.
- Change = dose/frequency of same medication stops old row, creates new row; route is read-only on existing row.
- Undo bar for stop and for dose/frequency change.
- Enter-to-next: shared helper, used only in medication flow in Stage 1.

---

### 4. Hyperbole / Subjective Framing — "The cause of the refresh"
**Severity:** Nit  
**Affected Scope:** Line 206 (Alternative G)  
**Style Rationale:** "This is the cause of the refresh" presents a causal claim as absolute fact. The prior paragraph (line 19) describes the mechanism (route change triggers rebuild). The alternative description should mirror that precision: "Every tap changes the route, which rebuilds the Meds tab and loses scroll, search text, and open rows."  
**Action:** **CALIBRATE** to match the mechanistic description already given in The Problem section.

---

### 5. Synonym Redundancy — "Quick and rapid", "Clean and elegant" patterns
**Severity:** Nit  
**Affected Scope:** Scattered (e.g., line 21: "Moving between fields by hand, and fields hidden behind the keyboard, slow the doctor down")  
**Style Rationale:** "By hand" and "hidden behind the keyboard" describe the same interaction problem from two angles. Pick one. Similarly, line 31: "A catalog drug with a saved dose and frequency is added in two taps: the drug tile opens the sheet already filled in, and Save adds it" — "already filled in" and "prefilled" are redundant with the two-tap count.  
**Action:** **DROP** the redundant clause. Example: "Moving between fields and fields hidden behind the keyboard slows the doctor down." → "Moving between fields and keyboard occlusion slow the doctor down."

---

### 6. Jargon / Buzzword — "Frontier-tier review"
**Severity:** Nit  
**Affected Scope:** Lines 162, 365  
**Style Rationale:** "Frontier-tier" is not a standard industry term; it appears to be project-internal jargon. If it has a precise definition (e.g., "review by two senior engineers with clinical-safety context"), spell it out once or reference the governing document.  
**Action:** **CALIBRATE** — Either define inline on first use or replace with the concrete requirement: "review by two engineers authorized for clinical-safety changes."

---

### 7. Defensive Caveat — "No legacy shims" as a project rule cited repeatedly
**Severity:** Nit  
**Affected Scope:** Lines 15, 145, 183  
**Style Rationale:** The "Zero Legacy Baggage Invariant" is stated in the AGENTS.md constitution. Citing it three times in the design doc is redundant. One reference in the Technical Plan (What is deleted) is sufficient.  
**Action:** **DROP** the repeats at lines 15 and 183. Keep the one at line 145 where the deletion is actually specified.

---

### 8. Wordiness — "The sheet shows one message that names the cause, mapped from ClinicalCommandFailure"
**Severity:** Nit  
**Affected Scope:** Line 156 (Failed saves)  
**Style Rationale:** "Shows one message that names the cause, mapped from" can be compressed to "Shows a message naming the cause, mapped from..."  
**Action:** **CALIBRATE** — Tighten phrasing throughout section 9.

---

### 9. Uncalibrated Claim — "The app only appends a line... It does not protect the line."
**Severity:** Nit  
**Affected Scope:** Line 58  
**Style Rationale:** This describes intended behavior, not verified behavior. Since the code doesn't exist yet, frame it as design intent: "The design appends a line... The line is not protected; if the doctor edits or deletes it, the row reflects the current `notes` content."  
**Action:** **CALIBRATE** — Use future/design tense consistently.

---

### 10. Filler — "It is documentation, not shipped in the app"
**Severity:** Nit  
**Affected Scope:** Line 230 (Target Files: mock HTML)  
**Style Rationale:** Obvious from context (a mock file under `docs/mocks/`).  
**Action:** **DROP**.

---

### 11. Redundant "Proposed Changes" Header Repetition
**Severity:** Nit  
**Affected Scope:** Every file entry in Detailed Implementation Plan (lines 227–344)  
**Style Rationale:** Each file entry has a "Proposed Changes" sub-header that repeats the section purpose. The bullet points are the signal; the header is noise.  
**Action:** **DROP** the "Proposed Changes" sub-headers; keep bullets directly under the file path.

---

### 12. Inconsistent Tense — Mix of Present and Future
**Severity:** Nit  
**Affected Scope:** Throughout Technical Plan and Implementation Plan  
**Style Rationale:** Some sections use present tense ("The sheet is a ModalBottomSheet"), others future ("The sheet will show..."). Pick one. Present tense is standard for design specs describing the target state.  
**Action:** **CALIBRATE** — Standardize to present tense ("The sheet shows...", "The form emits...").

---

### 13. Unverified External Dependency — "MACA skill", "opencode", specific model names
**Severity:** Major  
**Affected Scope:** Line 364  
**Style Rationale:** The verification plan names specific tools (MACA skill, opencode, Argon, AlphaCode, GLM, Claude Sonnet) and a specific past incident (2026-10-31 quota failure). This couples the design to a particular review orchestration that may not be available or relevant at implementation time.  
**Action:** **RELOCATE** — Move tooling/orchestration details to a separate review runbook. The design doc should state only the review *requirement* (frontier-tier + independent test review), not the *mechanism*.

---

### 14. Positive/Subjective Framing — "Smallest change that removes the two worst symptoms"
**Severity:** Nit  
**Affected Scope:** Line 211 (Alternative H)  
**Style Rationale:** "Worst symptoms" is subjective. Replace with the concrete problems identified in The Problem section: "Smallest change that eliminates the full-screen rebuild on every tap and the full-screen add list."  
**Action:** **CALIBRATE**.

---

### 15. Redundant "See the Technical Plan" Cross-Reference
**Severity:** Nit  
**Affected Scope:** Line 183 (Alternative C)  
**Style Rationale:** The reader is already in the Technical Plan section. The cross-reference adds no value.  
**Action:** **DROP**.

---

## Overall Assessment

The document is **technically sound and actionable** — the architecture, state transitions, command design, and test plan are well specified. The style issues are almost entirely editorial: defensive hedging about unverifiable project-internal classifications, operational detail that belongs in runbooks, and wordiness that obscures the engineering signal.

**Recommended editorial pass:** Apply the CALIBRATE/DROP/RELOCATE actions above. The result will be a tighter, more professional engineering design document that any reviewer can evaluate without needing access to the project risk map or tooling orchestration decisions.
[Tokens] upload: 20429 download: 2486 cache_read: 0 cache_write: 0

