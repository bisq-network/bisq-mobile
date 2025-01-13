package network.bisq.mobile.client

import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class ClientMainPresenter(
    notificationServiceController: NotificationServiceController,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    urlLauncher: UrlLauncher
) : MainPresenter(notificationServiceController, urlLauncher) {

    override fun onViewAttached() {
        super.onViewAttached()

        runCatching {
            applicationBootstrapFacade.activate()
            offersServiceFacade.activate()
            marketPriceServiceFacade.activate()
            tradesServiceFacade.activate()
            settingsServiceFacade.activate()
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    override fun onViewUnattaching() {
        // For Tor we might want to leave it running while in background to avoid delay of re-connect
        // when going into foreground again.
        // coroutineScope.launch {  webSocketClient.disconnect() }

        applicationBootstrapFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        settingsServiceFacade.deactivate()
        super.onViewUnattaching()
    }
}