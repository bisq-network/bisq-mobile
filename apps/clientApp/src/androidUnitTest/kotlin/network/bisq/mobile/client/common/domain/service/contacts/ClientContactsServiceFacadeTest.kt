package network.bisq.mobile.client.common.domain.service.contacts

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.contacts.ContactsNotPermittedException
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientContactsServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: ContactsApiGateway = mockk(relaxed = true)
    private val globalUiManager: GlobalUiManager = mockk(relaxed = true)
    private val capabilities = MutableStateFlow(BackendCapabilities(setOf(Feature.CONTACTS.key)))
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk { every { this@mockk.capabilities } returns this@ClientContactsServiceFacadeTest.capabilities }
    private val json = clientJson

    private val contactsObserver = WebSocketEventObserver()
    private val alice = createMockUserProfile("alice")
    private val bob = createMockUserProfile("bob")
    private var sequence = 0

    private lateinit var facade: ClientContactsServiceFacade

    override fun onSetup() {
        coEvery { apiGateway.subscribeContacts() } returns contactsObserver
        facade =
            ClientContactsServiceFacade(
                apiGateway,
                backendCapabilitiesService,
                json,
                globalUiManager,
                testDispatcher,
            )
    }

    @Test
    fun `subscribes to nothing when the node does not advertise the feature`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 0) { apiGateway.subscribeContacts() }
            assertTrue(facade.contacts.value.isEmpty())
        }

    /**
     * The ordering this facade actually runs in: activated before `ConfigServiceFacade` fetches
     * the capability manifest, so the gate opens only later — a snapshot read at activation
     * would leave the feature dead for the whole session.
     */
    @Test
    fun `subscribes once the node advertises the feature, even though it was unsupported at activate`() =
        runTest {
            capabilities.value = BackendCapabilities(emptySet())

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeContacts() }

            capabilities.value = BackendCapabilities(setOf(Feature.CONTACTS.key))
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeContacts() }
        }

    @Test
    fun `an added contact lands in the list and a re-sent entry updates it in place`() =
        runTest {
            activateAndSettle()

            emitContacts(listOf(entry(alice.id, tag = null)))
            advanceUntilIdle()
            assertEquals(listOf<String?>(null), facade.contacts.value.map { it.tag })

            // ADDED doubles as the update event: an annotation edit re-sends the entry.
            emitContacts(listOf(entry(alice.id, tag = "friend")))
            advanceUntilIdle()

            val contact = facade.contacts.value.single()
            assertEquals(alice.id, contact.userProfile.id)
            assertEquals("friend", contact.tag)
        }

    @Test
    fun `a contact removed by the node is dropped instead of being re-added`() =
        runTest {
            activateAndSettle()
            emitContacts(listOf(entry(alice.id), entry(bob.id)))
            advanceUntilIdle()
            assertEquals(2, facade.contacts.value.size)

            emitContacts(listOf(entry(bob.id)), modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertEquals(listOf(alice.id), facade.contacts.value.map { it.userProfile.id })
        }

    /**
     * REPLACE is the snapshot `WebSocketClientImpl` synthesises from the subscription response,
     * re-applied on every reconnect — what this client sees after being offline while the list
     * changed on another client of the same node. It is authoritative about absence.
     */
    @Test
    fun `a resubscribe snapshot drops a contact removed while this client was offline`() =
        runTest {
            activateAndSettle()
            emitContacts(listOf(entry(alice.id), entry(bob.id)))
            advanceUntilIdle()

            emitContacts(listOf(entry(bob.id)), modificationType = ModificationType.REPLACE)
            advanceUntilIdle()

            assertEquals(listOf(bob.id), facade.contacts.value.map { it.userProfile.id })
        }

    /** Newest first like the node app, whatever order events and optimistic upserts arrive in. */
    @Test
    fun `contacts publish newest first regardless of arrival order`() =
        runTest {
            activateAndSettle()
            emitContacts(listOf(entry(alice.id, date = 1000L)))
            advanceUntilIdle()
            coEvery { apiGateway.addContact(any(), any()) } returns
                Result.success(AddContactResponse(changed = true, entry = entry(bob.id, date = 2000L)))

            facade.addContact(bob.id, ContactReasonEnum.MANUALLY_ADDED)

            assertEquals(listOf(bob.id, alice.id), facade.contacts.value.map { it.userProfile.id })
        }

    @Test
    fun `add and remove report whether the list actually changed`() =
        runTest {
            coEvery { apiGateway.addContact(any(), any()) } returns Result.success(AddContactResponse(changed = true))
            coEvery { apiGateway.removeContact(any()) } returns Result.success(ContactMutationResponse(changed = false))

            assertEquals(true, facade.addContact(alice.id, ContactReasonEnum.MANUALLY_ADDED).getOrNull())
            assertEquals(false, facade.removeContact(alice.id).getOrNull())
        }

    /**
     * The mutation renders from its OWN response: the subscription push confirms it seconds later
     * over Tor, and waiting on it made a successful add look like it did nothing (the field report
     * that motivated this — the user retried an add that had already succeeded).
     */
    @Test
    fun `an added contact is visible immediately from the response, before any push`() =
        runTest {
            coEvery { apiGateway.addContact(any(), any()) } returns
                Result.success(AddContactResponse(changed = true, entry = entry(alice.id)))

            facade.addContact(alice.id, ContactReasonEnum.MANUALLY_ADDED)

            assertEquals(listOf(alice.id), facade.contacts.value.map { it.userProfile.id })
        }

    @Test
    fun `a removed contact disappears immediately from the response, before any push`() =
        runTest {
            activateAndSettle()
            emitContacts(listOf(entry(alice.id)))
            advanceUntilIdle()
            coEvery { apiGateway.removeContact(any()) } returns Result.success(ContactMutationResponse(changed = true))

            facade.removeContact(alice.id)

            assertTrue(facade.contacts.value.isEmpty())
        }

    @Test
    fun `a saved annotation update patches the entry locally in one repaint`() =
        runTest {
            activateAndSettle()
            emitContacts(listOf(entry(alice.id, tag = "old")))
            advanceUntilIdle()
            coEvery { apiGateway.updateAnnotations(any(), any(), any(), any()) } returns Result.success(Unit)

            facade.updateContact(alice.id, tag = "friend", trustScore = 0.7)

            val contact = facade.contacts.value.single()
            assertEquals("friend", contact.tag)
            assertEquals(0.7, contact.trustScore)
            assertNull(contact.notes, "an omitted field stays unchanged")
        }

    /** One 403 stands for all routes: they all sit under `contacts`, mapped to the CONTACTS permission. */
    @Test
    fun `a forbidden contacts call is reported as a withheld permission, whichever call it is`() =
        runTest {
            val forbidden = WebSocketRestApiException(HttpStatusCode.Forbidden, "permission_not_granted: CONTACTS")
            coEvery { apiGateway.addContact(any(), any()) } returns Result.failure(forbidden)
            coEvery { apiGateway.removeContact(any()) } returns Result.failure(forbidden)
            coEvery { apiGateway.updateAnnotations(any(), any(), any(), any()) } returns Result.failure(forbidden)

            val results =
                listOf(
                    "addContact" to facade.addContact(alice.id, ContactReasonEnum.MANUALLY_ADDED),
                    "removeContact" to facade.removeContact(alice.id),
                    "updateContact" to facade.updateContact(alice.id, tag = "friend"),
                )

            results.forEach { (name, result) ->
                assertTrue(
                    result.exceptionOrNull() is ContactsNotPermittedException,
                    "$name must translate the 403 into the typed permission failure",
                )
            }
        }

    /** The node's 404/400 bodies name the peer's profile id, and `handleError` logs `message`. */
    @Test
    fun `other contacts failures keep the status but not the node's body`() =
        runTest {
            coEvery { apiGateway.updateAnnotations(any(), any(), any(), any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.NotFound, "No contact found for profile ID ${alice.id}"))

            val exception = facade.updateContact(alice.id, tag = "friend").exceptionOrNull()

            assertNotNull(exception)
            assertFalse(exception is ContactsNotPermittedException)
            assertFalse(exception.message.orEmpty().contains(alice.id), "the node's body names the peer")
            assertTrue(exception.message.orEmpty().contains("404"))
            assertNull(exception.cause, "the original exception would carry the body into the log through the cause chain")
        }

    /**
     * isLoaded is what separates "not loaded yet" (spinner) from "genuinely no contacts" (empty
     * state) — an EMPTY snapshot must flip it too, or a user with no contacts spins forever.
     */
    @Test
    fun `isLoaded flips on the first event, an empty snapshot included`() =
        runTest {
            activateAndSettle()
            assertFalse(facade.isLoaded.value)

            emitContacts(emptyList(), modificationType = ModificationType.REPLACE)
            advanceUntilIdle()

            assertTrue(facade.isLoaded.value)

            facade.deactivate()
            assertFalse(facade.isLoaded.value, "a deactivated facade must not present stale data as loaded")
        }

    private suspend fun TestScope.activateAndSettle() {
        facade.activate()
        advanceUntilIdle()
    }

    private fun entry(
        profileId: String,
        tag: String? = null,
        date: Long = 1234L,
    ): ContactListEntryVO {
        val profile = if (profileId == alice.id) alice else bob
        return ContactListEntryVO(
            userProfile = profile,
            date = date,
            contactReason = ContactReasonEnum.MANUALLY_ADDED,
            trustScore = null,
            tag = tag,
            notes = null,
        )
    }

    private suspend fun emitContacts(
        entries: List<ContactListEntryVO>,
        modificationType: ModificationType = ModificationType.ADDED,
    ) {
        contactsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.CONTACTS,
                subscriberId = "contacts",
                deferredPayload = json.encodeToString(entries),
                modificationType = modificationType,
                sequenceNumber = sequence++,
            ),
        )
    }
}
