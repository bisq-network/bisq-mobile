package network.bisq.mobile.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// All android 10+ devices support GCM
private object LocalEncryption {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH = 128

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
        if (bytes.size < IV_LENGTH_BYTES + GCM_TAG_LENGTH / 8) {
            throw IllegalStateException("Encrypted payload too short to contain IV and authentication tag")
        }
        val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
        val data = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
        cipher.init(Cipher.DECRYPT_MODE, getKey(keyAlias), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(data)
    }
}

actual suspend fun encrypt(data: ByteArray, keyAlias: String): ByteArray {
    return withContext(Dispatchers.Default) {
        LocalEncryption.encrypt(data, keyAlias)
    }
}

actual suspend fun decrypt(data: ByteArray, keyAlias: String): ByteArray {
    return withContext(Dispatchers.Default) {
        LocalEncryption.decrypt(data, keyAlias)
    }
}
