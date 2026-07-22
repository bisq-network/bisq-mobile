package network.bisq.mobile.test.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.Buffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Shared contract tests for [network.bisq.mobile.data.datastore.serializer.jsonDataStoreSerializer].
 *
 * IllegalArgumentException wrapping ("Cannot read …") is intentionally not tested: [dataStoreJson]
 * decode failures surface as [SerializationException] (including JsonDecodingException), and these
 * models have no init validation that would throw plain [IllegalArgumentException] during decode.
 */
class JsonDataStoreSerializerTestSupport<T>(
    private val serializer: OkioSerializer<T>,
    private val defaultValue: T,
    private val sampleValue: () -> T,
    private val typeName: String,
    private val kSerializer: KSerializer<T>,
) {
    fun assertDefaultValue() {
        assertEquals(defaultValue, serializer.defaultValue)
    }

    suspend fun assertExhaustedReturnsDefault() {
        val result = serializer.readFrom(Buffer())

        assertEquals(defaultValue, result)
    }

    suspend fun assertDeserializesValidJson() {
        val expected = sampleValue()
        val json = dataStoreJson.encodeToString(kSerializer, expected)

        val result = serializer.readFrom(Buffer().writeUtf8(json))

        assertEquals(expected, result)
    }

    suspend fun assertWrapsSerializationExceptionInCorruptionException() {
        val exception =
            assertFailsWith<CorruptionException> {
                serializer.readFrom(Buffer().writeUtf8("{"))
            }

        assertEquals("Cannot deserialize $typeName", exception.message)
        assertIs<SerializationException>(exception.cause)
    }

    suspend fun assertRoundTrip() {
        val original = sampleValue()
        val buffer = Buffer()

        serializer.writeTo(original, buffer)
        val restored = serializer.readFrom(buffer)

        assertEquals(original, restored)
    }
}

fun <T> jsonDataStoreSerializerTestSupport(
    serializer: OkioSerializer<T>,
    defaultValue: T,
    sampleValue: () -> T,
    typeName: String,
    kSerializer: KSerializer<T>,
): JsonDataStoreSerializerTestSupport<T> =
    JsonDataStoreSerializerTestSupport(
        serializer = serializer,
        defaultValue = defaultValue,
        sampleValue = sampleValue,
        typeName = typeName,
        kSerializer = kSerializer,
    )
