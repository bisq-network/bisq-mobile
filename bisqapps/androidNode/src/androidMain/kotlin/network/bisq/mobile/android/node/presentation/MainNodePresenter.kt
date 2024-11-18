package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.data.repository.NodeGreetingRepository
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.MainPresenter

@Suppress("UNCHECKED_CAST")
class MainNodePresenter(
    greetingRepository: NodeGreetingRepository,
    private val supplier: AndroidApplicationService.Supplier
) : MainPresenter(greetingRepository as GreetingRepository<Greeting>) {

    override fun onViewAttached() {
        super.onViewAttached()

        val context = (view as Activity).applicationContext
        val filesDirsPath = (view as Activity).filesDir.toPath()
        supplier.applicationService =
            AndroidApplicationService(context, filesDirsPath)
        supplier.applicationService.initialize().join()
    }

    override fun onDestroying() {
        supplier.applicationService.shutdown()
        super.onDestroying()
    }


}