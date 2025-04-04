package network.bisq.mobile.client.web

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

class WebClientMainPresenter(
    connectivityService: ClientConnectivityService,
    openTradesNotificationService: OpenTradesNotificationService,
    tradesServiceFacade: TradesServiceFacade,
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
    webSocketClientProvider, 
    applicationBootstrapFacade,
    offersServiceFacade, 
    marketPriceServiceFacade, 
    settingsServiceFacade, 
    languageServiceFacade, 
    urlLauncher
) {
    override fun onViewAttached() {
        super.onViewAttached()

        // TODO PWA activation - probably won't work on tor browsers
        registerServiceWorker()
    }
    
    private fun registerServiceWorker() {
        CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                if ("serviceWorker" in js("navigator")) {
                    js("navigator.serviceWorker.register('/service-worker.js')")
                        .then { registration ->
                            console.log("ServiceWorker registration successful with scope: ${registration.scope}")
                        }
                        .catch { error ->
                            console.log("ServiceWorker registration failed: $error")
                        }
                }
            } catch (e: Exception) {
                console.error("Error registering service worker", e)
            }
        }
    }
}