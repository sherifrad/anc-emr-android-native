package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun interruptionAtEveryProvisioningStagePreservesDetectableArtifacts() = runBlocking {
        ProvisioningStage.entries.forEach { interruptedStage ->
            val fixture = Fixture(hook = ProvisioningHook { stage ->
                if (stage == interruptedStage) throw InjectedInterruption()
            })
            try {
                fixture.controller.onForegrounded()
                fixture.awaitState { it is VaultState.Uninitialized }
                fixture.controller.beginInitialization()
                val first = fixture.awaitState {
                    it is VaultState.Authenticating || it is VaultState.Blocked
                }
                if (first is VaultState.Authenticating) {
                    fixture.controller.completeAuthentication(first.request.id)
                    fixture.awaitState { it is VaultState.Blocked }
                }
                assertTrue(fixture.files.inspect() != VaultDiskState.Empty)
            } finally {
                fixture.cleanup()
            }
        }
    }

    @Test
    fun oldMissingAliasAndTamperedGcmEnvelopeFailClosedWithoutReplacement() = runBlocking {
        val missingAlias = Fixture()
        try {
            missingAlias.provision()
            val descriptor = (missingAlias.files.inspect() as VaultDiskState.Active).descriptor
            missingAlias.keys.deleteRegisteredTestAlias(descriptor.aliasId)
            missingAlias.controller.beginUnlock()
            assertEquals(
                VaultState.Blocked(VaultBlockReason.KEY_MISSING),
                missingAlias.awaitState { it is VaultState.Blocked },
            )
            assertTrue(!missingAlias.keys.contains(descriptor.aliasId))
        } finally {
            missingAlias.cleanup()
        }

        val tampered = Fixture()
        try {
            tampered.provision()
            val descriptor = (tampered.files.inspect() as VaultDiskState.Active).descriptor
            RandomAccessFile(tampered.files.envelopeFile(descriptor.vaultUuid), "rw").use { file ->
                file.seek(file.length() - 1)
                val original = file.readByte().toInt()
                file.seek(file.length() - 1)
                file.writeByte(original xor 0x01)
            }
            tampered.controller.beginUnlock()
            val request = tampered.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            tampered.controller.completeAuthentication(request.request.id)
            assertEquals(
                VaultState.Blocked(VaultBlockReason.GCM_AUTHENTICATION_FAILED),
                tampered.awaitState { it is VaultState.Blocked },
            )
        } finally {
            tampered.cleanup()
        }
    }

    @Test
    fun orphanAndPermanentlyInvalidatedAliasesRemainBlocked() = runBlocking {
        val orphanNamespace = UUID.randomUUID().toString()
        val orphanFiles = VaultFiles.isolatedTest(context, orphanNamespace)
        val orphanKeys = FakeVaultKeys().apply { generate(UUID.randomUUID().toString()) }
        val orphanController = VaultController(context, orphanFiles, orphanKeys)
        try {
            assertEquals(
                VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP),
                withTimeout(15_000) {
                    orphanController.view.first { it.state is VaultState.Blocked }.state
                },
            )
        } finally {
            orphanKeys.clearRegisteredTestAliases()
            orphanFiles.cleanupRegisteredTestNamespace()
        }

        val invalidated = Fixture()
        try {
            invalidated.provision()
            val descriptor = (invalidated.files.inspect() as VaultDiskState.Active).descriptor
            invalidated.keys.invalidate(descriptor.aliasId)
            invalidated.controller.beginUnlock()
            val request = invalidated.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            invalidated.controller.completeAuthentication(request.request.id)
            assertEquals(
                VaultState.Blocked(VaultBlockReason.KEY_PERMANENTLY_INVALIDATED),
                invalidated.awaitState { it is VaultState.Blocked },
            )
        } finally {
            invalidated.cleanup()
        }
    }

    @Test
    fun descriptorEnvelopeMismatchAndDatabaseAbsenceFailClosed() = runBlocking {
        val mismatch = Fixture()
        try {
            mismatch.provision()
            val descriptor = (mismatch.files.inspect() as VaultDiskState.Active).descriptor
            val envelope = mismatch.files.readEnvelope(descriptor.vaultUuid)
            mismatch.files.writeEnvelope(envelope.copy(aliasId = UUID.randomUUID().toString()))
            mismatch.controller.beginUnlock()
            val request = mismatch.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            mismatch.controller.completeAuthentication(request.request.id)
            assertEquals(
                VaultState.Blocked(VaultBlockReason.VAULT_METADATA_MISMATCH),
                mismatch.awaitState { it is VaultState.Blocked },
            )
        } finally {
            mismatch.cleanup()
        }

        val absentDatabase = Fixture()
        try {
            absentDatabase.provision()
            val descriptor = (absentDatabase.files.inspect() as VaultDiskState.Active).descriptor
            assertTrue(absentDatabase.files.databaseFile(descriptor.vaultUuid).delete())
            absentDatabase.controller.beginUnlock()
            assertEquals(
                VaultState.Blocked(VaultBlockReason.DATABASE_MISSING),
                absentDatabase.awaitState { it is VaultState.Blocked },
            )
        } finally {
            absentDatabase.cleanup()
        }
    }

    @Test
    fun backgroundAndManualLockHideSnapshotAndRejectLateAuthentication(): Unit = runBlocking {
        val fixture = Fixture()
        try {
            fixture.provision()
            fixture.unlock()
            assertEquals(3, fixture.controller.view.value.snapshot?.patients?.size)

            fixture.controller.onBackgrounded(authenticationOverlayActive = false)
            assertNull(fixture.controller.view.value.snapshot)
            fixture.awaitState { it is VaultState.Locked }

            fixture.controller.onForegrounded()
            fixture.controller.beginUnlock()
            val obsolete = fixture.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            fixture.controller.lock()
            fixture.awaitState { it is VaultState.Locked }
            fixture.controller.completeAuthentication(obsolete.request.id)
            assertEquals(VaultState.Locked, fixture.controller.view.value.state)
            assertNull(fixture.controller.view.value.snapshot)

            fixture.unlock()
            fixture.controller.lock()
            assertNull(fixture.controller.view.value.snapshot)
            fixture.awaitState { it is VaultState.Locked }
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun backgroundEpochRejectsLateDecryptAndReadCompletion() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.provision()
            val gates = fixture.keys.blockNextUnwrap()
            fixture.controller.beginUnlock()
            val auth = fixture.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            fixture.controller.completeAuthentication(auth.request.id)
            assertTrue(gates.first.await(10, TimeUnit.SECONDS))
            fixture.controller.onBackgrounded(authenticationOverlayActive = false)
            assertNull(fixture.controller.view.value.snapshot)
            gates.second.countDown()
            fixture.awaitState { it is VaultState.Locked }
            assertNull(fixture.controller.view.value.snapshot)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun initializationCancellationAndDetachBlockWhileUnlockCancellationRemainsRetryable() = runBlocking {
        listOf<(VaultController, Long) -> Unit>(
            { controller, id -> controller.cancelAuthentication(id) },
            { controller, id -> controller.authenticationDetached(id) },
        ).forEach { interrupt ->
            val fresh = Fixture()
            try {
                fresh.controller.onForegrounded()
                fresh.awaitState { it is VaultState.Uninitialized }
                fresh.controller.beginInitialization()
                val auth = fresh.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
                assertTrue(fresh.files.inspect() != VaultDiskState.Empty)
                interrupt(fresh.controller, auth.request.id)
                assertEquals(
                    VaultState.Blocked(VaultBlockReason.INCOMPLETE_SETUP),
                    fresh.awaitState { it is VaultState.Blocked },
                )
                assertTrue(fresh.files.inspect() != VaultDiskState.Empty)
            } finally {
                fresh.cleanup()
            }
        }

        val initialized = Fixture()
        try {
            initialized.provision()
            initialized.controller.beginUnlock()
            val auth = initialized.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            initialized.controller.authenticationDetached(auth.request.id)
            assertEquals(VaultState.Locked, initialized.awaitState { it is VaultState.Locked })
            initialized.controller.beginUnlock()
            val retry = initialized.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            initialized.controller.cancelAuthentication(retry.request.id)
            assertEquals(VaultState.Locked, initialized.awaitState { it is VaultState.Locked })
        } finally {
            initialized.cleanup()
        }
    }

    @Test
    fun repeatedBeginUnlockReservesExactlyOneAuthenticationRequest() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.provision()
            fixture.controller.onForegrounded()
            repeat(20) { fixture.controller.beginUnlock() }
            val auth = fixture.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            assertEquals(2L, auth.request.id)
            fixture.controller.cancelAuthentication(auth.request.id)
            assertEquals(VaultState.Locked, fixture.awaitState { it is VaultState.Locked })
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun pausedOpeningThenBackgroundLockAndResumeNeverPublishesUnlocked() = runBlocking {
        val admissionStarted = CountDownLatch(1)
        val releaseAdmission = CountDownLatch(1)
        val fixture = Fixture(
            openingHook = OpeningHook {
                admissionStarted.countDown()
                releaseAdmission.await(10, TimeUnit.SECONDS)
            },
        )
        val emitted = Collections.synchronizedList(mutableListOf<VaultState>())
        var collector: Job? = null
        try {
            fixture.provision()
            collector = launch { fixture.controller.view.collect { emitted += it.state } }
            fixture.controller.onForegrounded()
            fixture.controller.beginUnlock()
            val auth = fixture.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            fixture.controller.completeAuthentication(auth.request.id)
            assertTrue(admissionStarted.await(10, TimeUnit.SECONDS))
            fixture.controller.onBackgrounded(authenticationOverlayActive = false)
            fixture.controller.onForegrounded()
            releaseAdmission.countDown()
            assertEquals(VaultState.Locked, fixture.awaitState { it is VaultState.Locked })
            assertFalse(emitted.any { it is VaultState.Unlocked })
        } finally {
            collector?.cancel()
            releaseAdmission.countDown()
            fixture.cleanup()
        }
    }

    @Test
    fun zeroByteDatabaseAndWrongSizedUnwrapFailClosedWithoutReplacement() = runBlocking {
        val zeroByte = Fixture()
        try {
            zeroByte.provision()
            val descriptor = (zeroByte.files.inspect() as VaultDiskState.Active).descriptor
            val database = zeroByte.files.databaseFile(descriptor.vaultUuid)
            RandomAccessFile(database, "rw").use { it.setLength(0) }
            val before = sha256(database)
            zeroByte.controller.beginUnlock()
            assertEquals(
                VaultState.Blocked(VaultBlockReason.DATABASE_INTEGRITY_FAILED),
                zeroByte.awaitState { it is VaultState.Blocked },
            )
            assertEquals(0L, database.length())
            assertEquals(before, sha256(database))
        } finally {
            zeroByte.cleanup()
        }

        val wrongSize = Fixture()
        try {
            wrongSize.provision()
            wrongSize.keys.nextUnwrapSize = 31
            wrongSize.controller.beginUnlock()
            val auth = wrongSize.awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            wrongSize.controller.completeAuthentication(auth.request.id)
            assertEquals(
                VaultState.Blocked(VaultBlockReason.ENVELOPE_INVALID),
                wrongSize.awaitState { it is VaultState.Blocked },
            )
        } finally {
            wrongSize.cleanup()
        }
    }

    @Test
    fun testAliasesAreDisjointAndHardwareBackingIsMeasuredWithoutPromise() {
        assertFalse(VaultFiles.TEST_ALIAS_PREFIX.startsWith(VaultFiles.CANONICAL_ALIAS_PREFIX))
        assertFalse(VaultFiles.CANONICAL_ALIAS_PREFIX.startsWith(VaultFiles.TEST_ALIAS_PREFIX))
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val probePrefix = "com.sherifradwan.ancnative.p2.probe.$namespace."
        val testKeys = AndroidVaultKeys("${probePrefix}test.", testCleanupAllowed = true)
        val productionProbe = AndroidVaultKeys(
            aliasPrefix = "${probePrefix}prod.",
            testCleanupAllowed = true,
            orphanDetectionPrefixes = setOf(probePrefix),
        )
        val legacyKeys = AndroidVaultKeys(probePrefix, testCleanupAllowed = true)
        val testAliasId = UUID.randomUUID().toString()
        val legacyAliasId = UUID.randomUUID().toString()
        try {
            assertTrue(testKeys.generate(testAliasId) is KeyAccessResult.Success)
            assertFalse(productionProbe.hasAnyAlias())
            assertTrue(legacyKeys.generate(legacyAliasId) is KeyAccessResult.Success)
            assertTrue(productionProbe.hasAnyAlias())
            val measurement = testKeys.securityInfo(testAliasId)
            assertTrue(measurement is KeyAccessResult.Success)
            val info = (measurement as KeyAccessResult.Success<VaultKeySecurityInfo>).value
            File(context.cacheDir, KEY_SECURITY_EVIDENCE).writeText(
                JSONObject()
                    .put("available", true)
                    .put("inside_secure_hardware", info.insideSecureHardware)
                    .put("security_level", info.securityLevel ?: JSONObject.NULL)
                    .put("api", android.os.Build.VERSION.SDK_INT)
                    .toString(),
            )
        } finally {
            testKeys.deleteRegisteredTestAlias(testAliasId)
            legacyKeys.deleteRegisteredTestAlias(legacyAliasId)
            files.cleanupRegisteredTestNamespace()
        }
    }

    private inner class Fixture(
        hook: ProvisioningHook = ProvisioningHook {},
        openingHook: OpeningHook = OpeningHook {},
    ) {
        private val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val keys = FakeVaultKeys()
        val controller = VaultController(
            context = context,
            files = files,
            keys = keys,
            provisioningHook = hook,
            openingHook = openingHook,
        )

        suspend fun provision() {
            controller.onForegrounded()
            awaitState { it is VaultState.Uninitialized }
            controller.beginInitialization()
            val auth = awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            controller.completeAuthentication(auth.request.id)
            awaitState { it is VaultState.Locked }
        }

        suspend fun unlock() {
            controller.onForegrounded()
            controller.beginUnlock()
            val auth = awaitState { it is VaultState.Authenticating } as VaultState.Authenticating
            controller.completeAuthentication(auth.request.id)
            awaitState { it is VaultState.Unlocked }
        }

        suspend fun awaitState(predicate: (VaultState) -> Boolean): VaultState = withTimeout(15_000) {
            controller.view.first { predicate(it.state) }.state
        }

        suspend fun cleanup() {
            keys.releaseBlockedUnwrap()
            controller.lock()
            runCatching { awaitState { it is VaultState.Locked || it is VaultState.Blocked || it is VaultState.Uninitialized } }
            keys.clearRegisteredTestAliases()
            files.cleanupRegisteredTestNamespace()
        }
    }

    private class FakeVaultKeys : VaultKeyOperations {
        private val aliases = mutableMapOf<String, SecretKey>()
        private val invalidated = mutableSetOf<String>()
        @Volatile private var unwrapGate: Pair<CountDownLatch, CountDownLatch>? = null
        @Volatile var nextUnwrapSize: Int? = null

        override fun hasAnyAlias(): Boolean = aliases.isNotEmpty()
        override fun contains(aliasId: String): Boolean = aliases.containsKey(aliasId)

        override fun generate(aliasId: String): KeyAccessResult<Unit> {
            if (aliases.containsKey(aliasId)) return KeyAccessResult.Failure(KeyAccessFailure.InvalidEnvelope)
            aliases[aliasId] = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
            return KeyAccessResult.Success(Unit)
        }

        override fun wrap(
            vaultUuid: String,
            aliasId: String,
            passphrase: ByteArray,
        ): KeyAccessResult<WrappedPassphrase> {
            val key = aliases[aliasId] ?: return KeyAccessResult.Failure(KeyAccessFailure.Missing)
            val cipher = Cipher.getInstance(VaultFiles.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(aad(vaultUuid))
            return KeyAccessResult.Success(
                WrappedPassphrase(
                    vaultUuid,
                    aliasId,
                    VaultFiles.ALGORITHM,
                    cipher.iv,
                    cipher.doFinal(passphrase),
                ),
            )
        }

        override fun unwrap(envelope: WrappedPassphrase): KeyAccessResult<ByteArray> {
            unwrapGate?.let { (started, release) ->
                started.countDown()
                release.await(10, TimeUnit.SECONDS)
                unwrapGate = null
            }
            if (envelope.aliasId in invalidated) {
                return KeyAccessResult.Failure(KeyAccessFailure.PermanentlyInvalidated)
            }
            val key = aliases[envelope.aliasId] ?: return KeyAccessResult.Failure(KeyAccessFailure.Missing)
            return try {
                val cipher = Cipher.getInstance(envelope.algorithm)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.iv))
                cipher.updateAAD(aad(envelope.vaultUuid))
                val unwrapped = cipher.doFinal(envelope.ciphertext)
                val requestedSize = nextUnwrapSize.also { nextUnwrapSize = null }
                KeyAccessResult.Success(requestedSize?.let { unwrapped.copyOf(it) } ?: unwrapped)
            } catch (_: AEADBadTagException) {
                KeyAccessResult.Failure(KeyAccessFailure.AuthenticationFailed)
            } catch (_: Throwable) {
                KeyAccessResult.Failure(KeyAccessFailure.InvalidEnvelope)
            }
        }

        override fun deleteRegisteredTestAlias(aliasId: String) {
            aliases.remove(aliasId)
            invalidated.remove(aliasId)
        }

        override fun deleteOrphanAliases() {
            aliases.clear()
            invalidated.clear()
        }

        override fun securityInfo(aliasId: String): KeyAccessResult<VaultKeySecurityInfo> =
            if (aliases.containsKey(aliasId)) {
                KeyAccessResult.Success(VaultKeySecurityInfo(false, "TEST_SOFTWARE"))
            } else {
                KeyAccessResult.Failure(KeyAccessFailure.Missing)
            }

        fun invalidate(aliasId: String) {
            check(aliases.containsKey(aliasId))
            invalidated += aliasId
        }

        fun blockNextUnwrap(): Pair<CountDownLatch, CountDownLatch> =
            (CountDownLatch(1) to CountDownLatch(1)).also { unwrapGate = it }

        fun releaseBlockedUnwrap() {
            unwrapGate?.second?.countDown()
        }

        fun clearRegisteredTestAliases() {
            releaseBlockedUnwrap()
            unwrapGate = null
            aliases.clear()
            invalidated.clear()
        }
        private fun aad(vaultUuid: String) = "anc-p2-envelope-v1:$vaultUuid".toByteArray()
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { "%02x".format(it) }

    private class InjectedInterruption : RuntimeException()

    private companion object {
        const val KEY_SECURITY_EVIDENCE = "p2-key-security-measurement.json"
    }
}
