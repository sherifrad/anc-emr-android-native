package com.sherifradwan.ancnative.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.security.BiometricAuthManager
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.security.isValidNewPin
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType

@Composable
fun AppLockScreen(
    securityRepo: SecuritySettingsRepository,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settingsState by securityRepo.settings.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var confirmationPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricAttemptedOnLaunch by remember { mutableStateOf(false) }
    var lockoutRemainingMs by remember { mutableIntStateOf(securityRepo.getLockoutRemainingMs().toInt()) }
    val pinInvalidMessage = stringResource(R.string.pin_invalid)
    val pinMismatchMessage = stringResource(R.string.pin_mismatch)
    val pinIncorrectMessage = stringResource(R.string.pin_incorrect)
    val biometricFailedMessage = stringResource(R.string.pin_biometric_failed)
    val biometricUnavailableMessage = stringResource(R.string.pin_biometric_unavailable)
    LaunchedEffect(Unit) {
        while (true) {
            lockoutRemainingMs = securityRepo.getLockoutRemainingMs().toInt()
            kotlinx.coroutines.delay(1000)
        }
    }
    val setupMode = settingsState.needsPinSetup
    val legacyChangeRequired = settingsState.isLegacyPin && !settingsState.needsPinSetup

    LaunchedEffect(settingsState.needsPinSetup) {
        if (settingsState.needsPinSetup) {
            enteredPin = ""
            confirmationPin = ""
            isConfirmingPin = false
            errorMessage = null
        }
    }

    val isBiometricSupported = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    LaunchedEffect(setupMode, legacyChangeRequired, settingsState.isBiometricEnabled, isBiometricSupported) {
        if (!setupMode && !legacyChangeRequired && !biometricAttemptedOnLaunch && settingsState.isBiometricEnabled && isBiometricSupported && activity != null) {
            biometricAttemptedOnLaunch = true
            BiometricAuthManager.promptBiometric(
                activity = activity,
                onResult = { success ->
                    if (success) {
                        securityRepo.unlockApp()
                        onUnlockSuccess()
                    }
                },
            )
        }
    }

    fun handleDigit(digit: String) {
        if (lockoutRemainingMs > 0) return
        if (enteredPin.length < 6) {
            val updated = enteredPin + digit
            enteredPin = updated
            errorMessage = null

            if (updated.length == 6) {
                if (setupMode && !legacyChangeRequired) {
                    if (!isValidNewPin(updated)) {
                        errorMessage = pinInvalidMessage
                        enteredPin = ""
                    } else if (!isConfirmingPin) {
                        confirmationPin = updated
                        isConfirmingPin = true
                        enteredPin = ""
                    } else if (updated != confirmationPin) {
                        errorMessage = pinMismatchMessage
                        confirmationPin = ""
                        isConfirmingPin = false
                        enteredPin = ""
                    } else if (securityRepo.setupPin(updated)) {
                        securityRepo.unlockApp()
                        onUnlockSuccess()
                    }
                } else if (securityRepo.validatePin(updated)) {
                    if (settingsState.needsPinSetup || !isValidNewPin(updated)) {
                        isConfirmingPin = false
                        confirmationPin = ""
                        enteredPin = ""
                        errorMessage = null
                    } else {
                        securityRepo.unlockApp()
                        onUnlockSuccess()
                    }
                } else {
                    errorMessage = if (securityRepo.getLockoutRemainingMs() > 0) null else pinIncorrectMessage
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (lockoutRemainingMs > 0) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    fun triggerBiometric() {
        if (activity != null && isBiometricSupported) {
            errorMessage = null
            BiometricAuthManager.promptBiometric(
                activity = activity,
                onResult = { success ->
                    if (success) {
                        securityRepo.unlockApp()
                        onUnlockSuccess()
                    } else {
                        errorMessage = biometricFailedMessage
                    }
                },
            )
        } else {
            errorMessage = biometricUnavailableMessage
        }
    }

    val patientFamily = FieldFamilies.patient()
    val dark = isDarkTheme()
    val keypadEnabled = lockoutRemainingMs <= 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("appLockScreen"),
    ) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 400.dp) / 2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.clinic_logo_foreground),
                        contentDescription = stringResource(R.string.clinic_logo_cd),
                        modifier = Modifier.size(64.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = settingsState.clinicName,
                style = RecordType.HeaderName,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = settingsState.clinicSubtitle,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    setupMode && isConfirmingPin -> stringResource(R.string.pin_confirm_title)
                    setupMode -> stringResource(R.string.pin_setup_title)
                    legacyChangeRequired -> stringResource(R.string.pin_legacy_title)
                    else -> stringResource(R.string.pin_unlock_title)
                },
                style = RecordType.SectionTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            if (setupMode) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.pin_rule),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (setupMode && !isConfirmingPin) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pin_setup_hint),
                    style = RecordType.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    color = patientFamily.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pinSetup"),
                )
            }
            if (setupMode && isConfirmingPin) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pin_confirm_hint),
                    style = RecordType.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    color = patientFamily.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pinConfirm"),
                )
            }
            if (lockoutRemainingMs > 0) {
                Spacer(Modifier.height(10.dp))
                val seconds = (lockoutRemainingMs + 999) / 1000
                Surface(
                    shape = RecordShapes.Small,
                    color = if (dark) StatusHighDarkBg else StatusHighBg,
                    border = BorderStroke(1.dp, if (dark) StatusHighDarkBorder else StatusHighBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.pin_lockout, seconds / 60, seconds % 60),
                        style = RecordType.Action,
                        color = if (dark) StatusHighDarkText else StatusHighText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("pinLockoutMessage")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val dotsDescription = stringResource(R.string.pin_digits_entered_cd, enteredPin.length)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .testTag("pinDotsRow")
                    .semantics(mergeDescendants = true) {
                        contentDescription = dotsDescription
                    },
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = when {
                                    errorMessage != null -> MaterialTheme.colorScheme.error
                                    isFilled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .background(
                                if (isFilled) {
                                    if (errorMessage != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                            )
                            .testTag("pinDot-$i"),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("appLockErrorMessage"),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("1", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("1") }
                    KeypadButton("2", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("2") }
                    KeypadButton("3", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("3") }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("4", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("4") }
                    KeypadButton("5", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("5") }
                    KeypadButton("6", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("6") }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("7", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("7") }
                    KeypadButton("8", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("8") }
                    KeypadButton("9", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("9") }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isBiometricSupported && !setupMode && !legacyChangeRequired) {
                        val bioEnabled = isBiometricSupported && settingsState.isBiometricEnabled && !setupMode && !legacyChangeRequired && lockoutRemainingMs <= 0
                        Surface(
                            onClick = { triggerBiometric() },
                            enabled = bioEnabled,
                            shape = RecordShapes.Large,
                            color = if (bioEnabled) patientFamily.container else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (bioEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .minimumTouchTarget()
                                .semantics { role = Role.Button }
                                .testTag("pinKey-biometric"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = stringResource(R.string.biometric_fingerprint_cd),
                                    tint = if (bioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f).heightIn(min = 56.dp))
                    }

                    KeypadButton("0", enabled = keypadEnabled, modifier = Modifier.weight(1f)) { handleDigit("0") }

                    Surface(
                        onClick = { handleBackspace() },
                        enabled = keypadEnabled,
                        shape = RecordShapes.Large,
                        color = if (keypadEnabled) patientFamily.container else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (keypadEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .minimumTouchTarget()
                            .semantics { role = Role.Button }
                        .testTag("pinKey-backspace"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.keypad_backspace_cd),
                                tint = if (keypadEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            if (isBiometricSupported && settingsState.isBiometricEnabled && !setupMode && !legacyChangeRequired) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { triggerBiometric() },
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("useBiometricButton"),
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (keypadEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.security_unlock_with_fingerprint),
                        style = RecordType.Action,
                        color = if (keypadEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RecordShapes.Large,
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .heightIn(min = 56.dp)
            .minimumTouchTarget()
            .semantics { role = Role.Button }
            .testTag("pinKey-$digit"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = RecordType.HeaderName,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
