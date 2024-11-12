package network.bisq.mobile.android.node.service

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AndroidMemoryReportServiceTest {

    private lateinit var memoryReportService: AndroidMemoryReportService
    private val context = mockk<Context>(relaxed = true)
    private val activityManager = mockk<ActivityManager>(relaxed = true)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        memoryReportService = AndroidMemoryReportService(context)
    }

    @Test
    fun `logReport should log memory usage without crashing`() {
        val memoryInfo = mockk<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(any()) } answers { firstArg<ActivityManager.MemoryInfo>().apply {
            totalMem = 1024L * 1024L * 1024L  // 1 GB
            availMem = 512L * 1024L * 1024L    // 512 MB free
        }}

        memoryReportService.logReport()

        verify { activityManager.getMemoryInfo(any()) }
    }

    @Test
    fun `getUsedMemoryInBytes should return correct used memory`() {
        val memoryInfo = mockk<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(any()) } answers { firstArg<ActivityManager.MemoryInfo>().apply {
            totalMem = 1024L * 1024L * 1024L  // 1 GB
            availMem = 512L * 1024L * 1024L    // 512 MB free
        }}

        val usedMemory = memoryReportService.getUsedMemoryInBytes()

        assertEquals(512L * 1024L * 1024L, usedMemory) // 512 MB used in bytes
    }

    @Test
    fun `getUsedMemoryInMB should return correct used memory in MB`() {
        every { activityManager.getMemoryInfo(any()) } answers { firstArg<ActivityManager.MemoryInfo>().apply {
            totalMem = 1024L * 1024L * 1024L  // 1 GB
            availMem = 512L * 1024L * 1024L    // 512 MB free
        }}

        val usedMemoryMB = memoryReportService.getUsedMemoryInMB()

        assertEquals(512L, usedMemoryMB) // 512 MB used
    }

    @Test
    fun `getFreeMemoryInMB should return correct free memory in MB`() {
        every { activityManager.getMemoryInfo(any()) } answers { firstArg<ActivityManager.MemoryInfo>().apply {
            availMem = 512L * 1024L * 1024L    // 512 MB free
        }}

        val freeMemoryMB = memoryReportService.getFreeMemoryInMB()

        assertEquals(512L, freeMemoryMB) // 512 MB free
    }

    @Test
    fun `getTotalMemoryInMB should return correct total memory in MB`() {
        every { activityManager.getMemoryInfo(any()) } answers { firstArg<ActivityManager.MemoryInfo>().apply {
            totalMem = 1024L * 1024L * 1024L  // 1 GB
        }}

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