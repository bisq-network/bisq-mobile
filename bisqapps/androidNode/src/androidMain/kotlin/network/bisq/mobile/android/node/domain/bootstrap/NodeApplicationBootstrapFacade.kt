package network.bisq.mobile.android.node.domain.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider
) :
    ApplicationBootstrapFacade() {

    // Dependencies
    private val applicationServiceState: Observable<State> by lazy {
        applicationService.state.get()
    }

    // Misc
    private var applicationServiceStatePin: Pin? = null

    override fun activate() {
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
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

    override fun deactivate() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
    }
}