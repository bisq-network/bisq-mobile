package network.bisq.mobile.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private object LocalEncryption {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    private fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)
    private val keyStore = KeyStore
        .getInstance("AndroidKeyStore")
        .apply {
            load(null)
        }

    private fun getKey(keyAlias: String): SecretKey {
        val existingKey = keyStore
            .getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey(keyAlias)
    }

    private fun createKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val cipher = newCipher()
        cipher.init(Cipher.ENCRYPT_MODE, getKey(keyAlias))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(bytes)
        return iv + encrypted
    }

    fun decrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val cipher = newCipher()
        val iv = bytes.copyOfRange(0, cipher.blockSize)
        val data = bytes.copyOfRange(cipher.blockSize, bytes.size)
        cipher.init(Cipher.DECRYPT_MODE, getKey(keyAlias), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
}

actual fun encrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.encrypt(data, keyAlias)
}

actual fun decrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.decrypt(data, keyAlias)
}