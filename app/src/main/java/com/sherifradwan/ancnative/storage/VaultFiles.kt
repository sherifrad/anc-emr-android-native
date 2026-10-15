package com.sherifradwan.ancnative.storage

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

internal data class VaultDescriptor(
    val vaultUuid: String,
    val aliasId: String,
)

internal data class WrappedPassphrase(
    val vaultUuid: String,
    val aliasId: String,
    val algorithm: String,
    val iv: ByteArray,
    val ciphertext: ByteArray,
)

internal sealed interface VaultDiskState {
    data object Empty : VaultDiskState
    data object Incomplete : VaultDiskState
    data class Active(val descriptor: VaultDescriptor) : VaultDiskState
    data object Invalid : VaultDiskState
}

internal enum class VaultArtifactState {
    MISSING,
    PRESENT,
    INVALID,
}

internal class VaultFiles private constructor(
    val namespaceRoot: File,
    val aliasPrefix: String,
    private val cleanupAllowed: Boolean,
) {
    val activePointerFile: File get() = File(namespaceRoot, ACTIVE_POINTER)
    fun vaultDirectory(vaultUuid: String): File = File(namespaceRoot, "vault-$vaultUuid")
    fun descriptorFile(vaultUuid: String): File = File(vaultDirectory(vaultUuid), DESCRIPTOR)
    fun envelopeFile(vaultUuid: String): File = File(vaultDirectory(vaultUuid), ENVELOPE)
    fun databaseFile(vaultUuid: String): File = File(vaultDirectory(vaultUuid), DATABASE)

    fun inspect(): VaultDiskState {
        val pointerArtifacts = atomicArtifacts(activePointerFile)
        if (pointerArtifacts.any(File::exists)) return VaultDiskState.Incomplete
        if (!activePointerFile.exists()) {
            return if (namespaceRoot.listFiles().isNullOrEmpty()) {
                VaultDiskState.Empty
            } else {
                VaultDiskState.Incomplete
            }
        }
        return try {
            val vaultUuid = readBounded(activePointerFile, MAX_POINTER_BYTES)
                .toString(StandardCharsets.UTF_8)
                .trim()
                .canonicalUuid()
            val descriptor = readDescriptor(vaultUuid)
            if (descriptor.vaultUuid != vaultUuid) VaultDiskState.Invalid else VaultDiskState.Active(descriptor)
        } catch (_: Throwable) {
            VaultDiskState.Invalid
        }
    }

    fun prepareDescriptor(descriptor: VaultDescriptor) {
        descriptor.vaultUuid.canonicalUuid()
        descriptor.aliasId.canonicalUuid()
        check(namespaceRoot.mkdirs() || namespaceRoot.isDirectory)
        val vaultDirectory = vaultDirectory(descriptor.vaultUuid)
        check(vaultDirectory.mkdir()) { "vault-directory-exists" }
        writeAtomic(descriptorFile(descriptor.vaultUuid), encodeDescriptor(descriptor))
    }

    fun readDescriptor(vaultUuid: String): VaultDescriptor {
        vaultUuid.canonicalUuid()
        val bytes = readBounded(descriptorFile(vaultUuid), MAX_DESCRIPTOR_BYTES)
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == DESCRIPTOR_MAGIC)
            require(input.readInt() == FORMAT_VERSION)
            val storedVaultUuid = input.readUTF().canonicalUuid()
            val aliasId = input.readUTF().canonicalUuid()
            require(input.available() == 0)
            return VaultDescriptor(storedVaultUuid, aliasId)
        }
    }

    fun writeEnvelope(envelope: WrappedPassphrase) {
        require(envelope.iv.size in 12..32)
        require(envelope.ciphertext.size in 48..256)
        writeAtomic(envelopeFile(envelope.vaultUuid), encodeEnvelope(envelope))
    }

    fun readEnvelope(vaultUuid: String): WrappedPassphrase {
        val bytes = readBounded(envelopeFile(vaultUuid), MAX_ENVELOPE_BYTES)
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == ENVELOPE_MAGIC)
            require(input.readInt() == FORMAT_VERSION)
            val storedVaultUuid = input.readUTF().canonicalUuid()
            val aliasId = input.readUTF().canonicalUuid()
            val algorithm = input.readUTF()
            require(algorithm == ALGORITHM)
            val ivLength = input.readInt()
            require(ivLength in 12..32)
            val iv = ByteArray(ivLength).also(input::readFully)
            val ciphertextLength = input.readInt()
            require(ciphertextLength in 48..256)
            val ciphertext = ByteArray(ciphertextLength).also(input::readFully)
            require(input.available() == 0)
            return WrappedPassphrase(storedVaultUuid, aliasId, algorithm, iv, ciphertext)
        }
    }

    fun publishActive(vaultUuid: String) {
        val canonical = vaultUuid.canonicalUuid()
        writeAtomic(activePointerFile, canonical.toByteArray(StandardCharsets.UTF_8))
    }

    fun envelopeState(vaultUuid: String): VaultArtifactState {
        val file = envelopeFile(vaultUuid)
        if (atomicArtifacts(file).any(File::exists)) return VaultArtifactState.INVALID
        if (!file.exists()) return VaultArtifactState.MISSING
        if (!file.isFile || !file.canRead()) return VaultArtifactState.INVALID
        return try {
            readEnvelope(vaultUuid)
            VaultArtifactState.PRESENT
        } catch (_: Throwable) {
            VaultArtifactState.INVALID
        }
    }

    fun databaseState(vaultUuid: String): VaultArtifactState {
        val file = databaseFile(vaultUuid)
        if (!file.exists()) return VaultArtifactState.MISSING
        if (!file.isFile || !file.canRead()) return VaultArtifactState.INVALID
        return try {
            if (file.length() >= MIN_DATABASE_BYTES) VaultArtifactState.PRESENT else VaultArtifactState.INVALID
        } catch (_: Throwable) {
            VaultArtifactState.INVALID
        }
    }

    fun hasEnvelope(vaultUuid: String): Boolean = envelopeState(vaultUuid) == VaultArtifactState.PRESENT
    fun hasDatabase(vaultUuid: String): Boolean = databaseState(vaultUuid) == VaultArtifactState.PRESENT
    fun hasIncompletePointerArtifact(): Boolean = atomicArtifacts(activePointerFile).any(File::exists)

    fun cleanupUnpublishedArtifacts() {
        if (!activePointerFile.exists() && namespaceRoot.exists()) {
            namespaceRoot.listFiles()?.forEach { target ->
                target.deleteRecursively()
            }
        }
    }

    fun cleanupRegisteredTestNamespace() {
        check(cleanupAllowed) { "canonical-vault-cleanup-forbidden" }
        val canonicalRoot = namespaceRoot.canonicalFile
        require(canonicalRoot.name.startsWith(TEST_NAMESPACE_PREFIX))
        if (!canonicalRoot.exists()) return
        canonicalRoot.walkBottomUp().forEach { target ->
            require(target.canonicalFile.path.startsWith(canonicalRoot.path))
            check(target.delete()) { "test-namespace-cleanup-failed" }
        }
    }

    private fun encodeDescriptor(descriptor: VaultDescriptor): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(DESCRIPTOR_MAGIC)
            output.writeInt(FORMAT_VERSION)
            output.writeUTF(descriptor.vaultUuid)
            output.writeUTF(descriptor.aliasId)
        }
        bytes.toByteArray()
    }

    private fun encodeEnvelope(envelope: WrappedPassphrase): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(ENVELOPE_MAGIC)
            output.writeInt(FORMAT_VERSION)
            output.writeUTF(envelope.vaultUuid)
            output.writeUTF(envelope.aliasId)
            output.writeUTF(envelope.algorithm)
            output.writeInt(envelope.iv.size)
            output.write(envelope.iv)
            output.writeInt(envelope.ciphertext.size)
            output.write(envelope.ciphertext)
        }
        bytes.toByteArray()
    }

    private fun writeAtomic(file: File, bytes: ByteArray) {
        require(bytes.size <= MAX_ATOMIC_BYTES)
        check(file.parentFile?.mkdirs() != false || file.parentFile?.isDirectory == true)
        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(bytes)
            stream.fd.sync()
            atomicFile.finishWrite(stream)
        } catch (failure: Throwable) {
            stream?.let(atomicFile::failWrite)
            throw failure
        }
    }

    private fun readBounded(file: File, maximum: Int): ByteArray {
        require(file.isFile)
        require(file.length() in 1..maximum.toLong())
        return file.inputStream().use { input ->
            val output = ByteArrayOutputStream(minOf(maximum, 1024))
            val buffer = ByteArray(512)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maximum)
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    private fun atomicArtifacts(file: File): List<File> = listOf(
        File(file.path + ".bak"),
        File(file.path + ".new"),
    )

    private fun String.canonicalUuid(): String {
        val parsed = UUID.fromString(this)
        require(parsed.toString() == this.lowercase())
        return parsed.toString()
    }

    companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        private const val FORMAT_VERSION = 1
        private const val ACTIVE_POINTER = "active-vault.ptr"
        private const val DESCRIPTOR = "descriptor.bin"
        private const val ENVELOPE = "passphrase-envelope.bin"
        private const val DATABASE = "records.db"
        private const val DESCRIPTOR_MAGIC = 0x414E4344
        private const val ENVELOPE_MAGIC = 0x414E4345
        private const val MAX_POINTER_BYTES = 64
        private const val MAX_DESCRIPTOR_BYTES = 1024
        private const val MAX_ENVELOPE_BYTES = 4096
        private const val MAX_ATOMIC_BYTES = 4096
        private const val MIN_DATABASE_BYTES = 512L
        private const val TEST_NAMESPACE_PREFIX = "test-"
        internal const val CANONICAL_ALIAS_PREFIX = "com.sherifradwan.ancnative.p2.prod."
        internal const val TEST_ALIAS_PREFIX = "com.sherifradwan.ancnative.p2.test."
        internal const val LEGACY_CANONICAL_ALIAS_PREFIX = "com.sherifradwan.ancnative.p2."

        fun canonical(context: Context): VaultFiles = VaultFiles(
            namespaceRoot = File(context.noBackupFilesDir, "p2-vault/canonical"),
            aliasPrefix = CANONICAL_ALIAS_PREFIX,
            cleanupAllowed = false,
        )

        internal fun isolatedTest(context: Context, namespaceUuid: String): VaultFiles {
            val uuid = UUID.fromString(namespaceUuid).toString()
            return VaultFiles(
                namespaceRoot = File(context.noBackupFilesDir, "p2-vault-tests/$TEST_NAMESPACE_PREFIX$uuid"),
                aliasPrefix = "$TEST_ALIAS_PREFIX$uuid.",
                cleanupAllowed = true,
            )
        }
    }
}
