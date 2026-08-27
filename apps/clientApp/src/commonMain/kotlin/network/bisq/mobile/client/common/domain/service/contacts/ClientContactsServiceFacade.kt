package network.bisq.mobile.client.common.domain.service.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade

/**
 * Client-mode contacts: DORMANT STUB. The trusted-node API does not expose the contact list
 * yet (#1238 PR 3: gateway + WS subscription behind a BackendCapabilitiesService probe and
 * the sensitive CONTACTS pairing permission). Until then the Contacts segment is not in the
 * client's live set (see `CommunityHubService` gating), so nothing user-reachable calls
 * this — the failure results are a defensive backstop, not UX.
 */
class ClientContactsServiceFacade : ContactsServiceFacade() {
    private val _contacts = MutableStateFlow<List<ContactListEntryVO>>(emptyList())
    override val contacts: StateFlow<List<ContactListEntryVO>> = _contacts.asStateFlow()

    // TODO(#1238 PR 3): replace with api-gateway calls + CONTACTS WS subscription.
    override suspend fun addContact(
        userProfileId: String,
        reason: ContactReasonEnum,
    ): Result<Boolean> = notAvailable()

    override suspend fun removeContact(userProfileId: String): Result<Boolean> = notAvailable()

    override suspend fun setTag(
        userProfileId: String,
        tag: String,
    ): Result<Unit> = notAvailable()

    override suspend fun setNotes(
        userProfileId: String,
        notes: String,
    ): Result<Unit> = notAvailable()

    override suspend fun setTrustScore(
        userProfileId: String,
        trustScore: Double,
    ): Result<Unit> = notAvailable()

    private fun <T> notAvailable(): Result<T> = Result.failure(UnsupportedOperationException("Contacts API not available on trusted nodes yet"))
}
