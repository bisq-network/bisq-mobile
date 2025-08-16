package network.bisq.mobile.client

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade

/**
 * Redefinition to be able to access activity for trading notifications click handling
 */
class AndroidClientMainPresenter(
    accountsServiceFacade: AccountsServiceFacade,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    explorerServiceFacade: ExplorerServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    mediationServiceFacade: MediationServiceFacade,
    connectivityService: ConnectivityService,
    offersServiceFacade: OffersServiceFacade,
    reputationServiceFacade: ReputationServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    openTradesNotificationService: OpenTradesNotificationService,
    webSocketClientProvider: WebSocketClientProvider,
    urlLauncher: UrlLauncher
) : ClientMainPresenter(
    accountsServiceFacade,
    applicationBootstrapFacade,
    tradeChatMessagesServiceFacade,
    languageServiceFacade,
    explorerServiceFacade,
    marketPriceServiceFacade,
    mediationServiceFacade,
    connectivityService,
    offersServiceFacade,
    reputationServiceFacade,
    settingsServiceFacade,
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    tradeReadStateRepository,
    webSocketClientProvider,
    urlLauncher
) {
    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }
}