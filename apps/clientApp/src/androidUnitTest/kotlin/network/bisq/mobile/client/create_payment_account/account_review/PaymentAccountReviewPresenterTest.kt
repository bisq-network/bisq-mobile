package network.bisq.mobile.client.create_payment_account.account_review

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.PaymentAccountNameAlreadyExistsException
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.utils.CoroutineJobsManager
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountReviewPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var presenter: PaymentAccountReviewPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        paymentAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { globalUiManager }
                },
            )
        }

        every { globalUiManager.scheduleShowLoading() } returns Unit
        every { globalUiManager.hideLoading() } returns Unit
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): PaymentAccountReviewPresenter =
        PaymentAccountReviewPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then loading is true and payment account is null`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertTrue(state.isLoading)
            assertNull(state.paymentAccount)
        }

    @Test
    fun `when initialized then clears loading and derives review payment account state`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleCreateZelleAccount()
            presenter = createPresenter()

            // When
            presenter.initialize(account, samplePaymentMethod())
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            val paymentAccount = assertIs<ZelleAccount>(state.paymentAccount)
            assertEquals("Zelle Personal", paymentAccount.accountName)
        }

    @Test
    fun `when create account action succeeds then adds account and emits close flow effect`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleCreateZelleAccount()
            coEvery { paymentAccountsServiceFacade.addAccount(account) } returns Result.success(Unit)
            presenter = createPresenter()

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(PaymentAccountReviewUiAction.OnCreateAccountClick(account))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.addAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            assertEquals(PaymentAccountReviewEffect.CloseCreateAccountFlow, effectDeferred.await())
        }

    @Test
    fun `when create account action conflicts then shows duplicate account snackbar and does not emit close flow effect`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleCreateZelleAccount()
            coEvery { paymentAccountsServiceFacade.addAccount(account) } returns
                Result.failure(PaymentAccountNameAlreadyExistsException("Payment account already exists: Zelle Personal"))
            presenter = createPresenter()

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(PaymentAccountReviewUiAction.OnCreateAccountClick(account))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.addAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            verify {
                globalUiManager.showSnackbar(
                    "Account name already exists. Please choose a different one.",
                    SnackbarType.ERROR,
                    any(),
                    any(),
                )
            }
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
        }

    @Test
    fun `when create account action fails then shows error snackbar and does not emit close flow effect`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleCreateZelleAccount()
            coEvery { paymentAccountsServiceFacade.addAccount(account) } returns Result.failure(IllegalStateException("create failed"))
            presenter = createPresenter()

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(PaymentAccountReviewUiAction.OnCreateAccountClick(account))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.addAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            verify {
                globalUiManager.showSnackbar(
                    any(),
                    SnackbarType.ERROR,
                    any(),
                    any(),
                )
            }
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
        }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.ZELLE,
            name = "Zelle",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "1 day",
        )

    private fun sampleCreateZelleAccount(accountName: String = "Zelle Personal"): CreateZelleAccount =
        CreateZelleAccount(
            accountName = accountName,
            accountPayload =
                CreateZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                ),
        )
}
