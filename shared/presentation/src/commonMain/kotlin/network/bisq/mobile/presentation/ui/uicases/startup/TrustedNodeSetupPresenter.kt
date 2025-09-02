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

/**
 * Presenter for the Trusted Node Setup screen.
 */
class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider
) : BasePresenter(mainPresenter) {

    companion object {
        const val SAFEGUARD_TEST_TIMEOUT = 10000L
        const val LOCALHOST = "localhost"
        const val ANDROID_LOCALHOST = "10.0.2.2"
        const val IPV4_EXAMPLE = "192.168.1.10"
    }

    enum class NetworkType(private val i18nKey: String) {
        LAN("mobile.trustedNodeSetup.networkType.lan"),
        TOR("mobile.trustedNodeSetup.networkType.tor");
        val displayString: String get() = i18nKey.i18n()
    }

    private val _isApiUrlValid = MutableStateFlow(true)
    val isApiUrlValid: StateFlow<Boolean> get() = _isApiUrlValid.asStateFlow()

    private val _isBisqApiVersionValid = MutableStateFlow(true)
    val isBisqApiVersionValid: StateFlow<Boolean> get() = _isBisqApiVersionValid.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> get() = _host.asStateFlow()

    private val _port = MutableStateFlow("8090")
    val port: StateFlow<String> get() = _port.asStateFlow()

    private val _hostPrompt = MutableStateFlow(
        if (BuildConfig.IS_DEBUG) localHost() else IPV4_EXAMPLE
    )
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
        // Reset connection state when view is attached to ensure proper initial state
        _isConnected.value = false
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        updateHostPrompt()
        if (BuildConfig.IS_DEBUG) {
            _host.value = localHost()
            validateApiUrl()
        }

        launchUI {
            try {
                val data = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                data?.let {
                    if (it.bisqApiUrl.isBlank()) {
                        if (_host.value.isNotBlank()) onHostChanged(_host.value)
                    } else {
                        val parts = it.bisqApiUrl.split(':', limit = 2)
                        val savedHost = parts.getOrNull(0)?.trim().orEmpty()
                        val savedPort = parts.getOrNull(1)?.trim().orEmpty()
                        onHostChanged(savedHost)
                        if (savedPort.isNotBlank()) onPortChanged(savedPort)
                    }
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
            // We could warn the user and do an app restart (but we need a consistent solution for iOS too)
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
                        webSocketClientProvider.testClient(host.value, portValue)
                    }
                }

                if (success) {
                    val previousUrl = withContext(IODispatcher) { settingsRepository.fetch()?.bisqApiUrl }
                    val isCompatibleVersion = withContext(IODispatcher) {
                        webSocketClientProvider.get().await()
                        validateVersion()
                    }

                    if (isCompatibleVersion) {
                        withContext(IODispatcher) { updateSettings() }
                        _isConnected.value = true
                        _status.value = "mobile.trustedNodeSetup.status.connected".i18n()

                        val newApiUrl = _host.value + ":" + _port.value
                        if (previousUrl != newApiUrl) {
                            log.d { "user setup a new trusted node $newApiUrl" }
                            withContext(IODispatcher) {
                                userRepository.clear()
                            }
                        }

                        if (isWorkflow) {
                            // Compare current host/port with saved settings to decide navigation
                            val currentApiUrl = "${_host.value}:${_port.value}"
                            if (previousUrl.isNullOrBlank() || previousUrl != currentApiUrl) {
                                // No saved URL or different URL -> create profile
                                navigateToCreateProfile()
                            } else {
                                // Same URL as saved -> go to home
                                navigateToHome()
                            }
                        } else {
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
                    val endpoint = "${_host.value}:${_port.value}"
                    showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect".i18n(endpoint))
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
            delay(SAFEGUARD_TEST_TIMEOUT) // 10 seconds as a fallback
            if (_isLoading.value) {
                log.w { "Force stopping connection test after 10 seconds" }
                connectionJob.cancel()
                _isLoading.value = false
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTookTooLong".i18n())
            }
        }
    }

    private suspend fun updateSettings() {
        val newUrl = _host.value + ":" + _port.value
        settingsRepository.setBisqApiUrl(newUrl)
    }

    fun navigateToCreateProfile() {
        launchUI {
            navigateTo(Routes.CreateProfile) {
                it.popUpTo(Routes.TrustedNodeSetup.name) { inclusive = true }
            }
        }
    }

    private fun navigateToHome() {
        launchUI {
            navigateTo(Routes.TabContainer) {
                it.popUpTo(Routes.TrustedNodeSetup.name) { inclusive = true }
            }
        }
    }

    fun onSave() {
        if (!_isApiUrlValid.value) {
            showSnackbar("mobile.trustedNodeSetup.status.failed".i18n())
            return
        }
        launchUI {
            withContext(IODispatcher) { updateSettings() }
            navigateBack()
        }
    }

    private suspend fun validateVersion(): Boolean {
        val version = withContext(IODispatcher) { settingsServiceFacade.getTrustedNodeVersion() }
        val compatible = withContext(IODispatcher) { settingsServiceFacade.isApiCompatible() }
        _trustedNodeVersion.value = version
        _isBisqApiVersionValid.value = compatible
        return compatible
    }

    private fun updateHostPrompt() {
        if (selectedNetworkType.value == NetworkType.LAN) {
            if (BuildConfig.IS_DEBUG) {
                _hostPrompt.value = localHost()
            } else {
                _hostPrompt.value = IPV4_EXAMPLE
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
            // Accept "localhost" on any platform; on Android, normalize it to 10.0.2.2 (emulator host).
            val normalized = if (value.equals(LOCALHOST, ignoreCase = true) && !isIOS()) {
                ANDROID_LOCALHOST
            } else value
            if (normalized.equals(localHost(), ignoreCase = true)) return null
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
            return "mobile.trustedNodeSetup.port.invalid.empty".i18n()
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

    private fun localHost(): String {
        return if (isIOS()) LOCALHOST else ANDROID_LOCALHOST
    }
}
