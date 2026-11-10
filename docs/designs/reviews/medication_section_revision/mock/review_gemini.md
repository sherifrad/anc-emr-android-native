# Adversarial Review: `medication-section.html`

## Critical Failures & Blockers

* **ORACLE-2 FAILS ON TAP COUNT & CATALOG COMPLETENESS**: Adding a catalog drug cannot be completed in 2 taps from the default state. The picker is collapsed by default ([line 444](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L444)); expanding it requires tap 1, selecting a tile is tap 2, and tapping Save is tap 3. The mock conceals this by faking task tracking, initializing `S.task.taps = 1` inside [`openSheet()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L552) on the tile tap and ignoring the expand tap. Furthermore, catalog drug `antid` (Anti-D immunoglobulin) has `freq: ''` ([line 399](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L399)), causing validation failure ([line 657](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L657)) on Save and requiring additional taps.
* **ORACLE-1 FAILS DEFAULT DEMONSTRATION**: The seeded dataset contains only 4 active rows ([lines 404-407](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L404-L407)), which occupies ~240px and fails to overflow `#scroller` in the 780px phone frame ([line 79](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L79)). It cannot demonstrate scroll preservation without manually activating the external helper `#prep` ([line 856](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L856)). Moreover, when scrolled, adding an item prepends a row (`unshift`, [line 685](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L685)), causing a 60px visual layout shift even if numeric `scrollTop` is static.

---

## Oracle Checklist (ORACLE-1 to ORACLE-11)

### ORACLE-1: Tab, Scroll & Picker State Preservation
* **Verdict**: **FAIL**
* **Proof**: [`refreshLists()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L531) retains `S.query` and `S.showPast` across commits ([lines 682-700](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L682-L700)), and `#o1` logs static scroll metrics ([lines 815-820](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L815-L820)). However, default data in [`seed()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L403-L410) has only 4 active items, making the list too short to scroll.
* **Strongest Failure Reason**: Adding an active item prepends to the DOM via `unshift` ([line 685](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L685)), causing a 60px visual shift; stopping an item collapses active height, clamping `scrollTop` when near the bottom.

### ORACLE-2: 2-Tap Add & 2-Tap Stop
* **Verdict**: **FAIL**
* **Proof**: Default picker is collapsed with `hidden` ([lines 443-444](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L443-L444)). Expanding it requires an initial tap (`#pickerToggle`, [line 732](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L732)), making add 3 taps. Anti-D (`antid`, [line 399](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L399)) has empty frequency, blocking 2-tap save via [`validate()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L657).
* **Strongest Failure Reason**: The tap counter explicitly disguises the picker expansion tap by initializing `S.task.taps = 1` inside [`openSheet()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L552).

### ORACLE-3: 48x48px Hit Targets
* **Verdict**: **PASS**
* **Proof**: Touch targets explicitly enforce `>= 48px`: `.icon-btn` ([line 85](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L85)), `.tab` ([line 99](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L99)), `.btn` ([line 106](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L106)), `.row-stop` ([line 120](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L120)), `.chip` ([line 134](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L134)), `.inp` ([line 176](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L176)), `#kbAction` ([line 208](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L208)), verified by [`#auditBtn`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L864-L874).
* **Strongest Failure Reason**: Native browser calendar sub-elements inside `input[type="date"]` ([line 595](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L595)) contain shadow-DOM touch targets well below 48px.

### ORACLE-4: Contrast Ratios
* **Verdict**: **PASS**
* **Proof**: Hex tokens ([lines 14-35](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L14-L35)) deliver `>= 4.5:1` for normal body text and `>= 3:1` for large text across light and dark modes, tested via [`contrastTable()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L877-L894).
* **Strongest Failure Reason**: Default input placeholders in sheet inputs ([lines 583-585](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L583-L585)) omit `::placeholder` styling, relying on browser defaults that often dip below 4.5:1.

### ORACLE-5: 360px Narrow Viewport Truncation
* **Verdict**: **PASS**
* **Proof**: `.pname` specifies `overflow-wrap: anywhere` ([line 90](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L90)), `.pills` wrap ([line 91](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L91)), and GA, BP, and High Risk badge render cleanly ([lines 467-474](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L467-L474)).
* **Strongest Failure Reason**: In 200% font scale ([line 81](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L81)), the wrapped header consumes >350px of vertical viewport before any list rows appear.

### ORACLE-6: Field & Save Visible with Keyboard
* **Verdict**: **PASS**
* **Proof**: `.kb` takes 280px ([line 201](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L201)); `.sheet` uses `max-height: calc(100% - 12px)` ([line 162](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L162)) within flex `.app` ([line 82](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L82)), pinning `.sheet-foot` ([line 172](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L172)) above the keyboard, with auto-scroll via [`reveal()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L641-L645).
* **Strongest Failure Reason**: If a field with error messages exceeds `.sheet-body`'s shrunken height at 200% font scale, midpoint centering in `reveal()` clips the field top or bottom.

### ORACLE-7: Visible Disabling During Save
* **Verdict**: **PASS**
* **Proof**: `applyEntryState()` disables all `[data-entry]` elements ([lines 524-530](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L524-L530)), styled with hatched dashed backgrounds ([line 111](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L111)); sheet buttons disable via `${dis}` ([lines 601-604](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L601-L604)); `#scroller` is marked `inert` ([line 539](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L539)).
* **Strongest Failure Reason**: `#lockMsg` ([line 528](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L528)) is placed inside `#scroller`, which is set to `inert` ([line 539](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L539)), hiding the lock message from accessibility trees during saving.

### ORACLE-8: Save Failure Handling
* **Verdict**: **PASS**
* **Proof**: Failed save sets `S.phase = 'idle'`, preserves `D.values`, and displays named error cause `#failbox` without retry timers ([lines 674-678](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L674-L678), [line 581](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L581)).
* **Strongest Failure Reason**: The error text instructs "Close and reopen the patient" ([line 347](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L347)), but patient closure is not mocked (`notMock`, [line 730](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L730)).

### ORACLE-9: Cancel, Scrim, Back & Discard Prompt
* **Verdict**: **PASS**
* **Proof**: Scrim, cancel button, Back key, and Esc invoke `requestClose()` ([lines 705-710](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L705-L710), [line 739](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L739), [line 853](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L853)), evaluating `dirty(S.draft)` ([line 558](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L558)) to trigger `#dlgKeep`/`discard` alertdialog ([lines 605-608](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L605-L608)).
* **Strongest Failure Reason**: `dirty()` compares via `JSON.stringify` ([line 558](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L558)); property key reordering would trigger unintended discard prompts.

### ORACLE-10: No Color-Only Information
* **Verdict**: **PASS**
* **Proof**: Statuses and errors pair icons with text: stop symbol has `i-stop` + "Stop" ([lines 120](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L120), [485](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L485)); alert banner has `i-warn` + text ([line 474](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L474)); active chips have checkmark icons ([line 570](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L570)); errors show `i-warn` + string ([line 564](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L564)).
* **Strongest Failure Reason**: Invalid input borders turn red ([line 178](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L178)), relying entirely on the adjacent error paragraph for non-color indication.

### ORACLE-11: Out-of-Scope Stage 1 Note
* **Verdict**: **PASS**
* **Proof**: `<aside class="note" id="scopeNote">` ([lines 313-321](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L313-L321)) exhaustively enumerates course length, change history/"Changed" mark, undo for stop/dose, and favourites/variants.
* **Strongest Failure Reason**: The note lives in the outer review panel; at `<420px` responsive stacking ([line 211](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L211)), it is pushed below the fold.

---

## Additional Defects

1. **Accessibility (WAI-ARIA Radio Group)**: Chip groups use `role="radiogroup"` ([lines 568-571](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L568-L571)) but lack arrow key roving tabindex navigation; all 11 chips remain in standard Tab order.
2. **Accessibility (Toast Duration)**: The toast auto-dismisses after 3500ms ([line 721](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L721)), violating WCAG 2.2.1 for reading/motor impaired users.
3. **Accessibility (Scrim Tabbing)**: The scrim button has `tabindex="-1"` ([line 575](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L575)), preventing keyboard-only users from targeting the dismiss backdrop.
4. **RTL Directional Defect**: The saving progress bar uses `translateX(-100%)` to `translateX(250%)` ([line 170](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L170)), animating left-to-right regardless of `dir="rtl"`.
5. **RTL / Internationalization**: The simulated keyboard is hardcoded English QWERTY with LTR forced ([lines 205](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L205), [790](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L790)), failing to reflect Arabic layout when testing RTL.
6. **Keyboard Enter Trapping in Notes**: Pressing Enter in `f-notes` textarea without Shift triggers form submission via [`attemptSave()`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L639) ([lines 778-782](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L778-L782)), preventing multi-line entries unless Shift is held.
7. **Simulated Keyboard Action Desync**: Clicking `#kbAction` ("Next"/"Done", [lines 791-796](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/mocks/medication-section.html#L791-L796)) depends on `document.activeElement`. If focus blurs to `#kbAction` itself, the action silently drops.
