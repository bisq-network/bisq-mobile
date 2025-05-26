package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val connectivityService: ConnectivityService
) : ApplicationBootstrapFacade() {

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private var connectivityJob: Job? = null

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
                    
                    // Check connectivity before completing bootstrap
                    if (connectivityService.isConnected()) {
                        onInitialized()
                    } else {
                        setState("bootstrap.noConnectivity".i18n())
                        setProgress(0.95f) // Not fully complete
                        connectivityJob = connectivityService.runWhenConnected {
                            log.d { "Bootstrap: Connectivity restored, completing initialization" }
                            onInitialized()
                        }
                    }
                }

                State.FAILED -> {
                    setState("splash.applicationServiceState.FAILED".i18n())
                    setProgress(0f)
                }
            }
        }
    }

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    override fun deactivate() {
        connectivityJob?.cancel()
        connectivityJob = null
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
        isActive = false
        super.deactivate()
    }
}