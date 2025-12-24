package network.bisq.mobile.node.service.offers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OfferFormattingUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NodeOffersServiceFacadeIntegrationTest {
    // Minimal fake SettingsRepository
    private class FakeSettingsRepo : SettingsRepository {
        override val data: Flow<Settings> = flowOf(Settings())

        override suspend fun setFirstLaunch(value: Boolean) {}

        override suspend fun setShowChatRulesWarnBox(value: Boolean) {}

        override suspend fun setSelectedMarketCode(value: String) {}

        override suspend fun setNotificationPermissionState(value: PermissionState) {}

        override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {}

        override suspend fun update(transform: suspend (Settings) -> Settings) {}

        override suspend fun clear() {}
    }

    // Minimal fake MarketPriceServiceFacade to drive selectedMarketPriceItem
    private class FakeMarketPriceServiceFacade : MarketPriceServiceFacade(FakeSettingsRepo()) {
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? = selectedMarketPriceItem.value

        override fun findUSDMarketPriceItem(): MarketPriceItem? = selectedMarketPriceItem.value

        override fun refreshSelectedFormattedMarketPrice() {}

        override fun selectMarket(marketListItem: MarketListItem) {}

        fun set(item: MarketPriceItem) {
            _selectedMarketPriceItem.value = item
        }
    }

    private fun buildDto(
        id: String,
        market: MarketVO,
        amountMinor: Long,
        priceSpec: Any,
        formattedPrice: String = "INIT",
        formattedBaseAmount: String = "INIT",
    ): OfferItemPresentationDto {
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
            )
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.BUY,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(amountMinor),
                priceSpec =
                    when (priceSpec) {
                        is FixPriceSpecVO -> priceSpec
                        is FloatPriceSpecVO -> priceSpec
                        is MarketPriceSpecVO -> priceSpec
                        else -> FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(100_00L, market))
                    },
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        val user = createMockUserProfile("Alice")
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = user,
            formattedDate = "",
            formattedQuoteAmount = AmountFormatter.formatAmount(FiatVOFactory.run { from(amountMinor, market.quoteCurrencyCode) }, true, true),
            formattedBaseAmount = formattedBaseAmount,
            formattedPrice = formattedPrice,
            formattedPriceSpec = "",
            quoteSidePaymentMethods = emptyList(),
            baseSidePaymentMethods = emptyList(),
            reputationScore = ReputationScoreVO(0, 0.0, 0),
        )
    }

    @Test
    fun market_price_change_updates_offer_models() {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val initialMarketPrice = PriceQuoteVOFactory.fromPrice(100_00L, market)
        val updatedMarketPrice = PriceQuoteVOFactory.fromPrice(110_00L, market)

        val floatSpec = FloatPriceSpecVO(0.10)
        val marketSpec = MarketPriceSpecVO()
        val fixedSpec = FixPriceSpecVO(initialMarketPrice)

        val modelFloat = OfferItemPresentationModel(buildDto("float", market, 100_00L, floatSpec))
        val modelMarket = OfferItemPresentationModel(buildDto("market", market, 100_00L, marketSpec))
        val modelFixed = OfferItemPresentationModel(buildDto("fixed", market, 100_00L, fixedSpec))

        // Prepare offers and updated market item
        val offers = listOf(modelFloat, modelMarket, modelFixed)
        val updatedMarketItem =
            MarketPriceItem(
                market,
                updatedMarketPrice,
                PriceQuoteFormatter.format(updatedMarketPrice, true, true),
            )

        OfferFormattingUtil.updateOffersFormattedValues(offers, updatedMarketItem)

        // Expected formatted prices computed via the same production path
        val expectedMarketPrice = PriceQuoteFormatter.format(updatedMarketItem.priceQuote, true, true)
        val expectedFloatPrice = PriceQuoteFormatter.format(floatSpec.getPriceQuoteVO(updatedMarketItem), true, true)

        assertEquals(expectedMarketPrice, modelMarket.formattedPrice.value)
        assertEquals(expectedFloatPrice, modelFloat.formattedPrice.value)
        assertNotEquals("INIT", modelMarket.formattedBaseAmount.value)
        assertNotEquals("INIT", modelFloat.formattedBaseAmount.value)
        // Fixed offer should remain unchanged
        assertEquals("INIT", modelFixed.formattedPrice.value)
    }
}
