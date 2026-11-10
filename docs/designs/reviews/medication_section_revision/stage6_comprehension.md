# Comprehension Goldfish Review — `docs/designs/medication_section_revision.md` (lines 1–372)

## Paraphrase

**What this proposes**
The design moves every medication action (add, stop, edit, dose/frequency change) onto the Meds tab itself, via a bottom sheet hosting one shared medication form, so the app never navigates to a separate editor and the tab's scroll position, search text and expanded state survive. The tab becomes review-first: a compact active list with stop symbols and "Changed" marks, stopped drugs behind a "Show past" toggle, and a collapsed, searchable add picker. Dose/frequency changes write two records in one transaction (old row stopped, new row started) with a "Changed from …" text line appended to the existing `notes` field; stops and changes get undo bars; failed saves re-open in the sheet with a named cause; the Room schema stays frozen at v6; the Enter key advances focus through the form via a new shared helper. A clickable 360dp HTML mock is a gate before any code.

**How the existing system works, per this document**
Today, tapping a drug tile fires a start-editor event plus up to five field-change events and a submit event; the start event changes the route to a full-screen `MedicationEditor`, and after saving the workspace is rebuilt, losing scroll, search and open cards. The quick-add list renders every catalog drug as a full-width card. Saving goes through a command mechanism carrying expected patient/medication revisions, an operation id, a fingerprint and a commit receipt; draft autosave and recovery already exist; undo currently works only by deleting/restoring whole records. Schema v6 is frozen, and certain files are designated "level-3" paths requiring a special review tier.

---

## Findings

### 1. Major — The word "route" means two different things
- **Affected Scope**: Summary diagram (line 95: "route never changes"), Non-Goals (line 50), sections 4 ("Route can be chosen… read-only on an existing row"), section 1, file 14.
- **Discrepancy**: The document uses "route" for both the *navigation route* (`AncRoute.MedicationEditor`) and the *administration route* of a drug (injection vs oral). These appear within a few paragraphs of each other (e.g., "route never changes" in the architecture diagram vs "a different route is a separate drug"). A fresh reader can conflate them, especially in section 4, where the surrounding sentences mix both meanings. Recommend a distinct term for the drug's administration route (e.g., "dosing route") throughout.

### 2. Major — "Stopped" vs `Discontinued`: no defined status vocabulary
- **Affected Scope**: Goals (lines 29–30, "Stopped"), section 3 ("counts as stopped"), file 4 ("update the old row to `Discontinued`"), file 3 (`UndoMedicationChangeCommand` restores "the earlier status").
- **Discrepancy**: The UI/goal prose says drugs are "Stopped", but the repository change writes the status `Discontinued`. The document never enumerates the medication status values or states that "Stopped" and `Discontinued` are the same thing. A fresh reader cannot tell whether these are one status or two, and `UndoMedicationChangeCommand`'s "earlier status to restore" depends on that answer. Also unclear: is the value derived by `isOngoingMedication` ("stopped when end date is before today") a third, purely-presentational notion of "stopped" that may disagree with the stored status? Section 3 implies so ("the stored status is not rewritten"), but the vocabulary should be made explicit.

### 3. Major — Save-machinery concepts are never explained in-document
- **Affected Scope**: Goals (line 37 "the revision check"), section 4 (line 127), file 3, file 4, section 8.
- **Discrepancy**: The replace command "follows the same rules as `SaveMedicationCommand`: expected patient revision, expected medication revision, one operation id, a fingerprint, and a commit receipt" — but none of these concepts (what a revision guards against, what an operation id prevents, what a fingerprint covers, what a receipt is for) is defined anywhere in the document, including for `SaveMedicationCommand` itself. The reader is referred to code, which is outside the document. Additionally, section 8 says the undo command carries a "content hash" while file 3 calls it a "fingerprint" — the document never says whether these are the same thing. For the change the author calls "the largest change in the level-3 area," this vocabulary is load-bearing.

### 4. Major — The date chip's meaning changes per action, but that is never stated
- **Affected Scope**: Goals (line 32), section 3 (line 119), section 4 (lines 125–126), file 14 ("date chip with Today / 3 days ago / pick-a-date").
- **Discrepancy**: On add, the chip edits the *start date*; on stop, the sheet sets "the end date … today" and "the doctor can change the date chip" — so the chip must now edit the *end date*. The form (file 14) defines exactly one date chip and never describes this mode switch. Worse, in a dose change: "the old row becomes Stopped with the chosen date, and a new row starts on that date" — the source of "the chosen date" (is it the chip, defaulting to today?) is never stated, nor is it stated whether backdating it is intended and what it implies (a drug shown as active until the chosen stop date). This is clinical date semantics; it should be spelled out.

### 5. Major — Editing course length or start date on an existing row: undefined outcome
- **Affected Scope**: Section 4 (line 128: "Edits that are not dose or frequency (name, notes, status toggle) update the same row"), file 7, file 14.
- **Discrepancy**: The replace rule is triggered only by a dose or frequency change. But the form always shows the course-length control, and course length writes `endDate` — the very field a stop/replace writes. If the doctor opens an existing active row and picks a course length (or changes the start date), does that update the same row in place, or is it treated as a change with history? The document does not say, and the enumerated "same row" edits (name, notes, status toggle) do not include these fields. Similarly, the replace path is restricted to "an existing active row" (file 7); what happens when editing or re-activating a stopped row is undefined.

### 6. Major — Sheet/tab interaction and recovery placement are unstated
- **Affected Scope**: Section 2 (sheet "hosted by `MedsTab`"), file 16 ("Host `MedicationSheet`"), section 10 (draft recovery risk), file 9.
- **Discrepancy**: The sheet lives inside `MedsTab`, so its lifecycle is tied to that tab being composed. The document does not say what happens if the doctor swipes to another tab while the sheet is open (does the sheet vanish while the draft stays open? is draft discard triggered?), nor where a recovered draft reopens if the workspace's last-visited tab was not Meds. The stated back-handling contract ("Back closes the sheet") assumes the sheet is visible; the tab-switch case is silent. These behaviors are not verified from code — flagged here as unstated design, not confirmed gaps.

### 7. Major (not verified) — The "is open for record X" hook vs create drafts
- **Affected Scope**: Section 1 (line 104: `medicationDraft?.medicationId == X`), file 4/section 8 (delete/undo closing the draft), test plan line 352 ("a delete or undo of the open record closes the draft").
- **Discrepancy**: The hook answers "is the medication editor open for record X" by comparing `medicationId`. For a *new* medication (added from the picker), the draft conceptually has no stored record id — so how the shared delete/undo-close logic applies to a create-draft is unexplained. The document asserts "the shared delete and undo logic for all record types keeps working through that hook" without verifying the create case. Not verified; needs an explicit statement of what `medicationId` holds for creates.

### 8. Nit — "Changed from" line format is inconsistent across examples
- **Affected Scope**: Goal line 34 ("Changed from once daily on 2026-11-10"), CUJ 3 (line 84, "Changed from 1 tab · once daily on 2026-11-10 14:05"), file 2 (`Changed from <dose · frequency> on <yyyy-MM-dd HH:mm>`).
- **Discrepancy**: The goal's example omits the dose and the time; the canonical format in file 2 includes both. Since the format lives in one helper function, this is only a documentation inconsistency, but the goal text should match the spec'd format.

### 9. Nit — Project-process terms used without definition or link
- **Affected Scope**: Section 4 (line 127 "level-3 area"), Risks (line 162), Verification Plan (lines 364–365: "MACA skill", "frontier-tier review", "independent test review", "Argon, AlphaCode and GLM").
- **Discrepancy**: A fresh reader has no in-document definition of what "level-3" classifies, what a frontier-tier review or independent test review consists of, or what the MACA skill is. These recur four times; a one-line definition or a link would suffice.

### 10. Nit — "Common and Recent" are undefined
- **Affected Scope**: Section 5 (line 133: "Common and Recent stay as they are in Stage 1"), file 17.
- **Discrepancy**: The reader is told these stay untouched but is never told what they are (presumably quick-pick tile groups in the current picker). One clause would fix it.

### 11. Nit — End-date-equals-today edge is tested but its expected outcome is only implied
- **Affected Scope**: Section 3 (line 121), test plan (file 22, line 348: "including the end date equal to today").
- **Discrepancy**: "A drug whose end date is before today counts as stopped" implies end date == today remains active, and the test list explicitly covers that boundary — but the expected result is left to inference. In a clinical activity rule, the boundary case should state its expected outcome outright.

### 12. Nit — `isNonCompliant` is referenced but never explained
- **Affected Scope**: Section 3 (line 121), file 2 (line 236).
- **Discrepancy**: The shared active-check signature includes `isNonCompliant`, but the document never says what medication non-compliance means here or how it interacts with the end-date rule. The reader must trust that the existing behavior carries over intact.

### 13. Nit — Assumed-name references that a fresh reader cannot resolve
- **Affected Scope**: Verification Plan (line 363, "the project's whole-suite emulator script"), section 4 (line 125, "as agreed when Option C was chosen"), footer ("Drafted with EGM"), Alternative H ("Reason for Rejection: Not rejected…").
- **Discrepancy**: The emulator script is not named; "Option C" actually refers to "Alternative C" in the Alternatives section (naming mismatch); EGM is undefined; Alternative H's rejection reason乍 reads as a contradiction ("Not rejected") before resolving into "delivered in stages." All minor, but each forces a re-read.

---

## Claims about the code I could not verify (no code access)

All file/line and code-behavior assertions below are **not verified**:

- `AncState.kt:1470` (start-editor sets route), `AncState.kt:53`, `:357`, `:1483`, `:1716`, `AncApp.kt:238`, `RecordsRepository.kt` "near line 353", `EditorField` at `Components.kt:403`.
- `MedicationEditor.kt` is 396 lines; `EditorHooks.kt` contains `MedicationEditorHook`, `isOpenFor`, `rebaseCreateDraft`.
- `RecordsDao` already provides `insertMedication`, `updateMedication`, `incrementPatientRevision`.
- `PatientListScreen.kt` already uses a `ModalBottomSheet` in the described way.
- `filterMedicationTiles`, `SavePhase`, `medicationDraft`, `ClinicalCommandFailure`, `RecordUndoCommand`, `AncDatePickerDialog`, `keepSavedNotice`, `clearDrafts`, `UndoCoordinator.offerSaveUndo` exist and behave as described.
- The named test files (`AncStateTest`, `AncDeleteUndoStateTest`, `DraftRecoveryTest`, `MedicationEntryDesignTest`, `DeleteUndoUiTest`, `AncWorkflowTest`) exist and assert the old route.
- The "whole-suite emulator script" and the Room schema file under `app/schemas` exist as described.

---

## What the document does well

The two-tap cost accounting is consistent everywhere (goals, CUJs, alternatives); each alternative is rejected with a dated author rationale, which makes the decision trail self-contained; the risks section honestly names the notes-as-text fragility and the atomicity hazard; and the review-requirement sections are internally consistent about which paths trigger the elevated review.
