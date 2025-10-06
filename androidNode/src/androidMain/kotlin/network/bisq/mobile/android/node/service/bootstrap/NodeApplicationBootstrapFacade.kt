package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.KmpTorService.State.IDLE
import network.bisq.mobile.domain.service.network.KmpTorService.State.STARTED
import network.bisq.mobile.domain.service.network.KmpTorService.State.STARTING
import network.bisq.mobile.domain.service.network.KmpTorService.State.STARTING_FAILED
import network.bisq.mobile.domain.service.network.KmpTorService.State.STOPPED
import network.bisq.mobile.domain.service.network.KmpTorService.State.STOPPING
import network.bisq.mobile.domain.service.network.KmpTorService.State.STOPPING_FAILED
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val provider: AndroidApplicationService.Provider,
    private val kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade() {

    companion object {
        private const val BOOTSTRAP_STAGE_TIMEOUT_MS = 90_000L // 90 seconds per stage
    }

    private val applicationServiceState: Observable<State> by lazy { provider.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var currentTimeoutJob: Job? = null

    override fun activate() {
        super.activate()
        log.i { "Bootstrap: super.activate() completed, calling onInitializeAppState()" }

        observeTorState()
        observeApplicationState()

        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    override fun deactivate() {
        log.i { "Bootstrap: deactivate() called" }
        cancelTimeout()
        removeApplicationStateObserver()

        super.deactivate()
        log.i { "Bootstrap: deactivate() completed" }
    }

    private fun observeTorState() {
        serviceScope.launch {
            kmpTorService.state.collect { newState ->
                when (newState) {
                    IDLE -> {}
                    STARTING -> {
                        setState("mobile.bootstrap.tor.starting".i18n())
                        setProgress(0.1f)
                        startTimeoutForStage()
                    }

                    STARTED -> {
                        setState("mobile.bootstrap.tor.started".i18n())
                        setProgress(0.25f)
                    }

                    STOPPING -> {}
                    STOPPED -> {}
                    STARTING_FAILED -> {
                        val failure = kmpTorService.startupFailure.value
                        val errorMessage = listOfNotNull(
                            failure?.message,
                            failure?.cause?.message
                        ).firstOrNull() ?: "Unknown Tor error"
                        setState("mobile.bootstrap.tor.failed".i18n() + ": $errorMessage")
                        cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                        setBootstrapFailed(true)
                        log.e { "Bootstrap: Tor initialization failed - $errorMessage" }
                    }

                    STOPPING_FAILED -> {}
                }
            }
        }
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

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        bootstrapSuccessful = true
        cancelTimeout()
        log.i { "Bootstrap completed successfully - Tor monitoring will continue" }
    }


    private fun removeApplicationStateObserver() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
    }

    private fun startTimeoutForStage(stageName: String = state.value, extendedTimeout: Boolean = false) {
        currentTimeoutJob?.cancel()
        setTimeoutDialogVisible(false)
        setCurrentBootstrapStage(stageName)

        if (bootstrapSuccessful) {
            return
        }

        val timeoutDuration = if (extendedTimeout) {
            BOOTSTRAP_STAGE_TIMEOUT_MS * 2 // 2x longer for extended wait
        } else {
            BOOTSTRAP_STAGE_TIMEOUT_MS //  Normal timeout
        }

        log.i { "Bootstrap: Starting timeout for stage: $stageName (${timeoutDuration / 1000}s)" }

        currentTimeoutJob = serviceScope.launch {
            if (bootstrapSuccessful) {
                return@launch
            }
            try {
                delay(timeoutDuration)
                if (!bootstrapSuccessful) {
                    log.w { "Bootstrap: Timeout reached for stage: $stageName" }
                    setTimeoutDialogVisible(true)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "Bootstrap: Timeout job cancelled for stage: $stageName" }
                } else {
                    log.e(e) { "Bootstrap: Error in Timeout job, cancelled for stage: $stageName" }
                }
            }
        }
    }

    private fun cancelTimeout(showProgressToast: Boolean = true) {
        currentTimeoutJob?.cancel()
        currentTimeoutJob = null

        // If dialog was visible and we're cancelling due to progress, show toast
        setTimeoutDialogVisible(isTimeoutDialogVisible.value && showProgressToast && !isBootstrapFailed.value)
    }

    override fun extendTimeout() {
        log.i { "Bootstrap: Extending timeout for current stage" }
        val currentStage = currentBootstrapStage.value
        if (currentStage.isNotEmpty()) {
            // Restart timeout with double the duration for extended wait
            startTimeoutForStage(currentStage, extendedTimeout = true)
        }
        setTimeoutDialogVisible(false)
    }
}