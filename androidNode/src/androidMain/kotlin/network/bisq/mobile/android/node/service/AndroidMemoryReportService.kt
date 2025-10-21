package network.bisq.mobile.android.node.service

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import bisq.common.platform.MemoryReportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import java.util.concurrent.CompletableFuture
import kotlin.math.max

class AndroidMemoryReportService(context: Context) : MemoryReportService, Logging {
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private var deviceMemInfo = ActivityManager.MemoryInfo()
    private val appMemInfo = Debug.MemoryInfo()
    private val runtime = Runtime.getRuntime()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var peakTotalPssMB = -1L
    private fun bytesToMb(bytes: Long) = bytes / 1024 / 1024

    override fun initialize(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            scope.launch {
                while (isActive) {
                    logReport()
                    delay(10_000)
                }
            }
            true
        }
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            scope.cancel()
            // Shutdown logic here if needed
            true
        }
    }

    override fun logReport() {
        runCatching {
            val deviceTotalMB = getTotalMemoryInMB()
            val deviceAvailMB = getFreeMemoryInMB()
            val deviceUsedMB = deviceTotalMB - deviceAvailMB
            val deviceUsedPct = if (deviceTotalMB > 0) (deviceUsedMB.toDouble() * 100.0 / deviceTotalMB) else 0.0

            val totalPssMB = bytesToMb(getUsedMemoryInBytes())
            peakTotalPssMB = max(peakTotalPssMB, totalPssMB)
            val javaUsedMB = bytesToMb(runtime.totalMemory() - runtime.freeMemory())
            val javaMaxMB = bytesToMb(runtime.maxMemory())
            val nativeUsedMB = bytesToMb(Debug.getNativeHeapAllocatedSize())
            val nativeFreeMB = bytesToMb(Debug.getNativeHeapFreeSize())

            log.i {
                "Device memory: Used=$deviceUsedMB MB (${"%.1f".format(deviceUsedPct)}%), Available=$deviceAvailMB MB, Total=$deviceTotalMB MB\n" +
                        "App memory: PSS=$totalPssMB MB (peak=$peakTotalPssMB MB); Java heap: Used=$javaUsedMB MB, Max=$javaMaxMB MB; Native heap: Used=$nativeUsedMB MB, Free=$nativeFreeMB MB"
            }
        }.onFailure { exception ->
            log.e(exception) { "Failed to log memory report" }
        }
    }

    override fun getUsedMemoryInBytes(): Long {
        // Approximate app memory via PSS (KB → bytes)
        Debug.getMemoryInfo(appMemInfo)
        return appMemInfo.totalPss.toLong() * 1024L
    }

    override fun getUsedMemoryInMB(): Long {
        return bytesToMb(getUsedMemoryInBytes())
    }

    override fun getFreeMemoryInMB(): Long {
        activityManager.getMemoryInfo(deviceMemInfo)
        return bytesToMb(deviceMemInfo.availMem)
    }

    override fun getTotalMemoryInMB(): Long {
        activityManager.getMemoryInfo(deviceMemInfo)
        return bytesToMb(deviceMemInfo.totalMem)
    }
}
