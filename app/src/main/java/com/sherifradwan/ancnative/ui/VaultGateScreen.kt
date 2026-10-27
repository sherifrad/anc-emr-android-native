package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.AuthenticationPurpose
import com.sherifradwan.ancnative.storage.VaultBlockReason
import com.sherifradwan.ancnative.storage.VaultState

@Composable
fun VaultGateScreen(
    state: VaultState,
    onInitialize: () -> Unit,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("vaultGate")) {
        val horizontalPadding = maxOf(24.dp, (maxWidth - 520.dp) / 2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.clinic_logo_foreground),
                contentDescription = "Clinic Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dr. Sherif Radwan Clinic",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )

            Text(
                text = "Maternal-Fetal Medicine & Obstetrics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = when (state) {
                            VaultState.Locked -> Icons.Default.Lock
                            is VaultState.Authenticating -> Icons.Default.Fingerprint
                            VaultState.Uninitialized -> Icons.Default.Security
                            is VaultState.Blocked -> Icons.Default.Refresh
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(state.titleResource()),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(state.bodyResource()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (state) {
                VaultState.Uninitialized -> Button(
                    onClick = onInitialize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("initializeStorage"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.initialize_demo_storage), fontWeight = FontWeight.SemiBold)
                }
                VaultState.Locked -> Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("unlockStorage"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.unlock_demo_storage), fontWeight = FontWeight.SemiBold)
                }
                is VaultState.Blocked -> Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("retryStorage"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.retry_storage_check), fontWeight = FontWeight.SemiBold)
                }
                else -> Unit
            }
        }
    }
}

@StringRes
private fun VaultState.titleResource(): Int = when (this) {
    VaultState.Inspecting -> R.string.storage_checking_title
    VaultState.Uninitialized -> R.string.storage_uninitialized_title
    is VaultState.Authenticating -> R.string.storage_authenticating_title
    VaultState.Locked -> R.string.storage_locked_title
    is VaultState.Opening -> if (purpose == AuthenticationPurpose.INITIALIZE) {
        R.string.storage_initializing_title
    } else {
        R.string.storage_opening_title
    }
    is VaultState.Unlocked -> R.string.storage_unlocked_title
    is VaultState.Closing -> R.string.storage_closing_title
    is VaultState.Blocked -> R.string.storage_blocked_title
}

@StringRes
private fun VaultState.bodyResource(): Int = when (this) {
    VaultState.Inspecting -> R.string.storage_checking_body
    VaultState.Uninitialized -> R.string.storage_uninitialized_body
    is VaultState.Authenticating -> R.string.storage_authenticating_body
    VaultState.Locked -> R.string.storage_locked_body
    is VaultState.Opening -> if (purpose == AuthenticationPurpose.INITIALIZE) {
        R.string.storage_initializing_body
    } else {
        R.string.storage_opening_body
    }
    is VaultState.Unlocked -> R.string.storage_unlocked_body
    is VaultState.Closing -> if (busy) R.string.storage_closing_busy_body else R.string.storage_closing_body
    is VaultState.Blocked -> reason.bodyResource()
}

@StringRes
private fun VaultBlockReason.bodyResource(): Int = when (this) {
    VaultBlockReason.DEVICE_CREDENTIAL_UNAVAILABLE -> R.string.blocked_device_credential
    VaultBlockReason.INCOMPLETE_SETUP -> R.string.blocked_incomplete_setup
    VaultBlockReason.POINTER_MISSING -> R.string.blocked_pointer_missing
    VaultBlockReason.DESCRIPTOR_INVALID -> R.string.blocked_descriptor_invalid
    VaultBlockReason.ENVELOPE_MISSING -> R.string.blocked_envelope_missing
    VaultBlockReason.ENVELOPE_INVALID -> R.string.blocked_envelope_invalid
    VaultBlockReason.DATABASE_MISSING -> R.string.blocked_database_missing
    VaultBlockReason.KEY_MISSING -> R.string.blocked_key_missing
    VaultBlockReason.KEY_PERMANENTLY_INVALIDATED -> R.string.blocked_key_invalidated
    VaultBlockReason.GCM_AUTHENTICATION_FAILED -> R.string.blocked_gcm_failure
    VaultBlockReason.VAULT_METADATA_MISSING -> R.string.blocked_metadata_missing
    VaultBlockReason.VAULT_METADATA_MISMATCH -> R.string.blocked_metadata_mismatch
    VaultBlockReason.UNKNOWN_SCHEMA -> R.string.blocked_unknown_schema
    VaultBlockReason.DATABASE_INTEGRITY_FAILED -> R.string.blocked_integrity
    VaultBlockReason.DATABASE_OPEN_FAILED -> R.string.blocked_database_open
    VaultBlockReason.STORAGE_BUSY -> R.string.blocked_storage_busy
}
