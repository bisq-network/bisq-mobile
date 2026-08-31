package network.bisq.mobile.client.common.domain.service.contacts

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.domain.utils.Logging

/**
 * The trusted node's My Contacts endpoints and subscription. This file IS the wire contract —
 * the bisq2 contacts REST API ) mirrors these paths and record shapes; TODO change them
 * together or not at all.
 *
 * Every route rides the STANDARD CONTACTS API permission on the node, covered by any grantAll
 * pairing — a 403 only reaches here from a legacy explicit grant that predates the permission,
 * which the facade translates as a backstop.
 */
class ContactsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "contacts"

    // Rest API calls

    suspend fun addContact(
        userProfileId: String,
        reason: ContactReasonEnum,
    ): Result<AddContactResponse> {
        // Profile ids are hex, so no url path encoding is required according to RFC 3986
        val path = "$basePath/$userProfileId"
        return webSocketApiClient.post(path, AddContactRequest(reason))
    }

    suspend fun removeContact(userProfileId: String): Result<ContactMutationResponse> = webSocketApiClient.delete("$basePath/$userProfileId")

    /** All annotations in one request — a Save is one action; null = leave unchanged. */
    suspend fun updateAnnotations(
        userProfileId: String,
        tag: String?,
        notes: String?,
        trustScore: Double?,
    ): Result<Unit> = webSocketApiClient.put("$basePath/$userProfileId/annotations", UpdateContactAnnotationsRequest(tag, notes, trustScore))

    // Subscriptions

    suspend fun subscribeContacts(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.CONTACTS)
}

@Serializable
data class AddContactRequest(
    val contactReason: ContactReasonEnum,
)

/** null = leave that annotation unchanged. */
@Serializable
data class UpdateContactAnnotationsRequest(
    val tag: String? = null,
    val notes: String? = null,
    val trustScore: Double? = null,
)

/**
 * [entry] is the list entry as the node now holds it (also when [changed] is false — the contact
 * already existed). Carried so the facade can render the add IMMEDIATELY instead of waiting for
 * the subscription push, which over Tor arrives seconds later and made a successful add look
 * like it did nothing.
 */
@Serializable
data class AddContactResponse(
    val changed: Boolean,
    val entry: ContactListEntryVO? = null,
)

/**
 * Whether the mutation actually changed the list — `false` means the desired state already held
 * (already gone), which mirrors bisq2 core `ContactListService`'s boolean returns and must not
 * surface as an error.
 */
@Serializable
data class ContactMutationResponse(
    val changed: Boolean,
)
