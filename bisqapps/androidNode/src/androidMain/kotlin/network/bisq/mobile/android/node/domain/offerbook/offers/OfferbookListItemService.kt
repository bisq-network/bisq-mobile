package network.bisq.mobile.android.node.domain.offerbook.offers

import bisq.bisq_easy.BisqEasyServiceUtil
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
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
import bisq.presentation.formatters.DateFormatter
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import co.touchlab.kermit.Logger
import com.google.common.base.Joiner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.client.replicated_model.user.reputation.ReputationScore
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.offerbook.OfferbookListItem
import java.text.DateFormat
import java.util.Date
import java.util.Optional


class OfferbookListItemService(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    LifeCycleAware {
    // Dependencies
    private lateinit var userProfileService: UserProfileService
    private lateinit var userIdentityService: UserIdentityService
    private lateinit var reputationService: ReputationService
    private lateinit var bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService
    private lateinit var bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService
    private lateinit var marketPriceService: MarketPriceService

    // Properties
    private val _offerbookListItems = MutableStateFlow<ArrayList<OfferbookListItem>>(ArrayList())
    val offerbookListItems: StateFlow<List<OfferbookListItem>> get() = _offerbookListItems

    // Misc
    private val log = Logger.withTag(this::class.simpleName ?: "Offers")
    private var chatMessagesPin: Pin? = null
    private var selectedChannelPin: Pin? = null


    // Life cycle
    override fun initialize() {
        userProfileService =
            applicationServiceSupplier.userServiceSupplier.get().userProfileService
        userIdentityService =
            applicationServiceSupplier.userServiceSupplier.get().userIdentityService
        reputationService = applicationServiceSupplier.userServiceSupplier.get().reputationService
        bisqEasyOfferbookChannelService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService
        bisqEasyOfferbookChannelSelectionService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelSelectionService
        marketPriceService =
            applicationServiceSupplier.bondedRolesServiceSupplier.get().marketPriceService

        addSelectedChannelObservers()
    }

    override fun resume() {
        addSelectedChannelObservers()
    }

    override fun dispose() {
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
        val chatMessages: ObservableSet<BisqEasyOfferbookMessage> = marketChannel.chatMessages
        chatMessagesPin =
            chatMessages.addObserver(object : CollectionObserver<BisqEasyOfferbookMessage> {
                override fun add(message: BisqEasyOfferbookMessage) {
                    if (message.hasBisqEasyOffer()) {
                        val offerbookListItem: OfferbookListItem = createOfferItem(message)
                        log.e { "add offer $offerbookListItem" }
                        _offerbookListItems.value += offerbookListItem
                    }
                }

                override fun remove(message: Any) {
                    if (message is BisqEasyOfferbookMessage && message.hasBisqEasyOffer()) {
                        val toRemove =
                            _offerbookListItems.value.first { it.messageId == message.id }
                        log.e { "remove offer $toRemove" }
                        _offerbookListItems.value += toRemove
                    }
                }

                override fun clear() {
                    _offerbookListItems.value.clear()
                }
            })
    }

    private fun createOfferItem(message: BisqEasyOfferbookMessage): OfferbookListItem {
        val bisqEasyOffer: BisqEasyOffer = message.bisqEasyOffer.get()
        val date = DateFormatter.formatDateTime(
            Date(message.date), DateFormat.MEDIUM, DateFormat.SHORT,
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
        val formattedPrice: String =
            BisqEasyServiceUtil.getFormattedPriceSpec(priceSpec)
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
        val offerTitle = getOfferTitle(message, isMyMessage)
        val messageId = message.id
        val offerId = bisqEasyOffer.id
        val offerbookListItem = OfferbookListItem(
            messageId,
            offerId,
            isMyMessage,
            offerTitle,
            date,
            nym,
            userName,
            reputationScore,
            formattedQuoteAmount,
            formattedPrice,
            quoteSidePaymentMethods,
            baseSidePaymentMethods,
            supportedLanguageCodes
        )
        return offerbookListItem
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