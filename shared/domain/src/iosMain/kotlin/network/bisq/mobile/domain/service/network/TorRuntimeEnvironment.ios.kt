package network.bisq.mobile.domain.service.network

import io.matthewnelson.kmp.file.absoluteFile
import io.matthewnelson.kmp.file.resolve
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.tor.resource.noexec.tor.ResourceLoaderTorNoExec
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun torRuntimeEnvironment(baseDirPath: String?): TorRuntime.Environment = IosEnvironment

// Read documentation for further options
private val IosEnvironment: TorRuntime.Environment by lazy {
    // ../data/Library
    val library = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true)
        .firstOrNull()
        ?.toString()
        ?.ifBlank { null }
        ?.toFile()
        ?: "".toFile().absoluteFile.resolve("Library")

    // ../data/Library/Caches
    val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        .firstOrNull()
        ?.toString()
        ?.ifBlank { null }
        ?.toFile()
        ?: library.resolve("Caches")

    TorRuntime.Environment.Builder(
        workDirectory = library.resolve("kmptor"),
        cacheDirectory = caches.resolve("kmptor"),
        loader = ResourceLoaderTorNoExec::getOrCreate,
    )
}
