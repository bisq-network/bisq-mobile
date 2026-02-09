package network.bisq.mobile.client.common.domain.access

import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.access.utils.ApiAccessUtil.getProxyOptionFromRestUrl
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

const val LOCALHOST = "localhost"

class ApiAccessService(
    private val pairingService: PairingService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val pairingQrCodeDecoder: PairingQrCodeDecoder,
) : ServiceFacade(),
    Logging {
    fun getPairingCodeQr(value: String): Result<PairingQrCode> =
        try {
            val code = pairingQrCodeDecoder.decode(value.trim())
            Result.success(code)
        } catch (e: Exception) {
            log.e(e) { "Decoding pairing code failed." }
            Result.failure(Throwable("mobile.trustedNodeSetup.pairingCode.invalid".i18n()))
        }

    suspend fun requestPairing(
        pairingQrCode: PairingQrCode,
    ): Result<PairingResponse> {
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
                updatedSettings(
                    pairingData,
                    pairingQrCode,
                    clientName,
                )
            }.onFailure { error ->
                log.e(error) { "Pairing request failed." }
            }
    }

    private suspend fun updatedSettings(
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

    private fun generateClientName(): String {
        val platformInfo = getPlatformInfo()
        return "Bisq Connect ${platformInfo.name}"
    }
}
