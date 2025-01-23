package network.bisq.mobile.client

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

/**
 * Redefinition to be able to access activity for trading notifications click handling
 */
class AndroidClientMainPresenter(openTradesNotificationService: OpenTradesNotificationService,
                                 tradesServiceFacade: TradesServiceFacade,
                                 webSocketClientProvider: WebSocketClientProvider,
                                 applicationBootstrapFacade: ApplicationBootstrapFacade,
                                 offersServiceFacade: OffersServiceFacade,
                                 marketPriceServiceFacade: MarketPriceServiceFacade,
                                 settingsServiceFacade: SettingsServiceFacade, urlLauncher: UrlLauncher
) : ClientMainPresenter(
    openTradesNotificationService, tradesServiceFacade, webSocketClientProvider, applicationBootstrapFacade,
    offersServiceFacade, marketPriceServiceFacade, settingsServiceFacade, urlLauncher
) {
    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }
}