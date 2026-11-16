# Round 3f plan (owner decisions of 2026-10-30) - PLAN, not yet built

## Standing principle (applies to every round)
Reuse, never duplicate. If a function already implements a rule (EDD, date parse, SHA-256 hex, Rh/risk, bounded bitmap decode, ...), every other place calls that function. A spec that needs the same output elsewhere must name the existing function. Reviewers reject a second copy of a rule. Audit found existing violations: EDD x3, ISO date parse x2 + ~28 inline, SHA-hex x5, bitmap decode x4. Each is to be folded into one helper when its area is touched.

## Owner decisions
1. Camera must be INSIDE the app (a live preview frame with a capture button), so the app is never left, and the app lock cannot trigger while the phone camera app is open.
2. Photos are mostly antenatal cards, prescriptions and reports: modest quality is fine. Offer a small set of PREDEFINED quality presets (no free sliders or numbers). Applied at processing time.
3. Fix: attachment save/delete errors shown to the user; delete DB row first, then the file.
4. Real tests for PIN lockout (validatePin, reset, legacy migration) and the vault-key delete guard (verify with the phone later).
5. Remove the Rh tautology test and the duplicate PatientPrintHtmlGeneratorTest; fix the TEST_MANIFEST entries (UNIT-vault-cleanup dep, missing covers/entries).
6. Bounded photo decoding everywhere (one shared helper).
7. Photo encryption stays parked after these.

## Split into rounds (smaller = safer; each runs through the pipeline)
- 3f-1 (no new dependency): items 3, 4, 5, 6 plus the reuse helpers touched by them (shared bounded decode; sha256Hex).
- 3f-2 (camera + presets): in-app camera and quality presets. NEEDS A NEW LIBRARY (CameraX: camera-core, camera-camera2, camera-lifecycle, camera-view), which is a new dependency; owner OK needed. The CAMERA permission is already declared; add the runtime permission request and a clear "denied" screen.

## Quality presets (proposal, to confirm)
| Preset | Max side | WebP quality | Use |
|---|---|---|---|
| Small (default for documents) | 1280 px | 70 | cards, prescriptions |
| Standard | 1600 px | 75 | reports, lab sheets |
| Detailed | 1920 px | 85 | ultrasound prints (today's setting) |
Presets live in one enum used by the vault compressor; the chosen preset is stored with the attachment. Existing photos stay unchanged.

## In-app camera design notes
- Replace ActivityResultContracts.TakePicture in AddAttachmentDialog.kt and UltrasoundEditor.kt with one shared composable (reuse, not two copies).
- Preview inside a frame in the dialog; capture button; retake; flash toggle optional.
- Capture goes to the app's cache, then through the same vault compressor/preset; the temp file is deleted after saving or cancel.
- Lock interaction: the activity stays foreground, so the 10-minute away rule is not triggered. If the lock fires while the camera is open, release the camera and drop the temp file.
- Gallery pick stays as is, using the same compressor.
- To test: device pass with the Honor 400 (QA copy only). The emulator camera is virtual and enough for a first check.
