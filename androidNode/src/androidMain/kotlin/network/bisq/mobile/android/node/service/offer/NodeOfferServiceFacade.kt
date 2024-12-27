package network.bisq.mobile.android.node.service.offer

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.BitcoinPaymentMethodUtil
import bisq.account.payment_method.FiatPaymentMethod
import bisq.account.payment_method.FiatPaymentMethodUtil
import bisq.bisq_easy.BisqEasyServiceUtil
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.common.currency.Market
import bisq.offer.Direction
import bisq.offer.amount.spec.AmountSpec
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.price.spec.PriceSpec
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.service.offer.OfferServiceFacade
import network.bisq.mobile.domain.utils.Logging
import java.util.Date
import java.util.Optional

class NodeOfferServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) :
    OfferServiceFacade, Logging {

    // Dependencies
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelService
    }

    // API
    override suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>
    ): Result<String> {
        val offerId = createOffer(
            Mappings.DirectionMapping.toPojo(direction),
            Mappings.MarketMapping.toPojo(market),
            bitcoinPaymentMethods.map { BitcoinPaymentMethodUtil.getPaymentMethod(it) },
            fiatPaymentMethods.map { FiatPaymentMethodUtil.getPaymentMethod(it) },
            Mappings.AmountSpecMapping.toPojo(amountSpec),
            Mappings.PriceSpecMapping.toPojo(priceSpec),
            ArrayList<String>(supportedLanguageCodes)
        )
        return Result.success(offerId)
    }

    private suspend fun createOffer(
        direction: Direction,
        market: Market,
        bitcoinPaymentMethods: List<BitcoinPaymentMethod>,
        fiatPaymentMethods: List<FiatPaymentMethod>,
        amountSpec: AmountSpec,
        priceSpec: PriceSpec,
        supportedLanguageCodes: List<String>
    ): String {
        val userIdentity: UserIdentity = userIdentityService.selectedUserIdentity
        val chatMessageText = BisqEasyServiceUtil.createOfferBookMessageFromPeerPerspective(
            userIdentity.nickName,
            marketPriceService,
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec
        )
        val userProfile = userIdentity.userProfile
        val bisqEasyOffer = BisqEasyOffer(
            userProfile.networkId,
            direction,
            market,
            amountSpec,
            priceSpec,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            userProfile.terms,
            supportedLanguageCodes
        )

        val channel: BisqEasyOfferbookChannel = bisqEasyOfferbookChannelService.findChannel(market).get()

        val myOfferMessage = BisqEasyOfferbookMessage(
            channel.id,
            userProfile.id,
            Optional.of(bisqEasyOffer),
            Optional.of(chatMessageText),
            Optional.empty(),
            Date().time,
            false
        )

        // blocking call
        bisqEasyOfferbookChannelService.publishChatMessage(myOfferMessage, userIdentity).join()
        return bisqEasyOffer.id
    }
}