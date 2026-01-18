package network.bisq.mobile.client.common.domain.utils

//      network.bisq.mobile.client.common.access.utils
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object ByteArrayUtils {
    fun randomBytes(size: Int): ByteArray
}
