package network.bisq.mobile.client

import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.service.common.ClientLanguageServiceFacade
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    connectivityService: ClientConnectivityService,
    openTradesNotificationService: OpenTradesNotificationService,
    private val tradesServiceFacade: TradesServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    urlLauncher: UrlLauncher
) : MainPresenter(connectivityService, openTradesNotificationService, settingsServiceFacade, urlLauncher) {

    override fun onViewAttached() {
        super.onViewAttached()
        activateServices()
        listenForConnectivity()
    }

    override fun onViewUnattaching() {
        // For Tor we might want to leave it running while in background to avoid delay of re-connect
        // when going into foreground again.
        // coroutineScope.launch {  webSocketClient.disconnect() }
        deactivateServices()
        super.onViewUnattaching()
    }

    private fun listenForConnectivity() {
        backgroundScope.launch {
            webSocketClientProvider.get().connected.collect {
                if (webSocketClientProvider.get().isConnected()) {
                    log.d { "connectivity status changed to $it - reconnecting services" }
                    reactiveServices()
                }
            }
        }
    }

    private fun reactiveServices() {
        deactivateServices()
        activateServices()
    }

    private fun activateServices() {
        runCatching {
            applicationBootstrapFacade.activate()
            offersServiceFacade.activate()
            marketPriceServiceFacade.activate()
            tradesServiceFacade.activate()
            settingsServiceFacade.activate()
            languageServiceFacade.activate()
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    private fun deactivateServices() {
        applicationBootstrapFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        settingsServiceFacade.deactivate()
        languageServiceFacade.deactivate()
        super.onViewUnattaching()
    }
}