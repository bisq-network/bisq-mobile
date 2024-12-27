package network.bisq.mobile.client.service.offer

import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.service.offer.OfferServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientOfferServiceFacade(private val apiGateway: OfferApiGateway) : OfferServiceFacade, Logging {

    override suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>
    ): Result<String> {
        val apiResult = apiGateway.publishOffer(
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec,
            supportedLanguageCodes
        )
        if (apiResult.isSuccess) {
            return Result.success(apiResult.getOrThrow().offerId)
        } else {
            throw apiResult.exceptionOrNull()!!
        }
    }
}