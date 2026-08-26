package network.bisq.mobile.data.replicated.user.contact_list

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Mirrors bisq2 core `bisq.user.contact_list.ContactListEntry` (minus `myUserProfile`, which
 * no mobile surface needs). Field contracts from core `ContactListService`: [tag] <= 30 chars,
 * [notes] <= 600 chars, [trustScore] in 0.0..1.0 — all three user-editable and optional.
 */
@Serializable
data class ContactListEntryVO(
    val userProfile: UserProfileVO,
    val date: Long,
    val contactReason: ContactReasonEnum,
    val trustScore: Double? = null,
    val tag: String? = null,
    val notes: String? = null,
)
