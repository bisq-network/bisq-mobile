package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.ScrollState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Uses [runCurrent] instead of [kotlinx.coroutines.test.advanceUntilIdle], and always detaches the
 * presenter in a finally block: the presenter starts
 * [network.bisq.mobile.domain.utils.TimeUtils.tickerFlow] for the out-of-sync re-check, and an
 * un-cancelled ticker keeps the shared virtual-time scheduler busy forever, hanging runTest's
 * cleanup (same pattern and reasoning as `UserProfilePresenterTest.runPresenterTest`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradePresenterTest : PresentationKoinTestBase() {
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val tradeFlowPresenter: TradeFlowPresenter = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private lateinit var presenter: OpenTradePresenter
    private var scrollScope: CoroutineScope? = null

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
    }

    private fun runPresenterTest(block: suspend TestScope.() -> Unit) =
        runTest {
            try {
                block()
            } finally {
                // Cancel the delayed scroll-animation task before it can be advanced into —
                // animateScrollTo would need a MonotonicFrameClock plain presenter tests lack.
                scrollScope?.cancel()
                if (::presenter.isInitialized) {
                    presenter.onViewUnattaching()
                    // Disposal is launched on Main — run it so the ticker actually cancels.
                    runCurrent()
                }
            }
        }

    private fun createAndInitializePresenter() {
        presenter =
            OpenTradePresenter(
                mainPresenter,
                tradeReadStateRepository,
                tradesServiceFacade,
                userProfileServiceFacade,
                tradeFlowPresenter,
            )
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        scrollScope = scope
        presenter.initialize("tid", ScrollState(0), scope)
    }

    @Test
    fun `a trade stuck in INIT past the threshold is flagged out of sync`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            // The harness pins takeOfferDate far in the past, so an INIT trade is stuck right away.
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            createAndInitializePresenter()
            runCurrent()

            assertTrue(presenter.isTradeOutOfSync.value)
        }

    @Test
    fun `a trade in INIT within the threshold is not flagged`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            val tradeModel = harness.selectedTrade.value!!.bisqEasyTradeModel
            every { tradeModel.takeOfferDate } returns DateUtils.now()
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            createAndInitializePresenter()
            runCurrent()

            assertFalse(presenter.isTradeOutOfSync.value)
        }

    @Test
    fun `a stuck trade leaving INIT clears the flag`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            createAndInitializePresenter()
            runCurrent()
            assertTrue(presenter.isTradeOutOfSync.value)

            harness.tradeStateFlow.value = BisqEasyTradeStateEnum.REJECTED
            runCurrent()

            assertFalse(presenter.isTradeOutOfSync.value)
        }

    @Test
    fun `unattaching the view resets the flag`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            every { tradesServiceFacade.selectedTrade } returns harness.selectedTrade

            createAndInitializePresenter()
            runCurrent()
            assertTrue(presenter.isTradeOutOfSync.value)

            presenter.onViewUnattaching()
            runCurrent()

            assertFalse(presenter.isTradeOutOfSync.value)
        }
}
