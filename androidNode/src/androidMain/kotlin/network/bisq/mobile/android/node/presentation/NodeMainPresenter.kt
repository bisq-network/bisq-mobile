package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    notificationServiceController: NotificationServiceController,
    urlLauncher: UrlLauncher,
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade
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
                settingsServiceFacade.activate()
                log.i { "Start initializing applicationService" }
                applicationService.initialize()
                    .whenComplete { r: Boolean?, throwable: Throwable? ->
                        if (throwable == null) {
                            log.i { "ApplicationService initialized" }
                            applicationBootstrapFacade.deactivate()

                            offersServiceFacade.activate()
                            marketPriceServiceFacade.activate()
                            tradesServiceFacade.activate()
                        } else {
                            log.e("Initializing applicationService failed", throwable)
                        }
                    }
            } else {
                settingsServiceFacade.activate()
                offersServiceFacade.activate()
                marketPriceServiceFacade.activate()
                tradesServiceFacade.activate()

            }
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    override fun onViewUnattaching() {
        applicationBootstrapFacade.deactivate()
        settingsServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        super.onViewUnattaching()
    }

    override fun onDestroying() {
        provider.applicationService.onStop()
        super.onDestroying()
    }
}