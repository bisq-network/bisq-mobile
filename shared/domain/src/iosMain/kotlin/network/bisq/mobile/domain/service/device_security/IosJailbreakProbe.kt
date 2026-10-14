package network.bisq.mobile.domain.service.device_security

import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.data.utils.provideIsSimulator
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo

/**
 * Jailbreak heuristics for iOS: a known jailbreak app, tweak loader, shell or package manager path,
 * the rootless jailbreak directory, or a successful write outside the app sandbox. Any one positive
 * signal counts.
 *
 * The simulator is never probed: it runs on the macOS file system, where `/bin/bash` and
 * `/usr/sbin/sshd` exist and the sandbox is not enforced, so every check would be positive. A check
 * that fails counts as clean.
 */
class IosJailbreakProbe(
    private val isSimulator: () -> Boolean = ::isRunningOnSimulator,
    private val fileSystem: FileSystem = RealFileSystem(),
) : DeviceIntegrityProbe {
    interface FileSystem {
        fun exists(path: String): Boolean

        fun canWriteOutsideSandbox(): Boolean
    }

    class RealFileSystem : FileSystem {
        override fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

        @OptIn(ExperimentalForeignApi::class)
        override fun canWriteOutsideSandbox(): Boolean {
            val fileManager = NSFileManager.defaultManager
            val written = fileManager.createFileAtPath(PROBE_PATH, contents = null, attributes = null)
            if (written) {
                fileManager.removeItemAtPath(PROBE_PATH, error = null)
            }
            return written
        }

        companion object {
            const val PROBE_PATH = "/private/bisq_sandbox_probe"
        }
    }

    override fun detect(): DeviceIntegrity {
        if (isSimulator()) return DeviceIntegrity.CLEAN
        val jailbroken =
            JAILBREAK_PATHS.any { path -> safely { fileSystem.exists(path) } } ||
                safely { fileSystem.canWriteOutsideSandbox() }
        return if (jailbroken) DeviceIntegrity.ROOTED else DeviceIntegrity.CLEAN
    }

    private inline fun safely(check: () -> Boolean): Boolean =
        try {
            check()
        } catch (_: Exception) {
            false
        }

    companion object {
        val JAILBREAK_PATHS =
            listOf(
                "/Applications/Cydia.app",
                "/Applications/Sileo.app",
                "/Applications/Zebra.app",
                "/Library/MobileSubstrate/MobileSubstrate.dylib",
                "/usr/sbin/sshd",
                "/bin/bash",
                "/etc/apt",
                "/private/var/lib/apt",
                // Rootless jailbreaks (palera1n, Dopamine) install everything under this directory
                "/var/jb",
            )
    }
}

private fun isRunningOnSimulator(): Boolean = provideIsSimulator() || NSProcessInfo.processInfo.environment["SIMULATOR_UDID"] != null

actual fun createDeviceIntegrityProbe(): DeviceIntegrityProbe = IosJailbreakProbe()
