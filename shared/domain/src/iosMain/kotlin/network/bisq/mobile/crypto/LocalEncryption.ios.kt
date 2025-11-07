package network.bisq.mobile.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.ios.toByteArray
import network.bisq.mobile.ios.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
private object LocalEncryption {
    suspend fun encrypt(bytes: ByteArray, keyAlias: String): ByteArray = suspendCancellableCoroutine { continuation ->
        LocalEncryptionBridge.shared().encryptWithData(
            data = bytes.toNSData(),
            keyAlias = keyAlias
        ) { result, error ->
            if (error != null) {
                continuation.resumeWithException(
                    IllegalStateException("Encryption failed: ${error.localizedDescription}")
                )
            } else if (result != null) {
                continuation.resume(result.toByteArray())
            } else {
                continuation.resumeWithException(
                    IllegalStateException("Encryption failed: no result returned")
                )
            }
        }
    }
    
    suspend fun decrypt(bytes: ByteArray, keyAlias: String): ByteArray = suspendCancellableCoroutine { continuation ->
        LocalEncryptionBridge.shared().decryptWithData(
            data = bytes.toNSData(),
            keyAlias = keyAlias
        ) { result, error ->
            if (error != null) {
                continuation.resumeWithException(
                    IllegalStateException("Decryption failed: ${error.localizedDescription}")
                )
            } else if (result != null) {
                continuation.resume(result.toByteArray())
            } else {
                continuation.resumeWithException(
                    IllegalStateException("Decryption failed: no result returned")
                )
            }
        }
    }
}

actual suspend fun encrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.encrypt(data, keyAlias)
}

actual suspend fun decrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.decrypt(data, keyAlias)
}
