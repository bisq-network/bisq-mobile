package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.PayoutAddressPrep
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class PayoutAddressPrepSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = PayoutAddressPrepSerializer,
            defaultValue = PayoutAddressPrep(),
            sampleValue = ::samplePrep,
            typeName = "PayoutAddressPrep",
            kSerializer = PayoutAddressPrep.serializer(),
        )

    @Test
    fun `defaultValue returns empty PayoutAddressPrep`() {
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
    fun `writeTo round trips PayoutAddressPrep`() =
        runTest {
            support.assertRoundTrip()
        }

    private fun samplePrep() =
        PayoutAddressPrep(
            prefillByTradeId = mapOf("trade-1" to "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"),
            profilesWithCompletedTrade = setOf("profile-1", "profile-2"),
        )
}
