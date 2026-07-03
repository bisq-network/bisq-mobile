package network.bisq.mobile.presentation.tabs.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.combine
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase

class OfferbookMarketPresenter(
    private val mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val computeOfferbookMarketListUseCase: ComputeOfferbookMarketListUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.OfferbookMarket

    // searchText is ephemeral input with no persisted source of truth, so it stays a sibling StateFlow
    // instead of living in uiState. It still feeds the combine below (each keystroke rebuilds uiState and
    // re-filters the list), but keeping it separate lets the controlled search field bind to a synchronous
    // value that isn't routed through flowOn(default) — avoiding cursor/character jank on the BasicTextField.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // stateIn captures presenterScope at construction, and jobsManager.dispose() (called on view unattach)
    // cancels+recreates that scope. So the #1197 hazard is a presenter instance that is REUSED after a
    // dispose: its uiState stays bound to the now-dead scope. That was originally triggered by plain
    // RememberPresenterLifecycle, which disposes the scope on every unattach while the same instance lived on.
    // It is safe here because the binding is `factory` (each resolution is a fresh instance+scope, so a stale
    // instance is never reused) and the screen uses RememberPresenterLifecycleBackStackAware (scope survives
    // hide and is disposed only on pop, when the instance is discarded too).
    val uiState: StateFlow<OfferbookMarketUiState> =
        combine(
            settingsRepository.data,
            userProfileServiceFacade.ignoredProfileIds,
            _searchText,
            // globalPriceUpdate is a trigger only: it re-runs the compute when prices refresh.
            marketPriceServiceFacade.globalPriceUpdate,
            mainPresenter.languageCode,
            offersServiceFacade.offerbookMarketItems,
        ) { settings, ignoredProfileIds, searchText, _, languageCode, items ->
            OfferbookMarketUiState(
                hasIgnoredUsers = ignoredProfileIds.isNotEmpty(),
                filter = settings.marketFilter,
                sortBy = settings.marketSortBy,
                marketItems =
                    computeOfferbookMarketListUseCase(
                        settings.marketFilter,
                        searchText,
                        settings.marketSortBy,
                        languageCode,
                        items,
                    ),
            )
        }.flowOn(dispatcherProvider.default)
            .stateIn(presenterScope, SharingStarted.Eagerly, OfferbookMarketUiState())

    fun onAction(action: OfferbookMarketUiAction) {
        when (action) {
            is OfferbookMarketUiAction.OnSearchTextChanged -> _searchText.value = action.searchText
            is OfferbookMarketUiAction.OnFilterChanged ->
                presenterScope.launch { settingsRepository.setMarketFilter(action.filter) }

            is OfferbookMarketUiAction.OnSortByChanged ->
                presenterScope.launch { settingsRepository.setMarketSortBy(action.sortBy) }

            is OfferbookMarketUiAction.OnMarketSelected -> onSelectMarket(action.marketListItem)
        }
    }

    private fun onSelectMarket(marketListItem: MarketListItem) {
        offersServiceFacade
            .selectOfferbookMarket(marketListItem)
            .onSuccess {
                navigateTo(NavRoute.Offerbook)
            }.onFailure { e ->
                log.e("Market selection failed", e)
                showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR)
            }
    }
}
