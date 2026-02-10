package network.bisq.mobile.client.common.domain.access

import androidx.annotation.CallSuper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.access.utils.ApiAccessUtil.getProxyOptionFromRestUrl
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

const val LOCALHOST = "localhost"
const val LOOPBACK = "127.0.0.1"
const val ANDROID_LOCALHOST = "10.0.2.2"

// Demo mode pairing code - when entered, triggers demo mode with fake data
const val DEMO_PAIRING_CODE = "BISQ_DEMO_PAIRING_CODE"

// Demo mode URLs that trigger WebSocketClientDemo
const val DEMO_API_URL = "http://demo.bisq:21"
const val DEMO_WS_URL = "ws://demo.bisq:21"

// Demo mode credentials - used for both in-memory state and persisted settings
private const val DEMO_CLIENT_ID = "demo-client-id"
private const val DEMO_CLIENT_SECRET = "demo-client-secret"
private const val DEMO_SESSION_ID = "demo-session-id"
private const val DEMO_PAIRING_ID = "demo-pairing-id"

class ApiAccessService(
    private val pairingService: PairingService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val httpClientService: HttpClientService,
    private val pairingQrCodeDecoder: PairingQrCodeDecoder,
) : ServiceFacade(),
    Logging {
    // Auto-generate client name from platform info
    private val _clientName = MutableStateFlow(generateClientName())
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    // Provided by qr code
    private val _pairingQrCodeString = MutableStateFlow("")
    val pairingQrCodeString: StateFlow<String> =
        _pairingQrCodeString.asStateFlow()

    private val _webSocketUrl = MutableStateFlow("")
    val webSocketUrl: StateFlow<String> = _webSocketUrl.asStateFlow()

    private val _restApiUrl = MutableStateFlow("")
    val restApiUrl: StateFlow<String> = _restApiUrl.asStateFlow()

    private val _tlsFingerprint: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val tlsFingerprint: StateFlow<String?> = _tlsFingerprint.asStateFlow()

    private val _pairingCodeId: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val pairingCodeId: StateFlow<String?> = _pairingCodeId.asStateFlow()

    private val _grantedPermissions: MutableStateFlow<Set<Permission>> =
        MutableStateFlow(emptySet())
    val grantedPermissions: StateFlow<Set<Permission>> =
        _grantedPermissions.asStateFlow()

    // Provided by pairing response
    private val _clientId: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val clientId: StateFlow<String?> = _clientId.asStateFlow()

    private val _clientSecret: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val clientSecret: StateFlow<String?> = _clientSecret.asStateFlow()

    private val _sessionId: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _pairingResult: MutableStateFlow<Result<PairingResponse>?> =
        MutableStateFlow(null)
    val pairingResult: StateFlow<Result<PairingResponse>?> =
        _pairingResult.asStateFlow()

    private var requestPairingJob: Job? = null

    private val _pairingCodeError: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val pairingCodeError: StateFlow<String?> = _pairingCodeError.asStateFlow()

    private val pairingQrCodeDataStored: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private val _pairingResultStored: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val pairingResultStored: StateFlow<Boolean> =
        _pairingResultStored.asStateFlow()

    @CallSuper
    override suspend fun activate() {
        super.activate()
        serviceScope.launch {
            try {
                val settings = sensitiveSettingsRepository.fetch()
                val bisqApiUrl = adaptLoopbackForAndroid(settings.bisqApiUrl)
                if (_webSocketUrl.value.isBlank() && bisqApiUrl.isNotBlank()) {
                    _webSocketUrl.value =
                        restApiUrlToWebSocketUrl(bisqApiUrl)
                }
                if (_restApiUrl.value.isBlank() && bisqApiUrl.isNotBlank()) {
                    _restApiUrl.value = bisqApiUrl
                }
                if (_tlsFingerprint.value == null) {
                    _tlsFingerprint.value = settings.tlsFingerprint
                }
                if (_clientId.value == null) {
                    _clientId.value = settings.clientId
                }
                if (_clientSecret.value == null) {
                    _clientSecret.value = settings.clientSecret
                }
                if (_sessionId.value == null) {
                    _sessionId.value = settings.sessionId
                }

                // todo apply other fields as well
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
        serviceScope.launch {
            pairingQrCodeDataStored.collect { pairingDataStored ->
                if (pairingDataStored) {
                    // Wait for HttpClientService to apply settings and create new client
                    val clientReady = httpClientService.awaitClientReady()
                    if (clientReady) {
                        log.d { "HTTP client ready, proceeding with pairing request" }
                        requestPairing()
                    } else {
                        log.w { "Timeout waiting for HTTP client, proceeding with pairing request anyway" }
                        requestPairing()
                    }
                }
            }
        }
    }

    fun setPairingQrCodeString(value: String) {
        if (value.isBlank()) {
            log.w { "setPairingQrCodeString called with blank value" }
            return
        }
        // Clear any previous error
        _pairingCodeError.value = null

        val trimmedValue = value.trim()

        // Check for demo pairing code
        if (trimmedValue == DEMO_PAIRING_CODE) {
            log.i { "Demo pairing code detected - activating demo mode" }
            setupDemoMode()
            return
        }

        // Clear demo mode when a real pairing code is entered
        if (ApplicationBootstrapFacade.isDemo) {
            log.i { "Real pairing code entered - exiting demo mode" }
            ApplicationBootstrapFacade.isDemo = false
        }

        try {
            _pairingQrCodeString.value = trimmedValue
            pairingQrCodeDataStored.value = false
            // Use injected decoder instead of static call
            val pairingQrCode = pairingQrCodeDecoder.decode(trimmedValue)
            val wsUrl = adaptLoopbackForAndroid(pairingQrCode.webSocketUrl)
            _webSocketUrl.value = wsUrl
            // Convert WebSocket URL to REST API URL, preserving the port
            // webSocketUrlToRestApiUrl just replaces ws:// with http://, keeping the port intact
            _restApiUrl.value =
                webSocketUrlToRestApiUrl(wsUrl)
            _tlsFingerprint.value = pairingQrCode.tlsFingerprint
            val pairingCode = pairingQrCode.pairingCode
            _pairingCodeId.value = pairingCode.id
            _grantedPermissions.value = pairingCode.grantedPermissions

            // Detect if we need Tor proxy based on URL
            val proxyOption =
                if (_restApiUrl.value.contains(".onion")) {
                    BisqProxyOption.INTERNAL_TOR
                } else {
                    BisqProxyOption.NONE
                }

            log.i {
                "update SensitiveSettings: webSocketUrl=$wsUrl " +
                    "restApiUrl=${_restApiUrl.value} " +
                    "tlsFingerprint=${pairingQrCode.tlsFingerprint} " +
                    "clientName=${_clientName.value} " +
                    "proxyOption=$proxyOption"
            }

            updatedSettings(_restApiUrl.value, pairingQrCode.tlsFingerprint, proxyOption)
        } catch (e: Exception) {
            log.e(e) { "Failed to decode pairing QR code: ${e.message}" }
            _pairingCodeError.value = "mobile.trustedNodeSetup.pairingCode.invalid".i18n()
            // Reset pairing code ID to prevent pairing attempt with invalid data
            _pairingCodeId.value = null
        }
    }

    /**
     * Sets up demo mode with fake credentials.
     * This bypasses the normal pairing flow and uses WebSocketClientDemo.
     */
    private fun setupDemoMode() {
        _pairingQrCodeString.value = DEMO_PAIRING_CODE
        _webSocketUrl.value = DEMO_WS_URL
        _restApiUrl.value = DEMO_API_URL
        _tlsFingerprint.value = null
        _pairingCodeId.value = DEMO_PAIRING_ID
        // Grant all permissions in demo mode
        _grantedPermissions.value = Permission.entries.toSet()
        // Set fake credentials for demo mode
        _clientId.value = DEMO_CLIENT_ID
        _clientSecret.value = DEMO_CLIENT_SECRET
        _sessionId.value = DEMO_SESSION_ID

        // Mark demo mode globally
        ApplicationBootstrapFacade.isDemo = true

        log.i { "Demo mode setup complete - using $DEMO_API_URL" }

        // Update settings with demo values
        val clientName = _clientName.value
        serviceScope.launch {
            sensitiveSettingsRepository.update { currentSettings ->
                currentSettings.copy(
                    bisqApiUrl = DEMO_API_URL,
                    tlsFingerprint = null,
                    clientName = clientName,
                    clientId = DEMO_CLIENT_ID,
                    sessionId = DEMO_SESSION_ID,
                    clientSecret = DEMO_CLIENT_SECRET,
                    selectedProxyOption = BisqProxyOption.NONE,
                )
            }
            // Mark pairing as complete for demo mode
            _pairingResult.value =
                Result.success(
                    PairingResponse(
                        version = 1,
                        clientId = DEMO_CLIENT_ID,
                        clientSecret = DEMO_CLIENT_SECRET,
                        sessionId = DEMO_SESSION_ID,
                        sessionExpiryDate = Long.MAX_VALUE,
                    ),
                )
            _pairingResultStored.value = true
        }
    }

    fun updatedSettings(
        restApiUrl: String,
        tlsFingerprint: String?,
        proxyOption: BisqProxyOption? = null,
    ) {
        val clientName = _clientName.value
        serviceScope.launch {
            try {
                sensitiveSettingsRepository.update { currentSettings ->
                    currentSettings.copy(
                        bisqApiUrl = restApiUrl,
                        tlsFingerprint = tlsFingerprint,
                        clientName = clientName,
                        // Clear old credentials when setting a new config
                        // The pairing request must be unauthenticated
                        clientId = null,
                        sessionId = null,
                        clientSecret = null,
                        // Update proxy option if provided
                        selectedProxyOption = proxyOption ?: currentSettings.selectedProxyOption,
                    )
                }
                pairingQrCodeDataStored.value = true
            } catch (e: Exception) {
                log.e { "updatedSettings failed - ${e.message}" }
            }
        }
    }

    fun requestPairing() {
        if (requestPairingJob != null) {
            log.w { "Pairing request in process" }
            return
        }
        if (_pairingCodeId.value == null) {
            log.w { "Pairing code must not be null" }
            return
        }

        _pairingResultStored.value = false

        requestPairingJob?.cancel()
        requestPairingJob =
            serviceScope.launch(Dispatchers.Default) {
                // Now we do a HTTP POST request for pairing.
                // This request is unauthenticated and will return the data we
                // need for establishing an authenticated and authorized
                // websocket connection.
                try {
                    val result: Result<PairingResponse> =
                        pairingService.requestPairing(
                            _pairingCodeId.value!!,
                            _clientName.value,
                        )
                    _pairingResult.value = result
                    if (result.isSuccess) {
                        log.i { "Pairing request was successful." }
                        val pairingResponse = result.getOrThrow()
                        _clientId.value = pairingResponse.clientId
                        _clientSecret.value = pairingResponse.clientSecret
                        _sessionId.value = pairingResponse.sessionId
                        updatedSettings(pairingResponse)
                    } else {
                        log.w { "Pairing request failed." }
                    }
                } finally {
                    requestPairingJob = null
                }
            }
    }

    private fun updatedSettings(pairingResponse: PairingResponse) {
        serviceScope.launch {
            sensitiveSettingsRepository.update { currentSettings ->
                currentSettings.copy(
                    clientId = pairingResponse.clientId,
                    sessionId = pairingResponse.sessionId,
                    clientSecret = pairingResponse.clientSecret,
                )
            }
            _pairingResultStored.value = true
        }
    }

    private fun webSocketUrlToRestApiUrl(webSocketUrl: String): String =
        webSocketUrl
            .replaceFirst("wss", "https")
            .replaceFirst("ws", "http")

    private fun restApiUrlToWebSocketUrl(restApiUrl: String): String =
        restApiUrl
            .replaceFirst("https", "wss")
            .replaceFirst("http", "ws")

    private fun adaptLoopbackForAndroid(url: String): String {
        if (isIOS()) return url

        return url
            .replace(LOOPBACK, ANDROID_LOCALHOST)
            .replace(LOCALHOST, ANDROID_LOCALHOST)
    }

    fun isIOS(): Boolean {
        val platformInfo = getPlatformInfo()
        val isIOS = platformInfo.name.lowercase().contains("ios")
        log.d { "isIOS = $isIOS" }
        return isIOS
    }

    /**
     * Decodes a pairing code string into a PairingQrCode.
     * Used by TrustedNodeSetupPresenter.
     * Special case: DEMO_PAIRING_CODE bypasses validation and returns a demo PairingQrCode.
     */
    fun getPairingCodeQr(value: String): Result<PairingQrCode> {
        val trimmedValue = value.trim()

        // Special case for demo mode - bypass validation
        if (trimmedValue == DEMO_PAIRING_CODE) {
            log.i { "Demo pairing code detected - returning demo PairingQrCode" }
            val demoPairingCode =
                PairingCode(
                    id = DEMO_PAIRING_ID,
                    expiresAt = Instant.DISTANT_FUTURE,
                    grantedPermissions = Permission.entries.toSet(),
                )
            val demoPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode = demoPairingCode,
                    webSocketUrl = DEMO_WS_URL,
                    restApiUrl = DEMO_API_URL,
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            return Result.success(demoPairingQrCode)
        }

        return try {
            val code = pairingQrCodeDecoder.decode(trimmedValue)
            Result.success(code)
        } catch (e: Exception) {
            log.e(e) { "Decoding pairing code failed." }
            Result.failure(Throwable("mobile.trustedNodeSetup.pairingCode.invalid".i18n()))
        }
    }

    /**
     * Updates settings from a PairingQrCode.
     * Used by TrustedNodeSetupUseCase for the pairing flow.
     */
    suspend fun updateSettings(pairingQrCode: PairingQrCode) {
        val url = pairingQrCode.restApiUrl
        if (url.isNotBlank()) {
            sensitiveSettingsRepository.update {
                it.copy(
                    bisqApiUrl = pairingQrCode.restApiUrl,
                    selectedProxyOption = getProxyOptionFromRestUrl(url),
                    tlsFingerprint = pairingQrCode.tlsFingerprint,
                )
            }
        }
    }

    /**
     * Requests pairing with a PairingQrCode.
     * Used by TrustedNodeSetupUseCase for the pairing flow.
     * Returns a Result with the PairingResponse.
     * Special case: Demo mode returns fake credentials without making an HTTP request.
     */
    suspend fun requestPairing(
        pairingQrCode: PairingQrCode,
    ): Result<PairingResponse> {
        // Special case for demo mode - return fake credentials without HTTP request
        if (pairingQrCode.pairingCode.id == DEMO_PAIRING_ID) {
            log.i { "Demo mode detected - returning fake pairing response" }
            val demoResponse =
                PairingResponse(
                    version = 1,
                    clientId = DEMO_CLIENT_ID,
                    clientSecret = DEMO_CLIENT_SECRET,
                    sessionId = DEMO_SESSION_ID,
                    sessionExpiryDate = Long.MAX_VALUE,
                )
            // Setup demo mode state
            setupDemoMode()
            return Result.success(demoResponse)
        }

        // Now we do a HTTP POST request for pairing.
        // This request is unauthenticated and will return the data we
        // need for establishing an authenticated and authorized
        // websocket connection.
        val clientName = generateClientName()
        return pairingService
            .requestPairing(
                pairingQrCode.pairingCode.id,
                clientName,
            ).onSuccess { pairingData ->
                log.i { "Pairing request was successful." }
                updatedSettingsFromPairingResponse(
                    pairingData,
                    pairingQrCode,
                    clientName,
                )
            }.onFailure { error ->
                log.e(error) { "Pairing request failed." }
            }
    }

    private suspend fun updatedSettingsFromPairingResponse(
        pairingResponse: PairingResponse,
        pairingQrCode: PairingQrCode,
        clientName: String,
    ) {
        sensitiveSettingsRepository.update {
            it.copy(
                clientId = pairingResponse.clientId,
                sessionId = pairingResponse.sessionId,
                clientSecret = pairingResponse.clientSecret,
                bisqApiUrl = pairingQrCode.restApiUrl,
                tlsFingerprint = pairingQrCode.tlsFingerprint,
                clientName = clientName,
            )
        }
    }

    private fun generateClientName(): String {
        val platformInfo = getPlatformInfo()
        return "Bisq Connect ${platformInfo.name}"
    }
}
