Defects:
- `ReportData.kt:50-55,214-225`: `SingleLab` has no unit field and singles mapping omits unit, so one-date lab results lose their unit contrary to the rule.
- `ReportData.kt:115-127`: `latestScanForVisit` treats unparseable scan dates as earlier than any parseable date via the boolean key, inconsistent with other sort helpers and can wrongly choose/exclude scans when dates are malformed.
- `ReportData.kt:305-313`: `figuresFor` re-sorts selected candidates by display-date parsing instead of the raw-date ordering used when candidates were collected, so selected figures can be numbered out of actual date order for malformed/unparseable dates.
[Tokens] upload: 25487 download: 2683 cache_read: 0 cache_write: 0

