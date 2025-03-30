package network.bisq.mobile.client

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

/**
 * Redefinition to be able to access activity for trading notifications click handling
 */
class AndroidClientMainPresenter(
    connectivityService: ClientConnectivityService,
    openTradesNotificationService: OpenTradesNotificationService,
    tradesServiceFacade: TradesServiceFacade,
    tradeChatServiceFacade: TradeChatServiceFacade,
    webSocketClientProvider: WebSocketClientProvider,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    offersServiceFacade: OffersServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    urlLauncher: UrlLauncher
) : ClientMainPresenter(
    connectivityService,
    openTradesNotificationService,
    tradesServiceFacade,
    tradeChatServiceFacade,
    webSocketClientProvider,
    applicationBootstrapFacade,
    offersServiceFacade,
    marketPriceServiceFacade,
    settingsServiceFacade,
    languageServiceFacade,
    urlLauncher
) {
    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }
}