package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.AuthenticationPurpose
import com.sherifradwan.ancnative.storage.VaultBlockReason
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusTone

@Composable
fun VaultGateScreen(
    state: VaultState,
    onInitialize: () -> Unit,
    onUnlock: () -> Unit,
    onRetry: () -> Unit,
    clinicName: String? = null,
    clinicSubtitle: String? = null,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("vaultGate"),
    ) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 480.dp) / 2)
        val isBlocked = state is VaultState.Blocked
        val dark = isDarkTheme()
        val patientFamily = FieldFamilies.patient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.clinic_logo_foreground),
                        contentDescription = stringResource(R.string.clinic_logo_cd),
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = clinicName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name),
                style = RecordType.HeaderName,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = clinicSubtitle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.clinic_default_subtitle),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedCard(
                shape = RecordShapes.Large,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isBlocked) {
                        if (dark) StatusHighDarkBorder else StatusHighBorder
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vaultStatusCard"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isBlocked) {
                                if (dark) StatusHighDarkBg else StatusHighBg
                            } else {
                                patientFamily.container
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    BandChip(
                        text = stringResource(state.statusChipResource()),
                        tone = if (isBlocked) StatusTone.High else null,
                        family = patientFamily,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(state.titleResource()),
                        style = RecordType.SectionTitle,
                        color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(state.bodyResource()),
                        style = RecordType.FieldValue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                VaultState.Uninitialized -> VaultGateActionButton(
                    label = stringResource(R.string.initialize_demo_storage),
                    tag = "initializeStorage",
                    onClick = onInitialize,
                )
                VaultState.Locked -> VaultGateActionButton(
                    label = stringResource(R.string.unlock_demo_storage),
                    tag = "unlockStorage",
                    onClick = onUnlock,
                )
                is VaultState.Blocked -> VaultGateActionButton(
                    label = stringResource(R.string.retry_storage_check),
                    tag = "retryStorage",
                    onClick = onRetry,
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun VaultGateActionButton(
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RecordShapes.Small,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .minimumTouchTarget()
            .testTag(tag),
    ) {
        Text(
            text = label,
            style = RecordType.Action,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@StringRes
private fun VaultState.statusChipResource(): Int = when (this) {
    VaultState.Inspecting -> R.string.vault_status_checking
    VaultState.Uninitialized -> R.string.vault_status_setup_needed
    is VaultState.Authenticating -> if (request.purpose == AuthenticationPurpose.INITIALIZE) {
        R.string.vault_status_setup_needed
    } else {
        R.string.vault_status_locked
    }
    VaultState.Locked -> R.string.vault_status_locked
    is VaultState.Opening -> R.string.vault_status_opening
    is VaultState.Unlocked -> R.string.vault_status_unlocked
    is VaultState.Closing -> R.string.vault_status_closing
    is VaultState.Blocked -> R.string.vault_status_blocked
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
