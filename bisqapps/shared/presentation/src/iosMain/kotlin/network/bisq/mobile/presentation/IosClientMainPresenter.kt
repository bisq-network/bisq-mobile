package network.bisq.mobile.presentation

import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade

@Suppress("UNCHECKED_CAST")
class IosClientMainPresenter(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade
) : MainPresenter() {
    var applicationServiceInited = false
    override fun onViewAttached() {
        super.onViewAttached()

        if (!applicationServiceInited) {
            applicationServiceInited = true
            applicationBootstrapFacade.initialize()
        }
    }
}