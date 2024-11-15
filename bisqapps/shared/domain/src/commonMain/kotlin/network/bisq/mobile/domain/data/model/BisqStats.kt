
package network.bisq.mobile.domain.data.model

open class BisqStats: BaseModel() {
    protected open val offersOnline = 150

    protected open val publishedProfiles = 1275

    fun getOffersOnline(): Int {
        return offersOnline
    }

    fun getPublishedProfiles(): Int {
        return publishedProfiles
    }
}

interface BisqStatsFactory {
    fun createBisqStats(): BisqStats
}

class DefaultBisqStatsFactory : BisqStatsFactory {
    override fun createBisqStats() = BisqStats()
}