package network.bisq.mobile.client.presentation

import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.MainPresenter

@Suppress("UNCHECKED_CAST")
class AndroidClientMainPresenter(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade
) : MainPresenter(GreetingRepository()) {
    var applicationServiceInited = false
    override fun onViewAttached() {
        super.onViewAttached()

        if (!applicationServiceInited) {
            applicationServiceInited = true
            applicationBootstrapFacade.initialize()
        }
    }
}