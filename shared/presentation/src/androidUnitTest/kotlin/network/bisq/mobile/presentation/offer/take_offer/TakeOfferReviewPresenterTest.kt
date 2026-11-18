package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.data.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.data.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsDisplayMode
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferErrorDialog
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewUiAction
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewUiState
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferReviewPresenterTest : PlatformPresentationKoinTestBase() {
    /**
     * The X abandons the whole wizard: it pops back to one step below the flow's FIRST screen —
     * the offerbook, the peer profile, or the peer-offers screen, whichever launched it —
     * instead of unconditionally jumping to the offerbook tab (which stranded profile-launched
     * flows in the wrong place).
     */
    @Test
    fun `the X close pops the wizard back to wherever the flow was entered from`() =
        runTest {
            val fixture = makeFixture()
            every { fixture.coordinator.firstScreen() } returns NavRoute.TakeOfferTradeAmount

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnClose)
            advanceUntilIdle()

            verify {
                navigationManager.navigateBackTo(NavRoute.TakeOfferTradeAmount, true, false)
            }
        }

    /**
     * Rapid double-tap on the "Take offer" button must trigger the underlying
     * [TakeOfferCoordinator.takeOffer] only once. The atomic compareAndSet guard
     * is the structural protection — the progress dialog alone is not modal enough,
     * especially on the android node app where the API returns near-instantly.
     */
    @Test
    fun `rapid double-tap on onTakeOffer triggers underlying takeOffer only once`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            coVerify(exactly = 1) { fixture.coordinator.takeOffer() }
        }

    /**
     * After an error arrives on the error flow, the guard must be released so the
     * user can retry. The progress dialog must also be hidden (without this fix,
     * an error left the dialog up indefinitely).
     */
    @Test
    fun `error path releases guard and hides progress dialog`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            assertTrue(fixture.presenter.uiState.value.showTakeOfferProgressDialog, "progress dialog should be up before error")

            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            assertFalse(fixture.presenter.uiState.value.showTakeOfferProgressDialog, "progress dialog should be hidden after error")

            // Retry should now succeed
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            coVerify(exactly = 2) { fixture.coordinator.takeOffer() }
        }

    /**
     * A retry that fails with the exact same error message must still dismiss the progress
     * dialog. Without the per-attempt reset, the presenter's error StateFlow deduplicates the
     * identical value, no emission happens, and the dialog is stuck again.
     */
    @Test
    fun `retry failing with the same error message dismisses the dialog again`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            fixture.errorFlow.value = "boom"
            advanceUntilIdle()
            assertFalse(fixture.presenter.uiState.value.showTakeOfferProgressDialog)

            // Retry: the coordinator hands back the same flows, whose error value is still "boom".
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            assertFalse(
                fixture.presenter.uiState.value.showTakeOfferProgressDialog,
                "progress dialog must be dismissed on a repeat of the same error",
            )
            // Guard must be released again: a third attempt reaches the coordinator.
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            coVerify(exactly = 3) { fixture.coordinator.takeOffer() }
        }

    /**
     * Once SUCCESS arrives, the guard must stay engaged — the user is meant to leave
     * the screen via the success dialog. Resetting the guard would expose the button
     * again if the success dialog were dismissed unexpectedly.
     */
    @Test
    fun `success path keeps guard engaged so a subsequent tap is ignored`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()

            assertTrue(fixture.presenter.uiState.value.showTakeOfferSuccessDialog, "success dialog should be up")
            assertFalse(fixture.presenter.uiState.value.showTakeOfferProgressDialog, "progress dialog should be down")

            // Phantom tap after success — must be ignored.
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            coVerify(exactly = 1) { fixture.coordinator.takeOffer() }
        }

    @Test
    fun `price deviation rejection opens Trade Failed dialog not a snackbar`() =
        runTest {
            val fixture = makeFixture()
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            advanceUntilIdle()

            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.ProtocolFailure>(dialog)
            assertEquals(raw, dialog.message)
            assertFalse(dialog.atPeer)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `a peer-side protocol rejection uses the at-peer headline flag`() =
        runTest {
            val fixture = makeFixture()
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.errorFlow.value =
                "Invalid input: An error occurred at the peers side at taking the offer: $raw. " +
                "ErrorStackTrace: bisq.trade.exceptions.TradeProtocolException: $raw"
            advanceUntilIdle()

            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.ProtocolFailure>(dialog)
            assertEquals(raw, dialog.message)
            assertTrue(dialog.atPeer)
        }

    @Test
    fun `a protocol rejection after SUCCESS hides the success dialog`() =
        runTest {
            val fixture = makeFixture()
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()
            assertTrue(fixture.presenter.uiState.value.showTakeOfferSuccessDialog)

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            advanceUntilIdle()

            assertFalse(fixture.presenter.uiState.value.showTakeOfferSuccessDialog)
            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.ProtocolFailure>(dialog)
            assertEquals(raw, dialog.message)
        }

    @Test
    fun `SUCCESS after a protocol rejection does not open the success dialog`() =
        runTest {
            val fixture = makeFixture()
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()

            assertFalse(fixture.presenter.uiState.value.showTakeOfferSuccessDialog)
            assertIs<TakeOfferErrorDialog.ProtocolFailure>(fixture.presenter.uiState.value.takeOfferErrorDialog)
        }

    @Test
    fun `timeout class name is shown as an unexpected error not Trade Failed`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.errorFlow.value = "java.util.concurrent.TimeoutException"
            advanceUntilIdle()

            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.Unexpected>(dialog)
            assertEquals("java.util.concurrent.TimeoutException", dialog.message)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `job cancellation is not shown as a trade failure`() =
        runTest {
            val fixture = makeFixture()
            coEvery { fixture.coordinator.takeOffer() } throws CancellationException("navigated away")

            try {
                fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
                advanceUntilIdle()
            } catch (_: CancellationException) {
            }

            assertNull(fixture.presenter.uiState.value.takeOfferErrorDialog)
            assertFalse(fixture.presenter.uiState.value.showTakeOfferProgressDialog)
        }

    @Test
    fun `a timed-out take still shows the send-timed-out copy`() =
        runTest {
            I18nSupport.initialize("en")
            val fixture = makeFixture()

            // simpleName must contain "TimeoutCancellation" — same check as isTimeout().
            class TimeoutCancellationException : CancellationException("timed out")
            coEvery { fixture.coordinator.takeOffer() } throws TimeoutCancellationException()

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.Unexpected>(dialog)
            assertTrue(dialog.message.contains("timed out"), dialog.message)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `unexpected take-offer error opens a general error dialog not a snackbar`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()

            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            val dialog = fixture.presenter.uiState.value.takeOfferErrorDialog
            assertIs<TakeOfferErrorDialog.Unexpected>(dialog)
            assertEquals("boom", dialog.message)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `dismissing the take-offer error dialog clears it so the user can retry`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnTakeOffer)
            advanceUntilIdle()
            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnDismissTakeOfferError)

            assertNull(fixture.presenter.uiState.value.takeOfferErrorDialog)
        }

    @Test
    fun `the support channel is offered when discussions is live`() =
        runTest {
            val fixture = makeFixture(discussionsLive = true)
            assertTrue(fixture.presenter.uiState.value.isSupportChannelAvailable)
        }

    @Test
    fun `the support channel is withheld when discussions is not live`() =
        runTest {
            val fixture = makeFixture(discussionsLive = false)
            assertFalse(fixture.presenter.uiState.value.isSupportChannelAvailable)
        }

    @Test
    fun `the support channel is withdrawn when discussions stops being live`() =
        runTest {
            val capabilities = MutableStateFlow(BackendCapabilities(setOf(Feature.PRIVATE_CHAT.key)))
            val fixture =
                makeFixture(
                    discussionsLive = true,
                    capabilities = capabilities,
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.PRIVATE_CHAT),
                )
            assertTrue(fixture.presenter.uiState.value.isSupportChannelAvailable)

            capabilities.value = BackendCapabilities.UNAVAILABLE
            advanceUntilIdle()

            assertFalse(fixture.presenter.uiState.value.isSupportChannelAvailable)
        }

    @Test
    fun `opening the support channel navigates to it`() =
        runTest {
            val fixture = makeFixture(discussionsLive = true)
            fixture.presenter.onAction(TakeOfferReviewUiAction.OnOpenSupportChannel)
            advanceUntilIdle()
            verify { navigationManager.navigate(NavRoute.SupportChannel, any(), any()) }
        }

    // ---- fixture ----

    private data class Fixture(
        val presenter: TakeOfferReviewPresenter,
        val coordinator: TakeOfferCoordinator,
        val statusFlow: MutableStateFlow<TakeOfferStatus?>,
        val errorFlow: MutableStateFlow<String?>,
    )

    // ============== Maker's trade terms ============================

    @Test
    fun `an offer without trade terms shows no terms`() =
        runTest {
            val fixture = makeFixture(offerOptions = listOf(ReputationOptionVO(requiredTotalReputationScore = 30_000L)))

            assertNull(fixture.presenter.uiState.value.tradeTerms)
        }

    @Test
    fun `an offer with whitespace-only trade terms shows no terms`() =
        runTest {
            val fixture = makeFixture(offerOptions = listOf(TradeTermsOptionVO("  \n\t ")))

            assertNull(fixture.presenter.uiState.value.tradeTerms)
        }

    @Test
    fun `an offer with trade terms shows them trimmed and collapsed`() =
        runTest {
            val fixture =
                makeFixture(
                    offerOptions =
                        listOf(
                            ReputationOptionVO(requiredTotalReputationScore = 30_000L),
                            TradeTermsOptionVO("\n  I only accept payment from an account in your own name.  \n"),
                        ),
                )

            assertEquals(
                TradeTermsUiState(
                    text = "I only accept payment from an account in your own name.",
                    displayMode = TradeTermsDisplayMode.COLLAPSIBLE,
                    isExpanded = false,
                ),
                fixture.presenter.uiState.value.tradeTerms,
            )
        }

    @Test
    fun `trade terms longer than the review cap are cut to it and flagged`() =
        runTest {
            // bisq2 accepts up to 10,000 characters on an offer; the review step shows at most 2,000.
            val fixture = makeFixture(offerOptions = listOf(TradeTermsOptionVO("t".repeat(10_000))))

            val terms = assertNotNull(fixture.presenter.uiState.value.tradeTerms)
            assertEquals("t".repeat(2_000), terms.text)
            assertTrue(terms.isTruncated)
        }

    @Test
    fun `toggling the trade terms expands them and toggling again collapses them`() =
        runTest {
            val fixture = makeFixture(offerOptions = listOf(TradeTermsOptionVO("Own-name account only.")))

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnToggleTradeTerms)
            assertTrue(assertNotNull(fixture.presenter.uiState.value.tradeTerms).isExpanded)

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnToggleTradeTerms)
            assertFalse(assertNotNull(fixture.presenter.uiState.value.tradeTerms).isExpanded)
        }

    @Test
    fun `toggling when the offer has no trade terms leaves them absent`() =
        runTest {
            val fixture = makeFixture()

            fixture.presenter.onAction(TakeOfferReviewUiAction.OnToggleTradeTerms)

            assertNull(fixture.presenter.uiState.value.tradeTerms)
        }

    // ============== Payout-address notice ============================

    @Test
    fun `a first-time buyer without an address sees the mainchain announce notice`() =
        runTest {
            val presenter = makeBuyerReviewPresenter(isFirstTimeTrader = true)

            assertIs<TakeOfferReviewUiState.AddressNotice.MainchainAnnounce>(presenter.uiState.value.addressNotice)
        }

    @Test
    fun `a buyer with a collected address sees the confirmed notice with a truncated address`() =
        runTest {
            val presenter =
                makeBuyerReviewPresenter(
                    isFirstTimeTrader = true,
                    btcAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                )

            val notice = presenter.uiState.value.addressNotice
            assertIs<TakeOfferReviewUiState.AddressNotice.MainchainConfirmed>(notice)
            assertEquals("bc1qw508d6…f3t4", notice.truncatedAddress)
        }

    @Test
    fun `a veteran mainchain buyer sees no notice`() =
        runTest {
            val presenter = makeBuyerReviewPresenter(isFirstTimeTrader = false)

            assertNull(presenter.uiState.value.addressNotice)
        }

    @Test
    fun `a lightning buyer sees the lightning notice regardless of experience`() =
        runTest {
            val veteran = makeBuyerReviewPresenter(baseSidePaymentMethod = "LN", isFirstTimeTrader = false)
            val firstTimer = makeBuyerReviewPresenter(baseSidePaymentMethod = "LN", isFirstTimeTrader = true)

            assertIs<TakeOfferReviewUiState.AddressNotice.Lightning>(veteran.uiState.value.addressNotice)
            assertIs<TakeOfferReviewUiState.AddressNotice.Lightning>(firstTimer.uiState.value.addressNotice)
        }

    @Test
    fun `a taker who sells sees no notice at all`() =
        runTest {
            // Default fixture: BUY offer, taker sells — they receive fiat, not bitcoin.
            val fixture = makeFixture()

            assertNull(fixture.presenter.uiState.value.addressNotice)
        }

    private fun TestScope.makeBuyerReviewPresenter(
        baseSidePaymentMethod: String = "MAIN_CHAIN",
        btcAddress: String = "",
        isFirstTimeTrader: Boolean = false,
    ): TakeOfferReviewPresenter {
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true)
        every { marketPriceServiceFacade.findMarketPriceItem(any()) } returns null

        // SELL offer: the maker sells, so the taker is the buyer receiving bitcoin.
        val model =
            makeTakeOfferModel(direction = DirectionEnum.SELL).apply {
                this.baseSidePaymentMethod = baseSidePaymentMethod
                this.btcAddress = btcAddress
                this.isFirstTimeTrader = isFirstTimeTrader
            }
        val coordinator = mockk<TakeOfferCoordinator>(relaxed = true)
        every { coordinator.takeOfferModel } returns model

        return TakeOfferReviewPresenter(
            MainPresenterTestFactory.create(),
            marketPriceServiceFacade,
            coordinator,
            testCommunityHubService(
                enabled = emptySet(),
                requiredFeatures = emptyMap(),
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            ),
        )
    }

    private fun TestScope.makeFixture(
        discussionsLive: Boolean = false,
        capabilities: MutableStateFlow<BackendCapabilities> = MutableStateFlow(BackendCapabilities.UNAVAILABLE),
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
        offerOptions: List<OfferOptionVO> = emptyList(),
    ): Fixture {
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true)
        every { marketPriceServiceFacade.findMarketPriceItem(any()) } returns null

        val coordinator = mockk<TakeOfferCoordinator>(relaxed = true)
        every { coordinator.takeOfferModel } returns makeTakeOfferModel(offerOptions = offerOptions)

        val statusFlow = MutableStateFlow<TakeOfferStatus?>(null)
        val errorFlow = MutableStateFlow<String?>(null)
        coEvery { coordinator.takeOffer() } returns TakeOfferFlowResult(statusFlow, errorFlow)

        val presenter =
            TakeOfferReviewPresenter(
                MainPresenterTestFactory.create(),
                marketPriceServiceFacade,
                coordinator,
                testCommunityHubService(
                    enabled = if (discussionsLive) setOf(CommunitySegment.DISCUSSIONS) else emptySet(),
                    requiredFeatures = requiredFeatures,
                    capabilities = capabilities,
                    dispatcher = UnconfinedTestDispatcher(testScheduler),
                ),
            )
        return Fixture(presenter, coordinator, statusFlow, errorFlow)
    }

    private fun makeTakeOfferModel(
        direction: DirectionEnum = DirectionEnum.BUY,
        offerOptions: List<OfferOptionVO> = emptyList(),
    ): TakeOfferCoordinator.TakeOfferModel {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) })
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
            )
        val offer =
            BisqEasyOfferVO(
                id = "offer-1",
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = direction,
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = offerOptions,
                supportedLanguageCodes = emptyList(),
            )
        val dto =
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = false,
                userProfile = createMockUserProfile("Alice"),
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("BTC"),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            )
        val priceQuote = with(PriceQuoteVOFactory) { fromPrice(100_00L, market) }
        return TakeOfferCoordinator.TakeOfferModel().apply {
            offerItemPresentationVO = OfferItemPresentationModel(dto)
            originalPriceQuote = priceQuote
            this.priceQuote = priceQuote
            quoteAmount = with(FiatVOFactory) { fromFaceValue(50.0, "USD") }
            baseAmount = with(CoinVOFactory) { fromFaceValue(0.0005, "BTC") }
            quoteSidePaymentMethod = "SEPA"
            baseSidePaymentMethod = "MAIN_CHAIN"
        }
    }
}
