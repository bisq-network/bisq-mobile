package network.bisq.mobile.android.node.domain.offerbook.offer

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.currency.Market
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.common.util.StringUtils
import bisq.i18n.Res
import bisq.offer.Direction
import bisq.offer.amount.OfferAmountFormatter
import bisq.offer.amount.spec.AmountSpec
import bisq.offer.amount.spec.RangeAmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.payment_method.PaymentMethodSpecUtil
import bisq.offer.price.spec.PriceSpec
import bisq.offer.price.spec.PriceSpecFormatter
import bisq.presentation.formatters.DateFormatter
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import com.google.common.base.Joiner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.offerbook.OfferListItem
import network.bisq.mobile.utils.Logging
import java.text.DateFormat
import java.util.Date
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
    private val _offerListItems = MutableStateFlow<List<OfferListItem>>(emptyList())
    val offerListItems: StateFlow<List<OfferListItem>> get() = _offerListItems

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

    // Private
    private fun addSelectedChannelObservers() {
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
                if (channel is BisqEasyOfferbookChannel) {
                    addChatMessagesObservers(channel)
                }
            }
    }

    private fun addChatMessagesObservers(marketChannel: BisqEasyOfferbookChannel) {
        chatMessagesPin?.unbind()
        _offerListItems.value = emptyList()

        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = marketChannel.chatMessages
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (message.hasBisqEasyOffer()) {
                        val offerListItem: OfferListItem = createOfferItem(message)
                        _offerListItems.value = _offerListItems.value + offerListItem
                        log.i { "add offer $offerListItem" }
                    }
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.hasBisqEasyOffer()) {
                        val offerListItem =
                            _offerListItems.value.first { it.messageId == message.id }
                        _offerListItems.value = _offerListItems.value - offerListItem
                        log.i { "remove offer $offerListItem" }
                    }
                }

                override fun clear() {
                    _offerListItems.value = emptyList()
                }
            })
    }

    private fun createOfferItem(message: BisqEasyOfferbookMessage): OfferListItem {
        val bisqEasyOffer: BisqEasyOffer = message.bisqEasyOffer.get()
        val date = message.date
        val formattedDate = DateFormatter.formatDateTime(
            Date(date), DateFormat.MEDIUM, DateFormat.SHORT,
            true, " " + Res.get("temporal.at") + " "
        )

        val authorUserProfileId = message.authorUserProfileId
        val senderUserProfile: Optional<UserProfile> =
            userProfileService.findUserProfile(authorUserProfileId)
        val nym: String = senderUserProfile.map { it.nym }.orElse("")
        val userName: String = senderUserProfile.map { it.userName }.orElse("")
        val reputationScore =
            senderUserProfile.flatMap(reputationService::findReputationScore)
                .map {
                    ReputationScore(
                        it.totalScore,
                        it.fiveSystemScore,
                        it.ranking
                    )
                }
                .orElse(ReputationScore.NONE)
        val amountSpec: AmountSpec = bisqEasyOffer.amountSpec
        val priceSpec: PriceSpec = bisqEasyOffer.priceSpec
        val hasAmountRange = amountSpec is RangeAmountSpec
        val market: Market = bisqEasyOffer.market
        val formattedQuoteAmount: String =
            OfferAmountFormatter.formatQuoteAmount(
                marketPriceService,
                amountSpec,
                priceSpec,
                market,
                hasAmountRange,
                true
            )
        val formattedPrice: String = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, true)

        val quoteSidePaymentMethods: List<String> =
            PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.quoteSidePaymentMethodSpecs)
                .map { it.name }
                .toList()
        val baseSidePaymentMethods: List<String> =
            PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.baseSidePaymentMethodSpecs)
                .map { it.name }
                .toList()
        val supportedLanguageCodes: String =
            Joiner.on(",").join(bisqEasyOffer.supportedLanguageCodes)
        val isMyMessage = message.isMyMessage(userIdentityService)
        val direction: network.bisq.mobile.client.replicated_model.offer.Direction =
            if (bisqEasyOffer.direction.isBuy) {
                network.bisq.mobile.client.replicated_model.offer.Direction.BUY
            } else {
                network.bisq.mobile.client.replicated_model.offer.Direction.SELL
            }

        val offerTitle = getOfferTitle(message, isMyMessage)
        val messageId = message.id
        val offerId = bisqEasyOffer.id
        val offerListItem = OfferListItem(
            messageId,
            offerId,
            isMyMessage,
            direction,
            offerTitle,
            date,
            formattedDate,
            nym,
            userName,
            reputationScore,
            formattedQuoteAmount,
            formattedPrice,
            quoteSidePaymentMethods,
            baseSidePaymentMethods,
            supportedLanguageCodes
        )
        return offerListItem
    }

    private fun getOfferTitle(message: BisqEasyOfferbookMessage, isMyMessage: Boolean): String {
        if (isMyMessage) {
            val direction: Direction = message.bisqEasyOffer.get().direction
            val directionString: String =
                StringUtils.capitalize(Res.get("offer." + direction.name.lowercase()))
            return Res.get(
                "bisqEasy.tradeWizard.review.chatMessage.myMessageTitle",
                directionString
            )
        } else {
            return message.text
        }
    }
}