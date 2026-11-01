# Plan: design (UI/UX) work separated from logic (functions) work
Written 2026-11-01 by the orchestrator after the lab entry round (commit 3993cd6). Read-only analysis; no app code changed.
Evidence: tracker tasks, OWNER_DECISIONS D1-D23, `git log`, greps of `app/src/main` (listed under "Duplicates found"). Nothing here was run on a device.

## Rules for every round from now on
1. A round is EITHER logic OR design, never both.
   - Logic round: model/state/storage code and unit tests. No layout, colour or wording changes. Compose files are touched only to call the new function.
   - Design round: Compose files, shared components and strings only. No model, state or storage changes, no clinical rule changes. Mock-up first, owner approves, then build.
   - If a feature needs both, split it: logic round first (with its tests), design round second that only calls it.
2. One function per rule. Before a lane writes anything it greps for the rule; the round's static check asserts "defined once, called from the UI". A copy found in review is a Hard Blocker.
3. Shared UI pieces (card, section title, label/value, choice chips, action bar) exist once in a design-system file; editors call them. No editor draws its own.
4. Reviews: a frontier-tier review and an independent test review for every `app/src` round (AGENTS.md). Still owed for the ultrasound, lab settings and lab entry commits.

## A. DONE (committed locally, not pushed; device checks still owed)
| What | Commit | Kind |
|---|---|---|
| Medication: four statuses, Prophylaxis button hidden, profile and print use the new status | 419ccc8 | logic + design |
| Ultrasound: impression built from the doctor's choices, old presets removed, live report card | 8c06c76 | logic + design |
| Lab settings: edit test name, range, unit, category; "Codex" wording gone | 279f095 | logic + design |
| Lab entry: pinned action bar, panel brings all tests, flag from range, light unit/range line | 3993cd6 | logic + design |
| Earlier: one due-date rule, one date parser, reuse sweep (7 duplicates merged), draft recovery, PIN lock, camera, print fixes | see tracker | logic |

## B. LOGIC / FUNCTIONS still to do (fleet writes tests first; no visual change)
| # | Item | Rule that must exist once | Notes |
|---|---|---|---|
| L1 | Dating: changing LMP or IVF dates never changes GA (w01) | the dating engine: due date per method + which one is agreed | tests prove changing LMP/IVF changes GA; the side-by-side screen is design D3 |
| L2 | New patient: choose dating method (w02) | reuses L1, no second dating rule | after L1 |
| L3 | Attention notes: split into lines, count "+N more" (w03) | one note-splitting function for profile card, list and print | |
| L4 | Hysterotomy counts as cesarean, difference shown (w12, D17) | the existing single previous-delivery function | |
| L5 | Delivery plan fields (w04) | one delivery-plan model | OWNER INPUT FIRST: which new fields |
| L6 | Panel queue survives rotation | queue state moved into the state layer (not `remember`) | found in the lab review; small |
| L7 | Edit medications in settings | same edit pattern as lab; share the collision/trim helper, do not copy `applyLabItemEdit` | |
| L8 | Cleanup of duplicates (list below) | | removal only |
| L9 | Print title "Active Medications" -> "Medications" | | needs owner yes/no |
| L10 | Verify c16/c20 (BP alert on print, severe-BP badge) are really done | | tracker says active |
| L11 | Data and security backlog | a02 reload after save, s13 restore/lock race, s14 odd-values list, s15 PBKDF2, s16 audit trail, s08 photo encryption, s10 backup attachments/EDD | each its own round; schema changes need a migration |
| L12 | Performance | a01 decode off main thread, a07 catalog off main | low priority |
| L13 | c14 LMP cycle length | stored field + migration | owner said not now |
| L14 | Release | r02 shrink, r04 version/upgrade test, r05 final regression, r06 merge/push (owner only) | |

## C. GRAPHIC UI/UX still to do (mock-up -> owner approves -> Compose only)
| # | Item | Notes |
|---|---|---|
| D1 | Design system file (w11): `SectionCard`, `SectionTitle`, `LabelValue`, `ChoiceChips`, `EditorActionBar` (generalises the lab bottom bar), tokens | FIRST design round; removes the repeated boilerplate below; no behaviour change |
| D2 | Apply D1 to the editors already redone (lab, ultrasound, medication) | pure refactor; screenshots before/after |
| D3 | Dating screen: three due dates side by side, "Use this one" (w01 UI) | after L1 |
| D4 | Obstetric history (w10) two switchable designs; previous pregnancies list (w09) | mock-ups first |
| D5 | Same B style for visit, dating, other histories, patient form, delivery editor (w04 UI) | mock-ups first, one at a time |
| D6 | Attention card shows "+N more" (w03 UI) | after L3 |
| D7 | Lab: on-screen number keypad | owner decision needed; native number keyboard works today |
| D8 | Ultrasound: biometry steppers, numbered collapsible sections | steppers need owner-approved reference values (a logic input) |
| D9 | Polish backlog: u06 Arabic-name card icon order, u07 profile header badge, u09 large-font chip, u11 workspace opens scrolled, u17 camera/dialog items; visit editor Cancel/Save row | all use D1 |

## D. Duplicates found now (evidence from grep, this session)
1. `ui/ClinicalPresets.kt`: `ClinicalLabPreset`, `ClinicalMedPreset`, `commonLabPresets`, `commonMedPresets` are referenced only inside that file, and `commonUltrasoundImpressions` has no references. They are a dead second copy of the lab and medication catalogs that `ClinicalCodexRepository` already owns. Remove (L8).
2. Card boilerplate: the same `Card(... cardColors(surface) ... border ... elevation)` + bold primary section title is written by hand 3-5 times in each of the 8 editors (about 25 copies). Becomes `SectionCard` (D1).
3. Action row: `EditorActions` (6 editors) and the new lab bottom bar are two implementations of Cancel/Save. Becomes one `EditorActionBar` with optional "add another" (D1).
4. Lab name presets (`Al Borg Labs`, `El Mokhtabar`, ...) are hard-coded in `LabEditor.kt`; demo data uses "Al Borg Medical Labs". One list in the model (L8).
5. Status text `"Active"` is a literal in 8 files (known, from the medication round). One constant (L8).
6. Older, from the reuse sweep and still open: `UltrasoundPhotoViewerDialog` hand-formats "W+D wks"; unused import in `ClinicalModels.kt`. The full duplicate sweep (a11) is still to run before the final regression.

## E. Proposed order
1. L8 cleanup (small, removal only) + L6 rotation fix. One logic round.
2. L1 -> L2 (dating). Logic round, then D3.
3. L3 + L4 (+N more, hysterotomy). One logic round, then D6.
4. D1 design system (mock sheet for approval) -> D2 -> D4/D5 one section at a time.
5. L7 medication editing, L5 delivery plan after the owner lists the fields.
6. L11/L12/L14 and device testing (including the parked settings-dialog test D23) before release.
