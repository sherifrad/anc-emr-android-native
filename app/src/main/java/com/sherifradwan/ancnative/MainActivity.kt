package com.sherifradwan.ancnative

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sherifradwan.ancnative.security.AppThemeMode
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.state.AncViewModel
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.storage.VaultAutoAction
import com.sherifradwan.ancnative.storage.vaultAutoAction
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AncViewModel
    private val securityRepo by lazy { SecuritySettingsRepository.getInstance(this) }

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        viewModel = ViewModelProvider(this)[AncViewModel::class.java]
        viewModel.onForegrounded()
        enableEdgeToEdge()
        setContent {
            val securitySettings = securityRepo.settings.collectAsStateWithLifecycle().value
            val isDarkTheme = when (securitySettings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                )
                onDispose {}
            }
            AncTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                val state = viewModel.state.collectAsStateWithLifecycle().value
                val appUnlocked = !securitySettings.isAppLocked && !securitySettings.needsPinSetup
                val authenticating = state.vaultState as? VaultState.Authenticating
                LaunchedEffect(authenticating?.request?.id) {
                    authenticating?.request?.let { request ->
                        viewModel.completeAuthentication(request)
                    }
                }
                LaunchedEffect(securitySettings.isAppLocked) {
                    if (securitySettings.isAppLocked) viewModel.lockVault()
                }
                LaunchedEffect(state.vaultState, appUnlocked) {
                    when (vaultAutoAction(state.vaultState, appUnlocked)) {
                        VaultAutoAction.BEGIN_UNLOCK -> viewModel.beginUnlock()
                        VaultAutoAction.BEGIN_INITIALIZATION -> viewModel.beginInitialization()
                        VaultAutoAction.NONE -> Unit
                    }
                }
                AncApp(
                    state = state,
                    onEvent = viewModel::dispatch,
                    onStartVisit = viewModel::startVisit,
                    onSaveVisit = viewModel::saveVisit,
                    onInitialize = viewModel::beginInitialization,
                    onUnlock = viewModel::beginUnlock,
                    onLock = {
                        securityRepo.lockApp()
                    },
                    onRetryStorage = viewModel::retryStorageInspection,
                    securityRepo = securityRepo,
                    onExportBackup = viewModel::exportBackup,
                    onRestoreBackup = viewModel::restoreBackup,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (com.sherifradwan.ancnative.security.shouldLockOnReturn(securityRepo.backgroundTimeMs(), System.currentTimeMillis())) {
            securityRepo.lockApp()
        }
        viewModel.onForegrounded()
    }

    override fun onStop() {
        super.onStop()
        securityRepo.recordBackgroundTime(System.currentTimeMillis())
        viewModel.onBackgrounded(authenticationOverlayActive = true)
    }
}
