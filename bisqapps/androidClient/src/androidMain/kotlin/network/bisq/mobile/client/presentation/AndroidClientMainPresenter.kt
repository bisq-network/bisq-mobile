package network.bisq.mobile.client.presentation

import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.MainPresenter

class AndroidClientMainPresenter(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade
) : MainPresenter() {
    private var applicationServiceInited = false
    override fun onViewAttached() {
        super.onViewAttached()

        if (!applicationServiceInited) {
            applicationServiceInited = true
            applicationBootstrapFacade.initialize()
        }
    }
}