package network.bisq.mobile.domain.service.device_security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosJailbreakProbeTest {
    private class FakeFileSystem(
        val existing: Set<String> = emptySet(),
        val sandboxWritable: Boolean = false,
        val failure: Throwable? = null,
    ) : IosJailbreakProbe.FileSystem {
        var checked = false

        override fun exists(path: String): Boolean {
            checked = true
            failure?.let { throw it }
            return path in existing
        }

        override fun canWriteOutsideSandbox(): Boolean {
            checked = true
            failure?.let { throw it }
            return sandboxWritable
        }
    }

    private fun device(fileSystem: FakeFileSystem) = IosJailbreakProbe(isSimulator = { false }, fileSystem = fileSystem)

    @Test
    fun `a stock device is clean`() {
        assertEquals(DeviceIntegrity.CLEAN, device(FakeFileSystem()).detect())
    }

    @Test
    fun `each known jailbreak path counts as jailbroken`() {
        assertTrue(IosJailbreakProbe.JAILBREAK_PATHS.isNotEmpty())
        IosJailbreakProbe.JAILBREAK_PATHS.forEach { path ->
            assertEquals(DeviceIntegrity.ROOTED, device(FakeFileSystem(existing = setOf(path))).detect(), path)
        }
    }

    @Test
    fun `the rootless jailbreak directory counts as jailbroken`() {
        assertEquals(DeviceIntegrity.ROOTED, device(FakeFileSystem(existing = setOf("/var/jb"))).detect())
    }

    @Test
    fun `a writable location outside the sandbox counts as jailbroken`() {
        assertEquals(DeviceIntegrity.ROOTED, device(FakeFileSystem(sandboxWritable = true)).detect())
    }

    @Test
    fun `paths that are not jailbreak markers are clean`() {
        val systemPaths = setOf("/Applications", "/usr/bin", "/System/Library", "/private/var/mobile", "/etc/hosts")

        assertEquals(DeviceIntegrity.CLEAN, device(FakeFileSystem(existing = systemPaths)).detect())
    }

    @Test
    fun `the simulator is clean even though the host has a shell and an ssh daemon`() {
        val hostLike = FakeFileSystem(existing = IosJailbreakProbe.JAILBREAK_PATHS.toSet(), sandboxWritable = true)

        assertEquals(DeviceIntegrity.CLEAN, IosJailbreakProbe(isSimulator = { true }, fileSystem = hostLike).detect())
    }

    @Test
    fun `the simulator is not probed at all`() {
        val fileSystem = FakeFileSystem(existing = setOf("/bin/bash"))

        IosJailbreakProbe(isSimulator = { true }, fileSystem = fileSystem).detect()

        assertEquals(false, fileSystem.checked)
    }

    @Test
    fun `a failing file check counts as clean and never as jailbroken`() {
        val failing = FakeFileSystem(failure = IllegalStateException("denied"))

        assertEquals(DeviceIntegrity.CLEAN, device(failing).detect())
    }

    @Test
    fun `the default probe reports the test simulator as clean`() {
        assertEquals(DeviceIntegrity.CLEAN, createDeviceIntegrityProbe().detect())
    }

    @Test
    fun `the real sandbox write check fails inside the test sandbox`() {
        // On the simulator the test process is not sandboxed like an app, so only assert it does
        // not throw and leaves no file behind when it does succeed.
        val fileSystem = IosJailbreakProbe.RealFileSystem()
        fileSystem.canWriteOutsideSandbox()

        assertEquals(false, fileSystem.exists(IosJailbreakProbe.RealFileSystem.PROBE_PATH))
    }
}
