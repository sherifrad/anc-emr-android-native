Defects:
- `PatientPrintHtmlGeneratorTest`: deleted `q19a_*` scan missing-value/no-invented-finding tests and `q19h_*` visit findings/notes tests; if scans/visits still print, those safety rules still apply → keep/restore adapted.
- `PatientPrintHtmlEscapeTest`: `q93_visitsSection_escapedOnce` no longer checks escaped visit notes (`BP <90 systolic ...`); HTML-escaping safety still applies → keep/restore assertion.
- `q93_bpUnderNinetyNote` wording changed visit→scan but still asserts escaped text; acceptable only if the marker is intentionally in scan impression now.
Other deletions are layout/markup-only (`oracleA`, caps, CSS/table-scroll, section-title count) and are removable.
[Tokens] upload: 31625 download: 1124 cache_read: 0 cache_write: 0

