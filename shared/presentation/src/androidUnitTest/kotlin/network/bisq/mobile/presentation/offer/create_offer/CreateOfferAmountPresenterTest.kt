package network.bisq.mobile.presentation.offer.create_offer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferAmountPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun fixed_slider_updates_progressively_and_limit_info_updates_on_release() =
        runTest(testDispatcher) {
            // Arrange market prices map (100 USD per BTC)
            val marketUSD = MarketVOFactory.USD
            val marketUSDItem =
                MarketPriceItem(
                    marketUSD,
                    with(PriceQuoteVOFactory) { fromPrice(100_00L, marketUSD) },
                    formattedPrice = "100 USD",
                )
            val prices = mapOf(marketUSD to marketUSDItem)

            // Mock MarketPriceServiceFacade to avoid Koin
            val marketPriceServiceFacade =
                mockk<MarketPriceServiceFacade>(relaxed = true).apply {
                    every { findMarketPriceItem(any()) } answers {
                        val arg = firstArg<MarketVO>()
                        prices.values.firstOrNull { it.market.baseCurrencyCode == arg.baseCurrencyCode && it.market.quoteCurrencyCode == arg.quoteCurrencyCode }
                    }
                    every { findUSDMarketPriceItem() } returns prices[marketUSD]
                    every { refreshSelectedFormattedMarketPrice() } returns Unit
                    every { selectMarket(any()) } returns Result.success(Unit)
                }

            // Mock the Android top-level function accessed by MainPresenter
            mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
            every { getScreenWidthDp() } returns 480

            val mainPresenter =
                MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

            val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
            val createOfferPresenter =
                CreateOfferPresenter(
                    mainPresenter,
                    marketPriceServiceFacade,
                    offersServiceFacade,
                    mockk<SettingsServiceFacade>(relaxed = true),
                )
            // Prepare model with market set
            createOfferPresenter.createOfferModel =
                CreateOfferPresenter.CreateOfferModel().also { m ->
                    m.market = marketUSD
                }

            val amountPresenter =
                CreateOfferAmountPresenter(
                    mainPresenter,
                    marketPriceServiceFacade,
                    createOfferPresenter,
                    mockk<UserProfileServiceFacade>(relaxed = true),
                    mockk<ReputationServiceFacade>(relaxed = true),
                )

            // Let initial init coroutines run
            runCurrent()

            val initialOverlayInfo = amountPresenter.amountLimitInfoOverlayInfo.value
            val beforeQuote = amountPresenter.formattedQuoteSideFixedAmount.value
            val beforeBase = amountPresenter.formattedBaseSideFixedAmount.value

            // Act: progressive updates on drag (heavy conversions/formatting do occur in Create flow)
            amountPresenter.onFixedAmountSliderValueChange(0.75f)
            val midQuote = amountPresenter.formattedQuoteSideFixedAmount.value
            val midBase = amountPresenter.formattedBaseSideFixedAmount.value
            assertNotEquals(beforeQuote, midQuote)
            assertNotEquals(beforeBase, midBase)

            // Heavy reputation/limit overlay should not run during drag
            assertEquals(initialOverlayInfo, amountPresenter.amountLimitInfoOverlayInfo.value)

            // On release, heavy path is allowed to run; should complete without changing mid-drag formatted values
            amountPresenter.onSliderDragFinished()
            advanceTimeBy(0)
            runCurrent()
            // Sanity: formatted values remain the latest ones set during drag
            assertEquals(midQuote, amountPresenter.formattedQuoteSideFixedAmount.value)
            assertEquals(midBase, amountPresenter.formattedBaseSideFixedAmount.value)
        }

    @Test
    fun range_slider_updates_progressively_and_limit_info_updates_on_release() =
        runTest(testDispatcher) {
            // Arrange market prices map (100 USD per BTC)
            val marketUSD = MarketVOFactory.USD
            val marketUSDItem =
                MarketPriceItem(
                    marketUSD,
                    with(PriceQuoteVOFactory) { fromPrice(100_00L, marketUSD) },
                    formattedPrice = "100 USD",
                )
            val prices = mapOf(marketUSD to marketUSDItem)

            // Mock MarketPriceServiceFacade to avoid Koin
            val marketPriceServiceFacade =
                mockk<MarketPriceServiceFacade>(relaxed = true).apply {
                    every { findMarketPriceItem(any()) } answers {
                        val arg = firstArg<MarketVO>()
                        prices.values.firstOrNull { it.market.baseCurrencyCode == arg.baseCurrencyCode && it.market.quoteCurrencyCode == arg.quoteCurrencyCode }
                    }
                    every { findUSDMarketPriceItem() } returns prices[marketUSD]
                    every { refreshSelectedFormattedMarketPrice() } returns Unit
                    every { selectMarket(any()) } returns Result.success(Unit)
                }

            // Mock the Android top-level function accessed by MainPresenter
            mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
            every { getScreenWidthDp() } returns 480

            val mainPresenter =
                MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

            val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
            val createOfferPresenter =
                CreateOfferPresenter(
                    mainPresenter,
                    marketPriceServiceFacade,
                    offersServiceFacade,
                    mockk<SettingsServiceFacade>(relaxed = true),
                )
            // Prepare model with market set
            createOfferPresenter.createOfferModel =
                CreateOfferPresenter.CreateOfferModel().also { m ->
                    m.market = marketUSD
                }

            val amountPresenter =
                CreateOfferAmountPresenter(
                    mainPresenter,
                    marketPriceServiceFacade,
                    createOfferPresenter,
                    mockk<UserProfileServiceFacade>(relaxed = true),
                    mockk<ReputationServiceFacade>(relaxed = true),
                )

            // Let initial init coroutines run
            runCurrent()

            val initialOverlayInfo = amountPresenter.amountLimitInfoOverlayInfo.value
            val beforeMinSlider = amountPresenter.minRangeSliderValue.value
            val beforeMaxSlider = amountPresenter.maxRangeSliderValue.value

            // Act: progressive updates on drag for range (simulate each thumb moving)
            amountPresenter.onMinRangeSliderValueChange(0.3f)
            amountPresenter.onMaxRangeSliderValueChange(0.7f)
            val midMinSlider = amountPresenter.minRangeSliderValue.value
            val midMaxSlider = amountPresenter.maxRangeSliderValue.value
            assertNotEquals(beforeMinSlider, midMinSlider)
            assertNotEquals(beforeMaxSlider, midMaxSlider)

            // Heavy reputation/limit overlay should not run during drag
            assertEquals(initialOverlayInfo, amountPresenter.amountLimitInfoOverlayInfo.value)

            // On release, heavy path is allowed to run; ensure stability of slider positions
            amountPresenter.onSliderDragFinished()
            advanceTimeBy(0)
            runCurrent()
            assertEquals(midMinSlider, amountPresenter.minRangeSliderValue.value)
            assertEquals(midMaxSlider, amountPresenter.maxRangeSliderValue.value)
        }
}
