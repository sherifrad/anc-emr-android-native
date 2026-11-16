# Code review: `wp1_main.patch` (`model/DeliveryNotes.kt`)

Verdict: the pure functions are a faithful transcription of design §5.3 and I traced Theorem A/B and Corollary C against this exact code — they hold (strict branch runs first on pre-trimmed lines, `matchEntire` anchors both ends, DOTALL covers U+2028/U+0085 inside values, compose's trim/drop/distinct matches N(U), no label is a prefix of another). The failures below are integration, test, and edge-case failures, not proof-breaking ones.

## Blocker

None in the pasted code. M2 below becomes a blocker if the parser move is not already landed.

## Major

**M1 — Behaviour change shipped with zero test changes.** The strict branch (`DeliveryNotes.kt`, patch lines 21–31) changes parsing for any line starting exactly `Delivery Plan:`, `Postpartum Note:`, `Agreed Delivery Fees:`, `Agreed Delivery Hospital:`. Design §8 step 2 states the existing suite *contains* such fixtures ("except any case that uses exact-case `Postpartum Note:` with `;` or a hospital phrase") — their expected boxes change. This patch moves or updates no tests. Concrete failure: a fixture like `Postpartum Note: IUD at 6 weeks; remove at 6 weeks` previously split at `;` across boxes (and could derive a hospital); now it lands whole in postpartum → `:app:testDebugUnitTest` fails — or worse, stays green because the tests still exercise the old `ui/` copy (M2).

**M2 — Old parser path and consumers not repointed; two parsers can coexist.** The diff is an in-place modification of `model/DeliveryNotes.kt` (no `rename from ui/…`, no deletion, no import changes to `DeliveryPlanningCard`/print/summary). If the §8-step-1 `git mv` is not a prior commit, the app compiles with the card still calling the `ui` copy while reducers compose with the `model` copy. Concrete divergence: notes contain `Delivery Plan: SVD at Cleopatra Hospital` — the card (old parser) derives `Cleopatra Hospital` into the hospital box and echoes the label inside the plan box; after any sheet save the hospital box is empty and the plan box clean, so the card changes under the doctor. Close this with the `rg "parseDeliveryNotes|obstetricNotes"` output from design §10.4 in the PR; AGENTS.md's zero-legacy-baggage rule also forbids leaving the old path.

**M3 — `else -> postpartum += value` is a silent catch-all** (patch lines 24–29). Today `groupValues[1]` can only be one of the four alternatives, but if a fifth label is ever added to the regex and the `when` is forgotten, its lines silently land in the postpartum box. Replace `else` with an explicit `LABEL_POSTPARTUM ->` branch and no `else`: a future fifth alternative then fails to compile instead of mis-filing clinical text.

## Minor

**m1 — U+2028/U+0085 between labelled lines folds content into one box** (regex, patch lines 50–53). `lines()` does not split on these, so a note pasted from a NEL-terminated source — `Agreed Delivery Hospital: Cleopatra` + U+0085 + `Agreed Delivery Fees: 20000` — is *one* line; the strict rule swallows everything after the first label into the hospital value and the fees vanish from their box. The round trip is self-consistent (`isComposedDeliveryNotes` → true, no notice), so nothing ever surfaces it. A8 tests these separators only "at the edges of values"; add a between-labels pinning test, or reject these separators in strict values.

**m2 — compose does not split field values on U+2028/U+0085** (patch lines 67–68). A visual line break inside a field is stored and re-shown as one labelled line, so Goal 3's "one line per non-blank line" fails for those separators. This is required by N(U)'s definition (splitting would break Theorem A), so fix the KDoc and add a test rather than the code.

**m3 — `isComposedDeliveryNotes` is byte-exact** (patch lines 75–76). A composed doc with a trailing `\n` or CRLF endings compares unequal → the notice fires and the next edit-save silently rewrites, dropping bytes with no visible change. Disclosed by A1, but the notice copy promises a visible restructure, so this reads as a false warning. Consider comparing normalised forms.

**m4 — Label-constants ↔ `strings.xml` drift test absent.** The four hard-coded English `LABEL_*` constants (patch lines 41–44) are correct per D11, but the A8 test pinning them to the card titles lives elsewhere. If the strings patch renames a title, stored labels and display text drift silently and the round trip breaks.

**m5 — Strict rule documented twice** (patch lines 9–13 and 46–49). Two normative copies will drift; keep the paragraph on `parseDeliveryNotes` and link from `COMPOSED_LINE`.

## Dimensions checked and not applicable

No composables in this patch: 360dp layout, 48dp hit targets, hard-coded colours, focus/Enter-to-next, rotation/recomposition state, RTL and accessibility are untouched. The only strings are the stored-label constants (hard-coded by design). File grows ~50 lines, far under the 1,500 ceiling, and the change moves clinical logic *out* of the UI layer (D7), not into a composable. Design conformance otherwise: strict branch is first in the loop, compose sits next to parse before the segmentation section, existing KDoc intact — all per §5.3 and §8 steps 1–2.
