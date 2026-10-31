package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultStateTest {
    @Test
    fun epochGateRejectsOldAuthenticationReadAndSaveCallbacks() {
        val gate = EpochGate()
        val first = gate.advance()
        assertTrue(gate.accepts(first))
        val locked = gate.advance()
        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(locked))
    }

    @Test
    fun blockedReasonsRemainTypedAndDoNotContainPatientData() {
        val required = setOf(
            VaultBlockReason.INCOMPLETE_SETUP,
            VaultBlockReason.KEY_MISSING,
            VaultBlockReason.KEY_PERMANENTLY_INVALIDATED,
            VaultBlockReason.GCM_AUTHENTICATION_FAILED,
            VaultBlockReason.DATABASE_MISSING,
            VaultBlockReason.VAULT_METADATA_MISSING,
            VaultBlockReason.VAULT_METADATA_MISMATCH,
            VaultBlockReason.UNKNOWN_SCHEMA,
        )
        required.forEach { reason ->
            val view = VaultSessionView(VaultState.Blocked(reason))
            assertTrue(view.snapshot == null)
        }
    }
}
