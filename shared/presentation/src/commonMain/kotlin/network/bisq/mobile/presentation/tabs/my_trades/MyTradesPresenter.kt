package network.bisq.mobile.presentation.tabs.my_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class MyTradesPresenter(
    mainPresenter: MainPresenter,
    private val backendCapabilitiesService: BackendCapabilitiesService,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.MyTrades

    companion object {
        private const val LAST_TAB = 1
    }

    private val _uiState = MutableStateFlow(MyTradesUiState())
    val uiState: StateFlow<MyTradesUiState> = _uiState.asStateFlow()

    /**
     * True only when the connected trusted node supports the closed-trades API
     * (node version strictly greater than the build-target version).
     * When this flips to false while the history tab is selected, the presenter
     * clamps the tab back to the open-trades tab automatically.
     */
    val showHistoryTab: StateFlow<Boolean> =
        backendCapabilitiesService.capabilities
            .map { it.isSupported(Feature.CLOSED_TRADES) }
            .stateIn(presenterScope, SharingStarted.Eagerly, false)

    override fun onViewAttached() {
        super.onViewAttached()
        // Clamp selectedTab back to open-trades if history becomes unavailable.
        showHistoryTab
            .onEach { historyAvailable ->
                if (!historyAvailable && _uiState.value.selectedTab > 0) {
                    _uiState.update { it.copy(selectedTab = 0) }
                }
            }.launchIn(presenterScope)
    }

    fun setInitialTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index.coerceIn(0, currentMaxTabIndex())) }
    }

    fun onAction(action: MyTradesUiAction) {
        when (action) {
            is MyTradesUiAction.OnSelectTab ->
                _uiState.update { it.copy(selectedTab = action.index.coerceIn(0, currentMaxTabIndex())) }
        }
    }

    /**
     * The history tab (index 1) is only valid when the trusted node supports the closed-trades
     * API. Reading [showHistoryTab].value at update time gives a dynamic clamp so a deep-link
     * landing on `initialTab=1` against an unsupported node is rejected up front rather than
     * snapped back to 0 by the [showHistoryTab] observer in [onViewAttached].
     */
    private fun currentMaxTabIndex(): Int = if (showHistoryTab.value) LAST_TAB else 0
}
