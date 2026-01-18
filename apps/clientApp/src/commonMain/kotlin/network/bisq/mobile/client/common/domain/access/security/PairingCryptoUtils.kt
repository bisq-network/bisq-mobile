package network.bisq.mobile.client.common.domain.access.security

import network.bisq.mobile.client.common.domain.security.RawKeyPair

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object PairingCryptoUtils {
    fun generateKeyPair(): RawKeyPair

    fun generatePublic(encodedPublicKey: ByteArray): ByteArray

    fun sign(
        message: ByteArray,
        encodedPrivateKey: ByteArray,
    ): ByteArray // 64 bytes

    fun verify(
        message: ByteArray,
        signature: ByteArray,
        encodedPublicKey: ByteArray,
    ): Boolean
}
