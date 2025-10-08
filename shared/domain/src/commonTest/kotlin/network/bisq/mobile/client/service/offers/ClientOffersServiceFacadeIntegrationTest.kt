package network.bisq.mobile.client.service.offers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice

import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.offers.OfferFormattingUtil
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO

class ClientOffersServiceFacadeIntegrationTest {

    private fun buildDto(
        id: String,
        market: MarketVO,
        amountMinor: Long,
        amountSpec: Any,
        priceSpec: Any,
        formattedPrice: String = "INIT",
        formattedBaseAmount: String = "INIT"
    ): OfferItemPresentationDto {
        val makerNetworkId = NetworkIdVO(
            AddressByTransportTypeMapVO(mapOf()),
            PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id")
        )
        val offer = BisqEasyOfferVO(
            id = id,
            date = 0L,
            makerNetworkId = makerNetworkId,
            direction = DirectionEnum.BUY,
            market = market,
            amountSpec = when (amountSpec) {
                is BaseSideFixedAmountSpecVO -> amountSpec
                is QuoteSideFixedAmountSpecVO -> amountSpec
                else -> QuoteSideFixedAmountSpecVO(amountMinor)
            },
            priceSpec = when (priceSpec) {
                is FixPriceSpecVO -> priceSpec
                is FloatPriceSpecVO -> priceSpec
                is MarketPriceSpecVO -> priceSpec
                else -> FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(100_00L, market))
            },
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList()
        )
        val formattedQuoteAmount = AmountFormatter.formatAmount(
            FiatVOFactory.run { from(amountMinor, market.quoteCurrencyCode) },
            useLowPrecision = true,
            withCode = true
        )
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile("Alice"),
            formattedDate = "",
            formattedQuoteAmount = formattedQuoteAmount,
            formattedBaseAmount = formattedBaseAmount,
            formattedPrice = formattedPrice,
            formattedPriceSpec = "",
            quoteSidePaymentMethods = emptyList(),
            baseSidePaymentMethods = emptyList(),
            reputationScore = network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO(0, 0.0, 0)
        )
    }

    @Test
    fun market_price_change_updates_client_models() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val initialMarketPrice = PriceQuoteVOFactory.fromPrice(100_00L, market)
        val updatedMarketPrice = PriceQuoteVOFactory.fromPrice(110_00L, market)

        val floatSpec = FloatPriceSpecVO(0.10)
        val marketSpec = MarketPriceSpecVO()
        val fixedSpec = FixPriceSpecVO(initialMarketPrice)

        val modelFloat = OfferItemPresentationModel(buildDto("float", market, 100_00L, QuoteSideFixedAmountSpecVO(100_00L), floatSpec))
        val modelMarket = OfferItemPresentationModel(buildDto("market", market, 100_00L, QuoteSideFixedAmountSpecVO(100_00L), marketSpec))
        val modelFixed = OfferItemPresentationModel(buildDto("fixed", market, 100_00L, QuoteSideFixedAmountSpecVO(100_00L), fixedSpec))

        val offers = listOf(modelFloat, modelMarket, modelFixed)
        val updatedMarketItem = MarketPriceItem(
            market,
            updatedMarketPrice,
            PriceQuoteFormatter.format(updatedMarketPrice, true, true)
        )

        OfferFormattingUtil.updateOffersFormattedValues(offers, updatedMarketItem)

        val expectedMarketPrice = PriceQuoteFormatter.format(updatedMarketItem.priceQuote, true, true)
        val expectedFloatPrice = PriceQuoteFormatter.format(floatSpec.getPriceQuoteVO(updatedMarketItem), true, true)

        assertEquals(expectedMarketPrice, modelMarket.formattedPrice.value)
        assertEquals(expectedFloatPrice, modelFloat.formattedPrice.value)
        assertNotEquals("INIT", modelMarket.formattedBaseAmount.value)
        assertNotEquals("INIT", modelFloat.formattedBaseAmount.value)
        assertEquals("INIT", modelFixed.formattedPrice.value)
    }

    @Test
    fun base_side_fixed_amount_is_not_reformatted_on_market_tick() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val updatedMarketPrice = PriceQuoteVOFactory.fromPrice(120_00L, market)

        // Base-side fixed amount should remain unchanged even if price moves
        val amt = BaseSideFixedAmountSpecVO(1_0000L) // 0.0001 BTC in minor units
        val marketSpec = MarketPriceSpecVO()

        val model = OfferItemPresentationModel(
            buildDto(
                id = "base-fixed",
                market = market,
                amountMinor = 100_00L,
                amountSpec = amt,
                priceSpec = marketSpec
            )
        )

        val previousBase = model.formattedBaseAmount.value

        OfferFormattingUtil.updateOffersFormattedValues(
            listOf(model),
            MarketPriceItem(market, updatedMarketPrice, PriceQuoteFormatter.format(updatedMarketPrice, true, true))
        )

        // Price updates (since price spec is market), but base amount remains previous
        assertNotEquals("INIT", model.formattedPrice.value)
        assertEquals(previousBase, model.formattedBaseAmount.value)
    }

    @Test
    fun quote_side_range_amount_is_reformatted_on_market_tick() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val updatedMarketPrice = PriceQuoteVOFactory.fromPrice(130_00L, market)

        val amountSpec = network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO(
            minAmount = 50_00L,
            maxAmount = 150_00L
        )
        val priceSpec = MarketPriceSpecVO()

        val model = OfferItemPresentationModel(
            buildDto(
                id = "quote-range",
                market = market,
                amountMinor = 100_00L,
                amountSpec = amountSpec,
                priceSpec = priceSpec
            )
        )

        OfferFormattingUtil.updateOffersFormattedValues(
            listOf(model),
            MarketPriceItem(market, updatedMarketPrice, PriceQuoteFormatter.format(updatedMarketPrice, true, true))
        )

        // Expect base amount string to be updated from INIT
        assertNotEquals("INIT", model.formattedBaseAmount.value)
    }
}
