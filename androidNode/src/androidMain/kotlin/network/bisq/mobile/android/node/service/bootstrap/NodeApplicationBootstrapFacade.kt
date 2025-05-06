package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider
) : ApplicationBootstrapFacade() {

    // Dependencies
    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }

    // Misc
    private var applicationServiceStatePin: Pin? = null

    override fun activate() {
        // Check if already active to prevent duplicate activation
        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            // Force reset of bootstrap state to ensure it runs again
            deactivate()
        }
        
        super.activate()
        
        // Reset progress and state
        onInitializeAppState()
        
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            when (state) {
                State.INITIALIZE_APP -> {
                    onInitializeAppState()
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                }

                // not used
                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                    isActive = true
                    log.i { "Bootstrap activated" }
                    setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
                    setProgress(1f)
                }

                State.FAILED -> {
                    setState("splash.applicationServiceState.FAILED".i18n())
                    setProgress(0f)
                }
            }
        }
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    override fun deactivate() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
        isActive = false
        super.deactivate()
    }
}