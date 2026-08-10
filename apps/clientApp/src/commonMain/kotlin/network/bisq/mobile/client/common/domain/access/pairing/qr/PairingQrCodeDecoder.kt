package network.bisq.mobile.client.common.domain.access.pairing.qr

import network.bisq.mobile.client.common.domain.access.LOCALHOST
import network.bisq.mobile.client.common.domain.access.pairing.PairingCodeDecoder
import network.bisq.mobile.client.common.domain.access.pairing.UnsupportedPairingVersionException
import network.bisq.mobile.client.common.domain.utils.BinaryDecodingUtils
import network.bisq.mobile.data.utils.EnvironmentController
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

const val LOOPBACK = "127.0.0.1"
const val ANDROID_LOCALHOST = "10.0.2.2"

private const val URL_SAFE_BASE64_ALPHABET =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

@OptIn(ExperimentalEncodingApi::class)
class PairingQrCodeDecoder(
    private val environmentController: EnvironmentController,
) {
    fun decode(qrCodeAsBase64: String): PairingQrCode =
        decode(
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .decode(
                    sanitize(qrCodeAsBase64),
                ),
        )

    /**
     * The node writes the payload as one long line followed by an ASCII-art QR block, and
     * terminals soft-wrap it, so real-world copies arrive with embedded newlines, trailing
     * art, or padding. Whitespace is never part of the url-safe base64 alphabet, so joining
     * the fragments and cutting at the first foreign character recovers exactly the payload.
     */
    private fun sanitize(raw: String): String =
        raw
            .asSequence()
            .filterNot { it.isWhitespace() }
            .takeWhile { it in URL_SAFE_BASE64_ALPHABET }
            .joinToString("")

    fun decode(qrCodeBytes: ByteArray): PairingQrCode {
        val reader = BinaryDecodingUtils(qrCodeBytes)

        // ---- Version ----
        val version = reader.readByte()
        if (version != PairingQrCodeFormat.VERSION) {
            throw UnsupportedPairingVersionException("Unsupported QR code version: $version")
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

        val adjustedWebSocketUrl = adjustWebSocketUrlForDevice(webSocketUrl)
        val restApiUrl = webSocketUrlToRestApiUrl(adjustedWebSocketUrl)
        return PairingQrCode(
            version = version,
            pairingCode = pairingCode,
            webSocketUrl = adjustedWebSocketUrl,
            restApiUrl = restApiUrl,
            tlsFingerprint = tlsFingerprint,
            torClientAuthSecret = torClientAuthSecret,
        )
    }

    private fun adjustWebSocketUrlForDevice(url: String): String {
        if (!environmentController.isSimulator()) return url
        // Don't replace onion addresses — Tor traffic goes through the Tor proxy, not emulator loopback
        if (url.contains(".onion")) return url
        // On emulators/simulators, replace the host with the appropriate loopback
        // because emulators can't reach LAN IPs — they route to the host via special addresses
        val emulatorHost = if (getPlatformInfo().type == PlatformType.IOS) LOCALHOST else ANDROID_LOCALHOST
        return replaceHost(url, emulatorHost)
    }

    private fun replaceHost(
        url: String,
        newHost: String,
    ): String {
        // URL format: scheme://host:port/...
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return url
        val hostStart = schemeEnd + 3
        val portOrPathStart =
            url.indexOf(':', hostStart).takeIf { it >= 0 }
                ?: url.indexOf('/', hostStart).takeIf { it >= 0 }
                ?: url.length
        return url.substring(0, hostStart) + newHost + url.substring(portOrPathStart)
    }

    private fun webSocketUrlToRestApiUrl(webSocketUrl: String): String =
        webSocketUrl
            .replaceFirst("wss://", "https://")
            .replaceFirst("ws://", "http://")
}
