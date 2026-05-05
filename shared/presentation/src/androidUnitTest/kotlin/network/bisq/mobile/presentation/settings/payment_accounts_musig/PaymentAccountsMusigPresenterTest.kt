package network.bisq.mobile.presentation.settings.payment_accounts_musig

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountsMusigPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var accountsFlow: MutableStateFlow<List<PaymentAccount>>

    private lateinit var presenter: PaymentAccountsMusigPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        paymentAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)
        accountsFlow = MutableStateFlow(emptyList())

        startKoin {
            modules(
                module {
                    single<NavigationManager> { navigationManager }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    single<GlobalUiManager> { globalUiManager }
                },
            )
        }

        every { paymentAccountsServiceFacade.accountsFlow } returns accountsFlow
        coEvery { paymentAccountsServiceFacade.getAccounts() } returns Result.success(Unit)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): PaymentAccountsMusigPresenter =
        PaymentAccountsMusigPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then has expected defaults`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountsFlow.value =
                listOf(
                    sampleZelleAccount(accountName = "Zelle Personal"),
                    sampleMoneroAccount(),
                )
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.fiatAccounts.size)
            assertEquals("Zelle Personal", state.fiatAccounts.first().accountName)
            assertEquals(1, state.cryptoAccounts.size)
            assertEquals("Monero Main", state.cryptoAccounts.first().accountName)
        }

    @Test
    fun `when only fiat accounts emitted then crypto list remains empty`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountsFlow.value = listOf(sampleZelleAccount(accountName = "Only Fiat"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.fiatAccounts.size)
            assertTrue(state.cryptoAccounts.isEmpty())
        }

    @Test
    fun `when tab selected then selected tab state is updated`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnAddFiatAccountClick)
            advanceUntilIdle()

            // Then
            verify {
                navigationManager.navigate(
                    NavRoute.CreatePaymentAccount(accountType = PaymentAccountType.FIAT),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `when add crypto action triggered then navigates to create payment account crypto route`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountsMusigUiAction.OnAddCryptoAccountClick)
            advanceUntilIdle()

            // Then
            verify {
                navigationManager.navigate(
                    NavRoute.CreatePaymentAccount(accountType = PaymentAccountType.CRYPTO),
                    any(),
                    any(),
                )
            }
        }

    private fun sampleZelleAccount(accountName: String): ZelleAccount =
        ZelleAccount(
            accountName = accountName,
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.LOW,
                    paymentMethodName = "Zelle",
                    currency = "USD",
                    country = "United States",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
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
