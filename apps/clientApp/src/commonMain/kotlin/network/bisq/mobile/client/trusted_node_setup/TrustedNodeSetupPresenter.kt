package network.bisq.mobile.client.trusted_node_setup

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClient
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.NetworkUtils.isPrivateIPv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidPort
import network.bisq.mobile.domain.utils.NetworkUtils.isValidTorV3Address
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
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

        val publicDomains =
            Regex(
                """^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$""",
                RegexOption.IGNORE_CASE,
            )
        val commonLocalNetworkSuffixes = setOf(".local", ".lan", ".internal")
    }

    // Must not be injected in constructor as node has not defined the WebSocketClientProvider dependency
    // Better would be that this presenter and screen is only instantiated in client
    // See https://github.com/bisq-network/bisq-mobile/issues/684
    private val wsClientService: WebSocketClientService by inject()

    private val apiAccessService: ApiAccessService by inject()

    private val httpClientService: HttpClientService by inject()

    private val _wsClientConnectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val wsClientConnectionState = _wsClientConnectionState.asStateFlow()

    val clientName: StateFlow<String> = apiAccessService.clientName
    val pairingQrCodeString: StateFlow<String> =
        apiAccessService.pairingQrCodeString
    val restApiUrl: StateFlow<String> = apiAccessService.restApiUrl
    val pairingResult: StateFlow<Result<PairingResponse>?> =
        apiAccessService.pairingResult
    val pairingResultPersisted: StateFlow<Boolean> =
        apiAccessService.pairingResultStored
    val pairingCodeError: StateFlow<String?> =
        apiAccessService.pairingCodeError

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isPairingInProgress = MutableStateFlow(true)
    val isPairingInProgress: StateFlow<Boolean> =
        _isPairingInProgress.asStateFlow()

    private val _selectedProxyOption = MutableStateFlow(BisqProxyOption.NONE)
    val selectedProxyOption = _selectedProxyOption.asStateFlow()

    val torState: StateFlow<KmpTorService.TorState> =
        kmpTorService.state.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            KmpTorService.TorState.Stopped(),
        )

    val torProgress: StateFlow<Int> =
        kmpTorService.bootstrapProgress.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            0,
        )

    private val userExplicitlyChangedProxy = MutableStateFlow(false)

    private val _timeoutCounter = MutableStateFlow(0L)
    val timeoutCounter = _timeoutCounter.asStateFlow()

    private val _showQrCodeView = MutableStateFlow(false)
    val showQrCodeView: StateFlow<Boolean> = _showQrCodeView.asStateFlow()

    private val _showQrCodeError = MutableStateFlow(false)
    val showQrCodeError: StateFlow<Boolean> = _showQrCodeError.asStateFlow()

    private val _triggerApiUrlValidation = MutableStateFlow(0)
    val triggerApiUrlValidation = _triggerApiUrlValidation.asStateFlow()

    // Track ongoing connect attempt and countdown to support cancellation
    private var connectJob: Job? = null
    private var countdownJob: Job? = null

    // Backing flow to track sticky pairing completion state (once true, stays true)
    private val _pairingCompletedSticky = MutableStateFlow(false)

    val pairingCompleted: StateFlow<Boolean> =
        pairingResultPersisted
            .combine(pairingResult) { pairingResultPersisted, pairingResult ->
                val currentSuccess =
                    pairingResultPersisted && pairingResult != null && pairingResult.isSuccess

                // Make it sticky: once true, stays true
                if (currentSuccess) {
                    _pairingCompletedSticky.value = true
                }

                _pairingCompletedSticky.value
            }.stateIn(
                scope = presenterScope,
                started = SharingStarted.Lazily,
                initialValue = false,
            )

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        /*if (BuildConfig.IS_DEBUG) {
            _apiUrl.value = "http://" + localHost() + ":8090"
        }*/

        presenterScope.launch {
            try {
                val settings = sensitiveSettingsRepository.fetch()
                _selectedProxyOption.value = settings.selectedProxyOption
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            } finally {
                _isPairingInProgress.value = false
            }
        }
    }

    fun onPairingCodeChanged(value: String) {
        apiAccessService.setPairingQrCodeString(value)
        // Trigger automatic proxy detection for pasted pairing code
        detectAndSetProxyOption()
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

        _isPairingInProgress.value = true
        _status.value =
            "mobile.trustedNodeSetup.status.settingUpConnection".i18n()

        // Start a general countdown for the entire operation (Tor + pairing + connection)
        // This provides visual feedback and enables the Cancel button
        val totalTimeoutSecs = 120L // 2 minutes for the entire operation
        countdownJob?.cancel()
        countdownJob =
            presenterScope.launch {
                for (i in totalTimeoutSecs downTo 0) {
                    _timeoutCounter.value = i
                    delay(1000)
                }
            }

        val newApiUrlString = restApiUrl.value
        log.d { "Test: $newApiUrlString isWorkflow $isWorkflow" }
        val newApiUrl = parseAndNormalizeUrl(newApiUrlString)

        connectJob =
            presenterScope.launch {
                if (newApiUrl == null) {
                    onConnectionError(
                        IllegalArgumentException("mobile.trustedNodeSetup.apiUrl.invalid.format".i18n()),
                        newApiUrlString,
                    )
                    _isPairingInProgress.value = false
                    connectJob = null
                    return@launch
                }
                val tlsFingerprint = apiAccessService.tlsFingerprint.value
                var clientId = apiAccessService.clientId.value
                var sessionId = apiAccessService.sessionId.value

                try {
                    // Setup proxy FIRST before any network requests
                    // This ensures the HTTP client is configured correctly for onion URLs
                    val newProxyHost: String?
                    val newProxyPort: Int?
                    val newProxyIsTor: Boolean
                    val newProxyOption = selectedProxyOption.value
                    log.d { "Proxy option: $newProxyOption for URL: ${newApiUrl.host}" }
                    when (newProxyOption) {
                        BisqProxyOption.INTERNAL_TOR -> {
                            log.d { "Using INTERNAL_TOR proxy, checking Tor state: ${kmpTorService.state.value}" }
                            // Start Tor if not already started and wait for it to be fully bootstrapped
                            if (kmpTorService.state.value !is KmpTorService.TorState.Started) {
                                _status.value = "mobile.trustedNodeSetup.status.startingTor".i18n()
                                log.d { "Tor not started, attempting to start..." }
                                val started = kmpTorService.startTor()
                                if (!started) {
                                    val startError =
                                        (kmpTorService.state.value as? KmpTorService.TorState.Stopped)?.error
                                            ?: IllegalStateException("Failed to start Tor")
                                    log.e(startError) { "Failed to start Tor" }
                                    throw startError
                                }
                                log.d { "Tor started, waiting for bootstrap to complete..." }
                                // Wait for Tor to be fully bootstrapped (100%)
                                _status.value = "mobile.trustedNodeSetup.status.bootstrappingTor".i18n()
                                kmpTorService.bootstrapProgress.filter { it >= 100 }.first()
                                log.d { "Tor bootstrap complete (100%)" }
                            }
                            newProxyHost = "127.0.0.1"
                            newProxyPort = kmpTorService.awaitSocksPort()
                            newProxyIsTor = true
                            log.d { "Using Tor proxy at $newProxyHost:$newProxyPort" }

                            // Wait for HTTP client to be updated with Tor proxy settings
                            // Use timeout to avoid hanging forever if the flow doesn't emit
                            log.d { "Waiting for HTTP client to update with Tor proxy settings..." }
                            val httpClientUpdated =
                                withTimeoutOrNull(5000) {
                                    httpClientService.httpClientChangedFlow.first()
                                }
                            if (httpClientUpdated != null) {
                                log.d { "HTTP client updated with Tor proxy settings: $httpClientUpdated" }
                            } else {
                                log.w { "Timeout waiting for HTTP client update, proceeding anyway" }
                            }
                        }

                        BisqProxyOption.EXTERNAL_TOR,
                        BisqProxyOption.SOCKS_PROXY,
                        -> {
                            // External proxy options are not supported in pairing flow
                            // They can only be configured via automatic detection
                            log.w { "External proxy options not supported in pairing flow: $newProxyOption" }
                            showSnackbar("mobile.trustedNodeSetup.error.externalProxyNotSupported".i18n())
                            _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
                            _isPairingInProgress.value = false
                            connectJob = null
                            return@launch
                        }

                        BisqProxyOption.NONE -> {
                            newProxyHost = null
                            newProxyPort = null
                            newProxyIsTor = false
                            log.d { "No proxy configured" }
                        }
                    }

                    // If clientId or sessionId is null, we need to request pairing first
                    // This must happen AFTER Tor is started so the HTTP client uses the correct proxy
                    if (clientId == null || sessionId == null) {
                        _status.value = "mobile.trustedNodeSetup.status.requestingPairing".i18n()
                        log.d { "ClientId or SessionId is null, requesting pairing..." }

                        val pairingTimeoutSecs = 60L
                        withTimeout(pairingTimeoutSecs * 1000) {
                            // Trigger pairing request
                            apiAccessService.requestPairing()

                            // Wait for pairing result
                            val pairingResult =
                                apiAccessService.pairingResult
                                    .filterNotNull()
                                    .first()

                            if (pairingResult.isFailure) {
                                val pairingError = pairingResult.exceptionOrNull()
                                log.e(pairingError) { "Pairing request failed" }
                                throw pairingError ?: IllegalStateException("Pairing request failed")
                            }

                            log.d { "Pairing request successful" }
                            // Update clientId and sessionId from pairing response
                            clientId = apiAccessService.clientId.value
                            sessionId = apiAccessService.sessionId.value
                        }
                    }

                    _status.value =
                        "mobile.trustedNodeSetup.status.connecting".i18n()
                    _wsClientConnectionState.value =
                        ConnectionState.Connecting
                    val error =
                        wsClientService.testConnection(
                            apiUrl = newApiUrl,
                            tlsFingerprint = tlsFingerprint,
                            clientId = clientId,
                            sessionId = sessionId,
                            proxyHost = newProxyHost,
                            proxyPort = newProxyPort,
                            isTorProxy = newProxyIsTor,
                        )

                    if (error != null) {
                        _wsClientConnectionState.value =
                            ConnectionState.Disconnected(error)
                        throw error
                    } else {
                        // we only dispose client if we are sure new settings differ from the old one
                        // because it wont emit if they are the same, and new clients wont be instantiated
                        val currentSettings =
                            sensitiveSettingsRepository.fetch()
                        // Explicitly include port in URL to preserve non-default ports (e.g., :80 for HTTP)
                        // Ktor's Url.toString() drops default ports, which breaks QR code URLs with explicit ports
                        val apiUrlWithPort = "${newApiUrl.protocol.name}://${newApiUrl.host}:${newApiUrl.port}"
                        val updatedSettings =
                            currentSettings.copy(
                                bisqApiUrl = apiUrlWithPort,
                                tlsFingerprint = tlsFingerprint,
                                clientId = clientId,
                                sessionId = sessionId,
                                externalProxyUrl =
                                    when (newProxyOption) {
                                        BisqProxyOption.EXTERNAL_TOR,
                                        BisqProxyOption.SOCKS_PROXY,
                                        -> "$newProxyHost:$newProxyPort"

                                        else -> ""
                                    },
                                selectedProxyOption = newProxyOption,
                            )
                        if (currentSettings != updatedSettings) {
                            wsClientService.disposeClient()
                            // we need to do it in 1 update to not trigger unnecessary flow emits
                            sensitiveSettingsRepository.update { updatedSettings }
                        }
                        val error =
                            wsClientService.connect() // waits till new clients are initialized
                        if (error != null) {
                            _wsClientConnectionState.value =
                                ConnectionState.Disconnected(error)
                            throw error
                        }
                        // wait till connectionState is changed to a final state
                        wsClientService.connectionState
                            .filter { it !is ConnectionState.Connecting }
                            .first()
                        _wsClientConnectionState.value =
                            ConnectionState.Connected // successful test regardless of final state
                        _status.value =
                            "mobile.trustedNodeSetup.status.connected".i18n()
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
                } catch (e: CancellationException) {
                    // user cancelled: do not show error, just reset state
                    _wsClientConnectionState.value =
                        ConnectionState.Disconnected()
                    _status.value = ""
                } catch (e: Throwable) {
                    onConnectionError(e, newApiUrl.toNormalizedString())
                    currentCoroutineContext().ensureActive()
                } finally {
                    countdownJob?.cancel()
                    countdownJob = null
                    _isPairingInProgress.value = false
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
            presenterScope.launch {
                try {
                    kmpTorService.stopTor()
                } catch (e: Exception) {
                    log.w(e) { "Failed to stop Tor on cancel" }
                }
            }
        }

        _wsClientConnectionState.value = ConnectionState.Disconnected()
        _status.value = ""
        _isPairingInProgress.value = false
        _timeoutCounter.value = 0
        connectJob = null
    }

    private fun Url.toNormalizedString(): String = "${protocol.name}://$host:$port"

    private fun onConnectionError(
        error: Throwable,
        newApiUrl: String,
    ) {
        when (error) {
            is TimeoutCancellationException -> {
                log.e(error) { "Connection test timed out" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n())
                _status.value = "mobile.trustedNodeSetup.status.failed".i18n()
            }

            is IncompatibleHttpApiVersionException -> {
                log.d { "Invalid version cannot connect" }
                showSnackbar("mobile.trustedNodeSetup.connectionJob.messages.incompatible".i18n())
                _status.value =
                    "mobile.trustedNodeSetup.status.invalidVersion".i18n()
            }

            is UnauthorizedApiAccessException -> {
                _status.value =
                    "mobile.trustedNodeSetup.status.passwordIncorrectOrMissing".i18n()
            }

            else -> {
                if (error::class.simpleName == "ConnectException" || error::class.simpleName == "SocketException") {
                    showSnackbar(
                        "mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect".i18n(
                            newApiUrl,
                        ),
                    )
                } else {
                    val errorMessage = error.message
                    if (errorMessage != null) {
                        showSnackbar(
                            "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n(
                                errorMessage,
                            ),
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
        presenterScope.launch {
            navigateTo(NavRoute.Splash) {
                it.popUpTo(NavRoute.Splash) { inclusive = true }
            }
        }
    }

    private fun parseAndNormalizeUrl(value: String): Url? {
        val raw = value.trim()
        val withScheme = if (raw.contains("://")) raw else "http://$raw"
        val first =
            parseUrl(withScheme)
                ?: return null
        // Detect if user explicitly provided a port in input
        val hasExplicitPort =
            Regex("^https?://[^/]+:\\d+").containsMatchIn(withScheme)
        val host = first.host
        val needsDefaultPort =
            !hasExplicitPort && (
                host == LOCALHOST || host.isValidIpv4() ||
                    host.endsWith(
                        ".onion",
                        ignoreCase = true,
                    )
            )
        val port = if (needsDefaultPort) 8090 else first.port
        // Normalize to protocol://host:port (drop any path/query as before)
        val normalized = "${first.protocol.name}://$host:$port"
        return parseUrl(normalized)
    }

    fun validateApiUrl(
        value: String,
        proxyOption: BisqProxyOption,
    ): String? {
        if (value.isEmpty()) {
            if (!userExplicitlyChangedProxy.value && proxyOption == BisqProxyOption.INTERNAL_TOR) {
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
        } else if (!userExplicitlyChangedProxy.value && proxyOption == BisqProxyOption.INTERNAL_TOR) {
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

    private fun localHost(): String = if (isIOS()) LOCALHOST else ANDROID_LOCALHOST

    fun onShowQrCodeView() {
        _showQrCodeView.value = true
    }

    fun onQrCodeFailed() {
        _showQrCodeView.value = false
        _showQrCodeError.value = true
    }

    fun onQrCodeErrorClosed() {
        _showQrCodeError.value = false
    }

    fun onQrCodeViewDismissed() {
        _showQrCodeView.value = false
    }

    fun onQrCodeResult(value: String) {
        apiAccessService.setPairingQrCodeString(value)
        _showQrCodeView.value = false
        // Trigger automatic proxy detection for QR code
        detectAndSetProxyOption()
    }

    /**
     * Detects if the current API URL requires Tor proxy and sets the proxy option accordingly.
     * This is called after scanning a QR code or pasting a pairing code.
     */
    private fun detectAndSetProxyOption() {
        val url = apiAccessService.restApiUrl.value
        if (url.isNotBlank()) {
            val parsedUrl = parseAndNormalizeUrl(url)
            if (parsedUrl != null) {
                if (parsedUrl.host.endsWith(".onion")) {
                    // .onion URL requires Tor
                    if (selectedProxyOption.value != BisqProxyOption.INTERNAL_TOR) {
                        log.d { "Pairing code contains .onion URL, setting proxy to INTERNAL_TOR" }
                        _selectedProxyOption.value = BisqProxyOption.INTERNAL_TOR
                    }
                } else {
                    // Clearnet URL (including localhost/10.0.2.2) should use no proxy
                    if (selectedProxyOption.value != BisqProxyOption.NONE) {
                        log.d { "Pairing code contains clearnet URL, setting proxy to NONE" }
                        _selectedProxyOption.value = BisqProxyOption.NONE
                    }
                }
            }
        }
    }
}
