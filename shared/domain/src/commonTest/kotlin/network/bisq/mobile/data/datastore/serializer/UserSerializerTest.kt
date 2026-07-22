package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import network.bisq.mobile.data.model.User
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
class UserSerializerTest {
    @Test
    fun `defaultValue returns empty User`() {
        assertEquals(User(), UserSerializer.defaultValue)
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = UserSerializer.readFrom(Buffer())

            assertEquals(User(), result)
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            val expected = sampleUser()
            val json = dataStoreJson.encodeToString(User.serializer(), expected)

            val result = UserSerializer.readFrom(Buffer().writeUtf8(json))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    UserSerializer.readFrom(Buffer().writeUtf8("{"))
                }

            assertEquals("Cannot deserialize User", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips User`() =
        runTest {
            val original = sampleUser()
            val buffer = Buffer()

            UserSerializer.writeTo(original, buffer)
            val restored = UserSerializer.readFrom(buffer)

            assertEquals(original, restored)
        }

    private fun sampleUser() =
        User(
            tradeTerms = "terms",
            statement = "statement",
        )
}
