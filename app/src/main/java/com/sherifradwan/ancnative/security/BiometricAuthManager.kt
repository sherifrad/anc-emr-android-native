package com.sherifradwan.ancnative.security

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

object BiometricAuthManager {

    fun isBiometricAvailable(context: Context): Boolean {
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: Activity,
        title: String = "Women's Health Clinic · Biometric Authentication",
        subtitle: String = "Touch the fingerprint sensor to unlock the application",
        onResult: (Boolean) -> Unit,
    ): CancellationSignal? {
        if (!isBiometricAvailable(activity)) {
            onResult(false)
            return null
        }

        val cancellationSignal = CancellationSignal()
        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton("Cancel & Use PIN", activity.mainExecutor) { _, _ ->
                    onResult(false)
                }
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
                )
                .build()

            prompt.authenticate(
                cancellationSignal,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onResult(false)
                    }

                    override fun onAuthenticationFailed() {
                        // User can retry touching the sensor
                    }
                },
            )
            return cancellationSignal
        } catch (_: Throwable) {
            onResult(false)
            return null
        }
    }
}
