package network.bisq.mobile.presentation.common.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.NotificationBuilder
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class PrivateChatNotificationServiceTest : PresentationKoinTestBase() {
    private companion object {
        const val CHANNEL_ID = "discussion.a-b"

        /** Mirrors the service's own `FOREGROUND_DEBOUNCE_MS`, which is private. */
        const val FOREGROUND_DEBOUNCE_MS = 1000L
    }

    private val notificationController: NotificationController = mockk(relaxed = true)
    private val privateChatServiceFacade: PrivateChatServiceFacade = mockk(relaxed = true)
    private val appForegroundController: ForegroundDetector = mockk(relaxed = true)

    private val channels = MutableStateFlow<List<TwoPartyPrivateChatChannel>>(emptyList())
    private val isForeground = MutableStateFlow(true)

    private var notifyCount = 0
    private var lastConfig: NotificationConfig? = null

    private lateinit var service: PrivateChatNotificationService

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        every { privateChatServiceFacade.channels } returns channels
        every { appForegroundController.isForeground } returns isForeground
        every { notificationController.notify(any<NotificationBuilder.() -> Unit>()) } answers {
            notifyCount++
            // Run the DSL block the same way the real controller does, so what the service actually
            // configured — not just that it called — is assertable.
            lastConfig = NotificationBuilder().apply(firstArg<NotificationBuilder.() -> Unit>()).build()
        }

        service =
            PrivateChatNotificationService(
                notificationController,
                privateChatServiceFacade,
                appForegroundController,
                testDispatcher,
            )
    }

    @Test
    fun `unread messages already seen before backgrounding do not notify`() =
        runTest {
            val channel = channelWithUnread(3)
            channels.value = listOf(channel)
            goForeground()

            goBackground()

            assertEquals(0, notifyCount, "these were read while the app was open")
        }

    @Test
    fun `a message arriving while backgrounded notifies`() =
        runTest {
            val channel = channelWithUnread(3)
            channels.value = listOf(channel)
            goForeground()
            goBackground()

            channel.setUnreadCount(4)
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    /**
     * The snapshot of "already seen" counts is taken the moment the app goes away, ahead of the
     * debounce. Taken a debounce later instead, this message would land inside the window, be folded
     * into the snapshot as already read, and never notify at all.
     */
    @Test
    fun `a message arriving inside the debounce window still notifies`() =
        runTest {
            val channel = channelWithUnread(3)
            channels.value = listOf(channel)
            goForeground()

            isForeground.value = false
            advanceTimeBy(FOREGROUND_DEBOUNCE_MS / 2)
            channel.setUnreadCount(4)
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    @Test
    fun `the notification names the peer but redacts it on the lock screen`() =
        runTest {
            val channel = channelWithUnread(3)
            channels.value = listOf(channel)
            goForeground()
            goBackground()

            channel.setUnreadCount(4)
            advanceUntilIdle()

            val config = assertNotNull(lastConfig, "the service must have posted a notification")
            assertEquals("You received a new message from ${channel.peer.userName}", config.body)
            assertEquals(
                NotificationRedactions.chatMessage(),
                config.android?.lockScreen,
                "the body names the peer, so a secure lock screen must see the summary instead",
            )
        }

    @Test
    fun `consuming the conversation does not re-notify`() =
        runTest {
            val channel = channelWithUnread(0)
            channels.value = listOf(channel)
            goForeground()
            goBackground()
            channel.setUnreadCount(2)
            advanceUntilIdle()

            channel.setUnreadCount(0)
            advanceUntilIdle()

            assertEquals(1, notifyCount, "lowering the count must not resurrect the notification")
        }

    /**
     * `channels` re-emits whenever the facade republishes the list, and the node facade builds a
     * *new* model when a channel is re-added. A plain `collect` leaves the previous per-channel
     * collectors running, so a superseded instance keeps reporting — which is the observable half of
     * the collector leak `collectLatest` fixes. (The count comparison hides duplicate collectors on
     * the *same* instance, so that alone cannot be asserted here.)
     */
    @Test
    fun `a superseded channel instance stops being observed`() =
        runTest {
            val stale = channelWithUnread(0)
            channels.value = listOf(stale)
            goForeground()
            goBackground()

            val fresh = channelWithUnread(0)
            channels.value = listOf(fresh)
            advanceUntilIdle()

            stale.setUnreadCount(5)
            advanceUntilIdle()

            assertEquals(0, notifyCount, "the replaced instance must no longer be observed")
        }

    /**
     * A two-party channel id is derived from the two profile ids, so leaving a conversation and being
     * messaged again re-creates it under the *same* id, counting from 1. If the seen-counts were only
     * ever merged into, the high-water mark from before the channel disappeared would outlive it and
     * `1 > 5` would silence that peer for good.
     */
    @Test
    fun `a conversation left and re-created still notifies`() =
        runTest {
            val original = channelWithUnread(5)
            channels.value = listOf(original)
            goForeground()

            // Leaving the DM removes the channel, and needs the UI — so it happens while foregrounded.
            channels.value = emptyList()
            goBackground()

            channels.value = listOf(channelWithUnread(1))
            advanceUntilIdle()

            assertEquals(1, notifyCount, "the re-created conversation must not inherit the old count")
        }

    @Test
    fun `the service still works after a stop and restart`() =
        runTest {
            val channel = channelWithUnread(0)
            channels.value = listOf(channel)
            goForeground()

            service.stopNotificationService()
            advanceUntilIdle()
            service.startService()
            goForeground()
            goBackground()

            channel.setUnreadCount(1)
            advanceUntilIdle()

            assertEquals(1, notifyCount, "a lifecycle restart must not leave the service dead")
        }

    @Test
    fun `suppressed local delivery posts nothing`() =
        runTest {
            val channel = channelWithUnread(0)
            channels.value = listOf(channel)
            goForeground()
            service.setLocalDeliverySuppressed(true)

            goBackground()
            channel.setUnreadCount(1)
            advanceUntilIdle()

            assertEquals(0, notifyCount, "the relay owns delivery in this mode")
        }

    private suspend fun TestScope.goForeground() {
        isForeground.value = true
        advanceUntilIdle()
    }

    private suspend fun TestScope.goBackground() {
        isForeground.value = false
        advanceUntilIdle()
    }

    private fun channelWithUnread(unreadCount: Long) =
        TwoPartyPrivateChatChannel(
            id = CHANNEL_ID,
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            peer = createMockUserProfile("peer"),
            myUserProfile = createMockUserProfile("me"),
        ).apply { setUnreadCount(unreadCount) }
}
