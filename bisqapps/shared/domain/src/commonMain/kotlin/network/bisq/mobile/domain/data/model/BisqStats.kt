
package network.bisq.mobile.domain.data.model

class BisqStats: BaseModel() {
    val offersOnline = 150

    val publishedProfiles = 1275
}

interface BisqStatsFactory {
    fun createBisqStats(): BisqStats
}

class DefaultBisqStatsFactory : BisqStatsFactory {
    override fun createBisqStats() = BisqStats()
}