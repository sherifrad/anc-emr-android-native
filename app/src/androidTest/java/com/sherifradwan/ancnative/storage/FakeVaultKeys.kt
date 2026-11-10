package com.sherifradwan.ancnative.storage

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** In-memory vault keys for instrumented tests: software AES in place of the Android keystore. Shared by the vault tests. */
internal class FakeVaultKeys : VaultKeyOperations {
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
