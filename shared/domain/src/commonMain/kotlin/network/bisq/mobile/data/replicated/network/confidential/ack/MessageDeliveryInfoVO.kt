package network.bisq.mobile.data.replicated.network.confidential.ack

import kotlinx.serialization.Serializable

@Serializable
data class MessageDeliveryInfoVO(
    val messageDeliveryStatus: MessageDeliveryStatusEnum,
    val ackRequestingMessageId: String,
    val canManuallyResendMessage: Boolean,
)
