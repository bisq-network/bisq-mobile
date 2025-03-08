package network.bisq.mobile.client.service.offers

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO

@Serializable
data class AddAccountRequest(
    val accountName: String,
    val accountData: String,
)