package network.bisq.mobile.android.node.service

import android.app.ActivityManager
import android.content.Context
import bisq.common.platform.MemoryReportService
import network.bisq.mobile.utils.Logging
import java.util.concurrent.CompletableFuture

/**
 * Memory report for bisq jars calculations
 */
class AndroidMemoryReportService(private val context: Context) : MemoryReportService, Logging {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun logReport() {
        val usedMemory = usedMemoryInMB
        val freeMemory = freeMemoryInMB
        val totalMemory = totalMemoryInMB
        log.i("Memory Report - Used: ${usedMemory}MB, Free: ${freeMemory}MB, Total: ${totalMemory}MB")
    }

    override fun getUsedMemoryInBytes(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return memoryInfo.totalMem - memoryInfo.availMem
    }

    override fun getUsedMemoryInMB(): Long {
        return bytesToMegabytes(getUsedMemoryInBytes())
    }

    override fun getFreeMemoryInMB(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return bytesToMegabytes(memoryInfo.availMem)
    }

    override fun getTotalMemoryInMB(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return bytesToMegabytes(memoryInfo.totalMem)
    }

    override fun initialize(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            // Initialization logic here if needed
            true
        }
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            // Shutdown logic here if needed
            true
        }
    }

    private fun bytesToMegabytes(bytes: Long): Long {
        return bytes / (1024 * 1024)
    }
}
