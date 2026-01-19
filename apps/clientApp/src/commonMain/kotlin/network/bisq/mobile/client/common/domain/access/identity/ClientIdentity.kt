package network.bisq.mobile.client.common.domain.access.identity

import network.bisq.mobile.client.common.domain.security.RawKeyPair

class ClientIdentity(
    val deviceName: String,
    val rawKeyPair: RawKeyPair,
) {
    val publicKey: ByteArray
        get() = rawKeyPair.publicKey
}
