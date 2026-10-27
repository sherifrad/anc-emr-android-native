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
                backupStatusMessage = "تعذر قراءة ملف النسخة الاحتياطية المختار!"
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
                                text = "إعدادات العيادة والنظام",
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
                                "الأمان والعيادة",
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
                                "دليل التحاليل",
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
                                "دليل الأدوية",
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
                                    text = "المظهر ونمط العرض (App Theme)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Text(
                                text = "اختر نمط الألوان المناسب لبيئة العمل (عيادة مضيئة أو غرفة موجات فوق صوتية معتمة):",
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
                                            text = mode.displayNameAr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = mode.displayNameEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                text = "هوية العيادة والشعار (Clinic Branding)",
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
                                    text = "شعار العيادة المعتمد (الرمز الدائري)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "متطابق مع أيقونة التطبيق وشاشة البداية (Splash Screen) بالخلفية العاجية #FBF8F5 دون أي اقتصاص للأطراف.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editedClinicName,
                            onValueChange = { editedClinicName = it },
                            label = { Text("اسم العيادة (الهيدر والشريط العلوي)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("clinicNameField"),
                        )

                        OutlinedTextField(
                            value = editedClinicSubtitle,
                            onValueChange = { editedClinicSubtitle = it },
                            label = { Text("الوصف أو التخصص (العنوان الفرعي)") },
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
                                    brandingSavedMessage = "تم حفظ وتطبيق هوية العيادة بنجاح!"
                                },
                                modifier = Modifier.testTag("saveBrandingButton"),
                            ) {
                                Text("حفظ هوية العيادة")
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
                                text = "رمز المرور (PIN - 6 أرقام)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = "رمز الحماية مكون من 6 أرقام لمنع الدخول غير المصرح به للملفات الطبية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("changePinButton"),
                        ) {
                            Text("تغيير رمز المرور المكون من 6 أرقام", fontWeight = FontWeight.SemiBold)
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
                                        text = "فتح التطبيق بالبصمة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (isBiometricAvailable) "بصمة الإصبع مدعومة وجاهزة" else "البصمة غير متوفرة في النظام",
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
                            text = "عند تفعيل البصمة، يمكنك فتح التطبيق بلمسة واحدة مباشرة دون الحاجة لكتابة رمز المرور في كل مرة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (isBiometricAvailable && activity != null) {
                            OutlinedButton(
                                onClick = {
                                    BiometricAuthManager.promptBiometric(
                                        activity = activity,
                                        onResult = { ok ->
                                            biometricFeedbackMessage = if (ok) "تم التحقق من البصمة بنجاح! جاهزة للاستخدام." else "فشل التحقق من البصمة."
                                        },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("testBiometricButton"),
                            ) {
                                Text("اختبار قراءة البصمة الآن")
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
                                text = "القفل التلقائي للشاشة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = "تحديد المهلة الزمنية قبل قفل التطبيق تلقائياً عند تركه في الخلفية:",
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
                                        text = timeout.displayNameAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = timeout.displayNameEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    text = "النسخ الاحتياطي المشفر للخزنة (Encrypted Vault)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Text(
                                text = "تشفير قياسي مؤسسي (AES-256 GCM مع PBKDF2 بـ 65,536 تكرار). يمكنك تصدير ملفات مشفرة ونقلها أو استعادتها بأمان تام مع ضمان عدم فقد أي بيانات (Zero Data Loss).",
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
                                    Text("تصدير مشفر", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        restoreFileLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.weight(1f).testTag("restoreBackupButton"),
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("استعادة مشفرة", maxLines = 1)
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
                        Text("قفل التطبيق الآن (Lock App Now)", fontWeight = FontWeight.Bold)
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
                            context.startActivity(Intent.createChooser(shareIntent, "حفظ أو مشاركة النسخة المشفرة"))
                            backupStatusMessage = "تم إنشاء وتصدير النسخة الاحتياطية المشفرة بنجاح (${backupBytes.size} بايت)!"
                            isBackupError = false
                        } catch (e: Exception) {
                            backupStatusMessage = "تم إنشاء النسخة ولكن تعذر فتح المشاركة: ${e.message}"
                            isBackupError = true
                        }
                    } else {
                        backupStatusMessage = "فشل إنشاء النسخة الاحتياطية!"
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
                            backupStatusMessage = "تم استعادة الخزنة بنجاح (${result.patientCount} مريضة)! جميع السجلات متطابقة بنسبة 100% دون أي فقد في البيانات."
                            isBackupError = false
                        }
                        is BackupInspectionResult.InvalidPassphraseOrCorrupted -> {
                            backupStatusMessage = "فشل فك التشفير: كلمة المرور غير صحيحة أو الملف تالف!"
                            isBackupError = true
                        }
                        is BackupInspectionResult.Error -> {
                            backupStatusMessage = "خطأ أثناء الاستعادة: ${result.reason}"
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
            Text("تعيين رمز PIN جديد (6 أرقام)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اختر 6 أرقام تود استخدامها لقفل وفتح التطبيق، وسيتم حفظ الرمز وتفعيله مباشرة دون الحاجة لأي رمز سابق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("رمز PIN الجديد (6 أرقام)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("newPinField"),
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("تأكيد رمز PIN الجديد") },
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
                        errorMessage = "يجب أن يتكون رمز المرور من 6 أرقام!"
                    } else if (newPin != confirmPin) {
                        errorMessage = "رمز المرور الجديد وتأكيده غير متطابقين!"
                    } else {
                        securityRepo.updatePin(newPin)
                        onSuccess()
                    }
                },
                modifier = Modifier.testTag("saveNewPinButton"),
            ) {
                Text("حفظ رمز PIN الجديد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
            Text("تصدير نسخة احتياطية مشفرة", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "أدخل كلمة مرور قوية لتشفير ملف النسخة الاحتياطية (AES-256 GCM). ستحتاج لهذه الكلمة عند استعادة السجلات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("كلمة مرور النسخة المشفرة") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backupPassphraseField"),
                )

                OutlinedTextField(
                    value = confirmPassphrase,
                    onValueChange = { confirmPassphrase = it },
                    label = { Text("تأكيد كلمة المرور") },
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
                        errorMessage = "يجب أن لا تقل كلمة المرور عن 6 أحرف/أرقام!"
                    } else if (passphrase != confirmPassphrase) {
                        errorMessage = "كلمة المرور وتأكيدها غير متطابقين!"
                    } else {
                        onConfirm(passphrase.toCharArray())
                    }
                },
                modifier = Modifier.testTag("confirmExportBackupButton"),
            ) {
                Text("تصدير وتشفير")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
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
            Text("استعادة الخزنة المشفرة", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "أدخل كلمة المرور الخاصة بهذا الملف المشفر لفك تشفيره واستعادة جميع سجلات المرضى والزيارات والسونار.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("كلمة المرور لفك التشفير") },
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
                        errorMessage = "يرجى إدخال كلمة المرور!"
                    } else {
                        onConfirm(passphrase.toCharArray())
                    }
                },
                modifier = Modifier.testTag("confirmRestoreBackupButton"),
            ) {
                Text("فك التشفير واستعادة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}
