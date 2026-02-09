package network.bisq.mobile.client.trusted_node_setup

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeConnectionStatus
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeSetupUseCase
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Presenter for the Trusted Node Setup screen.
 */
class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val kmpTorService: KmpTorService,
    private val trustedNodeSetupUseCase: TrustedNodeSetupUseCase,
    private val apiAccessService: ApiAccessService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(TrustedNodeSetupUiState())
    val uiState: StateFlow<TrustedNodeSetupUiState> = _uiState.asStateFlow()

    private var connectJob: Job? = null
    private var countdownJob: Job? = null

    private var pairingQrCode: PairingQrCode? = null

    private var isWorkflow: Boolean = true

    override fun onViewAttached() {
        super.onViewAttached()
        observeFlows()
    }

    suspend fun initialize(isWorkflow: Boolean) {
        this.isWorkflow = isWorkflow
        if (!isWorkflow) {
            val settings = sensitiveSettingsRepository.fetch()
            _uiState.update {
                it.copy(
                    apiUrl = settings.bisqApiUrl,
                    status = TrustedNodeConnectionStatus.Connected,
                )
            }
        }
    }

    private fun observeFlows() {
        presenterScope.launch {
            trustedNodeSetupUseCase.state.collect { state ->
                when (state.connectionStatus) {
                    is TrustedNodeConnectionStatus.IncompatibleHttpApiVersion -> {
                        _uiState.update {
                            it.copy(
                                status = state.connectionStatus,
                                serverVersion = state.serverVersion,
                            )
                        }
                    }

                    else -> {
                        _uiState.update { it.copy(status = state.connectionStatus) }
                    }
                }
            }
        }

        // Observe Tor service flows
        presenterScope.launch {
            kmpTorService.state.collect { torState ->
                _uiState.update { it.copy(torState = torState) }
            }
        }

        presenterScope.launch {
            kmpTorService.bootstrapProgress.collect { torProgress ->
                _uiState.update { it.copy(torProgress = torProgress) }
            }
        }
    }

    fun onAction(action: TrustedNodeSetupUiAction) {
        when (action) {
            is TrustedNodeSetupUiAction.OnPairingCodeChange -> onPairingCodeChanged(action.value)
            is TrustedNodeSetupUiAction.OnTestAndSavePress -> onTestAndSavePressed()
            TrustedNodeSetupUiAction.OnCancelPress -> onCancelPressed()
            TrustedNodeSetupUiAction.OnShowQrCodeView -> {
                _uiState.update { it.copy(showQrCodeView = true) }
            }

            TrustedNodeSetupUiAction.OnQrCodeViewDismiss -> {
                _uiState.update { it.copy(showQrCodeView = false) }
            }

            TrustedNodeSetupUiAction.OnQrCodeFail -> {
                _uiState.update { it.copy(showQrCodeView = false, showQrCodeError = true) }
            }

            TrustedNodeSetupUiAction.OnQrCodeErrorClose -> {
                _uiState.update { it.copy(showQrCodeError = false) }
            }

            is TrustedNodeSetupUiAction.OnQrCodeResult -> onQrCodeResult(action.value)
        }
    }

    private fun onQrCodeResult(value: String) {
        onPairingCodeChanged(value)
    }

    private fun onPairingCodeChanged(
        pairingCode: String,
    ) {
        val trimPairingCode = pairingCode.trim()
        if (trimPairingCode.isBlank()) {
            _uiState.update {
                it.copy(
                    pairingCodeEntry = DataEntry(),
                    apiUrl = EMPTY_STRING,
                    showQrCodeView = false,
                    status = TrustedNodeConnectionStatus.Idle,
                )
            }
        } else {
            apiAccessService
                .getPairingCodeQr(trimPairingCode)
                .onSuccess { code ->
                    pairingQrCode = code
                    _uiState.update {
                        it.copy(
                            pairingCodeEntry = it.pairingCodeEntry.updateValue(trimPairingCode),
                            apiUrl = code.restApiUrl,
                            showQrCodeView = false,
                            status = TrustedNodeConnectionStatus.Idle,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            pairingCodeEntry = it.pairingCodeEntry.updateValueWithError(trimPairingCode, error.message),
                            apiUrl = EMPTY_STRING,
                            showQrCodeView = false,
                            status = TrustedNodeConnectionStatus.Idle,
                        )
                    }
                }
        }
    }

    private fun onTestAndSavePressed() {
        val pairingQrCode = pairingQrCode
        if (connectJob != null || pairingQrCode == null) return

        // Start a general countdown for the entire operation (Tor + pairing + connection)
        // This provides visual feedback and enables the Cancel button
        val totalTimeoutSecs = 120L // 2 minutes for the entire operation
        countdownJob?.cancel()
        countdownJob =
            presenterScope.launch {
                for (i in totalTimeoutSecs downTo 0) {
                    _uiState.update {
                        it.copy(timeoutCounter = i)
                    }
                    delay(1000)
                }
            }

        connectJob =
            presenterScope.launch {
                val isSetupSuccess = trustedNodeSetupUseCase.execute(pairingQrCode)
                if (isSetupSuccess) {
                    navigateToSplashScreen()
                }
                countdownJob?.cancel()
                countdownJob = null
                connectJob = null
            }
    }

    private fun onCancelPressed() {
        // cancel ongoing connect attempt and revert to idle state
        countdownJob?.cancel()
        connectJob?.cancel()

        // If Tor is still bootstrapping, stop it to avoid inconsistent state on next attempt
        if (kmpTorService.state.value is KmpTorService.TorState.Starting) {
            presenterScope.launch {
                try {
                    kmpTorService.stopTor()
                } catch (e: Exception) {
                    log.w(e) { "Failed to stop Tor on cancel" }
                }
            }
        }

        _uiState.update {
            it.copy(
                status = TrustedNodeConnectionStatus.Idle,
                timeoutCounter = 0,
            )
        }
        connectJob = null
    }

    private fun navigateToSplashScreen() {
        presenterScope.launch {
            navigateTo(NavRoute.Splash) {
                it.popUpTo(NavRoute.Splash) { inclusive = true }
            }
        }
    }
}
