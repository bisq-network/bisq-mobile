package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountsMusigPresenterTest : ClientKoinIntegrationTestBase() {
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)
    private val accountsFlow = MutableStateFlow<List<PaymentAccount>>(emptyList())

    private lateinit var presenter: PaymentAccountsMusigPresenter

    override fun additionalModules(): List<Module> = listOf(module { single<NavigationManager> { navigationManager } })

    override fun onSetup() {
        every { paymentAccountsServiceFacade.accountsFlow } returns accountsFlow
        coEvery { paymentAccountsServiceFacade.getAccounts() } returns Result.success(Unit)
    }

    private fun createPresenter(): PaymentAccountsMusigPresenter =
        PaymentAccountsMusigPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then has expected defaults`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertTrue(state.fiatAccounts.isEmpty())
            assertTrue(state.cryptoAccounts.isEmpty())
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
            assertEquals(PaymentAccountTab.FIAT, state.selectedTab)
            assertFalse(state.showDeleteConfirmationDialog)
        }

    @Test
    fun `when view attached and get accounts succeeds then loading is cleared without error`() =
        runTest {
            // Given
            coEvery { paymentAccountsServiceFacade.getAccounts() } returns Result.success(Unit)
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify { paymentAccountsServiceFacade.getAccounts() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
        }

    @Test
    fun `when view attached and get accounts fails then error state is shown`() =
        runTest {
            // Given
            coEvery { paymentAccountsServiceFacade.getAccounts() } returns Result.failure(IllegalStateException("load failed"))
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify { paymentAccountsServiceFacade.getAccounts() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertTrue(state.isLoadingAccountsError)
        }

    @Test
    fun `when retry clicked after failure then get accounts is requested again and error clears`() =
        runTest {
            // Given
            coEvery { paymentAccountsServiceFacade.getAccounts() } returnsMany
                listOf(
                    Result.failure(IllegalStateException("first fail")),
                    Result.success(Unit),
                )
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnRetryLoadAccountsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { paymentAccountsServiceFacade.getAccounts() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
        }

    @Test
    fun `when fiat and crypto accounts emitted then ui state maps each account type correctly`() =
        runTest {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountsFlow.value =
                listOf(
                    sampleZelleAccount(),
                    sampleWiseAccount(),
                    sampleMoneroAccount(),
                )
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(2, state.fiatAccounts.size)
            assertTrue(state.fiatAccounts.any { it.accountName == "Zelle Personal" })
            assertTrue(state.fiatAccounts.any { it.accountName == "Wise Main" })
            assertEquals(1, state.cryptoAccounts.size)
            assertEquals("Monero Main", state.cryptoAccounts.first().accountName)
        }

    @Test
    fun `when only fiat accounts emitted then crypto list remains empty`() =
        runTest {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountsFlow.value = listOf(sampleZelleAccount())
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.fiatAccounts.size)
            assertTrue(state.cryptoAccounts.isEmpty())
        }

    @Test
    fun `when tab selected then selected tab state is updated`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnTabSelect(PaymentAccountTab.CRYPTO))
            advanceUntilIdle()

            // Then
            assertEquals(PaymentAccountTab.CRYPTO, presenter.uiState.value.selectedTab)
        }

    @Test
    fun `when add fiat action triggered then navigates to create payment account fiat route`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnAddFiatAccountClick)
            advanceUntilIdle()

            // Then
            verify {
                navigationManager.navigate(
                    ClientNavRoute.CreatePaymentAccount(accountType = PaymentAccountType.FIAT),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `when add crypto action triggered then navigates to create payment account crypto route`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnAddCryptoAccountClick)
            advanceUntilIdle()

            // Then
            verify {
                navigationManager.navigate(
                    ClientNavRoute.CreatePaymentAccount(accountType = PaymentAccountType.CRYPTO),
                    any(),
                    any(),
                )
            }
        }

    private fun sampleZelleAccount(accountName: String = "Zelle Personal"): ZelleAccount =
        ZelleAccount(
            accountName = accountName,
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.LOW,
                    paymentMethodName = "Zelle",
                    currency = FiatCurrency(code = "USD", name = "US Dollar"),
                    country = Country(code = "US", name = "United States"),
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleWiseAccount(accountName: String = "Wise Main"): WiseAccount =
        WiseAccount(
            accountName = accountName,
            accountPayload =
                WiseAccountPayload(
                    selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")),
                    holderName = "Satoshi",
                    email = "satoshi@example.com",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                    paymentMethodName = "Wise",
                ),
            creationDate = null,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    private fun sampleMoneroAccount(accountName: String = "Monero Main"): MoneroAccount =
        MoneroAccount(
            accountName = accountName,
            accountPayload =
                MoneroAccountPayload(
                    currencyName = "Monero",
                    address = "84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ",
                    isInstant = false,
                    useSubAddresses = false,
                    supportAutoConf = true,
                    currencyCode = "XMR",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )
}
