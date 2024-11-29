package network.bisq.mobile.client.websocket.subscription

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.model.OfferListItem
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
enum class Topic(val typeOf: KType) {
    MARKET_PRICE(typeOf<Map<String, Long>>()),
    NUM_OFFERS(typeOf<Map<String, Int>>()),
    OFFERS(typeOf<List<OfferListItem>>())
}