package network.bisq.mobile.android.node.main.bootstrap

import bisq.application.State
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapModel

class NodeApplicationBootstrapFacade(
    override val model: ApplicationBootstrapModel,
    applicationService: AndroidApplicationService
) :
    ApplicationBootstrapFacade {

    init {
        val model = model as ClientApplicationBootstrapModel
        applicationService.state.addObserver { state: State ->
            when (state) {
                State.INITIALIZE_APP -> {
                    model.setState("Starting Bisq")
                    model.setProgress(0f)
                }

                State.INITIALIZE_NETWORK -> {
                    model.setState("Initialize P2P network")
                    model.setProgress(0.5f)
                }

                // not used
                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    model.setState("Initialize services")
                    model.setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                    model.setState("Bisq started")
                    model.setProgress(1f)
                }

                State.FAILED -> {
                    model.setState("Startup failed")
                    model.setProgress(0f)
                }
            }
        }
    }
}