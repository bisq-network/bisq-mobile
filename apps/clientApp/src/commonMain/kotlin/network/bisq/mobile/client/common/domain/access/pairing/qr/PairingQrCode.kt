package network.bisq.mobile.client.common.domain.access.pairing.qr

import network.bisq.mobile.client.common.domain.access.pairing.PairingCode

data class PairingQrCode(
    val version: Byte,
    val pairingCode: PairingCode,
    val webSocketUrl: String,
    val tlsFingerprint: String?,
    val torClientAuthSecret: String?,
)
