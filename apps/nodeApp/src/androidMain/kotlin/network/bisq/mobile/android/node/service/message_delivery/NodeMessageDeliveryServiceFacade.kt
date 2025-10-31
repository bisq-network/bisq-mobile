package network.bisq.mobile.android.node.service.message_delivery

import bisq.chat.ChatMessageType
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import bisq.common.observable.Pin
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade


class NodeMessageDeliveryServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    MessageDeliveryServiceFacade() {

    // Dependencies
    private val bisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val networkService by lazy { applicationService.networkService.get() }
    private val userIdentityService by lazy { applicationService.userService.get().userIdentityService }


    private val statusPins: MutableMap<String, Pin> = mutableMapOf()
    private val deliveryStatusMapPins: MutableMap<String, Pin> = mutableMapOf()

    override fun activate() {
        super.activate()
    }

    override fun deactivate() {
        super.deactivate()

        statusPins.values.forEach { it.unbind() }
        statusPins.clear()
        deliveryStatusMapPins.values.forEach { it.unbind() }
        deliveryStatusMapPins.clear()
    }

    override fun onResendMessage(messageId: String) {
        networkService.resendMessageService.ifPresent { service -> service.manuallyResendMessage(messageId) }
    }

    override fun addMessageDeliveryStatusObserver(tradeMessageId: String, onNewStatus: (entry: Pair<String, MessageDeliveryInfoVO>) -> Unit) {
        val message: BisqEasyOpenTradeMessage? = findBisqEasyOpenTradeMessages(tradeMessageId)
        if (message == null) {
            log.w { "TradeMessage for id $tradeMessageId not found" }
            return
        }
        val tradeMessage: BisqEasyOpenTradeMessage = message

        val deliveryStatusMapPin = networkService.messageDeliveryStatusByMessageId.addObserver { ackRequestingMessageId, status ->
            if (ackRequestingMessageId == null || status == null) {
                return@addObserver
            }
            val tradeMessageId = tradeMessage.id
            var chatMessageId: String = tradeMessage.ackRequestingMessageId
            var peersProfileId: String? = null
            val separator: String = BisqEasyOpenTradeMessage.ACK_REQUESTING_MESSAGE_ID_SEPARATOR
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
                val statusPin = status.addObserver { status: MessageDeliveryStatus ->
                    val canManuallyResendMessage = canManuallyResendMessage(status, ackRequestingMessageId)

                    val statusEnum = Mappings.MessageDeliveryStatusMapping.fromBisq2Model(status)
                    val messageDeliveryInfo = MessageDeliveryInfoVO(
                        statusEnum,
                        ackRequestingMessageId,
                        canManuallyResendMessage
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

    private fun findBisqEasyOpenTradeMessages(messageId: String): BisqEasyOpenTradeMessage? {
        return bisqEasyOpenTradeChannelService.channels
            .flatMap { it.chatMessages }
            .find {
                it.id == messageId &&
                        it.isMyMessage(userIdentityService) &&
                        (it.chatMessageType == ChatMessageType.TEXT ||
                                it.chatMessageType == ChatMessageType.PROTOCOL_LOG_MESSAGE)
            }
    }

    private fun canManuallyResendMessage(
        status: MessageDeliveryStatus,
        ackRequestingMessageId: String
    ): Boolean = status == MessageDeliveryStatus.FAILED &&
            networkService.resendMessageService
                .map({ it.canManuallyResendMessage(ackRequestingMessageId) })
                .orElse(false)
}