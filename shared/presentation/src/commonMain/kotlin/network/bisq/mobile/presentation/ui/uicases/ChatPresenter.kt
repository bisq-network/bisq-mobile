package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage

interface IChatPresenter {
    val messages: StateFlow<List<ChatMessage>>

    fun addMessage(msg: ChatMessage)

    fun addReactions(reactionType: String, message: ChatMessage)
}

private val initialMessages = listOf(
    ChatMessage(
        "10",
        "Hal Finney",
        "It’s a game-changer, Satoshi. A free market built on Bitcoin’s principles. Let’s hope someone builds it.",
        "8:25 PM",
        "", null
    ),
    ChatMessage(
        "20",
        "Satoshi Nakamoto",
        "Nodes could broadcast offers, like Bitcoin relays transactions. Decentralized and resilient.",
        "8:20 PM",
        "", null
    ),
    ChatMessage(
        "25",
        "SYSTEM",
        "Someone entering the chat, with a very very long system message",
        "8:16 PM",
        "", null
    ),
    ChatMessage(
        "30",
        "Hal Finney",
        "Interesting. What about matching buyers and sellers without a central order book?",
        "8:15 PM",
        "", null
    ),
    ChatMessage(
        "40",
        "Satoshi Nakamoto",
        "Multi-signature transactions. Both parties sign off on trades.",
        "8:10 PM",
        "", null
    ),
    ChatMessage(
        "50",
        "Hal Finney",
        "A decentralized trading platform? That could be huge. But how do we handle trust?",
        "8:05 PM",
        "", null
    ),
    ChatMessage(
        "60",
        "Satoshi Nakamoto",
        "Hal, imagine trading assets directly without intermediaries—peer-to-peer, like Bitcoin.",
        "8:00 PM",
        "", null
    )
)

class ChatPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter), IChatPresenter {
    private val _messages = MutableStateFlow(initialMessages)
    override val messages = _messages

    override fun addMessage(msg: ChatMessage) {
        val updatedMessages = listOf(msg) + _messages.value
        _messages.value = updatedMessages
    }

    override fun addReactions(reactionType: String, message: ChatMessage) {
        val updatedMessages = _messages.value.map {
            if (it.messageID == message.messageID) {
                it.copy(reaction = reactionType)
            } else {
                it
            }
        }
        _messages.value = updatedMessages
    }
}