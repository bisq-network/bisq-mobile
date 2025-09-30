package network.bisq.mobile.android.node.utils

import java.io.File
import java.io.PrintWriter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

object MemoryProfiler {
    private val runtime = Runtime.getRuntime()
    private val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private var writer: PrintWriter? = null
    private var timer: Timer? = null

    @Synchronized
    fun start(outputFile: File, interval: Long) {

        stop()

        writer = PrintWriter(outputFile).apply {
            println("time,usedMB,totalMB,freeMB")
            flush()
        }

        timer = fixedRateTimer("memory-profiler", initialDelay = 0, period = interval) {
            val total = runtime.totalMemory() / (1024 * 1024)
            val free = runtime.freeMemory() / (1024 * 1024)
            val used = total - free
            val time = LocalTime.now().format(dateFormat)

            writer?.let {
                try {
                    it.println("$time,$used,$total,$free")
                    it.flush()
                } catch (e: Exception) {
                    cancel()
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        timer?.cancel()
        timer = null
        writer?.close()
        writer = null
    }
}