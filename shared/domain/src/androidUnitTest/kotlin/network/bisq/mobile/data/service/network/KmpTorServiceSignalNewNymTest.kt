package network.bisq.mobile.data.service.network

import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.ctrl.Reply
import io.matthewnelson.kmp.tor.runtime.core.ctrl.TorCmd
import io.matthewnelson.kmp.tor.runtime.core.util.executeAsync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Test
import java.nio.file.Files

class KmpTorServiceSignalNewNymTest {
    @After
    fun tearDown() {
        unmockkStatic(TOR_CMD_UTIL_JVM)
    }

    private fun createService(): KmpTorService {
        val root = Files.createTempDirectory("kmpTorSignalNewNym").toFile().apply { deleteOnExit() }
        return KmpTorService(root.absolutePath.toPath())
    }

    private fun setTorRuntime(
        service: KmpTorService,
        runtime: TorRuntime?,
    ) {
        val field =
            KmpTorService::class.java.getDeclaredField("torRuntime").apply {
                isAccessible = true
            }
        field.set(service, runtime)
    }

    @Test
    fun `signalNewNym is no-op when tor runtime is not started`() =
        runTest {
            createService().signalNewNym()
        }

    @Test
    fun `signalNewNym sends NEWNYM when tor runtime is available`() =
        runTest {
            mockkStatic(TOR_CMD_UTIL_JVM)
            val mockRuntime = mockk<TorRuntime>()
            coEvery { mockRuntime.executeAsync(TorCmd.Signal.NewNym) } returns Reply.Success.OK

            val service = createService()
            setTorRuntime(service, mockRuntime)
            service.signalNewNym()
            coVerify(exactly = 1) { mockRuntime.executeAsync(TorCmd.Signal.NewNym) }
        }

    @Test
    fun `signalNewNym swallows exceptions from tor runtime`() =
        runTest {
            mockkStatic(TOR_CMD_UTIL_JVM)
            val mockRuntime = mockk<TorRuntime>()
            coEvery { mockRuntime.executeAsync(TorCmd.Signal.NewNym) } throws RuntimeException("rate limited")

            val service = createService()
            setTorRuntime(service, mockRuntime)
            service.signalNewNym()
            coVerify(exactly = 1) { mockRuntime.executeAsync(TorCmd.Signal.NewNym) }
        }

    private companion object {
        // @file:JvmName("TorCmdUtil") on kmp-tor's executeAsync extensions
        const val TOR_CMD_UTIL_JVM = "io.matthewnelson.kmp.tor.runtime.core.util.TorCmdUtil"
    }
}
