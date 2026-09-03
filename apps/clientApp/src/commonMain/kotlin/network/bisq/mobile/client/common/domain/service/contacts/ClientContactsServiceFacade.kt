package network.bisq.mobile.client.common.domain.service.contacts

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.util.notifyIfDemoModeRestricted
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.contacts.ContactsNotPermittedException
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager

/**
 * My Contacts over the trusted node's API.
 *
 * Gated on [Feature.CONTACTS] like the other capability-gated facades: a node that does not
 * advertise it has no contacts topic or endpoints, so [activate] subscribes to nothing —
 * subscribing anyway would not fail fast (such a node cannot parse the topic out of the request
 * and simply never answers, costing a full subscribe timeout). The CONTACTS API permission is
 * STANDARD, so every pairing covers it via grantAll — no per-device grant gate exists or is
 * needed. The same capability drives the Contacts segment's visibility via `CommunityHubService`,
 * so a user who can see the UI has a facade that is live.
 */
class ClientContactsServiceFacade(
    private val apiGateway: ContactsApiGateway,
    private val backendCapabilitiesService: BackendCapabilitiesService,
    private val json: Json,
    private val globalUiManager: GlobalUiManager,
    // Injectable so tests can drive the subscription collector on their virtual-time dispatcher.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ContactsServiceFacade() {
    private val _contacts = MutableStateFlow<List<ContactListEntryVO>>(emptyList())
    override val contacts: StateFlow<List<ContactListEntryVO>> = _contacts.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // Guarded by [stateMutex]: the subscription collector AND the mutations write it — a mutation
    // applies its own outcome immediately (the response tells us the result; waiting for the
    // subscription push, seconds over Tor, made successful actions look like they did nothing),
    // and the next authoritative REPLACE snapshot reconciles whatever the pushes reorder.
    private val stateMutex = Mutex()
    private val contactsByProfileId: MutableMap<String, ContactListEntryVO> = linkedMapOf()

    override suspend fun activate() {
        super.activate()

        serviceScope.launch(defaultDispatcher) {
            // Awaited rather than read as a snapshot: this facade is activated before
            // ConfigServiceFacade fetches the /config/capabilities manifest, so a snapshot would
            // always say "unsupported". On a node that never advertises the feature this simply
            // never resumes, which is the intended "subscribe to nothing": serviceScope is
            // cancelled on deactivate.
            backendCapabilitiesService.capabilities.first { it.isSupported(Feature.CONTACTS) }
            log.i { "Contacts are supported by the paired node; subscribing" }

            subscribeContacts()
        }
    }

    override suspend fun deactivate() {
        // Cancelled before the state is cleared: super.deactivate() disposes the scope the
        // subscription collector runs on, and an event landing between a clear and the cancel
        // would repopulate what we just dropped.
        super.deactivate()
        stateMutex.withLock {
            contactsByProfileId.clear()
            _contacts.value = emptyList()
            _isLoaded.value = false
        }
    }

    override suspend fun addContact(
        userProfileId: String,
        reason: ContactReasonEnum,
    ): Result<Boolean> {
        // "Nothing changed" honours the Result<Boolean> contract without demo-mode side effects.
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(false)
        return apiGateway
            .addContact(userProfileId, reason)
            .map { response ->
                // Rendered from the response, not the push: the node returns the entry it now
                // holds, so the add is visible the moment the request returns.
                response.entry?.let { upsertLocally(it) }
                response.changed
            }.recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun removeContact(userProfileId: String): Result<Boolean> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(false)
        return apiGateway
            .removeContact(userProfileId)
            .map { response ->
                if (response.changed) removeLocally(userProfileId)
                response.changed
            }.recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun updateContact(
        userProfileId: String,
        tag: String?,
        notes: String?,
        trustScore: Double?,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .updateAnnotations(userProfileId, tag, notes, trustScore)
            .map {
                // Applied locally on success so the card updates in one repaint; the next
                // authoritative snapshot carries the same values.
                patchLocally(userProfileId) { entry ->
                    entry.copy(
                        tag = tag ?: entry.tag,
                        notes = notes ?: entry.notes,
                        trustScore = trustScore ?: entry.trustScore,
                    )
                }
            }.recoverCatching { throw asDomainFailure(it) }
    }

    private suspend fun upsertLocally(entry: ContactListEntryVO) {
        stateMutex.withLock {
            contactsByProfileId[entry.userProfile.id] = entry
            publishContacts()
        }
    }

    private suspend fun removeLocally(userProfileId: String) {
        stateMutex.withLock {
            contactsByProfileId.remove(userProfileId)
            publishContacts()
        }
    }

    private suspend fun patchLocally(
        userProfileId: String,
        transform: (ContactListEntryVO) -> ContactListEntryVO,
    ) {
        stateMutex.withLock {
            contactsByProfileId[userProfileId]?.let { contactsByProfileId[userProfileId] = transform(it) }
            publishContacts()
        }
    }

    // Private

    /**
     * Translates a 403 into [ContactsNotPermittedException] so a withheld permission (a legacy
     * explicit grant that predates CONTACTS and was never re-paired) does not reach the UI as a
     * connection problem. Every other status is reduced to its code, without
     * the original as cause: the node's 404/400 bodies embed the peer's profile id, and
     * `handleError` logs `message` — same scrubbing as the private chat facade.
     */
    private fun asDomainFailure(cause: Throwable): Throwable =
        when {
            cause !is WebSocketRestApiException -> cause
            cause.httpStatusCode == HttpStatusCode.Forbidden -> ContactsNotPermittedException()
            else -> IllegalStateException("Contacts request failed with HTTP ${cause.httpStatusCode.value}")
        }

    private suspend fun subscribeContacts() {
        val observer = apiGateway.subscribeContacts()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            val payload: WebSocketEventPayload<List<ContactListEntryVO>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            stateMutex.withLock {
                when (webSocketEvent.modificationType) {
                    // A contact removed here or on another client of the same node; upserting it
                    // like the other types would resurrect it.
                    ModificationType.REMOVED -> payload.payload.forEach { contactsByProfileId.remove(it.userProfile.id) }

                    // The node pushes the full list as REPLACE on every change, and the
                    // (re)subscription snapshot arrives the same way — authoritative about
                    // absence too: a contact removed on another client while this one was
                    // offline is simply not in it.
                    ModificationType.REPLACE -> {
                        contactsByProfileId.clear()
                        payload.payload.forEach { contactsByProfileId[it.userProfile.id] = it }
                    }

                    else -> payload.payload.forEach { contactsByProfileId[it.userProfile.id] = it }
                }
                // Any processed event means the node has answered — including an EMPTY snapshot,
                // which is how "genuinely no contacts" becomes renderable as such.
                _isLoaded.value = true
                publishContacts()
            }
        }
    }

    private fun publishContacts() {
        // Newest first, matching the node facade — and insertion order would be unstable here
        // anyway, since optimistic upserts append behind whatever the last snapshot ordered.
        _contacts.value = contactsByProfileId.values.sortedByDescending { it.date }
    }
}
