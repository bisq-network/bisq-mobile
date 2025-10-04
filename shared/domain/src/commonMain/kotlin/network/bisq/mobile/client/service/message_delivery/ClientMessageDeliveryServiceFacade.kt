package network.bisq.mobile.client.service.message_delivery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    override fun addMessageDeliveryStatusObserver(tradeMessageId: String): StateFlow<Map<String, MessageDeliveryInfoVO>> {
        // TODO impl
        val _messageDeliveryInfoByPeersProfileId =
            MutableStateFlow<Map<String, MessageDeliveryInfoVO>>(emptyMap())
        val messageDeliveryInfoByPeersProfileId: StateFlow<Map<String, MessageDeliveryInfoVO>> =
            _messageDeliveryInfoByPeersProfileId.asStateFlow()

        return messageDeliveryInfoByPeersProfileId
    }

    override fun removeMessageDeliveryStatusObserver(tradeMessageId: String) {
        // TODO impl
    }
}