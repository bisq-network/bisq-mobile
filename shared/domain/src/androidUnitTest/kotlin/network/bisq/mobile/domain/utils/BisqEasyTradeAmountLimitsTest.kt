package network.bisq.mobile.domain.utils

import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Covers the required-reputation-score entry points that resolve their config from the injected
 * [TradeAmountLimitsVO]. When market prices are unavailable the amount cannot be converted, so these
 * must degrade to null / "not invalid" rather than throw.
 */
class BisqEasyTradeAmountLimitsTest {
    private val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")

    private fun marketServiceWithoutPrices(): MarketPriceServiceFacade =
        mockk(relaxed = true) {
            every { findMarketPriceItem(any()) } returns null
            every { findUSDMarketPriceItem() } returns null
        }

    private fun buildBuyOffer(): BisqEasyOfferVO =
        BisqEasyOfferVO(
            id = "offer-1",
            date = 0L,
            makerNetworkId =
                NetworkIdVO(
                    AddressByTransportTypeMapVO(mapOf()),
                    PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
                ),
            direction = DirectionEnum.BUY,
            market = market,
            amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L),
            priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) }),
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList(),
        )

    @Test
    fun `findRequiredReputationScoreForMinOrFixedAmount returns null when market price is unavailable`() {
        val marketPriceServiceFacade = marketServiceWithoutPrices()

        val result =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
                marketPriceServiceFacade,
                buildBuyOffer(),
                TradeAmountLimitsVO.DEFAULT,
            )

        assertNull(result)
    }

    @Test
    fun `findRequiredReputationScoreForMaxOrFixedAmount returns null when market price is unavailable`() {
        val marketPriceServiceFacade = marketServiceWithoutPrices()

        val result =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(
                marketPriceServiceFacade,
                buildBuyOffer(),
                TradeAmountLimitsVO.DEFAULT,
            )

        assertNull(result)
    }
}
