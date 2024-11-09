package bisq.android.impl

import bisq.common.platform.MemoryReportService
import java.util.concurrent.CompletableFuture

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
