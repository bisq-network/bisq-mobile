package network.bisq.mobile.android.node.service.trade

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.ChatChannelDomain
import bisq.chat.ChatChannelSelectionService
import bisq.chat.ChatService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.monetary.Monetary
import bisq.common.observable.Pin
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.i18n.Res
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.payment_method.BitcoinPaymentMethodSpec
import bisq.offer.payment_method.FiatPaymentMethodSpec
import bisq.offer.payment_method.PaymentMethodSpecUtil
import bisq.support.mediation.MediationRequestService
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeService
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.service.trade.TakeOfferStatus
import network.bisq.mobile.domain.service.trade.TradeServiceFacade
import network.bisq.mobile.domain.utils.Logging
import kotlin.time.Duration.Companion.seconds

class NodeTradeServiceFacade(applicationService: AndroidApplicationService.Provider) : TradeServiceFacade, Logging {

    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelService
    }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }
    private val mediationRequestService: MediationRequestService by lazy { applicationService.supportService.get().mediationRequestService }
    private val bisqEasyTradeService: BisqEasyTradeService by lazy { applicationService.tradeService.get().bisqEasyTradeService }
    private val bisqEasyOpenTradeChannelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val chatService: ChatService by lazy { applicationService.chatService.get() }

    // API
    override suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): Result<String> {
        try {
            val tradeId = doTakeOffer(
                Mappings.BisqEasyOfferMapping.toPojo(bisqEasyOffer),
                Mappings.MonetaryMapping.toPojo(takersBaseSideAmount),
                Mappings.MonetaryMapping.toPojo(takersQuoteSideAmount),
                BitcoinPaymentMethodSpec(PaymentMethodSpecUtil.getBitcoinPaymentMethod(bitcoinPaymentMethod)),
                FiatPaymentMethodSpec(PaymentMethodSpecUtil.getFiatPaymentMethod(fiatPaymentMethod)),
                takeOfferStatus,
                takeOfferErrorMessage
            )
            return Result.success(tradeId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun doTakeOffer(
        bisqEasyOffer: BisqEasyOffer,
        takersBaseSideAmount: Monetary,
        takersQuoteSideAmount: Monetary,
        bitcoinPaymentMethodSpec: BitcoinPaymentMethodSpec,
        fiatPaymentMethodSpec: FiatPaymentMethodSpec,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): String {
        var errorMessagePin: Pin? = null
        var peersErrorMessagePin: Pin? = null
        try {
            check(!bannedUserService.isNetworkIdBanned(bisqEasyOffer.makerNetworkId)) { "You cannot take the offer because the maker is banned" }
            val takerIdentity = userIdentityService.selectedUserIdentity
            check(!bannedUserService.isUserProfileBanned(takerIdentity.userProfile)) // if taker is banned we don't give him hints.
            val mediator = mediationRequestService.selectMediator(bisqEasyOffer.makersUserProfileId, takerIdentity.id)
            val priceSpec = bisqEasyOffer.priceSpec
            val marketPrice: Long = marketPriceService.findMarketPrice(bisqEasyOffer.market).map { it.priceQuote.value }.orElse(0)
            val bisqEasyProtocol: BisqEasyProtocol = bisqEasyTradeService.createBisqEasyProtocol(
                takerIdentity.identity,
                bisqEasyOffer,
                takersBaseSideAmount,
                takersQuoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice
            )
            val bisqEasyTrade: BisqEasyTrade = bisqEasyProtocol.model
            log.i { "Selected mediator for trade ${bisqEasyTrade.shortId}: ${mediator.map(UserProfile::getUserName).orElse("N/A")}" }

            val tradeId = bisqEasyTrade.id
            errorMessagePin = bisqEasyTrade.errorMessageObservable().addObserver { message: String? ->
                if (message != null) {
                    takeOfferErrorMessage.value = Res.get(
                        "bisqEasy.openTrades.failed.popup",
                        message,
                        bisqEasyTrade.errorStackTrace.take(500)
                    )
                }
            }
            peersErrorMessagePin = bisqEasyTrade.peersErrorMessageObservable().addObserver { peersErrorMessage: String? ->
                if (peersErrorMessage != null) {
                    takeOfferErrorMessage.value = Res.get(
                        "bisqEasy.openTrades.failedAtPeer.popup",
                        peersErrorMessage,
                        bisqEasyTrade.peersErrorStackTrace.take(500)
                    )
                }
            }

            bisqEasyTradeService.takeOffer(bisqEasyTrade);
            takeOfferStatus.value = TakeOfferStatus.SENT
            val contract: BisqEasyContract = bisqEasyTrade.contract

            // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message
            withTimeout(150.seconds) {
                bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.mediator)
                    .thenAccept { result ->
                        // In case the user has switched to another market we want to select that market in the offer book
                        val chatChannelSelectionService: ChatChannelSelectionService =
                            chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK)
                        bisqEasyOfferbookChannelService.findChannel(contract.offer.market)
                            .ifPresent { chatChannel: BisqEasyOfferbookChannel? -> chatChannelSelectionService.selectChannel(chatChannel) }
                        takeOfferStatus.value = TakeOfferStatus.SUCCESS
                        bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                            .ifPresent { channel ->
                                val taker = userIdentityService.selectedUserIdentity.userProfile.userName
                                val maker: String = channel.peer.userName
                                val encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker)
                                chatService.bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
                            }
                    }
                    .join()
            }
            return tradeId
        } finally {
            errorMessagePin?.unbind()
            peersErrorMessagePin?.unbind()
        }
    }
}