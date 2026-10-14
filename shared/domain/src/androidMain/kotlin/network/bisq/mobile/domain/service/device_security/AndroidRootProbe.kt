package network.bisq.mobile.domain.service.device_security

import java.io.File

/**
 * Root heuristics for Android: an `su` binary at a known location, or the data directory of a root
 * manager (Magisk, KernelSU). Any one positive signal counts.
 *
 * Kept narrow on purpose, because a false positive is worse than a false negative:
 * - `Build.TAGS` containing `test-keys` is not used: unrooted custom ROMs are signed with test keys.
 * - `which su` is not used: it starts a process and finds nothing the fixed locations miss.
 * - `su` must be a non-empty regular file; a directory or an empty placeholder is not a binary.
 * - A location that cannot be read counts as absent.
 *
 * The emulator's "Google APIs" images ship `/system/xbin/su`, but `/system/xbin` is `0750 root:shell`
 * so apps cannot see it and the emulator reports clean. To test the warning, boot the AVD with
 * `-writable-system`, then: `adb root`, `adb disable-verity`, `adb reboot`, `adb root`, `adb remount`,
 * `adb shell 'echo fake > /system/bin/su'`.
 */
class AndroidRootProbe(
    private val fileSystem: FileSystem = RealFileSystem(),
) : DeviceIntegrityProbe {
    interface FileSystem {
        fun isNonEmptyFile(path: String): Boolean

        fun exists(path: String): Boolean
    }

    /** [rootDir] stands in for the device root in tests. */
    class RealFileSystem(
        private val rootDir: String = "",
    ) : FileSystem {
        override fun isNonEmptyFile(path: String): Boolean = File(rootDir + path).let { it.isFile && it.length() > 0 }

        override fun exists(path: String): Boolean = File(rootDir + path).exists()
    }

    override fun detect(): DeviceIntegrity {
        val rooted =
            SU_PATHS.any { path -> safely { fileSystem.isNonEmptyFile(path) } } ||
                ROOT_MANAGER_MARKERS.any { path -> safely { fileSystem.exists(path) } }
        return if (rooted) DeviceIntegrity.ROOTED else DeviceIntegrity.CLEAN
    }

    private inline fun safely(check: () -> Boolean): Boolean =
        try {
            check()
        } catch (_: SecurityException) {
            false
        }

    companion object {
        val SU_PATHS =
            listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
            )

        val ROOT_MANAGER_MARKERS =
            listOf(
                "/sbin/.magisk",
                "/data/adb/magisk",
                "/data/adb/ksu",
            )
    }
}

actual fun createDeviceIntegrityProbe(): DeviceIntegrityProbe = AndroidRootProbe()
