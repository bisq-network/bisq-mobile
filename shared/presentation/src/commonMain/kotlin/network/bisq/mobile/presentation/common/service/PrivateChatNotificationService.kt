package network.bisq.mobile.presentation.common.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationChannels
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.common.notification.model.android.AndroidNotificationCategory
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import kotlin.concurrent.Volatile

/**
 * Posts a local notification when a private chat (DM) receives a message while the app is
 * backgrounded.
 *
 * A thin bridge, not a counter: Bisq 2's `ChatNotificationService` already filters own messages,
 * ignored senders and stale messages, and persists what has been consumed. This service only watches
 * the resulting per-channel unread count and turns an increase into a notification.
 */
@OptIn(FlowPreview::class)
class PrivateChatNotificationService(
    private val notificationController: NotificationController,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    private val appForegroundController: ForegroundDetector,
    // Injectable so tests can drive the debounce and the observers on their virtual-time dispatcher.
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Logging {
    private companion object {
        const val FOREGROUND_DEBOUNCE_MS = 1000L
    }

    /**
     * Deliberately NOT routed through `ForegroundServiceController`.
     *
     * That controller is a Koin singleton whose `unregisterObservers()` cancels *every* registered job
     * process-wide, and `OpenTradesNotificationService` calls it on several of its own transitions.
     * Registering DM observers there would let the trade service silently kill them while this service
     * still believed they were live. The process is kept alive by the foreground service the trade
     * service already starts, so an ordinary scope is enough.
     */
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var lifecycleObserverJob: Job? = null
    private var observerJob: Job? = null

    private val unreadCountByChannelId = mutableMapOf<String, Long>()
    private val stateMutex = Mutex()

    @Volatile
    private var isLocalDeliverySuppressed = false

    init {
        setupLifecycleObserver()
    }

    /**
     * Arms the foreground/background observer. Called from `init` and again from the lifecycle
     * services' `activateServiceFacades()`, because [stopNotificationService] is followed by a fresh
     * `activate()` on the trigger-full-lifecycle-restart path. Idempotent.
     */
    fun startService() {
        setupLifecycleObserver()
    }

    /**
     * `false` is a flag flip only — the lifecycle observer re-registers on the next background
     * transition. Matches `OpenTradesNotificationService`, and costs nothing in practice: the push
     * opt-in that drives this can only be changed from the UI, so the app is foregrounded when it
     * flips and observers would be unregistered either way.
     */
    fun setLocalDeliverySuppressed(suppressed: Boolean) {
        if (isLocalDeliverySuppressed == suppressed) return
        isLocalDeliverySuppressed = suppressed
        if (suppressed) {
            log.i { "Suppressing local DM notifications — unregistering observers" }
            unregisterObservers()
        }
    }

    /**
     * Stops observing but leaves [scope] alive, so a later [startService] works. Cancelling the scope
     * would make this Koin singleton permanently dead after a deactivate/activate cycle; between
     * cycles the scope holds a `SupervisorJob` with no children, which is essentially free.
     */
    suspend fun stopNotificationService() {
        log.d { "Stopping PrivateChatNotificationService." }
        lifecycleObserverJob?.cancel()
        lifecycleObserverJob = null
        unregisterObservers()
        stateMutex.withLock { unreadCountByChannelId.clear() }
    }

    // Private

    private fun setupLifecycleObserver() {
        if (lifecycleObserverJob?.isActive == true) return

        lifecycleObserverJob =
            appForegroundController.isForeground
                // Ahead of the debounce, deliberately. The snapshot has to describe the moment the app
                // went away: taken a debounce later, a DM arriving in between is folded into "already
                // seen" and never notified at all. Re-snapshotting here rather than relying on the one
                // taken on the way in is also what reflects a conversation left while the app was open.
                .onEach { isForeground ->
                    if (!isForeground && !isLocalDeliverySuppressed) markCurrentCountsAsSeen()
                }
                // Behind it: arming and disarming the per-channel collectors is the part that must not
                // churn on a rapid foreground flip.
                .debounce(FOREGROUND_DEBOUNCE_MS)
                .distinctUntilChanged()
                .onEach { isForeground ->
                    if (isForeground) {
                        unregisterObservers()
                        markCurrentCountsAsSeen()
                    } else if (!isLocalDeliverySuppressed) {
                        registerObservers()
                    }
                }.launchIn(scope)
    }

    /**
     * `collectLatest`, not `collect`: `channels` re-emits on every unread-count change on the client,
     * and a plain `collect` would launch a second set of per-channel collectors each time without
     * cancelling the first — an unbounded leak for as long as the app stays backgrounded.
     */
    private fun registerObservers() {
        if (observerJob?.isActive == true) return
        observerJob =
            scope.launch {
                privateChatServiceFacade.channels.collectLatest { channels ->
                    coroutineScope {
                        channels.forEach { channel ->
                            launch {
                                channel.unreadCount.collect { unreadCount ->
                                    onUnreadCountChanged(channel.id, channel.peer.userName, unreadCount)
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * The counts the user has now seen, recorded when the app comes to the foreground. Without this
     * [onUnreadCountChanged] compares against a default of zero, so the first emission after the app
     * is backgrounded reads as an increase and notifies for messages the user already read.
     *
     * Replaces the map rather than merging into it. A two-party channel id is derived from the two
     * profile ids, so leaving a conversation and being messaged again re-creates it under the *same*
     * id, starting from an unread count of 1. A merged map would still hold the high-water mark from
     * before, `1 > 5` is false, and that peer's messages would stop notifying for good.
     */
    private suspend fun markCurrentCountsAsSeen() {
        val current = privateChatServiceFacade.channels.value.associate { it.id to it.unreadCount.value }
        stateMutex.withLock {
            unreadCountByChannelId.clear()
            unreadCountByChannelId.putAll(current)
        }
    }

    private fun unregisterObservers() {
        observerJob?.cancel()
        observerJob = null
    }

    /**
     * Notifies only on an increase over what the user has seen (see [markCurrentCountsAsSeen]). Bisq
     * 2 lowers the count when the conversation is consumed, and re-notifying then would resurrect a
     * notification the user just dismissed by reading.
     */
    private suspend fun onUnreadCountChanged(
        chatChannelId: String,
        peerUserName: String,
        unreadCount: Long,
    ) {
        val shouldNotify =
            stateMutex.withLock {
                val previous = unreadCountByChannelId[chatChannelId] ?: 0L
                unreadCountByChannelId[chatChannelId] = unreadCount
                unreadCount > previous
            }
        if (!shouldNotify) return

        notificationController.notify {
            this.id = NotificationIds.getNewPrivateChatMessageId(chatChannelId)
            this.title = "mobile.privateChatNotifications.newMessage.title".i18n()
            this.body = "mobile.privateChatNotifications.newMessage.message".i18n(peerUserName)
            android {
                channelId = NotificationChannels.USER_MESSAGES
                category = AndroidNotificationCategory.CATEGORY_MESSAGE
                lockScreen = NotificationRedactions.chatMessage()
                pressAction = NotificationPressAction.Route(NavRoute.PrivateChat(chatChannelId))
                group = chatChannelId
            }
            ios {
                pressAction = NotificationPressAction.Route(NavRoute.PrivateChat(chatChannelId))
            }
        }
    }
}
