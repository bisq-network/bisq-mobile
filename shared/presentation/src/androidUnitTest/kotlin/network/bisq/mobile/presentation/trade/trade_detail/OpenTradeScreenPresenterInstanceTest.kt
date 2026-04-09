package network.bisq.mobile.presentation.trade.trade_detail

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a.BuyerState1aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_4.BuyerState4Presenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TradeStatesProvider
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_a.SellerState3aPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_4.SellerState4Presenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression test for the stale-instance bug where OpenTradeScreen injected presenter
 * sub-instances (BuyerState1aPresenter, SellerState3aPresenter, BuyerState4Presenter,
 * SellerState4Presenter) directly via koinInject(), receiving brand-new factory instances
 * that were never the same objects used by TradeFlowPresenter/TradeStatesProvider.
 *
 * Fix: OpenTradeScreen now accesses sub-presenters through
 * presenter.tradeFlowPresenter.tradeStatesProvider / presenter.tradeFlowPresenter,
 * guaranteeing identity.
 *
 * These tests verify that the presenter accessors on OpenTradePresenter and TradeFlowPresenter
 * all resolve to the single canonical instances held in TradeStatesProvider.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradeScreenPresenterInstanceTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)

    private val testKoinModule =
        module {
            single { CoroutineExceptionHandlerSetup() }
            factory<CoroutineJobsManager> {
                DefaultCoroutineJobsManager().apply {
                    get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                }
            }
            single<NavigationManager> { navigationManager }
            single<GlobalUiManager> { globalUiManager }
        }

    // Single canonical instances — these are the ones TradeStatesProvider holds.
    private val canonicalBuyerState1aPresenter = BuyerState1aPresenter(mainPresenter, tradesServiceFacade)
    private val canonicalSellerState3aPresenter = SellerState3aPresenter(mainPresenter, tradesServiceFacade)
    private val canonicalBuyerState4Presenter =
        BuyerState4Presenter(mainPresenter, tradesServiceFacade, tradeReadStateRepository)
    private val canonicalSellerState4Presenter =
        SellerState4Presenter(mainPresenter, tradesServiceFacade, tradeReadStateRepository)

    private lateinit var tradeStatesProvider: TradeStatesProvider
    private lateinit var tradeFlowPresenter: TradeFlowPresenter
    private lateinit var openTradePresenter: OpenTradePresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testKoinModule) }

        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow<TradeItemPresentationModel?>(null)

        tradeStatesProvider =
            TradeStatesProvider(
                sellerState1Presenter = mockk(relaxed = true),
                sellerState2aPresenter = mockk(relaxed = true),
                sellerState2bPresenter = mockk(relaxed = true),
                sellerState3aPresenter = canonicalSellerState3aPresenter,
                sellerStateMainChain3bPresenter = mockk(relaxed = true),
                sellerStateLightning3bPresenter = mockk(relaxed = true),
                sellerState4Presenter = canonicalSellerState4Presenter,
                buyerState1aPresenter = canonicalBuyerState1aPresenter,
                buyerState2aPresenter = mockk(relaxed = true),
                buyerState2bPresenter = mockk(relaxed = true),
                buyerState3aPresenter = mockk(relaxed = true),
                buyerStateMainChain3bPresenter = mockk(relaxed = true),
                buyerStateLightning3bPresenter = mockk(relaxed = true),
                buyerState4Presenter = canonicalBuyerState4Presenter,
            )

        tradeFlowPresenter = TradeFlowPresenter(mainPresenter, tradesServiceFacade, tradeStatesProvider)

        openTradePresenter =
            OpenTradePresenter(
                mainPresenter = mainPresenter,
                tradeReadStateRepository = tradeReadStateRepository,
                tradesServiceFacade = tradesServiceFacade,
                userProfileServiceFacade = mockk(relaxed = true),
                tradeFlowPresenter = tradeFlowPresenter,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun buyerState1aPresenter_accessedViaTradeFlowPresenter_isSameInstanceAsInTradeStatesProvider() {
        assertSame(
            expected = canonicalBuyerState1aPresenter,
            actual = openTradePresenter.tradeFlowPresenter.tradeStatesProvider.buyerState1aPresenter,
            message =
                "OpenTradeScreen must read showInvalidAddressDialog/showBarcodeView from the same " +
                    "BuyerState1aPresenter instance that TradeFlowPane uses, not a separate koinInject() instance.",
        )
    }

    @Test
    fun sellerState3aPresenter_accessedViaTradeFlowPresenter_isSameInstanceAsInTradeStatesProvider() {
        assertSame(
            expected = canonicalSellerState3aPresenter,
            actual = openTradePresenter.tradeFlowPresenter.tradeStatesProvider.sellerState3aPresenter,
            message =
                "OpenTradeScreen must read showInvalidAddressDialog/isLightning from the same " +
                    "SellerState3aPresenter instance that TradeFlowPane uses, not a separate koinInject() instance.",
        )
    }

    @Test
    fun buyerState4Presenter_accessedViaTradeFlowPresenter_isSameInstanceAsInTradeStatesProvider() {
        assertSame(
            expected = canonicalBuyerState4Presenter,
            actual = openTradePresenter.tradeFlowPresenter.buyerState4Presenter,
            message =
                "OpenTradeScreen must read showCloseTradeDialog from the same " +
                    "BuyerState4Presenter instance that TradeFlowPane uses, not a separate koinInject() instance.",
        )
    }

    @Test
    fun sellerState4Presenter_accessedViaTradeFlowPresenter_isSameInstanceAsInTradeStatesProvider() {
        assertSame(
            expected = canonicalSellerState4Presenter,
            actual = openTradePresenter.tradeFlowPresenter.sellerState4Presenter,
            message =
                "OpenTradeScreen must read showCloseTradeDialog from the same " +
                    "SellerState4Presenter instance that TradeFlowPane uses, not a separate koinInject() instance.",
        )
    }
}
