package network.bisq.mobile.data.model.market

import kotlinx.serialization.Serializable

@Serializable
enum class MarketSortBy {
    MostOffers,
    NameAZ,
    NameZA,
}

@Serializable
enum class MarketFilter {
    WithOffers,
    All,
}
