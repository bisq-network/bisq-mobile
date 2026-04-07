package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.service.trades.TradePropertiesDto
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.i18n.i18n
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
enum class Topic(
    val typeOf: KType,
    val importance: TopicImportance,
    val titleKey: String,
    val descriptionKey: String,
) {
    MARKET_PRICE(
        typeOf<Map<String, PriceQuoteVO>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.market_price.title",
        "mobile.client.topic.market_price.desc",
    ),
    NUM_OFFERS(
        typeOf<Map<String, Int>>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.num_offers.title",
        "mobile.client.topic.num_offers.desc",
    ),
    NUM_USER_PROFILES(
        typeOf<Int>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.num_user_profiles.title",
        "mobile.client.topic.num_user_profiles.desc",
    ),
    OFFERS(
        typeOf<List<OfferItemPresentationDto>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.offers.title",
        "mobile.client.topic.offers.desc",
    ),
    TRADES(
        typeOf<List<TradeItemPresentationDto>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.trades.title",
        "mobile.client.topic.trades.desc",
    ),
    TRADE_PROPERTIES(
        typeOf<List<Map<String, TradePropertiesDto>>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.trade_properties.title",
        "mobile.client.topic.trade_properties.desc",
    ),
    TRADE_CHAT_MESSAGES(
        typeOf<List<BisqEasyOpenTradeMessageDto>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.trade_chat_messages.title",
        "mobile.client.topic.trade_chat_messages.desc",
    ),
    CHAT_REACTIONS(
        typeOf<List<BisqEasyOpenTradeMessageReactionVO>>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.chat_reactions.title",
        "mobile.client.topic.chat_reactions.desc",
    ),
    REPUTATION(
        typeOf<Map<String, ReputationScoreVO>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.reputation.title",
        "mobile.client.topic.reputation.desc",
    ),
    ALERT_NOTIFICATIONS(
        typeOf<List<AuthorizedAlertDataDto>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.alert_notifications.title",
        "mobile.client.topic.alert_notifications.desc",
    ),
    TRADE_RESTRICTING_ALERT(
        typeOf<AuthorizedAlertDataDto?>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.trade_restricting_alert.title",
        "mobile.client.topic.trade_restricting_alert.desc",
    ),
    ;

    fun i18n(): String = titleKey.i18n()
}

@Serializable
enum class TopicImportance { CRITICAL, COSMETIC }
