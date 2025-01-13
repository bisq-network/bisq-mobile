package network.bisq.mobile.domain.data.replicated.trade.bisq_easy

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO

@Serializable
data class BisqEasyTradePartyVO(val networkId: NetworkIdVO)