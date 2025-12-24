package network.bisq.mobile.presentation.tabs.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

class OpenTradeListPresenter(
    private val mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _sortedOpenTradeItems: MutableStateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())
    val sortedOpenTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = _sortedOpenTradeItems.asStateFlow()

    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed

    private val _tradeGuideVisible = MutableStateFlow(false)
    val tradeGuideVisible: StateFlow<Boolean> get() = _tradeGuideVisible.asStateFlow()
    val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    private val _isLoadingTrades = MutableStateFlow(true)
    val isLoadingTrades = _isLoadingTrades.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        tradesServiceFacade.resetSelectedTradeToNull()
        _isLoadingTrades.value = true
        presenterScope.launch {
            combine(
                mainPresenter.tradesWithUnreadMessages,
                tradesServiceFacade.openTradeItems,
                mainPresenter.languageCode,
            ) { _, openTrades, _ ->
                openTrades
            }.collect { openTrades ->
                _sortedOpenTradeItems.value =
                    openTrades.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
                _isLoadingTrades.value = false
            }
        }
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
    }

    fun onOpenTradeGuide() {
        navigateTo(NavRoute.TradeGuideOverview)
    }

    fun onCloseTradeGuideConfirmation() {
        _tradeGuideVisible.value = false
    }

    fun onConfirmTradeRules(value: Boolean) {
        _tradeGuideVisible.value = false
        presenterScope.launch {
            settingsServiceFacade.confirmTradeRules(value)
        }
    }

    fun onSelect(openTradeItem: TradeItemPresentationModel) {
        if (tradeRulesConfirmed.value) {
            navigateToOpenTrade(openTradeItem)
        } else {
            log.w { "User hasn't accepted trade rules yet, showing dialog" }
            _tradeGuideVisible.value = true
        }
    }

    fun onNavigateToOfferbook() {
        navigateToTab(NavRoute.TabOfferbookMarket)
    }

    private fun navigateToOpenTrade(openTradeItem: TradeItemPresentationModel) {
        try {
            navigateTo(NavRoute.OpenTrade(openTradeItem.tradeId))
        } catch (e: Exception) {
            log.e(e) { "Failed to open trade ${openTradeItem.tradeId}" }
            showSnackbar("mobile.bisqEasy.openTrades.failed".i18n(e.message ?: "unknown"))
        }
    }
}
