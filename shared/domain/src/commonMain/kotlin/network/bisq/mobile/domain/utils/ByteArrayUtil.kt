package network.bisq.mobile.domain.utils

import io.ktor.util.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.ByteString.Companion.decodeBase64

object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ByteArrayAsBase64", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64String = value.encodeBase64()
        encoder.encodeString(base64String)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64String = decoder.decodeString()
        return base64String.decodeBase64()?.toByteArray()!!
    }
}

fun ByteArray.toHex(): String {
    return joinToString("") {
        it.toUByte()
            .toString(16)
            .padStart(2, '0')
    }
}


fun String.base64ToByteArray(): ByteArray? {
    return decodeBase64()?.toByteArray()
}

fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have an even length" }

    return chunked(2)
        .map { it.toUByte(16).toByte() }
        .toByteArray()
}

fun concat(vararg byteArrays: ByteArray): ByteArray {
    val totalLength = byteArrays.sumOf { it.size }
    val result = ByteArray(totalLength)
    var currentIndex = 0
    for (array in byteArrays) {
        array.copyInto(result, currentIndex)
        currentIndex += array.size
    }
    return result
}
