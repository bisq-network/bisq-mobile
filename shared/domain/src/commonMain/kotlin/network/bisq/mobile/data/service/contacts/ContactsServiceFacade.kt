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
     * Whether the initial list has been received, so an empty [contacts] means "genuinely no
     * contacts" rather than "not loaded yet". The node flips it during activation (the in-process
     * store is already loaded); the Connect app only once the subscription snapshot arrives — which
     * over Tor is what separates a spinner from a wrong "no contacts yet".
     */
    abstract val isLoaded: StateFlow<Boolean>

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

    /**
     * Applies the user-editable annotations in ONE call — a Save is one action, and on the Connect
     * app one round trip; per-field requests made an edit crawl onto the card field by field over
     * Tor. `null` = leave that field unchanged.
     */
    abstract suspend fun updateContact(
        userProfileId: String,
        tag: String? = null,
        notes: String? = null,
        trustScore: Double? = null,
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
