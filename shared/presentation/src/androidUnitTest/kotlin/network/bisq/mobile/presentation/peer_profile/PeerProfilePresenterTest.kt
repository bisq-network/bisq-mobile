package network.bisq.mobile.presentation.peer_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behaviour tests for [PeerProfilePresenter] (issue #545).
 *
 * The lookup distinguishes three failure shapes that look alike from the outside — an own profile, a
 * peer the network does not know, and a lookup that could not complete — and only the last offers a
 * retry. Most of what follows pins those apart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerProfilePresenterTest : PresentationKoinTestBase() {
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade
    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>
    private lateinit var ownProfiles: MutableStateFlow<List<UserProfileVO>>
    private lateinit var presenter: PeerProfilePresenter

    private val peer = createMockUserProfile(PEER_ID)

    private companion object {
        /** [createMockUserProfile] sets `networkId.pubKey.id` to the name, so the id is the name. */
        const val PEER_ID = "peer-1"
        const val OWN_ID = "my-profile"

        val REPUTATION = ReputationScoreVO(totalScore = 12_400L, fiveSystemScore = 4.5, ranking = 7)
    }

    override fun onKoinReady() {
        ignoredProfileIds = MutableStateFlow(emptySet())
        ownProfiles = MutableStateFlow(emptyList())

        userProfileServiceFacade =
            mockk(relaxed = true) {
                every { ignoredProfileIds } returns this@PeerProfilePresenterTest.ignoredProfileIds
                every { userProfiles } returns ownProfiles
            }
        reputationServiceFacade = mockk(relaxed = true)

        coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
        coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.success(REPUTATION)

        presenter =
            PeerProfilePresenter(
                userProfileServiceFacade = userProfileServiceFacade,
                reputationServiceFacade = reputationServiceFacade,
                mainPresenter = mockk<MainPresenter>(relaxed = true),
            )
    }

    // ---------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when a known peer is initialized then profile and reputation are exposed`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(PEER_ID, state.profileId)
            assertEquals(peer, state.userProfile)
            assertEquals(peer.userName, state.displayName)
            assertEquals(4.5, state.starRating)
            assertEquals(12_400L, state.reputationScore)
            assertFalse(state.isLoading)
            assertFalse(state.isNotFound)
            assertFalse(state.isLoadFailed)
        }

    @Test
    fun `when the peer is unknown then reports not found rather than a load failure`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns null

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isNotFound)
            assertFalse(state.isLoadFailed)
            assertFalse(state.isLoading)
        }

    @Test
    fun `when the lookup throws then reports a load failure rather than not found`() =
        runTest {
            // On the client flavour findUserProfile is a round-trip to the trusted node, so a
            // dropped connection must not be surfaced as "this peer does not exist".
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } throws RuntimeException("connection lost")

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isLoadFailed)
            assertFalse(state.isNotFound)
            assertFalse(state.isLoading)
        }

    @Test
    fun `when retry is clicked after a load failure then the profile loads`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } throws RuntimeException("connection lost")
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isLoadFailed)

            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
            presenter.onAction(PeerProfileUiAction.OnRetryLoadClick)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.isLoadFailed)
            assertEquals(peer, state.userProfile)
            assertFalse(state.isLoading)
        }

    @Test
    fun `when reputation cannot be resolved then the profile still renders with a zero score`() =
        runTest {
            // A peer with no reputation yet is exactly the peer a user most wants to inspect.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no score"))

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(peer, state.userProfile)
            assertEquals(0L, state.reputationScore)
            assertEquals(0.0, state.starRating)
            assertFalse(state.isLoadFailed)
        }

    @Test
    fun `when initialized twice with the same id then the profile is loaded once`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.findUserProfile(PEER_ID) }
        }

    // ---------------------------------------------------------------------------------------
    // Own-profile guard
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when the id is one of my profiles then guards without looking the peer up`() =
        runTest {
            ownProfiles.value = listOf(createMockUserProfile(OWN_ID))

            presenter.initialize(OWN_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isOwnProfile)
            assertFalse(presenter.uiState.value.isLoading)
            coVerify(exactly = 0) { userProfileServiceFacade.findUserProfile(any()) }
        }

    @Test
    fun `when the owned-profiles flow is not warmed yet then falls back to the identity ids`() =
        runTest {
            ownProfiles.value = emptyList()
            coEvery { userProfileServiceFacade.getUserIdentityIds() } returns listOf(OWN_ID)

            presenter.initialize(OWN_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isOwnProfile)
        }

    // ---------------------------------------------------------------------------------------
    // Ignore / undo ignore
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when the peer is ignored elsewhere then the ignored state follows live`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            assertFalse(presenter.uiState.value.isIgnored)

            ignoredProfileIds.value = setOf(PEER_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isIgnored)

            ignoredProfileIds.value = emptySet()
            advanceUntilIdle()
            assertFalse(presenter.uiState.value.isIgnored)
        }

    @Test
    fun `when ignore is confirmed twice in a row then the peer is ignored once`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile(PEER_ID) } coAnswers { delay(Long.MAX_VALUE) }
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnConfirmIgnore)
            presenter.onAction(PeerProfileUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.ignoreUserProfile(PEER_ID) }
            assertFalse(presenter.uiState.value.showIgnoreConfirmDialog)
            assertFalse(presenter.isIgnoreActionEnabled.value)
        }

    @Test
    fun `when ignoring fails then the action becomes available again`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile(PEER_ID) } throws RuntimeException("fail")
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            assertTrue(presenter.isIgnoreActionEnabled.value)
        }

    @Test
    fun `when undo ignore is clicked then the ignore is lifted`() =
        runTest {
            ignoredProfileIds.value = setOf(PEER_ID)
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnUndoIgnoreClick)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(PEER_ID) }
        }

    // ---------------------------------------------------------------------------------------
    // Dialogs and the report draft
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when dialog actions are dispatched then their flags toggle`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnIgnoreClick)
            assertTrue(presenter.uiState.value.showIgnoreConfirmDialog)

            presenter.onAction(PeerProfileUiAction.OnDismissIgnoreDialog)
            assertFalse(presenter.uiState.value.showIgnoreConfirmDialog)

            presenter.onAction(PeerProfileUiAction.OnReportClick)
            assertTrue(presenter.uiState.value.showReportDialog)
        }

    @Test
    fun `when a report fails then the typed message is kept for a second attempt`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            presenter.onAction(PeerProfileUiAction.OnReportClick)

            presenter.onAction(
                PeerProfileUiAction.OnReportFailure(
                    message = "Could not reach the moderator",
                    reportMessage = "This user violated chat rules",
                ),
            )
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.showReportDialog)
            assertEquals("This user violated chat rules", state.reportDraft)
        }

    @Test
    fun `when the report dialog is dismissed then the kept message is discarded`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            presenter.onAction(PeerProfileUiAction.OnReportFailure("error", "a half-written report"))
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnDismissReportDialog)

            assertNull(presenter.uiState.value.reportDraft)
            assertFalse(presenter.uiState.value.showReportDialog)
        }
}
