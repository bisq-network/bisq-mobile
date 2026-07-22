package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class OfferbookFilterConfigsSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = OfferbookFilterConfigsSerializer,
            defaultValue = OfferbookFilterConfigs(),
            sampleValue = ::sampleConfigs,
            typeName = "OfferbookFilterConfigs",
            kSerializer = OfferbookFilterConfigs.serializer(),
        )

    @Test
    fun `defaultValue returns empty OfferbookFilterConfigs`() {
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
    fun `writeTo round trips OfferbookFilterConfigs`() =
        runTest {
            support.assertRoundTrip()
        }

    private fun sampleConfigs() =
        OfferbookFilterConfigs(
            configsByMarket =
                mapOf(
                    "BTC/EUR" to
                        OfferbookFilterConfig(
                            selectedPaymentMethodIds = setOf("SEPA"),
                            selectedSettlementMethodIds = setOf("MAIN_CHAIN"),
                            onlyMyOffers = true,
                            hasManualPaymentFilter = true,
                            hasManualSettlementFilter = false,
                        ),
                ),
        )
}
