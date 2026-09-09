package network.bisq.mobile.domain.service.community

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.CommunityNotificationLevel
import network.bisq.mobile.data.replicated.chat.ChatChannel
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.mentionsOrCites
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import kotlin.concurrent.Volatile

/**
 * Feeds the Community hub's entry-point badge from the public chat channels — the producer
 * [CommunityHubService.unreadCount] has been waiting for since #1743 shipped the slot — plus the
 * private chat channels behind the Messages segment (#1825).
 *
 * Two rules here are load-bearing, not defensive:
 *  - **Support is excluded.** The facade serves both domains, and the hub's aggregate is a strict
 *    Discussions + Messages sum by design (#1746).
 *  - **Each addend is gated on its segment being live.** The hub's icon appears whenever *any*
 *    segment is live, so without this a release build would badge the icon for a segment the user
 *    cannot open.
 *
 * Domain rather than presentation because both collaborators are domain and it touches no UI; and it
 * has to outlive the segment, since the badge shows on every main tab while the hub presenter is a
 * factory bound to a mounted tab. [CommunityHubService.setUnreadCount] stays the single write seam.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityUnreadCountAggregator(
    private val publicChatServiceFacade: PublicChatServiceFacade,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    private val communityHubService: CommunityHubService,
    private val settingsRepository: SettingsRepository,
    /** The user's OWN profiles ([network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade.userProfiles]). */
    private val ownProfiles: StateFlow<List<UserProfileVO>>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile
    private var job: Job? = null

    /**
     * Idempotent, because activation is not a once-per-process call: the lifecycle restart paths
     * (a Tor bootstrap retry, for one) deactivate then activate the same singleton, and a second
     * collector on the same flows would outlive every one of them.
     */
    fun start() {
        if (job?.isActive == true) {
            return
        }
        job =
            scope.launch {
                combine(
                    communityHubService.liveSegments,
                    discussionBadgeCount(),
                    privateChatServiceFacade.channels.flatMapLatest { unreadCountSum(it) },
                ) { liveSegments, discussionCount, messagesCount ->
                    // A gated segment is ABSENT from the map, not zero: its tab is not rendered, so
                    // it must not badge the entry icon either. The channel counts are Longs and the
                    // badge is an Int, and an unchecked toInt() wraps in both directions: a large
                    // positive to a negative, and a large negative back to a positive that the
                    // hub's own clamp then lets through as a false maximum. Hence a two-sided
                    // clamp per segment rather than a ceiling.
                    buildMap {
                        if (CommunitySegment.DISCUSSIONS in liveSegments) {
                            put(CommunitySegment.DISCUSSIONS, discussionCount.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                        }
                        if (CommunitySegment.MESSAGES in liveSegments) {
                            put(CommunitySegment.MESSAGES, messagesCount.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                        }
                    }
                }.collect { counts ->
                    // One write for map and aggregate: the service derives the sum, so the entry
                    // badge and the tab pills can never disagree.
                    communityHubService.setUnreadCounts(counts)
                }
            }
    }

    /**
     * Clears the badge as it stops: with no producer left, a stale count would sit there.
     *
     * Joins rather than only cancelling, because the collector runs on its own dispatcher while this
     * is called from the lifecycle's: an emission already inside [CommunityHubService.setUnreadCount]
     * would otherwise land after the clear and freeze the badge on exactly the stale count.
     */
    suspend fun stop() {
        job?.cancelAndJoin()
        job = null
        communityHubService.setUnreadCounts(emptyMap())
    }

    /**
     * The Discussions addend honors the Community notifications preference, matching desktop,
     * where the nav badges count notifications and notifications respect the ALL / MENTION / OFF
     * setting: ALL badges every unread message, MENTIONS_AND_REPLIES only the unread messages that
     * mention one of the user's own profiles or cite one of their messages, OFF none. The Messages
     * addend stays unfiltered — the preference is scoped to the public channels.
     */
    private fun discussionBadgeCount(): Flow<Long> =
        combine(
            settingsRepository.data.map { it.communityNotificationLevel }.distinctUntilChanged(),
            ownProfiles,
            publicChatServiceFacade.channels,
        ) { level, profiles, channels ->
            Triple(level, profiles, channels.filter { it.chatChannelDomain == ChatChannelDomainEnum.DISCUSSION })
        }.flatMapLatest { (level, profiles, discussions) ->
            when (level) {
                CommunityNotificationLevel.ALL -> unreadCountSum(discussions)
                CommunityNotificationLevel.MENTIONS_AND_REPLIES -> mentionUnreadCountSum(discussions, profiles)
                CommunityNotificationLevel.OFF -> flowOf(0L)
            }
        }

    /**
     * "Unread and about me", approximated as the newest [ChatChannel.unreadCount] messages that
     * qualify — the same newest-N reading `PublicChatNotificationService` applies, valid because the
     * persisted unread counter tracks the tail of the channel.
     */
    private fun mentionUnreadCountSum(
        channels: List<CommonPublicChatChannel>,
        myProfiles: List<UserProfileVO>,
    ): Flow<Long> {
        if (channels.isEmpty()) {
            return flowOf(0L)
        }
        return combine(
            channels.map { channel ->
                combine(channel.unreadCount, channel.chatMessages) { unread, messages ->
                    if (unread <= 0L) {
                        0L
                    } else {
                        messages
                            .sortedByDescending { it.date }
                            .take(unread.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                            .count { !it.isMyMessage && it.mentionsOrCites(myProfiles) }
                            .toLong()
                    }
                }
            },
        ) { counts -> counts.sum() }
    }

    private fun unreadCountSum(channels: List<ChatChannel<*>>): Flow<Long> {
        if (channels.isEmpty()) {
            return flowOf(0L)
        }
        return combine(channels.map { it.unreadCount }) { counts -> counts.sum() }
    }
}
