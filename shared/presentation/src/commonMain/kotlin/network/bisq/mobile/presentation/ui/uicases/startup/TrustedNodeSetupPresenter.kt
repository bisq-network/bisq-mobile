package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidPort
import network.bisq.mobile.domain.utils.NetworkUtils.isValidTorV3Address
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider
) : BasePresenter(mainPresenter) {

    companion object {
        const val SAFEGUARD_TEST_TIMEOUT = 20000L
    }

    enum class NetworkType(val displayString: String) {
        LAN("mobile.trustedNodeSetup.networkType.lan".i18n()),
        TOR("mobile.trustedNodeSetup.networkType.tor".i18n())
    }

    private val _isApiUrlValid = MutableStateFlow(true)
    val isApiUrlValid: StateFlow<Boolean> get() = _isApiUrlValid.asStateFlow()

    private val _isBisqApiVersionValid = MutableStateFlow(true)
    val isBisqApiVersionValid: StateFlow<Boolean> get() = _isBisqApiVersionValid.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> get() = _host.asStateFlow()

    private val _port = MutableStateFlow("8090")
    val port: StateFlow<String> get() = _port.asStateFlow()

    private val _hostPrompt = MutableStateFlow("10.0.2.2")
    val hostPrompt: StateFlow<String> get() = _hostPrompt.asStateFlow()

    private val _trustedNodeVersion = MutableStateFlow("")
    val trustedNodeVersion: StateFlow<String> get() = _trustedNodeVersion.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> get() = _status.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> get() = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _selectedNetworkType = MutableStateFlow(NetworkType.LAN)
    val selectedNetworkType: StateFlow<NetworkType> get() = _selectedNetworkType.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        updateHostPrompt()
        if (BuildConfig.IS_DEBUG) {
            _host.value = "10.0.2.2"
        }

        launchUI {
            try {
                val data = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                data?.let {
                    onHostChanged(
                        if (it.bisqApiUrl.isBlank() && _host.value.isNotBlank())
                            _host.value
                        else it.bisqApiUrl
                    )
                    validateVersion()
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    fun onHostChanged(host: String) {
        _host.value = host
        validateApiUrl()
    }

    fun onPortChanged(port: String) {
        _port.value = port
        validateApiUrl()
    }

    fun onNetworkType(value: NetworkType) {
        _selectedNetworkType.value = value
        updateHostPrompt()
        validateApiUrl()
    }

    suspend fun isNewApiUrl(): Boolean {
        var isNewApiUrl = false
        settingsRepository.fetch()?.let {
            val newApiUrl = _host.value + ":" + _port.value
            if (it.bisqApiUrl.isNotBlank() && it.bisqApiUrl != newApiUrl) {
                isNewApiUrl = true
            }
        }
        return isNewApiUrl
    }

    fun testConnection(isWorkflow: Boolean) {
        if (!isWorkflow) {
            // TODO implement feature to allow changing from settings
            // this is not trivial from UI perspective, its making NavGraph related code to crash when
            // landing back in the TabContainer Home.
            showSnackbar("mobile.trustedNodeSetup.testConnection.message".i18n())
            return
        }
        _isLoading.value = true
        _status.value = "mobile.trustedNodeSetup.status.connecting".i18n()
        log.d { "Test: ${_host.value} isWorkflow $isWorkflow" }

        val connectionJob = launchUI {
            try {
                // Add a timeout to prevent indefinite waiting
                val success = withTimeout(15000) { // 15 second timeout
                    withContext(IODispatcher) {
                        val portValue = port.value.toIntOrNull() ?: return@withContext false
                        return@withContext webSocketClientProvider.testClient(host.value, portValue)
                    }
                }

                if (success) {
                    val previousUrl = settingsRepository.fetch()?.bisqApiUrl
                    val isCompatibleVersion = withContext(IODispatcher) {
                        updateSettings()
                        delay(DEFAULT_DELAY)
                        webSocketClientProvider.get().await()
                        validateVersion()
                    }

                    if (isCompatibleVersion) {
                        _isConnected.value = true
                        _status.value = "mobile.trustedNodeSetup.status.connected".i18n()

                        val newApiUrl = _host.value + ":" + _port.value
                        if (previousUrl != newApiUrl) {
                            log.d { "user setup a new trusted node $newApiUrl" }
                            withContext(IODispatcher) {
                                userRepository.fetch()?.let {
                                    userRepository.delete(it)
                                }
                            }
                        } else if (!isWorkflow) {
                            navigateBack()
                        }
                    } else {
                        webSocketClientProvider.get().disconnect(isTest = true)
                        log.d { "Invalid version cannot connect" }
                        showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.incompatible".i18n())
                        _isConnected.value = false
                        _status.value = "mobile.trustedNodeSetup.status.invalidVersion".i18n()
                    }
                } else {
                    showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect".i18n(_host.value))
                    _isConnected.value = false
                    _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
                }
            } catch (e: TimeoutCancellationException) {
                log.e(e) { "Connection test timed out after 15 seconds" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n())
                _isConnected.value = false
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            } catch (e: Exception) {
                val errorMessage = e.message
                log.e(e) { "Error testing connection: $errorMessage" }
                if (errorMessage != null) {
                    showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n(errorMessage))
                } else {
                    showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.unknownError".i18n())
                }

                _isConnected.value = false
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            } finally {
                _isLoading.value = false
            }
        }

        launchUI {
            delay(SAFEGUARD_TEST_TIMEOUT) // 20 seconds as a fallback
            if (_isLoading.value) {
                log.w { "Force stopping connection test after 20 seconds" }
                connectionJob.cancel()
                _isLoading.value = false
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTookTooLong".i18n())
            }
        }
    }

    private suspend fun updateSettings() {
        val currentSettings = settingsRepository.fetch()
        val updatedSettings = Settings().apply {
            bisqApiUrl = _host.value + ":" + _port.value
            firstLaunch = currentSettings?.firstLaunch ?: true
        }
        settingsRepository.update(updatedSettings)
    }

    fun navigateToCreateProfile() {
        launchUI { navigateTo(Routes.CreateProfile) }
    }

    fun onSave() {
        launchUI {
            updateSettings()
            navigateBack()
        }
    }

    private suspend fun validateVersion(): Boolean {
        _trustedNodeVersion.value = settingsServiceFacade.getTrustedNodeVersion()
        if (settingsServiceFacade.isApiCompatible()) {
            _isBisqApiVersionValid.value = true
            return true
        } else {
            _isBisqApiVersionValid.value = false
            return false
        }
    }

    private fun updateHostPrompt() {
        if (selectedNetworkType.value == NetworkType.LAN) {
            if (BuildConfig.IS_DEBUG) {
                _hostPrompt.value = "10.0.2.2"
            } else {
                _hostPrompt.value = "192.168.1.10"
            }
        } else {
            _hostPrompt.value = "mobile.trustedNodeSetup.host.prompt".i18n()
        }
    }

    fun validateHost(value: String): String? {
        if (value.isEmpty()) {
            return "mobile.trustedNodeSetup.host.invalid.empty".i18n()
        }
        if (selectedNetworkType.value == NetworkType.LAN) {
            // We only support IPv4 as we only support LAN addresses
            if (!value.isValidIpv4()) {
                return "mobile.trustedNodeSetup.host.ip.invalid".i18n()
            }
        } else if (!value.isValidTorV3Address()) {
            return "mobile.trustedNodeSetup.host.onion.invalid".i18n()
        }

        return null
    }

    fun validatePort(value: String): String? {
        if (value.isEmpty()) {
            return "mobile.trustedNodeSetup.host.invalid.empty".i18n()
        }
        if (!value.isValidPort()) {
            return "mobile.trustedNodeSetup.port.invalid".i18n()
        }
        return null
    }

    private fun validateApiUrl() {
        _isApiUrlValid.value = validateHost(host.value) == null &&
                validatePort(port.value) == null
    }
}
