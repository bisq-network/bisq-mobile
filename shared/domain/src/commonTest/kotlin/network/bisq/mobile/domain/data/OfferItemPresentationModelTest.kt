package network.bisq.mobile.domain.data

import kotlin.test.Test
import kotlin.test.assertEquals
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO

class OfferItemPresentationModelTest {

    private fun dummyDto(
        formattedPrice: String = "100 USD",
        formattedBaseAmount: String = "0.003 BTC"
    ): OfferItemPresentationDto {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val amountSpec = QuoteSideFixedAmountSpecVO(100_00)
        val priceSpec = FixPriceSpecVO(
            PriceQuoteVOFactory.fromPrice(100_00L, market)
        )
        val makerNetworkId = NetworkIdVO(
            AddressByTransportTypeMapVO(mapOf()),
            PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id")
        )
        val offer = BisqEasyOfferVO(
            id = "offer-1",
            date = 0L,
            makerNetworkId = makerNetworkId,
            direction = DirectionEnum.BUY,
            market = market,
            amountSpec = amountSpec,
            priceSpec = priceSpec,
            protocolTypes = emptyList(),
            baseSidePaymentMethodSpecs = emptyList(),
            quoteSidePaymentMethodSpecs = emptyList(),
            offerOptions = emptyList(),
            supportedLanguageCodes = emptyList()
        )
        val user = createMockUserProfile("Alice")
        val reputation = ReputationScoreVO(0, 0.0, 0)
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = user,
            formattedDate = "",
            formattedQuoteAmount = "100 USD",
            formattedBaseAmount = formattedBaseAmount,
            formattedPrice = formattedPrice,
            formattedPriceSpec = "",
            quoteSidePaymentMethods = emptyList(),
            baseSidePaymentMethods = emptyList(),
            reputationScore = reputation
        )
    }

    @Test
    fun updateFormattedPrice_updates_value_when_different() {
        val model = OfferItemPresentationModel(dummyDto(formattedPrice = "100 USD"))
        assertEquals("100 USD", model.formattedPrice.value)
        model.updateFormattedPrice("101 USD")
        assertEquals("101 USD", model.formattedPrice.value)
    }

    @Test
    fun updateFormattedPrice_noop_when_same() {
        val model = OfferItemPresentationModel(dummyDto(formattedPrice = "100 USD"))
        model.updateFormattedPrice("100 USD")
        assertEquals("100 USD", model.formattedPrice.value)
    }

    @Test
    fun updateFormattedBaseAmount_updates_value_when_different() {
        val model = OfferItemPresentationModel(dummyDto(formattedBaseAmount = "0.003 BTC"))
        assertEquals("0.003 BTC", model.formattedBaseAmount.value)
        model.updateFormattedBaseAmount("0.004 BTC")
        assertEquals("0.004 BTC", model.formattedBaseAmount.value)
    }

    @Test
    fun updateFormattedBaseAmount_noop_when_same() {
        val model = OfferItemPresentationModel(dummyDto(formattedBaseAmount = "0.003 BTC"))
        model.updateFormattedBaseAmount("0.003 BTC")
        assertEquals("0.003 BTC", model.formattedBaseAmount.value)
    }
}
