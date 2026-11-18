1 Problem as reported.
Owner reports four PDF/print defects: unlabeled patient name; misaligned age, blood group and clinical risk on patient strip; oversized lab tables and wasted space; Arabic doctor name/title not using supplied ArslanWessamB.ttf.

2 Root cause or current behaviour.
- Name has no label: PatientPrintHtmlGenerator.kt lines 225-226 render `.patient-name` directly; only `patientNameDirAttr` at line 176 handles RTL.
- Strip alignment: lines 857-860 use flex `align-items: flex-end`, right-aligned cells, and lines 244-247 puts risk pill outside `.vital-value`, so baseline differs.
- Lab waste: lines 552-560 compute fixed percentage widths; lines 880 and 883-884 use fixed table layout and 2px 5px padding; notes rendered as separate block by `renderLabNotesHtml` lines 663-684 and appended lines 589-606, 632-650.
- Font: only font reference found is README line 1; generator CSS lines 807 and 913-915 does not reference `fonts/ArslanWessamB.ttf`; loading code with base URL is not visible in files given.
- Data model supports notes: ReportData.kt lines 43-48, 51-56, 74-78.

3 Proposed fix.
1. Add `<span class="patient-tag">Patient</span>` before name in lines 225-226; CSS `.patient-tag { font-size: 9pt; }`, keep dir attr on name only.
2. Change strip CSS lines 857-860 to:
`.pt-vitals-strip { display: flex; gap: 12px; align-items: baseline; }`
`.vital-cell { display: flex; flex-direction: column; gap: 1px; min-width: 72px; }`
`.vital-label { white-space: nowrap; }`
`.vital-value { min-height: 1.2em; }`
and wrap `$riskPillHtml` in `<span class="vital-value">`.
3. Compact labs: set `table.clinical-table { table-layout: auto; }` for lab tables or remove fixed widths; reduce padding to `1px 3px`; keep font 9pt; use `width: max-content` on date/value columns and allow Test column to wrap. Merge notes into matching date cell as `<span class="lab-note-inline">note</span>`; remove separate `lab-notes` block or keep only unmatched notes.
4. Add `@font-face { font-family: "A Arslan Wessam B"; src: url(fonts/ArslanWessamB.ttf) format("truetype"); font-weight: 400 700; }` to buildCss; apply `font-family: "A Arslan Wessam B", "Noto Naskh Arabic", Tahoma, sans-serif;` to `.doc-ar-name` and `.doc-ar-title`. Safe fallback as above.
5. Keep HTML strings escaped; no new hardcoded user-facing strings outside existing report labels.

4 Tests to add.
- Unit test `generateHtml` contains `Patient` tag before escaped name and preserves `dir="rtl"` for Arabic names.
- CSS assertion for vitals strip and 9pt minimum in lab note inline.
- Lab section test: notes render inside matching date cell, no `.lab-notes` block when all notes matched; compact padding class.
- Font test: generated HTML contains `@font-face` and `fonts/ArslanWessamB.ttf`, applied to `.doc-ar-name`.

5 Risks, protected or level 3 files touched.
- `PatientPrintHtmlGenerator.kt` is protected by `*Generator*`; changes need review and tests.
- `ReportData.kt` may need note matching helpers; not listed protected but data schema-ish.
- Risk: WebView base URL must be `file:///android_asset/`; asset path not visible in files given. Font fallback must remain readable.
- Questions: exact patient tag wording; whether notes can always map by date and test; whether 8pt notes acceptable or enforce 9pt.

6 Task entries.
- PDF patient strip label and alignment: add Patient tag and baseline CSS in PatientPrintHtmlGenerator.kt; reuse existing RTL attr; avoid changing footer.
- Compact lab tables and inline notes: reduce padding and fixed widths, move notes into date cells; update renderLabsSection and renderLabNotesHtml.
- Arabic doctor font embedding: add @font-face and fallback in buildCss; test generated CSS and font reference.

