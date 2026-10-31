package com.sherifradwan.ancnative.ui

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import com.sherifradwan.ancnative.storage.BackupInspectionResult
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.ui.graphics.Color
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.security.AppThemeMode
import com.sherifradwan.ancnative.security.AutoLockTimeout
import com.sherifradwan.ancnative.security.BiometricAuthManager
import com.sherifradwan.ancnative.security.SecuritySettingsRepository

enum class SettingsTab {
    SECURITY,
    LAB_CODEX,
    MED_CODEX,
}

@Composable
fun SecuritySettingsDialog(
    securityRepo: SecuritySettingsRepository,
    onDismiss: () -> Unit,
    onLockNow: () -> Unit,
    onExportBackup: ((CharArray, (ByteArray?) -> Unit) -> Unit)? = null,
    onRestoreBackup: ((ByteArray, CharArray, (BackupInspectionResult) -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var settingsState by remember { mutableStateOf(securityRepo.settings.value) }
    var editedClinicName by remember(settingsState.clinicName) { mutableStateOf(settingsState.clinicName) }
    var editedClinicSubtitle by remember(settingsState.clinicSubtitle) { mutableStateOf(settingsState.clinicSubtitle) }
    var brandingSavedMessage by remember { mutableStateOf<String?>(null) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var biometricFeedbackMessage by remember { mutableStateOf<String?>(null) }

    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isBackupError by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showRestorePassphraseDialog by remember { mutableStateOf(false) }

    val restoreFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
            if (bytes != null && bytes.isNotEmpty()) {
                pendingRestoreBytes = bytes
                showRestorePassphraseDialog = true
            } else {
                backupStatusMessage = "Unable to read selected backup file!"
                isBackupError = true
            }
        }
    }

    val isBiometricAvailable = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("securitySettingsDialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header: Clinic Emblem + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(46.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.clinic_logo_foreground),
                                    contentDescription = "Clinic Logo",
                                    modifier = Modifier.size(42.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Clinic & System Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Clinic Settings, Codex & Security",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("closeSettingsButton"),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                var selectedTab by remember { mutableStateOf(SettingsTab.SECURITY) }
                val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }

                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    Tab(
                        selected = selectedTab == SettingsTab.SECURITY,
                        onClick = { selectedTab = SettingsTab.SECURITY },
                        text = {
                            Text(
                                "Security & Clinic",
                                fontWeight = if (selectedTab == SettingsTab.SECURITY) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == SettingsTab.SECURITY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.LAB_CODEX,
                        onClick = { selectedTab = SettingsTab.LAB_CODEX },
                        text = {
                            Text(
                                "Lab Codex",
                                fontWeight = if (selectedTab == SettingsTab.LAB_CODEX) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == SettingsTab.LAB_CODEX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.MED_CODEX,
                        onClick = { selectedTab = SettingsTab.MED_CODEX },
                        text = {
                            Text(
                                "Med Codex",
                                fontWeight = if (selectedTab == SettingsTab.MED_CODEX) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == SettingsTab.MED_CODEX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (selectedTab == SettingsTab.LAB_CODEX) {
                    LabCodexSettingsSection(codexDelegate = codexRepo)
                } else if (selectedTab == SettingsTab.MED_CODEX) {
                    MedicationCodexSettingsSection(codexDelegate = codexRepo)
                } else {
                    // Section: Theme Mode (Light / Dark / System Default)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().testTag("themeSettingsCard"),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.BrightnessAuto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "App Theme & Appearance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Text(
                                text = "Choose color scheme for your clinical environment (Bright Clinic or Dimmed Ultrasound Suite):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = settingsState.themeMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            securityRepo.setThemeMode(mode)
                                            settingsState = securityRepo.settings.value
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                        .testTag("themeOption-${mode.name}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            securityRepo.setThemeMode(mode)
                                            settingsState = securityRepo.settings.value
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = mode.displayNameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 0: Clinic Branding & Visual Identity
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Clinic Branding & Visual Identity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Logo preview & details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(R.drawable.clinic_logo_foreground),
                                        contentDescription = "Clinic Logo",
                                        modifier = Modifier.size(50.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clinic Official Emblem",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Consistent with application icon and splash screen with ivory background #FBF8F5 without clipping.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editedClinicName,
                            onValueChange = { editedClinicName = it },
                            label = { Text("Clinic Name (Header & Top Bar)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("clinicNameField"),
                        )

                        OutlinedTextField(
                            value = editedClinicSubtitle,
                            onValueChange = { editedClinicSubtitle = it },
                            label = { Text("Specialty or Description (Subtitle)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("clinicSubtitleField"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (brandingSavedMessage != null) {
                                Text(
                                    text = brandingSavedMessage ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Button(
                                onClick = {
                                    securityRepo.updateClinicBranding(editedClinicName, editedClinicSubtitle)
                                    settingsState = securityRepo.settings.value
                                    brandingSavedMessage = "Clinic branding saved and applied successfully!"
                                },
                                modifier = Modifier.testTag("saveBrandingButton"),
                            ) {
                                Text("Save Clinic Branding")
                            }
                        }
                    }
                }

                // Section 1: 6-Digit PIN Management
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "PIN Passcode (6 Digits)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = "6-digit security PIN to prevent unauthorized access to clinical patient records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("changePinButton"),
                        ) {
                            Text("Change 6-Digit PIN", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section 2: Biometric Fingerprint Management
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(
                                        text = "Biometric Fingerprint Unlock",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (isBiometricAvailable) "Fingerprint supported and ready" else "Fingerprint not available on device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBiometricAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }

                            Switch(
                                checked = settingsState.isBiometricEnabled && isBiometricAvailable,
                                onCheckedChange = { enabled ->
                                    securityRepo.setBiometricEnabled(enabled)
                                    settingsState = securityRepo.settings.value
                                },
                                enabled = isBiometricAvailable,
                                modifier = Modifier.testTag("biometricSwitch"),
                            )
                        }

                        Text(
                            text = "When enabled, you can unlock the application instantly with a single touch without typing your PIN each time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (isBiometricAvailable && activity != null) {
                            OutlinedButton(
                                onClick = {
                                    BiometricAuthManager.promptBiometric(
                                        activity = activity,
                                        onResult = { ok ->
                                            biometricFeedbackMessage = if (ok) "Fingerprint verified successfully! Ready for use." else "Fingerprint verification failed."
                                        },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("testBiometricButton"),
                            ) {
                                Text("Test Fingerprint Now")
                            }
                        }

                        if (biometricFeedbackMessage != null) {
                            Text(
                                text = biometricFeedbackMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Section 3: Auto-Lock Timeout Controls
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Auto-Lock Timeout",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = "Set inactivity duration before the app automatically locks in the background:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        AutoLockTimeout.entries.forEach { timeout ->
                            val isSelected = settingsState.autoLockTimeout == timeout
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        securityRepo.setAutoLockTimeout(timeout)
                                        settingsState = securityRepo.settings.value
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                    .testTag("timeoutOption-${timeout.name}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        securityRepo.setAutoLockTimeout(timeout)
                                        settingsState = securityRepo.settings.value
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = timeout.displayNameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                    // Section 4: Encrypted Vault Backup & Restore (Zero Data Loss)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().testTag("backupVaultSettingsCard"),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Encrypted Vault Backup & Restore",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Text(
                                text = "Enterprise AES-256 GCM encryption with PBKDF2 (65,536 iterations). Safely export and restore all records with guaranteed Zero Data Loss.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (backupStatusMessage != null) {
                                Text(
                                    text = backupStatusMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBackupError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier.weight(1f).testTag("exportBackupButton"),
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Export Backup", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        restoreFileLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(1f).testTag("restoreBackupButton"),
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restore Backup", maxLines = 1)
                                }
                            }
                        }
                    }

                    // Lock App Now Button
                    Button(
                        onClick = {
                            onDismiss()
                            onLockNow()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lockAppNowButton"),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lock App Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Sub-dialog: Change 6-Digit PIN
    if (showChangePinDialog) {
        ChangePinDialog(
            securityRepo = securityRepo,
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                settingsState = securityRepo.settings.value
            },
        )
    }

    // Sub-dialog: Export Backup
    if (showExportDialog && onExportBackup != null) {
        ExportBackupDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { passphrase ->
                showExportDialog = false
                onExportBackup(passphrase) { backupBytes ->
                    if (backupBytes != null && backupBytes.isNotEmpty()) {
                        try {
                            val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
                            val backupFile = File(backupDir, "ANC_Vault_Backup_${System.currentTimeMillis()}.ancvault")
                            FileOutputStream(backupFile).use { it.write(backupBytes) }
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                backupFile,
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Save or Share Encrypted Backup"))
                            backupStatusMessage = "Encrypted backup created and exported successfully (${backupBytes.size} bytes)!"
                            isBackupError = false
                        } catch (e: Exception) {
                            backupStatusMessage = "Backup created but unable to open share sheet: ${e.message}"
                            isBackupError = true
                        }
                    } else {
                        backupStatusMessage = "Failed to generate backup!"
                        isBackupError = true
                    }
                }
            },
        )
    }

    // Sub-dialog: Restore Passphrase
    if (showRestorePassphraseDialog && pendingRestoreBytes != null && onRestoreBackup != null) {
        RestorePassphraseDialog(
            onDismiss = {
                showRestorePassphraseDialog = false
                pendingRestoreBytes = null
            },
            onConfirm = { passphrase ->
                val bytes = pendingRestoreBytes ?: return@RestorePassphraseDialog
                showRestorePassphraseDialog = false
                pendingRestoreBytes = null
                onRestoreBackup(bytes, passphrase) { result ->
                    when (result) {
                        is BackupInspectionResult.Valid -> {
                            backupStatusMessage = "Vault restored successfully (${result.patientCount} patients)! All records 100% verified with Zero Data Loss."
                            isBackupError = false
                        }
                        is BackupInspectionResult.InvalidPassphraseOrCorrupted -> {
                            backupStatusMessage = "Decryption failed: Incorrect passphrase or corrupted backup file!"
                            isBackupError = true
                        }
                        is BackupInspectionResult.Error -> {
                            backupStatusMessage = "Error during restore: ${result.reason}"
                            isBackupError = true
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ChangePinDialog(
    securityRepo: SecuritySettingsRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set New 6-Digit PIN", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter 6 digits to lock and unlock the application. The new PIN will be active immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("New 6-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("newPinField"),
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("confirmPinField"),
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length != 6) {
                        errorMessage = "PIN must be exactly 6 digits!"
                    } else if (newPin != confirmPin) {
                        errorMessage = "New PIN and confirmation do not match!"
                    } else {
                        securityRepo.updatePin(newPin)
                        onSuccess()
                    }
                },
                modifier = Modifier.testTag("saveNewPinButton"),
            ) {
                Text("Save New PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ExportBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export Encrypted Backup", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a strong passphrase to encrypt the backup file (AES-256 GCM). You will need this passphrase to restore the vault.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Backup Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backupPassphraseField"),
                )

                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = { confirmPassphrase = it },
                    label = { Text("Confirm Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("confirmBackupPassphraseField"),
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.length < 6) {
                        errorMessage = "Passphrase must be at least 6 characters!"
                    } else if (passphrase != confirmPassphrase) {
                        errorMessage = "Passphrase and confirmation do not match!"
                    } else {
                        onConfirm(passphrase.toCharArray())
                    }
                },
                modifier = Modifier.testTag("confirmExportBackupButton"),
            ) {
                Text("Export & Encrypt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun RestorePassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Restore Encrypted Vault", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the passphrase for this encrypted file to restore all patients, visits, ultrasounds, and clinical records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase for Decryption") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("restorePassphraseField"),
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.isEmpty()) {
                        errorMessage = "Please enter the passphrase!"
                    } else {
                        onConfirm(passphrase.toCharArray())
                    }
                },
                modifier = Modifier.testTag("confirmRestoreBackupButton"),
            ) {
                Text("Decrypt & Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
