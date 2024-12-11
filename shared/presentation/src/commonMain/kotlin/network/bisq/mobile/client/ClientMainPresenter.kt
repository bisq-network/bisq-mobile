package network.bisq.mobile.client

import kotlinx.coroutines.launch
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class ClientMainPresenter(
    notificationServiceController: NotificationServiceController,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val webSocketClient: WebSocketClient,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    private val  marketPriceServiceFacade: MarketPriceServiceFacade
) : MainPresenter(notificationServiceController) {

    override fun onViewAttached() {
        super.onViewAttached()
        runCatching {
            backgroundScope.launch {
                runCatching {
                    webSocketClient.connect()
                }.onSuccess {
                    log.d { "Connected to trusted node" }
                }.onFailure {
                    // TODO give user feedback (we could have a general error screen covering usual
                    //  issues like connection issues and potential solutions)
                    log.e { "ERROR: FAILED to connect to trusted node - details above" }
                }
            }

            applicationBootstrapFacade.activate()
            offerbookServiceFacade.activate()
            marketPriceServiceFacade.activate()
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
        offerbookServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        super.onViewUnattaching()
    }
}