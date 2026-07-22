package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class SettingsSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = SettingsSerializer,
            defaultValue = Settings(),
            sampleValue = ::sampleSettings,
            typeName = "Settings",
            kSerializer = Settings.serializer(),
        )

    @Test
    fun `defaultValue returns empty Settings`() {
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
    fun `writeTo round trips Settings`() =
        runTest {
            support.assertRoundTrip()
        }

    private fun sampleSettings() =
        Settings(
            firstLaunch = false,
            selectedMarketCode = "BTC/EUR",
            rememberOfferbookFilterPreferences = false,
        )
}
