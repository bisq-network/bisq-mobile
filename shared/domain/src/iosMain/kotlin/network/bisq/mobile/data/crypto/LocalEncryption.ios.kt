package network.bisq.mobile.data.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import network.bisq.mobile.crypto.LocalEncryptionBridge
import network.bisq.mobile.domain.utils.toByteArray
import network.bisq.mobile.domain.utils.toNSData
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
private object LocalEncryption {
    suspend fun encrypt(
        bytes: ByteArray,
        keyAlias: String,
    ): ByteArray =
        suspendCancellableCoroutine { continuation ->
            LocalEncryptionBridge.Companion.shared().encryptWithData(
                data = bytes.toNSData(),
                keyAlias = keyAlias,
            ) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException("Encryption failed: ${error.localizedDescription}"),
                    )
                } else if (result != null) {
                    continuation.resume(result.toByteArray())
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Encryption failed: no result returned"),
                    )
                }
            }
        }

    suspend fun decrypt(
        bytes: ByteArray,
        keyAlias: String,
    ): ByteArray =
        suspendCancellableCoroutine { continuation ->
            LocalEncryptionBridge.Companion.shared().decryptWithData(
                data = bytes.toNSData(),
                keyAlias = keyAlias,
            ) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException("Decryption failed: ${error.localizedDescription}"),
                    )
                } else if (result != null) {
                    continuation.resume(result.toByteArray())
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Decryption failed: no result returned"),
                    )
                }
            }
        }
}

actual suspend fun encrypt(
    data: ByteArray,
    keyAlias: String,
): ByteArray = LocalEncryption.encrypt(data, keyAlias)

actual suspend fun decrypt(
    data: ByteArray,
    keyAlias: String,
): ByteArray = LocalEncryption.decrypt(data, keyAlias)
