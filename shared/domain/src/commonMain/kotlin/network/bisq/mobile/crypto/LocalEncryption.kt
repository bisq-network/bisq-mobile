package network.bisq.mobile.crypto

private const val DEFAULT_KEY_ALIAS = "network.bisq.mobile"

expect suspend fun encrypt(data: ByteArray, keyAlias: String = DEFAULT_KEY_ALIAS): ByteArray

expect suspend fun decrypt(data: ByteArray, keyAlias: String = DEFAULT_KEY_ALIAS): ByteArray