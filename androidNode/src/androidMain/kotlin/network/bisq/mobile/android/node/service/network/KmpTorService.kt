package network.bisq.mobile.android.node.service.network

import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString


/**
 * We use the external tor setup of Bisq 2 and use the kmp-tor runtime.
 * The task of that class is to start the kmp tor runtime and configure the data for the external tor setup.
 *
 * 1. Setup kmp tor runtime: Create runtime, set environment, config and add observers.
 * 2. Start the kmp tor runtime
 * 3. Find socksPort by listening on TorEvent.NOTICE with data: `Socks listener listening on port {socksPort}.`
 * 4. Find control port by observing creation or mutation of the `control.txt` file which contains the control port.
 *    Read the value from format: `PORT=127.0.0.1:{controlPort}`
 * 5. Write the `external_tor.config` with socks port, control port and path to the auth cookie. This will be used by the Bisq 2 tor lib.
 * 6. After tor daemon is started, we are completed. The network service can now be initialized.
 *    The bisq 2 tor lib will detect the external tor and use that.
 *
 */
class KmpTorService : ServiceFacade(), Logging {

    private lateinit var baseDir: Path
    private var torRuntime: TorRuntime? = null
    private var deferredSocksPort = CompletableDeferred<Int>()
    private var torDaemonStarted = CompletableDeferred<Boolean>()
    private var controlPortFileObserverJob: Job? = null

    private val _startupFailure: MutableStateFlow<KmpTorException?> = MutableStateFlow(null)
    val startupFailure: StateFlow<KmpTorException?> = _startupFailure.asStateFlow()

    fun startTor(baseDir: Path): CompletableDeferred<Boolean> {
        this.baseDir = baseDir
        log.i("Start kmp-tor")
        val torStartupCompleted = CompletableDeferred<Boolean>()

        require(torRuntime == null) { "torRuntime is expected to be null at startTor" }
        setupTorRuntime()

        val configCompleted = configTor()

        torRuntime!!.enqueue(
            Action.StartDaemon,
            { error ->
                resetAndDispose()
                handleError("Starting tor daemon failed: $error")
            },
            {
                log.i("Tor daemon started")
                torDaemonStarted.takeIf { !it.isCompleted }?.complete(true)

                launchIO {
                    configCompleted.await()
                    log.i("kmp-tor startup completed")
                    torStartupCompleted.takeIf { !it.isCompleted }?.complete(true)
                }
            }
        )
        return torStartupCompleted
    }

    fun stopTor() {
        if (torRuntime == null) {
            log.w("Tor runtime is already null, skipping stop")
            return
        }

        if (!torDaemonStarted.isCompleted || !torDaemonStarted.isActive) {
            log.i("Tor daemon is still starting, waiting for it to complete before stopping")
            return
        }

        torRuntime!!.enqueue(
            Action.StopDaemon,
            { error ->
                resetAndDispose()
                handleError("Failed to stop Tor daemon: $error")
                torRuntime = null
            },
            {
                log.i { "Tor daemon stopped" }
                resetAndDispose()
                torRuntime = null
            }
        )
    }

    fun restartTor() {
        require(torRuntime != null) { "torRuntime is null. setupTor must be called before stopTor" }
        resetAndDispose()

        val configCompleted = configTor()

        torRuntime!!.enqueue(
            Action.RestartDaemon,
            { error ->
                resetAndDispose()
                handleError("Restarting tor daemon failed: $error")
            },
            {
                log.i { "Tor daemon restarted" }
                torDaemonStarted.takeIf { !it.isCompleted }?.complete(true)

                launchIO {
                    configCompleted.await()
                }
            }
        )
    }

    private fun setupTorRuntime() {
        val torDir = getTorDir()
        val cacheDirectory = getTorCacheDir()
        val controlPortFile = getControlPortFile()
        val environment = TorRuntime.Environment.Builder(
            workDirectory = torDir,
            cacheDirectory = cacheDirectory,
            loader = ResourceLoaderTorExec::getOrCreate
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
                TorOption.ControlPortWriteToFile.configure(controlPortFile)
                TorOption.CookieAuthentication.configure(true)
                TorOption.DataDirectory.configure(torDir)
                TorOption.CacheDirectory.configure(cacheDirectory)
                TorOption.DisableNetwork.configure(true) // Bisq 2 tor lib managed the DisableNetwork state, initially it is disabled.
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
        launchIO {
            try {
                val socksPort = deferredSocksPort.await()
                val controlPort = readControlPort().await()
                disposeControlPortFileObserver()
                writeExternalTorConfig(socksPort, controlPort)
                torDaemonStarted.await()
                // _startupCompleted.value = true
                configCompleted.takeIf { !it.isCompleted }?.complete(true)
            } catch (error: Exception) {
                handleError("Configuring tor failed: $error")
            }
        }
        return configCompleted
    }

    private fun readControlPort(): Deferred<Int> {
        val deferred = CompletableDeferred<Int>()
        controlPortFileObserverJob = launchIO {
            val controlPortFile = getControlPortFile()
            log.i("Path to controlPortFile: ${controlPortFile.absolutePath}")
            try {
                // We can't use FileObserver because it misses events between event processing.
                // Tor writes the port to a swap file first, and renames it afterward.
                // The FileObserver can miss the second operation, causing a deadlock.
                // See Bisq 2 tor implementation at: bisq.network.tor.process.control_port.ControlPortFilePoller

                var lastModified = 0L
                var iterations = 0
                val delay: Long = 100
                val maxIterations = 30 * 1000 / delay // 30 seconds with 100ms delays
                val startTime = System.currentTimeMillis()
                val timeoutMs = 60_000 // 60 second timeout
                while (!deferred.isCompleted && iterations < maxIterations) {
                    iterations++
                    if (System.currentTimeMillis() - startTime > timeoutMs) {
                        deferred.completeExceptionally(
                            KmpTorException("Timeout waiting for control port file")
                        )
                        break
                    }
                    log.i("readControlPort iterations=$iterations")
                    if (controlPortFile.exists()) {
                        val currentModified = controlPortFile.lastModified()
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
                handleError("Observing file ${controlPortFile.absolutePath} failed: ${e.message}")
                deferred.completeExceptionally(e)
            }
        }
        return deferred
    }

    private fun readControlPortFile(file: File, deferred: CompletableDeferred<Int>) {
        try {
            // Expected string in file: `PORT=127.0.0.1:{port}`
            val line = file.readLines().firstOrNull { it.startsWith("PORT=127.0.0.1:") }
                ?: error("No PORT line found")
            val port = line.removePrefix("PORT=127.0.0.1:").toInt()
            deferred.takeIf { !it.isCompleted }?.complete(port)
            log.i("Control port read from control.txt file: $port")

            // We rename the file so that the file observer is not taking an old file from the last run
            getControlPortFile().renameTo(getControlPortBackupFile())
        } catch (error: Exception) {
            handleError("Failed to read control port from control.txt file: $error")
            deferred.completeExceptionally(error)
        }
    }

    private fun writeExternalTorConfig(socksPort: Int, controlPort: Int) {
        try {
            val cookieFile = File(getTorDir(), "control_auth_cookie")
            val configContent = buildString {
                appendLine("UseExternalTor 1")
                appendLine("CookieAuthentication 1")
                appendLine("CookieAuthFile ${cookieFile.absolutePath}")
                appendLine("SocksPort 127.0.0.1:$socksPort")
                appendLine("ControlPort 127.0.0.1:$controlPort")
            }

            val configFile = File(getTorDir(), "external_tor.config")
            FileOutputStream(configFile).use { fos ->
                fos.write(configContent.toByteArray())
                fos.fd.sync() // ensures data is flushed to disk
            }

            log.i { "Wrote external_tor.config to ${configFile.absolutePath}\n\n$configContent\n\n" }
        } catch (error: Exception) {
            handleError("Failed to write external_tor.config. {$error}")
        }
    }

    private fun getTorDir(): File {
        val torDirPath = Path(baseDir.absolutePathString(), "tor")
        val torDir = torDirPath.toFile()
        if (!torDir.exists()) {
            torDir.mkdirs()
        }
        return torDir
    }

    private fun getTorCacheDir(): File {
        val torDirPath = Path(baseDir.absolutePathString(), "tor")
        val torCacheDir = Path(torDirPath.absolutePathString(), "cache").toFile()
        if (!torCacheDir.exists()) {
            torCacheDir.mkdirs()
        }
        return torCacheDir
    }

    private fun getControlPortFile() = File(getTorDir(), "control-port.txt")

    private fun getControlPortBackupFile() = File(getTorDir(), "control-port-backup.txt")

    private fun handleError(messageString: String) {
        log.e(messageString)
        _startupFailure.value = KmpTorException(messageString)
    }

    private fun resetAndDispose() {
        deferredSocksPort = CompletableDeferred()
        torDaemonStarted = CompletableDeferred()
        disposeControlPortFileObserver()
        _startupFailure.value = null
    }

    private fun disposeControlPortFileObserver() {
        controlPortFileObserverJob?.cancel()
        controlPortFileObserverJob = null
    }
}
