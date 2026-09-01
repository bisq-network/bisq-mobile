package network.bisq.mobile.node.common.domain.service.contacts

import bisq.common.observable.Pin
import bisq.user.contact_list.ContactListEntry
import bisq.user.contact_list.ContactReason
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.domain.utils.resultCatching
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import kotlin.jvm.optionals.getOrNull

/**
 * Node-mode contacts: a thin observer over bisq2 core's `ContactListService`, which the node
 * runs in-process (persisted store, auto-added entries from trades/chats included). All
 * mutations delegate to core, whose observable set drives [contacts] — the observer fires
 * once at subscription, covering the initial load.
 */
class NodeContactsServiceFacade(
    private val provider: AndroidApplicationService.Provider,
) : ContactsServiceFacade() {
    private val contactListService by lazy { provider.userService.get().contactListService }
    private val userProfileService by lazy { provider.userService.get().userProfileService }
    private val userIdentityService by lazy { provider.userService.get().userIdentityService }

    private val _contacts = MutableStateFlow<List<ContactListEntryVO>>(emptyList())
    override val contacts: StateFlow<List<ContactListEntryVO>> = _contacts.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private var contactsPin: Pin? = null

    override suspend fun activate() {
        super.activate()
        contactsPin = contactListService.contactListEntries.addObserver(Runnable { refreshContacts() })
        // The observer fired synchronously above with the in-process store's content.
        _isLoaded.value = true
    }

    override suspend fun deactivate() {
        contactsPin?.unbind()
        contactsPin = null
        _contacts.value = emptyList()
        _isLoaded.value = false
        super.deactivate()
    }

    // The ensureActive gates below: runCatching also catches CancellationException, so without
    // them a navigate-away during a mutation would surface as a failed action instead of
    // propagating the cancellation. A cancellation-shaped failure with the caller still active
    // (a timeout) stays a Result.failure, which is what the caller can act on.
    // TODO replace the runCatching+ensureActive pairs with the shared resultCatching helper once
    //  this branch is rebased onto current main, which carries it.
    override suspend fun addContact(
        userProfileId: String,
        reason: ContactReasonEnum,
    ): Result<Boolean> =
        resultCatching {
            val peer =
                userProfileService.findUserProfile(userProfileId).getOrNull()
                    ?: error("No user profile found")
            val myProfile = userIdentityService.selectedUserIdentity.userProfile
            contactListService.addContactListEntry(peer, myProfile, reason.toBisq2())
        }.onFailure { currentCoroutineContext().ensureActive() }

    // A missing entry is `false`, not an error (see the base-class contract): a stale remove
    // means the peer is already gone, which is exactly the state the user asked for.
    override suspend fun removeContact(userProfileId: String): Result<Boolean> =
        resultCatching {
            val entry = contactListService.contactListEntries.firstOrNull { it.userProfile.id == userProfileId }
            entry != null && contactListService.removeContactListEntry(entry)
        }.onFailure { currentCoroutineContext().ensureActive() }

    // Refreshes explicitly: bisq2 core mutates the entry IN PLACE and persists, without touching
    // the observable set — so the add/remove observer wired in activate() never fires for edits
    // and the flow would go stale until an unrelated add/remove.
    override suspend fun updateContact(
        userProfileId: String,
        tag: String?,
        notes: String?,
        trustScore: Double?,
    ): Result<Unit> =
        resultCatching {
            val entry = requireEntry(userProfileId)
            tag?.let { contactListService.setTag(entry, it) }
            notes?.let { contactListService.setNotes(entry, it) }
            trustScore?.let { contactListService.setTrustScore(entry, it) }
            refreshContacts()
        }.onFailure { currentCoroutineContext().ensureActive() }

    private fun requireEntry(userProfileId: String): ContactListEntry =
        contactListService.contactListEntries.firstOrNull { it.userProfile.id == userProfileId }
            ?: error("No contact list entry found")

    private fun refreshContacts() {
        _contacts.value =
            contactListService.contactListEntries
                .map { it.toVO() }
                .sortedByDescending { it.date }
    }

    private fun ContactListEntry.toVO(): ContactListEntryVO =
        ContactListEntryVO(
            userProfile = Mappings.UserProfileMapping.fromBisq2Model(userProfile),
            date = date,
            contactReason = contactReason.toDomain(),
            trustScore = trustScore.getOrNull(),
            tag = tag.getOrNull(),
            notes = notes.getOrNull(),
        )

    private fun ContactReason.toDomain(): ContactReasonEnum = ContactReasonEnum.valueOf(name)

    private fun ContactReasonEnum.toBisq2(): ContactReason = ContactReason.valueOf(name)
}
