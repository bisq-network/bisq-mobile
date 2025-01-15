package network.bisq.mobile.client.service.trades

import kotlinx.serialization.Serializable

@Serializable
data class TradeEventVO(val tradeEventType: TradeEventTypeEnum, val data: String? = null)