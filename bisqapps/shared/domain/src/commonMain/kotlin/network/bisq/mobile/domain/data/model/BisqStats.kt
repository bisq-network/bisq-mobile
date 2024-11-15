
package network.bisq.mobile.domain.data.model

open class BisqStats: BaseModel() {
    open val offersOnline = 150

    open val publishedProfiles = 1275
}

interface BisqStatsFactory {
    fun createBisqStats(): BisqStats
}

class DefaultBisqStatsFactory : BisqStatsFactory {
    override fun createBisqStats() = BisqStats()
}