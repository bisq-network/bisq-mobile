package network.bisq.mobile.domain.service.device_security

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the real file checks against a temporary folder that stands in for the device root, so each
 * case is a real file layout rather than a mocked answer.
 */
class AndroidRootProbeTest {
    @get:Rule
    val deviceRoot = TemporaryFolder()

    private fun probe() = AndroidRootProbe(AndroidRootProbe.RealFileSystem(deviceRoot.root.absolutePath))

    private fun createFile(
        path: String,
        content: String = "\u007fELF binary",
    ) {
        val file = File(deviceRoot.root, path.removePrefix("/"))
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun createDirectory(path: String) {
        File(deviceRoot.root, path.removePrefix("/")).mkdirs()
    }

    @Test
    fun `a stock device layout is clean`() {
        createFile("/system/bin/sh")
        createFile("/system/bin/toybox")
        createDirectory("/system/xbin")
        createDirectory("/data/local/tmp")

        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `an empty device root is clean`() {
        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `an su binary at any known location is rooted`() {
        assertTrue(AndroidRootProbe.SU_PATHS.isNotEmpty())
        AndroidRootProbe.SU_PATHS.forEach { path ->
            deviceRoot.root.listFiles()?.forEach { it.deleteRecursively() }
            createFile(path)

            assertEquals(DeviceIntegrity.ROOTED, probe().detect(), "su at $path")
        }
    }

    @Test
    fun `each root manager marker is rooted`() {
        assertTrue(AndroidRootProbe.ROOT_MANAGER_MARKERS.isNotEmpty())
        AndroidRootProbe.ROOT_MANAGER_MARKERS.forEach { path ->
            deviceRoot.root.listFiles()?.forEach { it.deleteRecursively() }
            createDirectory(path)

            assertEquals(DeviceIntegrity.ROOTED, probe().detect(), "marker at $path")
        }
    }

    @Test
    fun `a directory named su is not an su binary`() {
        AndroidRootProbe.SU_PATHS.forEach { createDirectory(it) }

        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `an empty su placeholder file is not an su binary`() {
        createFile("/system/xbin/su", content = "")

        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `binaries with names that merely contain su are clean`() {
        createFile("/system/bin/sudo")
        createFile("/system/bin/sum")
        createFile("/system/xbin/su.bak")
        createFile("/system/xbin/busybox")

        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `su outside the known locations is not looked for`() {
        createFile("/storage/emulated/0/Download/su")

        assertEquals(DeviceIntegrity.CLEAN, probe().detect())
    }

    @Test
    fun `an unreadable location counts as absent and never as rooted`() {
        val throwing =
            object : AndroidRootProbe.FileSystem {
                override fun isNonEmptyFile(path: String): Boolean = throw SecurityException("denied")

                override fun exists(path: String): Boolean = throw SecurityException("denied")
            }

        assertEquals(DeviceIntegrity.CLEAN, AndroidRootProbe(throwing).detect())
    }

    @Test
    fun `one unreadable location does not hide an su binary at another`() {
        val fileSystem =
            object : AndroidRootProbe.FileSystem {
                override fun isNonEmptyFile(path: String): Boolean =
                    when (path) {
                        AndroidRootProbe.SU_PATHS.first() -> throw SecurityException("denied")
                        AndroidRootProbe.SU_PATHS.last() -> true
                        else -> false
                    }

                override fun exists(path: String): Boolean = false
            }

        assertEquals(DeviceIntegrity.ROOTED, AndroidRootProbe(fileSystem).detect())
    }

    @Test
    fun `the known su locations are absolute paths ending in su`() {
        AndroidRootProbe.SU_PATHS.forEach { path ->
            assertTrue(path.startsWith("/") && path.endsWith("/su"), path)
        }
    }

    @Test
    fun `the default probe reports this test host as clean`() {
        assertEquals(DeviceIntegrity.CLEAN, createDeviceIntegrityProbe().detect())
    }
}
