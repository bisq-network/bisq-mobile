package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import network.bisq.mobile.android.node.AndroidApplicationService

import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.i18n.i18n

interface ApplicationServiceInitializationCallback {
    fun onApplicationServiceInitialized()
    fun onApplicationServiceInitializationFailed(throwable: Throwable)
}

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val connectivityService: ConnectivityService,
) : ApplicationBootstrapFacade() {

    companion object {
        private const val DEFAULT_CONNECTIVITY_TIMEOUT_MS = 15000L
    }

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var initializationCallback: ApplicationServiceInitializationCallback? = null

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setInitializationCallback(callback: ApplicationServiceInitializationCallback) {
        this.initializationCallback = callback
    }

    override fun activate() {
        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            deactivate()
        }

        super.activate()
        log.i { "Bootstrap: Tor not supported in configuration (CLEARNET only) - skipping Tor initialization" }
        onInitializeAppState()
        setupApplicationStateObserver()
    }

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        bootstrapSuccessful = true
        log.i { "Bootstrap completed successfully - Tor monitoring will continue" }
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    private fun setupApplicationStateObserver() {
        log.i { "Bootstrap: Setting up application state observer" }
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            log.i { "Bootstrap: Application state changed to: $state" }
            when (state) {
                State.INITIALIZE_APP -> {
                    onInitializeAppState()
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                }


                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                    isActive = true
                    log.i { "Bootstrap: Application services initialized successfully" }
                    val isConnected = connectivityService.isConnected()
                    log.i { "Bootstrap: Connectivity check - Connected: $isConnected" }

                    if (isConnected) {
                        log.i { "Bootstrap: All systems ready - completing initialization" }
                        onInitialized()
                    } else {
                        log.w { "Bootstrap: No connectivity detected - waiting for connection" }
                        setState("bootstrap.noConnectivity".i18n())
                        setProgress(0.95f)

                        val connectivityJob = connectivityService.runWhenConnected {
                            log.i { "Bootstrap: Connectivity restored, completing initialization" }
                            onInitialized()
                        }


                        torBootstrapScope.launch {
                            delay(DEFAULT_CONNECTIVITY_TIMEOUT_MS)
                            if (!isActive) {
                                log.w { "Bootstrap: Connectivity timeout - proceeding anyway" }
                                connectivityJob.cancel()
                                onInitialized()
                            }
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

    override fun deactivate() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null

        isActive = false
        super.deactivate()
    }
}