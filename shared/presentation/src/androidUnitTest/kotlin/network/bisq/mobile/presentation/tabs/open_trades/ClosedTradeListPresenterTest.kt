package network.bisq.mobile.presentation.tabs.open_trades

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.GetPaginatedClosedTradesUseCase
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.my_trades.closed.ClosedTradeListPresenter
import network.bisq.mobile.presentation.tabs.my_trades.closed.ClosedTradeListUiAction
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
class ClosedTradeListPresenterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }
    private val closedTradesTickFlow = MutableStateFlow(0)

    private val testModule =
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

    private lateinit var presenter: ClosedTradeListPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
        I18nSupport.initialize("en")
        GenericErrorHandler.clearGenericError()
        every { tradesServiceFacade.closedTradesChangeTick } returns closedTradesTickFlow
        every { mainPresenter.languageCode } returns MutableStateFlow("en")
        coEvery {
            tradesServiceFacade.getClosedTradesPaginated(any(), any(), any(), any(), any())
        } returns
            Result.success(
                PaginatedResponse(
                    items = emptyList(),
                    page = 1,
                    pageSize = 20,
                    totalItems = 0,
                    totalPages = 0,
                ),
            )
        val useCase = GetPaginatedClosedTradesUseCase(tradesServiceFacade)
        presenter = ClosedTradeListPresenter(mainPresenter, tradesServiceFacade, useCase, userProfileServiceFacade)
        presenter.onViewAttached()
    }

    @AfterTest
    fun tearDown() {
        presenter.onViewUnattaching()
        stopKoin()
        Dispatchers.resetMain()
        GenericErrorHandler.clearGenericError()
    }

    @Test
    fun initialState_searchQueryIsEmpty() {
        assertEquals("", presenter.uiState.value.searchQuery)
    }

    @Test
    fun initialState_sortIsNewestFirst() {
        assertEquals(TradeSort.NEWEST_FIRST, presenter.uiState.value.sortBy)
    }

    @Test
    fun initialState_filterSheetIsHidden() {
        assertFalse(presenter.uiState.value.showFilterSheet)
    }

    @Test
    fun onSearchQueryChange_updatesSearchQuery() {
        presenter.onAction(ClosedTradeListUiAction.OnSearchQueryChange("alice"))
        assertEquals("alice", presenter.uiState.value.searchQuery)
    }

    @Test
    fun onApplyFilters_updatesSortAndFilters() {
        presenter.onAction(
            ClosedTradeListUiAction.OnApplyFilters(
                sort = TradeSort.OLDEST_FIRST,
                outcome = TradeOutcomeFilter.COMPLETED,
                role = TradeRoleFilter.BUYER,
            ),
        )
        assertEquals(TradeSort.OLDEST_FIRST, presenter.uiState.value.sortBy)
        assertEquals(
            TradeOutcomeFilter.COMPLETED,
            presenter.uiState.value.outcomeFilter,
        )
        assertEquals(
            TradeRoleFilter.BUYER,
            presenter.uiState.value.roleFilter,
        )
        assertFalse(presenter.uiState.value.showFilterSheet)
    }

    @Test
    fun onShowFilterSheet_setsShowFilterSheetTrue() {
        presenter.onAction(ClosedTradeListUiAction.OnShowFilterSheet)
        assertTrue(presenter.uiState.value.showFilterSheet)
    }

    @Test
    fun onDismissFilterSheet_setsShowFilterSheetFalse() =
        runTest {
            presenter.onAction(ClosedTradeListUiAction.OnShowFilterSheet)
            presenter.onAction(ClosedTradeListUiAction.OnDismissFilterSheet)
            assertFalse(presenter.uiState.value.showFilterSheet)
        }

    @Test
    fun onResetFilters_resetsSortAndFilters() {
        presenter.onAction(
            ClosedTradeListUiAction.OnApplyFilters(
                sort = TradeSort.OLDEST_FIRST,
                outcome = TradeOutcomeFilter.COMPLETED,
                role = TradeRoleFilter.BUYER,
            ),
        )
        presenter.onAction(ClosedTradeListUiAction.OnResetFilters)
        assertEquals(TradeSort.NEWEST_FIRST, presenter.uiState.value.sortBy)
        assertEquals(
            TradeOutcomeFilter.ALL,
            presenter.uiState.value.outcomeFilter,
        )
        assertEquals(
            TradeRoleFilter.ALL,
            presenter.uiState.value.roleFilter,
        )
    }

    @Test
    fun onClearSearch_resetsSearch() {
        presenter.onAction(ClosedTradeListUiAction.OnSearchQueryChange("x"))
        presenter.onAction(ClosedTradeListUiAction.OnClearSearch)
        assertEquals("", presenter.uiState.value.searchQuery)
    }
}
