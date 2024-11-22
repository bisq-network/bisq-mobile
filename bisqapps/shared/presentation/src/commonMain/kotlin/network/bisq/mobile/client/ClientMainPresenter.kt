package network.bisq.mobile.client

import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class ClientMainPresenter(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offerbookServiceFacade: OfferbookServiceFacade
) : MainPresenter() {

    private var applicationServiceInited = false
    override fun onViewAttached() {
        super.onViewAttached()

        if (!applicationServiceInited) {
            applicationServiceInited = true
            applicationBootstrapFacade.initialize()
            offerbookServiceFacade.initialize()
        }
    }

    override fun onDestroying() {
        super.onDestroying()
    }
}