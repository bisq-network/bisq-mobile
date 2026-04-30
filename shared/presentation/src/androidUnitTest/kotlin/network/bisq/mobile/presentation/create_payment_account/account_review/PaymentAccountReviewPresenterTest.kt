package network.bisq.mobile.presentation.create_payment_account.account_review

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
    fun `when create account action succeeds then adds account and emits close flow effect`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleZelleAccount()
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
    fun `when create account action fails then shows error snackbar and does not emit close flow effect`() =
        runTest(testDispatcher) {
            // Given
            val account = sampleZelleAccount()
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

    private fun sampleZelleAccount(accountName: String = "Zelle Personal"): ZelleAccount =
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
}
