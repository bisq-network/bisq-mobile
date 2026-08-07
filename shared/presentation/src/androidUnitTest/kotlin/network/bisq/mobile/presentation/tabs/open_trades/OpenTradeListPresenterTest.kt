package network.bisq.mobile.presentation.tabs.open_trades

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.FilterOpenTradesUseCase
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.my_trades.open.OpenTradeListPresenter
import network.bisq.mobile.presentation.tabs.my_trades.open.OpenTradeListUiAction
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradeListPresenterTest : PresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val settingsServiceFacade: SettingsServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)

    private val openTradeItemsFlow = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    private val tradeRulesConfirmedFlow = MutableStateFlow(false)
    private val tradesWithUnreadFlow = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val filterOpenTradesUseCase = FilterOpenTradesUseCase()

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        GenericErrorHandler.clearGenericError()

        // Reset the shared MutableStateFlow fields to their initial values so a value set by
        // one test (e.g. tradeRulesConfirmedFlow.value = true) cannot leak into another test.
        openTradeItemsFlow.value = emptyList()
        tradeRulesConfirmedFlow.value = false
        tradesWithUnreadFlow.value = emptyMap()

        every { tradesServiceFacade.openTradeItems } returns openTradeItemsFlow
        every { settingsServiceFacade.tradeRulesConfirmed } returns tradeRulesConfirmedFlow
        every { mainPresenter.tradesWithUnreadMessages } returns tradesWithUnreadFlow
    }

    override fun onTearDown() {
        try {
            GenericErrorHandler.clearGenericError()
        } finally {
            super.onTearDown()
        }
    }

    private fun buildPresenter(): OpenTradeListPresenter {
        val p =
            OpenTradeListPresenter(
                mainPresenter = mainPresenter,
                tradesServiceFacade = tradesServiceFacade,
                settingsServiceFacade = settingsServiceFacade,
                userProfileServiceFacade = userProfileServiceFacade,
                filterOpenTradesUseCase = filterOpenTradesUseCase,
                backgroundDispatcher = testDispatcher,
            )
        p.onViewAttached()
        return p
    }

    private inline fun withPresenter(block: (OpenTradeListPresenter) -> Unit) {
        val presenter = buildPresenter()
        try {
            block(presenter)
        } finally {
            presenter.onViewUnattaching()
        }
    }

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    fun `initial state isLoading is true before filter debounce fires`() =
        runTest {
            withPresenter { presenter ->
                // The combine hasn't collected yet because the 400ms filter debounce hasn't fired.
                // flowOn(Dispatchers.Default) means the collect runs on a real thread pool, so we
                // can only observe the immediate synchronous state here.
                assertTrue(presenter.uiState.value.isLoading)
            }
        }

    @Test
    fun `initial state sortBy is NEWEST_FIRST`() =
        runTest {
            withPresenter { presenter ->
                assertEquals(TradeSort.NEWEST_FIRST, presenter.uiState.value.sortBy)
            }
        }

    @Test
    fun `initial state roleFilter is ALL`() =
        runTest {
            withPresenter { presenter ->
                assertEquals(TradeRoleFilter.ALL, presenter.uiState.value.roleFilter)
            }
        }

    @Test
    fun `initial state filteredOpenTrades is empty`() =
        runTest {
            withPresenter { presenter ->
                assertEquals(emptyList(), presenter.uiState.value.filteredOpenTrades)
            }
        }

    // -----------------------------------------------------------------------
    // Search — state is updated synchronously via _uiState.update
    // -----------------------------------------------------------------------

    @Test
    fun `OnSearchQueryChange updates searchQuery`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnSearchQueryChange("alice"))
                assertEquals("alice", presenter.uiState.value.searchQuery)
            }
        }

    @Test
    fun `OnClearSearch clears searchQuery`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnSearchQueryChange("something"))
                presenter.onAction(OpenTradeListUiAction.OnClearSearch)
                assertEquals("", presenter.uiState.value.searchQuery)
            }
        }

    // -----------------------------------------------------------------------
    // Filter actions — immediate state update
    // -----------------------------------------------------------------------

    @Test
    fun `OnSortChange updates sortBy immediately`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnSortChange(TradeSort.OLDEST_FIRST))
                assertEquals(TradeSort.OLDEST_FIRST, presenter.uiState.value.sortBy)
            }
        }

    @Test
    fun `OnRoleFilterChange updates roleFilter immediately`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnRoleFilterChange(TradeRoleFilter.BUYER))
                assertEquals(TradeRoleFilter.BUYER, presenter.uiState.value.roleFilter)
            }
        }

    // -----------------------------------------------------------------------
    // Filter debounce (400ms) — state updates are synchronous; the debounce
    // controls when the combine/collect re-fires (which runs on Dispatchers.Default
    // in production and is not observable in unit tests without a real dispatcher).
    // We verify: (a) state updates are immediate, (b) the second change wins.
    // -----------------------------------------------------------------------

    @Test
    fun `filter debounce - sort state updates immediately on each action`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnSortChange(TradeSort.OLDEST_FIRST))
                assertEquals(TradeSort.OLDEST_FIRST, presenter.uiState.value.sortBy)

                advanceTimeBy(200) // within debounce window

                presenter.onAction(OpenTradeListUiAction.OnSortChange(TradeSort.NEWEST_FIRST))
                // State reflects the latest action immediately
                assertEquals(TradeSort.NEWEST_FIRST, presenter.uiState.value.sortBy)
            }
        }

    // -----------------------------------------------------------------------
    // Reset filters
    // -----------------------------------------------------------------------

    @Test
    fun `OnResetFilters resets all filter fields to defaults`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnSortChange(TradeSort.OLDEST_FIRST))
                presenter.onAction(OpenTradeListUiAction.OnRoleFilterChange(TradeRoleFilter.SELLER))
                presenter.onAction(OpenTradeListUiAction.OnResetFilters)

                assertEquals(TradeSort.NEWEST_FIRST, presenter.uiState.value.sortBy)
                assertEquals(TradeRoleFilter.ALL, presenter.uiState.value.roleFilter)
            }
        }

    @Test
    fun `OnResetFilters also closes the filter sheet`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnShowFilterSheet)
                presenter.onAction(OpenTradeListUiAction.OnResetFilters)
                assertFalse(presenter.uiState.value.showFilterSheet)
            }
        }

    // -----------------------------------------------------------------------
    // Filter sheet visibility
    // -----------------------------------------------------------------------

    @Test
    fun `OnShowFilterSheet sets showFilterSheet true`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnShowFilterSheet)
                assertTrue(presenter.uiState.value.showFilterSheet)
            }
        }

    @Test
    fun `OnDismissFilterSheet sets showFilterSheet false`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnShowFilterSheet)
                presenter.onAction(OpenTradeListUiAction.OnDismissFilterSheet)
                assertFalse(presenter.uiState.value.showFilterSheet)
            }
        }

    @Test
    fun `per-filter actions do not auto-dismiss the filter sheet`() =
        runTest {
            withPresenter { presenter ->
                presenter.onAction(OpenTradeListUiAction.OnShowFilterSheet)
                presenter.onAction(OpenTradeListUiAction.OnSortChange(TradeSort.OLDEST_FIRST))
                presenter.onAction(OpenTradeListUiAction.OnRoleFilterChange(TradeRoleFilter.SELLER))
                assertTrue(presenter.uiState.value.showFilterSheet)
            }
        }

    // -----------------------------------------------------------------------
    // OnSelectTrade — trade rules not yet confirmed
    // -----------------------------------------------------------------------

    @Test
    fun `OnSelectTrade when tradeRulesConfirmed false sets tradeGuideVisible true`() =
        runTest {
            tradeRulesConfirmedFlow.value = false
            withPresenter { presenter ->
                val trade = mockTradeItem("t-abc", isSeller = false, takeOfferDate = 1L)

                presenter.onAction(OpenTradeListUiAction.OnSelectTrade(trade))

                assertTrue(presenter.uiState.value.tradeGuideVisible)
            }
        }

    @Test
    fun `OnSelectTrade when tradeRulesConfirmed false does NOT navigate`() =
        runTest {
            tradeRulesConfirmedFlow.value = false
            withPresenter { presenter ->
                val trade = mockTradeItem("t-abc", isSeller = false, takeOfferDate = 1L)

                presenter.onAction(OpenTradeListUiAction.OnSelectTrade(trade))

                verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
            }
        }

    // -----------------------------------------------------------------------
    // OnSelectTrade — trade rules confirmed
    // -----------------------------------------------------------------------

    @Test
    fun `OnSelectTrade when tradeRulesConfirmed true navigates to OpenTrade and does not show trade guide`() =
        runTest {
            tradeRulesConfirmedFlow.value = true
            withPresenter { presenter ->
                val trade = mockTradeItem("t-abc", isSeller = false, takeOfferDate = 1L)

                presenter.onAction(OpenTradeListUiAction.OnSelectTrade(trade))

                verify { navigationManager.navigate(NavRoute.OpenTrade("t-abc"), any(), any()) }
                assertFalse(presenter.uiState.value.tradeGuideVisible)
            }
        }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a minimal [TradeItemPresentationModel] mock for unit testing. Only the properties
     * accessed by [FilterOpenTradesUseCase] and the presenter are stubbed; everything else
     * is handled by MockK's relaxed mode.
     */
    private fun mockTradeItem(
        tradeId: String,
        isSeller: Boolean,
        takeOfferDate: Long,
        peersUserName: String = "Peer-$tradeId",
    ): TradeItemPresentationModel {
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns isSeller
        every { tradeModel.takeOfferDate } returns takeOfferDate
        every { tradeModel.id } returns tradeId

        val item = mockk<TradeItemPresentationModel>(relaxed = true)
        every { item.bisqEasyTradeModel } returns tradeModel
        every { item.tradeId } returns tradeId
        every { item.peersUserName } returns peersUserName
        every { item.myUserName } returns "Me"
        every { item.shortTradeId } returns tradeId.take(8)
        every { item.market } returns "BTC/USD"
        every { item.quoteAmountWithCode } returns "100 USD"
        every { item.baseAmountWithCode } returns "0.001 BTC"
        every { item.formattedPrice } returns "50000"
        every { item.bitcoinSettlementMethodDisplayString } returns "MAIN_CHAIN"
        every { item.fiatPaymentMethodDisplayString } returns "SEPA"
        every { item.quoteAmount } returns 100L
        return item
    }
}
