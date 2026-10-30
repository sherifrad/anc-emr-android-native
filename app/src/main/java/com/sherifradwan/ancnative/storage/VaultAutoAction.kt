package com.sherifradwan.ancnative.storage

internal enum class VaultAutoAction { BEGIN_UNLOCK, BEGIN_INITIALIZATION, NONE }

internal fun vaultAutoAction(state: VaultState, appUnlocked: Boolean): VaultAutoAction = when (state) {
    VaultState.Locked -> if (appUnlocked) VaultAutoAction.BEGIN_UNLOCK else VaultAutoAction.NONE
    VaultState.Uninitialized -> VaultAutoAction.BEGIN_INITIALIZATION
    else -> VaultAutoAction.NONE
}
