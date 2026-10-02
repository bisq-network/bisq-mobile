package network.bisq.mobile.domain.service.offers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.AuthorOffersSnapshot
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Detection of my own sell offers that my current reputation score no longer covers. A $50
 * fixed-amount offer requires a score of 10,000 under the default limits; my score is compared
 * with the 5% tolerance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OffersBelowReputationServiceTest {
    private val me = createMockUserProfile("me")
    private val selectedProfile = MutableStateFlow<UserProfileVO?>(me)
    private val allDataReceived = MutableStateFlow(true)
    private val scores = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val prices = mutableMapOf(MarketVOFactory.USD to priceItem(MarketVOFactory.USD))
    private val snapshots = mutableMapOf<String, AuthorOffersSnapshot>()

    private val reputationService =
        mockk<ReputationServiceFacade> {
            every { scoreByUserProfileId } returns scores
            // Unknown until a test sets a score: with an empty map, observeReputation stays silent.
            coEvery { getReputation(any()) } returns Result.failure(IllegalStateException("unknown"))
        }

    private val offersService =
        mockk<OffersServiceFacade> {
            coEvery { offersByAuthor(any()) } answers { snapshots[firstArg()] ?: AuthorOffersSnapshot(emptyList(), false) }
            coEvery { deleteOffer(any()) } returns Result.success(true)
        }

    private val marketPriceService =
        mockk<MarketPriceServiceFacade> {
            every { findMarketPriceItem(any()) } answers { prices[firstArg()] }
            every { findUSDMarketPriceItem() } answers { prices[MarketVOFactory.USD] }
        }

    @Test
    fun `my sell offer below my reputation is flagged on start`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)

            val service = startService()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertTrue(service.state.value.isWarningVisible)
        }

    @Test
    fun `a flagged offer carries its market and formatted amount`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)

            val service = startService()

            assertEquals(
                OffendingOffer(offerId = "offer-1", market = MarketVOFactory.USD, formattedAmount = "50 USD"),
                service.state.value.offendingOffers
                    .single(),
            )
        }

    @Test
    fun `no check runs before the initial data is received`() =
        runTest {
            allDataReceived.value = false
            givenMyOffers(sellOffer())
            givenScore(me, 0)

            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            allDataReceived.value = true
            runCurrent()

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `an offer is flagged when my score drops below the requirement`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, SUFFICIENT_SCORE)
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            givenScore(me, 0)
            runCurrent()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertTrue(service.state.value.isWarningVisible)
        }

    @Test
    fun `no warning when my score covers the offer`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, SUFFICIENT_SCORE)

            val service = startService()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `buy offers are never flagged`() =
        runTest {
            givenMyOffers(sellOffer(direction = DirectionEnum.BUY))
            givenScore(me, 0)

            val service = startService()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `a score within the tolerance still covers the offer`() =
        runTest {
            givenMyOffers(sellOffer())
            // 9,600 * 1.05 = 10,080 >= 10,000 required.
            givenScore(me, 9_600)

            val service = startService()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `an incomplete snapshot is retried until it completes`() =
        runTest {
            snapshots[me.id] = AuthorOffersSnapshot(emptyList(), mayBeIncomplete = true)
            givenScore(me, 0)
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            givenMyOffers(sellOffer())
            advanceTimeBy(RETRY_MS + 1)

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `nothing is flagged when the snapshot stays incomplete`() =
        runTest {
            snapshots[me.id] = AuthorOffersSnapshot(listOf(sellOffer()), mayBeIncomplete = true)
            givenScore(me, 0)

            val service = startService()
            advanceUntilIdle() // spends every retry

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a check before the USD price arrives is retried until it does`() =
        runTest {
            prices.clear()
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            prices[MarketVOFactory.USD] = priceItem(MarketVOFactory.USD)
            advanceTimeBy(RETRY_MS + 1)

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `an offer without a market price counts as valid and the others are still flagged`() =
        runTest {
            val eur = MarketVO("BTC", "EUR", "Bitcoin", "Euro")
            givenMyOffers(sellOffer(id = "eur", market = eur), sellOffer(id = "usd"))
            givenScore(me, 0)

            val service = startService()

            assertEquals(listOf("usd"), service.offendingIds())
        }

    @Test
    fun `flagged offers clear and the warning hides when my score recovers`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()

            givenScore(me, SUFFICIENT_SCORE)
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `switching profile checks the new profile's offers`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            snapshots[other.id] = AuthorOffersSnapshot(listOf(sellOffer(id = "theirs")), false)
            givenScore(me, 0)
            givenScore(other, 0)
            val service = startService()

            selectedProfile.value = other
            runCurrent()

            assertEquals(listOf("theirs"), service.offendingIds())
        }

    @Test
    fun `a refresh after switching profile does not check the previous profile`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            givenScoreBeforeSnapshot(me, 0)
            val service = startService()

            selectedProfile.value = other
            runCurrent()
            service.refresh()
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a retrying check of the previous profile does not publish after a switch`() =
        runTest {
            val other = createMockUserProfile("other")
            snapshots[me.id] = AuthorOffersSnapshot(emptyList(), mayBeIncomplete = true)
            givenScoreBeforeSnapshot(me, 0)
            val service = startService()

            selectedProfile.value = other
            runCurrent()
            givenMyOffers(sellOffer(id = "mine"))
            advanceTimeBy(RETRY_MS + 1)

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `editing my profile keeps what I dismissed`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()
            service.dismiss()

            selectedProfile.value = me.copy(statement = "edited")
            runCurrent()
            service.refresh()
            runCurrent()

            assertEquals(listOf("offer-1"), service.offendingIds())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `dismiss hides the warning but keeps the offers for the badge`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()

            service.dismiss()

            assertFalse(service.state.value.isWarningVisible)
            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `after dismiss the same offers do not show the warning again`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()
            service.dismiss()

            service.refresh()
            runCurrent()

            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `dismiss is remembered per profile`() =
        runTest {
            val other = createMockUserProfile("other")
            givenMyOffers(sellOffer(id = "mine"))
            snapshots[other.id] = AuthorOffersSnapshot(listOf(sellOffer(id = "theirs")), false)
            givenScore(me, 0)
            givenScore(other, 0)
            val service = startService()
            service.dismiss()
            selectedProfile.value = other
            runCurrent()
            assertTrue(service.state.value.isWarningVisible)
            service.dismiss()

            selectedProfile.value = me
            runCurrent()

            assertEquals(listOf("mine"), service.offendingIds())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `after dismiss a newly flagged offer shows the warning again`() =
        runTest {
            givenMyOffers(sellOffer(id = "first"))
            givenScore(me, 0)
            val service = startService()
            service.dismiss()

            givenMyOffers(sellOffer(id = "first"), sellOffer(id = "second"))
            service.refresh()
            runCurrent()

            assertTrue(service.state.value.isWarningVisible)
            assertEquals(listOf("first", "second"), service.offendingIds())
        }

    @Test
    fun `a refresh re-checks my offers`() =
        runTest {
            givenScore(me, 0)
            val service = startService()
            assertTrue(service.offendingIds().isEmpty())

            givenMyOffers(sellOffer())
            service.refresh()
            runCurrent()

            assertEquals(listOf("offer-1"), service.offendingIds())
        }

    @Test
    fun `remove deletes each offer once and clears the warning`() =
        runTest {
            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "b"))
            givenScore(me, 0)
            val service = startService()

            val removedAll = service.removeOffers()

            assertTrue(removedAll)
            coVerify(exactly = 1) { offersService.deleteOffer("a") }
            coVerify(exactly = 1) { offersService.deleteOffer("b") }
            val state = service.state.value
            assertTrue(state.offendingOffers.isEmpty())
            assertFalse(state.isWarningVisible)
            assertFalse(state.isRemoving)
        }

    @Test
    fun `failed removals stay flagged with an error mark`() =
        runTest {
            givenMyOffers(sellOffer(id = "ok"), sellOffer(id = "rejected"), sellOffer(id = "thrown"))
            givenScore(me, 0)
            coEvery { offersService.deleteOffer("rejected") } returns Result.success(false)
            coEvery { offersService.deleteOffer("thrown") } throws IllegalStateException("boom")
            val service = startService()

            val removedAll = service.removeOffers()

            assertFalse(removedAll)
            val state = service.state.value
            assertEquals(listOf("rejected", "thrown"), service.offendingIds())
            assertTrue(state.offendingOffers.all { it.hasRemoveError })
            assertTrue(state.isWarningVisible)
            assertFalse(state.isRemoving)
        }

    @Test
    fun `a removal cancelled by a dropped connection fails and the rest are still removed`() =
        runTest {
            givenMyOffers(sellOffer(id = "dropped"), sellOffer(id = "ok"))
            givenScore(me, 0)
            coEvery { offersService.deleteOffer("dropped") } throws CancellationException("request disposed")
            val service = startService()

            val removedAll = service.removeOffers()

            assertFalse(removedAll)
            coVerify(exactly = 1) { offersService.deleteOffer("ok") }
            assertEquals(listOf("dropped"), service.offendingIds())
            assertTrue(
                service.state.value.offendingOffers
                    .single()
                    .hasRemoveError,
            )
        }

    @Test
    fun `a remove error survives a later check`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            coEvery { offersService.deleteOffer("offer-1") } returns Result.success(false)
            val service = startService()
            service.removeOffers()

            service.refresh()
            runCurrent()

            assertTrue(
                service.state.value.offendingOffers
                    .single()
                    .hasRemoveError,
            )
        }

    @Test
    fun `a score change during a remove is applied once it finishes`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer("offer-1") } coAnswers { deletion.await() }
            val service = startService()
            launch { service.removeOffers() }
            runCurrent()

            givenScore(me, SUFFICIENT_SCORE)
            runCurrent()
            deletion.complete(Result.success(false))
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a retrying check does not publish in the middle of a remove`() =
        runTest {
            givenMyOffers(sellOffer(id = "a"))
            givenScore(me, 0)
            val service = startService()
            snapshots[me.id] = AuthorOffersSnapshot(emptyList(), mayBeIncomplete = true)
            service.refresh()
            runCurrent()
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer("a") } coAnswers { deletion.await() }
            launch { service.removeOffers() }
            runCurrent()

            givenMyOffers(sellOffer(id = "a"), sellOffer(id = "b"))
            advanceTimeBy(RETRY_MS + 1)
            assertEquals(listOf("a"), service.offendingIds())

            deletion.complete(Result.success(true))
            runCurrent()
            assertEquals(listOf("b"), service.offendingIds())
        }

    @Test
    fun `a removed offer still in the cache does not come back`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()
            service.removeOffers()

            // Bisq Connect drops the offer from its cache only when the REMOVED event arrives.
            service.refresh()
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    @Test
    fun `a second remove while removing is ignored`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val deletion = CompletableDeferred<Result<Boolean>>()
            coEvery { offersService.deleteOffer(any()) } coAnswers { deletion.await() }
            val service = startService()

            val first = async { service.removeOffers() }
            runCurrent()
            val second = service.removeOffers()
            deletion.complete(Result.success(true))

            assertTrue(first.await())
            assertFalse(second)
            coVerify(exactly = 1) { offersService.deleteOffer("offer-1") }
        }

    @Test
    fun `an offer deleted elsewhere is dropped for good`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()
            service.dismiss()

            service.markRemoved("offer-1")
            service.refresh()
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `offers deleted elsewhere at the same time are all dropped`() =
        runTest {
            val ids = (1..CONCURRENT_DELETIONS).map { "offer-$it" }
            givenMyOffers(*ids.map { sellOffer(id = it) }.toTypedArray())
            givenScore(me, 0)
            val service = startService()

            ids.chunked(CONCURRENT_DELETIONS / 4).map { chunk -> thread { chunk.forEach(service::markRemoved) } }.forEach { it.join() }
            service.refresh()
            runCurrent()

            assertTrue(service.offendingIds().isEmpty())
        }

    @Test
    fun `starting twice checks once`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()

            service.start()
            runCurrent()

            coVerify(exactly = 1) { offersService.offersByAuthor(me.id) }
        }

    @Test
    fun `stop clears the flagged offers`() =
        runTest {
            givenMyOffers(sellOffer())
            givenScore(me, 0)
            val service = startService()

            service.stop()

            assertTrue(service.offendingIds().isEmpty())
            assertFalse(service.state.value.isWarningVisible)
        }

    private fun TestScope.startService(): OffersBelowReputationService {
        val userProfileService =
            mockk<UserProfileServiceFacade> {
                every { selectedUserProfile } returns this@OffersBelowReputationServiceTest.selectedProfile
            }
        val networkService =
            mockk<NetworkServiceFacade> {
                every { allDataReceived } returns this@OffersBelowReputationServiceTest.allDataReceived
            }
        val configService =
            mockk<ConfigServiceFacade> {
                every { tradeAmountLimits } returns MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
            }
        val service =
            OffersBelowReputationService(
                offersServiceFacade = offersService,
                reputationServiceFacade = reputationService,
                userProfileServiceFacade = userProfileService,
                marketPriceServiceFacade = marketPriceService,
                configServiceFacade = configService,
                networkServiceFacade = networkService,
                dispatcher = StandardTestDispatcher(testScheduler),
            )
        service.start()
        runCurrent()
        return service
    }

    private fun givenMyOffers(vararg offers: OfferItemPresentationModel) {
        snapshots[me.id] = AuthorOffersSnapshot(offers.toList(), mayBeIncomplete = false)
    }

    // observeReputation re-resolves through getReputation whenever my entry in the map changes.
    private fun givenScore(
        profile: UserProfileVO,
        score: Long,
    ) {
        coEvery { reputationService.getReputation(profile.id) } returns Result.success(ReputationScoreVO(score, 0.0, 0))
        scores.value = scores.value + (profile.id to score)
    }

    // With the snapshot still empty, the next profile's score stays unknown instead of resolving to zero.
    private fun givenScoreBeforeSnapshot(
        profile: UserProfileVO,
        score: Long,
    ) {
        coEvery { reputationService.getReputation(profile.id) } returns Result.success(ReputationScoreVO(score, 0.0, 0))
    }

    private fun sellOffer(
        id: String = "offer-1",
        market: MarketVO = MarketVOFactory.USD,
        direction: DirectionEnum = DirectionEnum.SELL,
    ): OfferItemPresentationModel {
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId =
                    NetworkIdVO(
                        AddressByTransportTypeMapVO(mapOf()),
                        PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = me.id),
                    ),
                direction = direction,
                market = market,
                // $50 fixed amount.
                amountSpec = QuoteSideFixedAmountSpecVO(50_0000L),
                priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(USD_PRICE_VALUE, market) }),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationModel(
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = true,
                userProfile = me,
                formattedDate = "",
                formattedQuoteAmount = "50 USD",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = emptyList(),
                baseSidePaymentMethods = emptyList(),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            ),
        )
    }

    private fun priceItem(market: MarketVO) = MarketPriceItem(market, with(PriceQuoteVOFactory) { fromPrice(USD_PRICE_VALUE, market) }, formattedPrice = "")

    private fun OffersBelowReputationService.offendingIds(): List<String> = state.value.offendingOffers.map { it.offerId }

    private companion object {
        const val USD_PRICE_VALUE = 100_000_00L

        // Well above the 10,000 a $50 offer requires.
        const val SUFFICIENT_SCORE = 999_999L
        const val RETRY_MS = 3_000L

        // Enough for unsynchronised writes to the removed ids to collide.
        const val CONCURRENT_DELETIONS = 2_000
    }
}
