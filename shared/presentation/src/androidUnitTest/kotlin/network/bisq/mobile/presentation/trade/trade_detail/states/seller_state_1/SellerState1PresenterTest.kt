package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SellerState1PresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userDefinedAccountsServiceFacade: UserDefinedAccountsServiceFacade = mockk(relaxed = true)

    override fun onKoinReady() {
        coEvery { userDefinedAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
    }

    @Test
    fun `rapid double-tap on onSendPaymentData triggers sellerSendsPaymentAccount only once`() =
        runTest {
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
        runTest {
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
