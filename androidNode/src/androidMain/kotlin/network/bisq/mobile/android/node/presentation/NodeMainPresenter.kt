package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offer.OfferServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.service.trade.TradeServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    notificationServiceController: NotificationServiceController,
    urlLauncher: UrlLauncher,
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offerServiceFacade: OfferServiceFacade,
    private val tradeServiceFacade: TradeServiceFacade,
) : MainPresenter(notificationServiceController, urlLauncher) {

    private var applicationServiceCreated = false
    override fun onViewAttached() {
        super.onViewAttached()

        runCatching {
            if (!applicationServiceCreated) {
                applicationServiceCreated = true
                val filesDirsPath = (view as Activity).filesDir.toPath()
                val applicationContext = (view as Activity).applicationContext
                val applicationService =
                    AndroidApplicationService(
                        androidMemoryReportService,
                        applicationContext,
                        filesDirsPath
                    )
                provider.applicationService = applicationService

                applicationBootstrapFacade.activate()
                log.i { "Start initializing applicationService" }
                applicationService.initialize()
                    .whenComplete { r: Boolean?, throwable: Throwable? ->
                        if (throwable == null) {
                            log.i { "ApplicationService initialized" }
                            applicationBootstrapFacade.deactivate()
                            offerbookServiceFacade.activate()
                            marketPriceServiceFacade.activate()
                            offerServiceFacade.activate()
                            tradeServiceFacade.activate()
                        } else {
                            log.e("Initializing applicationService failed", throwable)
                        }
                    }
            } else {
                offerbookServiceFacade.activate()
                marketPriceServiceFacade.activate()
                offerServiceFacade.activate()
                tradeServiceFacade.activate()
            }
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    override fun onViewUnattaching() {
        applicationBootstrapFacade.deactivate()
        offerbookServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        offerServiceFacade.deactivate()
        tradeServiceFacade.deactivate()
        super.onViewUnattaching()
    }

    override fun onDestroying() {
        provider.applicationService.onStop()
        super.onDestroying()
    }
}