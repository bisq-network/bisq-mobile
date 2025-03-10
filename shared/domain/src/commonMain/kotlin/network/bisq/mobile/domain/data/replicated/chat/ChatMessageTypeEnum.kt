package network.bisq.mobile.domain.data.replicated.chat

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMessageTypeEnum {
    TEXT,
    LEAVE,
    TAKE_BISQ_EASY_OFFER,
    PROTOCOL_LOG_MESSAGE,
    CHAT_RULES_WARNING
}