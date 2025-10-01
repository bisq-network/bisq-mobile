package network.bisq.mobile.domain.data.replicated.network.confidential.ack

import kotlinx.serialization.Serializable

@Serializable
data class MessageDeliveryInfoVO(
    val messageDeliveryStatus: MessageDeliveryStatusEnum,
    val ackRequestingMessageId: String,
    val canManuallyResendMessage: Boolean
)
