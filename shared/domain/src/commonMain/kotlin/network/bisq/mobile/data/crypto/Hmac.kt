package network.bisq.mobile.data.crypto

expect fun hmacSha256(
    key: ByteArray,
    data: ByteArray,
): ByteArray
