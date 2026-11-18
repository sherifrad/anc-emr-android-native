Defects:
- ReportData.kt:87-93 and ReportSections.kt:376/537: `unlinkedScans(patient, shownVisits)` only excludes scans for the first 12 visits, so scans linked to later visits appear as "Other scans" and those visit rows lose their scan outcome.
- ReportSections.kt:506: the scan-outcome `Column` uses `semantics(mergeDescendants = true)`, merging all outcome text into one accessibility node and reducing screen-reader granularity.
- ReportData.kt:67: the normal-amniotic-fluid filter was removed, so "Fluid: Normal/..." is now emitted for normal fluid values instead of being suppressed.
[Tokens] upload: 24387 download: 2265 cache_read: 0 cache_write: 0

