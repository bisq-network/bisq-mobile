package network.bisq.mobile.node.common.domain.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeApplicationBootstrapFacade(
    private val provider: AndroidApplicationService.Provider,
    kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade(kmpTorService) {
    enum class BootstrapPhase {
        INITIALIZE_APP,
        INITIALIZE_NETWORK,
        INITIALIZE_SERVICES,
        APP_INITIALIZED,
    }

    private val applicationServiceState: Observable<State> by lazy { provider.state.get() }
    private var applicationServiceStatePin: Pin? = null

    private val _bootstrapPhase = MutableStateFlow(BootstrapPhase.INITIALIZE_APP)
    val bootstrapPhase: StateFlow<BootstrapPhase> = _bootstrapPhase.asStateFlow()

    override suspend fun activate() {
        super.activate()
        log.i { "Bootstrap: super.activate() completed, calling onInitializeAppState()" }

        // Set initial state before observing; bisq2 core's application State drives progress from here.
        _bootstrapPhase.value = BootstrapPhase.INITIALIZE_APP
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)

        // The node embeds bisq2 core's own Tor via NetworkService and never starts KmpTorService
        // (see NodeDomainModule) — so the base observeTorState() would be a dead no-op here. Tor and
        // network failures surface through observeApplicationState() -> State.FAILED instead.
        observeApplicationState()
    }

    override suspend fun deactivate() {
        log.i { "Bootstrap: deactivate() called" }
        removeApplicationStateObserver()

        super.deactivate()
        log.i { "Bootstrap: deactivate() completed" }
    }

    // internal (not private) to expose the state→phase mapping as a same-module unit-test seam,
    // mirroring the base class's protected observeTorState().
    internal fun observeApplicationState() {
        log.i { "Bootstrap: Setting up application state observer" }
        applicationServiceStatePin =
            applicationServiceState.addObserver { state: State ->
                log.i { "Bootstrap: Application state changed to: $state" }
                when (state) {
                    State.INITIALIZE_APP -> {
                        _bootstrapPhase.value = BootstrapPhase.INITIALIZE_APP
                        startTimeoutForStage()
                        // state and progress are set at activate and when tor is started
                    }

                    State.INITIALIZE_NETWORK -> {
                        _bootstrapPhase.value = BootstrapPhase.INITIALIZE_NETWORK
                        setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                        setProgress(0.5f)
                        startTimeoutForStage()
                    }

                    State.INITIALIZE_WALLET -> {}

                    State.INITIALIZE_SERVICES -> {
                        _bootstrapPhase.value = BootstrapPhase.INITIALIZE_SERVICES
                        setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                        setProgress(0.75f)
                        startTimeoutForStage()
                    }

                    State.APP_INITIALIZED -> {
                        _bootstrapPhase.value = BootstrapPhase.APP_INITIALIZED
                        log.i { "Bootstrap: Application services initialized successfully" }
                        onInitialized()
                    }

                    State.FAILED -> {
                        // Do not collapse bootstrapPhase to a FAILED value: that would discard how
                        // far bootstrap got and regress the splash steps. Failure is tracked
                        // orthogonally via setBootstrapFailed(true), which drives the failure dialog.
                        setState("splash.applicationServiceState.FAILED".i18n())
                        cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                        cancelElapsedTimer() // Terminal failure: stop the elapsed ticker's background work
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
