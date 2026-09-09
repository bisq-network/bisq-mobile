package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferErrorDialog
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferReviewPresenterTest : PlatformPresentationKoinTestBase() {
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
            fixture.presenter.onTakeOffer()
            fixture.presenter.onTakeOffer()
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()
            assertTrue(fixture.presenter.showTakeOfferProgressDialog.value, "progress dialog should be up before error")

            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            assertFalse(fixture.presenter.showTakeOfferProgressDialog.value, "progress dialog should be hidden after error")

            // Retry should now succeed
            fixture.presenter.onTakeOffer()
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()
            fixture.errorFlow.value = "boom"
            advanceUntilIdle()
            assertFalse(fixture.presenter.showTakeOfferProgressDialog.value)

            // Retry: the coordinator hands back the same flows, whose error value is still "boom".
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            assertFalse(
                fixture.presenter.showTakeOfferProgressDialog.value,
                "progress dialog must be dismissed on a repeat of the same error",
            )
            // Guard must be released again: a third attempt reaches the coordinator.
            fixture.presenter.onTakeOffer()
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()

            assertTrue(fixture.presenter.showTakeOfferSuccessDialog.value, "success dialog should be up")
            assertFalse(fixture.presenter.showTakeOfferProgressDialog.value, "progress dialog should be down")

            // Phantom tap after success — must be ignored.
            fixture.presenter.onTakeOffer()
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            advanceUntilIdle()

            val dialog = fixture.presenter.takeOfferErrorDialog.value
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.errorFlow.value =
                "Invalid input: An error occurred at the peers side at taking the offer: $raw. " +
                "ErrorStackTrace: bisq.trade.exceptions.TradeProtocolException: $raw"
            advanceUntilIdle()

            val dialog = fixture.presenter.takeOfferErrorDialog.value
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()
            assertTrue(fixture.presenter.showTakeOfferSuccessDialog.value)

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            advanceUntilIdle()

            assertFalse(fixture.presenter.showTakeOfferSuccessDialog.value)
            val dialog = fixture.presenter.takeOfferErrorDialog.value
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
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.errorFlow.value = "The trade failed: '$raw'\n\nStack trace: TradeProtocolException"
            fixture.statusFlow.value = TakeOfferStatus.SUCCESS
            advanceUntilIdle()

            assertFalse(fixture.presenter.showTakeOfferSuccessDialog.value)
            assertIs<TakeOfferErrorDialog.ProtocolFailure>(fixture.presenter.takeOfferErrorDialog.value)
        }

    @Test
    fun `timeout class name is shown as an unexpected error not Trade Failed`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.errorFlow.value = "java.util.concurrent.TimeoutException"
            advanceUntilIdle()

            val dialog = fixture.presenter.takeOfferErrorDialog.value
            assertIs<TakeOfferErrorDialog.Unexpected>(dialog)
            assertEquals("java.util.concurrent.TimeoutException", dialog.message)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `unexpected take-offer error opens a general error dialog not a snackbar`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()

            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            val dialog = fixture.presenter.takeOfferErrorDialog.value
            assertIs<TakeOfferErrorDialog.Unexpected>(dialog)
            assertEquals("boom", dialog.message)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `dismissing the take-offer error dialog clears it so the user can retry`() =
        runTest {
            val fixture = makeFixture()
            fixture.presenter.onTakeOffer()
            advanceUntilIdle()
            fixture.errorFlow.value = "boom"
            advanceUntilIdle()

            fixture.presenter.onDismissTakeOfferError()

            assertNull(fixture.presenter.takeOfferErrorDialog.value)
        }

    @Test
    fun `the support channel is offered when discussions is live`() =
        runTest {
            val fixture = makeFixture(discussionsLive = true)
            assertTrue(fixture.presenter.isSupportChannelAvailable.value)
        }

    @Test
    fun `the support channel is withheld when discussions is not live`() =
        runTest {
            val fixture = makeFixture(discussionsLive = false)
            assertFalse(fixture.presenter.isSupportChannelAvailable.value)
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
            assertTrue(fixture.presenter.isSupportChannelAvailable.value)

            capabilities.value = BackendCapabilities.UNAVAILABLE
            advanceUntilIdle()

            assertFalse(fixture.presenter.isSupportChannelAvailable.value)
        }

    @Test
    fun `opening the support channel navigates to it`() =
        runTest {
            val fixture = makeFixture(discussionsLive = true)
            fixture.presenter.onOpenSupportChannel()
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

    private fun TestScope.makeFixture(
        discussionsLive: Boolean = false,
        capabilities: MutableStateFlow<BackendCapabilities> = MutableStateFlow(BackendCapabilities.UNAVAILABLE),
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): Fixture {
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true)
        every { marketPriceServiceFacade.findMarketPriceItem(any()) } returns null

        val coordinator = mockk<TakeOfferCoordinator>(relaxed = true)
        every { coordinator.takeOfferModel } returns makeTakeOfferModel()

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

    private fun makeTakeOfferModel(): TakeOfferCoordinator.TakeOfferModel {
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
                direction = DirectionEnum.BUY,
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
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
