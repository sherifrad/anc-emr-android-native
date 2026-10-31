package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultAutoActionTest {
    @Test fun lockedBeginsUnlock() {
        assertEquals(VaultAutoAction.BEGIN_UNLOCK, vaultAutoAction(VaultState.Locked))
    }

    @Test fun uninitializedBeginsInitialization() {
        assertEquals(VaultAutoAction.BEGIN_INITIALIZATION, vaultAutoAction(VaultState.Uninitialized))
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
        states.forEach { assertEquals(it.toString(), VaultAutoAction.NONE, vaultAutoAction(it)) }
    }

    @Test fun everyBlockedReasonDoesNothing() {
        VaultBlockReason.values().forEach {
            assertEquals(it.name, VaultAutoAction.NONE, vaultAutoAction(VaultState.Blocked(it)))
        }
    }
}
