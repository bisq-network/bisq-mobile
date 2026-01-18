package network.bisq.mobile.client.common.domain.access.security

import network.bisq.mobile.client.common.domain.security.RawKeyPair

// iosMain
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PairingCryptoUtils {
    const val CURVE = "secp256r1"
    const val KEY_ALGORITHM = "EC"
    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

    actual fun generateKeyPair(): RawKeyPair {
        TODO("Not yet implemented")
    }

    actual fun generatePublic(encodedPublicKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun sign(message: ByteArray, encodedPrivateKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun verify(message: ByteArray, signature: ByteArray, encodedPublicKey: ByteArray): Boolean {
        TODO("Not yet implemented")
    }
    /*actual fun generateKeyPair(): KeyPair {
        val privateKey = P256.Signing.PrivateKey()
        val publicKey = privateKey.publicKey

        return KeyPair(
            publicKey = publicKey.derRepresentation.toByteArray(),
            privateKey = privateKey.derRepresentation.toByteArray()
        )
    }

    actual fun sign(message: ByteArray, privateKey: ByteArray): ByteArray {
        val privKey = P256.Signing.PrivateKey(
            derRepresentation = privateKey.toNSData()
        )

        val signature = privKey.signature(
            for = message.toNSData()
        )

        return signature.derRepresentation.toByteArray()
    }

    actual fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        val pubKey = P256.Signing.PublicKey(
            derRepresentation = publicKey.toNSData()
        )

        val sig = try {
            P256.Signing.ECDSASignature(
                derRepresentation = signature.toNSData()
            )
        } catch (_: Throwable) {
            return false
        }

        return pubKey.isValidSignature(
            sig,
            for = message.toNSData()
        )
    }
    actual fun generatePublic(encodedPublicKey: ByteArray): ByteArray {
        val ctx = Secp256k1.contextVerify()

        val pubkey = Secp256k1Pubkey()
        if (!Secp256k1.ecPubkeyParse(ctx, pubkey, encodedPublicKey)) {
            throw IllegalArgumentException("Invalid secp256k1 public key")
        }

        return Secp256k1.ecPubkeySerializeCompressed(ctx, pubkey)
    }
    private fun ByteArray.toNSData(): NSData =
        memScoped {
            NSData.create(
                bytes = allocArrayOf(this@toNSData),
                length = size.toULong()
            )
        }

    private fun NSData.toByteArray(): ByteArray {
        val array = ByteArray(length.toInt())
        memScoped {
            memcpy(
                array.refTo(0),
                bytes,
                length
            )
        }
        return array
    }*/

}
