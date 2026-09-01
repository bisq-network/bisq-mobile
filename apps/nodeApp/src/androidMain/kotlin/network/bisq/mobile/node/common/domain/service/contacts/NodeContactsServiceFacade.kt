package network.bisq.mobile.node.common.domain.service.contacts

import bisq.common.observable.Pin
import bisq.user.contact_list.ContactListEntry
import bisq.user.contact_list.ContactReason
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

    private var contactsPin: Pin? = null

    override suspend fun activate() {
        super.activate()
        contactsPin = contactListService.contactListEntries.addObserver(Runnable { refreshContacts() })
    }

    override suspend fun deactivate() {
        contactsPin?.unbind()
        contactsPin = null
        _contacts.value = emptyList()
        super.deactivate()
    }

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
        }

    // A missing entry is `false`, not an error (see the base-class contract): a stale remove
    // means the peer is already gone, which is exactly the state the user asked for.
    override suspend fun removeContact(userProfileId: String): Result<Boolean> =
        resultCatching {
            val entry = contactListService.contactListEntries.firstOrNull { it.userProfile.id == userProfileId }
            entry != null && contactListService.removeContactListEntry(entry)
        }

    // The set* methods below refresh explicitly: bisq2 core mutates the entry IN PLACE and
    // persists, without touching the observable set — so the add/remove observer wired in
    // activate() never fires for them and the flow would go stale until an unrelated add/remove.
    override suspend fun setTag(
        userProfileId: String,
        tag: String,
    ): Result<Unit> =
        resultCatching {
            contactListService.setTag(requireEntry(userProfileId), tag)
            refreshContacts()
        }

    override suspend fun setNotes(
        userProfileId: String,
        notes: String,
    ): Result<Unit> =
        resultCatching {
            contactListService.setNotes(requireEntry(userProfileId), notes)
            refreshContacts()
        }

    override suspend fun setTrustScore(
        userProfileId: String,
        trustScore: Double,
    ): Result<Unit> =
        resultCatching {
            contactListService.setTrustScore(requireEntry(userProfileId), trustScore)
            refreshContacts()
        }

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
