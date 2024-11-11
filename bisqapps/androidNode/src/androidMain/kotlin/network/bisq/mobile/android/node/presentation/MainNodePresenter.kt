package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.GreetingRepository
import network.bisq.mobile.presentation.MainPresenter

class MainNodePresenter(greetingRepository: GreetingRepository): MainPresenter(greetingRepository) {
    override fun onViewAttached() {
        super.onViewAttached()
        // TODO add node init logic
    }
}