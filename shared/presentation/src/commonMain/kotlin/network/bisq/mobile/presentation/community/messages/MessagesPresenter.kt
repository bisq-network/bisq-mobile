package network.bisq.mobile.presentation.community.messages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.reputation.observeReputation
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Messages tab of the Community hub (#1825): the private chats inbox. Renders straight from
 * [PrivateChatServiceFacade.channels] — never a navigation-time snapshot — so a channel gained
 * (incoming DM) or left elsewhere is already reflected here on back-navigation. The detail
 * screen, facades and notifications shipped with #1750; this presenter only lists and routes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesPresenter(
    mainPresenter: MainPresenter,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.CommunityMessages

    // Seeded synchronously from the facade's CURRENT value so a revisit doesn't flash the empty
    // state (same idiom as ContactsPresenter). Reputation needs a suspend resolve, so seeds carry
    // rating 0.0 and the collector below refines them within its first emission.
    private val _uiState =
        MutableStateFlow(
            PrivateChatListUiState(
                conversations =
                    privateChatServiceFacade.channels.value
                        .map { it.toItemSnapshot() }
                        .sortedNewestFirst(),
                isLoading = privateChatServiceFacade.channels.value.isEmpty(),
            ),
        )
    val uiState: StateFlow<PrivateChatListUiState> = _uiState.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        privateChatServiceFacade.channels
            .flatMapLatest { channels ->
                if (channels.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(channels.map { it.itemFlow() }) { it.toList() }
                }
            }.map { items -> PrivateChatListUiState(conversations = items.sortedNewestFirst(), isLoading = false) }
            .onEach { _uiState.value = it }
            .launchIn(presenterScope)
    }

    fun onAction(action: PrivateChatListUiAction) {
        when (action) {
            is PrivateChatListUiAction.OnConversationClick -> navigateTo(NavRoute.PrivateChat(action.channelId))
            PrivateChatListUiAction.OnBrowseOfferbookClick -> navigateToTab(NavRoute.TabOfferbookMarket)
        }
    }

    /** A row stays live per channel: message set, unread count and reputation each push updates. */
    private fun TwoPartyPrivateChatChannel.itemFlow() =
        combine(
            chatMessages,
            unreadCount,
            reputationServiceFacade.observeReputation(peer.id),
        ) { messages, unread, reputation ->
            toItem(
                latestMessage = messages.maxByOrNull { it.date },
                unread = unread,
                starRating = reputation?.fiveSystemScore ?: 0.0,
            )
        }

    private fun TwoPartyPrivateChatChannel.toItemSnapshot(): PrivateChatListItemUiState =
        toItem(
            latestMessage = chatMessages.value.maxByOrNull { it.date },
            unread = unreadCount.value,
            starRating = 0.0,
        )

    private fun TwoPartyPrivateChatChannel.toItem(
        latestMessage: TwoPartyPrivateChatMessage?,
        unread: Long,
        starRating: Double,
    ): PrivateChatListItemUiState =
        PrivateChatListItemUiState(
            channelId = id,
            peerProfile = peer,
            starRating = starRating,
            lastMessagePreview =
                latestMessage?.let { message ->
                    if (message.isMyMessage) {
                        "mobile.privateChats.list.previewMine".i18n(message.decodedText)
                    } else {
                        message.decodedText
                    }
                } ?: "",
            lastMessageTimeLabel = latestMessage?.let { DateUtils.lastSeen(it.date) } ?: "",
            lastMessageEpochMillis = latestMessage?.date ?: 0L,
            unreadCount = unread,
        )

    private fun List<PrivateChatListItemUiState>.sortedNewestFirst() = sortedByDescending { it.lastMessageEpochMillis }
}
