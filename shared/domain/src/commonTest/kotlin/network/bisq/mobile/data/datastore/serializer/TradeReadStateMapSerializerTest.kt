package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import network.bisq.mobile.data.model.TradeReadStateMap
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * IllegalArgumentException wrapping ("Cannot read …") is intentionally not tested: [dataStoreJson]
 * decode failures surface as [SerializationException] (including JsonDecodingException), and this
 * model has no init validation that would throw plain [IllegalArgumentException] during decode.
 */
class TradeReadStateMapSerializerTest {
    @Test
    fun `defaultValue returns empty TradeReadStateMap`() {
        assertEquals(TradeReadStateMap(), TradeReadStateMapSerializer.defaultValue)
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = TradeReadStateMapSerializer.readFrom(Buffer())

            assertEquals(TradeReadStateMap(), result)
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            val expected = sampleMap()
            val json = dataStoreJson.encodeToString(TradeReadStateMap.serializer(), expected)

            val result = TradeReadStateMapSerializer.readFrom(Buffer().writeUtf8(json))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    TradeReadStateMapSerializer.readFrom(Buffer().writeUtf8("{"))
                }

            assertEquals("Cannot deserialize TradeReadStateMap", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips TradeReadStateMap`() =
        runTest {
            val original = sampleMap()
            val buffer = Buffer()

            TradeReadStateMapSerializer.writeTo(original, buffer)
            val restored = TradeReadStateMapSerializer.readFrom(buffer)

            assertEquals(original, restored)
        }

    private fun sampleMap() = TradeReadStateMap(mapOf("trade-1" to 2))
}
