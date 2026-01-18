package network.bisq.mobile.client.common.domain.access.security

import network.bisq.mobile.client.common.domain.security.RawKeyPair

// FIXME Add CryptoKit dependency and implementation

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PairingCryptoUtils {
    actual fun generateKeyPair(): RawKeyPair {
        TODO("Not yet implemented")
    }

    actual fun generatePublic(encodedPublicKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun sign(
        message: ByteArray,
        encodedPrivateKey: ByteArray,
    ): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun verify(
        message: ByteArray,
        signature: ByteArray,
        encodedPublicKey: ByteArray,
    ): Boolean {
        TODO("Not yet implemented")
    }
}
