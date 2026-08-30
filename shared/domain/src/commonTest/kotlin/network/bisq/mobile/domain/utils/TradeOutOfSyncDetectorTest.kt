package network.bisq.mobile.domain.utils

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.data.replicated.contract.PartyVO
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.utils.TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeOutOfSyncDetectorTest {
    private companion object {
        const val TAKE_OFFER_DATE = 1_000_000L
    }

    @Test
    fun `a trade in INIT past the threshold is out of sync`() {
        val trade = createTradeModel(BisqEasyTradeStateEnum.INIT)

        assertTrue(TradeOutOfSyncDetector.isOutOfSync(trade, nowMs = TAKE_OFFER_DATE + OUT_OF_SYNC_THRESHOLD_MS + 1))
    }

    @Test
    fun `a trade in INIT within the threshold is not out of sync`() {
        val trade = createTradeModel(BisqEasyTradeStateEnum.INIT)

        assertFalse(TradeOutOfSyncDetector.isOutOfSync(trade, nowMs = TAKE_OFFER_DATE + OUT_OF_SYNC_THRESHOLD_MS))
    }

    @Test
    fun `a trade past the threshold but out of INIT is not out of sync`() {
        val trade = createTradeModel(BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST)

        assertFalse(TradeOutOfSyncDetector.isOutOfSync(trade, nowMs = TAKE_OFFER_DATE + OUT_OF_SYNC_THRESHOLD_MS + 1))
    }

    @Test
    fun `a stuck trade leaving INIT stops being out of sync`() {
        val trade = createTradeModel(BisqEasyTradeStateEnum.INIT)
        val now = TAKE_OFFER_DATE + OUT_OF_SYNC_THRESHOLD_MS + 1

        assertTrue(TradeOutOfSyncDetector.isOutOfSync(trade, nowMs = now))

        trade.setTradeState(BisqEasyTradeStateEnum.REJECTED)

        assertFalse(TradeOutOfSyncDetector.isOutOfSync(trade, nowMs = now))
    }

    /** Minimal real model, mirroring `createMockTradeItem` in `OpenTradeListItem.kt` — every type in the chain is a plain data class. */
    private fun createTradeModel(tradeState: BisqEasyTradeStateEnum): BisqEasyTradeModel {
        val makerProfile = createMockUserProfile("Maker")
        val takerProfile = createMockUserProfile("Taker")
        val market = MarketVO("BTC", "EUR", "Bitcoin", "Euro")
        val priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(50_000L, market))
        val bitcoinPaymentMethod = BitcoinPaymentMethodSpecVO("MAIN_CHAIN", null)
        val fiatPaymentMethod = FiatPaymentMethodSpecVO("SEPA", null)
        val offer =
            BisqEasyOfferVO(
                id = "test-offer-1",
                date = 0L,
                makerNetworkId = makerProfile.networkId,
                direction = DirectionEnum.SELL,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(500_00),
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = listOf(bitcoinPaymentMethod),
                quoteSidePaymentMethodSpecs = listOf(fiatPaymentMethod),
                offerOptions = emptyList(),
                supportedLanguageCodes = listOf("en"),
            )
        return BisqEasyTradeModel(
            BisqEasyTradeDto(
                contract =
                    BisqEasyContractVO(
                        takeOfferDate = TAKE_OFFER_DATE,
                        offer = offer,
                        maker = PartyVO(RoleEnum.MAKER, makerProfile.networkId),
                        taker = PartyVO(RoleEnum.TAKER, takerProfile.networkId),
                        baseSideAmount = 1_000_000L,
                        quoteSideAmount = 500_00L,
                        baseSidePaymentMethodSpec = bitcoinPaymentMethod,
                        quoteSidePaymentMethodSpec = fiatPaymentMethod,
                        mediator = null,
                        priceSpec = priceSpec,
                        marketPrice = 50_000L,
                    ),
                id = "test-trade-1",
                tradeRole = TradeRoleEnum.BUYER_AS_TAKER,
                myIdentity = IdentityVO(tag = "test", networkId = takerProfile.networkId),
                taker = BisqEasyTradePartyVO(takerProfile.networkId),
                maker = BisqEasyTradePartyVO(makerProfile.networkId),
                tradeState = tradeState,
                paymentAccountData = null,
                bitcoinPaymentData = null,
                paymentProof = null,
                interruptTradeInitiator = null,
                errorMessage = null,
                errorStackTrace = null,
                peersErrorMessage = null,
                peersErrorStackTrace = null,
            ),
        )
    }
}
