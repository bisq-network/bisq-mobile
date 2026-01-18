package network.bisq.mobile.client.common.domain.security

data class RawKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)
