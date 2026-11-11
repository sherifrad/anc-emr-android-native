No critical or major findings. The added comments overwhelmingly earn their place: they carry constraints the code does not show (360dp narrowing, saved-state survival across rotation, monotonic-clock undo windows, the pipe-joined name/route key, why the notes line is capped while closed). None are false, generic filler, banners, step narration, emoji, or end markers, and I found no em dash or wrong string in added user-visible text.

Minor findings:

**1. Minor — `MedicationCard.kt`, `MedicationCard` KDoc**
Quoted: `One medication row, the only one. Closed, it shows the name, one "dose · route · frequency" line and, when there are notes, their first line; the whole note and the other facts show when the row is opened. Active rows carry the one-tap Stop control; stopped rows do not.`
Rule: The Over-Explained Comment — three stacked sentences around facts the composables below already show; every sentence true, but the length is the tell.
Fix: Cut to one line, e.g. `/** The one medication row: closed it shows name, dose·route·frequency and the first line of notes; open it shows everything. */`

**2. Minor — `MedicationForm.kt`, `MedicationForm` KDoc ("Fields in order: …")**
Quoted: `Fields in order: name, dose, route (read-only on an existing row), frequency, status, one date (Start date while active, Stop date while stopped) and notes. Enter (IME Next) moves focus to the next text field; notes keeps Enter for a new line.`
Rule: Restating the Obvious / Line-by-Line Narration — the field list and IME wiring are directly readable in the composable a few lines below.
Fix: Delete the field-order and IME sentences; keep the non-obvious facts (per-draft reset via `draftKey`, the one-frame focus retry, reveal-after-rejected-save).

**3. Minor — `MedicationForm.kt`, `LaunchedEffect(draftKey, focusNameOnStart)` inline comment**
Quoted: `The name field may not be attached yet after Save and add another, in which case requestFocus reports false. Wait one more frame and retry once so focus still lands.`
Rule: Restating the Obvious — the same retry fact is already stated in the KDoc ("the focus request is retried once after the next frame"), so the reader pays twice for one fact.
Fix: Keep the inline comment (it is next to the code) and drop the retry sentence from the KDoc.

**4. Minor — `MedicationCard.kt`, actions `Row` comment**
Quoted: `The weight lets the actions take the rest of the line, or a whole line when they wrap, and sit at its end.`
Rule: Restating the Obvious — `Modifier.weight(1f)` and `Arrangement.End` show this; only the large-text wrap half is not already visible, and that is FlowRow's standard behavior.
Fix: Remove the comment, or shorten to the one non-obvious half (`// actions wrap to a full line at large text scales.`).

**5. Minor — `MedsTab.kt`, Prescribe button comment**
Quoted: `The weight gives the button the rest of the line, or a whole line when it wraps, and it sits at the end.`
Rule: Restating the Obvious — near-identical filler to finding 4, restating `Box(weight(1f), contentAlignment = Alignment.CenterEnd)` inside `FlowRow`.
Fix: Remove the comment.

Everything else added in this diff is comment-worthy and correctly sized; no code, identifier, or formatting change is flagged (the scope guardrail holds — all findings are comment-only fixes).
