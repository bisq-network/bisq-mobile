package network.bisq.mobile.client.common.domain.access.identity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.access.security.PairingCryptoUtils
import network.bisq.mobile.client.common.domain.access.session.SessionToken
import network.bisq.mobile.client.common.domain.security.RawKeyPair
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientService(
    val pairingService: PairingService,
) : ServiceFacade(),
    Logging {
    protected var qrCode: String? = null
    protected var sessionToken: SessionToken? = null
    var grantedPermissions: Set<Permission> = emptySet()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _pairingQrCodeString = MutableStateFlow("")
    val pairingQrCodeString: StateFlow<String> =
        _pairingQrCodeString.asStateFlow()

    private val _webSocketUrl = MutableStateFlow("")
    val webSocketUrl: StateFlow<String> = _webSocketUrl.asStateFlow()

    private val _pairingQrCode: MutableStateFlow<PairingQrCode?> =
        MutableStateFlow(null)
    val pairingQrCode: StateFlow<PairingQrCode?> = _pairingQrCode.asStateFlow()

    private val _clientIdentity: MutableStateFlow<ClientIdentity?> =
        MutableStateFlow(null)
    val clientIdentity: StateFlow<ClientIdentity?> =
        _clientIdentity.asStateFlow()

    fun setPairingQrCodeString(value: String) {
        _pairingQrCodeString.value = value
        startPairing()
    }

    fun setDeviceName(value: String) {
        _deviceName.value = value
        startPairing()
    }

    fun startPairing() {
        if (_pairingQrCodeString.value.length > 0 && _deviceName.value.length >= 4) {
            try {
                val pairingQrCode =
                    PairingQrCodeDecoder.decode(_pairingQrCodeString.value)
                _pairingQrCode.value = pairingQrCode

                val clientIdentity = createClientIdentity(_deviceName.value)
                _clientIdentity.value = clientIdentity
                persistClientIdentity(clientIdentity)

                applyWebSocketUrl(pairingQrCode.webSocketUrl)

                val pairingCode = pairingQrCode.pairingCode

                applyGrantedPermissions(pairingCode.grantedPermissions)

                serviceScope.launch {
                    val result: Result<PairingResponse> =
                        pairingService.requestPairing(
                            pairingQrCode,
                            clientIdentity,
                        )
                    if (result.isSuccess) {
                        val pairingResponse = result.getOrThrow()
                        // TODO
                        pairingResponse.sessionId
                    } else {
                        log.w { "Pairing request failed." }
                    }
                }
            } catch (e: Exception) {
                log.e("PairingCode decoding failed", e)
            }
        }
    }

    private fun createClientIdentity(deviceName: String): ClientIdentity {
        val rawKeyPair: RawKeyPair = PairingCryptoUtils.generateKeyPair()
        return ClientIdentity(deviceName, rawKeyPair)
    }

    private fun persistClientIdentity(clientIdentity: ClientIdentity) {
        // TODO
    }

    private fun applyWebSocketUrl(value: String) {
        _webSocketUrl.value = value

        // TODO
    }

    private fun applyGrantedPermissions(grantedPermissions: Set<Permission>) {
        // todo
    }
}
