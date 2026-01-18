package network.bisq.mobile.client.common.domain.access.pairing

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.identity.ClientIdentity
import network.bisq.mobile.client.common.domain.access.pairing.api.ClientPairingService
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.security.PairingCryptoUtils
import network.bisq.mobile.client.common.domain.security.RawKeyPair
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

class PairingService(
    private val clientPairingService: ClientPairingService,
) : ServiceFacade(), Logging {
    suspend fun requestPairing(
        pairingQrCode: PairingQrCode,
        clientIdentity: ClientIdentity,
    ): Result<PairingResponse> {
        val keyPair: RawKeyPair = clientIdentity.rawKeyPair
        val clientPublicKey: ByteArray = keyPair.publicKey
        val pairingCodeId: String = pairingQrCode.pairingCode.id
        val timestamp: Instant = Clock.System.now()
        val pairingRequestPayload =
            PairingRequestPayload(
                PairingRequestPayload.VERSION,
                pairingCodeId,
                clientPublicKey,
                clientIdentity.deviceName,
                timestamp,
            )

        val signature: ByteArray =
            signPayload(pairingRequestPayload, keyPair.privateKey)
        val pairingRequest = PairingRequest(pairingRequestPayload, signature)

        return clientPairingService.requestPairing(pairingRequest)
    }

    private fun signPayload(
        pairingRequestPayload: PairingRequestPayload,
        privateKey: ByteArray,
    ): ByteArray {
        val message = PairingRequestPayloadEncoder.encode(pairingRequestPayload)
        return PairingCryptoUtils.sign(message, privateKey)
    }
}
