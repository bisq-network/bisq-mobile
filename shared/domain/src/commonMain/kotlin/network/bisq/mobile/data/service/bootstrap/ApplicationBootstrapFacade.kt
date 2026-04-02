package network.bisq.mobile.data.service.bootstrap

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.KmpTorService.TorState
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import kotlin.concurrent.Volatile

abstract class ApplicationBootstrapFacade(
    private val kmpTorService: KmpTorService,
) : ServiceFacade() {
    companion object {
        @Volatile
        var isDemo = false
        private const val BOOTSTRAP_STAGE_TIMEOUT_MS =
            90_000L // 90 seconds per stage

        // Grace period before showing the Tor bootstrap failed dialog.
        // On iOS, kmp-tor can emit transient Stopped(error) events during initial
        // circuit establishment (especially over slow networks or onion addresses).
        // We suppress the failure dialog during this window and let Tor retry.
        internal const val TOR_FAILURE_GRACE_PERIOD_MS = 60_000L
    }

    @Volatile
    private var currentTimeoutJob: Job? = null

    @Volatile
    private var bootstrapSuccessful = false

    @Volatile
    private var torProgressCollectJob: Job? = null

    @Volatile
    private var torStartingTimestamp: Long = 0L

    // Overridable for testing — allows tests to control time without platform-specific calls
    protected open fun currentTimeMillis(): Long = DateUtils.now()

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> get() = _state.asStateFlow()

    fun setState(value: String) {
        _state.value = value
    }

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> get() = _progress.asStateFlow()

    fun setProgress(value: Float) {
        _progress.value = value
    }

    private val _isTimeoutDialogVisible = MutableStateFlow(false)
    val isTimeoutDialogVisible: StateFlow<Boolean> get() = _isTimeoutDialogVisible.asStateFlow()

    fun setTimeoutDialogVisible(visible: Boolean) {
        _isTimeoutDialogVisible.value = visible
    }

    private val _isBootstrapFailed = MutableStateFlow(false)
    val isBootstrapFailed: StateFlow<Boolean> get() = _isBootstrapFailed.asStateFlow()

    fun setBootstrapFailed(failed: Boolean) {
        _isBootstrapFailed.value = failed
    }

    private val _torBootstrapFailed = MutableStateFlow(false)
    val torBootstrapFailed: StateFlow<Boolean> =
        _torBootstrapFailed.asStateFlow()

    fun setTorBootstrapFailed(failed: Boolean) {
        _torBootstrapFailed.value = failed
    }

    private val _currentBootstrapStage = MutableStateFlow("")
    val currentBootstrapStage: StateFlow<String> get() = _currentBootstrapStage.asStateFlow()

    fun setCurrentBootstrapStage(stage: String) {
        _currentBootstrapStage.value = stage
    }

    private val _shouldShowProgressToast = MutableStateFlow(false)
    val shouldShowProgressToast: StateFlow<Boolean> get() = _shouldShowProgressToast.asStateFlow()

    fun setShouldShowProgressToast(show: Boolean) {
        _shouldShowProgressToast.value = show
    }

    override suspend fun activate() {
        // Reset all transient state for a fresh bootstrap attempt.
        // Without this, a deactivate/activate cycle (e.g. retry after connection failure)
        // would leave stale flags that cause observers to fire immediately.
        _torBootstrapFailed.value = false
        _isBootstrapFailed.value = false
        _isTimeoutDialogVisible.value = false
        _shouldShowProgressToast.value = false
        _currentBootstrapStage.value = ""
        bootstrapSuccessful = false
        currentTimeoutJob?.cancel()
        currentTimeoutJob = null
        torProgressCollectJob?.cancel()
        torProgressCollectJob = null
        torStartingTimestamp = 0L

        super.activate()
    }

    override suspend fun deactivate() {
        cancelTimeout()
        super.deactivate()
    }

    fun handleBootstrapFailure(e: Throwable) {
        setBootstrapFailed(true)
        setShouldShowProgressToast(false)
    }

    protected fun observeTorState() {
        serviceScope.launch {
            kmpTorService.state.collect { newState ->
                when (newState) {
                    is TorState.Starting -> {
                        torStartingTimestamp = currentTimeMillis()
                        setState("mobile.bootstrap.tor.starting".i18n(0))
                        setProgress(0.1f)
                        startTimeoutForStage()
                        torProgressCollectJob?.cancel()
                        torProgressCollectJob =
                            serviceScope.launch {
                                kmpTorService.bootstrapProgress.collect {
                                    setState("mobile.bootstrap.tor.starting".i18n(it))
                                }
                            }
                    }

                    is TorState.Started -> {
                        torProgressCollectJob?.cancel()
                        setState("mobile.bootstrap.tor.started".i18n())
                        setProgress(0.25f)
                        onTorStarted()
                    }

                    is TorState.Stopping -> {}

                    is TorState.Stopped -> {
                        torProgressCollectJob?.cancel()
                        if (newState.error != null) {
                            val errorMessage =
                                listOfNotNull(
                                    newState.error.message,
                                    newState.error.cause?.message,
                                ).firstOrNull()
                                    ?: "Unknown Tor error"

                            val elapsed = currentTimeMillis() - torStartingTimestamp
                            if (torStartingTimestamp > 0 && elapsed < TOR_FAILURE_GRACE_PERIOD_MS) {
                                // Within grace period — Tor may still recover. Log but don't
                                // show the failure dialog yet. The general stage timeout
                                // (90s) will catch genuinely stuck connections.
                                log.w { "Bootstrap: Tor error within grace period (${elapsed / 1000}s): $errorMessage — suppressing failure dialog" }
                                setState("mobile.bootstrap.tor.starting".i18n(0))
                            } else {
                                setState("mobile.bootstrap.tor.failed".i18n() + ": $errorMessage")
                                cancelTimeout(showProgressToast = false)
                                setTorBootstrapFailed(true)
                                log.e { "Bootstrap: Tor initialization failed - $errorMessage" }
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun startTimeoutForStage(
        stageName: String = state.value,
        extendedTimeout: Boolean = false,
    ) {
        currentTimeoutJob?.cancel()
        setTimeoutDialogVisible(false)
        setCurrentBootstrapStage(stageName)

        if (bootstrapSuccessful) {
            return
        }

        val timeoutDuration =
            if (extendedTimeout) {
                BOOTSTRAP_STAGE_TIMEOUT_MS * 2 // 2x longer for extended wait
            } else {
                BOOTSTRAP_STAGE_TIMEOUT_MS //  Normal timeout
            }

        log.i { "Bootstrap: Starting timeout for stage: $stageName (${timeoutDuration / 1000}s)" }

        currentTimeoutJob =
            serviceScope.launch {
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

    protected fun cancelTimeout(showProgressToast: Boolean = true) {
        currentTimeoutJob?.cancel()
        currentTimeoutJob = null

        // If dialog was visible and we're cancelling due to progress, show toast
        setTimeoutDialogVisible(isTimeoutDialogVisible.value && showProgressToast && !isBootstrapFailed.value)
    }

    fun extendTimeout() {
        log.i { "Bootstrap: Extending timeout for current stage" }
        val currentStage = currentBootstrapStage.value
        if (currentStage.isNotEmpty()) {
            // Restart timeout with double the duration for extended wait
            startTimeoutForStage(currentStage, extendedTimeout = true)
        }
        setTimeoutDialogVisible(false)
    }

    protected open fun onInitialized() {
        bootstrapSuccessful = true
        cancelTimeout()
    }

    protected open fun onTorStarted() {}

    fun startTor(purgeTorDir: Boolean) {
        serviceScope.launch {
            setTorBootstrapFailed(false)
            if (purgeTorDir) {
                kmpTorService.stopAndPurgeWorkingDir()
            }
            kmpTorService.startTor()
        }
    }
}
