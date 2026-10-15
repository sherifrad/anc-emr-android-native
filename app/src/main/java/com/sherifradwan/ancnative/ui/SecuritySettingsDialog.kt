package com.sherifradwan.ancnative.ui

import android.app.Activity
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.material3.TabRow
import androidx.compose.ui.graphics.Color
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.R
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
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var settingsState by remember { mutableStateOf(securityRepo.settings.value) }
    var editedClinicName by remember(settingsState.clinicName) { mutableStateOf(settingsState.clinicName) }
    var editedClinicSubtitle by remember(settingsState.clinicSubtitle) { mutableStateOf(settingsState.clinicSubtitle) }
    var brandingSavedMessage by remember { mutableStateOf<String?>(null) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var biometricFeedbackMessage by remember { mutableStateOf<String?>(null) }

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
                            color = androidx.compose.ui.graphics.Color(0xFFFBF8F5),
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

                TabRow(
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
                                color = androidx.compose.ui.graphics.Color(0xFFFBF8F5),
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
