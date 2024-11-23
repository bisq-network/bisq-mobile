package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    private val supplier: AndroidApplicationService.Supplier,
    private val androidMemoryReportService: AndroidMemoryReportService,
    applicationBootstrapFacade: ApplicationBootstrapFacade
) : MainPresenter(applicationBootstrapFacade) {

    override fun initializeServices() {
        val context = (view as Activity).applicationContext
        val filesDirsPath = (view as Activity).filesDir.toPath()
        supplier.applicationService =
            AndroidApplicationService(androidMemoryReportService, filesDirsPath)
        supplier.applicationService.initialize()
        super.initializeServices()
    }

    override fun onDestroying() {
        supplier.applicationService.shutdown()
        super.onDestroying()
    }
}