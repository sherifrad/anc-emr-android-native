You are BUILDER B for this Android Kotlin repository (Jetpack Compose, MVI). Antislop rules apply while you write: calm clinical UI, theme colours only (plus the existing ClinicalCardTokens), 360 dp first, 48 dp tap targets, no emoji, no em dash character, no comments that restate the code.
You may edit or create ONLY these files:
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt
app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt
app/src/main/java/com/sherifradwan/ancnative/ui/components/ZoomableImage.kt (new)
app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt
app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt
app/src/main/res/values/strings.xml (ADD the single key `zoom_reset` only, never change or delete a key; another builder adds other keys at the same time)
Do NOT edit tests or any other file. Do NOT run any git command that changes anything. No network, no installs. Do NOT run gradle at all (another builder is compiling in the same folder; the orchestrator compiles after you finish). Be careful with imports and types because you cannot compile: read the neighbouring code for the exact names.
Read docs/designs/reviews/owner_fixes_2026-11-14/SPEC.md and implement sections F2 and F4 exactly (names, signatures, test tags are part of the contract). For F4 use `androidx.compose.foundation.gestures` (`detectTransformGestures` and `detectTapGestures`) with `Modifier.pointerInput`, `graphicsLayer`, `clipToBounds`; one finger drag must only be consumed when scale > 1. When finished print one line per ORACLE (F2, F4) saying what you changed and where.
