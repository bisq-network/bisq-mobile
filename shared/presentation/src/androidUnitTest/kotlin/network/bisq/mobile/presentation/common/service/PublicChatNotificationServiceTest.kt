package network.bisq.mobile.presentation.common.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.model.CommunityNotificationLevel
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationBuilder
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * The Community notifications preference (#1812) applied to the public channels: ALL notifies on any
 * unread increase, MENTIONS_AND_REPLIES only when the fresh messages mention me (`@userName`, desktop
 * semantics) or cite one of my messages, OFF never arms observers at all. Structure mirrors
 * [PrivateChatNotificationServiceTest]: unread-count deltas with a seen-baseline, so channel history
 * (10-day TTL replay) never storms on a cold start — at most one notification per channel burst.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PublicChatNotificationServiceTest : PresentationKoinTestBase() {
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val publicChatServiceFacade: PublicChatServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val appForegroundController: ForegroundDetector = mockk(relaxed = true)

    private val me = createMockUserProfile("Alice")
    private val mySecondProfile = createMockUserProfile("AliceTrading")
    private val peer = createMockUserProfile("Bob")
    private val myProfiles = MutableStateFlow(listOf(me))

    private val channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())
    private val isForeground = MutableStateFlow(true)
    private lateinit var settingsRepository: SettingsRepositoryMock

    private var notifyCount = 0
    private var lastConfig: NotificationConfig? = null

    private lateinit var service: PublicChatNotificationService

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        notifyCount = 0
        lastConfig = null
        every { publicChatServiceFacade.channels } returns channels
        every { appForegroundController.isForeground } returns isForeground
        myProfiles.value = listOf(me)
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(me)
        every { userProfileServiceFacade.userProfiles } returns myProfiles
        coEvery { userProfileServiceFacade.getUserIdentityIds() } returns listOf(me.id)
        every { notificationController.notify(any<NotificationBuilder.() -> Unit>()) } answers {
            notifyCount++
            lastConfig = NotificationBuilder().apply(firstArg<NotificationBuilder.() -> Unit>()).build()
        }
    }

    private fun startService(level: CommunityNotificationLevel = CommunityNotificationLevel.ALL) {
        settingsRepository = SettingsRepositoryMock(Settings(communityNotificationLevel = level))
        service =
            PublicChatNotificationService(
                notificationController,
                publicChatServiceFacade,
                userProfileServiceFacade,
                settingsRepository,
                appForegroundController,
                testDispatcher,
            )
        service.startService()
    }

    private fun channel(
        domain: ChatChannelDomainEnum,
        unread: Long = 0,
    ) = CommonPublicChatChannel(
        id = "${domain.name.lowercase()}.channel",
        chatChannelDomain = domain,
        channelTitle = domain.name.lowercase(),
    ).apply { setUnreadCount(unread) }

    private fun CommonPublicChatChannel.newMessage(
        text: String,
        citationOfMine: Boolean = false,
    ) {
        addChatMessage(
            createMockCommonPublicChatMessage(
                id = "msg-${chatMessages.value.size}-$id",
                text = text,
                citation = if (citationOfMine) Citation(authorUserProfileId = me.id, text = "quoted", chatMessageId = null) else null,
                senderUserProfile = peer,
                myUserProfile = me,
            ),
        )
        setUnreadCount(unreadCount.value + 1)
    }

    private suspend fun TestScope.goForeground() {
        isForeground.value = true
        advanceUntilIdle()
    }

    private suspend fun TestScope.goBackground() {
        isForeground.value = false
        advanceUntilIdle()
    }

    @Test
    fun `level all notifies once when a channel gains unread while backgrounded`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.ALL)
            goForeground()
            goBackground()

            discussions.newMessage("what about fees")
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    /** The cold-start storm guard: history present before backgrounding is seen state and never notifies. */
    @Test
    fun `unread history seen before backgrounding does not notify`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION, unread = 47)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.ALL)
            goForeground()
            goBackground()
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }

    @Test
    fun `level off never arms and never notifies`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.OFF)
            goForeground()
            goBackground()

            discussions.newMessage("hello @${me.userName}")
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }

    @Test
    fun `mentions level ignores a plain message`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.MENTIONS_AND_REPLIES)
            goForeground()
            goBackground()

            discussions.newMessage("nothing about anyone")
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }

    @Test
    fun `mentions level notifies on a mention of my userName`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.MENTIONS_AND_REPLIES)
            goForeground()
            goBackground()

            discussions.newMessage("hey @${me.userName} what do you think")
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    /** Desktop checks all identities — a mention of any owned profile qualifies, not only the selected one. */
    @Test
    fun `mentions level notifies on a mention of a non selected owned profile`() =
        runTest {
            myProfiles.value = listOf(me, mySecondProfile)
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.MENTIONS_AND_REPLIES)
            goForeground()
            goBackground()

            discussions.newMessage("ping @${mySecondProfile.userName} are you around")
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    @Test
    fun `mentions level notifies on a reply citing my message`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.MENTIONS_AND_REPLIES)
            goForeground()
            goBackground()

            discussions.newMessage("I disagree", citationOfMine = true)
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    @Test
    fun `a discussions notification routes to the hub and a support one to the support channel`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            val support = channel(ChatChannelDomainEnum.SUPPORT)
            channels.value = listOf(discussions, support)
            startService(CommunityNotificationLevel.ALL)
            goForeground()
            goBackground()

            discussions.newMessage("fee talk")
            advanceUntilIdle()
            val discussionsAction = assertNotNull(assertNotNull(lastConfig).android).pressAction
            val discussionsRoute = assertIs<NotificationPressAction.Route>(discussionsAction)
            assertIs<NavRoute.CommunityHub>(discussionsRoute.route)

            support.newMessage("need help")
            advanceUntilIdle()
            val supportAction = assertNotNull(assertNotNull(lastConfig).android).pressAction
            val supportRoute = assertIs<NotificationPressAction.Route>(supportAction)
            assertIs<NavRoute.SupportChannel>(supportRoute.route)
        }

    @Test
    fun `flipping the level to off mid session disarms armed observers`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.ALL)
            goForeground()
            goBackground()

            settingsRepository.setCommunityNotificationLevel(CommunityNotificationLevel.OFF)
            advanceUntilIdle()

            discussions.newMessage("after the flip")
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }

    /** The badge pipeline is read-only to this service: notifying must never move a channel's unread count. */
    @Test
    fun `the service never writes the channel unread count`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.ALL)
            goForeground()
            goBackground()

            discussions.newMessage("fee talk")
            advanceUntilIdle()

            assertEquals(1, notifyCount)
            assertEquals(1L, discussions.unreadCount.value, "only the test wrote the count; the service must not")
        }

    @Test
    fun `no notifications while the app is in the foreground`() =
        runTest {
            val discussions = channel(ChatChannelDomainEnum.DISCUSSION)
            channels.value = listOf(discussions)
            startService(CommunityNotificationLevel.ALL)
            goForeground()

            discussions.newMessage("seen live on screen")
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }
}
