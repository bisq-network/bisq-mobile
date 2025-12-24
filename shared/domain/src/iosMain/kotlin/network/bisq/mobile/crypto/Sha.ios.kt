package network.bisq.mobile.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun getSha256(data: ByteArray): ByteArray =
    memScoped {
        val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
        val uData = data.toUByteArray()
        CC_SHA256(uData.toCValues().ptr, uData.size.convert(), digest)
        ByteArray(CC_SHA256_DIGEST_LENGTH) { digest[it].toByte() }
    }
