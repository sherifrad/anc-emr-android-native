ROLE: TEST DESIGNER for ROUND 3f-1 (SPEC_FIX_R3.md section "Round 3f-1", Q53 and S29-S35). Write tests only; no production code.
ALLOWED files: .agents/checks/check_r3f1.py (NEW), .agents/checks/check_r3b.py (rewrite S6 only, per Q53c), .agents/TEST_MANIFEST.json,
app/src/test/java/com/sherifradwan/ancnative/: NegativeControlAndClinicalValidationTest.kt (Rh test only), storage/HashingTest.kt (NEW), storage/BoundedBitmapTest.kt (NEW),
storage/VaultCleanupRuleTest.kt (extend), security/SecuritySettingsRepositoryTest.kt (NEW), security/FakeSharedPreferences.kt (NEW test helper), state/AttachmentErrorStateTest.kt (NEW).
Before writing, READ ui/ClinicalFormat.kt (isRhNegative/isRhPositive), security/PinPolicy.kt (lockoutDelayMs, isValidNewPin), the current SecuritySettingsRepository.kt and storage/VaultCleanupRule.kt and VaultController.kt.
Follow Q53 exactly. Expectations come from the SPEC and the documented behaviour, not from guessing today's output on ambiguous inputs; say which you assumed.
New tests must reference only the APIs defined in the spec (Hashing.kt, BoundedBitmap.kt, cleanUpAbandonedInitialization, the internal repository constructor, AttachmentError/AttachmentFailed/DismissAttachmentError); they will not compile until core is done - that is expected.
check_r3f1.py: python3 stdlib only, follow check_r3e.py style, strip comments first, PASS/FAIL per S, exit 1 on any FAIL, every S must FAIL on today's code. S31 must match the real guard (the `if (!hasVaultDatabase(` immediately before the deleteOrphanAliases invocation), not a text window.
TEST_MANIFEST: edit directly; keep other entries unchanged apart from what Q53d names. Run check_r3f1.py and report the real results.
