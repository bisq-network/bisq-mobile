package network.bisq.mobile.presentation.common.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.model.CommunityNotificationLevel
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.mentionsOrCites
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationChannels
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.common.notification.model.android.AndroidNotificationCategory
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

/**
 * Community notifications for the PUBLIC channels (Discussions and Support), governed by the global
 * [CommunityNotificationLevel] preference (#1812). Structure mirrors [PrivateChatNotificationService]
 * deliberately — unread-count deltas over a seen-baseline, so the channels' replayed history (10-day
 * P2P TTL) never storms on a cold start: a burst is at most one notification per channel.
 *
 * This service only READS the channel flows. The hub badge pipeline (channel unread →
 * CommunityUnreadCountAggregator → CommunityHubService) has no writer here and cannot be disturbed.
 *
 * MENTIONS_AND_REPLIES classifies the burst's newest messages with the shared desktop-semantics
 * predicate [mentionsOrCites]; OFF never arms observers at all.
 */
@OptIn(FlowPreview::class)
class PublicChatNotificationService(
    private val notificationController: NotificationController,
    private val publicChatServiceFacade: PublicChatServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val appForegroundController: ForegroundDetector,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Logging {
    private companion object {
        const val FOREGROUND_DEBOUNCE_MS = 1000L
    }

    // Same isolation rationale as PrivateChatNotificationService: never routed through
    // ForegroundServiceController, whose process-wide unregisterObservers() the trade service calls.
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var lifecycleObserverJob: Job? = null
    private var levelObserverJob: Job? = null

    // Read and written only under [jobMutex]; see the private sibling for why @Volatile is not enough.
    private var observerJob: Job? = null

    private val unreadCountByChannelId = mutableMapOf<String, Long>()
    private val stateMutex = Mutex()
    private val jobMutex = Mutex()

    @kotlin.concurrent.Volatile
    private var isLocalDeliverySuppressed = false

    @kotlin.concurrent.Volatile
    private var currentLevel: CommunityNotificationLevel = CommunityNotificationLevel.OFF

    @kotlin.concurrent.Volatile
    private var isForegroundNow = true

    fun startService() {
        setupLifecycleObserver()
        setupLevelObserver()
    }

    fun setLocalDeliverySuppressed(suppressed: Boolean) {
        if (isLocalDeliverySuppressed == suppressed) return
        isLocalDeliverySuppressed = suppressed
        if (suppressed) {
            log.i { "Suppressing local community notifications — unregistering observers" }
            scope.launch { unregisterObservers() }
        }
    }

    suspend fun stopNotificationService() {
        log.d { "Stopping PublicChatNotificationService." }
        lifecycleObserverJob?.cancelAndJoin()
        lifecycleObserverJob = null
        levelObserverJob?.cancelAndJoin()
        levelObserverJob = null
        unregisterObservers()
        stateMutex.withLock { unreadCountByChannelId.clear() }
    }

    private fun setupLifecycleObserver() {
        if (lifecycleObserverJob?.isActive == true) return

        lifecycleObserverJob =
            appForegroundController.isForeground
                .onEach { isForeground ->
                    isForegroundNow = isForeground
                    // Seen-snapshot ahead of the debounce, like the sibling: a message landing inside
                    // the window must still notify.
                    if (!isForeground && deliveryArmed()) markCurrentCountsAsSeen()
                }.debounce(FOREGROUND_DEBOUNCE_MS)
                .distinctUntilChanged()
                .onEach { isForeground ->
                    if (isForeground) {
                        unregisterObservers()
                        markCurrentCountsAsSeen()
                    } else if (deliveryArmed()) {
                        registerObservers()
                    }
                }.launchIn(scope)
    }

    /**
     * The preference is live: OFF disarms observers that are already running, and leaving OFF while
     * backgrounded arms them — no app restart needed for the setting to take effect.
     */
    private fun setupLevelObserver() {
        if (levelObserverJob?.isActive == true) return

        levelObserverJob =
            settingsRepository.data
                .map { it.communityNotificationLevel }
                .distinctUntilChanged()
                .onEach { level ->
                    currentLevel = level
                    if (level == CommunityNotificationLevel.OFF) {
                        unregisterObservers()
                    } else if (!isForegroundNow && deliveryArmed()) {
                        markCurrentCountsAsSeen()
                        registerObservers()
                    }
                }.launchIn(scope)
    }

    private fun deliveryArmed(): Boolean = !isLocalDeliverySuppressed && currentLevel != CommunityNotificationLevel.OFF

    private suspend fun registerObservers() {
        jobMutex.withLock {
            if (!deliveryArmed()) return@withLock
            if (observerJob?.isActive == true) return@withLock
            observerJob =
                scope.launch {
                    publicChatServiceFacade.channels.collectLatest { channels ->
                        coroutineScope {
                            channels.forEach { channel ->
                                launch {
                                    channel.unreadCount.collect { unreadCount ->
                                        onUnreadCountChanged(channel, unreadCount)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private suspend fun markCurrentCountsAsSeen() {
        val current = publicChatServiceFacade.channels.value.associate { it.id to it.unreadCount.value }
        stateMutex.withLock {
            unreadCountByChannelId.clear()
            unreadCountByChannelId.putAll(current)
        }
    }

    private suspend fun unregisterObservers() {
        jobMutex.withLock {
            observerJob?.cancel()
            observerJob = null
        }
    }

    private suspend fun onUnreadCountChanged(
        channel: CommonPublicChatChannel,
        unreadCount: Long,
    ) {
        val previous =
            stateMutex.withLock {
                val previous = unreadCountByChannelId[channel.id] ?: 0L
                unreadCountByChannelId[channel.id] = unreadCount
                previous
            }
        if (unreadCount <= previous) return
        if (!burstQualifies(channel, (unreadCount - previous).toInt())) return

        val isSupport = channel.chatChannelDomain == ChatChannelDomainEnum.SUPPORT
        val channelName =
            if (isSupport) "mobile.communityNotifications.channel.support".i18n() else "mobile.communityNotifications.channel.discussions".i18n()
        val route =
            if (isSupport) {
                NavRoute.SupportChannel
            } else {
                NavRoute.CommunityHub(initialSegment = CommunitySegment.DISCUSSIONS.name)
            }
        notificationController.notify {
            this.id = NotificationIds.getNewPublicChatMessageId(channel.id)
            this.title = "mobile.communityNotifications.newMessage.title".i18n(channelName)
            this.body = "mobile.communityNotifications.newMessage.message".i18n(channelName)
            android {
                channelId = NotificationChannels.USER_MESSAGES
                category = AndroidNotificationCategory.CATEGORY_MESSAGE
                lockScreen = NotificationRedactions.chatMessage()
                pressAction = NotificationPressAction.Route(route)
                group = NotificationIds.getNewPublicChatMessageId(channel.id)
            }
            ios {
                categoryId = NotificationRedactions.CHAT_MESSAGE_CATEGORY
                pressAction = NotificationPressAction.Route(route)
            }
        }
    }

    /**
     * ALL passes every burst. MENTIONS_AND_REPLIES inspects the burst's newest [delta] messages with
     * the shared desktop-semantics predicate — a mention of any of my profiles or a citation of one
     * of my messages (checked against ALL identity ids, like desktop checks all identities).
     */
    private suspend fun burstQualifies(
        channel: CommonPublicChatChannel,
        delta: Int,
    ): Boolean {
        if (currentLevel == CommunityNotificationLevel.ALL) return true

        val myProfiles = listOfNotNull(userProfileServiceFacade.selectedUserProfile.value)
        val myIdentityIds = runCatching { userProfileServiceFacade.getUserIdentityIds() }.getOrDefault(emptyList())
        return channel.chatMessages.value
            .sortedByDescending { it.date }
            .take(delta.coerceAtLeast(1))
            .any { message ->
                !message.isMyMessage &&
                    (message.mentionsOrCites(myProfiles) || message.citation?.authorUserProfileId in myIdentityIds.toSet())
            }
    }
}
