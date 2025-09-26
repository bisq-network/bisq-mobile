package network.bisq.mobile.presentation.ui.uicases.take_offer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.bitcoinFrom
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.presentation.MainPresenter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.Ignore

class TakeOfferDoubleSubmissionTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Ignore("Covered by UI-level interactivity lock on the review screen")
    @Test
    fun takeOffer_isCalledOnlyOnce_whenInvokedTwiceQuickly() = runTest(testDispatcher) {
        // Arrange
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        val marketPriceService = mockk<MarketPriceServiceFacade>(relaxed = true)
        every { marketPriceService.findMarketPriceItem(any()) } returns mockk<MarketPriceItem>(relaxed = true)

        val tradesService = mockk<TradesServiceFacade>()
        coEvery { tradesService.takeOffer(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            // Simulate a bit of work so the second call overlaps
            delay(200)
            Result.success("trade-1")
        }
        coEvery { tradesService.selectOpenTrade(any()) } returns Unit

        val presenter = TakeOfferPresenter(mainPresenter, marketPriceService, tradesService)
        presenter.takeOfferModel = TakeOfferPresenter.TakeOfferModel().apply {
            offerItemPresentationVO = mockk(relaxed = true)
            baseAmount = CoinVOFactory.bitcoinFrom(1000)
            quoteAmount = FiatVOFactory.from(10000, "JPY")
            baseSidePaymentMethod = "BTC_ONCHAIN"
            quoteSidePaymentMethod = "WISE"
        }

        // Act: launch 2 concurrent invocations
        val first = async { presenter.takeOffer() }
        val second = async { presenter.takeOffer() }
        first.await()
        second.await()

        // Assert: service takeOffer should be called exactly once
        coVerify(exactly = 1) { tradesService.takeOffer(any(), any(), any(), any(), any(), any(), any()) }
    }
}

