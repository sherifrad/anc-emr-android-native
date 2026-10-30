# Photo storage and encryption plan (paused item s08)

Status: PLAN ONLY. No code changed. Owner decides before any round starts.

## What the code does today (read from PatientAttachmentVault.kt)
- Every photo is already shrunk to at most 1920 px on the long side and saved as lossy WebP, quality 85.
- Files sit in app-private storage: `filesDir/patient_attachments/<patientId>/`. Other apps cannot read them.
- A SHA-256 hash is stored with each record for integrity checks.
- Photos are NOT encrypted at rest and NOT included in backups (labels now say so, round 3d).

## Does encryption need the pictures reformatted? No.
Encrypting means scrambling the saved bytes of each file (AES-256-GCM). The picture format stays WebP and is not re-compressed. Size grows by about 28 bytes per file (nonce + tag). So encryption itself costs almost no storage. The storage cost is the photos themselves. (Sizes below are estimates, not measured on real clinic photos.)

## Where storage really goes
| Lever | Today | Option | Effect (estimate) |
|---|---|---|---|
| Max side | 1920 px | 1600 px | about 30% smaller files |
| WebP quality | 85 | 75 | about 30-40% smaller, still fine for ultrasound prints |
| Both | | 1600 px, q75 | roughly halves each photo |
| Original kept? | No, only the shrunk copy | keep as is | no extra cost |
| Orphans | deleted with record? | check | a small audit round |

A typical 1920 px q85 ultrasound photo is likely in the 150-400 KB range; 1000 photos is then roughly 150-400 MB. Measure 20 real photos before choosing (no patient data leaves the phone; just file sizes).

## Proposed rounds (after the owner picks)
1. P1 Measure and trim (small, safe): read real file sizes on the clinic phone, pick size/quality, add a unit test for the output size cap. No encryption yet.
2. P2 Encrypt at rest (medium): the per-file AES-GCM key lives in the existing vault key hierarchy; photos are encrypted on save and decrypted in memory for display; the shared viewer decrypts to a memory bitmap, never to a plain temp file. One-time migration of existing plain photos, resumable, with the hash re-checked after each file. Old plain file removed only after the encrypted copy is verified.
3. P3 Photos in backups (optional, owner choice): large backups. Options: records-only (today), or a separate photo archive made on demand. Needs a backup format version bump and a web-app change; do after P2.

## Risks to settle first
- Migration interruption: must be resumable and never delete the plain file before the encrypted copy verifies.
- Lock behavior: with the vault closing on lock (3e), decrypted images must also be dropped from memory on lock.
- PDF/print path (`PatientPrintHtmlGenerator` re-encodes photos as PNG at quality 100): this is the biggest memory/size spike; fix in P1.

## Decisions needed from the owner
1. Is a smaller photo (1600 px, q75) acceptable for clinical use?
2. Encrypt existing photos (migration) or only new ones?
3. Photos in backups: yes / no / separate archive?

## Owner decisions (2026-10-30)
Smaller photos acceptable; use predefined presets only (see SPEC_3F_PLAN.md). In-app camera first. Encryption of existing photos and photos in backups remain open and come after.
