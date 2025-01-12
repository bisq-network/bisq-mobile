package network.bisq.mobile.client.websocket.subscription

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.service.trades.TradeProperties
import network.bisq.mobile.domain.data.presentation.offerbook.OfferItemPresentationVO
import network.bisq.mobile.domain.data.presentation.open_trades.TradeItemPresentationVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
enum class Topic(val typeOf: KType) {
    MARKET_PRICE(typeOf<Map<String, PriceQuoteVO>>()),
    NUM_OFFERS(typeOf<Map<String, Int>>()),
    OFFERS(typeOf<List<OfferItemPresentationVO>>()),
    TRADES(typeOf<List<TradeItemPresentationVO>>()),
    TRADE_PROPERTIES(typeOf<List<Map<String, TradeProperties>>>()),
}