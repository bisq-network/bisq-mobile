package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferReviewPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager() }
                },
            )
        }
        I18nSupport.initialize("en")
        // MainPresenter.init touches a platform-specific helper transitively; stub it.
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
     * Once SUCCESS arrives, the guard must stay engaged — the user is meant to leave
     * the screen via the success dialog. Resetting the guard would expose the button
     * again if the success dialog were dismissed unexpectedly.
     */
    @Test
    fun `success path keeps guard engaged so a subsequent tap is ignored`() =
        runTest(testDispatcher) {
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

    // ---- fixture ----

    private data class Fixture(
        val presenter: TakeOfferReviewPresenter,
        val coordinator: TakeOfferCoordinator,
        val statusFlow: MutableStateFlow<TakeOfferStatus?>,
        val errorFlow: MutableStateFlow<String?>,
    )

    private fun makeFixture(): Fixture {
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
