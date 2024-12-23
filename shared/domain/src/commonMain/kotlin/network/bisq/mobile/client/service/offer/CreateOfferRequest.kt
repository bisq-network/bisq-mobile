package network.bisq.mobile.client.service.offer

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO

@Serializable
data class CreateOfferRequest(
    val direction: DirectionEnum,
    val market: MarketVO,
    val bitcoinPaymentMethods: Set<String>,
    val fiatPaymentMethods: Set<String>,
    val amountSpec: AmountSpecVO,
    val priceSpec: PriceSpecVO,
    val supportedLanguageCodes: Set<String>
)