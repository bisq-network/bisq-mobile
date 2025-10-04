package network.bisq.mobile.domain.data.replicated.network.confidential.ack

import kotlinx.serialization.Serializable

@Serializable
enum class MessageDeliveryStatusEnum {
    CONNECTING,
    SENT,
    ACK_RECEIVED,
    TRY_ADD_TO_MAILBOX,
    ADDED_TO_MAILBOX,
    MAILBOX_MSG_RECEIVED,
    FAILED;
}