package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

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

    // Entries written before the reported markers existed must still load, with no markers.
    @Test
    fun `readFrom loads an entry without reported markers`() =
        runTest {
            val source = Buffer().writeUtf8("""{"map":{"trade-1":{"stateName":"INIT","transitionAtMs":1000}}}""")

            assertEquals(
                TradeStallClockMap(mapOf("trade-1" to TradeStallClockEntry("INIT", 1_000L))),
                TradeStallClockMapSerializer.readFrom(source),
            )
        }

    // The null transition time is part of the contract (first sighting), so the round-trip sample
    // carries both a stamped and an unstamped entry, and one with reported markers.
    private fun sampleMap() =
        TradeStallClockMap(
            mapOf(
                "trade-1" to TradeStallClockEntry("INIT", 1_000L),
                "trade-2" to TradeStallClockEntry("BTC_CONFIRMED", null, setOf("completed", "phase:buyer_4_complete")),
            ),
        )
}
