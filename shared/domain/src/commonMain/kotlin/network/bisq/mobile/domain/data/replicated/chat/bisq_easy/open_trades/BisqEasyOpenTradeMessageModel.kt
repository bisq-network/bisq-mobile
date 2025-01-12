package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO

class BisqEasyOpenTradeMessageModel(
    val bisqEasyOpenTradeMessage: BisqEasyOpenTradeMessageVO,
    val chatMessageReactions: MutableSet<BisqEasyOpenTradeMessageReactionVO>
)
