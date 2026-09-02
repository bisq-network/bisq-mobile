package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class TradeStallClockMapSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = TradeStallClockMapSerializer,
            defaultValue = TradeStallClockMap(),
            sampleValue = ::sampleMap,
            typeName = "TradeStallClockMap",
            kSerializer = TradeStallClockMap.serializer(),
        )

    @Test
    fun `defaultValue returns empty TradeStallClockMap`() {
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
    fun `writeTo round trips TradeStallClockMap`() =
        runTest {
            support.assertRoundTrip()
        }

    // The null transition time is part of the contract (first sighting), so the round-trip sample
    // carries both a stamped and an unstamped entry.
    private fun sampleMap() =
        TradeStallClockMap(
            mapOf(
                "trade-1" to TradeStallClockEntry("INIT", 1_000L),
                "trade-2" to TradeStallClockEntry("BTC_CONFIRMED", null),
            ),
        )
}
