## Team GPT-6 Luna - lens UI, UX, ACCESSIBILITY and TRANSITIONS

### Bugs

| # | Sev | file:line | quote (<=1 line) | failure scenario (input -> wrong result) | fix (1 line) | confidence H/M/L |
|---|---|---|---|---|---|---|
| 1 | Should-fix | `app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt:1157` | `.clickable { onViewPhotos(linkedScan, idx) },` | A clinician using touch accessibility or with limited dexterity tries to open a scan photo from the visit history; the chip’s text and 4dp vertical padding produce a target well below 48dp, making it difficult to activate reliably. A similar small clickable chip appears at line 1513. | Give photo chips at least a 48dp minimum height while retaining their visible content. | H |

### Style groups

| rule | deviation | count | examples file:line | introduced by (commit/model if known) |
|---|---|---:|---|---|
| User-facing UI text should use string resources, not inline literals, to support localization and consistent copy. | `CodexSettingsSections.kt` uses hard-coded English labels, tab names, confirmation copy, and button text. | Many (at least 20 visible instances) | `app/src/main/java/com/sherifradwan/ancnative/ui/CodexSettingsSections.kt:125,148,398-407` | `5c272ef3` (`feat(ui): 100% English conversion...`); author shown by blame as `sherifrad` |
| User-facing UI text should use string resources, not inline literals. | Photo-chip labels are constructed inline rather than localized. | 2 | `app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt:1165,1521` | `c0e49179` (`fix(clinical-ui): tested delivery-notes parser...`); author shown by blame as `sherifrad` |

**Dominant pattern:** UI is organized in feature-oriented files under `ui/`, with most shared/user-facing text in `strings.xml`, Compose state close to the screen/editor, and shared styling in `Theme.kt` and `Components.kt`.

### Not covered / not verified

- Read-only source inspection only; no builds or tests were run.
- I did not verify target sizes, font scaling, contrast, or transitions on actual devices.
- No clinical, storage, or security behavior outside the UI/UX/accessibility lens was assessed.

### Verdict: can the project continue to the next tasks?

**YES-after-fixes.** First: (1) enlarge the undersized clickable photo chips; (2) move hard-coded Codex UI copy into string resources; (3) localize the photo-chip labels.
