package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import network.bisq.mobile.data.model.Settings
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
class SettingsSerializerTest {
    @Test
    fun `defaultValue returns empty Settings`() {
        assertEquals(Settings(), SettingsSerializer.defaultValue)
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = SettingsSerializer.readFrom(Buffer())

            assertEquals(Settings(), result)
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            val expected = sampleSettings()
            val json = dataStoreJson.encodeToString(Settings.serializer(), expected)

            val result = SettingsSerializer.readFrom(Buffer().writeUtf8(json))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    SettingsSerializer.readFrom(Buffer().writeUtf8("{"))
                }

            assertEquals("Cannot deserialize Settings", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips Settings`() =
        runTest {
            val original = sampleSettings()
            val buffer = Buffer()

            SettingsSerializer.writeTo(original, buffer)
            val restored = SettingsSerializer.readFrom(buffer)

            assertEquals(original, restored)
        }

    private fun sampleSettings() =
        Settings(
            firstLaunch = false,
            selectedMarketCode = "BTC/EUR",
            rememberOfferbookFilterPreferences = false,
        )
}
