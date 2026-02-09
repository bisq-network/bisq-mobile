package network.bisq.mobile.client.trusted_node_setup.use_case

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.utils.ApiAccessUtil.getProxyOptionFromRestUrl
import network.bisq.mobile.client.common.domain.access.utils.ApiAccessUtil.parseAndNormalizeUrl
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import kotlin.coroutines.cancellation.CancellationException

class PairingRequestFailedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ExternalProxyNotSupportedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class TrustedNodeSetupUseCase(
    private val kmpTorService: KmpTorService,
    private val httpClientService: HttpClientService,
    private val apiAccessService: ApiAccessService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val wsClientService: WebSocketClientService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
) : Logging {
    private val _state = MutableStateFlow(TrustedNodeSetupUseCaseState())
    val state = _state.asStateFlow()

    private data class ProxySettings(
        val host: String?,
        val port: Int?,
        val isTor: Boolean,
    )

    companion object {
        private const val PAIRING_TIMEOUT_SECONDS = 60L
        private const val HTTP_CLIENT_UPDATE_TIMEOUT_MS = 5000L
    }

    suspend fun execute(pairingQrCode: PairingQrCode): Boolean {
        try {
            updateState(TrustedNodeConnectionStatus.SettingUpConnection)

            val newApiUrl = parseAndNormalizeUrl(pairingQrCode.restApiUrl)
            if (newApiUrl == null) {
                updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.apiUrl.invalid.format"))
                return false
            }

            apiAccessService.updateSettings(pairingQrCode)

            val proxySettings = determineProxySettings(pairingQrCode, newApiUrl.host)
            val (clientId, sessionId) = ensurePairingCredentials(pairingQrCode)
            val tlsFingerprint = pairingQrCode.tlsFingerprint

            updateState(TrustedNodeConnectionStatus.Connecting)

            val testError =
                wsClientService.testConnection(
                    apiUrl = newApiUrl,
                    tlsFingerprint = tlsFingerprint,
                    clientId = clientId,
                    sessionId = sessionId,
                    proxyHost = proxySettings.host,
                    proxyPort = proxySettings.port,
                    isTorProxy = proxySettings.isTor,
                )

            if (testError != null) {
                return handleError(testError, pairingQrCode.restApiUrl)
            }

            val connectError = wsClientService.connect()
            if (connectError != null) {
                return handleError(connectError, pairingQrCode.restApiUrl)
            }

            wsClientService
                .connectionState
                .first { it !is ConnectionState.Connecting }

            updateState(TrustedNodeConnectionStatus.Connected)

            if (!proxySettings.isTor) {
                try {
                    kmpTorService.stopTor()
                } catch (e: Exception) {
                    log.w(e) { "Failed to stop Tor after switching proxy option" }
                }
            }

            applicationBootstrapFacade.setState("mobile.bootstrap.connectedToTrustedNode".i18n())
            applicationBootstrapFacade.setProgress(1.0f)

            return true
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            return handleError(e, pairingQrCode.restApiUrl)
        }
    }

    private fun updateState(
        status: TrustedNodeConnectionStatus,
        serverVersion: String? = null,
    ) {
        _state.update {
            it.copy(
                connectionStatus = status,
                serverVersion = serverVersion ?: it.serverVersion,
            )
        }
    }

    private suspend fun setupTorProxyIfNeeded(): ProxySettings {
        log.d { "Using INTERNAL_TOR proxy, checking Tor state: ${kmpTorService.state.value}" }
        // Start Tor if not already started and wait for it to be fully bootstrapped
        if (kmpTorService.state.value !is KmpTorService.TorState.Started) {
            updateState(TrustedNodeConnectionStatus.StartingTor)

            log.d { "Tor not started, attempting to start..." }
            val started = kmpTorService.startTor()

            if (!started) {
                val torError = (kmpTorService.state.value as? KmpTorService.TorState.Stopped)?.error
                val startError = torError ?: IllegalStateException("Failed to start Tor")
                log.e(startError) { "Failed to start Tor" }
                throw startError
            }
            log.d { "Tor started, waiting for bootstrap to complete..." }
            // Wait for Tor to be fully bootstrapped (100%)
            updateState(TrustedNodeConnectionStatus.BootstrappingTor)
            kmpTorService.bootstrapProgress.filter { it >= 100 }.first()
            log.d { "Tor bootstrap complete (100%)" }
        }

        val proxyHost = "127.0.0.1"
        val proxyPort = kmpTorService.awaitSocksPort()
        log.d { "Using Tor proxy at $proxyHost:$proxyPort" }

        // Wait for HTTP client to be updated with Tor proxy settings
        // Use timeout to avoid hanging forever if the flow doesn't emit
        val httpClientUpdated =
            withTimeoutOrNull(HTTP_CLIENT_UPDATE_TIMEOUT_MS) {
                httpClientService.httpClientChangedFlow.first()
            }
        if (httpClientUpdated != null) {
            log.d { "HTTP client updated with Tor proxy settings: $httpClientUpdated" }
        } else {
            log.w { "Timeout waiting for HTTP client update, proceeding anyway" }
        }

        return ProxySettings(host = proxyHost, port = proxyPort, isTor = true)
    }

    private suspend fun determineProxySettings(
        pairingQrCode: PairingQrCode,
        apiUrl: String,
    ): ProxySettings {
        val proxyOption = getProxyOptionFromRestUrl(pairingQrCode.restApiUrl)
        log.d { "Proxy option: $proxyOption for URL: $apiUrl" }

        return when (proxyOption) {
            BisqProxyOption.INTERNAL_TOR -> {
                setupTorProxyIfNeeded()
            }

            BisqProxyOption.EXTERNAL_TOR,
            BisqProxyOption.SOCKS_PROXY,
            -> {
                // External proxy options are not supported in pairing flow
                // They can only be configured via automatic detection
                log.w { "External proxy options not supported in pairing flow: $proxyOption" }
                throw ExternalProxyNotSupportedException("External proxy options not supported in pairing flow")
            }

            BisqProxyOption.NONE -> {
                log.d { "No proxy configured" }
                ProxySettings(host = null, port = null, isTor = false)
            }
        }
    }

    private suspend fun ensurePairingCredentials(
        pairingQrCode: PairingQrCode,
    ): Pair<String?, String?> {
        val settings = sensitiveSettingsRepository.fetch()
        val shouldRequestPairing =
            settings.clientId == null || settings.sessionId == null || settings.bisqApiUrl != pairingQrCode.restApiUrl
        var clientId = settings.clientId
        var sessionId = settings.sessionId

        // If clientId or sessionId is null, we need to request pairing first
        // This must happen AFTER Tor is started so the HTTP client uses the correct proxy
        if (shouldRequestPairing) {
            updateState(TrustedNodeConnectionStatus.RequestingPairing)
            log.d { "ClientId or SessionId is null, requesting pairing..." }

            val pairingResult =
                withTimeout(PAIRING_TIMEOUT_SECONDS * 1000) {
                    apiAccessService.requestPairing(pairingQrCode)
                }
            pairingResult
                .onSuccess { pairingResponse ->
                    log.d { "Pairing request successful" }
                    clientId = pairingResponse.clientId
                    sessionId = pairingResponse.sessionId
                }.onFailure { error ->
                    log.e("Pairing request failed: ${error.message}")
                    throw PairingRequestFailedException("Pairing request failed", error)
                }
        }

        return Pair(clientId, sessionId)
    }

    private fun handleError(
        error: Throwable,
        restApiUrl: String,
    ): Boolean {
        when (error) {
            is PairingRequestFailedException -> {
                updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.status.pairingRequestFailed"))
            }

            is ExternalProxyNotSupportedException -> {
                updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.error.externalProxyNotSupported"))
            }

            is TimeoutCancellationException -> {
                updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut"))
            }

            is CancellationException -> {
                updateState(TrustedNodeConnectionStatus.Idle)
            }

            is IncompatibleHttpApiVersionException -> {
                log.d { "Invalid version cannot connect" }
                updateState(
                    TrustedNodeConnectionStatus.IncompatibleHttpApiVersion,
                    error.serverVersion,
                )
            }

            is UnauthorizedApiAccessException -> {
                updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.status.passwordIncorrectOrMissing"))
            }

            else -> {
                if (error::class.simpleName == "ConnectException" || error::class.simpleName == "SocketException") {
                    updateState(
                        TrustedNodeConnectionStatus.Failed(
                            "mobile.trustedNodeSetup.connectionJob.messages.couldNotConnect",
                            restApiUrl,
                        ),
                    )
                } else {
                    val errorMessage = error.message
                    if (errorMessage != null) {
                        updateState(
                            TrustedNodeConnectionStatus.Failed(
                                "mobile.trustedNodeSetup.connectionJob.messages.connectionError",
                                errorMessage,
                            ),
                        )
                    } else {
                        updateState(TrustedNodeConnectionStatus.Failed("mobile.trustedNodeSetup.connectionJob.messages.unknownError"))
                    }
                }
            }
        }
        return false
    }
}
