package network.bisq.mobile.presentation.offer

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade

/** The node has no price for [market], so no offer amount or price can be derived for it. */
class MissingPriceQuoteException(
    market: MarketVO,
) : IllegalStateException("No market price for ${market.baseCurrencyCode}/${market.quoteCurrencyCode}")

/**
 * [priceSpec]'s quote for [market] from the node's price feed, or null when the feed has no usable
 * price for it. A zero price counts as none: no amount can be converted with it.
 */
fun MarketPriceServiceFacade.findPriceQuote(
    market: MarketVO,
    priceSpec: PriceSpecVO = MarketPriceSpecVO(),
): PriceQuoteVO? =
    findMarketPriceItem(market)
        ?.takeIf { it.priceQuote.value > 0 }
        ?.let { priceSpec.getPriceQuoteVO(it) }
