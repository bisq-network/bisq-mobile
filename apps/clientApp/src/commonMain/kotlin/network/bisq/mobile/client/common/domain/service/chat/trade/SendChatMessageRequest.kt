package network.bisq.mobile.client.common.domain.service.chat.trade

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.chat.CitationVO

@Serializable
data class SendChatMessageRequest(
    val text: String,
    val citation: CitationVO?,
)
