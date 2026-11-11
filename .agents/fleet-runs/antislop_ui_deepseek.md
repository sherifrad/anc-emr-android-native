# Read-only audit brief (do NOT edit any file)
Project: Android EMR for antenatal care (Kotlin, Jetpack Compose). Safety-critical clinical UI, must work on a 360dp wide phone. Repo root is the current directory.
Report format: a markdown list. Each finding = `path:line | severity (BLOCKER/SHOULD/NIT) | what is wrong | suggested fix in one line`. Only report things you verified by reading the file at that line. No praise, no summary of what is fine, max 40 findings, most severe first. Do not invent files or line numbers. Never write patient data. If you find nothing in a category, say "none found".

## Your slice: UI layout, accessibility, states (Compose)
Scope: app/src/main/java/com/sherifradwan/ancnative/ui/ (all .kt, including patienthub/ and patientdirectory/, but NOT report/).
Check for:
1. Touch targets under 48dp (clickable/IconButton/Modifier.size small) and adjacent targets with no gap.
2. Fixed widths/heights in dp or Modifier.width(>=300.dp) or weight-less Rows of 3+ items that can truncate or overflow at 360dp; maxLines/overflow Ellipsis on critical text (patient name, gestational age, blood pressure, high-risk alert).
3. Status shown by color only (no icon or text); hardcoded Color(0x..) instead of theme tokens; low-contrast pairs (compute with WCAG if you flag one).
4. Icon-only buttons or clickable Icons with contentDescription = null; clickable Box/Row/Text with no role/onClick semantics.
5. Data views missing empty, loading or error state, or states that only say "No data".
6. Decorative slop: endless animations (infiniteRepeatable), glow/shadow stacks, generic AI icons, left color stripes with no meaning, emoji in UI strings.
7. Business logic inside composables (should be AncEvent intents), duplicated components that already exist in ui/components or ui/patienthub/components.
