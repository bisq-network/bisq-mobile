package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.MainActivity
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

/**
 * Node main presenter has a very different setup than the rest of the apps (bisq2 core dependencies)
 */
class NodeMainPresenter(
    urlLauncher: UrlLauncher,
    openTradesNotificationService: OpenTradesNotificationService,
    private val tradesServiceFacade: TradesServiceFacade,
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
) : MainPresenter(openTradesNotificationService, settingsServiceFacade, urlLauncher) {

    private var applicationServiceCreated = false

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }
    override fun onViewAttached() {
        super.onViewAttached()

        runCatching {
            if (!applicationServiceCreated) {
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
                            languageServiceFacade.activate()
                        } else {
                            log.e("Initializing applicationService failed", throwable)
                        }
                    }
                applicationServiceCreated = true
            } else {
                settingsServiceFacade.activate()
                offersServiceFacade.activate()
                marketPriceServiceFacade.activate()
                tradesServiceFacade.activate()

                languageServiceFacade.activate()
            }
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    override fun onViewUnattaching() {
        deactivateServices()
        super.onViewUnattaching()
    }

    override fun onDestroying() {
//        TODO for notifications to work even if the app gets killed this needs to be commented out
//        but it can't be done yet because of lack of support in bisq2 jars
        provider.applicationService.onStop()
        applicationServiceCreated = false
        super.onDestroying()
    }

    private fun deactivateServices() {
        applicationBootstrapFacade.deactivate()
        settingsServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
//        TODO for notifications to work even if the app gets killed this needs to be commented out
//        but it can't be done yet because of lack of support in bisq2 jars
        tradesServiceFacade.deactivate()
        languageServiceFacade.deactivate()
    }
}