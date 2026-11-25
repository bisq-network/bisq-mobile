package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.TimeUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeTradesServiceFacade
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aUiState
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_b.BuyerState2bPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_a.BuyerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_4.BuyerState4Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.State4UiState
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TradeStatesProvider
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a.SellerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_4.SellerState4Presenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The trade screen handed another trade id while it stays composed, which a singleTop navigation to
 * another trade used to do: nothing from the first trade may survive into the second. These render the
 * whole screen with its real presenters, switch the id from trade A to trade B, and check what the screen
 * then shows and acts on.
 *
 * Both trades are buyer trades the header offers to cancel, so the close-type re-check cannot tell them
 * apart, and they sit on different steps (A on 2b, B on 3a), so the step the flow shows says which trade
 * it follows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradeScreenTradeSwitchUiTest : PresentationKoinComposeTestBase() {
    private val tradeA = buyerTrade(TRADE_A, BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION)
    private val tradeB = buyerTrade(TRADE_B, BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION)
    private val tradesServiceFacade = RecordingTradesServiceFacade(listOf(tradeA, tradeB))

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)

    private lateinit var openTradePresenter: OpenTradePresenter
    private lateinit var headerPresenter: TradeDetailsHeaderPresenter

    override fun onKoinReady() {
        // The open trade's out-of-sync re-check ticks forever, which would keep waitForIdle from ever
        // settling; one tick is all the screen needs.
        mockkObject(TimeUtils)
        every { TimeUtils.tickerFlow(any()) } returns flowOf(Unit)
        every { mainPresenter.isSmallScreen } returns MutableStateFlow(false)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
    }

    override fun onTearDown() {
        unmockkObject(TimeUtils)
        super.onTearDown()
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                // Factories, as in production: a screen rebuilt for another trade gets new presenters, and
                // these fields follow the latest one.
                factory {
                    OpenTradePresenter(
                        mainPresenter,
                        mockk<TradeReadStateRepository>(relaxed = true),
                        tradesServiceFacade,
                        userProfileServiceFacade,
                        testCommunityHubService(
                            enabled = setOf(CommunitySegment.DISCUSSIONS),
                            requiredFeatures = emptyMap(),
                            dispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler),
                        ),
                        TradeFlowPresenter(mainPresenter, tradesServiceFacade, tradeStatesProvider()),
                    ).also { openTradePresenter = it }
                }
                factory {
                    TradeDetailsHeaderPresenter(
                        mainPresenter,
                        tradesServiceFacade,
                        mockk<MediationServiceFacade>(relaxed = true),
                        userProfileServiceFacade,
                        SettingsRepositoryMock(),
                    ).also { headerPresenter = it }
                }
            },
        )

    @Test
    fun `a cancel dialog opened on trade A is gone once the screen switches to trade B`() {
        val cancelWarning = "bisqEasy.openTrades.cancelTrade.warning.buyer".i18n("bisqEasy.openTrades.cancelTrade.warning.part2".i18n())
        var routeTradeId by mutableStateOf(TRADE_A)
        setTestContent { OpenTradeScreen(routeTradeId) }
        assertEquals(TRADE_A, tradesServiceFacade.selectedTrade.value?.tradeId)

        // The user opens the cancel dialog on trade A.
        headerPresenter.onOpenInterruptionConfirmationDialog()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(cancelWarning).assertExists()

        // The screen switches to trade B while the dialog is open.
        routeTradeId = TRADE_B
        composeTestRule.waitForIdle()
        assertEquals(TRADE_B, tradesServiceFacade.selectedTrade.value?.tradeId)

        // The dialog was trade A's: it cannot be confirmed against trade B, and nothing was sent.
        composeTestRule.onNodeWithText(cancelWarning).assertDoesNotExist()
        assertEquals(emptyList(), tradesServiceFacade.cancelledTradeIds)
    }

    @Test
    fun `after the screen switches from trade A to trade B the trade flow shows trade B's step`() {
        var routeTradeId by mutableStateOf(TRADE_A)
        setTestContent { OpenTradeScreen(routeTradeId) }
        assertEquals(TradeFlowPresenter.TradePhaseState.BUYER_STATE2B, openTradePresenter.tradeFlowPresenter.tradePhaseState.value)

        routeTradeId = TRADE_B
        composeTestRule.waitForIdle()

        assertEquals(TRADE_B, openTradePresenter.selectedTrade.value?.tradeId)
        assertEquals(
            TradeFlowPresenter.TradePhaseState.BUYER_STATE3A,
            openTradePresenter.tradeFlowPresenter.tradePhaseState.value,
            "the trade flow still follows trade A while the screen shows trade B",
        )
    }

    /**
     * Step presenters are stubs: these tests are about which trade the screen and its flow are bound to,
     * not what a step renders. Each step stub shows the trade its pane is handed.
     */
    private fun tradeStatesProvider(): TradeStatesProvider {
        val buyerState2b = mockk<BuyerState2bPresenter>(relaxed = true)
        every { buyerState2b.selectedTrade } returns tradesServiceFacade.selectedTrade
        val buyerState3a = mockk<BuyerState3aPresenter>(relaxed = true)
        every { buyerState3a.selectedTrade } returns tradesServiceFacade.selectedTrade
        val buyerState1a = mockk<BuyerState1aPresenter>(relaxed = true)
        every { buyerState1a.uiState } returns MutableStateFlow(BuyerState1aUiState())
        val sellerState3a = mockk<SellerState3aPresenter>(relaxed = true)
        every { sellerState3a.showInvalidAddressDialog } returns MutableStateFlow(false)
        every { sellerState3a.isLightning } returns MutableStateFlow(true)
        val buyerState4 = mockk<BuyerState4Presenter>(relaxed = true)
        every { buyerState4.uiState } returns MutableStateFlow(State4UiState())
        val sellerState4 = mockk<SellerState4Presenter>(relaxed = true)
        every { sellerState4.uiState } returns MutableStateFlow(State4UiState())

        val provider = mockk<TradeStatesProvider>(relaxed = true)
        every { provider.buyerState1aPresenter } returns buyerState1a
        every { provider.sellerState3aPresenter } returns sellerState3a
        every { provider.buyerState4Presenter } returns buyerState4
        every { provider.sellerState4Presenter } returns sellerState4
        every { provider.presenterForPhase(TradeFlowPresenter.TradePhaseState.BUYER_STATE2B) } returns buyerState2b
        every { provider.presenterForPhase(TradeFlowPresenter.TradePhaseState.BUYER_STATE3A) } returns buyerState3a
        return provider
    }

    private class RecordingTradesServiceFacade(
        trades: List<TradeItemPresentationModel>,
    ) : FakeTradesServiceFacade() {
        private val _selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)
        override val selectedTrade: StateFlow<TradeItemPresentationModel?> = _selectedTrade.asStateFlow()
        override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(trades)

        val cancelledTradeIds = mutableListOf<String>()

        override fun selectOpenTrade(tradeId: String) {
            _selectedTrade.value = openTradeItems.value.find { it.tradeId == tradeId }
        }

        override suspend fun cancelTrade(
            tradeId: String,
            reason: AnalyticsEvent.Trade.InterruptReason,
        ): Result<Unit> {
            cancelledTradeIds += tradeId
            return Result.success(Unit)
        }
    }

    private companion object {
        const val TRADE_A = "trade-A"
        const val TRADE_B = "trade-B"

        fun buyerTrade(
            tradeId: String,
            state: BisqEasyTradeStateEnum,
        ): TradeItemPresentationModel {
            val harness = createTradeDetailsHeaderTestHarness(isSeller = false)
            harness.tradeStateFlow.value = state
            val trade = harness.selectedTrade.value!!
            every { trade.tradeId } returns tradeId
            every { trade.shortTradeId } returns tradeId
            return trade
        }
    }
}
