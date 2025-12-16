package network.bisq.mobile.presentation.offerbook

import kotlin.test.Test
import kotlin.test.assertEquals
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

class OfferbookFilterDerivationsTest {

    @Test
    fun empty_offers_yield_empty_sets() {
        val offers = emptyList<OfferItemPresentationModel>()
        assertEquals(emptySet(), OfferbookFilterDerivations.paymentMethodIds(offers))
        assertEquals(emptySet(), OfferbookFilterDerivations.settlementMethodIds(offers))
    }

    @Test
    fun merges_unique_methods_from_multiple_offers() {
        val m1 = makeOfferModel(
            id = "o1",
            payment = listOf("SEPA", "REVOLUT"),
            settlement = listOf("BTC")
        )
        val m2 = makeOfferModel(
            id = "o2",
            payment = listOf("PIX"),
            settlement = listOf("LIGHTNING")
        )
        val mergedPayment = OfferbookFilterDerivations.paymentMethodIds(listOf(m1, m2))
        val mergedSettlement = OfferbookFilterDerivations.settlementMethodIds(listOf(m1, m2))
        assertEquals(setOf("SEPA", "REVOLUT", "PIX"), mergedPayment)
        assertEquals(setOf("BTC", "LIGHTNING"), mergedSettlement)
    }

    private fun makeOfferModel(
        id: String,
        payment: List<String>,
        settlement: List<String>
    ): OfferItemPresentationModel {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) })
        val makerNetworkId = NetworkIdVO(AddressByTransportTypeMapVO(mapOf()), PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"))
        val offer = BisqEasyOfferVO(
            id = id,
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
        val user = createMockUserProfile("Tester")
        val reputation = ReputationScoreVO(0, 0.0, 0)
        val dto = OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = user,
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = "",
            formattedPriceSpec = "",
            quoteSidePaymentMethods = payment,
            baseSidePaymentMethods = settlement,
            reputationScore = reputation
        )
        return OfferItemPresentationModel(dto)
    }
}

