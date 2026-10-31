package com.sherifradwan.ancnative.storage

internal enum class VaultAutoAction { BEGIN_UNLOCK, BEGIN_INITIALIZATION, NONE }

internal fun vaultAutoAction(state: VaultState): VaultAutoAction = when (state) {
    VaultState.Locked -> VaultAutoAction.BEGIN_UNLOCK
    VaultState.Uninitialized -> VaultAutoAction.BEGIN_INITIALIZATION
    else -> VaultAutoAction.NONE
}
