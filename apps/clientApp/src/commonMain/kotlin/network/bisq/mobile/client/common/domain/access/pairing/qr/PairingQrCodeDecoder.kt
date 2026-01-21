package network.bisq.mobile.client.common.domain.access.pairing.qr

import network.bisq.mobile.client.common.domain.access.pairing.PairingCodeDecoder
import network.bisq.mobile.client.common.domain.utils.BinaryDecodingUtils
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PairingQrCodeDecoder {
    fun decode(qrCodeAsBase64: String): PairingQrCode =
        decode(
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .decode(qrCodeAsBase64),
        )

    fun decode(qrCodeBytes: ByteArray): PairingQrCode {
        val reader = BinaryDecodingUtils(qrCodeBytes)

        // ---- Version ----
        val version = reader.readByte()
        require(version == PairingQrCodeFormat.VERSION) {
            "Unsupported QR code version: $version"
        }

        // ---- PairingCode ----
        val pairingCodeBytes =
            reader.readBytes(PairingQrCodeFormat.MAX_PAIRING_CODE_BYTES)
        val pairingCode = PairingCodeDecoder.decode(pairingCodeBytes)

        // ---- Address ----
        val webSocketUrl =
            reader.readString(PairingQrCodeFormat.MAX_WS_URL_BYTES)

        // ---- Flags ----
        val flags = reader.readByte()

        var tlsFingerprint: String? = null
        var torClientAuthSecret: String? = null

        // ---- Optional fields (order must match encoder) ----
        if ((flags.toInt() and PairingQrCodeFormat.FLAG_TLS_FINGERPRINT) != 0) {
            tlsFingerprint =
                reader.readString(PairingQrCodeFormat.MAX_TLS_FINGERPRINT_BYTES)
        }

        if ((flags.toInt() and PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH) != 0) {
            torClientAuthSecret =
                reader.readString(PairingQrCodeFormat.MAX_TOR_SECRET_BYTES)
        }

        return PairingQrCode(
            version = version,
            pairingCode = pairingCode,
            webSocketUrl = webSocketUrl,
            tlsFingerprint = tlsFingerprint,
            torClientAuthSecret = torClientAuthSecret,
        )
    }
}
