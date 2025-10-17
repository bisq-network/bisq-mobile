package network.bisq.mobile.domain.service.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.Action.Companion.stopDaemonSync
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.utils.Logging
import okio.FileSystem
import okio.Path
import okio.SYSTEM


/**
 * We use the external tor setup of Bisq Easy and use the kmp-tor runtime.
 * The task of that class is to start the kmp tor runtime and configure the data for the external tor setup.
 *
 * 1. Setup kmp tor runtime: Create runtime, set environment, config and add observers.
 * 2. Start the kmp tor runtime
 * 3. Find socksPort by listening on TorEvent.NOTICE with data: `Socks listener listening on port {socksPort}.`
 * 4. Find control port by observing creation or mutation of the `control.txt` file which contains the control port.
 *    Read the value from format: `PORT=127.0.0.1:{controlPort}`
 * 5. Write the `external_tor.config` with socks port, control port and path to the auth cookie. This will be used by the Bisq Easy tor lib.
 * 6. After tor daemon is started, we are completed. The network service can now be initialized.
 *    The bisq 2 tor lib will detect the external tor and use that.
 *
 */
class KmpTorService(private val baseDir: Path) : BaseService(), Logging {
    enum class State {
        IDLE,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        STARTING_FAILED,
        STOPPING_FAILED
    }

    private var torRuntime: TorRuntime? = null
    private var deferredSocksPort = CompletableDeferred<Int>()
    private var torDaemonStarted = CompletableDeferred<Boolean>()
    private var controlPortFileObserverJob: Job? = null
    private var configJob: Job? = null

    private val _startupFailure: MutableStateFlow<KmpTorException?> = MutableStateFlow(null)
    val startupFailure: StateFlow<KmpTorException?> get() = _startupFailure.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> get() = _state.asStateFlow()

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
        val torDir = getTorDir()
        val cacheDirectory = getTorCacheDir()
        val controlPortFile = getControlPortFile()
        val environment = TorRuntime.Environment.Builder(
            workDirectory = File(torDir.toString()),
            cacheDirectory = File(cacheDirectory.toString()),
            loader = ::torResourceLoader,
        )

        torRuntime = TorRuntime.Builder(environment) {
            required(TorEvent.ERR)
            observerStatic(TorEvent.ERR, OnEvent.Executor.Immediate) { data ->
                handleError("Tor error event: $data")
            }

            required(TorEvent.NOTICE)
            observerStatic(TorEvent.NOTICE, OnEvent.Executor.Immediate) { data ->
                tryParseSockPort(data)
            }

            // See https://github.com/05nelsonm/kmp-tor-resource/blob/master/docs/tor-man.adoc#DataDirectory
            config { _ ->
                TorOption.SocksPort.configure { auto() }
                TorOption.ControlPort.configure { auto() }
                TorOption.ControlPortWriteToFile.configure(File(controlPortFile.toString()))
                TorOption.CookieAuthentication.configure(true)
                TorOption.DataDirectory.configure(File(torDir.toString()))
                TorOption.CacheDirectory.configure(File(cacheDirectory.toString()))
                TorOption.DisableNetwork.configure(true) // Bisq Easy tor lib managed the DisableNetwork state, initially it is disabled.
                TorOption.NoExec.configure(true)
                TorOption.TruncateLogFile.configure(true)
            }
        }
    }

    private fun tryParseSockPort(data: String) {
        // Expected string: `Socks listener listening on port {port}.`
        if (data.startsWith("Socks listener listening on port ")) {
            log.i { "Tor Notice: $data" }
            val portAsString = data
                .removePrefix("Socks listener listening on port ")
                .trimEnd('.')
            val socksPort = portAsString.toInt()
            log.i { "Socks port: $socksPort" }
            deferredSocksPort.takeIf { !it.isCompleted }?.complete(socksPort)
        }
    }

    private fun configTor(): CompletableDeferred<Boolean> {
        val configCompleted = CompletableDeferred<Boolean>()
        configJob = launchIO {
            try {
                val socksPort = deferredSocksPort.await()
                val controlPort = readControlPort().await()
                disposeControlPortFileObserver()

                writeExternalTorConfig(socksPort, controlPort)
                torDaemonStarted.await()
                verifyControlPortAccessible(controlPort)
                delay(100L)

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

    private fun readControlPort(): Deferred<Int> {
        val deferred = CompletableDeferred<Int>()
        controlPortFileObserverJob = launchIO {
            val controlPortFile = getControlPortFile()
            log.i("Path to controlPortFile: $controlPortFile")
            try {
                // We can't use FileObserver because it misses events between event processing.
                // Tor writes the port to a swap file first, and renames it afterward.
                // The FileObserver can miss the second operation, causing a deadlock.
                // See Bisq Easy tor implementation at: bisq.network.tor.process.control_port.ControlPortFilePoller

                var lastModified = 0L
                var iterations = 0
                val delay: Long = 100
                val maxIterations = 30 * 1000 / delay // 30 seconds with 100ms delays
                val startTime = Clock.System.now().toEpochMilliseconds()
                val timeoutMs = 60_000 // 60 second timeout
                while (!deferred.isCompleted && iterations < maxIterations) {
                    iterations++
                    if (Clock.System.now().toEpochMilliseconds() - startTime > timeoutMs) {
                        deferred.completeExceptionally(
                            KmpTorException("Timeout waiting for control port file")
                        )
                        break
                    }
                    log.i("readControlPort iterations=$iterations")
                    val currentMetadata = FileSystem.SYSTEM.metadataOrNull(controlPortFile)
                    if (currentMetadata != null) {
                        val currentModified = currentMetadata.lastModifiedAtMillis ?: 0L
                        if (currentModified != lastModified) {
                            lastModified = currentModified
                            log.i("We detected modification change of controlPortFile: $controlPortFile")
                            readControlPortFile(controlPortFile, deferred)
                        }
                    }
                    if (!deferred.isCompleted) {
                        delay(delay)
                    }
                }
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(
                        KmpTorException("Failed to read control port after $iterations iterations")
                    )
                }
            } catch (e: Exception) {
                handleError("Observing file controlPortFile failed: ${e.message}")
                deferred.completeExceptionally(e)
            } finally {
                if (controlPortFileObserverJob === this@launchIO) {
                    controlPortFileObserverJob = null
                }
            }
        }
        return deferred
    }

    private fun readControlPortFile(file: Path, deferred: CompletableDeferred<Int>) {
        try {
            // Expected string in file: `PORT=127.0.0.1:{port}`
            val lines = FileSystem.SYSTEM.read(file) {
                readUtf8().lines()
            }
            val line = lines.firstOrNull { it.startsWith("PORT=127.0.0.1:") }
                ?: error("No PORT line found")
            val port = line.removePrefix("PORT=127.0.0.1:").toInt()
            deferred.takeIf { !it.isCompleted }?.complete(port)
            log.i("Control port read from control.txt file: $port")

            // Rename the file so the observer doesn't pick up an old file
            val backup = getControlPortBackupFile()
            FileSystem.SYSTEM.atomicMove(getControlPortFile(), backup)
        } catch (error: Exception) {
            handleError("Failed to read control port from control.txt file: $error")
            deferred.completeExceptionally(error)
        }
    }

    private suspend fun writeExternalTorConfig(socksPort: Int, controlPort: Int) {
        try {
            val torDir = getTorDir()
            val cookieFile = torDir / "control_auth_cookie"
            val configContent = buildString {
                appendLine("UseExternalTor 1")
                appendLine("CookieAuthentication 1")
                appendLine("CookieAuthFile ${cookieFile.toString()}")
                appendLine("SocksPort 127.0.0.1:$socksPort")
                appendLine("ControlPort 127.0.0.1:$controlPort")
            }

            val configFile = torDir / "external_tor.config"
            withContext(IODispatcher) {
                FileSystem.SYSTEM.write(configFile) {
                    writeUtf8(configContent)
                    flush()
                }
            }

            // Validate that the config file was written correctly and is readable
            validateExternalTorConfig(configFile, socksPort, controlPort)

            log.i { "Wrote external_tor.config to ${configFile}\n\n$configContent\n\n" }
        } catch (error: Exception) {
            handleError("Failed to write external_tor.config: $error")
            throw error
        }
    }

    private suspend fun validateExternalTorConfig(configFile: Path, expectedSocksPort: Int, expectedControlPort: Int) {
        try {
            withContext(IODispatcher) {
                if (!FileSystem.SYSTEM.exists(configFile)) {
                    throw KmpTorException("external_tor.config file does not exist after writing")
                }
                val content = FileSystem.SYSTEM.read(configFile) { readUtf8() }
                if (!content.contains("UseExternalTor 1")) {
                    throw KmpTorException("external_tor.config missing UseExternalTor directive")
                }
                if (!content.contains("SocksPort 127.0.0.1:$expectedSocksPort")) {
                    throw KmpTorException("external_tor.config missing or incorrect SocksPort")
                }
                if (!content.contains("ControlPort 127.0.0.1:$expectedControlPort")) {
                    throw KmpTorException("external_tor.config missing or incorrect ControlPort")
                }
                log.i { "external_tor.config validation successful" }
            }
        } catch (error: Exception) {
            log.e(error) { "external_tor.config validation failed" }
            throw KmpTorException("external_tor.config validation failed: ${error.message}", error)
        }
    }

    private suspend fun verifyControlPortAccessible(controlPort: Int) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        try {
            delay(500)
            repeat(3) { attempt ->
                try {
                    val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", controlPort)
                    socket.close()
                    log.i { "Verified control port $controlPort is accessible" }
                    return
                } catch (e: Exception) {
                    if (attempt < 2) delay(250)
                }
            }
            log.w { "Control port $controlPort not yet accessible, but continuing anyway" }
        } catch (e: Exception) {
            log.w(e) { "Control port $controlPort not yet accessible, but continuing anyway" }
        } finally {
            selectorManager.close()
        }
    }

    private fun getTorDir(): Path {
        val torDir = baseDir / "tor"
        if (!FileSystem.SYSTEM.exists(torDir)) {
            FileSystem.SYSTEM.createDirectories(torDir)
        }
        return torDir
    }

    private fun getTorCacheDir(): Path {
        val torDir = getTorDir()
        val cacheDir = torDir / "cache"
        if (!FileSystem.SYSTEM.exists(cacheDir)) {
            FileSystem.SYSTEM.createDirectories(cacheDir)
        }
        return cacheDir
    }

    private fun getControlPortFile(): Path = getTorDir() / "control-port.txt"

    private fun getControlPortBackupFile(): Path = getTorDir() / "control-port-backup.txt"

    private fun handleError(messageString: String) {
        log.e(messageString)
        _startupFailure.value = KmpTorException(messageString)
    }

    private fun resetAndDispose() {
        if (deferredSocksPort.isActive) {
            deferredSocksPort.cancel("KmpTorService resetAndDispose called before socks port became available")
        }
        deferredSocksPort = CompletableDeferred()
        torDaemonStarted = CompletableDeferred()
        configJob?.cancel()
        configJob = null
        disposeControlPortFileObserver()
        _startupFailure.value = null
    }

    private fun disposeControlPortFileObserver() {
        controlPortFileObserverJob?.cancel()
        controlPortFileObserverJob = null
    }

    /**
     * Suspends until socks port is available
     */
    suspend fun getSocksPort(): Int {
        return deferredSocksPort.await()
    }
}
