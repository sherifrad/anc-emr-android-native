package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultAutoActionTest {
    @Test fun lockedBeginsUnlockWhenAppUnlocked() {
        assertEquals(VaultAutoAction.BEGIN_UNLOCK, vaultAutoAction(VaultState.Locked, appUnlocked = true))
    }

    @Test fun lockedDoesNothingWhileAppLocked() {
        assertEquals(VaultAutoAction.NONE, vaultAutoAction(VaultState.Locked, appUnlocked = false))
    }

    @Test fun uninitializedBeginsInitialization() {
        assertEquals(VaultAutoAction.BEGIN_INITIALIZATION, vaultAutoAction(VaultState.Uninitialized, appUnlocked = true))
    }

    @Test fun otherStatesDoNothing() {
        val states = listOf(
            VaultState.Inspecting,
            VaultState.Authenticating(AuthenticationRequest(1, AuthenticationPurpose.UNLOCK)),
            VaultState.Opening(AuthenticationPurpose.INITIALIZE),
            VaultState.Opening(AuthenticationPurpose.UNLOCK),
            VaultState.Unlocked(1),
            VaultState.Closing(),
            VaultState.Closing(busy = true),
        )
        listOf(true, false).forEach { appUnlocked ->
            states.forEach {
                assertEquals(it.toString(), VaultAutoAction.NONE, vaultAutoAction(it, appUnlocked))
            }
        }
    }

    @Test fun everyBlockedReasonDoesNothing() {
        VaultBlockReason.values().forEach {
            assertEquals(it.name, VaultAutoAction.NONE, vaultAutoAction(VaultState.Blocked(it), appUnlocked = true))
        }
    }
}
