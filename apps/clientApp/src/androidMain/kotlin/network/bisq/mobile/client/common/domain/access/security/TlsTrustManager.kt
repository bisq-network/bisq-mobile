package network.bisq.mobile.client.common.domain.access.security

import android.annotation.SuppressLint
import android.util.Base64
import network.bisq.mobile.client.common.domain.access.pairing.qr.ANDROID_LOCALHOST
import network.bisq.mobile.client.common.domain.access.pairing.qr.LOOPBACK
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

const val LOCALHOST = "localhost"

@SuppressLint("CustomX509TrustManager")
class TlsTrustManager(
    val expectedHost: String,
    fingerprint: String,
) : X509TrustManager {
    private val pinnedFingerprint: ByteArray =
        Base64.decode(fingerprint, Base64.DEFAULT)

    override fun checkServerTrusted(
        chain: Array<X509Certificate>?,
        authType: String?,
    ) {
        require(!(chain == null || chain.isEmpty())) { "Empty certificate chain" }

        try {
            val cert = chain[0] // leaf cert

            // If host is ANDROID_LOCALHOST we use the LOOPBACK host as that was
            // used for SAN setup at server
            val hostToTest =
                if (expectedHost == ANDROID_LOCALHOST) {
                    LOOPBACK
                } else {
                    expectedHost
                }
            // Skip SAN check for .onion hosts: the server cannot include the
            // onion address in its certificate SAN because the TLS cert is
            // generated before Tor publishes the hidden service address.
            // Security is maintained by fingerprint pinning (below) plus
            // Tor's own cryptographic host authentication.
            val isOnionHost = hostToTest.endsWith(".onion")
            if (!isOnionHost && !SanVerifier.matchesHost(cert, hostToTest)) {
                // In case we had tested with "127.0.0.1" and it failed we will
                // test again with "localhost".
                if (hostToTest != LOOPBACK ||
                    !SanVerifier.matchesHost(
                        cert,
                        LOCALHOST,
                    )
                ) {
                    throw SecurityException(
                        "Certificate SAN does not match host: $hostToTest",
                    )
                }
            }

            val hash = MessageDigest.getInstance("SHA-256").digest(cert.encoded)

            if (!MessageDigest.isEqual(hash, pinnedFingerprint)) {
                throw SecurityException("TLS fingerprint verification failed")
            }
        } catch (e: Exception) {
            throw SecurityException("TLS trust check failed", e)
        }
    }

    override fun checkClientTrusted(
        chain: Array<X509Certificate?>?,
        authType: String?,
    ): Unit = throw UnsupportedOperationException()

    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
}
