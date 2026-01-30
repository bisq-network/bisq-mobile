package network.bisq.mobile.client.common.domain.access

import androidx.annotation.CallSuper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

const val LOCALHOST = "localhost"
const val LOOPBACK = "127.0.0.1"
const val ANDROID_LOCALHOST = "10.0.2.2"

class ApiAccessService(
    private val pairingService: PairingService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val httpClientService: HttpClientService,
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

    // private var pairingCode: PairingCode? = null
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
        try {
            _pairingQrCodeString.value = value.trim()
            pairingQrCodeDataStored.value = false
            val pairingQrCode =
                PairingQrCodeDecoder.decode(value.trim())
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

    fun updatedSettings(
        restApiUrl: String,
        tlsFingerprint: String?,
        proxyOption: BisqProxyOption? = null,
    ) {
        try {
            serviceScope.launch {
                val currentSettings = sensitiveSettingsRepository.fetch()
                val updatedSettings =
                    currentSettings.copy(
                        bisqApiUrl = restApiUrl,
                        tlsFingerprint = tlsFingerprint,
                        clientName = _clientName.value,
                        // Clear old credentials when setting a new config
                        // The pairing request must be unauthenticated
                        clientId = null,
                        sessionId = null,
                        clientSecret = null,
                        // Update proxy option if provided
                        selectedProxyOption = proxyOption ?: currentSettings.selectedProxyOption,
                    )
                sensitiveSettingsRepository.update { updatedSettings }
                pairingQrCodeDataStored.value = true
            }
        } catch (ignore: Exception) {
            log.e { "updatedSettings failed" }
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
                    // _pairingCodeExpiresAt.value = pairingResponse.expiresAt
                    updatedSettings(pairingResponse)
                } else {
                    log.w { "Pairing request failed." }
                }

                requestPairingJob = null
            }
    }

    private fun updatedSettings(pairingResponse: PairingResponse) {
        serviceScope.launch {
            val currentSettings =
                sensitiveSettingsRepository.fetch()
            val updatedSettings =
                currentSettings.copy(
                    clientId = pairingResponse.clientId,
                    sessionId = pairingResponse.sessionId,
                    clientSecret = pairingResponse.clientSecret,
                )

            sensitiveSettingsRepository.update { updatedSettings }
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

    private fun generateClientName(): String {
        val platformInfo = getPlatformInfo()
        return "Bisq Connect ${platformInfo.name}"
    }
}
