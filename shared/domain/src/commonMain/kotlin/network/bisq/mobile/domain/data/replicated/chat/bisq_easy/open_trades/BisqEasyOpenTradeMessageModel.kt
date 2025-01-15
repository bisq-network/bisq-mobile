package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades

import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO

//todo will get completed with work on chat
class BisqEasyOpenTradeMessageModel(
    val bisqEasyOpenTradeMessage: BisqEasyOpenTradeMessageDto
) {
    //todo set initial value
    val chatMessageReactions: MutableSet<BisqEasyOpenTradeMessageReactionVO> = mutableSetOf()
}
