package network.bisq.mobile.presentation.peer_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
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
    private lateinit var privateChatServiceFacade: PrivateChatServiceFacade
    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>
    private lateinit var ownProfiles: MutableStateFlow<List<UserProfileVO>>
    private lateinit var reputationScores: MutableStateFlow<Map<String, Long>>
    private lateinit var presenter: PeerProfilePresenter

    private val peer = createMockUserProfile(PEER_ID)

    private companion object {
        /** [createMockUserProfile] sets `networkId.pubKey.id` to the name, so the id is the name. */
        const val PEER_ID = "peer-1"
        const val OTHER_ID = "peer-2"
        const val OWN_ID = "my-profile"

        val REPUTATION = ReputationScoreVO(totalScore = 12_400L, fiveSystemScore = 4.5, ranking = 7)
    }

    override fun onKoinReady() {
        // The snackbar assertions compare resolved text, so the bundle has to be loaded.
        I18nSupport.initialize("en")
        ignoredProfileIds = MutableStateFlow(emptySet())
        ownProfiles = MutableStateFlow(emptyList())
        reputationScores = MutableStateFlow(emptyMap())

        userProfileServiceFacade =
            mockk(relaxed = true) {
                every { ignoredProfileIds } returns this@PeerProfilePresenterTest.ignoredProfileIds
                every { userProfiles } returns ownProfiles
            }
        // Never left to the relaxed mock: the presenter collects this, and a mocked StateFlow would
        // go silent by accident rather than by design.
        reputationServiceFacade =
            mockk(relaxed = true) {
                every { scoreByUserProfileId } returns reputationScores
            }
        privateChatServiceFacade = mockk(relaxed = true) { every { isSupported } returns flowOf(true) }

        coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
        coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.success(REPUTATION)

        presenter =
            PeerProfilePresenter(
                userProfileServiceFacade = userProfileServiceFacade,
                reputationServiceFacade = reputationServiceFacade,
                privateChatServiceFacade = privateChatServiceFacade,
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
    fun `when reputation is absent from a loaded snapshot then the score is a genuine zero`() =
        runTest {
            // A peer with no reputation yet is exactly the peer a user most wants to inspect, so the
            // screen must still render. A non-empty score map proves the snapshot arrived and this
            // peer simply is not in it.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no score"))
            reputationScores.value = mapOf("someone-else" to 500L)

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(peer, state.userProfile)
            assertEquals(0L, state.reputationScore)
            assertEquals(0.0, state.starRating)
            assertFalse(state.isReputationUnknown)
            assertFalse(state.isLoadFailed)
        }

    @Test
    fun `when the reputation snapshot has not arrived then the score is unknown rather than zero`() =
        runTest {
            // The same failure as above, but with nothing loaded at all. Reporting "0 pts" here would
            // contradict the offerbook card the user tapped through, which may show 4.5 stars for
            // this very peer.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no score"))
            reputationScores.value = emptyMap()

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(peer, state.userProfile)
            assertTrue(state.isReputationUnknown)
            assertFalse(state.isLoadFailed)
        }

    @Test
    fun `when the reputation lookup throws then the score is unknown even with a loaded snapshot`() =
        runTest {
            // A thrown lookup never reached a verdict, so the snapshot says nothing about this peer.
            // Running it through the zero fallback would render a transport error as a confident
            // "0 pts" — the misreading the unknown state exists to prevent.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } throws RuntimeException("transport")
            reputationScores.value = mapOf("someone-else" to 500L)

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(peer, state.userProfile)
            assertTrue(state.isReputationUnknown)
            assertEquals(0L, state.reputationScore)
            assertFalse(state.isLoadFailed)
        }

    @Test
    fun `when the snapshot arrives after the profile then the score fills in`() =
        runTest {
            // The client facade answers getReputation from a cache the REPUTATION subscription fills
            // asynchronously, so a screen opened before the first payload resolves to "unknown". It
            // must not stay that way once the payload lands.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("not cached yet"))

            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isReputationUnknown)

            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.success(REPUTATION)
            reputationScores.value = mapOf(PEER_ID to 12_400L)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.isReputationUnknown)
            assertEquals(12_400L, state.reputationScore)
            assertEquals(4.5, state.starRating)
        }

    @Test
    fun `when another peer's score changes then this peer is not looked up again`() =
        runTest {
            // Re-resolving is not free on the node flavour — Bisq2 ranks a peer by sorting every score
            // it holds — and the snapshot changes on every peer's update, not just this one's.
            var lookups = 0
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } answers {
                lookups++
                Result.success(REPUTATION)
            }
            // Non-empty before loading: a snapshot arriving at all is a relevant change, and this test
            // is about the ones that are not.
            reputationScores.value = mapOf("someone-else" to 500L)

            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            val afterLoad = lookups

            reputationScores.value = mapOf("someone-else" to 900L, "a-third-peer" to 10L)
            advanceUntilIdle()

            assertEquals(afterLoad, lookups)
        }

    @Test
    fun `when a slow load is superseded by a retry then its failure is discarded`() =
        runTest {
            // findUserProfile is a node round-trip on the client flavour, so a stalled first attempt
            // can finish *after* a fast retry has already rendered the profile. The blocker releases
            // it at exactly that point: without cancelling the superseded load, its failure lands
            // last and replaces the profile with the load-failed screen.
            val firstCallBlocker = CompletableDeferred<Unit>()
            var calls = 0
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } coAnswers {
                calls++
                if (calls == 1) {
                    firstCallBlocker.await()
                    throw RuntimeException("slow failure")
                }
                peer
            }

            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isLoading)

            presenter.onAction(PeerProfileUiAction.OnRetryLoadClick)
            advanceUntilIdle()
            assertEquals(peer, presenter.uiState.value.userProfile)

            firstCallBlocker.complete(Unit)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(peer, state.userProfile)
            assertFalse(state.isLoadFailed)
            assertFalse(state.isLoading)
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

    /**
     * A presenter is bound to one peer for life: navigation gives every destination its own back
     * stack entry, so it gives every peer its own presenter instance. This pins what the presenter
     * does if that ever stops holding — it keeps the peer it was bound to rather than swapping to a
     * half-loaded second one.
     */
    @Test
    fun `when initialized again with a different id then the first peer is kept`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.initialize(OTHER_ID)
            advanceUntilIdle()

            assertEquals(peer, presenter.uiState.value.userProfile)
            coVerify(exactly = 0) { userProfileServiceFacade.findUserProfile(OTHER_ID) }
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
    fun `when the ignore call is cancelled then no error is surfaced to the user`() =
        runTest {
            // Backing out while the call is in flight cancels presenterScope. Letting that reach
            // handleError would pop a global "unexpected error" snackbar on whatever screen the user
            // landed on, plus log a failure that never happened.
            coEvery { userProfileServiceFacade.ignoreUserProfile(PEER_ID) } throws CancellationException("navigated away")
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
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
                PeerProfileUiAction.OnReportFailure(reportMessage = "This user violated chat rules"),
            )
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertFalse(state.showReportDialog)
            assertEquals("This user violated chat rules", state.reportDraft)
            // ReportUserPresenter already raised the error snackbar; a second one here would double it.
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    // ---------------------------------------------------------------------------------------
    // Private chat entry point
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when the peer is a normal profile then the private message button is offered`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.canSendPrivateMessage)
        }

    @Test
    fun `when the node does not support private chat then the button is withheld`() =
        runTest {
            every { privateChatServiceFacade.isSupported } returns flowOf(false)

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.canSendPrivateMessage)
        }

    @Test
    fun `when the capability manifest arrives late then the button appears without re-navigating`() =
        runTest {
            // On Bisq Connect the capability set starts at the legacy baseline and only becomes
            // accurate once /config/capabilities lands. Reading it once latched the button hidden for
            // the life of the screen.
            val isSupported = MutableStateFlow(false)
            every { privateChatServiceFacade.isSupported } returns isSupported

            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            assertFalse(presenter.uiState.value.canSendPrivateMessage, "not advertised yet")

            isSupported.value = true
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.canSendPrivateMessage)
        }

    @Test
    fun `when the peer is ignored then the button is withheld`() =
        runTest {
            ignoredProfileIds.value = setOf(PEER_ID)

            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.canSendPrivateMessage)
        }

    /** Bisq 2 would create a `sorted(me, me)` channel and select it on the node. */
    @Test
    fun `when the profile is my own then the button is withheld`() =
        runTest {
            ownProfiles.value = listOf(createMockUserProfile(OWN_ID))

            presenter.initialize(OWN_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isOwnProfile)
            assertFalse(presenter.uiState.value.canSendPrivateMessage)
        }

    @Test
    fun `when opening a private chat succeeds then it navigates to that channel`() =
        runTest {
            coEvery { privateChatServiceFacade.findOrCreateChannel(PEER_ID) } returns Result.success("discussion.a-b")
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnSendPrivateMessageClick)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.PrivateChat("discussion.a-b"), any(), any()) }
            assertFalse(presenter.uiState.value.isOpeningPrivateChat, "the loading state must be released")
        }

    @Test
    fun `when opening a private chat fails then it reports the error and stays put`() =
        runTest {
            coEvery { privateChatServiceFacade.findOrCreateChannel(PEER_ID) } returns
                Result.failure(IllegalStateException("boom"))
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnSendPrivateMessageClick)
            advanceUntilIdle()

            verify(exactly = 0) { navigationManager.navigate(any<NavRoute.PrivateChat>(), any(), any()) }
            verify { globalUiManager.showSnackbar("mobile.privateChats.openChat.failed".i18n(), any(), any(), any()) }
            assertFalse(presenter.uiState.value.isOpeningPrivateChat)
        }

    /**
     * A cancellation that arrives as a `Result` is never the screen's own: `WebSocketApiClient`
     * rethrows the caller's and keeps only what it calls "NOT our cancellation - e.g. the request
     * timeout", which is a `TimeoutCancellationException` and therefore a `CancellationException`.
     * Rethrowing on type here would drop the snackbar for a request that really did time out and
     * leave the button's spinner running, so the handler asks whether *this* coroutine is still
     * active instead.
     */
    @Test
    fun `when opening a private chat fails with a boxed cancellation then it still reports it`() =
        runTest {
            coEvery { privateChatServiceFacade.findOrCreateChannel(PEER_ID) } returns
                Result.failure(CancellationException("request timed out"))
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnSendPrivateMessageClick)
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.privateChats.openChat.failed".i18n(), any(), any(), any()) }
            assertFalse(presenter.uiState.value.isOpeningPrivateChat, "the spinner must not survive the failure")
        }

    /**
     * A withheld permission is not a connection problem: the node advertises the capability from a
     * public endpoint, so the button is offered and only the call can discover the pairing lacks it.
     */
    @Test
    fun `when private chat was not permitted then it says so instead of blaming the connection`() =
        runTest {
            coEvery { privateChatServiceFacade.findOrCreateChannel(PEER_ID) } returns
                Result.failure(PrivateChatNotPermittedException())
            presenter.initialize(PEER_ID)
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnSendPrivateMessageClick)
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.privateChats.notPermitted".i18n(), any(), any(), any()) }
            verify(exactly = 0) { globalUiManager.showSnackbar("mobile.privateChats.openChat.failed".i18n(), any(), any(), any()) }
        }

    @Test
    fun `when the report dialog is dismissed then the kept message is discarded`() =
        runTest {
            presenter.initialize(PEER_ID)
            advanceUntilIdle()
            presenter.onAction(PeerProfileUiAction.OnReportFailure("a half-written report"))
            advanceUntilIdle()

            presenter.onAction(PeerProfileUiAction.OnReportSuccess)

            assertNull(presenter.uiState.value.reportDraft)
            assertFalse(presenter.uiState.value.showReportDialog)
        }
}
