package network.bisq.mobile.domain.service.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.tor.runtime.Action.Companion.startDaemonAsync
import io.matthewnelson.kmp.tor.runtime.Action.Companion.stopDaemonAsync
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.coroutines.cancellation.CancellationException


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
    sealed class TorState {
        protected abstract val i18nKey: String

        data class Stopped(val error: Throwable? = null) : TorState() {
            override val i18nKey = "mobile.kmpTorService.state.stopped"
        }

        object Stopping : TorState() {
            override val i18nKey = "mobile.kmpTorService.state.stopping"
        }

        object Starting : TorState() {
            override val i18nKey = "mobile.kmpTorService.state.starting"
        }

        object Started : TorState() {
            override val i18nKey = "mobile.kmpTorService.state.started"
        }

        val displayString: String get() = i18nKey.i18n()
    }

    private var torRuntime: TorRuntime? = null
    private var startDefer: Deferred<Unit>? = null
    private var controlMutex = Mutex()

    private val _state = MutableStateFlow<TorState>(TorState.Stopped())
    val state = _state.asStateFlow()

    private val _socksPort: MutableStateFlow<Int?> = MutableStateFlow(null)
    val socksPort = _socksPort.asStateFlow()

    private val _bootstrapProgress: MutableStateFlow<Int> = MutableStateFlow(0)
    val bootstrapProgress: StateFlow<Int> = _bootstrapProgress.asStateFlow()

    private val bootstrapRegex = Regex("""Bootstrapped (\d+)%""")

    suspend fun startTor(timeoutMs: Long = 60_000): Boolean {
        when (_state.value) {
            is TorState.Started -> return true
            is TorState.Stopping -> return false
            is TorState.Starting -> {
                return _state.filter { it !is TorState.Starting }.first() is TorState.Started
            }

            is TorState.Stopped -> {
                try {
                    var remainingTime = 0L
                    controlMutex.withLock {
                        if (_state.value !is TorState.Stopped) {
                            // state may have changed since we last checked it
                            return _state.filter { it !is TorState.Starting }.first() is TorState.Started
                        }
                        log.i("Starting kmp-tor")
                        _state.value = TorState.Starting
                        startDefer = serviceScope.async {
                            val runtime = getTorRuntime()
                            val startTime = Clock.System.now().toEpochMilliseconds()
                            withContext(Dispatchers.IO) {
                                withTimeout(timeoutMs) {
                                    runtime.startDaemonAsync()
                                    configTor()
                                }
                            }
                            val durationMs = Clock.System.now().toEpochMilliseconds() - startTime
                            remainingTime = (timeoutMs - durationMs).coerceAtLeast(0)
                        }
                        startDefer?.await()
                    }
                    withTimeout(remainingTime) {
                        awaitBootstrapped()
                    }
                    _state.value = TorState.Started
                    log.i("Started kmp-tor successfully")
                    return true
                } catch (error: Throwable) {
                    try {
                        withContext(Dispatchers.IO) {
                            val runtime = getTorRuntime()
                            runtime.stopDaemonAsync()
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                    cleanupService()
                    _state.value = TorState.Stopped(error)

                    val errorMessage = listOfNotNull(
                        error.message,
                        error.cause?.message
                    ).firstOrNull() ?: "Unknown Tor error"
                    log.e(error) { "Starting kmp-tor daemon failed: $errorMessage" }
                    currentCoroutineContext().ensureActive()
                    return false
                } finally {
                    startDefer =
                        null // ensure that startDefer always becomes null on cancel or success
                }
            }
        }

    }

    private suspend fun awaitBootstrapped() {
        return coroutineScope {
            val bootstrapDeferred = async {
                withContext(serviceScope.coroutineContext) {
                    _bootstrapProgress.filter { it >= 100 }.first()
                }
            }
            val stopDetectionJob = launch { awaitStopped(); bootstrapDeferred.cancel() }
            try {
                bootstrapDeferred.await()
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
            } finally {
                stopDetectionJob.cancel()
            }
        }
    }

    private suspend fun awaitStopped(): TorState.Stopped {
        return withContext(serviceScope.coroutineContext) {
            _state.filter { it is TorState.Stopped }.first() as TorState.Stopped
        }
    }

    /**
     * Suspends until socks port is available or state is Stopped.
     *
     * Will return early with null if state is already Stopped
     */
    suspend fun awaitSocksPort(): Int? {
        return coroutineScope {
            // then we race for port or stop of service
            val portDeferred = async {
                withContext(serviceScope.coroutineContext) {
                    socksPort.filterNotNull().first()
                }
            }
            val stopDetectionJob = launch { awaitStopped(); portDeferred.cancel() }
            try {
                portDeferred.await()
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive() // job cancellation is separate from current coroutine cancellation
                null
            } finally {
                stopDetectionJob.cancel()
            }
        }
    }

    suspend fun stopTor() {
        startDefer?.cancel()
        controlMutex.withLock {
            when (_state.value) {
                is TorState.Stopped, is TorState.Stopping -> return
                else -> {
                    _state.value = TorState.Stopping
                    try {
                        val runtime = getTorRuntime()
                        withContext(Dispatchers.IO) {
                            runtime.stopDaemonAsync()
                        }
                        log.i { "Tor daemon stopped successfully" }
                        _state.value = TorState.Stopped()
                    } catch (e: Exception) {
                        log.e(e) { "Tor daemon stopped with error" }
                        _state.value = TorState.Stopped(e)
                    } finally {
                        cleanupService()
                    }
                }
            }
        }
    }

    private fun getTorRuntime(): TorRuntime {
        torRuntime?.let { return it }

        val torDir = getTorDir()
        val cacheDirectory = getTorCacheDir()
        val controlPortFile = getControlPortFile()
        val environment = TorRuntime.Environment.Builder(
            workDirectory = File(torDir.toString()),
            cacheDirectory = File(cacheDirectory.toString()),
            loader = ::torResourceLoader,
        )

        val runtime = TorRuntime.Builder(environment) {
            required(TorEvent.ERR)
            observerStatic(TorEvent.ERR, OnEvent.Executor.Immediate) { data ->
                log.e("Tor error event: $data")
            }

            required(TorEvent.NOTICE)
            observerStatic(TorEvent.NOTICE, OnEvent.Executor.Immediate) { data ->
                tryParseSockPort(data)
                tryParseBootstrapProgress(data)
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

        torRuntime = runtime
        return runtime
    }

    private fun tryParseSockPort(data: String) {
        // Expected string: `Socks listener listening on port {port}.`
        if (data.startsWith("Socks listener listening on port ")) {
            log.i { "Tor Notice: $data" }
            val portAsString = data
                .removePrefix("Socks listener listening on port ")
                .trimEnd('.')
            val parsedPort = portAsString.toIntOrNull()
            if (parsedPort == null) {
                log.e { "Failed to parse socks port from: $data" }
            } else {
                log.i { "Socks port: $parsedPort" }
                _socksPort.value = parsedPort
            }
        }
    }

    private fun tryParseBootstrapProgress(data: String) {
        // Expected string: `Bootstrapped 00%: Starting` or `Bootstrapped 100%: Done`
        bootstrapRegex.find(data)?.let { matchResult ->
            val percentage = matchResult.groupValues[1].toIntOrNull()
            if (percentage == null) {
                log.w { "Failed to parse bootstrap progress from: $data" }
            } else {
                _bootstrapProgress.value = percentage
                log.i { "Tor bootstrap progress: $percentage%" }

                if (percentage == 100) {
                    log.i { "Tor successfully connected (100% bootstrapped)" }
                }
            }
        }
    }

    private suspend fun configTor() {
        try {
            // Note: protected by outer withTimeout in startTor()
            val socksPort = socksPort.filterNotNull().first()
            val controlPort = readControlPort()

            writeExternalTorConfig(socksPort, controlPort)
            verifyControlPortAccessible(controlPort)
            delay(100L)

            log.i { "Tor configuration completed successfully" }
        } catch (error: Exception) {
            throw error
        }
    }

    private suspend fun readControlPort(): Int {
        val controlPortFile = getControlPortFile()
        log.i("Path to controlPortFile: $controlPortFile")
        try {
            // We can't use FileObserver because it misses events between event processing.
            // Tor writes the port to a swap file first, and renames it afterward.
            // The FileObserver can miss the second operation, causing a deadlock.
            // See Bisq Easy tor implementation at: bisq.network.tor.process.control_port.ControlPortFilePoller

            val delay: Long = 100
            val startTime = Clock.System.now().toEpochMilliseconds()
            val timeoutMs = 30_000 // 30 second timeout
            while (true) {
                if (Clock.System.now().toEpochMilliseconds() - startTime > timeoutMs) {
                    throw KmpTorException("Timed out waiting for control port file")
                }
                val currentMetadata = withContext(Dispatchers.IO) {
                    FileSystem.SYSTEM.metadataOrNull(controlPortFile)
                }
                if (currentMetadata != null) {
                    return parsePortFromFile(controlPortFile)
                }
                delay(delay)
            }
        } catch (e: Exception) {
            log.e(e) { "Observing file controlPortFile failed" }
            throw e
        }
    }

    private suspend fun parsePortFromFile(file: Path): Int {
        try {
            // Expected string in file: `PORT=127.0.0.1:{port}`
            val lines = withContext(Dispatchers.IO) {
                FileSystem.SYSTEM.read(file) {
                    readUtf8().lines()
                }
            }
            val line = lines.firstOrNull { it.contains("PORT=") }
                ?: error("No PORT line found")
            val portRegex = Regex("""PORT=.*:(\d+)""")
            val port = portRegex.find(line)?.groupValues?.get(1)?.toInt()
                ?: error("Failed to parse port from line: $line")
            log.i("Control port read from control.txt file: $port")
            // Rename the file so the observer doesn't pick up an old file
            val backup = getControlPortBackupFile()
            FileSystem.SYSTEM.atomicMove(getControlPortFile(), backup)
            return port
        } catch (error: Exception) {
            log.e(error) { "Failed to read control port from control.txt file" }
            throw error
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
            log.e("Failed to write external_tor.config: $error")
            throw error
        }
    }

    private suspend fun validateExternalTorConfig(
        configFile: Path,
        expectedSocksPort: Int,
        expectedControlPort: Int
    ) {
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


    /**
     * Deletes the Tor working directory (including cache) to allow a fresh start on next run.
     * Should be called only when Tor is fully stopped.
     */
    private fun purgeWorkingDir() {
        val fs = FileSystem.SYSTEM
        val torDir = getTorDir()
        fun deleteRecursivelyFs(path: Path) {
            // Try to list children; if fails, treat as file
            val children = runCatching { fs.list(path) }.getOrNull()
            children?.forEach { child -> deleteRecursivelyFs(child) }
            runCatching { fs.delete(path) }
        }
        runCatching {
            if (fs.exists(torDir)) {
                deleteRecursivelyFs(torDir)
                log.i { "Purged Tor working directory at $torDir" }
            } else {
                log.i { "Tor working directory not found; nothing to purge" }
            }
        }.onFailure { e ->
            log.w(e) { "Failed to purge Tor working directory" }
        }
    }

    /**
     * Stops Tor (if running), waits for its control port to become unavailable, then purges the Tor directory.
     * Safe to call even if Tor was not started in this process; we detect lingering daemons via last-known control port.
     */
    suspend fun stopAndPurgeWorkingDir(timeoutMs: Long = 7_000) {
        // Attempt graceful stop if we own a runtime
        runCatching { stopTor() }
            .onFailure { e -> log.w(e) { "stopTorSync failed; will still wait for port to close and purge" } }

        // Try to read a last-known control port (backup first, then current)
        val lastKnownPort = runCatching { readLastKnownControlPort() }.getOrNull()
        if (lastKnownPort != null) {
            waitForControlPortClosed(lastKnownPort, timeoutMs)
        }

        // Finally purge
        purgeWorkingDir()
    }

    private fun readLastKnownControlPort(): Int? {
        val fs = FileSystem.SYSTEM
        val backup = getControlPortBackupFile()
        val current = getControlPortFile()
        fun parse(file: Path): Int? {
            if (!fs.exists(file)) return null
            return runCatching {
                fs.read(file) { readUtf8() }
                    .lineSequence()
                    .firstOrNull { it.startsWith("PORT=127.0.0.1:") }
                    ?.removePrefix("PORT=127.0.0.1:")
                    ?.toInt()
            }.getOrNull()
        }
        return parse(backup) ?: parse(current)
    }

    private suspend fun waitForControlPortClosed(port: Int, timeoutMs: Long = 7_000) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        try {
            val start = Clock.System.now().toEpochMilliseconds()
            while (true) {
                val stillOpen = runCatching {
                    val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port)
                    socket.close(); true
                }.getOrElse { false }
                if (!stillOpen) {
                    log.i { "Control port $port is closed" }
                    return
                }
                if (Clock.System.now().toEpochMilliseconds() - start > timeoutMs) {
                    log.w { "Control port $port still open after ${timeoutMs}ms; continuing with purge" }
                    return
                }
                delay(200)
            }
        } finally {
            selectorManager.close()
        }
    }

    private fun cleanupService() {
        _bootstrapProgress.value = 0
        _socksPort.value = null
        torRuntime = null
    }
}
