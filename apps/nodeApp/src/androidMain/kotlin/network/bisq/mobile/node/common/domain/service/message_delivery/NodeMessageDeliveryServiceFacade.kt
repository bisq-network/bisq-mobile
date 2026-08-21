package network.bisq.mobile.node.common.domain.service.message_delivery

import bisq.chat.ChatMessageType
import bisq.common.observable.Pin
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus
import network.bisq.mobile.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage as Bisq2BisqEasyOpenTradeMessage

class NodeMessageDeliveryServiceFacade(
    private val applicationService: AndroidApplicationService.Provider,
) : MessageDeliveryServiceFacade() {
    // Dependencies
    private val bisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val networkService by lazy { applicationService.networkService.get() }
    private val userIdentityService by lazy { applicationService.userService.get().userIdentityService }

    private val statusPins: MutableMap<String, Pin> = mutableMapOf()
    private val deliveryStatusMapPins: MutableMap<String, Pin> = mutableMapOf()

    override suspend fun activate() {
        super.activate()
    }

    override suspend fun deactivate() {
        super.deactivate()

        statusPins.values.forEach { it.unbind() }
        statusPins.clear()
        deliveryStatusMapPins.values.forEach { it.unbind() }
        deliveryStatusMapPins.clear()
    }

    override fun onResendMessage(messageId: String) {
        networkService.resendMessageService.ifPresent { service -> service.manuallyResendMessage(messageId) }
    }

    /**
     * Trade chat messages only, by design.
     *
     * This is `ChatMessageListItem.addSubscriptionToMessageDeliveryStatus` from Bisq 2 desktop, plus one
     * narrowing desktop does not need: desktop already holds the message, this has to find it, and it
     * looks only in the trade channels. A two-party (DM) message id therefore misses and lands in the
     * warning below.
     *
     * Widening the lookup would not give DMs a delivery state. Desktop registers the observer for a DM
     * and still shows nothing, because `updateMessageStatus` returns early unless a peer profile id was
     * parsed out of the ack id — which only a `BisqEasyOpenTradeMessage` carries. `PrivateChatPresenter`
     * does not register an observer at all for that reason, so this warning is not reachable from a DM
     * today; it stays as the guard for a trade message that genuinely could not be resolved.
     */
    override fun addMessageDeliveryStatusObserver(
        tradeMessageId: String,
        onNewStatus: (entry: Pair<String, MessageDeliveryInfoVO>) -> Unit,
    ) {
        val message: Bisq2BisqEasyOpenTradeMessage? = findBisqEasyOpenTradeMessages(tradeMessageId)
        if (message == null) {
            log.w { "TradeMessage for id $tradeMessageId not found" }
            return
        }
        val tradeMessage: Bisq2BisqEasyOpenTradeMessage = message

        val deliveryStatusMapPin =
            networkService.messageDeliveryStatusByMessageId.addObserver { ackRequestingMessageId, status ->
                if (ackRequestingMessageId == null || status == null) {
                    return@addObserver
                }
                val tradeMessageId = tradeMessage.id
                var chatMessageId: String = tradeMessage.ackRequestingMessageId
                var peersProfileId: String? = null
                val separator: String = Bisq2BisqEasyOpenTradeMessage.ACK_REQUESTING_MESSAGE_ID_SEPARATOR
                // In case of a bisqEasyOpenTradeMessage we use the message id and receiver id separated with a '_'.
                // This allows us to handle the ACK messages separately to know when the message was received by
                // both the peer and the mediator (in case of mediation).
                var messageId: String = ackRequestingMessageId
                if (messageId.contains(separator)) {
                    val parts = messageId.split(separator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    messageId = parts[0]
                    peersProfileId = parts[1]
                }
                if (peersProfileId == null) {
                    log.w { "peersProfileId is null for messageId $messageId" }
                    return@addObserver
                }

                if (chatMessageId.contains(separator)) {
                    val parts = chatMessageId.split(separator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    chatMessageId = parts[0]
                }

                if (messageId == chatMessageId) {
                    val statusPin =
                        status.addObserver { status: MessageDeliveryStatus ->
                            val canManuallyResendMessage = canManuallyResendMessage(status, ackRequestingMessageId)

                            val statusEnum = Mappings.MessageDeliveryStatusMapping.fromBisq2Model(status)
                            val messageDeliveryInfo =
                                MessageDeliveryInfoVO(
                                    statusEnum,
                                    ackRequestingMessageId,
                                    canManuallyResendMessage,
                                )
                            onNewStatus(peersProfileId to messageDeliveryInfo)
                        }

                    statusPins.remove(tradeMessageId)?.unbind()
                    statusPins.put(tradeMessageId, statusPin)
                }
            }
        deliveryStatusMapPins.remove(tradeMessageId)?.unbind()
        deliveryStatusMapPins.put(tradeMessageId, deliveryStatusMapPin)
    }

    override fun removeMessageDeliveryStatusObserver(tradeMessageId: String) {
        statusPins.remove(tradeMessageId)?.unbind()
        deliveryStatusMapPins.remove(tradeMessageId)?.unbind()
    }

    private fun findBisqEasyOpenTradeMessages(messageId: String): Bisq2BisqEasyOpenTradeMessage? =
        bisqEasyOpenTradeChannelService.channels
            .flatMap { it.chatMessages }
            .find {
                it.id == messageId &&
                    it.isMyMessage(userIdentityService) &&
                    (
                        it.chatMessageType == ChatMessageType.TEXT ||
                            it.chatMessageType == ChatMessageType.PROTOCOL_LOG_MESSAGE
                    )
            }

    private fun canManuallyResendMessage(
        status: MessageDeliveryStatus,
        ackRequestingMessageId: String,
    ): Boolean =
        status == MessageDeliveryStatus.FAILED &&
            networkService.resendMessageService
                .map({ it.canManuallyResendMessage(ackRequestingMessageId) })
                .orElse(false)
}
