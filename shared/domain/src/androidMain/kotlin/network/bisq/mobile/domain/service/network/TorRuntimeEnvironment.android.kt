package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import java.io.File

actual fun torRuntimeEnvironment(baseDirPath: String?): TorRuntime.Environment {
    requireNotNull(baseDirPath) { "baseDirPath must not be null" }
    val torDir = File(baseDirPath, "tor")
    val cacheDir = File(torDir, "cache")
    torDir.mkdirs()
    cacheDir.mkdirs()

    return TorRuntime.Environment.Builder(
        workDirectory = torDir,
        cacheDirectory = cacheDir,
        loader = ResourceLoaderTorExec::getOrCreate
    )
}

