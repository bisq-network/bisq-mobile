package network.bisq.mobile.client.common.domain.service.accounts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentMethodDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientPaymentAccountsServiceFacadeTest {
    private val apiGateway: PaymentAccountsApiGateway = mockk(relaxed = true)
    private val facade = ClientPaymentAccountsServiceFacade(apiGateway)

    @Test
    fun `when getAccounts succeeds then maps and updates sorted state`() =
        runTest {
            // Given
            val accountDtoB = sampleAccountDto(accountName = "B")
            val accountDtoA = sampleAccountDto(accountName = "A")
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(accountDtoB, accountDtoA))

            // When
            val result = facade.getAccounts()

            // Then
            assertTrue(result.isSuccess)
            val state = facade.accounts.value
            assertEquals(2, state.size)
            assertIs<UserDefinedFiatAccount>(state[0])
            assertEquals("A", state[0].accountName)
            assertEquals("B", state[1].accountName)
            coVerify(exactly = 1) { apiGateway.getPaymentAccounts() }
        }

    @Test
    fun `when getAccounts fails then returns failure and keeps state unchanged`() =
        runTest {
            // Given
            val exception = IllegalStateException("boom")
            coEvery { apiGateway.getPaymentAccounts() } returns Result.failure(exception)

            // When
            val result = facade.getAccounts()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            assertTrue(facade.accounts.value.isEmpty())
        }

    @Test
    fun `when addAccount succeeds then maps dto and appends in sorted state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val accountB = sampleDomainAccount("B")
            coEvery { apiGateway.addAccount(any()) } returns Result.success(sampleAccountDto("B"))

            // When
            val result = facade.addAccount(accountB)

            // Then
            assertTrue(result.isSuccess)
            val state = facade.accounts.value
            assertEquals(listOf("A", "B"), state.map { it.accountName })
            coVerify(exactly = 1) { apiGateway.addAccount(any()) }
        }

    @Test
    fun `when addAccount fails then returns failure and keeps existing state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val exception = RuntimeException("add failed")
            coEvery { apiGateway.addAccount(any()) } returns Result.failure(exception)

            // When
            val result = facade.addAccount(sampleDomainAccount("B"))

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            assertEquals(listOf("A"), facade.accounts.value.map { it.accountName })
        }

    @Test
    fun `when deleteAccount succeeds then removes account from state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns
                Result.success(
                    listOf(
                        sampleAccountDto("A"),
                        sampleAccountDto("B"),
                    ),
                )
            facade.getAccounts()
            coEvery { apiGateway.deleteAccount("A") } returns Result.success(Unit)

            // When
            val result = facade.deleteAccount(sampleDomainAccount("A"))

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("B"), facade.accounts.value.map { it.accountName })
            coVerify(exactly = 1) { apiGateway.deleteAccount("A") }
        }

    @Test
    fun `when deleteAccount fails then returns failure and keeps state unchanged`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val exception = RuntimeException("delete failed")
            coEvery { apiGateway.deleteAccount("A") } returns Result.failure(exception)

            // When
            val result = facade.deleteAccount(sampleDomainAccount("A"))

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            assertEquals(listOf("A"), facade.accounts.value.map { it.accountName })
        }

    @Test
    fun `when getFiatPaymentMethods succeeds then maps dto list to domain list`() =
        runTest {
            // Given
            val fiatMethodDto =
                FiatPaymentMethodDto(
                    paymentRail = FiatPaymentRailDto.SEPA,
                    name = "SEPA",
                    supportedCurrencyCodes = "EUR",
                    supportedNameAndCodes = "Euro (EUR)",
                    countryNames = "Germany",
                    chargebackRisk = FiatPaymentMethodChargebackRiskDto.LOW,
                )
            coEvery { apiGateway.getFiatPaymentMethods() } returns Result.success(listOf(fiatMethodDto))

            // When
            val result = facade.getFiatPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            val method = result.getOrThrow().single()
            assertEquals(FiatPaymentRail.SEPA, method.paymentRail)
            assertEquals("SEPA", method.name)
        }

    @Test
    fun `when getFiatPaymentMethods fails then returns failure`() =
        runTest {
            // Given
            val exception = RuntimeException("fiat failed")
            coEvery { apiGateway.getFiatPaymentMethods() } returns Result.failure(exception)

            // When
            val result = facade.getFiatPaymentMethods()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `when getCryptoPaymentMethods succeeds then maps dto list to domain list`() =
        runTest {
            // Given
            val cryptoMethodDto =
                CryptoPaymentMethodDto(
                    code = "XMR",
                    name = "Monero",
                    category = "PRIVACY",
                )
            coEvery { apiGateway.getCryptoPaymentMethods() } returns Result.success(listOf(cryptoMethodDto))

            // When
            val result = facade.getCryptoPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            val method = result.getOrThrow().single()
            assertEquals("XMR", method.code)
            assertEquals("Monero", method.name)
            assertEquals("PRIVACY", method.category)
        }

    @Test
    fun `when getCryptoPaymentMethods fails then returns failure`() =
        runTest {
            // Given
            val exception = RuntimeException("crypto failed")
            coEvery { apiGateway.getCryptoPaymentMethods() } returns Result.failure(exception)

            // When
            val result = facade.getCryptoPaymentMethods()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    private fun sampleDomainAccount(accountName: String): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            accountName = accountName,
            accountPayload =
                UserDefinedFiatAccountPayload(
                    accountData = "account-data",
                ),
        )

    private fun sampleAccountDto(accountName: String): PaymentAccountDto =
        UserDefinedFiatAccountDto(
            accountName = accountName,
            accountPayload = UserDefinedFiatAccountPayloadDto(accountData = "account-data"),
            tradeLimitInfo = null,
            tradeDuration = null,
            creationDate = null,
        )
}
