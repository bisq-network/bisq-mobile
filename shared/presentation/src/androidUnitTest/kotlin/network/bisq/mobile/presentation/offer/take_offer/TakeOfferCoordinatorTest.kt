package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakePayoutAddressPrepRepository
import network.bisq.mobile.presentation.common.test_utils.FakeTradesServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferCoordinatorTest : PlatformPresentationKoinTestBase() {
    @Test
    fun selectOfferToTake_fixedAmountSpec_noAmountRange() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer with fixed amount
        val fixedAmountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = fixedAmountSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: No amount range, amounts are set from the fixed spec
        assertFalse(presenter.takeOfferModel.hasAmountRange)
        assertFalse(presenter.showAmountScreen())
        assertEquals(500_000L, presenter.takeOfferModel.quoteAmount.value)
        assertTrue(presenter.takeOfferModel.baseAmount.value > 0)
        assertEquals(1, presenter.totalSteps) // No amount screen added
    }

    /**
     * A facade that returns a bare failure without writing to the error flow (the client facade
     * did exactly that) must not leave the flows silent — the presenter dismisses the blocking
     * progress dialog only on an emission.
     */
    @Test
    fun takeOffer_facadeFailureWithoutErrorFlowWrite_populatesErrorMessage() =
        runTest {
            // Arrange: USD market at $100,000/BTC
            val marketUSD = MarketVOFactory.USD
            val marketUSDItem =
                MarketPriceItem(
                    marketUSD,
                    with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                    formattedPrice = "100000 USD",
                )
            val prices = mapOf(marketUSD to marketUSDItem)
            val settingsRepo = SettingsRepositoryMock()
            val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

            val tradesServiceFacade = FakeTradesServiceFacade(Result.failure(RuntimeException("node rejected the request")))
            val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

            val fixedAmountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
            val dto = OfferTestFactory.makeOfferDto(amountSpec = fixedAmountSpec)
            presenter.selectOfferToTake(OfferItemPresentationModel(dto))

            // Act
            val flowResult = presenter.takeOffer()

            // Assert: the failure reason reached the error flow even though the facade never wrote it
            assertEquals("node rejected the request", flowResult.errorMessageFlow.first())
        }

    @Test
    fun selectOfferToTake_wideRange_hasAmountRange() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer with wide range (100_000 to 5_000_000)
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Effective range: 100_000 to 5_000_000
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Has amount range because (5_000_000 - 100_000) >= 10_000
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps) // Amount screen added
    }

    @Test
    fun selectOfferToTake_collapsedRange_noAmountRange_setsFixedAmount() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer where range collapses after clamping
        // Offer range: 1_070_000 to 1_075_000 (difference = 5_000, which is < 10_000 slider step)
        // After clamping with trade limits (60_000 to 6_000_000), effective range is still 1_070_000 to 1_075_000
        // Since (1_075_000 - 1_070_000) = 5_000 < 10_000, range collapses
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 1_070_000L, maxAmount = 1_075_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Range collapsed, amounts set to midpoint
        assertFalse(presenter.takeOfferModel.hasAmountRange)
        assertFalse(presenter.showAmountScreen())
        // Midpoint: (1_070_000 + 1_075_000) / 2 = 1_072_500
        assertEquals(1_072_500L, presenter.takeOfferModel.quoteAmount.value)
        assertTrue(presenter.takeOfferModel.baseAmount.value > 0)
        assertEquals(1, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_missingMarketPrice_fallsBackToShowAmountScreen() {
        // Arrange: Empty prices map (no market price data)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, emptyMap())

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer with range spec
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Falls back to showing amount screen when trade limits are 0
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_invertedRange_fallsBackToShowAmountScreen() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer where min > max trade limit
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Offer min = 7_000_000 > trade limit max = 6_000_000
        // This creates an inverted range: effectiveMin > effectiveMax
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 7_000_000L, maxAmount = 10_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Falls back to showing amount screen for inverted range
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_multiplePaymentMethods_incrementsTotalSteps() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade(), mockk(relaxed = true), FakePayoutAddressPrepRepository())

        // Act: Select offer with wide range and 2 quote payment methods
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto =
            OfferTestFactory.makeOfferDto(
                amountSpec = rangeSpec,
                paymentMethods = listOf("SEPA", "Wise"),
                btcMethods = listOf("BTC"),
            )
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Total steps = 1 (base) + 1 (amount) + 1 (payment methods) = 3
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.takeOfferModel.hasMultipleQuoteSidePaymentMethods)
        assertTrue(presenter.showAmountScreen())
        assertTrue(presenter.showPaymentMethodsScreen())
        assertEquals(3, presenter.totalSteps)
    }

    // ---- firstScreen + checkTakeOfferEligibility, extracted from OfferbookPresenter so
    // ---- every take-offer entry point (offerbook, peer profile) shares one gate and one routing.

    @Test
    fun firstScreen_rangeOffer_startsAtAmountScreen() {
        val coordinator = makeCoordinator()
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        coordinator.selectOfferToTake(OfferItemPresentationModel(OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)))

        assertEquals(NavRoute.TakeOfferTradeAmount, coordinator.firstScreen())
    }

    @Test
    fun firstScreen_fixedAmountSingleMethods_startsAtReview() {
        val coordinator = makeCoordinator()
        val fixedSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        coordinator.selectOfferToTake(OfferItemPresentationModel(OfferTestFactory.makeOfferDto(amountSpec = fixedSpec)))

        assertEquals(NavRoute.TakeOfferReviewTrade, coordinator.firstScreen())
    }

    @Test
    fun firstScreen_fixedAmountMultiplePaymentMethods_startsAtPaymentMethod() {
        val coordinator = makeCoordinator()
        val fixedSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = fixedSpec, paymentMethods = listOf("SEPA", "Wise"))
        coordinator.selectOfferToTake(OfferItemPresentationModel(dto))

        assertEquals(NavRoute.TakeOfferPaymentMethod, coordinator.firstScreen())
    }

    @Test
    fun firstScreen_fixedAmountMultipleSettlementMethods_startsAtSettlementMethod() {
        val coordinator = makeCoordinator()
        val fixedSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = fixedSpec, btcMethods = listOf("MAIN_CHAIN", "LN"))
        coordinator.selectOfferToTake(OfferItemPresentationModel(dto))

        assertEquals(NavRoute.TakeOfferSettlementMethod, coordinator.firstScreen())
    }

    /** SELL offer: the maker is the seller, so it is the MAKER's score that must satisfy the amount. */
    @Test
    fun checkTakeOfferEligibility_sellOffer_makerScoreSufficient_isEligible() =
        runTest {
            val reputationServiceFacade = mockk<ReputationServiceFacade>(relaxed = true)
            coEvery { reputationServiceFacade.getReputation(any()) } returns
                Result.success(ReputationScoreVO(totalScore = 1_000_000L, fiveSystemScore = 5.0, ranking = 1))
            val coordinator = makeCoordinator(reputationServiceFacade)
            // $50 fixed → required score 50 × requiredReputationScorePerUsd (> 0)
            val dto =
                OfferTestFactory.makeOfferDto(
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                    direction = DirectionEnum.SELL,
                )

            val result =
                coordinator.checkTakeOfferEligibility(
                    OfferItemPresentationModel(dto),
                    createMockUserProfile("me"),
                )

            assertEquals(TakeOfferEligibility.Eligible, result)
            // The maker's id from OfferTestFactory is "id"; my own profile must NOT be queried.
            coVerify(exactly = 1) { reputationServiceFacade.getReputation("id") }
            coVerify(exactly = 0) { reputationServiceFacade.getReputation("me") }
        }

    @Test
    fun checkTakeOfferEligibility_sellOffer_makerScoreTooLow_buyerWarning() =
        runTest {
            I18nSupport.initialize("en")
            val reputationServiceFacade = mockk<ReputationServiceFacade>(relaxed = true)
            coEvery { reputationServiceFacade.getReputation(any()) } returns
                Result.success(ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0))
            val coordinator = makeCoordinator(reputationServiceFacade)
            val dto =
                OfferTestFactory.makeOfferDto(
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                    direction = DirectionEnum.SELL,
                )

            val result =
                coordinator.checkTakeOfferEligibility(
                    OfferItemPresentationModel(dto),
                    createMockUserProfile("me"),
                )

            assertIs<TakeOfferEligibility.NotEnoughReputation>(result)
            assertFalse(result.isSellerAsTakerWarning)
            assertEquals("chat.message.takeOffer.buyer.invalidOffer.headline".i18n(), result.headline)
            assertTrue(result.message.isNotBlank())
        }

    /** BUY offer: the taker would sell, so it is MY score that must satisfy the amount. */
    @Test
    fun checkTakeOfferEligibility_buyOffer_myScoreTooLow_sellerAsTakerWarning() =
        runTest {
            I18nSupport.initialize("en")
            val reputationServiceFacade = mockk<ReputationServiceFacade>(relaxed = true)
            coEvery { reputationServiceFacade.getReputation(any()) } returns
                Result.success(ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0))
            val coordinator = makeCoordinator(reputationServiceFacade)
            val dto =
                OfferTestFactory.makeOfferDto(
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                    direction = DirectionEnum.BUY,
                )

            val result =
                coordinator.checkTakeOfferEligibility(
                    OfferItemPresentationModel(dto),
                    createMockUserProfile("me"),
                )

            assertIs<TakeOfferEligibility.NotEnoughReputation>(result)
            assertTrue(result.isSellerAsTakerWarning)
            assertEquals("chat.message.takeOffer.seller.insufficientScore.headline".i18n(), result.headline)
            coVerify(exactly = 1) { reputationServiceFacade.getReputation("me") }
        }

    /**
     * A failed reputation lookup counts as score 0 — the strict policy the offerbook shipped with
     * (the "not cached yet" allowance is deliberately commented out there). Documented here so a
     * future policy change flips a test, not silently both entry points.
     */
    @Test
    fun checkTakeOfferEligibility_reputationLookupFailure_treatsScoreAsZero() =
        runTest {
            I18nSupport.initialize("en")
            val reputationServiceFacade = mockk<ReputationServiceFacade>(relaxed = true)
            coEvery { reputationServiceFacade.getReputation(any()) } returns
                Result.failure(RuntimeException("Reputation of user id not cached yet"))
            val coordinator = makeCoordinator(reputationServiceFacade)
            val dto =
                OfferTestFactory.makeOfferDto(
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                    direction = DirectionEnum.SELL,
                )

            val result =
                coordinator.checkTakeOfferEligibility(
                    OfferItemPresentationModel(dto),
                    createMockUserProfile("me"),
                )

            assertIs<TakeOfferEligibility.NotEnoughReputation>(result)
        }

    // ============== Payout-address step ==============================

    @Test
    fun addressStep_firstTimeBuyerOnSingleMainchain_addsStepAndLeadsWizard() =
        runTest {
            val coordinator = makeAddressStepCoordinator()

            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = "profile-1")

            assertTrue(coordinator.showBtcAddressScreen())
            assertEquals(2, coordinator.totalSteps) // address + review
            assertEquals(NavRoute.TakeOfferBtcAddress, coordinator.firstScreen())
        }

    @Test
    fun addressStep_veteranProfile_neverSeesTheStep() =
        runTest {
            val repo = FakePayoutAddressPrepRepository()
            repo.markTradeCompleted("profile-1")
            val coordinator = makeAddressStepCoordinator(repo)

            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = "profile-1")

            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(1, coordinator.totalSteps)
            assertEquals(NavRoute.TakeOfferReviewTrade, coordinator.firstScreen())
        }

    @Test
    fun addressStep_takerAsSeller_neverSeesTheStep() =
        runTest {
            val coordinator = makeAddressStepCoordinator()

            // BUY offer: the maker buys, so the taker sells and receives fiat, not bitcoin.
            coordinator.selectOfferToTake(
                OfferItemPresentationModel(
                    OfferTestFactory.makeOfferDto(
                        amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                        btcMethods = listOf("MAIN_CHAIN"),
                        direction = DirectionEnum.BUY,
                    ),
                ),
                takerProfileId = "profile-1",
            )

            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(1, coordinator.totalSteps)
        }

    @Test
    fun addressStep_lightningSettlement_neverSeesTheStep() =
        runTest {
            val coordinator = makeAddressStepCoordinator()

            coordinator.selectOfferToTake(
                OfferItemPresentationModel(
                    OfferTestFactory.makeOfferDto(
                        amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                        btcMethods = listOf("LN"),
                        direction = DirectionEnum.SELL,
                    ),
                ),
                takerProfileId = "profile-1",
            )

            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(1, coordinator.totalSteps)
        }

    @Test
    fun addressStep_multiSettlementOffer_stepJoinsAndLeavesWithTheCommittedChoice() =
        runTest {
            val coordinator = makeAddressStepCoordinator()

            coordinator.selectOfferToTake(
                OfferItemPresentationModel(
                    OfferTestFactory.makeOfferDto(
                        amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                        btcMethods = listOf("MAIN_CHAIN", "LN"),
                        direction = DirectionEnum.SELL,
                    ),
                ),
                takerProfileId = "profile-1",
            )

            // Undecided settlement: the step stays out of the count.
            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(2, coordinator.totalSteps) // settlement + review

            coordinator.commitSettlementMethod("MAIN_CHAIN")
            assertTrue(coordinator.showBtcAddressScreen())
            assertEquals(3, coordinator.totalSteps)

            // Re-picking Lightning removes the step again and drops a collected address with it.
            coordinator.commitBtcAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
            coordinator.commitSettlementMethod("LN")
            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(2, coordinator.totalSteps)
            assertEquals("", coordinator.takeOfferModel.btcAddress)
        }

    @Test
    fun addressStep_syncSelectOverload_keepsTheStepOff() {
        val coordinator = makeAddressStepCoordinator()

        coordinator.selectOfferToTake(mainchainSellOffer())

        assertFalse(coordinator.showBtcAddressScreen())
        assertEquals(1, coordinator.totalSteps)
    }

    @Test
    fun takeOffer_collectedAddress_isPersistedForTheNewTrade() =
        runTest {
            val repo = FakePayoutAddressPrepRepository()
            val coordinator = makeAddressStepCoordinator(repo)
            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = "profile-1")
            coordinator.commitBtcAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")

            coordinator.takeOffer()

            assertEquals(
                "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                repo.mutableData.value.prefillByTradeId["trade-1"],
            )
        }

    @Test
    fun takeOffer_skippedAddress_persistsNothing() =
        runTest {
            val repo = FakePayoutAddressPrepRepository()
            val coordinator = makeAddressStepCoordinator(repo)
            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = "profile-1")

            coordinator.takeOffer()

            assertTrue(
                repo.mutableData.value.prefillByTradeId
                    .isEmpty(),
            )
        }

    @Test
    fun warmUp_marksEveryProfileOnTheCompletedHistoryPage() =
        runTest {
            val veteranProfile = createMockUserProfile("history-profile")
            val trade =
                mockk<ClosedTradeListItem> {
                    every { myUserProfile } returns veteranProfile
                }
            val facade =
                ClosedTradesFake(
                    Result.success(PaginatedResponse(listOf(trade), page = 1, pageSize = 100, totalItems = 1L, totalPages = 1)),
                )
            val repo = FakePayoutAddressPrepRepository()
            val coordinator = makeAddressStepCoordinator(repo, facade)

            coordinator.warmUpFirstTimeTraderFlag(veteranProfile.id)

            assertTrue(veteranProfile.id in repo.mutableData.value.profilesWithCompletedTrade)
            // And the gate honours it: the same profile no longer gets the address step.
            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = veteranProfile.id)
            assertFalse(coordinator.showBtcAddressScreen())
        }

    @Test
    fun warmUp_skipsTheHistoryFetchForAKnownVeteran() =
        runTest {
            val facade = ClosedTradesFake(Result.success(PaginatedResponse(emptyList(), 1, 100, 0L, 0)))
            val repo = FakePayoutAddressPrepRepository()
            repo.markTradeCompleted("profile-1")
            val coordinator = makeAddressStepCoordinator(repo, facade)

            coordinator.warmUpFirstTimeTraderFlag("profile-1")

            assertEquals(0, facade.closedTradesRequests)
        }

    @Test
    fun warmUp_historyFailureLeavesTheFlagUntouched() =
        runTest {
            val facade = ClosedTradesFake(Result.failure(RuntimeException("node unreachable")))
            val repo = FakePayoutAddressPrepRepository()
            val coordinator = makeAddressStepCoordinator(repo, facade)

            coordinator.warmUpFirstTimeTraderFlag("profile-1")

            assertTrue(
                repo.mutableData.value.profilesWithCompletedTrade
                    .isEmpty(),
            )
        }

    @Test
    fun warmUp_concurrentCallsForTheSameProfileCoalesceIntoOneFetch() =
        runTest {
            val facade =
                ClosedTradesFake(
                    Result.success(PaginatedResponse(emptyList(), 1, 100, 0L, 0)),
                    delayMillis = 100,
                )
            val coordinator = makeAddressStepCoordinator(FakePayoutAddressPrepRepository(), facade)

            val first = launch { coordinator.warmUpFirstTimeTraderFlag("profile-1") }
            val second = launch { coordinator.warmUpFirstTimeTraderFlag("profile-1") }
            first.join()
            second.join()

            assertEquals(1, facade.closedTradesRequests)

            // A later call finds the session memo and never refetches either.
            coordinator.warmUpFirstTimeTraderFlag("profile-1")
            assertEquals(1, facade.closedTradesRequests)
        }

    @Test
    fun selectOfferToTake_awaitsAnInFlightWarmUpBeforeClassifying() =
        runTest {
            val veteranProfile = createMockUserProfile("history-profile")
            val trade =
                mockk<ClosedTradeListItem> {
                    every { myUserProfile } returns veteranProfile
                }
            val facade =
                ClosedTradesFake(
                    Result.success(PaginatedResponse(listOf(trade), page = 1, pageSize = 100, totalItems = 1L, totalPages = 1)),
                    delayMillis = 100,
                )
            val coordinator = makeAddressStepCoordinator(FakePayoutAddressPrepRepository(), facade)

            // Background warm-up is mid-fetch when the user taps the offer.
            val background = launch { coordinator.warmUpFirstTimeTraderFlag(veteranProfile.id) }
            yield()
            coordinator.selectOfferToTake(mainchainSellOffer(), takerProfileId = veteranProfile.id)
            background.join()

            // The tap coalesced with the in-flight fetch and still classified correctly.
            assertFalse(coordinator.showBtcAddressScreen())
            assertEquals(1, facade.closedTradesRequests)
        }

    private class ClosedTradesFake(
        private val closedTrades: Result<PaginatedResponse<ClosedTradeListItem>>,
        private val delayMillis: Long = 0,
    ) : FakeTradesServiceFacade() {
        var closedTradesRequests = 0

        override suspend fun getClosedTradesPaginated(
            params: PaginationParams,
            search: String?,
            sortBy: TradeSort?,
            outcomeFilter: TradeOutcomeFilter,
            roleFilter: TradeRoleFilter,
        ): Result<PaginatedResponse<ClosedTradeListItem>> {
            closedTradesRequests++
            if (delayMillis > 0) delay(delayMillis)
            return closedTrades
        }
    }

    private fun mainchainSellOffer(): OfferItemPresentationModel =
        OfferItemPresentationModel(
            OfferTestFactory.makeOfferDto(
                amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                btcMethods = listOf("MAIN_CHAIN"),
                direction = DirectionEnum.SELL,
            ),
        )

    private fun makeAddressStepCoordinator(
        payoutAddressPrepRepository: FakePayoutAddressPrepRepository = FakePayoutAddressPrepRepository(),
        tradesServiceFacade: FakeTradesServiceFacade = FakeTradesServiceFacade(),
    ): TakeOfferCoordinator {
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, OfferTestFactory.usdPrices())
        return TakeOfferCoordinator(
            marketPriceServiceFacade,
            tradesServiceFacade,
            FakeConfigServiceFacade(),
            mockk(relaxed = true),
            payoutAddressPrepRepository,
        )
    }

    private fun makeCoordinator(reputationServiceFacade: ReputationServiceFacade = mockk(relaxed = true)): TakeOfferCoordinator {
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, OfferTestFactory.usdPrices())
        return TakeOfferCoordinator(
            marketPriceServiceFacade,
            FakeTradesServiceFacade(),
            FakeConfigServiceFacade(),
            reputationServiceFacade,
            FakePayoutAddressPrepRepository(),
            computationDispatcher = testDispatcher,
        )
    }
}
