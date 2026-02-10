package network.bisq.mobile.client.common.domain.websocket

import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.replicated.common.currency.marketListDemoObj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketClientDemoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `FakeSubscriptionData offers use valid market codes matching marketListDemoObj`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.offers.forEach { offer ->
            val market = offer.bisqEasyOffer.market
            assertTrue(
                validBaseCurrencyCodes.contains(market.baseCurrencyCode),
                "Offer ${offer.bisqEasyOffer.id} has invalid baseCurrencyCode: ${market.baseCurrencyCode}. " +
                    "Expected one of: $validBaseCurrencyCodes",
            )
            assertTrue(
                validQuoteCurrencyCodes.contains(market.quoteCurrencyCode),
                "Offer ${offer.bisqEasyOffer.id} has invalid quoteCurrencyCode: ${market.quoteCurrencyCode}. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData offers priceSpec markets use valid market codes`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()

        FakeSubscriptionData.offers.forEach { offer ->
            val priceSpec = offer.bisqEasyOffer.priceSpec
            // Check if priceSpec has a market (FixPriceSpecVO has priceQuote with market)
            val priceSpecMarket =
                when (priceSpec) {
                    is network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO ->
                        priceSpec.priceQuote.market
                    else -> null
                }

            priceSpecMarket?.let { market ->
                assertTrue(
                    validBaseCurrencyCodes.contains(market.baseCurrencyCode),
                    "Offer ${offer.bisqEasyOffer.id} priceSpec has invalid baseCurrencyCode: ${market.baseCurrencyCode}. " +
                        "Expected one of: $validBaseCurrencyCodes",
                )
            }
        }
    }

    @Test
    fun `FakeSubscriptionData marketPrice uses valid market codes`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.marketPrice.forEach { (currencyCode, priceQuote) ->
            assertTrue(
                validQuoteCurrencyCodes.contains(currencyCode),
                "MarketPrice key '$currencyCode' is not a valid quote currency code. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
            assertTrue(
                validBaseCurrencyCodes.contains(priceQuote.market.baseCurrencyCode),
                "MarketPrice for $currencyCode has invalid baseCurrencyCode: ${priceQuote.market.baseCurrencyCode}. " +
                    "Expected one of: $validBaseCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData numOffers keys match valid quote currency codes`() {
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.numOffers.keys.forEach { currencyCode ->
            assertTrue(
                validQuoteCurrencyCodes.contains(currencyCode),
                "NumOffers key '$currencyCode' is not a valid quote currency code. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData offers can be serialized and deserialized`() {
        val serialized = json.encodeToString(FakeSubscriptionData.offers)
        val deserialized = json.decodeFromString<List<network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto>>(serialized)

        assertEquals(FakeSubscriptionData.offers.size, deserialized.size)
        deserialized.forEachIndexed { index, dto ->
            assertEquals(FakeSubscriptionData.offers[index].bisqEasyOffer.id, dto.bisqEasyOffer.id)
        }
    }

    @Test
    fun `FakeSubscriptionData has at least one offer`() {
        assertTrue(
            FakeSubscriptionData.offers.isNotEmpty(),
            "FakeSubscriptionData.offers should not be empty",
        )
    }

    @Test
    fun `FakeSubscriptionData offers baseCurrencyCode should be BTC not Bitcoin`() {
        FakeSubscriptionData.offers.forEach { offer ->
            assertEquals(
                "BTC",
                offer.bisqEasyOffer.market.baseCurrencyCode,
                "Offer ${offer.bisqEasyOffer.id} should use 'BTC' as baseCurrencyCode, not '${offer.bisqEasyOffer.market.baseCurrencyCode}'",
            )
        }
    }
}
