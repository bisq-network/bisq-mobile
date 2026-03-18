package network.bisq.mobile.data.crypto

/**
 * @return cryptographically secure random bytes
 */
expect fun nextSecureRandomBytes(count: Int): ByteArray
