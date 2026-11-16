ROLE: TEST DESIGNER for ROUND 3f-3 (SPEC_FIX_R3.md section "Round 3f-3"). Tests only; no production code.
ALLOWED files: .agents/checks/check_r3f3.py (NEW), .agents/TEST_MANIFEST.json, app/src/test/java/com/sherifradwan/ancnative/storage/BoundedBitmapTest.kt (extend; keep existing tests).
1. check_r3f3.py: S43-S44 as the spec says; python3 stdlib only; strip Kotlin comments first; PASS/FAIL per S; exit 1 on any FAIL; style of check_r3f2.py; each S must FAIL on today's code.
2. BoundedBitmapTest: exifTransform for orientation 1..8 and unknown values per the spec table (use the android.media.ExifInterface constant VALUES as plain ints in the test: 1..8).
3. TEST_MANIFEST.json: add STATIC-r3f3 (deps: the files named in S43-S44 + the check); extend the BoundedBitmap entry's covers; edit directly, keep others unchanged.
Run check_r3f3.py and report real results (the unit test will not compile until core is done).
