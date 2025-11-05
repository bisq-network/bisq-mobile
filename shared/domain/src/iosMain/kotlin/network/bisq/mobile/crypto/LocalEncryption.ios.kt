package network.bisq.mobile.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import network.bisq.mobile.ios.cfDictionaryOf
import network.bisq.mobile.ios.toByteArray
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.dataWithLength
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrLabel
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecUseDataProtectionKeychain
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private object LocalEncryption {
    private const val KEY_SIZE = 32 // 256 bits
    private val AES128_BLOCK_SIZE = kCCBlockSizeAES128.toInt() // 16 bytes
    private const val SERVICE_NAME = "network.bisq.mobile"


    private fun generateAndStoreSymmetricKey(keyAlias: String) {
        val keyData = NSMutableData.dataWithLength(KEY_SIZE.toULong()) as NSMutableData
        val randomResult = SecRandomCopyBytes(
            kSecRandomDefault,
            KEY_SIZE.toULong(),
            keyData.mutableBytes
        )
        if (randomResult != errSecSuccess) {
            throw IllegalStateException("SecRandomCopyBytes failed with status: $randomResult")
        }

        memScoped {
            val status = SecItemAdd(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrLabel to keyAlias,
                    kSecAttrAccount to "Account $keyAlias",
                    kSecAttrService to "Service $SERVICE_NAME",
                    kSecUseDataProtectionKeychain to kCFBooleanTrue,
                    kSecValueData to keyData
                ),
                null,
            )
            if (status != errSecSuccess && status != errSecDuplicateItem) {
                throw IllegalStateException("Failed to store '$keyAlias' key with status: $status")
            }
        }
    }

    private fun retrieveSymmetricKey(keyAlias: String): ByteArray? {
        memScoped {
            val result = alloc<CFTypeRefVar>()
            return when (val status = SecItemCopyMatching(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrAccount to "Account $keyAlias",
                    kSecAttrService to "Service $SERVICE_NAME",
                    kSecUseDataProtectionKeychain to kCFBooleanTrue,
                    kSecReturnData to kCFBooleanTrue
                ),
                result.ptr,
            )) {
                errSecSuccess -> (CFBridgingRelease(result.value) as? NSData)?.toByteArray()
                errSecItemNotFound -> null
                else -> throw IllegalStateException("Error reading key: $status")
            }
        }
    }

    private fun getOrCreateKey(keyAlias: String): ByteArray {
        return retrieveSymmetricKey(keyAlias) ?: run {
            generateAndStoreSymmetricKey(keyAlias)
            retrieveSymmetricKey(keyAlias)
                ?: throw IllegalStateException("Failed to retrieve generated key")
        }
    }

    fun encrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val keyData = getOrCreateKey(keyAlias)

        // Generate random IV
        val iv = ByteArray(AES128_BLOCK_SIZE)
        val ivData = NSMutableData.dataWithLength(AES128_BLOCK_SIZE.toULong()) as NSMutableData
        val randomResult =
            SecRandomCopyBytes(kSecRandomDefault, AES128_BLOCK_SIZE.toULong(), ivData.mutableBytes)
        if (randomResult != errSecSuccess) {
            throw IllegalStateException("Failed to generate IV: $randomResult")
        }
        (ivData as NSData).toByteArray().copyInto(iv)

        // Perform encryption
        memScoped {
            val dataOutAvailable = bytes.size + AES128_BLOCK_SIZE
            val dataOut = allocArray<platform.posix.uint8_tVar>(dataOutAvailable)
            val dataOutMoved = alloc<ULongVar>()

            val status = bytes.usePinned { dataPin ->
                keyData.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        CCCrypt(
                            kCCEncrypt,
                            kCCAlgorithmAES,
                            kCCOptionPKCS7Padding,
                            keyPin.addressOf(0),
                            KEY_SIZE.toULong(),
                            ivPin.addressOf(0),
                            dataPin.addressOf(0),
                            bytes.size.toULong(),
                            dataOut,
                            dataOutAvailable.toULong(),
                            dataOutMoved.ptr
                        )
                    }
                }
            }

            if (status != kCCSuccess) {
                throw IllegalStateException("Encryption failed with status: $status")
            }

            // Prepend IV to encrypted data
            val encryptedSize = dataOutMoved.value.toInt()
            val result = ByteArray(AES128_BLOCK_SIZE + encryptedSize)
            iv.copyInto(result, 0, 0, AES128_BLOCK_SIZE)
            for (i in 0 until encryptedSize) {
                result[AES128_BLOCK_SIZE + i] = dataOut[i].toByte()
            }
            return result
        }
    }

    fun decrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val keyData = getOrCreateKey(keyAlias)

        if (bytes.size < AES128_BLOCK_SIZE) {
            throw IllegalArgumentException("Invalid encrypted data: too short")
        }

        // Extract IV and encrypted data
        val iv = bytes.copyOfRange(0, AES128_BLOCK_SIZE)
        val encryptedData = bytes.copyOfRange(AES128_BLOCK_SIZE, bytes.size)

        // Perform decryption
        memScoped {
            val dataOutAvailable = encryptedData.size + AES128_BLOCK_SIZE
            val dataOut = allocArray<platform.posix.uint8_tVar>(dataOutAvailable)
            val dataOutMoved = alloc<ULongVar>()

            val status = encryptedData.usePinned { dataPin ->
                keyData.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        CCCrypt(
                            kCCDecrypt,
                            kCCAlgorithmAES,
                            kCCOptionPKCS7Padding,
                            keyPin.addressOf(0),
                            KEY_SIZE.toULong(),
                            ivPin.addressOf(0),
                            dataPin.addressOf(0),
                            encryptedData.size.toULong(),
                            dataOut,
                            dataOutAvailable.toULong(),
                            dataOutMoved.ptr
                        )
                    }
                }
            }

            if (status != kCCSuccess) {
                throw IllegalStateException("Decryption failed with status: $status")
            }

            val decryptedSize = dataOutMoved.value.toInt()
            return ByteArray(decryptedSize) { i ->
                dataOut[i].toByte()
            }
        }
    }
}

actual fun encrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.encrypt(data, keyAlias)
}

actual fun decrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.decrypt(data, keyAlias)
}
