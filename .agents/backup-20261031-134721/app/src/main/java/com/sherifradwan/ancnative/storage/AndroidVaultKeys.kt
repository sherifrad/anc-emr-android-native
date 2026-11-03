package com.sherifradwan.ancnative.storage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

internal sealed interface KeyAccessFailure {
    data object Missing : KeyAccessFailure
    data object PermanentlyInvalidated : KeyAccessFailure
    data object AuthenticationRequired : KeyAccessFailure
    data object AuthenticationFailed : KeyAccessFailure
    data object InvalidEnvelope : KeyAccessFailure
}

internal sealed interface KeyAccessResult<out T> {
    data class Success<T>(val value: T) : KeyAccessResult<T>
    data class Failure(val reason: KeyAccessFailure) : KeyAccessResult<Nothing>
}

internal data class VaultKeySecurityInfo(
    val insideSecureHardware: Boolean,
    val securityLevel: String?,
)

internal interface VaultKeyOperations {
    fun hasAnyAlias(): Boolean
    fun contains(aliasId: String): Boolean
    fun generate(aliasId: String): KeyAccessResult<Unit>
    fun wrap(vaultUuid: String, aliasId: String, passphrase: ByteArray): KeyAccessResult<WrappedPassphrase>
    fun unwrap(envelope: WrappedPassphrase): KeyAccessResult<ByteArray>
    fun securityInfo(aliasId: String): KeyAccessResult<VaultKeySecurityInfo>
    fun deleteRegisteredTestAlias(aliasId: String)
    fun deleteOrphanAliases()
}

internal class AndroidVaultKeys(
    private val aliasPrefix: String,
    private val testCleanupAllowed: Boolean = false,
    private val orphanDetectionPrefixes: Set<String> = emptySet(),
) : VaultKeyOperations {
    private val keyStore: KeyStore
        get() = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    override fun hasAnyAlias(): Boolean {
        val aliases = keyStore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            if (matchesOwnedAlias(alias, aliasPrefix)) return true
            if (orphanDetectionPrefixes.any { prefix -> matchesOwnedAlias(alias, prefix) }) return true
        }
        return false
    }

    override fun contains(aliasId: String): Boolean = keyStore.containsAlias(fullAlias(aliasId))

    override fun generate(aliasId: String): KeyAccessResult<Unit> {
        return try {
            val alias = fullAlias(aliasId)
            if (keyStore.containsAlias(alias)) {
                KeyAccessResult.Failure(KeyAccessFailure.InvalidEnvelope)
            } else {
                val builder = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
                    init(builder.build())
                    generateKey()
                }
                KeyAccessResult.Success(Unit)
            }
        } catch (_: Throwable) {
            KeyAccessResult.Failure(KeyAccessFailure.InvalidEnvelope)
        }
    }

    override fun wrap(
        vaultUuid: String,
        aliasId: String,
        passphrase: ByteArray,
    ): KeyAccessResult<WrappedPassphrase> = cryptResult {
        val cipher = Cipher.getInstance(VaultFiles.ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey(aliasId))
        cipher.updateAAD(aad(vaultUuid))
        WrappedPassphrase(
            vaultUuid = vaultUuid,
            aliasId = aliasId,
            algorithm = VaultFiles.ALGORITHM,
            iv = cipher.iv.copyOf(),
            ciphertext = cipher.doFinal(passphrase),
        )
    }

    override fun unwrap(envelope: WrappedPassphrase): KeyAccessResult<ByteArray> = cryptResult {
        require(envelope.algorithm == VaultFiles.ALGORITHM)
        val cipher = Cipher.getInstance(envelope.algorithm)
        cipher.init(
            Cipher.DECRYPT_MODE,
            existingKey(envelope.aliasId),
            GCMParameterSpec(GCM_TAG_BITS, envelope.iv),
        )
        cipher.updateAAD(aad(envelope.vaultUuid))
        cipher.doFinal(envelope.ciphertext).also { require(it.size == PASSPHRASE_BYTES) }
    }

    @Suppress("DEPRECATION")
    override fun securityInfo(aliasId: String): KeyAccessResult<VaultKeySecurityInfo> = cryptResult {
        val key = existingKey(aliasId)
        val keyInfo = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEY_STORE)
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        VaultKeySecurityInfo(
            insideSecureHardware = keyInfo.isInsideSecureHardware,
            securityLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
                    KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "UNKNOWN_SECURE"
                    else -> "UNKNOWN"
                }
            } else {
                null
            },
        )
    }

    override fun deleteRegisteredTestAlias(aliasId: String) {
        check(testCleanupAllowed) { "canonical-key-cleanup-forbidden" }
        val alias = fullAlias(aliasId)
        require(alias.startsWith(aliasPrefix))
        keyStore.deleteEntry(alias)
    }

    override fun deleteOrphanAliases() {
        try {
            val aliases = keyStore.aliases()
            val toDelete = mutableListOf<String>()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (matchesOwnedAlias(alias, aliasPrefix) || orphanDetectionPrefixes.any { matchesOwnedAlias(alias, it) }) {
                    toDelete.add(alias)
                }
            }
            toDelete.forEach { keyStore.deleteEntry(it) }
        } catch (_: Throwable) {
        }
    }

    private fun existingKey(aliasId: String): SecretKey {
        val alias = fullAlias(aliasId)
        return (keyStore.getKey(alias, null) as? SecretKey) ?: throw MissingKeyException()
    }

    private inline fun <T> cryptResult(block: () -> T): KeyAccessResult<T> = try {
        KeyAccessResult.Success(block())
    } catch (_: MissingKeyException) {
        KeyAccessResult.Failure(KeyAccessFailure.Missing)
    } catch (_: KeyPermanentlyInvalidatedException) {
        KeyAccessResult.Failure(KeyAccessFailure.PermanentlyInvalidated)
    } catch (_: android.security.keystore.UserNotAuthenticatedException) {
        KeyAccessResult.Failure(KeyAccessFailure.AuthenticationRequired)
    } catch (_: AEADBadTagException) {
        KeyAccessResult.Failure(KeyAccessFailure.AuthenticationFailed)
    } catch (_: Throwable) {
        KeyAccessResult.Failure(KeyAccessFailure.InvalidEnvelope)
    }

    private fun fullAlias(aliasId: String): String {
        require(aliasId.matches(UUID_PATTERN))
        return aliasPrefix + aliasId
    }

    private fun matchesOwnedAlias(alias: String, prefix: String): Boolean =
        alias.startsWith(prefix) && alias.removePrefix(prefix).matches(UUID_PATTERN)

    private fun aad(vaultUuid: String): ByteArray =
        "anc-p2-envelope-v1:$vaultUuid".toByteArray(Charsets.UTF_8)

    private class MissingKeyException : Exception()

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val AUTHORIZATION_SECONDS = 30
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
        val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
