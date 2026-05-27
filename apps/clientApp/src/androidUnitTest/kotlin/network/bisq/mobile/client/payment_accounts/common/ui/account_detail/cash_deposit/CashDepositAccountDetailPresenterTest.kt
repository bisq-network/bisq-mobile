package network.bisq.mobile.client.payment_accounts.common.ui.account_detail.cash_deposit

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailPresenter
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CashDepositAccountDetailPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var presenter: CashDepositAccountDetailPresenter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        paymentAccountsServiceFacade = mockk(relaxed = true)
        runCatching { stopKoin() }
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }
        presenter =
            CashDepositAccountDetailPresenter(paymentAccountsServiceFacade, mockk<MainPresenter>(relaxed = true))
    }

    @After
    fun tearDown() {
        presenter.onDestroy()
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize loads country details for account country`() =
        runTest(testDispatcher) {
            val details = sampleCountryDetails()
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(details)

            presenter.initialize(sampleAccount())
            advanceUntilIdle()

            assertEquals(details, presenter.uiState.value.countryDetails)
            assertFalse(presenter.uiState.value.isLoadingCountryDetails)
            assertFalse(presenter.uiState.value.isCountryDetailsError)
        }

    @Test
    fun `initialize clears loading and shows error when country details fail`() =
        runTest(testDispatcher) {
            coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.failure(RuntimeException("boom"))

            presenter.initialize(sampleAccount())
            advanceUntilIdle()

            assertNull(presenter.uiState.value.countryDetails)
            assertFalse(presenter.uiState.value.isLoadingCountryDetails)
            assertEquals(true, presenter.uiState.value.isCountryDetailsError)
        }

    private fun sampleAccount(): CashDepositAccount =
        CashDepositAccount(
            accountName = "Cash Deposit Main",
            accountPayload =
                CashDepositAccountPayload(
                    paymentMethodName = "Cash Deposit",
                    currency = FiatCurrency("USD", "US Dollar"),
                    country = Country("US", "United States"),
                    holderName = "Alice Doe",
                    bankName = "Bisq Bank",
                    accountNr = "123456789",
                ),
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleCountryDetails(): BankAccountCountryDetails =
        BankAccountCountryDetails(
            country = Country("US", "United States"),
            bankAccountValidationSupported = true,
            holderIdRequired = true,
            holderIdDescription = "Account owner ID",
            holderIdDescriptionShort = "Owner ID",
            bankAccountTypeRequired = true,
            bankNameRequired = true,
            bankIdRequired = true,
            bankIdDescription = "Routing number",
            bankIdDescriptionShort = "Routing",
            branchIdRequired = true,
            branchIdDescription = "Branch number",
            branchIdDescriptionShort = "Branch",
            accountNrDescription = "Account number",
            nationalAccountIdRequired = true,
            nationalAccountIdDescription = "National account number",
            nationalAccountIdDescriptionShort = "National ID",
        )
}
