package network.bisq.mobile.client.common.domain.access

import androidx.annotation.CallSuper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ApiAccessService(
    private val pairingService: PairingService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val webSocketClientService: WebSocketClientService,
) : ServiceFacade(),
    Logging {
    private val _clientName = MutableStateFlow("Alice") // todo
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _pairingQrCodeString = MutableStateFlow("")
    val pairingQrCodeString: StateFlow<String> =
        _pairingQrCodeString.asStateFlow()

    private val _webSocketUrl = MutableStateFlow("")
    val webSocketUrl: StateFlow<String> = _webSocketUrl.asStateFlow()

    private val _grantedPermissions: MutableStateFlow<Set<Permission>> =
        MutableStateFlow(emptySet())
    val grantedPermissions: StateFlow<Set<Permission>> =
        _grantedPermissions.asStateFlow()

    private val _pairingResult: MutableStateFlow<Result<PairingResponse>?> =
        MutableStateFlow(null)
    val pairingResult: StateFlow<Result<PairingResponse>?> =
        _pairingResult.asStateFlow()

    private var requestPairingJob: Job? = null

    private var pairingDataStored: Boolean = false

    @CallSuper
    override suspend fun activate() {
        super.activate()
        serviceScope.launch {
            try {
                val settings = sensitiveSettingsRepository.fetch()
                if (_webSocketUrl.value.isBlank() && settings.bisqApiUrl.isNotBlank()) {
                    _webSocketUrl.value = settings.bisqApiUrl
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    fun setPairingQrCodeString(value: String) {
        if (value.isNotBlank()) {
            applyPairingQrCode(value)
        }
    }

    fun setDeviceName(value: String) {
        if (value.length >= 4) {
            _clientName.value = value

            if (pairingDataStored) {
                requestPairing()
            }
        }
    }

    fun applyPairingQrCode(value: String) {
        _pairingQrCodeString.value = value
        try {
            val pairingQrCode =
                PairingQrCodeDecoder.decode(_pairingQrCodeString.value)

            _webSocketUrl.value = pairingQrCode.webSocketUrl
            val pairingCode = pairingQrCode.pairingCode
            _grantedPermissions.value = pairingCode.grantedPermissions

            serviceScope.launch {
                log.i {
                    "update SensitiveSettings: webSocketUrl=$webSocketUrl " +
                        "tlsFingerprint=${pairingQrCode.tlsFingerprint}" +
                        "clientName=${_clientName.value}" +
                        "clientKeyPair (not logged)"
                }
                val currentSettings = sensitiveSettingsRepository.fetch()
                val bisqApiUrl =
                    pairingQrCode.webSocketUrl
                        .replaceFirst("wss", "https")
                        .replaceFirst("ws", "http")
                val updatedSettings =
                    currentSettings.copy(
                        bisqApiUrl = bisqApiUrl,
                        tlsFingerprint = pairingQrCode.tlsFingerprint,
                        clientName = _clientName.value,
                    )
                sensitiveSettingsRepository.update { updatedSettings }

                pairingDataStored = true
                if (_clientName.value.length >= 4) {
                    requestPairing()
                }
            }
        } catch (e: Exception) {
            log.e("PairingCode decoding failed", e)
        }
    }

    fun requestPairing() {
        if (requestPairingJob != null) {
            log.w { "Pairing request in process" }
            return
        }

        try {
            val pairingQrCode =
                PairingQrCodeDecoder.decode(_pairingQrCodeString.value)

            _webSocketUrl.value = pairingQrCode.webSocketUrl
            val pairingCode = pairingQrCode.pairingCode
            _grantedPermissions.value = pairingCode.grantedPermissions

            requestPairingJob?.cancel()
            requestPairingJob =
                serviceScope.launch {
                    // Now we do a HTTP POST request for pairing.
                    // This request is unauthenticated and will return the data we
                    // need for establishing an authenticated and authorized
                    // websocket connection.
                    val result: Result<PairingResponse> =
                        pairingService.requestPairing(
                            pairingQrCode,
                            _clientName.value,
                        )
                    _pairingResult.value = result
                    if (result.isSuccess) {
                        log.i { "Pairing request was successful." }
                        val pairingResponse = result.getOrThrow()
                        applyPairingResponse(pairingResponse)
                    } else {
                        log.w { "Pairing request failed." }
                    }

                    requestPairingJob = null
                }
        } catch (e: Exception) {
            log.e("PairingCode decoding failed", e)
        }
    }

    private fun applyPairingResponse(pairingResponse: PairingResponse) {
        serviceScope.launch {
            val currentSettings =
                sensitiveSettingsRepository.fetch()
            val updatedSettings =
                currentSettings.copy(
                    sessionId = pairingResponse.sessionId,
                    clientId = pairingResponse.clientId,
                    clientSecret = pairingResponse.clientSecret,
                )

            sensitiveSettingsRepository.update { updatedSettings }

            serviceScope.launch {
                log.e { "webSocketClientService.connect" }
                val result = webSocketClientService.connect()
                log.e { "webSocketClientService.connect $result" }
            }
        }
    }
}
