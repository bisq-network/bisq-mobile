package network.bisq.mobile.domain.crypto

expect fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray