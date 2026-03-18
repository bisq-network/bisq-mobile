package network.bisq.mobile.data.replicated.trade.bisq_easy

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO

@Serializable
data class BisqEasyTradePartyVO(
    val networkId: NetworkIdVO,
)
