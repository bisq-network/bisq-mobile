package network.bisq.mobile.client.service.message_delivery

import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade

// TODO impl
class ClientMessageDeliveryServiceFacade() :
    MessageDeliveryServiceFacade() {

    override fun activate() {
        super.activate()
    }

    override fun deactivate() {
        super.deactivate()
    }

    override fun onResendMessage(messageId: String) {
        // TODO impl
    }

    override fun addMessageDeliveryStatusObserver(tradeMessageId: String, onNewStatus: (entry: Pair<String, MessageDeliveryInfoVO>) -> Unit) {
        // TODO impl
    }

    override fun removeMessageDeliveryStatusObserver(tradeMessageId: String) {
        // TODO impl
    }
}