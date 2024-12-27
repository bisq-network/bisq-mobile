package network.bisq.mobile.domain.service.offer

import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO

interface OfferServiceFacade : LifeCycleAware {
    suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>,
    ): Result<String>
}