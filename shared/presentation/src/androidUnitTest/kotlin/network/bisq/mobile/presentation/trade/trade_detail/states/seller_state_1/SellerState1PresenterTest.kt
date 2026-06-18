package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SellerState1PresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userDefinedAccountsServiceFacade: UserDefinedAccountsServiceFacade = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { globalUiManager }
                },
            )
        }
        I18nSupport.initialize("en")
        coEvery { userDefinedAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid double-tap on onSendPaymentData triggers sellerSendsPaymentAccount only once`() =
        runTest(testDispatcher) {
            val presenter =
                SellerState1Presenter(
                    mainPresenter,
                    tradesServiceFacade,
                    userDefinedAccountsServiceFacade,
                )
            presenter.onViewAttached()
            presenter.onPaymentDataInput("IBAN DE89370400440532013000")
            coEvery { tradesServiceFacade.sellerSendsPaymentAccount(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onSendPaymentData()
            presenter.onSendPaymentData()
            advanceUntilIdle()

            coVerify(exactly = 1) { tradesServiceFacade.sellerSendsPaymentAccount(any()) }
            assertFalse(presenter.isSendPaymentDataEnabled.value)
        }

    @Test
    fun `failure path re-enables send button for retry`() =
        runTest(testDispatcher) {
            val presenter =
                SellerState1Presenter(
                    mainPresenter,
                    tradesServiceFacade,
                    userDefinedAccountsServiceFacade,
                )
            presenter.onViewAttached()
            presenter.onPaymentDataInput("IBAN DE89370400440532013000")
            coEvery { tradesServiceFacade.sellerSendsPaymentAccount(any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.onSendPaymentData()
            advanceUntilIdle()

            assertTrue(presenter.isSendPaymentDataEnabled.value)
        }
}
