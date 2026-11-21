package network.bisq.mobile.presentation.common.test_utils

import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator

/**
 * Offer fixtures shared by the create-offer / take-offer / offerbook test suites.
 *
 * Prefer these over hand-rolling an offer: the wizard presenters read many fields off the offer and
 * the coordinator models, so a partially populated fixture fails in a presenter `init` block rather
 * than in the assertion you care about.
 */
object OfferTestFactory {
    const val USD_PRICE_VALUE = 100_000_00L

    /** BTC/USD at $100,000. */
    fun usdMarketPriceItem(): MarketPriceItem = makeMarketPriceItem(MarketVOFactory.USD, USD_PRICE_VALUE)

    /** Price table holding only BTC/USD — enough for the amount-limit math. */
    fun usdPrices(): Map<MarketVO, MarketPriceItem> = mapOf(MarketVOFactory.USD to usdMarketPriceItem())

    fun makeMarketPriceItem(
        market: MarketVO,
        priceValue: Long,
    ): MarketPriceItem =
        MarketPriceItem(
            market,
            with(PriceQuoteVOFactory) { fromPrice(priceValue, market) },
            formattedPrice = "${priceValue / 100} ${market.quoteCurrencyCode}",
        )

    /**
     * A fully populated [CreateOfferCoordinator.CreateOfferModel] with FIXED pricing.
     *
     * `CreateOfferReviewPresenter.onViewAttached()` reads most of these fields synchronously and
     * formats them, so a partially populated model fails there rather than in the assertion. FIXED
     * pricing keeps `calculateEffectivePrice` returning `priceQuote` verbatim, so callers don't have
     * to reason about market-markup arithmetic.
     */
    fun makeCreateOfferModel(
        market: MarketVO = MarketVOFactory.USD,
        direction: DirectionEnum = DirectionEnum.BUY,
    ): CreateOfferCoordinator.CreateOfferModel {
        val priceQuote = with(PriceQuoteVOFactory) { fromPrice(USD_PRICE_VALUE, market) }
        return CreateOfferCoordinator.CreateOfferModel().also { model ->
            model.market = market
            model.direction = direction
            model.amountType = CreateOfferCoordinator.AmountType.FIXED_AMOUNT
            model.quoteSideFixedAmount = with(FiatVOFactory) { fromFaceValue(5000.0, market.quoteCurrencyCode) }
            model.baseSideFixedAmount = with(CoinVOFactory) { fromFaceValue(0.05, market.baseCurrencyCode) }
            model.priceType = CreateOfferCoordinator.PriceType.FIXED
            model.originalPriceQuote = priceQuote
            model.priceQuote = priceQuote
            model.availableQuoteSidePaymentMethods = listOf("REVOLUT")
            model.selectedQuoteSidePaymentMethods = setOf("REVOLUT")
            model.selectedBaseSidePaymentMethods = setOf("MAIN_CHAIN")
        }
    }

    fun makeOfferDto(
        amountSpec: AmountSpecVO = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L),
        paymentMethods: List<String> = listOf("SEPA"),
        btcMethods: List<String> = listOf("BTC"),
        direction: DirectionEnum = DirectionEnum.BUY,
        priceSpec: PriceSpecVO = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(USD_PRICE_VALUE, MarketVOFactory.USD) }),
        date: Long = 0L,
        isMyOffer: Boolean = false,
        formattedPrice: String = "",
        supportedLanguageCodes: List<String> = emptyList(),
    ): OfferItemPresentationDto {
        val market = MarketVOFactory.USD
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
            )
        val offer =
            BisqEasyOfferVO(
                id = "offer-1",
                date = date,
                makerNetworkId = makerNetworkId,
                direction = direction,
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = btcMethods.map { BitcoinPaymentMethodSpecVO(it, null) },
                quoteSidePaymentMethodSpecs = paymentMethods.map { FiatPaymentMethodSpecVO(it, null) },
                offerOptions = emptyList(),
                supportedLanguageCodes = supportedLanguageCodes,
            )
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = isMyOffer,
            userProfile = createMockUserProfile("Alice"),
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = formattedPrice,
            formattedPriceSpec = "",
            quoteSidePaymentMethods = paymentMethods,
            baseSidePaymentMethods = btcMethods,
            reputationScore = ReputationScoreVO(0, 0.0, 0),
        )
    }
}
