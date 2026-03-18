package network.bisq.mobile.client.common.domain.service.message_delivery

import network.bisq.mobile.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade

// TODO impl
class ClientMessageDeliveryServiceFacade : MessageDeliveryServiceFacade() {
    override suspend fun activate() {
        super.activate()
    }

    override suspend fun deactivate() {
        super.deactivate()
    }

    override fun onResendMessage(messageId: String) {
        // TODO impl
    }

    override fun addMessageDeliveryStatusObserver(
        tradeMessageId: String,
        onNewStatus: (entry: Pair<String, MessageDeliveryInfoVO>) -> Unit,
    ) {
        // TODO impl
    }

    override fun removeMessageDeliveryStatusObserver(tradeMessageId: String) {
        // TODO impl
    }
}
