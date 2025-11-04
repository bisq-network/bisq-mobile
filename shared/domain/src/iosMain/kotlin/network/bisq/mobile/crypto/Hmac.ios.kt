package network.bisq.mobile.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA256

@OptIn(ExperimentalForeignApi::class)
actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val result = ByteArray(CC_SHA256_DIGEST_LENGTH)

    key.usePinned { keyPinned ->
        data.usePinned { dataPinned ->
            result.usePinned { resultPinned ->
                CCHmac(
                    kCCHmacAlgSHA256,
                    keyPinned.addressOf(0),
                    key.size.toULong(),
                    dataPinned.addressOf(0),
                    data.size.toULong(),
                    resultPinned.addressOf(0)
                )
            }
        }
    }

    return result
}