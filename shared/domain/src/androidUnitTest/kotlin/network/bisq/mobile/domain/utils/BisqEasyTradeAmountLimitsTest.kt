package network.bisq.mobile.domain.utils

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the required-reputation-score entry points that resolve their config from the injected
 * [TradeAmountLimitsVO], plus the [BisqEasyTradeAmountLimits.isBuyOfferInvalid] reputation-fetch
 * path — including that a cancellation surfaced by the reputation lookup is treated as a plain
 * failure while the caller is still active (rather than crashing or being swallowed silently).
 */
class BisqEasyTradeAmountLimitsTest {
    private val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")

    private fun marketServiceWithoutPrices(): MarketPriceServiceFacade =
        mockk(relaxed = true) {
            every { findMarketPriceItem(any()) } returns null
            every { findUSDMarketPriceItem() } returns null
        }

    // A priced market service so isBuyOfferInvalid gets past the "required score unavailable"
    // guard and actually exercises the reputation lookup below it.
    private fun marketServiceWithPrices(): MarketPriceServiceFacade =
        mockk(relaxed = true) {
            every { findMarketPriceItem(any()) } answers {
                val requested = firstArg<MarketVO>()
                MarketPriceItem(requested, with(PriceQuoteVOFactory) { fromPrice(100_00L, requested) }, formattedPrice = "100")
            }
            every { findUSDMarketPriceItem() } answers {
                MarketPriceItem(market, with(PriceQuoteVOFactory) { fromPrice(100_00L, market) }, formattedPrice = "100")
            }
        }

    private fun buildBuyOffer(id: String = "offer-1"): BisqEasyOfferVO =
        BisqEasyOfferVO(
            id = id,
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

    private fun buildBuyOfferModel(id: String): OfferItemPresentationModel =
        OfferItemPresentationModel(
            OfferItemPresentationDto(
                bisqEasyOffer = buildBuyOffer(id),
                isMyOffer = false,
                userProfile = createMockUserProfile("Alice"),
                formattedDate = "",
                formattedQuoteAmount =
                    FiatVOFactory.run { from(10_0000L, market.quoteCurrencyCode) }.let { "10 USD" },
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = emptyList(),
                baseSidePaymentMethods = emptyList(),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            ),
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

    @Test
    fun `isBuyOfferInvalid returns false when seller reputation clears the required score`() =
        runTest {
            val reputationServiceFacade =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } returns Result.success(ReputationScoreVO(Long.MAX_VALUE, 0.0, 0))
                }

            val isInvalid =
                BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                    item = buildBuyOfferModel("valid-high-score"),
                    useCache = false,
                    marketPriceServiceFacade = marketServiceWithPrices(),
                    reputationServiceFacade = reputationServiceFacade,
                    userProfileId = "seller",
                    limits = TradeAmountLimitsVO.DEFAULT,
                )

            assertFalse(isInvalid)
        }

    @Test
    fun `isBuyOfferInvalid returns true and defaults score to zero when reputation lookup fails`() =
        runTest {
            val reputationServiceFacade =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } returns Result.failure(RuntimeException("no reputation"))
                }

            val isInvalid =
                BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                    item = buildBuyOfferModel("invalid-lookup-fail"),
                    useCache = false,
                    marketPriceServiceFacade = marketServiceWithPrices(),
                    reputationServiceFacade = reputationServiceFacade,
                    userProfileId = "seller",
                    limits = TradeAmountLimitsVO.DEFAULT,
                )

            assertTrue(isInvalid)
        }

    @Test
    fun `isBuyOfferInvalid treats a cancellation from the reputation lookup as a failure while caller is active`() =
        runTest {
            // A CancellationException that does NOT stem from our own cancellation (e.g. a request
            // timeout) must fall through to the plain failure handling, not propagate or crash.
            val reputationServiceFacade =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } throws CancellationException("request timed out")
                }

            val isInvalid =
                BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                    item = buildBuyOfferModel("cancel-active"),
                    useCache = false,
                    marketPriceServiceFacade = marketServiceWithPrices(),
                    reputationServiceFacade = reputationServiceFacade,
                    userProfileId = "seller",
                    limits = TradeAmountLimitsVO.DEFAULT,
                )

            // The active test coroutine means ensureActive() is a no-op: score defaults to 0 -> invalid.
            assertTrue(isInvalid)
        }

    @Test
    fun `isBuyOfferInvalid propagates cancellation when the caller is genuinely cancelled`() =
        runTest {
            // Reputation lookup parks until the caller is cancelled, so the CancellationException
            // observed here is a genuine cancellation of our own job (not a timeout) and must propagate.
            val gate = CompletableDeferred<Unit>()
            val reputationServiceFacade =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } coAnswers {
                        gate.await()
                        Result.success(ReputationScoreVO(0, 0.0, 0))
                    }
                }
            val item = buildBuyOfferModel("cancel-genuine")
            val marketPriceServiceFacade = marketServiceWithPrices()

            var propagated = false
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                            item = item,
                            useCache = false,
                            marketPriceServiceFacade = marketPriceServiceFacade,
                            reputationServiceFacade = reputationServiceFacade,
                            userProfileId = "seller",
                            limits = TradeAmountLimitsVO.DEFAULT,
                        )
                    } catch (e: CancellationException) {
                        propagated = true
                        throw e
                    }
                }

            child.cancel()
            child.join()

            assertTrue(propagated)
        }
}
