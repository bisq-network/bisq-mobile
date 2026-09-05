package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.ScrollState
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
        // The presenter resolves the trade from the open trades once they have synced, so each test
        // says which trades are open.
        every { tradesServiceFacade.openTradesSynced } returns MutableStateFlow(true)
        every { tradesServiceFacade.openTradesSyncFailed } returns MutableStateFlow(false)
    }

    private fun givenOpenTrades(vararg trades: TradeItemPresentationModel) {
        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(trades.toList())
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

    private fun TestScope.createPresenter(
        enabledSegments: Set<CommunitySegment> = setOf(CommunitySegment.DISCUSSIONS),
    ) {
        presenter =
            OpenTradePresenter(
                mainPresenter,
                tradeReadStateRepository,
                tradesServiceFacade,
                userProfileServiceFacade,
                testCommunityHubService(
                    enabled = enabledSegments,
                    requiredFeatures = emptyMap(),
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                ),
                tradeFlowPresenter,
            )
    }

    private fun TestScope.createAndInitializePresenter(
        enabledSegments: Set<CommunitySegment> = setOf(CommunitySegment.DISCUSSIONS),
    ) {
        createPresenter(enabledSegments)
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        scrollScope = scope
        presenter.initialize("tid", ScrollState(0), scope)
    }

    @Test
    fun `a trade missing once the open trades synced raises the not found dialog`() =
        runPresenterTest {
            // Synced and empty, so the lookup can only come up empty.
            givenOpenTrades()

            createAndInitializePresenter()
            runCurrent()

            assertTrue(presenter.showTradeNotFoundDialog.value)
            assertNull(presenter.selectedTrade.value)
        }

    @Test
    fun `a trade stuck in INIT past the threshold is flagged out of sync`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            // The harness pins takeOfferDate far in the past, so an INIT trade is stuck right away.
            givenOpenTrades(harness.selectedTrade.value!!)

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
            givenOpenTrades(harness.selectedTrade.value!!)

            createAndInitializePresenter()
            runCurrent()

            assertFalse(presenter.isTradeOutOfSync.value)
        }

    @Test
    fun `support channel is available when discussions is live`() =
        runPresenterTest {
            createPresenter(enabledSegments = setOf(CommunitySegment.DISCUSSIONS))

            assertTrue(presenter.isSupportChannelAvailable.value)
        }

    @Test
    fun `support channel is withheld when discussions is not live`() =
        runPresenterTest {
            createPresenter(enabledSegments = setOf(CommunitySegment.CONTACTS))

            assertFalse(presenter.isSupportChannelAvailable.value)
        }

    @Test
    fun `opening support channel pushes it without changing back stack options`() =
        runPresenterTest {
            createPresenter()

            presenter.onOpenSupportChannel()
            runCurrent()

            val setup = slot<NavOptionsBuilder.() -> Unit>()
            verify { navigationManager.navigate(NavRoute.SupportChannel, capture(setup), any()) }

            val navOptions = navOptions(setup.captured)
            assertEquals(-1, navOptions.popUpToId)
            assertEquals(null, navOptions.popUpToRoute)
            assertFalse(navOptions.shouldLaunchSingleTop())
        }

    @Test
    fun `a stuck trade leaving INIT clears the flag`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            givenOpenTrades(harness.selectedTrade.value!!)

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
            givenOpenTrades(harness.selectedTrade.value!!)

            createAndInitializePresenter()
            runCurrent()
            assertTrue(presenter.isTradeOutOfSync.value)

            presenter.onViewUnattaching()
            runCurrent()

            assertFalse(presenter.isTradeOutOfSync.value)
        }

    /**
     * A singleTop re-navigation keeps the presenter, so the reset in initialize is all that stands
     * between the previous trade's panes and the next trade's first state.
     */
    @Test
    fun `re-initialising clears the previous trade's panes before the next trade resolves`() =
        runPresenterTest {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            harness.tradeStateFlow.value = BisqEasyTradeStateEnum.REJECTED
            givenOpenTrades(harness.selectedTrade.value!!)

            createAndInitializePresenter()
            runCurrent()
            assertTrue(presenter.tradeAbortedBoxVisible.value)

            presenter.initialize("other", ScrollState(0), scrollScope!!)

            assertFalse(presenter.tradeAbortedBoxVisible.value)
            assertFalse(presenter.tradeProcessBoxVisible.value)
        }
}
