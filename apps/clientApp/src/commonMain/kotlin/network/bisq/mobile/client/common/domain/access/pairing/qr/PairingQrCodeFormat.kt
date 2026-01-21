package network.bisq.mobile.client.common.domain.access.pairing.qr

object PairingQrCodeFormat {
    // ---- Versioning ----
    const val VERSION: Byte = 1

    // ---- Flags ----
    const val FLAG_TLS_FINGERPRINT: Int = 1 // 1 shl 0
    const val FLAG_TOR_CLIENT_AUTH: Int = 1 shl 1

    // ---- Limits ----
    const val MAX_PAIRING_CODE_BYTES: Int = 4096
    const val MAX_WS_URL_BYTES: Int = 512
    const val MAX_TLS_FINGERPRINT_BYTES: Int = 128
    const val MAX_TOR_SECRET_BYTES: Int = 256
}
