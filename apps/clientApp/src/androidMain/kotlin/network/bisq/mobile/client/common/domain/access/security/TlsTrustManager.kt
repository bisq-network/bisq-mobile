package network.bisq.mobile.client.common.domain.access.security

import android.annotation.SuppressLint
import android.util.Base64
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

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

            if (!SanVerifier.matchesHost(cert, expectedHost)) {
                throw SecurityException(
                    "Certificate SAN does not match host: " + expectedHost,
                )
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
