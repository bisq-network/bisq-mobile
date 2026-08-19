package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.service.chat.private_chat.TwoPartyPrivateChatChannelDto
import network.bisq.mobile.client.common.domain.service.chat.private_chat.TwoPartyPrivateChatMessageDto
import network.bisq.mobile.client.common.domain.service.chat.private_chat.TwoPartyPrivateChatMessageReactionDto
import network.bisq.mobile.client.common.domain.service.chat.trade.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.client.common.domain.service.network.NetworkInfoDto
import network.bisq.mobile.client.common.domain.service.trades.TradeItemPresentationDto
import network.bisq.mobile.client.common.domain.service.trades.TradePropertiesDto
import network.bisq.mobile.data.model.trade.ClosedTradeListItemDto
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
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
    CLOSED_TRADES(
        typeOf<List<ClosedTradeListItemDto>>(),
        TopicImportance.CRITICAL,
        "mobile.client.topic.closed_trades.title",
        "mobile.client.topic.closed_trades.desc",
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
        typeOf<List<BisqEasyOpenTradeMessageReaction>>(),
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
    NETWORK_INFO(
        typeOf<NetworkInfoDto>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.network_info.title",
        "mobile.client.topic.network_info.desc",
    ),

    // COSMETIC for the three below, like CHAT_REACTIONS: a DM outage blocks no trade and risks no
    // funds, so "Affects core features" in SubscriptionsFailedDialog would rank them above the trade
    // chat they are not. TRADE_CHAT_MESSAGES stays CRITICAL because a payment is coordinated there.
    //
    // Neither private-chat gate can land a topic in that list to begin with: a node that does not
    // advertise the capability is never subscribed to at all (ClientPrivateChatServiceFacade.activate
    // waits for it), and a pairing that was never granted PRIVATE_CHAT_CHANNELS subscribes just fine
    // — bisq2 authorises its REST routes, never its topics. So these fail only on real transport
    // trouble, which is what the grouping has to describe.
    PRIVATE_CHAT_CHANNELS(
        typeOf<List<TwoPartyPrivateChatChannelDto>>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.private_chat_channels.title",
        "mobile.client.topic.private_chat_channels.desc",
    ),
    PRIVATE_CHAT_MESSAGES(
        typeOf<List<TwoPartyPrivateChatMessageDto>>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.private_chat_messages.title",
        "mobile.client.topic.private_chat_messages.desc",
    ),
    PRIVATE_CHAT_REACTIONS(
        typeOf<List<TwoPartyPrivateChatMessageReactionDto>>(),
        TopicImportance.COSMETIC,
        "mobile.client.topic.private_chat_reactions.title",
        "mobile.client.topic.private_chat_reactions.desc",
    ),
    ;

    fun i18n(): String = titleKey.i18n()
}

@Serializable
enum class TopicImportance { CRITICAL, COSMETIC }
