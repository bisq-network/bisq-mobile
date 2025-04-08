package network.bisq.mobile.client.service.chat.trade

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.CitationVO

@Serializable
data class SendChatMessageRequest(val text: String, val citation: CitationVO?)