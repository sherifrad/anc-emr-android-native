package com.sherifradwan.ancnative.storage

enum class AuthenticationPurpose {
    INITIALIZE,
    UNLOCK,
}

data class AuthenticationRequest(
    val id: Long,
    val purpose: AuthenticationPurpose,
)

enum class VaultBlockReason {
    DEVICE_CREDENTIAL_UNAVAILABLE,
    INCOMPLETE_SETUP,
    POINTER_MISSING,
    DESCRIPTOR_INVALID,
    ENVELOPE_MISSING,
    ENVELOPE_INVALID,
    DATABASE_MISSING,
    KEY_MISSING,
    KEY_PERMANENTLY_INVALIDATED,
    GCM_AUTHENTICATION_FAILED,
    VAULT_METADATA_MISSING,
    VAULT_METADATA_MISMATCH,
    UNKNOWN_SCHEMA,
    DATABASE_INTEGRITY_FAILED,
    DATABASE_OPEN_FAILED,
    STORAGE_BUSY,
}

sealed interface VaultState {
    data object Inspecting : VaultState
    data object Uninitialized : VaultState
    data class Authenticating(val request: AuthenticationRequest) : VaultState
    data object Locked : VaultState
    data class Opening(val purpose: AuthenticationPurpose) : VaultState
    data class Unlocked(val sessionEpoch: Long) : VaultState
    data class Closing(val busy: Boolean = false) : VaultState
    data class Blocked(val reason: VaultBlockReason) : VaultState
}

data class VaultSessionView(
    val state: VaultState = VaultState.Inspecting,
    val snapshot: RecordSnapshot? = null,
)

internal class EpochGate {
    private var epoch: Long = 0

    @Synchronized
    fun current(): Long = epoch

    @Synchronized
    fun advance(): Long = ++epoch

    @Synchronized
    fun accepts(candidate: Long): Boolean = candidate == epoch
}
