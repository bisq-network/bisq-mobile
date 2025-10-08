package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.Action.Companion.stopDaemonSync
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.net.Port.Ephemeral.Companion.toPortEphemeral
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.utils.Logging

/**
 * We use the external tor setup of Bisq Easy and use the kmp-tor runtime.
 * The task of that class is to start the kmp tor runtime and configure the data for the external tor setup.
 *
 * 1. Setup kmp tor runtime: Create runtime, set environment, config and add observers.
 * 2. Start the kmp tor runtime
 * 3. Find socksPort by listening on TorEvent.NOTICE with data: `Opening Socks listener on 127.0.0.1:{socksPort}.`
 * 4. After tor daemon is started, we are completed.
 */
class KmpTorClientService() : BaseService(), Logging {
    enum class State {
        IDLE,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        STARTING_FAILED,
        STOPPING_FAILED
    }

    companion object {
        const val SOCKS_PORT = 8090
    }

    private var baseDirPath: String? = null
    private var torRuntime: TorRuntime? = null
    private var deferredSocksPort = CompletableDeferred<Int>()
    private var torDaemonStarted = CompletableDeferred<Boolean>()
    private var bootstrapCompleted = CompletableDeferred<Boolean>()
    private var controlPortFileObserverJob: Job? = null
    private var configJob: Job? = null

    private val _startupFailure: MutableStateFlow<KmpTorException?> = MutableStateFlow(null)
    val startupFailure: StateFlow<KmpTorException?> get() = _startupFailure.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> get() = _state.asStateFlow()

    fun setBaseDirPath(value: String?) {
        baseDirPath = value
    }

    fun startTor(): CompletableDeferred<Boolean> {
        log.i("Start kmp-tor")
        val torStartupCompleted = CompletableDeferred<Boolean>()

        require(torRuntime == null) { "torRuntime is expected to be null at startTor" }
        _state.value = State.STARTING
        setupTorRuntime()

        val configCompleted = configTor()

        torRuntime!!.enqueue(
            Action.StartDaemon,
            { error ->
                resetAndDispose()
                handleError("Starting tor daemon failed: $error")
                torStartupCompleted.takeIf { !it.isCompleted }?.completeExceptionally(
                    KmpTorException("Starting tor daemon failed: $error")
                )
                // Cancel the running config coroutine
                configJob?.cancel()
                configJob = null
                _state.value = State.STARTING_FAILED
            },
            {
                log.i("Tor daemon started")
                torDaemonStarted.takeIf { !it.isCompleted }?.complete(true)

                launchIO {
                    configCompleted.await()
                    log.i("kmp-tor startup completed")

                    bootstrapCompleted.await()
                    log.i("kmp-tor bootstrap completed")

                    torStartupCompleted.takeIf { !it.isCompleted }?.complete(true)
                    _state.value = State.STARTED
                }
            }
        )
        return torStartupCompleted
    }

    fun stopTorSync() {
        _state.value = State.STOPPING
        if (torRuntime == null) {
            log.w("Tor runtime is null at stopTorSync")
            return
        }

        try {
            torRuntime!!.stopDaemonSync()
            log.i { "Tor daemon stopped" }
            _state.value = State.STOPPED
        } catch (e: Exception) {
            handleError("Failed to stop Tor daemon: $e")
            _state.value = State.STOPPING_FAILED
            throw e
        } finally {
            resetAndDispose()
            torRuntime = null
        }
    }

    private fun setupTorRuntime() {
        val environment = torRuntimeEnvironment(baseDirPath)

        torRuntime = TorRuntime.Builder(environment) {
            required(TorEvent.ERR)
            observerStatic(TorEvent.ERR, OnEvent.Executor.Immediate) { data ->
                handleError("Tor error event: $data")
            }

            required(TorEvent.NOTICE)
            observerStatic(TorEvent.NOTICE, OnEvent.Executor.Immediate) { data ->
                tryParseSockPort(data)
                waitForBootstrappedCompleted(data)
            }

            config {
                // We use the isNonPersistent version (SocksPort persist the port)
                // TODO We should use auto() but that comes with complications for Koin setup
                // TorOption.__SocksPort.configure { auto() }
                TorOption.__SocksPort.configure {
                    port(SOCKS_PORT.toPortEphemeral())
                }
            }
        }
    }

    private fun tryParseSockPort(data: String) {
        // Expected string: `Opening Socks listener on 127.0.0.1:{port}.`
        log.i { "Tor Notice: $data" }
        if (data.startsWith("Opening Socks listener on 127.0.0.1:")) {
            log.i { "Tor Notice: $data" }
            val portAsString = data
                .removePrefix("Opening Socks listener on 127.0.0.1:")
                .trimEnd('.')
            val socksPort = portAsString.toInt()
            log.i { "Socks port: $socksPort" }
            deferredSocksPort.takeIf { !it.isCompleted }?.complete(socksPort)
        }
    }

    private fun waitForBootstrappedCompleted(data: String) {
        if (data.equals("Bootstrapped 100% (done): Done")) {
            bootstrapCompleted.complete(true)
        }
    }

    private fun configTor(): CompletableDeferred<Boolean> {
        val configCompleted = CompletableDeferred<Boolean>()
        configJob = launchIO {
            try {
                val socksPort = deferredSocksPort.await()
                log.i { "Tor socksPort: $socksPort" }
                torDaemonStarted.await()

                log.i { "Tor configuration completed successfully" }
                configCompleted.takeIf { !it.isCompleted }?.complete(true)
            } catch (error: Exception) {
                log.e(error) { "Configuring tor failed" }
                handleError("Configuring tor failed: $error")
                configCompleted.takeIf { !it.isCompleted }?.completeExceptionally(error)
            } finally {
                if (configJob === this@launchIO) {
                    configJob = null
                }
            }
        }
        return configCompleted
    }

    private fun handleError(messageString: String) {
        log.e(messageString)
        _startupFailure.value = KmpTorException(messageString)
    }

    private fun resetAndDispose() {
        deferredSocksPort = CompletableDeferred()
        torDaemonStarted = CompletableDeferred()
        bootstrapCompleted = CompletableDeferred()
        configJob?.cancel()
        configJob = null
        disposeControlPortFileObserver()
        _startupFailure.value = null
    }

    private fun disposeControlPortFileObserver() {
        controlPortFileObserverJob?.cancel()
        controlPortFileObserverJob = null
    }
}
