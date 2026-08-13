package network.bisq.mobile.node.common.domain.utils

import android.app.ActivityManager
import android.os.Debug
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import network.bisq.mobile.node.common.test_utils.TestApplication
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class AndroidMemoryReportServiceTest {
    private lateinit var memoryReportService: AndroidMemoryReportService
    private val activityManager = mockk<ActivityManager>(relaxed = true)
    private val deviceMemInfo = ActivityManager.MemoryInfo()
    private val appMemInfo = mockk<Debug.MemoryInfo>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Debug::class)
        every { Debug.getMemoryInfo(any()) } returns Unit
        every { appMemInfo.totalPss } returns 512 * 1024 // 512 MB in KB
        every { Debug.getNativeHeapAllocatedSize() } returns 64L * 1024L * 1024L
        every { Debug.getNativeHeapFreeSize() } returns 32L * 1024L * 1024L
        every { activityManager.getMemoryInfo(any()) } answers {
            firstArg<ActivityManager.MemoryInfo>().apply {
                totalMem = 1024L * 1024L * 1024L // 1 GB
                availMem = 512L * 1024L * 1024L // 512 MB free
            }
        }
        memoryReportService =
            AndroidMemoryReportService(
                activityManager,
                deviceMemInfo,
                appMemInfo,
                Runtime.getRuntime(),
                true,
            )
    }

    @After
    fun tearDown() {
        memoryReportService.shutdown()
        unmockkStatic(Debug::class)
    }

    @Test
    fun `logReport should log memory usage without crashing`() {
        memoryReportService.logReport()

        verify { activityManager.getMemoryInfo(any()) }
        verify { Debug.getMemoryInfo(any()) }
    }

    @Test
    fun `getUsedMemoryInBytes should return correct used memory`() {
        val usedMemory = memoryReportService.getUsedMemoryInBytes()

        assertEquals(512L * 1024L * 1024L, usedMemory) // 512 MB used in bytes
    }

    @Test
    fun `getUsedMemoryInMB should return correct used memory in MB`() {
        val usedMemoryMB = memoryReportService.getUsedMemoryInMB()

        assertEquals(512L, usedMemoryMB) // 512 MB used
    }

    @Test
    fun `getFreeMemoryInMB should return correct free memory in MB`() {
        val freeMemoryMB = memoryReportService.getFreeMemoryInMB()

        assertEquals(512L, freeMemoryMB) // 512 MB free
    }

    @Test
    fun `getTotalMemoryInMB should return correct total memory in MB`() {
        val totalMemoryMB = memoryReportService.getTotalMemoryInMB()

        assertEquals(1024L, totalMemoryMB) // 1024 MB total
    }

    @Test
    fun `initialize should complete and return true`() {
        val future = memoryReportService.initialize()
        assertNotNull(future)
        assertTrue(future.get()) // Should return true on completion
    }

    @Test
    fun `shutdown should complete and return true`() {
        val future = memoryReportService.shutdown()
        assertNotNull(future)
        assertTrue(future.get()) // Should return true on completion
    }
}
