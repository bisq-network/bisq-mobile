package network.bisq.mobile.presentation.ui.uicases.startup

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import network.bisq.mobile.client.httpclient.BisqProxyOption
import network.bisq.mobile.client.httpclient.exception.PasswordIncorrectOrMissingException
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.client.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.data.repository.SensitiveSettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.NetworkUtils.isPrivateIPv4
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
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val kmpTorService: KmpTorService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
) : BasePresenter(mainPresenter) {

    companion object {
        const val LOCALHOST = "localhost"
        const val ANDROID_LOCALHOST = "10.0.2.2"
        const val IPV4_EXAMPLE = "192.168.1.10"

        val publicDomains = Regex("""^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$""", RegexOption.IGNORE_CASE)
        val commonLocalNetworkSuffixes = setOf(".local", ".lan", ".internal")
    }

    // Must not be injected in constructor as node has not defined the WebSocketClientProvider dependency
    // Better would be that this presenter and screen is only instantiated in client
    // See https://github.com/bisq-network/bisq-mobile/issues/684
    private val wsClientService: WebSocketClientService by inject()

    private val _wsClientConnectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val wsClientConnectionState = _wsClientConnectionState.asStateFlow()

    private val _apiUrl = MutableStateFlow("")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _proxyHost = MutableStateFlow("127.0.0.1")
    val proxyHost: StateFlow<String> = _proxyHost.asStateFlow()

    private val _proxyPort = MutableStateFlow("9050")
    val proxyPort: StateFlow<String> = _proxyPort.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    val isNewApiUrl: StateFlow<Boolean> =
        combine(sensitiveSettingsRepository.data, apiUrl) { settings, newUrl ->
            settings.bisqApiUrl.isNotBlank() && settings.bisqApiUrl != newUrl
        }.stateIn(presenterScope, SharingStarted.Lazily, false)

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedProxyOption = MutableStateFlow(BisqProxyOption.NONE)
    val selectedProxyOption = _selectedProxyOption.asStateFlow()

    val isApiUrlValid: StateFlow<Boolean> =
        combine(apiUrl, selectedProxyOption) { apiUrl, proxyOption ->
            validateApiUrl(apiUrl, proxyOption) == null
        }.stateIn(presenterScope, SharingStarted.Lazily, false)

    val isProxyUrlValid: StateFlow<Boolean> = combine(
        proxyHost,
        proxyPort,
        selectedProxyOption
    ) { h, p, proxyOption ->
        if (proxyOption == BisqProxyOption.EXTERNAL_TOR || proxyOption == BisqProxyOption.SOCKS_PROXY)
            validateProxyHost(h) == null &&
                    validatePort(p) == null
        else true
    }.stateIn(presenterScope, SharingStarted.Lazily, false)

    val apiUrlPrompt: StateFlow<String> = selectedProxyOption.map {
        when (it) {
            BisqProxyOption.INTERNAL_TOR, BisqProxyOption.EXTERNAL_TOR ->
                "mobile.trustedNodeSetup.host.prompt".i18n()

            else -> if (BuildConfig.IS_DEBUG) {
                "http://${localHost()}:8090"
            } else {
                "http://$IPV4_EXAMPLE:8090"
            }
        }
    }.stateIn(presenterScope, SharingStarted.Lazily, "")

    val torState: StateFlow<KmpTorService.TorState> =
        kmpTorService.state.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            KmpTorService.TorState.Stopped()
        )

    val torProgress: StateFlow<Int> =
        kmpTorService.bootstrapProgress.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            0
        )

    private val _userExplicitlyChangedProxy = MutableStateFlow(false)

    private val _timeoutCounter = MutableStateFlow(0L)
    val timeoutCounter = _timeoutCounter.asStateFlow()

    // Track ongoing connect attempt and countdown to support cancellation
    private var connectJob: Job? = null
    private var countdownJob: Job? = null

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        if (BuildConfig.IS_DEBUG) {
            _apiUrl.value = "http://" + localHost() + ":8090"
        }

        launchUI {
            try {
                val settings = withContext(Dispatchers.IO) {
                    sensitiveSettingsRepository.fetch()
                }
                _password.value = settings.bisqApiPassword
                _selectedProxyOption.value = settings.selectedProxyOption
                if (settings.bisqApiUrl.isBlank()) {
                    if (apiUrl.value.isNotBlank()) onApiUrlChanged(apiUrl.value)
                } else {
                    onApiUrlChanged(settings.bisqApiUrl)
                }
                if (settings.externalProxyUrl.isBlank()) {
                    if (_proxyHost.value.isNotBlank()) onProxyHostChanged(_proxyHost.value)
                } else {
                    val parts = settings.externalProxyUrl.split(':', limit = 2)
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

    fun onApiUrlChanged(apiUrl: String) {
        _apiUrl.value = apiUrl
    }

    fun onProxyHostChanged(host: String) {
        _proxyHost.value = host
    }

    fun onProxyPortChanged(port: String) {
        _proxyPort.value = port
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun onProxyOptionChanged(value: BisqProxyOption) {
        _selectedProxyOption.value = value
        _userExplicitlyChangedProxy.value = true
    }

    fun onTestAndSavePressed(isWorkflow: Boolean) {
        if (connectJob != null) return // already connecting

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
        _status.value = "mobile.trustedNodeSetup.status.settingUpConnection".i18n()

        val newApiUrlString = apiUrl.value
        log.d { "Test: $newApiUrlString isWorkflow $isWorkflow" }
        val newApiUrl = parseAndNormalizeUrl(newApiUrlString)

        connectJob = launchUI {
            if (newApiUrl == null) {
                onConnectionError(
                    IllegalArgumentException("mobile.trustedNodeSetup.apiUrl.invalid.format".i18n()),
                    newApiUrlString
                )
                _isLoading.value = false
                connectJob = null
                return@launchUI
            }
            try {
                val newProxyHost: String?
                val newProxyPort: Int?
                val newProxyIsTor: Boolean
                val newProxyOption = selectedProxyOption.value
                val password = _password.value
                when (newProxyOption) {
                    BisqProxyOption.INTERNAL_TOR -> {
                        if (kmpTorService.state.value !is KmpTorService.TorState.Started) {
                            val started = kmpTorService.startTor()
                            if (!started) {
                                val startError =
                                    (kmpTorService.state.value as? KmpTorService.TorState.Stopped)?.error
                                        ?: IllegalStateException("Failed to start Tor")
                                throw startError
                            }
                        }
                        newProxyHost = "127.0.0.1"
                        newProxyPort = kmpTorService.awaitSocksPort()
                        newProxyIsTor = true
                    }

                    BisqProxyOption.EXTERNAL_TOR -> {
                        newProxyHost = proxyHost.value
                        newProxyPort = proxyPort.value.toIntOrNull()
                        newProxyIsTor = true
                    }

                    BisqProxyOption.SOCKS_PROXY -> {
                        newProxyHost = proxyHost.value
                        newProxyPort = proxyPort.value.toIntOrNull()
                        newProxyIsTor = false
                    }

                    BisqProxyOption.NONE -> {
                        newProxyHost = null
                        newProxyPort = null
                        newProxyIsTor = false
                    }
                }

                val isExternalProxy =
                    newProxyOption == BisqProxyOption.EXTERNAL_TOR || newProxyOption == BisqProxyOption.SOCKS_PROXY

                val error = if (isExternalProxy && newProxyPort == null) {
                    IllegalArgumentException("mobile.trustedNodeSetup.proxyPort.invalid".i18n())
                } else {
                    val timeoutSecs = wsClientService.determineTimeout(newApiUrl.host) / 1000
                    countdownJob = launchUI {
                        for (i in timeoutSecs downTo 0) {
                            _timeoutCounter.value = i
                            delay(1000)
                        }
                    }
                    _status.value = "mobile.trustedNodeSetup.status.connecting".i18n()
                    _wsClientConnectionState.value = ConnectionState.Connecting
                    val result = wsClientService.testConnection(
                        newApiUrl,
                        newProxyHost,
                        newProxyPort,
                        newProxyIsTor,
                        password,
                    )
                    countdownJob?.cancel()
                    result
                }

                if (error != null) {
                    _wsClientConnectionState.value = ConnectionState.Disconnected(error)
                    throw error
                } else {
                    // we only dispose client if we are sure new settings differ from the old one
                    // because it wont emit if they are the same, and new clients wont be instantiated
                    val currentSettings = sensitiveSettingsRepository.fetch()
                    val updatedSettings = currentSettings.copy(
                        bisqApiUrl = newApiUrl.toString(),
                        externalProxyUrl = when (newProxyOption) {
                            BisqProxyOption.EXTERNAL_TOR,
                            BisqProxyOption.SOCKS_PROXY -> "$newProxyHost:$newProxyPort"
                            else -> ""
                        },
                        selectedProxyOption = newProxyOption,
                        bisqApiPassword = password,
                    )
                    if (currentSettings != updatedSettings) {
                        wsClientService.disposeClient()
                        // we need to do it in 1 update to not trigger unnecessary flow emits
                        sensitiveSettingsRepository.update { updatedSettings }
                    }
                    val error = wsClientService.connect() // waits till new clients are initialized
                    if (error != null) {
                        _wsClientConnectionState.value = ConnectionState.Disconnected(error)
                        throw error
                    }
                    // wait till connectionState is changed to a final state
                    wsClientService.connectionState.filter { it !is ConnectionState.Connecting }
                        .first()
                    _wsClientConnectionState.value = ConnectionState.Connected // successful test regardless of final state
                    _status.value = "mobile.trustedNodeSetup.status.connected".i18n()
                    if (currentSettings.bisqApiUrl != updatedSettings.bisqApiUrl) {
                        log.d { "user setup a new trusted node $newApiUrl" }
                        userRepository.clear()
                    }

                    if (newProxyOption != BisqProxyOption.INTERNAL_TOR) {
                        try {
                            kmpTorService.stopTor()
                        } catch (e: Exception) {
                            log.w(e) { "Failed to stop Tor after switching proxy option" }
                        }
                    }

                    // change the states before going back
                    applicationBootstrapFacade.setState("mobile.bootstrap.connectedToTrustedNode".i18n())
                    applicationBootstrapFacade.setProgress(1.0f)
                    navigateToSplashScreen() // to trigger navigateToNextScreen again
                }
            } catch (e: TimeoutCancellationException) {
                // timeout should be handled as an error (not a user cancellation)
                onConnectionError(e, newApiUrl.toNormalizedString())
                currentCoroutineContext().ensureActive()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // user cancelled: do not show error, just reset state
                _wsClientConnectionState.value = ConnectionState.Disconnected()
                _status.value = ""
            } catch (e: Throwable) {
                onConnectionError(e, newApiUrl.toNormalizedString())
                currentCoroutineContext().ensureActive()
            } finally {
                countdownJob?.cancel()
                countdownJob = null
                _isLoading.value = false
                connectJob = null
            }
        }
    }

    fun onCancelPressed() {
        // cancel ongoing connect attempt and revert to idle state
        countdownJob?.cancel()
        connectJob?.cancel()

        // If using INTERNAL_TOR and Tor is still bootstrapping, stop it to avoid inconsistent state on next attempt
        if (selectedProxyOption.value == BisqProxyOption.INTERNAL_TOR && kmpTorService.state.value is KmpTorService.TorState.Starting) {
            launchIO {
                try {
                    kmpTorService.stopTor()
                } catch (e: Exception) {
                    log.w(e) { "Failed to stop Tor on cancel" }
                }
            }
        }

        _wsClientConnectionState.value = ConnectionState.Disconnected()
        _status.value = ""
        _isLoading.value = false
        _timeoutCounter.value = 0
        connectJob = null
    }

    private fun Url.toNormalizedString(): String {
        return "${this.protocol.name}://${this.host}:${this.port}"
    }

    private fun onConnectionError(error: Throwable, newApiUrl: String) {
        when (error) {
            is TimeoutCancellationException -> {
                log.e(error) { "Connection test timed out" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n())
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            }

            is IncompatibleHttpApiVersionException -> {
                log.d { "Invalid version cannot connect" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.incompatible".i18n())
                _status.value = "mobile.trustedNodeSetup.status.invalidVersion".i18n()
            }

            is PasswordIncorrectOrMissingException -> {
                _status.value = "mobile.trustedNodeSetup.status.passwordIncorrectOrMissing".i18n()
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

    private fun navigateToSplashScreen() {
        launchUI {
            navigateTo(NavRoute.Splash) {
                it.popUpTo(NavRoute.Splash) { inclusive = true }
            }
        }
    }

    private fun parseAndNormalizeUrl(value: String): Url? {
        val raw = value.trim()
        val withScheme = if (raw.contains("://")) raw else "http://$raw"
        val first = parseUrl(withScheme) ?: return null
        // Detect if user explicitly provided a port in input
        val hasExplicitPort = Regex("^https?://[^/]+:\\d+").containsMatchIn(withScheme)
        val host = first.host
        val needsDefaultPort = !hasExplicitPort && (
            host == LOCALHOST || host.isValidIpv4() || host.endsWith(".onion", ignoreCase = true)
        )
        val port = if (needsDefaultPort) 8090 else first.port
        // Normalize to protocol://host:port (drop any path/query as before)
        val normalized = "${first.protocol.name}://$host:$port"
        return parseUrl(normalized)
    }

    fun validateApiUrl(value: String, proxyOption: BisqProxyOption): String? {
        if (value.isEmpty()) {
            if (!_userExplicitlyChangedProxy.value && proxyOption == BisqProxyOption.INTERNAL_TOR) {
                _selectedProxyOption.value = BisqProxyOption.NONE
            }
            return "mobile.trustedNodeSetup.apiUrl.invalid.empty".i18n()
        }

        if (value == "demo.bisq") return null

        val apiUrl = parseAndNormalizeUrl(value)

        if (apiUrl == null) {
            return "mobile.trustedNodeSetup.apiUrl.invalid.format".i18n()
        }

        if (apiUrl.protocol != URLProtocol.HTTP && apiUrl.protocol != URLProtocol.HTTPS) {
            return "mobile.trustedNodeSetup.apiUrl.forbidden.protocol".i18n()
        }

        if (apiUrl.host == "localhost") {
            return null
        }

        if (!proxyOption.isTorProxyOption && apiUrl.host.endsWith(".onion")) {
            _selectedProxyOption.value = BisqProxyOption.INTERNAL_TOR
        }

        if (apiUrl.host.endsWith(".onion")) {
            return if (!apiUrl.host.isValidTorV3Address()) {
                "mobile.trustedNodeSetup.host.onion.invalid".i18n()
            } else {
                null
            }
        } else if (!_userExplicitlyChangedProxy.value && proxyOption == BisqProxyOption.INTERNAL_TOR) {
            _selectedProxyOption.value = BisqProxyOption.NONE
        }

        if (apiUrl.host.isValidIpv4() && !apiUrl.host.isPrivateIPv4() && apiUrl.protocol != URLProtocol.HTTPS) {
            return "mobile.trustedNodeSetup.apiUrl.forbidden.clearnet".i18n()
        }

        if (!apiUrl.host.isValidIpv4() &&
            publicDomains.matches(apiUrl.host) &&
            commonLocalNetworkSuffixes.none { apiUrl.host.contains(it) } &&
            apiUrl.protocol != URLProtocol.HTTPS
        ) {
            return "mobile.trustedNodeSetup.apiUrl.forbidden.clearnet".i18n()
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
            return "mobile.trustedNodeSetup.proxyHost.invalid.empty".i18n()
        }
        if (value == "localhost") {
            return null
        }
        if (!value.isValidIpv4()) {
            return "mobile.trustedNodeSetup.host.ip.invalid".i18n()
        }
        return null
    }
}
