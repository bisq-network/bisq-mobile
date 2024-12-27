package network.bisq.mobile.android.node.service.offerbook.offer

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.OfferListItemMapping
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.utils.Logging
import java.util.Optional


class NodeOfferbookListItemService(private val applicationService: AndroidApplicationService.Provider) :
    LifeCycleAware, Logging {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }
    private val userProfileService: UserProfileService by lazy {
        applicationService.userService.get().userProfileService
    }
    private val userIdentityService: UserIdentityService by lazy {
        applicationService.userService.get().userIdentityService
    }
    private val reputationService: ReputationService by lazy {
        applicationService.userService.get().reputationService
    }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService
    }

    // Properties
    private val _offerListItems = MutableStateFlow<List<OfferListItemVO>>(emptyList())
    val offerListItems: StateFlow<List<OfferListItemVO>> get() = _offerListItems

    private val bisqEasyOfferbookMessages: MutableSet<BisqEasyOfferbookMessage> = mutableSetOf()

    // Misc
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null


    // Life cycle
    override fun activate() {
        addSelectedChannelObservers()
    }

    override fun deactivate() {
        chatMessagesPin?.unbind()
        chatMessagesPin = null

        selectedChannelPin?.unbind()
        selectedChannelPin = null
    }

    fun findBisqEasyOfferbookMessage(offer: OfferListItemVO): Optional<BisqEasyOfferbookMessage> =
        bisqEasyOfferbookMessages.stream()
            .filter { it.hasBisqEasyOffer() }
            .filter { it.bisqEasyOffer.get().id.equals(offer.bisqEasyOffer.id) }
            .findAny()

    // Private
    private fun addSelectedChannelObservers() {
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
                if (channel is BisqEasyOfferbookChannel) {
                    addChatMessagesObservers(channel)
                }
            }
    }

    private fun createOfferListItemVO(bisqEasyOfferbookMessage: BisqEasyOfferbookMessage): OfferListItemVO {
        return OfferListItemMapping.createOfferListItemVO(
            userProfileService,
            userIdentityService,
            reputationService,
            marketPriceService,
            bisqEasyOfferbookMessage
        )
    }

    private fun addChatMessagesObservers(channel: BisqEasyOfferbookChannel) {
        chatMessagesPin?.unbind()
        _offerListItems.value = emptyList()

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = channel.chatMessages
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (message.hasBisqEasyOffer()) {
                        val offerListItem: OfferListItemVO = createOfferListItemVO(message)
                        _offerListItems.value = _offerListItems.value + offerListItem
                        bisqEasyOfferbookMessages.add(message)
                        log.i { "add offer $offerListItem" }
                    }
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.hasBisqEasyOffer()) {
                        val offerListItem =
                            _offerListItems.value.first { it.bisqEasyOffer.id == message.bisqEasyOffer.get().id }
                        _offerListItems.value = _offerListItems.value - offerListItem
                        bisqEasyOfferbookMessages.remove(message)
                        log.i { "remove offer $offerListItem" }
                    }
                }

                override fun clear() {
                    _offerListItems.value = emptyList()
                    bisqEasyOfferbookMessages.clear()
                }
            })
    }
}






