package network.bisq.mobile.data.service.contacts

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ServiceFacade

/**
 * My Contacts: the user's contact directory, backed by bisq2 core's
 * `ContactListService` (node mode runs it in-process; client mode reaches it through the
 * trusted-node API once that ships — until then the client implementation is a dormant stub
 * and the feature is gated off, see `CommunityHubService`).
 *
 * [contacts] is the single source of truth the UI renders from — screens must observe it
 * rather than snapshot it, so a mutation on one screen (e.g. remove on Peer Profile) is
 * already reflected on any other (the Contacts tab) on back-navigation.
 */
abstract class ContactsServiceFacade : ServiceFacade() {
    abstract val contacts: StateFlow<List<ContactListEntryVO>>

    /**
     * Add/remove are idempotent and report whether the list actually changed: `false` means the
     * desired state already held (peer already a contact / already gone), which callers must not
     * surface as an error nor count as an action — a stale button press is benign, the rendered
     * state is already correct. Mirrors bisq2 core `ContactListService`'s boolean returns.
     */
    abstract suspend fun addContact(
        userProfileId: String,
        reason: ContactReasonEnum = ContactReasonEnum.MANUALLY_ADDED,
    ): Result<Boolean>

    abstract suspend fun removeContact(userProfileId: String): Result<Boolean>

    abstract suspend fun setTag(
        userProfileId: String,
        tag: String,
    ): Result<Unit>

    abstract suspend fun setNotes(
        userProfileId: String,
        notes: String,
    ): Result<Unit>

    abstract suspend fun setTrustScore(
        userProfileId: String,
        trustScore: Double,
    ): Result<Unit>

    fun isContact(userProfileId: String): Boolean = findContact(userProfileId) != null

    fun findContact(userProfileId: String): ContactListEntryVO? = contacts.value.firstOrNull { it.userProfile.id == userProfileId }

    companion object {
        // Mirrors bisq2 core ContactListService's editing contract.
        const val MAX_TAG_LENGTH = 30
        const val MAX_NOTES_LENGTH = 600
        const val MIN_TRUST_SCORE = 0.0
        const val MAX_TRUST_SCORE = 1.0
    }
}
