package network.bisq.mobile.domain.data.replicated.chat

import kotlinx.serialization.Serializable

@Serializable
enum class ChatChannelDomainEnum {
    BISQ_EASY_OFFERBOOK,
    BISQ_EASY_OPEN_TRADES,
    DISCUSSION,
    SUPPORT
}