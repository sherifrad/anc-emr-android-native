You are BUILDER C for this Android Kotlin repository (Jetpack Compose, MVI). Antislop rules apply while you write: calm clinical UI, theme colours only (plus the existing ClinicalCardTokens), 360 dp first, 48 dp tap targets, the level is never shown by colour alone, no emoji, no em dash character, no comments that restate the code, no decorative badges.
You may edit or create ONLY these files:
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt (new)
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionCard.kt (new)
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/OverviewTab.kt
app/src/main/res/values/strings.xml (ADD keys only, never change or delete one)
Do NOT edit tests or any other file. Do NOT run any git command that changes anything. No network, no installs.
Read docs/designs/reviews/owner_fixes_2026-11-14/SPEC_attention.md and implement it completely (storage format, the pure functions, colours helper, header strip, Overview card with `OverviewTab(onEditAttention: (() -> Unit)? = null)`, the rewritten `ClinicalAttentionDialog`). Names, signatures, test tags and visible labels in that spec are a contract that independent tests rely on.
Notes about the current state of the code: a previous builder already added `clinicalAttentionLevel(attention, severeBp)` and a colours helper (`clinicalAttentionColors` or similar) in ClinicalAttentionBanner.kt and a first version of the strip in PatientHubHeader.kt for an OLDER plan (one level for the whole text). Replace that work with the new spec: move `clinicalAttentionLevel` into ClinicalAttentionEntries.kt (same package, so imports stay valid), keep ONE colours helper named `attentionColors`, and make the old banner composable, the strip, the card and the dialog all use it. Remove the dialog's three level buttons and its single multi-line text area.
The patient screen (`PatientHubScreen.kt`) currently opens the dialog from the header strip and saves through `onSave` (it starts the patient editor, sets `PatientDraftField.CLINICAL_ATTENTION`, submits); keep that save path, and also open the dialog from the Overview card (`onEditAttention = { showAttentionDialog = true }` only when the patient can be edited).
You MAY run `./gradlew --offline :app:compileDebugKotlin -q` to check that everything compiles; fix every error in your files. When finished print one line per spec section saying what you changed and where.
