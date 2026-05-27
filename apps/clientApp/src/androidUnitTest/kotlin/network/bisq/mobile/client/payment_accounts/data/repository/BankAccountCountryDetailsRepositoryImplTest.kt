package network.bisq.mobile.client.payment_accounts.data.repository

import androidx.datastore.core.DataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details.BankAccountCountryDetailsCache
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.shared.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BankAccountCountryDetailsRepositoryImplTest {
    private val mockDataStore = mockk<DataStore<BankAccountCountryDetailsCache>>()
    private val repository = BankAccountCountryDetailsRepositoryImpl(mockDataStore)

    @Test
    fun `when valid local cache exists then returns country without refresh`() =
        runTest {
            // Given
            val germany = sampleDetails("DE", "Germany")
            every { mockDataStore.data } returns
                flowOf(
                    BankAccountCountryDetailsCache(
                        apiVersion = BuildConfig.BISQ_API_VERSION,
                        detailsByCountryCode = mapOf("DE" to germany),
                    ),
                )

            // When
            val result = repository.get("de") { error("Refresh should not be called") }

            // Then
            assertEquals(germany, result)
        }

    @Test
    fun `when cache is empty then fetches full list stores map and returns requested country`() =
        runTest {
            // Given
            val germany = sampleDetails("DE", "Germany")
            val france = sampleDetails("FR", "France")
            val updateSlot =
                slot<suspend (BankAccountCountryDetailsCache) -> BankAccountCountryDetailsCache>()
            every { mockDataStore.data } returns flowOf(BankAccountCountryDetailsCache())
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns
                BankAccountCountryDetailsCache(
                    apiVersion = BuildConfig.BISQ_API_VERSION,
                    detailsByCountryCode = mapOf("DE" to germany, "FR" to france),
                )
            var refreshCount = 0

            // When
            val result =
                repository.get("fr") {
                    refreshCount++
                    listOf(germany, france)
                }

            // Then
            assertEquals(france, result)
            assertEquals(1, refreshCount)
            val cached = updateSlot.captured(BankAccountCountryDetailsCache())
            assertEquals(BuildConfig.BISQ_API_VERSION, cached.apiVersion)
            assertEquals(setOf("DE", "FR"), cached.detailsByCountryCode.keys)
            coVerify(exactly = 1) { mockDataStore.updateData(any()) }
        }

    @Test
    fun `when cache api version is stale then refreshes full list`() =
        runTest {
            // Given
            val germany = sampleDetails("DE", "Germany")
            val staleGermany = sampleDetails("DE", "Stale Germany")
            every { mockDataStore.data } returns
                flowOf(
                    BankAccountCountryDetailsCache(
                        apiVersion = "stale-version",
                        detailsByCountryCode = mapOf("DE" to staleGermany),
                    ),
                )
            coEvery { mockDataStore.updateData(any()) } returns
                BankAccountCountryDetailsCache(
                    apiVersion = BuildConfig.BISQ_API_VERSION,
                    detailsByCountryCode = mapOf("DE" to germany),
                )

            // When
            val result = repository.get("DE") { listOf(germany) }

            // Then
            assertEquals(germany, result)
            coVerify(exactly = 1) { mockDataStore.updateData(any()) }
        }

    @Test
    fun `when called repeatedly then uses in memory map after first load`() =
        runTest {
            // Given
            val germany = sampleDetails("DE", "Germany")
            val france = sampleDetails("FR", "France")
            every { mockDataStore.data } returns
                flowOf(
                    BankAccountCountryDetailsCache(
                        apiVersion = BuildConfig.BISQ_API_VERSION,
                        detailsByCountryCode = mapOf("DE" to germany, "FR" to france),
                    ),
                )

            // When
            val first = repository.get("DE") { error("Refresh should not be called") }
            val second = repository.get("FR") { error("Refresh should not be called") }

            // Then
            assertEquals(germany, first)
            assertEquals(france, second)
            coVerify(exactly = 0) { mockDataStore.updateData(any()) }
        }

    @Test
    fun `when requested country is missing then throws`() =
        runTest {
            // Given
            every { mockDataStore.data } returns
                flowOf(
                    BankAccountCountryDetailsCache(
                        apiVersion = BuildConfig.BISQ_API_VERSION,
                        detailsByCountryCode = mapOf("DE" to sampleDetails("DE", "Germany")),
                    ),
                )

            // When & Then
            assertFailsWith<IllegalStateException> {
                repository.get("US") { error("Refresh should not be called") }
            }
        }

    private fun sampleDetails(
        code: String,
        name: String,
    ): BankAccountCountryDetailsDto =
        BankAccountCountryDetailsDto(
            country = CountryDto(code = code, name = name),
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
            accountNrDescription = "IBAN",
            nationalAccountIdRequired = false,
            nationalAccountIdDescription = "National Account ID",
            nationalAccountIdDescriptionShort = "National ID",
        )
}
