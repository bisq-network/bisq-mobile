package network.bisq.mobile.android.node.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

fun encryptFileAesGcm(input: File, output: File, password: String) {
    // AES-GCM with PBKDF2 key derivation
    // Output format: [salt(16)] [iv(12)] [ciphertext...]
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val key = deriveKey(password, salt)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)

    FileOutputStream(output).use { fos ->
        // write salt + iv upfront
        fos.write(salt)
        fos.write(iv)
        CipherOutputStream(fos, cipher).use { cos ->
            FileInputStream(input).use { fis ->
                fis.copyTo(cos)
            }
        }
    }
}

fun deriveKey(password: String, salt: ByteArray): ByteArray {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, 200_000, 256)
    val secret = factory.generateSecret(spec)
    return secret.encoded
}