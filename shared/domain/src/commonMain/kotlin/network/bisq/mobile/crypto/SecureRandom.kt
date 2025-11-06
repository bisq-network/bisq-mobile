package network.bisq.mobile.crypto

/**
 * @return cryptographically secure random bytes
 */
expect fun nextSecureRandomBytes(count: Int): ByteArray