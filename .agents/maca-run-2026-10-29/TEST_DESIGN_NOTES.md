# Test notes (designed from SPEC.md only; gradle not run)
- ParseDeliveryNotesSpecTest: 21 JVM tests on parseDeliveryNotes/DeliveryNoteSections only.
- O1: null/empty/blank/whitespace -> all four null; each non-empty line appears exactly once across sections; order kept; CRLF/blank-line tolerant; no blank non-null sections.
- O2: previous delivery / previous discharge (EN and AR) never in postpartum, but still reachable; hospital+fee line never lost.
- O3: fee-only and clinical sentences never returned as hospital; hospital null when none mentioned.
- Mixed-case keywords and an Arabic line (not dropped, not duplicated) covered.
- Ambiguous: (a) which section holds unclassified lines (test only requires reachable); (b) hospital+fee line goes to hospital or fees (O2 vs O3 tension; test accepts either, exactly once);
  (c) where history lines go (test only forbids postpartum); (d) Arabic keyword support; (e) exact section text format (labels stripped or not) - tests use substring checks;
  (f) "exactly one section" vs "reported drop" - tests assume no drops.
- Assumes DeliveryNoteSections is a data class with the four named String? properties.
- O4/O7 need Compose UI tests (Robolectric ok); O5/O6/O8/O9 can be pure unit tests if predicates/formatters are extracted; only RTL/dark visual checks need a device.
