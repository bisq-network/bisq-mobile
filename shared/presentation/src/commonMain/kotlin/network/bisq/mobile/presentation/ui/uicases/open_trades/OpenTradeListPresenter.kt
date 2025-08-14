package network.bisq.mobile.presentation.ui.uicases.open_trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class OpenTradeListPresenter(
    private val mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    private val _sortedOpenTradeItems: MutableStateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())
    val sortedOpenTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = _sortedOpenTradeItems.asStateFlow()

    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed

    private val _tradeGuideVisible = MutableStateFlow(false)
    val tradeGuideVisible: StateFlow<Boolean> get() = _tradeGuideVisible.asStateFlow()

    private val _tradesWithUnreadMessages: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
    val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = _tradesWithUnreadMessages.asStateFlow()

    private val _avatarMap: MutableStateFlow<Map<String, PlatformImage?>> = MutableStateFlow(emptyMap())
    val avatarMap: StateFlow<Map<String, PlatformImage?>> get() = _avatarMap.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        tradesServiceFacade.resetSelectedTradeToNull()

        launchUI {
            combine(
                mainPresenter.tradesWithUnreadMessages, tradesServiceFacade.openTradeItems, mainPresenter.languageCode
            ) { unreadMessages, openTrades, _ ->
                Pair(unreadMessages, openTrades)
            }.collect { (unreadMessages, openTrades) ->
                _tradesWithUnreadMessages.value = unreadMessages
                _sortedOpenTradeItems.value = openTrades.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
            }
        }

        launchAvatarLoaderJob()
    }

    override fun onViewUnattaching() {
        _tradesWithUnreadMessages.value = emptyMap()
        _avatarMap.update { emptyMap() }
        super.onViewUnattaching()
    }

    fun isRead(trade: TradeItemPresentationModel): Boolean {
        val latestChatCount = trade.bisqEasyOpenTradeChannelModel.chatMessages.value.size
        val chatCount = mainPresenter.readMessageCountsByTrade.value[trade.tradeId]
        return chatCount != null && chatCount == latestChatCount
    }

    fun onOpenTradeGuide() {
        navigateTo(Routes.TradeGuideOverview)
    }

    fun onCloseTradeGuideConfirmation() {
        _tradeGuideVisible.value = false
    }

    fun onConfirmTradeRules(value: Boolean) {
        _tradeGuideVisible.value = false
        launchUI {
            settingsServiceFacade.confirmTradeRules(value)
        }
    }

    fun onSelect(openTradeItem: TradeItemPresentationModel) {
        if (tradeRulesConfirmed.value) {
            tradesServiceFacade.selectOpenTrade(openTradeItem.tradeId)
            navigateTo(Routes.OpenTrade)
        } else {
            log.w { "User hasn't accepted trade rules yet, showing dialog" }
            _tradeGuideVisible.value = true
        }
    }

    fun onNavigateToOfferbook() {
        navigateToTab(Routes.TabOfferbook)
    }

    fun launchAvatarLoaderJob() {
        launchIO {
            tradesServiceFacade.openTradeItems.collect { trades ->
                trades.forEach { trade ->
                    val userProfile = trade.peersUserProfile
                    if (_avatarMap.value[userProfile.nym] == null) {
                        val currentAvatarMap = _avatarMap.value.toMutableMap()
                        currentAvatarMap[userProfile.nym] = userProfileServiceFacade.getUserAvatar(
                            userProfile
                        )
                        _avatarMap.value = currentAvatarMap
                    }
                }
            }
        }
    }
}