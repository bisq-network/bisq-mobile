package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import network.bisq.mobile.client.httpclient.NetworkType
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.client.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidPort
import network.bisq.mobile.domain.utils.NetworkUtils.isValidTorV3Address
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import org.koin.core.component.inject

/**
 * Presenter for the Trusted Node Setup screen.
 */
class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
) : BasePresenter(mainPresenter) {

    companion object {
        const val LOCALHOST = "localhost"
        const val ANDROID_LOCALHOST = "10.0.2.2"
        const val IPV4_EXAMPLE = "192.168.1.10"
    }

    // Must not be injected in constructor as node has not defined the WebSocketClientProvider dependency
    // Better would be that this presenter and screen is only instantiated in client
    // See https://github.com/bisq-network/bisq-mobile/issues/684
    private val wsClientService: WebSocketClientService by inject()

    private val _wsClientConnectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val wsClientConnectionState = _wsClientConnectionState.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> get() = _host.asStateFlow()

    private val _port = MutableStateFlow("8090")
    val port: StateFlow<String> get() = _port.asStateFlow()

    private val _proxyHost = MutableStateFlow("127.0.0.1")
    val proxyHost: StateFlow<String> get() = _proxyHost.asStateFlow()

    private val _proxyPort = MutableStateFlow("9050")
    val proxyPort: StateFlow<String> get() = _proxyPort.asStateFlow()

    val isNewApiUrl: StateFlow<Boolean> = combine(settingsRepository.data, host, port) { settings, h, p ->
        val newApiUrl = "$h:$p"
        settings.bisqApiUrl.isNotBlank() && settings.bisqApiUrl != newApiUrl
    }.stateIn(presenterScope, SharingStarted.Lazily, false)

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> get() = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _selectedNetworkType = MutableStateFlow(NetworkType.LAN)
    val selectedNetworkType: StateFlow<NetworkType> get() = _selectedNetworkType.asStateFlow()

    val hostPrompt: StateFlow<String> = selectedNetworkType.map {
        if (selectedNetworkType.value == NetworkType.LAN) {
            if (BuildConfig.IS_DEBUG) {
                localHost()
            } else {
                IPV4_EXAMPLE
            }
        } else {
            "mobile.trustedNodeSetup.host.prompt".i18n()
        }
    }.stateIn(presenterScope, SharingStarted.Lazily, "")

    private val _useExternalProxy = MutableStateFlow(false)
    val useExternalProxy: StateFlow<Boolean> get() = _useExternalProxy.asStateFlow()

    val isApiUrlValid: StateFlow<Boolean> = combine(host, port, selectedNetworkType) { h, p, networkType ->
        validateHost(h, networkType) == null &&
                validatePort(p) == null
    }.stateIn(presenterScope, SharingStarted.Lazily, false)

    val isProxyUrlValid: StateFlow<Boolean> = combine(proxyHost, proxyPort, useExternalProxy, selectedNetworkType) { h, p, useExternal, networkType ->
        if (networkType == NetworkType.TOR && useExternal) validateProxyHost(h) == null &&
                validatePort(p) == null
        else true
    }.stateIn(presenterScope, SharingStarted.Lazily, false)

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        if (BuildConfig.IS_DEBUG) {
            _host.value = localHost()
        }

        launchUI {
            try {
                val settings = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                _selectedNetworkType.value = settings.selectedNetworkType
                if (settings.bisqApiUrl.isBlank()) {
                    if (_host.value.isNotBlank()) onHostChanged(_host.value)
                } else {
                    val parts = settings.bisqApiUrl.split(':', limit = 2)
                    val savedHost = parts.getOrNull(0)?.trim().orEmpty()
                    val savedPort = parts.getOrNull(1)?.trim().orEmpty()
                    onHostChanged(savedHost)
                    if (savedPort.isNotBlank()) onPortChanged(savedPort)
                }
                _useExternalProxy.value = settings.useExternalProxy
                if (settings.proxyUrl.isBlank()) {
                    if (_proxyHost.value.isNotBlank()) onProxyHostChanged(_proxyHost.value)
                } else {
                    val parts = settings.proxyUrl.split(':', limit = 2)
                    val savedHost = parts.getOrNull(0)?.trim().orEmpty()
                    val savedPort = parts.getOrNull(1)?.trim().orEmpty()
                    onProxyHostChanged(savedHost)
                    if (savedPort.isNotBlank()) onProxyPortChanged(savedPort)
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onHostChanged(host: String) {
        _host.value = host
    }

    fun onPortChanged(port: String) {
        _port.value = port
    }

    fun onNetworkType(value: NetworkType) {
        _selectedNetworkType.value = value
    }

    fun onProxyHostChanged(host: String) {
        _proxyHost.value = host
    }

    fun onProxyPortChanged(port: String) {
        _proxyPort.value = port
    }

    fun onUseExternalProxyChanged(value: Boolean) {
        _useExternalProxy.value = value
    }

    fun onTestAndSavePressed(isWorkflow: Boolean) {
        if (!isWorkflow) {
            // TODO implement feature to allow changing from settings
            // this is not trivial from UI perspective, its making NavGraph related code to crash when
            // landing back in the TabContainer Home.
            // We could warn the user and do an app restart (but we need a consistent solution for iOS too)
            showSnackbar("mobile.trustedNodeSetup.testConnection.message".i18n())
            return
        }

        if (!isApiUrlValid.value || !isProxyUrlValid.value) return
        _isLoading.value = true
        _status.value = "mobile.trustedNodeSetup.status.connecting".i18n()
        log.d { "Test: ${host.value} isWorkflow $isWorkflow" }

        launchIO {
            try {
                val newHost = host.value
                val newPort = port.value.toIntOrNull()
                val newApiUrl = "$newHost:$newPort"
                val newProxyHost = proxyHost.value
                val newProxyPort = proxyPort.value.toIntOrNull()
                // TODO: this will be refactored with next PR when kmptor is used when useExternalProxy is false
                val useExternalProxy = selectedNetworkType.value == NetworkType.TOR && useExternalProxy.value

                val error = if (newPort == null) {
                    IllegalArgumentException("Invalid port value was provided")
                } else if (useExternalProxy && newProxyPort == null) {
                    IllegalArgumentException("Invalid proxy port value was provided")
                } else {
                    if (useExternalProxy) {
                        wsClientService.testConnection(
                            newHost,
                            newPort,
                            newProxyHost,
                            newProxyPort,
                            true,
                        )
                    } else {
                        wsClientService.testConnection(
                            newHost,
                            newPort,
                        )
                    }
                }

                if (error != null) {
                    _wsClientConnectionState.value = ConnectionState.Disconnected(error)
                    onConnectionError(error, newApiUrl)
                } else {
                    // we only dispose client if we are sure new settings differ from the old one
                    // because it wont emit if they are the same, and new clients wont be instantiated
                    val currentSettings = settingsRepository.fetch()
                    // the only reason next line is correct is that we disable inputs for the duration of this procedure
                    val updatedSettings = transformSettingsWithPresenterValues(currentSettings)
                    if (currentSettings != updatedSettings) {
                        wsClientService.disposeClient()
                        // we need to do it in 1 update to not trigger unnecessary flow emits
                        settingsRepository.update { updatedSettings }
                    }
                    val error = wsClientService.connect() // waits till new clients are initialized
                    if (error != null) {
                        _wsClientConnectionState.value = ConnectionState.Disconnected(error)
                        onConnectionError(error, newApiUrl)
                        return@launchIO
                    }
                    // wait till connectionState is changed to a final state
                    wsClientService.connectionState.filter { it !is ConnectionState.Connecting }
                        .first()
                    _wsClientConnectionState.value =
                        ConnectionState.Connected // this is a successful test regardless of final state
                    _status.value = "mobile.trustedNodeSetup.status.connected".i18n()
                    val previousUrl = currentSettings.bisqApiUrl
                    if (previousUrl != newApiUrl) {
                        log.d { "user setup a new trusted node $newApiUrl" }
                        userRepository.clear()
                    }

                    navigateToSplashScreen() // to trigger navigateToNextScreen again
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun onConnectionError(error: Throwable, newApiUrl: String) {
        when (error) {
            is TimeoutCancellationException -> {
                log.e(error) { "Connection test timed out after 15 seconds" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n())
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            }

            is IncompatibleHttpApiVersionException -> {
                log.d { "Invalid version cannot connect" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.incompatible".i18n())
                _status.value = "mobile.trustedNodeSetup.status.invalidVersion".i18n()
            }

            else -> {
                if (error::class.simpleName == "ConnectException" || error::class.simpleName == "SocketException") {
                    showSnackbar(
                        "mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect".i18n(
                            newApiUrl
                        )
                    )
                } else {
                    val errorMessage = error.message
                    if (errorMessage != null) {
                        showSnackbar(
                            "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n(
                                errorMessage
                            )
                        )
                    } else {
                        showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.unknownError".i18n())
                    }
                }
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            }
        }
    }

    /**
     * transforms the given settings and updates it's fields using presenter's state
     */
    private fun transformSettingsWithPresenterValues(settings: Settings): Settings {
        val newBisqUrl = host.value + ":" + port.value
        val selectedNetworkType = selectedNetworkType.value
        val useExternalProxy = selectedNetworkType == NetworkType.TOR && useExternalProxy.value
        val newProxyUrl = proxyHost.value + ":" + proxyPort.value
        return settings.copy(
            bisqApiUrl = newBisqUrl,
            proxyUrl = newProxyUrl,
            isProxyUrlTor = true, // we only support tor proxy for now
            selectedNetworkType = selectedNetworkType,
            useExternalProxy = useExternalProxy,
        )
    }

    private fun navigateToSplashScreen() {
        launchUI {
            navigateTo(NavRoute.Splash) {
                it.popUpTo(NavRoute.Splash) { inclusive = true }
            }
        }
    }

    fun onSave() {
        if (!isApiUrlValid.value || !isProxyUrlValid.value) {
            showSnackbar("mobile.trustedNodeSetup.status.failed".i18n())
            return
        }
        launchUI {
            withContext(IODispatcher) {
                settingsRepository.update(::transformSettingsWithPresenterValues)
            }
            navigateBack()
        }
    }

    fun validateHost(value: String, networkType: NetworkType): String? {
        if (value.isEmpty()) {
            return "mobile.trustedNodeSetup.host.invalid.empty".i18n()
        }

        if (value == "demo.bisq") return null

        if (networkType == NetworkType.LAN) {
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

    private fun localHost(): String {
        return if (isIOS()) LOCALHOST else ANDROID_LOCALHOST
    }

    fun validateProxyHost(value: String): String? {
        if (value.isEmpty()) {
            return "mobile.trustedNodeSetup.host.invalid.empty".i18n()
        }
        if (!value.isValidIpv4()) {
            return "mobile.trustedNodeSetup.host.ip.invalid".i18n()
        }
        return null
    }
}
