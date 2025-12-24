package network.bisq.mobile.node.common.domain.utils

import android.app.ActivityManager
import android.os.Debug
import bisq.common.platform.MemoryReportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import java.util.concurrent.CompletableFuture
import kotlin.math.max

class AndroidMemoryReportService(
    private val activityManager: ActivityManager,
    private val deviceMemInfo: ActivityManager.MemoryInfo,
    private val appMemInfo: Debug.MemoryInfo,
    private val runtime: Runtime,
    val isDebugBuild: Boolean,
) : MemoryReportService,
    Logging {
    private var reportJob: Job? = null
    private val intervalMs: Long = if (isDebugBuild) 10_000L else 60_000L
    private var lastTotalPssMB: Long? = null
    private var lastDeviceUsedPct: Double? = null
    private val memLock = Any()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var peakTotalPssMB = -1L

    private fun bytesToMb(bytes: Long) = bytes / 1024 / 1024

    override fun initialize(): CompletableFuture<Boolean> {
        // Start/Restart periodic reporting tied to scope
        reportJob?.cancel()
        reportJob =
            scope.launch {
                while (isActive) {
                    if (isDebugBuild) {
                        logReport()
                    } else {
                        logConditionalReport()
                    }
                    delay(intervalMs)
                }
            }
        return CompletableFuture.completedFuture(true)
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        reportJob?.cancel()
        scope.cancel()
        return CompletableFuture.completedFuture(true)
    }

    private fun logConditionalReport() {
        runCatching {
            val deviceTotalMB = getTotalMemoryInMB()
            val deviceAvailMB = getFreeMemoryInMB()
            val deviceUsedMB = deviceTotalMB - deviceAvailMB
            val deviceUsedPct = if (deviceTotalMB > 0) (deviceUsedMB.toDouble() * 100.0 / deviceTotalMB) else 0.0

            val totalPssMB = bytesToMb(getUsedMemoryInBytes())
            peakTotalPssMB = max(peakTotalPssMB, totalPssMB)

            val prevPss = lastTotalPssMB
            val prevPct = lastDeviceUsedPct

            val growthPct = if (prevPss != null && prevPss > 0) (totalPssMB - prevPss).toDouble() / prevPss else 0.0
            val crossed70 = deviceUsedPct >= 70.0 && (prevPct == null || prevPct < 70.0)
            val crossed80 = deviceUsedPct >= 80.0 && (prevPct == null || prevPct < 80.0)
            val crossed90 = deviceUsedPct >= 90.0 && (prevPct == null || prevPct < 90.0)
            val significantGrowth = growthPct >= 0.15

            if (crossed90) {
                log.e { "Memory: HIGH usage ${"%.1f".format(deviceUsedPct)}% (deviceUsed=${deviceUsedMB}MB/${deviceTotalMB}MB, appPss=${totalPssMB}MB, peak=${peakTotalPssMB}MB)" }
            } else if (crossed80 || crossed70 || significantGrowth) {
                log.w { "Memory: usage ${"%.1f".format(deviceUsedPct)}% (deviceUsed=${deviceUsedMB}MB/${deviceTotalMB}MB) appPss=${totalPssMB}MB (Δ=${"%.0f".format(growthPct * 100)}%), peak=${peakTotalPssMB}MB" }
            }

            lastTotalPssMB = totalPssMB
            lastDeviceUsedPct = deviceUsedPct
        }.onFailure { exception ->
            log.e(exception) { "Memory: Failed to log conditional memory report" }
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
                "Memory: Device Used=$deviceUsedMB MB (${"%.1f".format(deviceUsedPct)}%), Avail=$deviceAvailMB MB, Total=$deviceTotalMB MB; " +
                    "App PSS=$totalPssMB MB (peak=$peakTotalPssMB MB); Java heap Used=$javaUsedMB MB Max=$javaMaxMB MB; Native heap Used=$nativeUsedMB MB Free=$nativeFreeMB MB"
            }
        }.onFailure { exception ->
            log.e(exception) { "Failed to log memory report" }
        }
    }

    override fun getUsedMemoryInBytes(): Long {
        synchronized(memLock) {
            // Approximate app memory via PSS (KB → bytes)
            Debug.getMemoryInfo(appMemInfo)
            return appMemInfo.totalPss.toLong() * 1024L
        }
    }

    override fun getUsedMemoryInMB(): Long = bytesToMb(getUsedMemoryInBytes())

    override fun getFreeMemoryInMB(): Long {
        synchronized(memLock) {
            activityManager.getMemoryInfo(deviceMemInfo)
            return bytesToMb(deviceMemInfo.availMem)
        }
    }

    override fun getTotalMemoryInMB(): Long {
        synchronized(memLock) {
            activityManager.getMemoryInfo(deviceMemInfo)
            return bytesToMb(deviceMemInfo.totalMem)
        }
    }
}
