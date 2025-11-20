package network.bisq.mobile.node.common.domain.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeApplicationBootstrapFacade(
    private val provider: AndroidApplicationService.Provider,
    kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade(kmpTorService) {

    private val applicationServiceState: Observable<State> by lazy { provider.state.get() }
    private var applicationServiceStatePin: Pin? = null


    override suspend fun activate() {
        super.activate()
        log.i { "Bootstrap: super.activate() completed, calling onInitializeAppState()" }

        observeTorState()
        observeApplicationState()

        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    override suspend fun deactivate() {
        log.i { "Bootstrap: deactivate() called" }
        removeApplicationStateObserver()

        super.deactivate()
        log.i { "Bootstrap: deactivate() completed" }
    }

    private fun observeApplicationState() {
        log.i { "Bootstrap: Setting up application state observer" }
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            log.i { "Bootstrap: Application state changed to: $state" }
            when (state) {
                State.INITIALIZE_APP -> {
                    startTimeoutForStage()
                    // state and progress are set at activate and when tor is started
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                    startTimeoutForStage()
                }

                State.INITIALIZE_WALLET -> {}

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                    startTimeoutForStage()
                }

                State.APP_INITIALIZED -> {
                    log.i { "Bootstrap: Application services initialized successfully" }
                    onInitialized()
                }

                State.FAILED -> {
                    setState("splash.applicationServiceState.FAILED".i18n())
                    cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                    setBootstrapFailed(true)
                    val errorMessage = provider.applicationService.startupErrorMessage.get()
                    log.e { "Bootstrap: Application service failed - $errorMessage" }
                }
            }
        }
    }

    override fun onInitialized() {
        super.onInitialized()
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        log.i { "Bootstrap completed successfully - Tor monitoring will continue" }
    }


    private fun removeApplicationStateObserver() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
    }

}