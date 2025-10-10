package network.bisq.mobile.android.node.utils

import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val SALT_LEN = 16
private const val IV_LEN = 12
private const val GCM_TAG_BITS = 128
private const val PBKDF2_ITER = 600_000
private const val KEY_LEN_BITS = 256

private fun getCipher(): Cipher = Cipher.getInstance("AES/GCM/NoPadding")

fun encrypt(input: File, output: File, password: String) {
    val secureRandom = SecureRandom()
    val salt = ByteArray(SALT_LEN).also { secureRandom.nextBytes(it) }
    val iv = ByteArray(IV_LEN).also { secureRandom.nextBytes(it) }
    val key = deriveKey(password, salt)
    try {
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        FileOutputStream(output, false).use { fos ->
            fos.write(salt)
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(input).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    } finally {
        // Clear sensitive data
        salt.fill(0)
        iv.fill(0)
        key.encoded?.fill(0)
    }
}

/**
 * Decrypts an encrypted backup file.
 * @return A temporary file containing the decrypted content.
 * **Caller must delete this file after use.**
 */
fun decrypt(inputStream: InputStream, password: String): File {
    val salt = ByteArray(SALT_LEN).also { inputStream.readChunk(it) }
    val iv = ByteArray(IV_LEN).also { inputStream.readChunk(it) }
    val key = deriveKey(password, salt)

    try {
        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        val tempFile = File.createTempFile("decrypted_", ".zip")
        CipherInputStream(inputStream, cipher).use { cis ->
            FileOutputStream(tempFile).use { fos ->
                cis.copyTo(fos)
            }
        }

        // If password was wrong, GCM tag verification happens at cis.close()
        return tempFile
    } finally {
        // Clear sensitive data
        salt.fill(0)
        iv.fill(0)
        key.encoded?.fill(0)
    }
}

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val passwordChars = password.toCharArray()
    try {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passwordChars, salt, PBKDF2_ITER, KEY_LEN_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    } finally {
        passwordChars.fill('\u0000')
    }
}

private fun InputStream.readChunk(chunk: ByteArray) {
    var bytesRead = 0
    while (bytesRead < chunk.size) {
        val n = read(chunk, bytesRead, chunk.size - bytesRead)
        if (n == -1) throw EOFException("Unexpected end of stream")
        bytesRead += n
    }
}
