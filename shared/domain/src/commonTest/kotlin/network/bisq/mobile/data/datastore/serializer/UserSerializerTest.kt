package network.bisq.mobile.data.datastore.serializer

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.User
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class UserSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = UserSerializer,
            defaultValue = User(),
            sampleValue = ::sampleUser,
            typeName = "User",
            kSerializer = User.serializer(),
        )

    @Test
    fun `defaultValue returns empty User`() {
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
    fun `writeTo round trips User`() =
        runTest {
            support.assertRoundTrip()
        }

    private fun sampleUser() =
        User(
            tradeTerms = "terms",
            statement = "statement",
        )
}
