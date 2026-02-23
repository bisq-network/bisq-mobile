package network.bisq.mobile.domain.data.model

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
