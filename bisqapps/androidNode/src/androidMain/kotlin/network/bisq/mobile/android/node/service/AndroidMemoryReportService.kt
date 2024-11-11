package network.bisq.mobile.android.node.service

import bisq.common.platform.MemoryReportService
import java.util.concurrent.CompletableFuture

/**
 * TODO Android implementation (this is used by bisq2 jars)
 */
class AndroidMemoryReportService : MemoryReportService {
    override fun logReport() {
    }

    override fun getUsedMemoryInBytes(): Long {
        return 0
    }

    override fun getUsedMemoryInMB(): Long {
        return 0
    }

    override fun getFreeMemoryInMB(): Long {
        return 0
    }

    override fun getTotalMemoryInMB(): Long {
        return 0
    }

    override fun initialize(): CompletableFuture<Boolean>? {
        return null
    }

    override fun shutdown(): CompletableFuture<Boolean>? {
        return null
    }
}
