**No critical or major findings.** The added strings are specific, correctly scoped UI copy: no em dashes, no buzzwords from the Empty AI Vocabulary list, no fabricated numbers or claims (counts come from real `%1$d`/`%1$s` placeholders), no all-caps emphasis, no negative parallelism or theatrical takeaways. The removed generic strings ("Tap to add", "usual dose, today") being replaced with contextual ones is a net improvement. One minor finding:

**Minor — `app/src/main/res/values/strings.xml` (added line `medication_route_locked_note`)**
- **Quoted text:** `<string name="medication_route_locked_note">A different route is a new drug.</string>`
- **Rule it breaks:** Aphorism Formulas (R-36). "X is a Y" in a terse declarative is the exact shape the skill flags ("X is the currency of Z", "X is not a tool but a mirror"). It states a conclusion without stating the behavior behind it, and in a clinical UI that under-specification is the only real cost: a nurse reading it learns the rule's *label* but not what the app will actually do.
- **Exact fix:** Name the behavior instead of the aphorism: `<string name="medication_route_locked_note">A different route is saved as a new medication.</string>` (or, if the field is locked because the route is fixed at creation: "The route is fixed for this medication. Changing the route creates a new record.")

Not reported, for the record (with the skill's own carve-outs applied):
- "The medication was not saved." — passive, but the actor (the system/save operation) is unknown-irrelevant in an error dialog, which the skill explicitly does not ban.
- "No past medications recorded for this patient." — passive with an unknowable actor (history recording), same carve-out.
- "Active Prescriptions (%1$d)" — a screen title, not a list item, so the Title Case in List Elements rule doesn't reach it; "Prescriptions" is also a domain noun that reads naturally capitalized here.
- Identical `one`/`other` plural items ("Show past (%1$d)" twice) — a localization/plurals-mechanics issue, not one this skill governs.
