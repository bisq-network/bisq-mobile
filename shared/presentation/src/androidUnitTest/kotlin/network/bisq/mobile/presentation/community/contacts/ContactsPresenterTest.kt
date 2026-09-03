package network.bisq.mobile.presentation.community.contacts

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var facade: FakeContactsServiceFacade

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        facade = FakeContactsServiceFacade()
    }

    private class FakeContactsServiceFacade : ContactsServiceFacade() {
        val backing = MutableStateFlow<List<ContactListEntryVO>>(emptyList())
        override val contacts: StateFlow<List<ContactListEntryVO>> = backing.asStateFlow()

        val backingLoaded = MutableStateFlow(false)
        override val isLoaded: StateFlow<Boolean> = backingLoaded.asStateFlow()

        override suspend fun addContact(
            userProfileId: String,
            reason: ContactReasonEnum,
        ): Result<Boolean> = Result.success(true)

        override suspend fun removeContact(userProfileId: String): Result<Boolean> = Result.success(true)

        override suspend fun updateContact(
            userProfileId: String,
            tag: String?,
            notes: String?,
            trustScore: Double?,
        ): Result<Unit> = Result.success(Unit)
    }

    private fun entry(
        name: String,
        reason: ContactReasonEnum = ContactReasonEnum.MANUALLY_ADDED,
        date: Long = 1_700_000_000_000,
        tag: String? = null,
        trustScore: Double? = null,
    ): ContactListEntryVO =
        ContactListEntryVO(
            userProfile = createMockUserProfile(name),
            date = date,
            contactReason = reason,
            trustScore = trustScore,
            tag = tag,
        )

    private fun attachedPresenter(): ContactsPresenter {
        val presenter = ContactsPresenter(mainPresenter, facade, mockk<UserProfileServiceFacade>(relaxed = true))
        presenter.onViewAttached()
        return presenter
    }

    @Test
    fun `renders the facade's contacts with reason, tag and trust mapped through`() =
        runTest {
            facade.backing.value = listOf(entry("Alice", tag = "SEPA", trustScore = 0.9), entry("Bob", reason = ContactReasonEnum.PRIVATE_CHAT))
            val presenter = attachedPresenter()
            advanceUntilIdle()

            val items = presenter.uiState.value.contacts
            assertEquals(2, items.size)
            assertEquals("SEPA", items[0].tag)
            assertEquals(0.9, items[0].trustScore)
            assertEquals(ContactReasonEnum.PRIVATE_CHAT, items[1].contactReason)
            assertEquals(items[0].peerProfile.id, items[0].id)
        }

    /**
     * Acceptance criterion for contacts impl: the list renders from the facade's StateFlow, never a
     * navigation-time snapshot — a removal made elsewhere (Peer Profile) must already be
     * reflected here when the user navigates back, with no reload.
     */
    @Test
    fun `a contact removed through the facade disappears from the list without a reload`() =
        runTest {
            val alice = entry("Alice")
            val bob = entry("Bob")
            facade.backing.value = listOf(alice, bob)
            val presenter = attachedPresenter()
            advanceUntilIdle()
            assertEquals(2, presenter.uiState.value.contacts.size)

            // Simulates PeerProfile's remove landing in the shared facade while this tab is on the back stack.
            facade.backing.value = listOf(bob)
            advanceUntilIdle()

            assertEquals(
                listOf("Bob"),
                presenter.uiState.value.contacts
                    .map { it.peerProfile.userName },
            )
        }

    /**
     * The loading state follows the facade's OWN loaded signal, not the flow's first emission: a
     * StateFlow replays its empty initial value immediately, which on the Connect app showed a
     * wrong "no contacts yet" for the seconds the subscription snapshot took to arrive over Tor.
     */
    @Test
    fun `stays loading until the facade reports loaded, even though the empty flow already emitted`() =
        runTest {
            val presenter = attachedPresenter()
            assertEquals(true, presenter.uiState.value.isLoading)

            advanceUntilIdle()
            // The flow's initial empty emission alone must NOT clear the spinner.
            assertEquals(true, presenter.uiState.value.isLoading)

            facade.backingLoaded.value = true
            advanceUntilIdle()

            assertEquals(false, presenter.uiState.value.isLoading)
        }

    @Test
    fun `an empty list from a loaded facade renders as genuinely no contacts`() =
        runTest {
            facade.backingLoaded.value = true
            val presenter = attachedPresenter()
            advanceUntilIdle()

            assertEquals(false, presenter.uiState.value.isLoading)
            assertEquals(0, presenter.uiState.value.contacts.size)
        }

    @Test
    fun `seeds synchronously from an already-loaded facade without a loading flash`() =
        runTest {
            facade.backing.value = listOf(entry("Alice"))
            facade.backingLoaded.value = true
            val presenter = ContactsPresenter(mainPresenter, facade, mockk(relaxed = true))

            // Before any coroutine runs: seeded list, no loading state.
            assertEquals(1, presenter.uiState.value.contacts.size)
            assertEquals(false, presenter.uiState.value.isLoading)
        }
}
