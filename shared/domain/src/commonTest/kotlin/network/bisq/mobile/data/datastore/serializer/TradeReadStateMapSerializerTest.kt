package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeReadStateMap
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class TradeReadStateMapSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = TradeReadStateMapSerializer,
            defaultValue = TradeReadStateMap(),
            sampleValue = ::sampleMap,
            typeName = "TradeReadStateMap",
            kSerializer = TradeReadStateMap.serializer(),
        )

    @Test
    fun `defaultValue returns empty TradeReadStateMap`() {
        support.assertDefaultValue()
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            support.assertExhaustedReturnsDefault()
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            support.assertDeserializesValidJson()
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            support.assertWrapsSerializationExceptionInCorruptionException()
        }

    @Test
    fun `writeTo round trips TradeReadStateMap`() =
        runTest {
            support.assertRoundTrip()
        }

    private fun sampleMap() = TradeReadStateMap(mapOf("trade-1" to 2))
}
