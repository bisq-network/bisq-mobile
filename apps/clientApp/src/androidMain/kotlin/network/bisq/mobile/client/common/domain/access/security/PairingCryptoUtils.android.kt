package network.bisq.mobile.client.common.domain.access.security

import network.bisq.mobile.client.common.domain.security.RawKeyPair
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PairingCryptoUtils {
    init {
        Security.removeProvider("BC"); // remove Android's old one
        Security.insertProviderAt(BouncyCastleProvider(), 1);
    }

    const val CURVE = "secp256r1"
    const val KEY_ALGORITHM = "EC"
    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

    actual fun generateKeyPair(): RawKeyPair {
        try {
            val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
            generator.initialize(ECGenParameterSpec(CURVE))
            val keyPair = generator.generateKeyPair()

            return RawKeyPair(
                publicKey = keyPair.public.encoded,
                privateKey = keyPair.private.encoded
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate EC key pair", e)
        }
    }

    actual fun generatePublic(encodedPublicKey: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(encodedPublicKey)
        )
        return publicKey.encoded
    }

    actual fun sign(
        message: ByteArray,
        encodedPrivateKey: ByteArray
    ): ByteArray {
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(encodedPrivateKey)
        )

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(message)
        return signature.sign()
    }

    actual fun verify(
        message: ByteArray,
        signature: ByteArray,
        encodedPublicKey: ByteArray
    ): Boolean {
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(encodedPublicKey)
        )

        val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
        sig.initVerify(publicKey)
        sig.update(message)
        return sig.verify(signature)
    }
}
