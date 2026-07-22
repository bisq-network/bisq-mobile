package network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.test.datastore.jsonDataStoreSerializerTestSupport
import kotlin.test.Test

class BankAccountCountryDetailsCacheSerializerTest {
    private val support =
        jsonDataStoreSerializerTestSupport(
            serializer = BankAccountCountryDetailsCacheSerializer,
            defaultValue = BankAccountCountryDetailsCache(),
            sampleValue = ::sampleCache,
            typeName = "BankAccountCountryDetailsCache",
            kSerializer = BankAccountCountryDetailsCache.serializer(),
        )

    @Test
    fun `defaultValue returns empty BankAccountCountryDetailsCache`() {
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
    fun `writeTo round trips BankAccountCountryDetailsCache`() =
        runTest {
            support.assertRoundTrip()
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
