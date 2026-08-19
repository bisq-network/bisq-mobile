package network.bisq.mobile.presentation.private_chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.report_user.ReportUserPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.compose.PresentationInjectComposeUiTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Tests for [PrivateChatScreen] (issue #590).
 *
 * Drives the stateful screen against a real [PrivateChatPresenter] with mocked facades, so the
 * `LaunchedEffect` → `initialize` seam and the report slot's `reportTargetProfile != null` gate are
 * covered too. Same base as `PeerProfileScreenUiTest`, for the same reason: the screen resolves its
 * presenter through `RememberPresenterLifecycleBackStackAware`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivateChatScreenTest : PresentationInjectComposeUiTestBase() {
    private companion object {
        const val CHANNEL_ID = "discussion.a-b"
        const val PEER_NAME = "SatoshiFan"

        /** The top-bar action carries a test tag, not a content description. */
        const val LEAVE_BUTTON = "leave_private_chat_button"

        /** [ConfirmationDialog] tags both buttons; the leave dialog's texts are not unique enough. */
        const val DIALOG_CONFIRM = "dialog_confirm_yes"
        const val DIALOG_DISMISS = "dialog_confirm_no"
    }

    private lateinit var privateChatServiceFacade: PrivateChatServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade
    private lateinit var notificationController: NotificationController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var mainPresenter: MainPresenter

    private lateinit var channels: MutableStateFlow<List<TwoPartyPrivateChatChannel>>
    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>

    private val peer = createMockUserProfile(PEER_NAME)
    private val me = createMockUserProfile("me")

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                factory {
                    PrivateChatPresenter(
                        mainPresenter,
                        privateChatServiceFacade,
                        userProfileServiceFacade,
                        reputationServiceFacade,
                        notificationController,
                        settingsRepository,
                    )
                }
                factory { ReportUserPresenter(mainPresenter, userProfileServiceFacade) }
            },
        )

    override fun onKoinReady() {
        channels = MutableStateFlow(emptyList())
        ignoredProfileIds = MutableStateFlow(emptySet())

        privateChatServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        // Explicit rather than relaxed: `loadReputation` reads this to tell an unresolved score apart
        // from a real zero, and a relaxed mock cannot fabricate the map.
        reputationServiceFacade = mockk(relaxed = true) { every { scoreByUserProfileId } returns MutableStateFlow(emptyMap()) }
        notificationController = mockk(relaxed = true)
        settingsRepository = SettingsRepositoryMock()
        mainPresenter = mockk(relaxed = true)

        every { privateChatServiceFacade.channels } returns channels
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        coEvery { reputationServiceFacade.getReputation(any()) } returns Result.failure(IllegalStateException("none"))
    }

    private fun renderScreen() {
        setInjectTestContent { PrivateChatScreen(CHANNEL_ID) }
    }

    private fun givenChannel() {
        channels.value =
            listOf(
                TwoPartyPrivateChatChannel(
                    id = CHANNEL_ID,
                    chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
                    peer = peer,
                    myUserProfile = me,
                ),
            )
    }

    @Test
    fun `when the conversation is empty then the hint and the peer header are shown`() =
        runTest {
            givenChannel()

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.privateChats.chat.emptyHint".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.privateChats.peer.header".i18n(PEER_NAME)).assertIsDisplayed()
        }

    @Test
    fun `when the channel is unknown then the not-found dialog is shown`() =
        runTest {
            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.privateChats.chat.notFound".i18n()).assertIsDisplayed()
        }

    @Test
    fun `when leaving is confirmed then the channel is left`() =
        runTest {
            givenChannel()
            coEvery { privateChatServiceFacade.leaveChannel(CHANNEL_ID) } returns Result.success(Unit)

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag(LEAVE_BUTTON).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            coVerify(exactly = 1) { privateChatServiceFacade.leaveChannel(CHANNEL_ID) }
        }

    @Test
    fun `when leaving is cancelled then nothing is left`() =
        runTest {
            givenChannel()

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag(LEAVE_BUTTON).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).performClick()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            coVerify(exactly = 0) { privateChatServiceFacade.leaveChannel(any()) }
        }

    @Test
    fun `when the thread is opened then it is marked read`() =
        runTest {
            givenChannel()

            renderScreen()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            coVerify(exactly = 1) { privateChatServiceFacade.consumeNotifications(CHANNEL_ID) }
        }
}
