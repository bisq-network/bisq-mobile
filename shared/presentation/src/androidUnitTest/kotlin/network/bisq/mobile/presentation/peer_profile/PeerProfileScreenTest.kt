package network.bisq.mobile.presentation.peer_profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.NoOpAnalyticsService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.report_user.ReportUserPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
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
 * Setup is self-contained (own compose rule, own Koin, own dispatcher) rather than extending a leaf
 * base, following `PaymentAccountReviewScreenTest`. Screens resolved through
 * `RememberPresenterLifecycleBackStackAware` need two things a plain `setContent` does not give them
 * — a [LocalViewModelStoreOwner] and an explicitly provided Koin context — and both are visible here
 * rather than inherited.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PeerProfileScreenTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule(effectContext = testDispatcher)

    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var navigationManager: NavigationManager
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var koinApplication: KoinApplication
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>
    private lateinit var ownProfiles: MutableStateFlow<List<UserProfileVO>>

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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()

        ignoredProfileIds = MutableStateFlow(emptySet())
        ownProfiles = MutableStateFlow(emptyList())

        userProfileServiceFacade = mockk(relaxed = true)
        reputationServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        // TopBar reads both of these directly; a relaxed mock alone does not survive collectAsState.
        every { navigationManager.currentTab } returns MutableStateFlow(null)
        every { navigationManager.showBackButton() } returns false

        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        every { userProfileServiceFacade.userProfiles } returns ownProfiles
        coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
        coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.success(REPUTATION)

        viewModelStore = ViewModelStore()
        viewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = this@PeerProfileScreenTest.viewModelStore
            }

        runCatching { stopKoin() }
        koinApplication =
            startKoin {
                modules(
                    module {
                        single<NavigationManager> { navigationManager }
                        single<GlobalUiManager> { globalUiManager }
                        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                        // BasePresenter resolves this lazily; bound so opting this screen into
                        // screen tracking later cannot turn into a NoDefinitionFoundException.
                        single<AnalyticsService> { NoOpAnalyticsService }
                        single<ITopBarPresenter> { PreviewTopBarPresenter() }
                        factory { PeerProfilePresenter(userProfileServiceFacade, reputationServiceFacade, mainPresenter) }
                        factory { ReportUserPresenter(mainPresenter, userProfileServiceFacade) }
                    },
                )
            }
    }

    @After
    fun tearDown() {
        // Dispose the composition before Koin goes away, so onViewHidden/onViewUnattaching run
        // against a live container; clearing the store fires PresenterHolder.onCleared().
        runCatching {
            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
        runCatching { viewModelStore.clear() }
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    /**
     * [KoinIsolatedContext] is not ceremony. `RememberPresenterLifecycleBackStackAware` resolves the
     * presenter through `org.koin.compose.getKoin()`, whose backing composition local caches its
     * default `ComposeContextWrapper` process-wide and only re-resolves when reading it *throws*.
     * The first test in the class would otherwise pin the Koin instance, and every later test would
     * resolve against the one already stopped in teardown — `Scope '_root_' is closed`. Providing
     * this test's own application bypasses that cache. (`koinInject` is immune: it goes through
     * `currentKoinScope()`, which does check `scope.closed`.)
     */
    private fun renderScreen(profileId: String = PEER_ID) {
        composeTestRule.setContent {
            KoinIsolatedContext(koinApplication) {
                CompositionLocalProvider(
                    LocalIsTest provides true,
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                ) {
                    BisqTheme {
                        PeerProfileScreen(profileId)
                    }
                }
            }
        }
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
        runTest(testDispatcher) {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Twice on purpose: the top bar title binds to the loaded name, and so does the body
            // heading. That the count is 2 rather than 1 is the wrapper's title binding working.
            composeTestRule.onAllNodesWithText(PEER_ID).assertCountEquals(2)
            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(12_400L)).assertIsDisplayed()
        }

    @Test
    fun `when the lookup is still running then the top bar falls back to a generic title`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            ownProfiles.value = listOf(createMockUserProfile(OWN_ID))

            renderScreen(OWN_ID)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.peerProfile.ownProfileGuard".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText(ignoreButtonText()).assertDoesNotExist()
            coVerify(exactly = 0) { userProfileServiceFacade.findUserProfile(any()) }
        }

    @Test
    fun `when reputation cannot be resolved then the profile still renders with a zero score`() =
        runTest(testDispatcher) {
            coEvery { reputationServiceFacade.getReputation(PEER_ID) } returns Result.failure(RuntimeException("no score"))

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onAllNodesWithText(PEER_ID).assertCountEquals(2)
            composeTestRule.onNodeWithText("mobile.peerProfile.reputation".i18n(0L)).assertIsDisplayed()
        }

    // ---------------------------------------------------------------------------------------
    // Ignoring
    // ---------------------------------------------------------------------------------------

    @Test
    fun `when ignore is tapped then the confirmation dialog opens`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
