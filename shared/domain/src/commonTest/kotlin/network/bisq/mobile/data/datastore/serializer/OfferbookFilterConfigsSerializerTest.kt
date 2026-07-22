package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
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
class OfferbookFilterConfigsSerializerTest {
    @Test
    fun `defaultValue returns empty OfferbookFilterConfigs`() {
        assertEquals(OfferbookFilterConfigs(), OfferbookFilterConfigsSerializer.defaultValue)
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = OfferbookFilterConfigsSerializer.readFrom(Buffer())

            assertEquals(OfferbookFilterConfigs(), result)
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            val expected = sampleConfigs()
            val json = dataStoreJson.encodeToString(OfferbookFilterConfigs.serializer(), expected)

            val result = OfferbookFilterConfigsSerializer.readFrom(Buffer().writeUtf8(json))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    OfferbookFilterConfigsSerializer.readFrom(Buffer().writeUtf8("{"))
                }

            assertEquals("Cannot deserialize OfferbookFilterConfigs", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips OfferbookFilterConfigs`() =
        runTest {
            val original = sampleConfigs()
            val buffer = Buffer()

            OfferbookFilterConfigsSerializer.writeTo(original, buffer)
            val restored = OfferbookFilterConfigsSerializer.readFrom(buffer)

            assertEquals(original, restored)
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
