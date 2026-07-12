package network.bisq.mobile.data.service.bootstrap

import androidx.annotation.VisibleForTesting
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
        private const val BOOTSTRAP_ELAPSED_TICK_MS = 1_000L

        // Grace period before arming the failure dialog for transient Tor bootstrap errors.
        // On iOS, kmp-tor can emit transient Stopped(error) events during initial circuit
        // establishment (especially over slow networks or onion addresses). Start retries
        // are owned by [network.bisq.mobile.data.service.network.NetworkServiceFacade].
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

    @Volatile
    private var torBootstrapStartObserved: Boolean = false

    @Volatile
    private var torGracePeriodArmJob: Job? = null

    @Volatile
    private var pendingGracePeriodErrorMessage: String? = null

    @Volatile
    private var elapsedTimerJob: Job? = null

    @Volatile
    private var bootstrapStartedTimestamp: Long = 0L

    // Overridable for testing — allows tests to control time without platform-specific calls
    protected open fun currentTimeMillis(): Long = DateUtils.now()

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state.asStateFlow()

    fun setState(value: String) {
        _state.value = value
    }

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun setProgress(value: Float) {
        _progress.value = value
    }

    private val _torBootstrapProgress = MutableStateFlow(0)
    val torBootstrapProgress: StateFlow<Int> = _torBootstrapProgress.asStateFlow()

    fun setTorBootstrapProgress(value: Int) {
        _torBootstrapProgress.value = value.coerceIn(0, 100)
    }

    private val _bootstrapElapsedSeconds = MutableStateFlow(0L)
    val bootstrapElapsedSeconds: StateFlow<Long> = _bootstrapElapsedSeconds.asStateFlow()

    private val _isTimeoutDialogVisible = MutableStateFlow(false)
    val isTimeoutDialogVisible: StateFlow<Boolean> = _isTimeoutDialogVisible.asStateFlow()

    fun setTimeoutDialogVisible(visible: Boolean) {
        _isTimeoutDialogVisible.value = visible
    }

    private val _isBootstrapFailed = MutableStateFlow(false)
    val isBootstrapFailed: StateFlow<Boolean> = _isBootstrapFailed.asStateFlow()

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
    val currentBootstrapStage: StateFlow<String> = _currentBootstrapStage.asStateFlow()

    fun setCurrentBootstrapStage(stage: String) {
        _currentBootstrapStage.value = stage
    }

    private val _shouldShowProgressToast = MutableStateFlow(false)
    val shouldShowProgressToast: StateFlow<Boolean> = _shouldShowProgressToast.asStateFlow()

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
        _torBootstrapProgress.value = 0
        _bootstrapElapsedSeconds.value = 0L
        bootstrapSuccessful = false
        currentTimeoutJob?.cancel()
        currentTimeoutJob = null
        torProgressCollectJob?.cancel()
        torProgressCollectJob = null
        elapsedTimerJob?.cancel()
        elapsedTimerJob = null
        torStartingTimestamp = 0L
        torBootstrapStartObserved = false
        cancelGracePeriodArmJob()
        pendingGracePeriodErrorMessage = null
        bootstrapStartedTimestamp = currentTimeMillis()

        super.activate()
        startElapsedTimer()
    }

    override suspend fun deactivate() {
        cancelGracePeriodArmJob()
        pendingGracePeriodErrorMessage = null
        cancelTimeout()
        cancelElapsedTimer()
        super.deactivate()
    }

    fun handleBootstrapFailure(e: Throwable) {
        log.e(e) { "Bootstrap failed" }
        setBootstrapFailed(true)
        setShouldShowProgressToast(false)
        // Failure is terminal: stop the in-flight jobs so a pending stage timeout can't later show its
        // dialog (contradicting the failure state) and the elapsed ticker stops its background work.
        cancelTimeout(showProgressToast = false)
        cancelElapsedTimer()
    }

    protected fun observeTorState() {
        serviceScope.launch {
            kmpTorService.state.collect { newState ->
                when (newState) {
                    is TorState.Starting -> {
                        cancelGracePeriodArmJob()
                        pendingGracePeriodErrorMessage = null
                        torBootstrapStartObserved = true
                        torStartingTimestamp = currentTimeMillis()
                        setState("mobile.bootstrap.tor.starting".i18n(0))
                        setProgress(0.1f)
                        startTimeoutForStage()
                        torProgressCollectJob?.cancel()
                        torProgressCollectJob =
                            serviceScope.launch {
                                kmpTorService.bootstrapProgress.collect {
                                    setTorBootstrapProgress(it)
                                    setState("mobile.bootstrap.tor.starting".i18n(it))
                                }
                            }
                    }

                    is TorState.Started -> {
                        cancelGracePeriodArmJob()
                        pendingGracePeriodErrorMessage = null
                        torProgressCollectJob?.cancel()
                        setTorBootstrapProgress(100)
                        setState("mobile.bootstrap.tor.started".i18n())
                        setProgress(0.25f)
                        onTorStarted()
                    }

                    is TorState.Stopping -> {}

                    is TorState.Stopped -> {
                        torProgressCollectJob?.cancel()
                        val error = newState.error ?: return@collect
                        handleTorBootstrapError(error)
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
        cancelElapsedTimer()
    }

    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob =
            serviceScope.launch {
                while (!bootstrapSuccessful) {
                    val elapsedMillis = currentTimeMillis() - bootstrapStartedTimestamp
                    val elapsedSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
                    _bootstrapElapsedSeconds.value = elapsedSeconds
                    delay(BOOTSTRAP_ELAPSED_TICK_MS)
                }
            }
    }

    protected fun cancelElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = null
    }

    protected open fun onTorStarted() {}

    @VisibleForTesting
    internal fun markTorBootstrapStartingForTest(startTimestamp: Long) {
        torBootstrapStartObserved = true
        torStartingTimestamp = startTimestamp
    }

    @VisibleForTesting
    internal fun handleTorBootstrapErrorForTest(error: Throwable) {
        handleTorBootstrapError(error)
    }

    private fun handleTorBootstrapError(error: Throwable) {
        val errorMessage =
            generateSequence(error) { it.cause }
                .mapNotNull { it.message }
                .firstOrNull()
                ?: "Unknown Tor error"

        // Ignore stale Stopped(error) emissions from before the current bootstrap attempt.
        if (!torBootstrapStartObserved) {
            return
        }

        if (TorBootstrapErrorClassification.isTerminal(error)) {
            cancelGracePeriodArmJob()
            pendingGracePeriodErrorMessage = null
            armTorBootstrapFailed(errorMessage)
            return
        }

        val elapsed = currentTimeMillis() - torStartingTimestamp
        if (elapsed >= TOR_FAILURE_GRACE_PERIOD_MS) {
            cancelGracePeriodArmJob()
            pendingGracePeriodErrorMessage = null
            armTorBootstrapFailed(errorMessage)
            return
        }

        log.w { "Transient Tor bootstrap error within grace period (${elapsed}ms): $errorMessage" }
        pendingGracePeriodErrorMessage = errorMessage
        setState("mobile.bootstrap.tor.starting".i18n(0))
        scheduleGracePeriodArm()
    }

    private fun armTorBootstrapFailed(errorMessage: String) {
        cancelGracePeriodArmJob()
        pendingGracePeriodErrorMessage = null
        setState("mobile.bootstrap.tor.failed".i18n() + ": $errorMessage")
        cancelTimeout(showProgressToast = false)
        setTorBootstrapFailed(true)
        torBootstrapStartObserved = false
        torStartingTimestamp = 0L
        log.e { "Bootstrap: Tor initialization failed - $errorMessage" }
    }

    private fun scheduleGracePeriodArm() {
        if (torGracePeriodArmJob?.isActive == true) {
            return
        }
        val delayMs =
            (torStartingTimestamp + TOR_FAILURE_GRACE_PERIOD_MS - currentTimeMillis())
                .coerceAtLeast(0L)
        torGracePeriodArmJob =
            serviceScope.launch {
                try {
                    delay(delayMs)
                    if (torBootstrapStartObserved && !_torBootstrapFailed.value) {
                        armTorBootstrapFailed(
                            pendingGracePeriodErrorMessage ?: "Unknown Tor error",
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        log.d { "Bootstrap: Grace-period arm job cancelled" }
                    } else {
                        log.e(e) { "Bootstrap: Error in grace-period arm job" }
                    }
                }
            }
    }

    private fun cancelGracePeriodArmJob() {
        torGracePeriodArmJob?.cancel()
        torGracePeriodArmJob = null
    }

    fun startTor(purgeTorDir: Boolean) {
        serviceScope.launch {
            setTorBootstrapFailed(false)
            cancelGracePeriodArmJob()
            pendingGracePeriodErrorMessage = null
            torBootstrapStartObserved = false
            torStartingTimestamp = 0L
            // Restarting Tor is a fresh connection attempt, so reset the slow-path clock:
            // otherwise the "still connecting, this is normal" banner would reappear immediately
            // showing the elapsed time of the failed attempt. The elapsed ticker keeps running
            // (bootstrap has not succeeded), so it simply continues from this new baseline.
            bootstrapStartedTimestamp = currentTimeMillis()
            _bootstrapElapsedSeconds.value = 0L
            if (purgeTorDir) {
                kmpTorService.stopAndPurgeWorkingDir()
            }
            kmpTorService.startTor()
        }
    }
}
