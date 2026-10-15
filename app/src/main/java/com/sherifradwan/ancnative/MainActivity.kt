package com.sherifradwan.ancnative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.state.AncViewModel
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AncViewModel
    private val securityRepo by lazy { SecuritySettingsRepository.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AncViewModel::class.java]
        viewModel.onForegrounded()
        enableEdgeToEdge()
        setContent {
            AncTheme(darkTheme = false, dynamicColor = false) {
                val state = viewModel.state.collectAsStateWithLifecycle().value
                android.util.Log.d("VAULT_DEBUG", "Current vaultState: ${state.vaultState}")
                val authenticating = state.vaultState as? VaultState.Authenticating
                LaunchedEffect(authenticating?.request?.id) {
                    authenticating?.request?.let { request ->
                        viewModel.completeAuthentication(request)
                    }
                }
                LaunchedEffect(state.vaultState) {
                    when (state.vaultState) {
                        is VaultState.Locked -> viewModel.beginUnlock()
                        is VaultState.Uninitialized -> viewModel.beginInitialization()
                        is VaultState.Blocked -> viewModel.retryStorageInspection()
                        else -> Unit
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
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForegrounded()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onBackgrounded(authenticationOverlayActive = true)
    }
}
