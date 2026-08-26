package network.bisq.mobile.data.replicated.user.contact_list

import kotlinx.serialization.Serializable

/** Mirrors bisq2 core `bisq.user.contact_list.ContactReason` name-for-name. */
@Serializable
enum class ContactReasonEnum {
    PRIVATE_CHAT,
    BISQ_EASY_TRADE,
    MUSIG_TRADE,
    MANUALLY_ADDED,
}
