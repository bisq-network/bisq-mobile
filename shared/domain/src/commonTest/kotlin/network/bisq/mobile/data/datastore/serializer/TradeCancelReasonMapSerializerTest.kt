package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeCancelReasonMap
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TradeCancelReasonMapSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = TradeCancelReasonMapSerializer,
            defaultValue = TradeCancelReasonMap(),
            sampleValue = ::sampleMap,
            typeName = "TradeCancelReasonMap",
            kSerializer = TradeCancelReasonMap.serializer(),
        )

    @Test
    fun `defaultValue returns empty TradeCancelReasonMap`() {
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
    fun `writeTo round trips TradeCancelReasonMap`() =
        runTest {
            support.assertRoundTrip()
        }

    // A reason written by a newer app version must not crash an older one; the store's corruption
    // handler then resets it, which only loses the specific text.
    @Test
    fun `readFrom treats an unknown reason as corruption`() =
        runTest {
            val source = Buffer().writeUtf8("""{"map":{"trade-1":"SOME_FUTURE_REASON"}}""")

            assertFailsWith<CorruptionException> { TradeCancelReasonMapSerializer.readFrom(source) }
        }

    private fun sampleMap() =
        TradeCancelReasonMap(
            mapOf("trade-1" to TradeCancelReason.BANNED_ACCOUNT_DATA),
        )
}
