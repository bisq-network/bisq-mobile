package network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.data.datastore.dataStoreJson
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
class BankAccountCountryDetailsCacheSerializerTest {
    @Test
    fun `defaultValue returns empty BankAccountCountryDetailsCache`() {
        assertEquals(
            BankAccountCountryDetailsCache(),
            BankAccountCountryDetailsCacheSerializer.defaultValue,
        )
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = BankAccountCountryDetailsCacheSerializer.readFrom(Buffer())

            assertEquals(BankAccountCountryDetailsCache(), result)
        }

    @Test
    fun `readFrom deserializes valid JSON`() =
        runTest {
            val expected = sampleCache()
            val json = dataStoreJson.encodeToString(BankAccountCountryDetailsCache.serializer(), expected)

            val result = BankAccountCountryDetailsCacheSerializer.readFrom(Buffer().writeUtf8(json))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    BankAccountCountryDetailsCacheSerializer.readFrom(Buffer().writeUtf8("{"))
                }

            assertEquals("Cannot deserialize BankAccountCountryDetailsCache", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips BankAccountCountryDetailsCache`() =
        runTest {
            val original = sampleCache()
            val buffer = Buffer()

            BankAccountCountryDetailsCacheSerializer.writeTo(original, buffer)
            val restored = BankAccountCountryDetailsCacheSerializer.readFrom(buffer)

            assertEquals(original, restored)
        }

    private fun sampleCache() =
        BankAccountCountryDetailsCache(
            apiVersion = "1.0",
            detailsByCountryCode =
                mapOf(
                    "US" to
                        BankAccountCountryDetailsDto(
                            country = CountryDto(code = "US", name = "United States"),
                            bankAccountValidationSupported = true,
                            holderIdRequired = false,
                            holderIdDescription = "Holder ID",
                            holderIdDescriptionShort = "ID",
                            bankAccountTypeRequired = false,
                            bankNameRequired = true,
                            bankIdRequired = true,
                            bankIdDescription = "Bank ID",
                            bankIdDescriptionShort = "BIC",
                            branchIdRequired = false,
                            branchIdDescription = "Branch ID",
                            branchIdDescriptionShort = "Branch",
                            accountNrDescription = "Account number",
                            nationalAccountIdRequired = false,
                            nationalAccountIdDescription = "National Account ID",
                            nationalAccountIdDescriptionShort = "National ID",
                        ),
                ),
        )
}
