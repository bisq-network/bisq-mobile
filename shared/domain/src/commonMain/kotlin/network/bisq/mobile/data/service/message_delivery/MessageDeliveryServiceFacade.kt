package network.bisq.mobile.data.service.message_delivery

import network.bisq.mobile.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

abstract class MessageDeliveryServiceFacade :
    ServiceFacade(),
    LifeCycleAware,
    Logging {
    abstract fun onResendMessage(messageId: String)

    abstract fun addMessageDeliveryStatusObserver(
        tradeMessageId: String,
        onNewStatus: (entry: Pair<String, MessageDeliveryInfoVO>) -> Unit,
    )

    abstract fun removeMessageDeliveryStatusObserver(tradeMessageId: String)
}
