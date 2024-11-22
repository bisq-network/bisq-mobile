package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    private val supplier: AndroidApplicationService.Supplier,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade
) : MainPresenter(applicationBootstrapFacade) {

    var applicationServiceInited = false
    override fun onViewAttached() {
//        full override
//        super.onViewAttached()

        if (!applicationServiceInited) {
            applicationServiceInited = true
            val context = (view as Activity).applicationContext
            val filesDirsPath = (view as Activity).filesDir.toPath()
            supplier.applicationService =
                AndroidApplicationService(androidMemoryReportService, filesDirsPath)
            applicationBootstrapFacade.initialize()
            supplier.applicationService.initialize()
        }
    }

    override fun onDestroying() {
        supplier.applicationService.shutdown()
        super.onDestroying()
    }
}