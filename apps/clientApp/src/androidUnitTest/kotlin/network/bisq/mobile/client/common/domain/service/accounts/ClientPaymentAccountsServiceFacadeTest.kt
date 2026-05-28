package network.bisq.mobile.client.common.domain.service.accounts

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.service.accounts.all.ClientPaymentAccountsServiceFacade
import network.bisq.mobile.client.common.domain.service.accounts.all.PaymentAccountsApiGateway
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentMethodDto
import network.bisq.mobile.data.model.account.fiat.BankAccountCountryDetailsDto
import network.bisq.mobile.data.model.account.fiat.CountryDto
import network.bisq.mobile.data.model.account.fiat.FiatCurrencyDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.data.service.accounts.PaymentAccountNameAlreadyExistsException
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.repository.BankAccountCountryDetailsRepository
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientPaymentAccountsServiceFacadeTest {
    private val apiGateway: PaymentAccountsApiGateway = mockk(relaxed = true)
    private val bankAccountCountryDetailsRepository: BankAccountCountryDetailsRepository = mockk()
    private val facade = ClientPaymentAccountsServiceFacade(apiGateway, bankAccountCountryDetailsRepository)

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
            val state = facade.accountsFlow.first()
            assertEquals(2, state.size)
            assertIs<UserDefinedFiatAccount>(state[0])
            assertEquals("A", state[0].accountName)
            assertEquals("B", state[1].accountName)
            val accountsByName = facade.accountsByName.first()
            assertEquals(setOf("A", "B"), accountsByName.keys)
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
            assertTrue(facade.accountsFlow.first().isEmpty())
            assertTrue(facade.accountsByName.first().isEmpty())
        }

    @Test
    fun `when addAccount succeeds then maps dto and appends in sorted state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val accountB = sampleCreateAccountB()
            coEvery { apiGateway.addAccount(any()) } returns Result.success(sampleAccountDto("B"))

            // When
            val result = facade.addAccount(accountB)

            // Then
            assertTrue(result.isSuccess)
            val state = facade.accountsFlow.first()
            assertEquals(listOf("A", "B"), state.map { it.accountName })
            val accountsByName = facade.accountsByName.first()
            assertEquals(setOf("A", "B"), accountsByName.keys)
            coVerify(exactly = 1) { apiGateway.addAccount(any()) }
        }

    @Test
    fun `when addAccount conflicts then maps to duplicate account exception and keeps existing state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val mapBefore = facade.accountsByName.first()
            val exception = WebSocketRestApiException(HttpStatusCode.Conflict, "Payment account already exists: B")
            coEvery { apiGateway.addAccount(any()) } returns Result.failure(exception)

            // When
            val result = facade.addAccount(sampleCreateAccountB())

            // Then
            assertTrue(result.isFailure)
            val mappedException = assertIs<PaymentAccountNameAlreadyExistsException>(result.exceptionOrNull())
            assertEquals(exception.message, mappedException.message)
            assertEquals(listOf("A"), currentAccountNames())
            assertEquals(mapBefore, facade.accountsByName.first())
        }

    @Test
    fun `when addAccount fails then returns failure and keeps existing state`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val mapBefore = facade.accountsByName.first()
            val exception = RuntimeException("add failed")
            coEvery { apiGateway.addAccount(any()) } returns Result.failure(exception)

            // When
            val result = facade.addAccount(sampleCreateAccountB())

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            assertEquals(listOf("A"), currentAccountNames())
            assertEquals(mapBefore, facade.accountsByName.first())
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
            val result = facade.deleteAccount("A")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("B"), currentAccountNames())
            val accountsByName = facade.accountsByName.first()
            assertEquals(setOf("B"), accountsByName.keys)
            coVerify(exactly = 1) { apiGateway.deleteAccount("A") }
        }

    @Test
    fun `when deleteAccount fails then returns failure and keeps state unchanged`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A")))
            facade.getAccounts()
            val mapBefore = facade.accountsByName.first()
            val exception = RuntimeException("delete failed")
            coEvery { apiGateway.deleteAccount("A") } returns Result.failure(exception)

            // When
            val result = facade.deleteAccount("A")

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            assertEquals(listOf("A"), currentAccountNames())
            assertEquals(mapBefore, facade.accountsByName.first())
        }

    @Test
    fun `when getAccounts has duplicate names then returns failure and keeps map state unchanged`() =
        runTest {
            // Given
            coEvery { apiGateway.getPaymentAccounts() } returns Result.success(listOf(sampleAccountDto("A"), sampleAccountDto("A")))

            // When
            val result = facade.getAccounts()

            // Then
            assertTrue(result.isFailure)
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(facade.accountsFlow.first().isEmpty())
            assertTrue(facade.accountsByName.first().isEmpty())
        }

    @Test
    fun `when getFiatPaymentMethods succeeds then returns FiatPaymentMethod list`() =
        runTest {
            // Given
            val fiatMethodDto =
                FiatPaymentMethodDto(
                    paymentRail = FiatPaymentRailDto.SEPA,
                    name = "SEPA",
                    supportedCurrencies = listOf(FiatCurrencyDto(code = "EUR", name = "Euro")),
                    supportedCountries = listOf(CountryDto(code = "DE", name = "Germany")),
                    matchesAllCountries = false,
                    chargebackRisk = FiatPaymentMethodChargebackRiskDto.LOW,
                    tradeLimitInfo = EMPTY_STRING,
                    tradeDuration = EMPTY_STRING,
                )
            coEvery { apiGateway.getFiatPaymentMethods() } returns Result.success(listOf(fiatMethodDto))

            // When
            val result = facade.getFiatPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            val methods = result.getOrThrow()
            assertEquals(1, methods.size)
            assertIs<FiatPaymentMethod>(methods.single())
            coVerify(exactly = 1) { apiGateway.getFiatPaymentMethods() }
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
    fun `when getCryptoPaymentMethods succeeds then returns CryptoPaymentMethod list`() =
        runTest {
            // Given
            val cryptoMethodDto =
                CryptoPaymentMethodDto(
                    code = "XMR",
                    name = "Monero",
                    supportAutoConf = true,
                    tradeLimitInfo = EMPTY_STRING,
                    tradeDuration = EMPTY_STRING,
                )
            coEvery { apiGateway.getCryptoPaymentMethods() } returns Result.success(listOf(cryptoMethodDto))

            // When
            val result = facade.getCryptoPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            val methods = result.getOrThrow()
            assertEquals(1, methods.size)
            assertIs<CryptoPaymentMethod>(methods.single())
            coVerify(exactly = 1) { apiGateway.getCryptoPaymentMethods() }
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

    @Test
    fun `when getBankAccountCountryDetails succeeds then delegates to repository with backend refresh`() =
        runTest {
            // Given
            val germany = sampleGermanBankAccountCountryDetails()
            coEvery {
                bankAccountCountryDetailsRepository.get("DE", any())
            } coAnswers {
                secondArg<suspend () -> List<BankAccountCountryDetailsDto>>().invoke().single()
            }
            coEvery { apiGateway.getBankAccountCountryDetails() } returns Result.success(listOf(germany))

            // When
            val result = facade.getBankAccountCountryDetails("DE")

            // Then
            assertTrue(result.isSuccess)
            val details = result.getOrThrow()
            assertIs<BankAccountCountryDetails>(details)
            assertEquals(germany.country.code, details.country.code)
            assertEquals(germany.country.name, details.country.name)
            assertEquals(germany.bankNameRequired, details.bankNameRequired)
            coVerify(exactly = 1) { bankAccountCountryDetailsRepository.get("DE", any()) }
            coVerify(exactly = 1) { apiGateway.getBankAccountCountryDetails() }
        }

    @Test
    fun `when getBankAccountCountryDetails repository fails then returns failure`() =
        runTest {
            // Given
            val exception = IllegalStateException("missing country")
            coEvery { bankAccountCountryDetailsRepository.get("US", any()) } throws exception

            // When
            val result = facade.getBankAccountCountryDetails("US")

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    private suspend fun currentAccountNames(): List<String> = facade.accountsFlow.first().map { it.accountName }

    private fun sampleCreateAccountB(): CreateUserDefinedFiatAccount =
        CreateUserDefinedFiatAccount(
            accountName = "B",
            accountPayload =
                CreateUserDefinedFiatAccountPayload(
                    accountData = "account-data",
                ),
        )

    private fun sampleAccountDto(accountName: String): PaymentAccountDto =
        UserDefinedFiatAccountDto(
            accountName = accountName,
            accountPayload =
                UserDefinedFiatAccountPayloadDto(
                    accountData = "account-data",
                ),
        )

    private fun sampleGermanBankAccountCountryDetails(): BankAccountCountryDetailsDto =
        BankAccountCountryDetailsDto(
            country = CountryDto(code = "DE", name = "Germany"),
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
