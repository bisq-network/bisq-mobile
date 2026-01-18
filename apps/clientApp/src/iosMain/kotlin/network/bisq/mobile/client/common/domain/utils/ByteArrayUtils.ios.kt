package network.bisq.mobile.client.common.domain.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ByteArrayUtils {

    @OptIn(ExperimentalForeignApi::class)
    actual fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        val result = SecRandomCopyBytes(
            kSecRandomDefault,
            size.toULong(),
            bytes.refTo(0)
        )
        require(result == 0) { "Failed to generate secure random bytes" }
        return bytes
    }
}
