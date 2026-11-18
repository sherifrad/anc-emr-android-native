package com.sherifradwan.ancnative.ui

import android.app.Activity
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.security.AppThemeMode
import com.sherifradwan.ancnative.security.BiometricAuthManager
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.security.isValidNewPin
import com.sherifradwan.ancnative.storage.BackupInspectionResult
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.isRtlText
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow
import java.io.File
import java.io.FileOutputStream

enum class SettingsTab {
    SECURITY,
    LAB_CATALOG,
    MED_CATALOG,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecuritySettingsDialog(
    securityRepo: SecuritySettingsRepository,
    onDismiss: () -> Unit,
    onLockNow: () -> Unit,
    onExportBackup: ((CharArray, (ByteArray?) -> Unit) -> Unit)? = null,
    onRestoreBackup: ((ByteArray, CharArray, (BackupInspectionResult) -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context as? Activity
    var settingsState by remember { mutableStateOf(securityRepo.settings.value) }
    var editedClinicName by remember(settingsState.clinicName) { mutableStateOf(settingsState.clinicName) }
    var editedClinicSubtitle by remember(settingsState.clinicSubtitle) { mutableStateOf(settingsState.clinicSubtitle) }
    var editedDoctorNameEn by remember(settingsState.doctorNameEn) { mutableStateOf(settingsState.doctorNameEn) }
    var editedDoctorSpecialtyEn by remember(settingsState.doctorSpecialtyEn) { mutableStateOf(settingsState.doctorSpecialtyEn) }
    var editedDoctorNameAr by remember(settingsState.doctorNameAr) { mutableStateOf(settingsState.doctorNameAr) }
    var editedDoctorSpecialtyAr by remember(settingsState.doctorSpecialtyAr) { mutableStateOf(settingsState.doctorSpecialtyAr) }
    var editedClinicPhone by remember(settingsState.clinicPhone) { mutableStateOf(settingsState.clinicPhone) }
    var brandingSavedMessage by remember { mutableStateOf<String?>(null) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var biometricFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isBiometricError by remember { mutableStateOf(false) }

    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isBackupError by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestorePassphraseDialog by remember { mutableStateOf(false) }

    val backupReadErrorMessage = stringResource(R.string.security_backup_read_error)
    val brandingSavedNotice = stringResource(R.string.security_branding_saved)
    val biometricVerifiedMessage = stringResource(R.string.security_biometric_verified)
    val biometricFailedMessage = stringResource(R.string.security_biometric_failed)
    val backupShareTitle = stringResource(R.string.security_backup_share_title)
    val backupExportFailMessage = stringResource(R.string.security_backup_export_fail)
    val restoreDecryptFailMessage = stringResource(R.string.security_restore_decrypt_fail)

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
                showRestoreConfirmDialog = true
            } else {
                backupStatusMessage = backupReadErrorMessage
                isBackupError = true
            }
        }
    }

    val isBiometricAvailable = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp
    var selectedTab by remember { mutableStateOf(SettingsTab.SECURITY) }
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val patientFamily = FieldFamilies.patient()
    val narrowStacked = LocalDensity.current.fontScale > 1.3f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = maxHeight)
                .testTag("securitySettingsDialog"),
            shape = RecordShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.clinic_logo_foreground),
                                contentDescription = stringResource(R.string.clinic_logo_cd),
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.security_settings_dialog_title),
                            style = RecordType.SectionTitle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = settingsState.clinicName,
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("closeSettingsButton"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RecordFilterChip(
                        selected = selectedTab == SettingsTab.SECURITY,
                        label = stringResource(R.string.settings_tab_security),
                        onClick = { selectedTab = SettingsTab.SECURITY },
                        tag = "settingsTabSecurity",
                        wrap = true,
                        minTouchTarget = true,
                    )
                    RecordFilterChip(
                        selected = selectedTab == SettingsTab.LAB_CATALOG,
                        label = stringResource(R.string.settings_tab_lab_tests),
                        onClick = { selectedTab = SettingsTab.LAB_CATALOG },
                        tag = "settingsTabLabCatalog",
                        wrap = true,
                        minTouchTarget = true,
                    )
                    RecordFilterChip(
                        selected = selectedTab == SettingsTab.MED_CATALOG,
                        label = stringResource(R.string.settings_tab_medications),
                        onClick = { selectedTab = SettingsTab.MED_CATALOG },
                        tag = "settingsTabMedCatalog",
                        wrap = true,
                        minTouchTarget = true,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // CatalogSettingsSections expect SecuritySettingsDialog to own vertical scrolling.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectedTab == SettingsTab.LAB_CATALOG) {
                        LabCatalogSettingsSection(catalogDelegate = catalogRepo)
                    } else if (selectedTab == SettingsTab.MED_CATALOG) {
                        MedicationCatalogSettingsSection(catalogDelegate = catalogRepo)
                    } else {
                        CompositionLocalProvider(LocalFieldFamily provides patientFamily) {
                            RecordSectionCard(
                                title = stringResource(R.string.security_theme_title),
                                family = patientFamily,
                                modifier = Modifier.testTag("themeSettingsCard"),
                            ) {
                                Text(
                                    text = stringResource(R.string.security_theme_subtitle),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectableGroup(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    AppThemeMode.entries.forEach { mode ->
                                        val isSelected = settingsState.themeMode == mode
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clip(RecordShapes.Small)
                                                .clickable {
                                                    securityRepo.setThemeMode(mode)
                                                    settingsState = securityRepo.settings.value
                                                }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
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
                                            Text(
                                                text = stringResource(mode.labelRes()),
                                                style = if (isSelected) RecordType.KeyValue else RecordType.FieldValue,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            RecordSectionCard(
                                title = stringResource(R.string.security_branding_title),
                                family = patientFamily,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(R.drawable.clinic_logo_foreground),
                                                contentDescription = stringResource(R.string.clinic_logo_cd),
                                                modifier = Modifier.size(42.dp),
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.security_emblem_title),
                                            style = RecordType.CardName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = stringResource(R.string.security_emblem_desc),
                                            style = RecordType.Secondary,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                SecurityLabeledField(
                                    value = editedClinicName,
                                    onValueChange = { editedClinicName = it },
                                    label = stringResource(R.string.security_clinic_name_label),
                                    tag = "clinicNameField",
                                )

                                SecurityLabeledField(
                                    value = editedClinicSubtitle,
                                    onValueChange = { editedClinicSubtitle = it },
                                    label = stringResource(R.string.security_clinic_subtitle_label),
                                    tag = "clinicSubtitleField",
                                )

                                SecurityLabeledField(
                                    value = editedDoctorNameEn,
                                    onValueChange = { editedDoctorNameEn = it },
                                    label = stringResource(R.string.security_doctor_name_en_label),
                                    tag = "settingsDoctorNameEn",
                                )

                                SecurityLabeledField(
                                    value = editedDoctorSpecialtyEn,
                                    onValueChange = { editedDoctorSpecialtyEn = it },
                                    label = stringResource(R.string.security_doctor_specialty_en_label),
                                    tag = "settingsDoctorSpecialtyEn",
                                )

                                SecurityLabeledField(
                                    value = editedDoctorNameAr,
                                    onValueChange = { editedDoctorNameAr = it },
                                    label = stringResource(R.string.security_doctor_name_ar_label),
                                    textStyle = rtlAwareTextStyle(editedDoctorNameAr),
                                    tag = "settingsDoctorNameAr",
                                )

                                SecurityLabeledField(
                                    value = editedDoctorSpecialtyAr,
                                    onValueChange = { editedDoctorSpecialtyAr = it },
                                    label = stringResource(R.string.security_doctor_specialty_ar_label),
                                    textStyle = rtlAwareTextStyle(editedDoctorSpecialtyAr),
                                    tag = "settingsDoctorSpecialtyAr",
                                )

                                SecurityLabeledField(
                                    value = editedClinicPhone,
                                    onValueChange = { editedClinicPhone = it },
                                    label = stringResource(R.string.security_clinic_phone_label),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    textStyle = RecordType.FieldValue.copy(textDirection = TextDirection.Ltr),
                                    tag = "settingsClinicPhone",
                                )

                                if (brandingSavedMessage != null) {
                                    Text(
                                        text = brandingSavedMessage ?: "",
                                        style = RecordType.Secondary,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .semantics { liveRegion = LiveRegionMode.Polite }
                                            .testTag("brandingSavedMessage"),
                                    )
                                }

                                Button(
                                    onClick = {
                                        securityRepo.updateClinicBranding(editedClinicName, editedClinicSubtitle)
                                        securityRepo.updateClinicAttestation(
                                            nameEn = editedDoctorNameEn,
                                            specialtyEn = editedDoctorSpecialtyEn,
                                            nameAr = editedDoctorNameAr,
                                            specialtyAr = editedDoctorSpecialtyAr,
                                            phone = editedClinicPhone,
                                        )
                                        settingsState = securityRepo.settings.value
                                        editedDoctorNameEn = settingsState.doctorNameEn
                                        editedDoctorSpecialtyEn = settingsState.doctorSpecialtyEn
                                        editedDoctorNameAr = settingsState.doctorNameAr
                                        editedDoctorSpecialtyAr = settingsState.doctorSpecialtyAr
                                        editedClinicPhone = settingsState.clinicPhone
                                        brandingSavedMessage = brandingSavedNotice
                                    },
                                    shape = RecordShapes.Small,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumTouchTarget()
                                        .testTag("saveBrandingButton"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.security_save_branding),
                                        style = RecordType.Action,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            RecordSectionCard(
                                title = stringResource(R.string.security_pin_section_title),
                                family = patientFamily,
                            ) {
                                Text(
                                    text = stringResource(R.string.security_pin_section_desc),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    shape = RecordShapes.Small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumTouchTarget()
                                        .testTag("changePinButton"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.security_change_pin_action),
                                        style = RecordType.Action,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            RecordSectionCard(
                                title = stringResource(R.string.security_biometric_title),
                                family = patientFamily,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.security_biometric_switch_label),
                                            style = RecordType.CardName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = stringResource(
                                                if (isBiometricAvailable) {
                                                    R.string.security_biometric_ready
                                                } else {
                                                    R.string.security_biometric_unavailable
                                                },
                                            ),
                                            style = RecordType.Secondary,
                                            color = if (isBiometricAvailable) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                        )
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
                                    text = stringResource(R.string.security_biometric_desc),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (isBiometricAvailable && activity != null) {
                                    OutlinedButton(
                                        onClick = {
                                            BiometricAuthManager.promptBiometric(
                                                activity = activity,
                                                onResult = { ok ->
                                                    isBiometricError = !ok
                                                    biometricFeedbackMessage = if (ok) {
                                                        biometricVerifiedMessage
                                                    } else {
                                                        biometricFailedMessage
                                                    }
                                                },
                                            )
                                        },
                                        shape = RecordShapes.Small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumTouchTarget()
                                            .testTag("testBiometricButton"),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.security_test_biometric),
                                            style = RecordType.Action,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }

                                if (biometricFeedbackMessage != null) {
                                    Text(
                                        text = biometricFeedbackMessage ?: "",
                                        style = RecordType.Secondary,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isBiometricError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier
                                            .semantics { liveRegion = LiveRegionMode.Polite }
                                            .testTag("biometricFeedbackMessage"),
                                    )
                                }
                            }

                            RecordSectionCard(
                                title = stringResource(R.string.security_auto_lock_title),
                                family = patientFamily,
                            ) {
                                Text(
                                    text = stringResource(R.string.security_auto_lock_body),
                                    style = RecordType.FieldValue,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            RecordSectionCard(
                                title = stringResource(R.string.security_backup_title),
                                family = patientFamily,
                                modifier = Modifier.testTag("backupVaultSettingsCard"),
                            ) {
                                Text(
                                    text = stringResource(R.string.security_backup_desc),
                                    style = RecordType.Secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (backupStatusMessage != null) {
                                    Surface(
                                        shape = RecordShapes.Small,
                                        color = if (isBackupError) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            patientFamily.container
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isBackupError) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("backupStatusMessage"),
                                    ) {
                                        Text(
                                            text = backupStatusMessage ?: "",
                                            style = RecordType.Secondary,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isBackupError) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                .semantics { liveRegion = LiveRegionMode.Polite },
                                        )
                                    }
                                }

                                val exportButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                                    Button(
                                        onClick = { showExportDialog = true },
                                        shape = RecordShapes.Small,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = buttonModifier
                                            .minimumTouchTarget()
                                            .testTag("exportBackupButton"),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.security_export_backup),
                                            style = RecordType.Action,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                val restoreButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                                    OutlinedButton(
                                        onClick = { restoreFileLauncher.launch("*/*") },
                                        shape = RecordShapes.Small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = buttonModifier
                                            .minimumTouchTarget()
                                            .testTag("restoreBackupButton"),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.security_restore_backup),
                                            style = RecordType.Action,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }

                                if (narrowStacked) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        exportButton(Modifier.fillMaxWidth())
                                        restoreButton(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        exportButton(Modifier.weight(1f))
                                        restoreButton(Modifier.weight(1f))
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    onLockNow()
                                },
                                shape = RecordShapes.Small,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumTouchTarget()
                                    .testTag("lockAppNowButton"),
                            ) {
                                Text(
                                    text = stringResource(R.string.security_lock_app_now),
                                    style = RecordType.Action,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

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
                            context.startActivity(Intent.createChooser(shareIntent, backupShareTitle))
                            backupStatusMessage = resources.getString(R.string.security_backup_export_ok, backupBytes.size)
                            isBackupError = false
                        } catch (e: Exception) {
                            backupStatusMessage = resources.getString(R.string.security_backup_share_error, e.message ?: "")
                            isBackupError = true
                        }
                    } else {
                        backupStatusMessage = backupExportFailMessage
                        isBackupError = true
                    }
                }
            },
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreBytes != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreBytes = null
            },
            shape = RecordShapes.Large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.security_restore_confirm_title),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.security_restore_confirm_body),
                    style = RecordType.FieldValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        showRestorePassphraseDialog = true
                    },
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("confirmReplaceAllButton"),
                ) {
                    Text(
                        text = stringResource(R.string.security_restore_replace_all),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreBytes = null
                    },
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("cancelReplaceAllButton"),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier.testTag("restore_confirm_dialog"),
        )
    }

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
                            backupStatusMessage = context.resources.getQuantityString(
                                R.plurals.security_vault_restored,
                                result.patientCount,
                                result.patientCount,
                            )
                            isBackupError = false
                        }
                        is BackupInspectionResult.InvalidPassphraseOrCorrupted -> {
                            backupStatusMessage = restoreDecryptFailMessage
                            isBackupError = true
                        }
                        is BackupInspectionResult.Error -> {
                            backupStatusMessage = resources.getString(R.string.security_restore_error, result.reason)
                            isBackupError = true
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SecurityFormDialog(
    title: String,
    confirmLabel: String,
    confirmTag: String,
    cancelTag: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTag: String? = null,
    errorMessage: String? = null,
    errorTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp - 32).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxHeight)
                .let { if (dialogTag != null) it.testTag(dialogTag) else it },
            shape = RecordShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalFieldFamily provides FieldFamilies.patient()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = RecordType.SectionTitle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .semantics { heading() },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        content()
                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                style = RecordType.Action,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .let { m -> if (errorTag != null) m.testTag(errorTag) else m },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SheetCancelSaveRow(
                        enabled = true,
                        saving = false,
                        saveEnabled = true,
                        savingLabel = confirmLabel,
                        onCancel = onDismiss,
                        onSave = onConfirm,
                        cancelTag = cancelTag,
                        saveTag = confirmTag,
                        spinnerTag = "${confirmTag}Spinner",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

private fun rtlAwareTextStyle(text: String): TextStyle {
    val rtl = text.isBlank() || isRtlText(text)
    return RecordType.FieldValue.copy(
        textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr,
        textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
    )
}

@Composable
private fun SecurityLabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = RecordType.FieldValue,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            if (isError) {
                Text(
                    text = label,
                    style = RecordType.FieldLabel,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                FormFieldLabel(label)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            shape = RecordShapes.Small,
            textStyle = textStyle,
            colors = editorFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .testTag(tag),
        )
    }
}

@StringRes
private fun AppThemeMode.labelRes(): Int = when (this) {
    AppThemeMode.SYSTEM -> R.string.theme_mode_system
    AppThemeMode.LIGHT -> R.string.theme_mode_light
    AppThemeMode.DARK -> R.string.theme_mode_dark
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
    val invalidPinMessage = stringResource(R.string.pin_invalid)
    val pinMismatchMessage = stringResource(R.string.security_pin_mismatch)

    SecurityFormDialog(
        title = stringResource(R.string.security_set_pin_title),
        confirmLabel = stringResource(R.string.security_save_pin),
        confirmTag = "saveNewPinButton",
        cancelTag = "cancelChangePinButton",
        dialogTag = "changePinDialog",
        errorMessage = errorMessage,
        errorTag = "changePinError",
        onDismiss = onDismiss,
        onConfirm = {
            if (!isValidNewPin(newPin)) {
                errorMessage = invalidPinMessage
            } else if (newPin != confirmPin) {
                errorMessage = pinMismatchMessage
            } else {
                if (securityRepo.updatePin(newPin)) onSuccess()
            }
        },
    ) {
        Text(
            text = stringResource(R.string.pin_rule),
            style = RecordType.Secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecurityLabeledField(
            value = newPin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c in '0'..'9' }) newPin = it },
            label = stringResource(R.string.security_new_pin_label),
            isError = errorMessage != null && !isValidNewPin(newPin),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            tag = "newPinField",
        )

        SecurityLabeledField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c in '0'..'9' }) confirmPin = it },
            label = stringResource(R.string.security_confirm_pin_label),
            isError = errorMessage != null && isValidNewPin(newPin) && newPin != confirmPin,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            tag = "confirmPinField",
        )
    }
}

@Composable
private fun ExportBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val passphraseShortMessage = stringResource(R.string.security_passphrase_short)
    val passphraseMismatchMessage = stringResource(R.string.security_passphrase_mismatch)

    SecurityFormDialog(
        title = stringResource(R.string.security_export_dialog_title),
        confirmLabel = stringResource(R.string.security_export_backup),
        confirmTag = "confirmExportBackupButton",
        cancelTag = "cancelExportBackupButton",
        dialogTag = "exportBackupDialog",
        errorMessage = errorMessage,
        errorTag = "exportBackupError",
        onDismiss = onDismiss,
        onConfirm = {
            if (passphrase.length < 6) {
                errorMessage = passphraseShortMessage
            } else if (passphrase != confirmPassphrase) {
                errorMessage = passphraseMismatchMessage
            } else {
                onConfirm(passphrase.toCharArray())
            }
        },
    ) {
        Text(
            text = stringResource(R.string.security_export_dialog_body),
            style = RecordType.Secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecurityLabeledField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = stringResource(R.string.security_passphrase_label),
            isError = errorMessage != null && passphrase.length < 6,
            visualTransformation = PasswordVisualTransformation(),
            tag = "backupPassphraseField",
        )

        SecurityLabeledField(
            value = confirmPassphrase,
            onValueChange = { confirmPassphrase = it },
            label = stringResource(R.string.security_confirm_passphrase_label),
            isError = errorMessage != null && passphrase.length >= 6 && passphrase != confirmPassphrase,
            visualTransformation = PasswordVisualTransformation(),
            tag = "confirmBackupPassphraseField",
        )
    }
}

@Composable
private fun RestorePassphraseDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val passphraseEmptyMessage = stringResource(R.string.security_restore_passphrase_empty)

    SecurityFormDialog(
        title = stringResource(R.string.security_restore_dialog_title),
        confirmLabel = stringResource(R.string.security_decrypt_and_restore),
        confirmTag = "confirmRestoreBackupButton",
        cancelTag = "cancelRestorePassphraseButton",
        dialogTag = "restorePassphraseDialog",
        errorMessage = errorMessage,
        errorTag = "restorePassphraseError",
        onDismiss = onDismiss,
        onConfirm = {
            if (passphrase.isEmpty()) {
                errorMessage = passphraseEmptyMessage
            } else {
                onConfirm(passphrase.toCharArray())
            }
        },
    ) {
        Text(
            text = stringResource(R.string.security_restore_dialog_body),
            style = RecordType.Secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecurityLabeledField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = stringResource(R.string.security_passphrase_label),
            isError = errorMessage != null,
            visualTransformation = PasswordVisualTransformation(),
            tag = "restorePassphraseField",
        )
    }
}
