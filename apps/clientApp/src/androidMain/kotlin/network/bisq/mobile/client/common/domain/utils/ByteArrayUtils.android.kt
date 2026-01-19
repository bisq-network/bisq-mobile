package network.bisq.mobile.client.common.domain.utils

import java.security.SecureRandom

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ByteArrayUtils {
    private val secureRandom = SecureRandom()

    actual fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}
