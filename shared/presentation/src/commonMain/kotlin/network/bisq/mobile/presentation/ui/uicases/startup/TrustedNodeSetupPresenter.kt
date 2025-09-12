package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import network.bisq.mobile.client.network.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientService
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
    private val webSocketClientService: WebSocketClientService,
) : BasePresenter(mainPresenter) {

    companion object {
        const val LOCALHOST = "localhost"
        const val ANDROID_LOCALHOST = "10.0.2.2"
        const val IPV4_EXAMPLE = "192.168.1.10"
    }

    enum class NetworkType(private val i18nKey: String) {
        LAN("mobile.trustedNodeSetup.networkType.lan"),
        TOR("mobile.trustedNodeSetup.networkType.tor");

        val displayString: String get() = i18nKey.i18n()
    }

    private val _isBisqApiVersionValid = MutableStateFlow(true)
    val isBisqApiVersionValid: StateFlow<Boolean> get() = _isBisqApiVersionValid.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> get() = _host.asStateFlow()

    private val _port = MutableStateFlow("8090")
    val port: StateFlow<String> get() = _port.asStateFlow()

    private val _proxyHost = MutableStateFlow("127.0.0.1")
    val proxyHost: StateFlow<String> get() = _proxyHost.asStateFlow()

    private val _proxyPort = MutableStateFlow("9050")
    val proxyPort: StateFlow<String> get() = _proxyPort.asStateFlow()

    val isNewApiUrl: StateFlow<Boolean> = settingsRepository.data.map {
        val newApiUrl = _host.value + ":" + _port.value
        it.bisqApiUrl.isNotBlank() && it.bisqApiUrl != newApiUrl
    }.stateIn(presenterScope, SharingStarted.Lazily, false)

    val isApiUrlValid: StateFlow<Boolean> = host.combine(port) { h, p ->
        validateHost(h) == null &&
                validatePort(p) == null
    }.stateIn(presenterScope, SharingStarted.Eagerly, true)

    val isProxyUrlValid: StateFlow<Boolean> = proxyHost.combine(proxyPort) { h, p ->
        if (_useExternalTorProxy.value) validateProxyHost(_proxyHost.value) == null &&
                validatePort(_proxyPort.value) == null
        else true
    }.stateIn(presenterScope, SharingStarted.Eagerly, true)

    private val _hostPrompt = MutableStateFlow(
        if (BuildConfig.IS_DEBUG) localHost() else IPV4_EXAMPLE
    )
    val hostPrompt: StateFlow<String> get() = _hostPrompt.asStateFlow()

    private val _trustedNodeVersion = MutableStateFlow("")
    val trustedNodeVersion: StateFlow<String> get() = _trustedNodeVersion.asStateFlow()

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())

    val isConnected = _connectionState
        .map { it is ConnectionState.Connected }
        .stateIn(
            presenterScope,
            SharingStarted.Lazily,
            false,
        )

    val isLoading = _connectionState
        .map { it is ConnectionState.Connecting }
        .stateIn(
            presenterScope,
            SharingStarted.Lazily,
            false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val status: StateFlow<String> =
        _connectionState.mapLatest { state ->
            return@mapLatest when (state) {
                is ConnectionState.Connecting -> "mobile.trustedNodeSetup.status.connecting".i18n()
                is ConnectionState.Connected -> "mobile.trustedNodeSetup.status.connected".i18n()
                is ConnectionState.Disconnected -> {
                    if (state.error is IncompatibleHttpApiVersionException) {
                        "mobile.trustedNodeSetup.status.invalidVersion".i18n()
                    } else if (state.error != null) {
                        "mobile.trustedNodeSetup.status.failed".i18n()
                    } else {
                        ""
                    }
                }
            }
        }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            ""
        )

    private val _selectedNetworkType = MutableStateFlow(NetworkType.LAN)
    val selectedNetworkType: StateFlow<NetworkType> get() = _selectedNetworkType.asStateFlow()

    private val _useExternalTorProxy = MutableStateFlow(false)
    val useExternalTorProxy: StateFlow<Boolean> get() = _useExternalTorProxy.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        updateHostPrompt()
        if (BuildConfig.IS_DEBUG) {
            _host.value = localHost()
        }

        launchUI {
            try {
                val settings = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                if (settings.bisqApiUrl.isBlank()) {
                    if (_host.value.isNotBlank()) onHostChanged(_host.value)
                } else {
                    val parts = settings.bisqApiUrl.split(':', limit = 2)
                    val savedHost = parts.getOrNull(0)?.trim().orEmpty()
                    val savedPort = parts.getOrNull(1)?.trim().orEmpty()
                    onHostChanged(savedHost)
                    if (savedPort.isNotBlank()) onPortChanged(savedPort)
                }
                _useExternalTorProxy.value = settings.useExternalProxy
                if (settings.proxyUrl.isBlank()) {
                    if (_proxyHost.value.isNotBlank()) onTorProxyHostChanged(_proxyHost.value)
                } else {
                    val parts = settings.proxyUrl.split(':', limit = 2)
                    val savedHost = parts.getOrNull(0)?.trim().orEmpty()
                    val savedPort = parts.getOrNull(1)?.trim().orEmpty()
                    onTorProxyHostChanged(savedHost)
                    if (savedPort.isNotBlank()) onTorProxyPortChanged(savedPort)
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
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
        updateHostPrompt()
    }

    fun onTorProxyHostChanged(host: String) {
        _proxyHost.value = host
    }

    fun onTorProxyPortChanged(port: String) {
        _proxyPort.value = port
    }

    fun onUseExternalTorProxyChanged(value: Boolean) {
        _useExternalTorProxy.value = value
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

        log.d { "Test: ${_host.value} isWorkflow $isWorkflow" }

        if (!isApiUrlValid.value || !isProxyUrlValid.value) return

        launchUI {

            try {
                val newHost = _host.value
                val newPort = _port.value.toIntOrNull()
                val newApiUrl = "$newHost:$newPort"
                val newProxyHost = _proxyHost.value
                val newProxyPort = _proxyPort.value.toIntOrNull()
                val newTorProxyUrl = "$newProxyHost:$newProxyPort"
                val newUseExternalTorProxy = _useExternalTorProxy.value

                if (newPort == null || newProxyPort == null) return@launchUI

                _connectionState.value = ConnectionState.Connecting

                val error = webSocketClientService.testConnection(
                    newHost,
                    newPort,
                    newProxyHost,
                    newProxyPort,
                    newUseExternalTorProxy
                )

                if (error == null) {
                    _connectionState.value = ConnectionState.Connected
                } else {
                    _connectionState.value = ConnectionState.Disconnected(error)
                }

                if (error == null) {
                    val previousUrl =
                        withContext(IODispatcher) { settingsRepository.fetch().bisqApiUrl }

                    settingsRepository.update {
                        it.copy(
                            bisqApiUrl = newApiUrl,
                            proxyUrl = newTorProxyUrl,
                            isExternalProxyTor = newUseExternalTorProxy,
                        )
                    }
                    if (previousUrl != newApiUrl) {
                        log.d { "user setup a new trusted node $newApiUrl" }
                        withContext(IODispatcher) {
                            userRepository.clear()
                        }
                    }

                    if (isWorkflow) {
                        // Compare current host/port with saved settings to decide navigation
                        val currentApiUrl = "${_host.value}:${_port.value}"
                        if (previousUrl.isBlank() || previousUrl != currentApiUrl) {
                            // No saved URL or different URL -> create profile
                            navigateToCreateProfile()
                        } else {
                            // Same URL as saved -> go to home
                            navigateToHome()
                        }
                    } else {
                        navigateBack()
                    }
                } else if (error is TimeoutCancellationException) {
                    log.e(error) { "WS Connection test timed out" }
                    showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n())
                } else {
                    val errorMessage = error.message
                    if (error is IncompatibleHttpApiVersionException) {
                        showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.incompatible".i18n())
                    } else if (errorMessage != null) {
                        showSnackbar(
                            "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n(
                                errorMessage
                            )
                        )
                    } else {
                        showSnackbar(
                            "mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect".i18n(
                                "${_host.value}:${_port.value}"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // settings repository methods may throw
                val errorMessage = e.message
                log.e(e) { "Error testing connection: $errorMessage" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.unknownError".i18n())
            }
        }
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
        if (!isApiUrlValid.value || !isProxyUrlValid.value) {
            showSnackbar("mobile.trustedNodeSetup.status.failed".i18n())
            return
        }
        launchUI {
            withContext(IODispatcher) {
                settingsRepository.update {
                    it.copy(
                        bisqApiUrl = _host.value + ":" + _port.value,
                        proxyUrl = _proxyHost.value + ":" + _proxyPort.value,
                        isExternalProxyTor = _useExternalTorProxy.value,
                    )
                }
            }
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
        if (value != "demo.bisq") {
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
