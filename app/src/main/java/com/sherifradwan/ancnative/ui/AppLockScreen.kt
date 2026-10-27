package com.sherifradwan.ancnative.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.security.BiometricAuthManager
import com.sherifradwan.ancnative.security.SecuritySettingsRepository

@Composable
fun AppLockScreen(
    securityRepo: SecuritySettingsRepository,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settingsState = securityRepo.settings.value

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricAttemptedOnLaunch by remember { mutableStateOf(false) }

    val isBiometricSupported = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    // Auto-launch biometric prompt on start if biometric is enabled
    LaunchedEffect(Unit) {
        if (!biometricAttemptedOnLaunch && settingsState.isBiometricEnabled && isBiometricSupported && activity != null) {
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

    // Validate PIN whenever enteredPin reaches 6 digits
    fun handleDigit(digit: String) {
        if (enteredPin.length < 6) {
            val updated = enteredPin + digit
            enteredPin = updated
            errorMessage = null

            if (updated.length == 6) {
                if (securityRepo.validatePin(updated)) {
                    securityRepo.unlockApp()
                    onUnlockSuccess()
                } else {
                    errorMessage = "Incorrect PIN passcode. Please try again."
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
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
                        errorMessage = "Fingerprint verification failed. Use your PIN."
                    }
                },
            )
        } else {
            errorMessage = "Fingerprint not available on this device."
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("appLockScreen"),
    ) {
        val horizontalPadding = maxOf(24.dp, (maxWidth - 440.dp) / 2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Clinic Logo & Branding Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 3.dp,
                modifier = Modifier.size(90.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.clinic_logo_foreground),
                        contentDescription = "Clinic Logo",
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = settingsState.clinicName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = settingsState.clinicSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Enter Security PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(14.dp))

            // 6-Dot PIN Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("pinDotsRow"),
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredPin.length
                    val scale by animateFloatAsState(if (isFilled) 1.15f else 1.0f, label = "dotScale")
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (errorMessage != null) MaterialTheme.colorScheme.error
                                else if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .background(
                                if (isFilled) {
                                    if (errorMessage != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                } else Color.Transparent,
                            )
                            .testTag("pinDot-$i"),
                    )
                }
            }

            // Error message banner
            Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Numeric Keypad (3x4 Grid)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Row 1: 1, 2, 3
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("1", modifier = Modifier.weight(1f)) { handleDigit("1") }
                    KeypadButton("2", modifier = Modifier.weight(1f)) { handleDigit("2") }
                    KeypadButton("3", modifier = Modifier.weight(1f)) { handleDigit("3") }
                }

                // Row 2: 4, 5, 6
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("4", modifier = Modifier.weight(1f)) { handleDigit("4") }
                    KeypadButton("5", modifier = Modifier.weight(1f)) { handleDigit("5") }
                    KeypadButton("6", modifier = Modifier.weight(1f)) { handleDigit("6") }
                }

                // Row 3: 7, 8, 9
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KeypadButton("7", modifier = Modifier.weight(1f)) { handleDigit("7") }
                    KeypadButton("8", modifier = Modifier.weight(1f)) { handleDigit("8") }
                    KeypadButton("9", modifier = Modifier.weight(1f)) { handleDigit("9") }
                }

                // Row 4: Biometric, 0, Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Biometric button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isBiometricSupported && settingsState.isBiometricEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("pinKey-biometric")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = isBiometricSupported) { triggerBiometric() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint",
                                tint = if (isBiometricSupported && settingsState.isBiometricEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    // Digit 0
                    KeypadButton("0", modifier = Modifier.weight(1f)) { handleDigit("0") }

                    // Backspace button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("pinKey-backspace")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { handleBackspace() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Direct Biometric Link Action
            if (isBiometricSupported && settingsState.isBiometricEnabled) {
                TextButton(
                    onClick = { triggerBiometric() },
                    modifier = Modifier.testTag("useBiometricButton"),
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Unlock with Fingerprint",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .height(60.dp)
            .testTag("pinKey-$digit")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
