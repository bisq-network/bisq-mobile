package network.bisq.mobile.presentation.settings.payment_accounts_musig.detail

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
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
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
class PaymentAccountMusigDetailPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var accountsByNameFlow: MutableStateFlow<Map<String, network.bisq.mobile.domain.model.account.PaymentAccount>>

    private lateinit var presenter: PaymentAccountMusigDetailPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        paymentAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)
        accountsByNameFlow = MutableStateFlow(emptyMap())

        startKoin {
            modules(
                module {
                    single<NavigationManager> { navigationManager }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { globalUiManager }
                },
            )
        }

        every { paymentAccountsServiceFacade.accountsByName } returns accountsByNameFlow
        every { globalUiManager.scheduleShowLoading() } returns Unit
        every { globalUiManager.hideLoading() } returns Unit
        every { navigationManager.navigateBack(any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): PaymentAccountMusigDetailPresenter =
        PaymentAccountMusigDetailPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initialize with matching account then sets payment account and clears missing flag`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            presenter = createPresenter()

            // When
            presenter.initialize(account.accountName)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(account, state.paymentAccount)
            assertFalse(state.isAccountMissing)
        }

    @Test
    fun `when initialize with unknown account then marks account as missing`() =
        runTest(testDispatcher) {
            // Given
            val existingAccount = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(existingAccount.accountName to existingAccount)
            presenter = createPresenter()

            // When
            presenter.initialize("Bob")
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(null, state.paymentAccount)
            assertTrue(state.isAccountMissing)
        }

    @Test
    fun `when delete action triggered then shows delete confirmation dialog`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when cancel delete action triggered then hides delete confirmation dialog`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnCancelDeleteAccountClick)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when confirm delete without selected account then does not call delete or navigation`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { paymentAccountsServiceFacade.deleteAccount(any()) }
            verify(exactly = 0) { navigationManager.navigateBack(any()) }
            verify(exactly = 0) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 0) { globalUiManager.hideLoading() }
        }

    @Test
    fun `when confirm delete succeeds then hides dialog shows loading lifecycle and navigates back`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            coEvery { paymentAccountsServiceFacade.deleteAccount(account) } returns Result.success(Unit)
            presenter = createPresenter()
            presenter.initialize(account.accountName)
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.deleteAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            verify(exactly = 1) { navigationManager.navigateBack(any()) }
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when confirm delete fails then hides dialog loading and shows error snackbar without navigation`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            coEvery { paymentAccountsServiceFacade.deleteAccount(account) } returns Result.failure(IllegalStateException("delete failed"))
            presenter = createPresenter()
            presenter.initialize(account.accountName)
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.deleteAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            verify(exactly = 0) { navigationManager.navigateBack(any()) }
            verify {
                globalUiManager.showSnackbar(
                    any(),
                    SnackbarType.ERROR,
                    any(),
                    any(),
                )
            }
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    private fun sampleZelleAccount(): ZelleAccount =
        ZelleAccount(
            accountName = "Alice",
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
}
