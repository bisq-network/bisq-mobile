package network.bisq.mobile.client.websocket.subscription

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.service.trades.TradePropertiesDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationDto
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
enum class Topic(val typeOf: KType) {
    MARKET_PRICE(typeOf<Map<String, PriceQuoteVO>>()),
    NUM_OFFERS(typeOf<Map<String, Int>>()),
    OFFERS(typeOf<List<OfferItemPresentationDto>>()),
    TRADES(typeOf<List<TradeItemPresentationDto>>()),
    TRADE_PROPERTIES(typeOf<List<Map<String, TradePropertiesDto>>>()),
    TRADE_CHAT_MESSAGES(typeOf<List<BisqEasyOpenTradeMessageDto>>()),
    CHAT_REACTIONS(typeOf<List<BisqEasyOpenTradeMessageReactionVO>>()),
}