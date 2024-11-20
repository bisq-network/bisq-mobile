package network.bisq.mobile.android.node.domain.bootstrap

import bisq.application.State
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade

class NodeApplicationBootstrapFacade(
    private val supplier: AndroidApplicationService.Supplier
) :
    ApplicationBootstrapFacade() {

    override fun initialize() {
        supplier.stateSupplier.get().addObserver { state: State ->
            when (state) {
                State.INITIALIZE_APP -> {
                   setState("Starting Bisq")
                   setProgress(0f)
                }

                State.INITIALIZE_NETWORK -> {
                   setState("Initialize P2P network")
                   setProgress(0.5f)
                }

                // not used
                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                   setState("Initialize services")
                   setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                   setState("Bisq started")
                   setProgress(1f)
                }

                State.FAILED -> {
                   setState("Startup failed")
                   setProgress(0f)
                }
            }
        }
    }
}