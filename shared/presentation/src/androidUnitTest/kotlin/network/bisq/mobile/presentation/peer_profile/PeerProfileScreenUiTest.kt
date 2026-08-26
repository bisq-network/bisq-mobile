package network.bisq.mobile.presentation.peer_profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.report_user.ReportUserPresenter
import network.bisq.mobile.test.presentation.compose.PresentationInjectComposeUiTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Tests for [PeerProfileScreen] (issue #545).
 *
 * Drives the stateful screen against a real [PeerProfilePresenter] with mocked facades, so the
 * integration seam is covered too: `initialize` firing from the `LaunchedEffect`, the top-bar title
 * falling back while the lookup is in flight, and the report slot's `userProfile != null` gate.
 * Assertions are therefore behavioural — stub a facade, assert what renders, click, assert the facade
 * was called.
 *
 * The screen resolves its presenter through `RememberPresenterLifecycleBackStackAware`, so it needs
 * the `LocalViewModelStoreOwner` and pinned Koin graph that [PresentationInjectComposeUiTestBase]
 * provides via `setInjectTestContent`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerProfileScreenUiTest : PresentationInjectComposeUiTestBase() {
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade
    private lateinit var privateChatServiceFacade: PrivateChatServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>
    private lateinit var ownProfiles: MutableStateFlow<List<UserProfileVO>>
    private lateinit var reputationScores: MutableStateFlow<Map<String, Long>>

    private val peer = createMockUserProfile(PEER_ID)

    private companion object {
        /** [createMockUserProfile] sets `networkId.pubKey.id` to the name, so the id is the name. */
        const val PEER_ID = "SatoshiFan"
        const val OWN_ID = "my-profile"

        /** [ConfirmationDialog] tags both buttons — the only unambiguous way in, see below. */
        const val DIALOG_CONFIRM = "dialog_confirm_yes"
        const val DIALOG_DISMISS = "dialog_confirm_no"

        val REPUTATION = ReputationScoreVO(totalScore = 12_400L, fiveSystemScore = 4.5, ranking = 7)
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                factory {
                    PeerProfilePresenter(
                        userProfileServiceFacade,
                        reputationServiceFacade,
                        privateChatServiceFacade,
                        mockk {
                            every { contacts } returns MutableStateFlow(emptyList())
                        },
                        mockk {
                            every { liveSegments } returns MutableStateFlow(emptySet())
                        },
                        mainPresenter,
                    )
                }
                factory { ReportUserPresenter(mainPresenter, userProfileServiceFacade) }
            },
        )

    override fun onKoinReady() {
        ignoredProfileIds = MutableStateFlow(emptySet())
        ownProfiles = MutableStateFlow(emptyList())
        reputationScores = MutableStateFlow(emptyMap())

        userProfileServiceFacade = mockk(relaxed = true)
        reputationServiceFacade = mockk(relaxed = true)
        privateChatServiceFacade = mockk(relaxed = true) { every { isSupported } returns flowOf(true) }
        mainPresenter = mockk(relaxed = true)

        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        every { userProfileServiceFacade.userProfiles } returns ownProfiles
        // Never left to the relaxed mock: the presenter collects this, and a mocked StateFlow would
        // go silent by accident rather than by design.
        every { reputationServiceFacade.scoreByUserProfileId } returns reputationScores
        coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
        coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.success(REPUTATION)
    }

    private fun renderScreen(profileId: String = PEER_ID) {
        setInjectTestContent { PeerProfileScreen(profileId) }
    }

    private fun ignoreButtonText(): String = "chat.message.contextMenu.ignoreUser".i18n()

    private fun undoIgnoreButtonText(): String = "user.profileCard.userActions.undoIgnore".i18n()

    private fun reportButtonText(): String = "chat.message.contextMenu.reportUser".i18n()

    private fun retryButtonText(): String = "mobile.action.retry".i18n()

    // ---------------------------------------------------------------------------------------
    // Loading the profile
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when a known peer is opened then name and reputation are rendered`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Both bindings, separately: the body heading is the bare name, and the top bar title is
            // the name inside "Profile: {0}" — the wrapper's title binding is what this second one
            // covers, and the two texts no longer match each other.
            composeTestRule.onNodeWithText(PEER_ID).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.peerProfile.titleWithName".i18n(PEER_ID)).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(12_400L)).assertIsDisplayed()
        }

    @Test
    fun `when the reputation snapshot has not arrived then no score is claimed`() =
        runTest {
            // The relaxed mock leaves scoreByUserProfileId empty, which is exactly the "nothing has
            // loaded yet" case. Rendering "0 pts" and an empty star row here would read as a real
            // rating of zero for a peer who may well be highly rated.
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no snapshot"))

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.reputationUnavailable".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(0L)).assertDoesNotExist()
        }

    @Test
    fun `when the lookup is still running then the top bar falls back to a generic title`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } coAnswers {
                delay(Long.MAX_VALUE)
                peer
            }

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.title".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        }

    @Test
    fun `when the peer is unknown then no retry is offered`() =
        runTest {
            // A peer the network genuinely does not know will never resolve, so a retry would loop.
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns null

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.notFound".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText(retryButtonText()).assertDoesNotExist()
        }

    @Test
    fun `when the lookup fails then retrying loads the profile`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } throws RuntimeException("connection lost")

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("mobile.peerProfile.loadFailed".i18n()).assertIsDisplayed()

            coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
            composeTestRule.onNodeWithText(retryButtonText()).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(12_400L)).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.peerProfile.loadFailed".i18n()).assertDoesNotExist()
        }

    @Test
    fun `when the id is one of my profiles then it guards without looking the peer up`() =
        runTest {
            ownProfiles.value = listOf(createMockUserProfile(OWN_ID))

            renderScreen(OWN_ID)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.ownProfileGuard".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText(ignoreButtonText()).assertDoesNotExist()
            coVerify(exactly = 0) { userProfileServiceFacade.findUserProfile(any()) }
        }

    @Test
    fun `when reputation is absent from a loaded snapshot then a zero score still renders`() =
        runTest {
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no score"))
            // Non-empty: the snapshot arrived and this peer is genuinely unscored, as opposed to the
            // unavailable case above where nothing has loaded.
            reputationScores.value = mapOf("someone-else" to 500L)

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(PEER_ID).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(0L)).assertIsDisplayed()
        }

    // ---------------------------------------------------------------------------------------
    // Ignoring
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when ignore is tapped then the confirmation dialog opens`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(ignoreButtonText()).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.ignoreConfirm.headline".i18n()).assertIsDisplayed()
        }

    @Test
    fun `when the confirmation is accepted then the peer is ignored`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(ignoreButtonText()).performClick()
            composeTestRule.waitForIdle()

            // By content description: with the dialog open, "Ignore user" matches both the body
            // button and the dialog's confirm button — chat.ignoreUser.confirm has the same English
            // text as chat.message.contextMenu.ignoreUser.
            composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.ignoreUserProfile(PEER_ID) }
        }

    @Test
    fun `when the confirmation is cancelled then the dialog closes and nothing is ignored`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(ignoreButtonText()).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.ignoreConfirm.headline".i18n()).assertDoesNotExist()
            coVerify(exactly = 0) { userProfileServiceFacade.ignoreUserProfile(any()) }
        }

    @Test
    fun `when an ignore is in flight then the ignore button is disabled`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile(PEER_ID) } coAnswers { delay(Long.MAX_VALUE) }

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(ignoreButtonText()).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(ignoreButtonText()).assertIsNotEnabled()
        }

    @Test
    fun `when the peer is ignored elsewhere then the banner appears and the button flips`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("mobile.peerProfile.ignoredBanner".i18n()).assertDoesNotExist()

            ignoredProfileIds.value = setOf(PEER_ID)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.ignoredBanner".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText(undoIgnoreButtonText()).assertIsDisplayed()
            composeTestRule.onNodeWithText(ignoreButtonText()).assertDoesNotExist()
        }

    @Test
    fun `when the undo button is tapped then the ignore is lifted`() =
        runTest {
            ignoredProfileIds.value = setOf(PEER_ID)

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(undoIgnoreButtonText()).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(PEER_ID) }
        }

    @Test
    fun `when an undo is in flight then the undo button is disabled`() =
        runTest {
            // The banner states the ignore, the button undoes it — so the button is the only control
            // that has to report the in-flight state, and this is the mirror of the ignore case above.
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile(PEER_ID) } coAnswers { delay(Long.MAX_VALUE) }
            ignoredProfileIds.value = setOf(PEER_ID)

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(undoIgnoreButtonText()).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Still labelled "Undo ignore": the facade mock never writes back to ignoredProfileIds,
            // so isIgnored stays true and the button does not flip while the call hangs.
            composeTestRule.onNodeWithText(undoIgnoreButtonText()).assertIsNotEnabled()
        }

    // ---------------------------------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when report is tapped then the report dialog opens`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(reportButtonText()).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Anchored on the message field label: the dialog's headline and its submit button are
            // both "Report to moderator", so that string matches two nodes.
            composeTestRule.onNodeWithText("chat.reportToModerator.message".i18n()).assertIsDisplayed()
        }
}
