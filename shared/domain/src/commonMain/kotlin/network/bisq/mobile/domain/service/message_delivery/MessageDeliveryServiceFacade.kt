package network.bisq.mobile.domain.service.message_delivery

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

abstract class MessageDeliveryServiceFacade : ServiceFacade(), LifeCycleAware, Logging {
    abstract fun onResendMessage(messageId: String)
    abstract fun addMessageDeliveryStatusObserver(tradeMessageId: String): StateFlow<Map<String, MessageDeliveryInfoVO>>
    abstract fun removeMessageDeliveryStatusObserver(tradeMessageId: String)
}
