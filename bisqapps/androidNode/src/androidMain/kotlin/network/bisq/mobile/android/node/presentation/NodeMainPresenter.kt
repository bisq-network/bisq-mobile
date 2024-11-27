package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) : MainPresenter() {

    private var applicationServiceCreated = false
    override fun onViewAttached() {
        super.onViewAttached()
        if (!applicationServiceCreated) {
            applicationServiceCreated = true
            val filesDirsPath = (view as Activity).filesDir.toPath()
            val applicationService =
                AndroidApplicationService(androidMemoryReportService, filesDirsPath)
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
                    } else {
                        log.e("Initializing applicationService failed", throwable)
                    }
                }
        } else {
            offerbookServiceFacade.activate()
            marketPriceServiceFacade.activate()
        }
    }

    override fun onViewUnattaching() {
        applicationBootstrapFacade.deactivate()
        offerbookServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        super.onViewUnattaching()
    }

    override fun onDestroying() {
        provider.applicationService.onStop()
        super.onDestroying()
    }
}