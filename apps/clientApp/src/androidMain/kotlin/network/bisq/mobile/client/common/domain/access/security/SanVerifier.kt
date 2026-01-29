package network.bisq.mobile.client.common.domain.access.security

import java.security.cert.X509Certificate

object SanVerifier {
    fun matchesHost(
        cert: X509Certificate,
        host: String,
    ): Boolean {
        try {
            val sans = cert.subjectAlternativeNames
            if (sans == null) return false

            for (san in sans) {
                val type = san.get(0) as Int
                val value: Any? = san.get(1)

                // 2 = DNS, 7 = IP
                if (type == 2 &&
                    host.equals(
                        value as String?,
                        ignoreCase = true,
                    )
                ) {
                    return true
                }
                if (type == 7 && host == value) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
