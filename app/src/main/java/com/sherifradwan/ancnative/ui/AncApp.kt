package com.sherifradwan.ancnative.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.storage.VaultState

@Composable
fun AncApp(
    state: AncState,
    onEvent: (AncEvent) -> Unit,
    onStartVisit: (String, String?) -> Unit,
    onSaveVisit: () -> Unit,
    onInitialize: () -> Unit,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onRetryStorage: () -> Unit,
    securityRepo: SecuritySettingsRepository? = null,
) {
    val unlocked = state.vaultState is VaultState.Unlocked
    val securitySettings = securityRepo?.settings?.collectAsState()?.value
    val isAppLocked = securitySettings?.isAppLocked ?: false
    var showSettingsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = unlocked && !isAppLocked && (state.route != AncRoute.PatientList || state.showDiscardDialog)) {
        onEvent(if (state.showDiscardDialog) AncEvent.KeepEditing else AncEvent.Back)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (!unlocked) {
                if (state.vaultState is VaultState.Inspecting || state.vaultState is VaultState.Opening || state.vaultState is VaultState.Closing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("vaultGate"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.clinic_logo_foreground),
                                contentDescription = "Clinic Logo",
                                modifier = Modifier.size(80.dp),
                            )
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    VaultGateScreen(
                        state = state.vaultState,
                        onInitialize = onInitialize,
                        onUnlock = onUnlock,
                        onRetry = onRetryStorage,
                    )
                }
            } else if (isAppLocked && securityRepo != null) {
                AppLockScreen(
                    securityRepo = securityRepo,
                    onUnlockSuccess = { securityRepo.unlockApp() },
                )
            } else {
                if (state.route == AncRoute.PatientList) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.clinic_logo_foreground),
                                contentDescription = "Clinic Logo",
                                modifier = Modifier.size(36.dp),
                            )
                            Column {
                                Text(
                                    text = securitySettings?.clinicName ?: "Dr. Sherif Radwan Clinic",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Maternal-Fetal Medicine & Obstetrics",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (securityRepo != null) {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.minimumTouchTarget().testTag("openSecuritySettings"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Security Settings",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                when (val route = state.route) {
                    AncRoute.PatientList -> PatientListScreen(state, onEvent)
                    is AncRoute.Workspace -> WorkspaceScreen(
                        state = state,
                        route = route,
                        onEvent = onEvent,
                        onStartVisit = onStartVisit,
                    )
                    is AncRoute.Profile -> ProfileScreen(state, route, onEvent)
                    is AncRoute.VisitEditor -> VisitEditor(
                        state = state,
                        route = route,
                        onEvent = onEvent,
                        onSave = onSaveVisit,
                    )
                    is AncRoute.PatientEditor -> PatientEditor(state, route, onEvent)
                    is AncRoute.OtherHistoriesEditor -> OtherHistoriesEditor(state, route, onEvent)
                    is AncRoute.PreviousPregnancyEditor -> PreviousPregnancyEditor(state, route, onEvent)
                    is AncRoute.StatusEditor -> PatientStatusEditor(state, route, onEvent)
                    is AncRoute.DatingEditor -> DatingEditor(state, route, onEvent)
                    is AncRoute.UltrasoundEditor -> UltrasoundEditor(state, route, onEvent)
                    is AncRoute.LabEditor -> LabEditor(state, route, onEvent)
                    is AncRoute.MedicationEditor -> MedicationEditor(state, route, onEvent)
                }
            }
        }
    }

    if (securityRepo != null && showSettingsDialog) {
        SecuritySettingsDialog(
            securityRepo = securityRepo,
            onDismiss = { showSettingsDialog = false },
            onLockNow = {
                showSettingsDialog = false
                securityRepo.lockApp()
                onLock()
            },
        )
    }

    if (unlocked && !isAppLocked && state.showNewPatientInfo) {
        AlertDialog(
            onDismissRequest = { onEvent(AncEvent.HideNewPatientInfo) },
            title = {
                Text(stringResource(R.string.new_patient_title))
            },
            text = { Text(stringResource(R.string.new_patient_body)) },
            confirmButton = {
                Button(onClick = { onEvent(AncEvent.HideNewPatientInfo) }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (unlocked && !isAppLocked && state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(AncEvent.KeepEditing) },
            title = {
                Text(stringResource(R.string.discard_changes))
            },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                Button(
                    onClick = { onEvent(AncEvent.ConfirmDiscard) },
                    modifier = Modifier.testTag("discardDialog"),
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(AncEvent.KeepEditing) },
                    modifier = Modifier.testTag("keepEditingDialog"),
                ) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
    }
}
