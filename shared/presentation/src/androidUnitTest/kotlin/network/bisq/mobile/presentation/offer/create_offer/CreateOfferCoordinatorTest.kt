package network.bisq.mobile.presentation.offer.create_offer

import io.mockk.mockk
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateOfferCoordinatorTest : PlatformPresentationKoinTestBase() {
    private val eurMarket = MarketVO("BTC", "EUR", "Bitcoin", "Euro")

    @AfterTest
    fun resetDemoFlag() {
        ApplicationBootstrapFacade.isDemo = false
    }

    private fun coordinator(prices: Map<MarketVO, MarketPriceItem>) =
        CreateOfferCoordinator(
            FakeMarketPriceServiceFacade(SettingsRepositoryMock(), prices),
            mockk(relaxed = true),
            mockk(relaxed = true),
        ).apply { onStartCreateOffer() }

    @Test
    fun commitMarket_noPriceQuote_isRejectedAndLeavesTheModelUntouched() {
        val coordinator = coordinator(OfferTestFactory.usdPrices())

        val committed = coordinator.commitMarket(eurMarket)

        assertFalse(committed)
        assertNull(coordinator.createOfferModel.market)
    }

    @Test
    fun commitMarket_withPriceQuote_storesTheFeedQuote() {
        val eurItem = OfferTestFactory.makeMarketPriceItem(eurMarket, 90_000_00L)
        val coordinator = coordinator(mapOf(eurMarket to eurItem))

        val committed = coordinator.commitMarket(eurMarket)

        assertTrue(committed)
        assertEquals(eurMarket, coordinator.createOfferModel.market)
        assertEquals(eurItem.priceQuote, coordinator.createOfferModel.priceQuote)
    }

    @Test
    fun commitMarket_inDemo_usesTheDemoNodePriceFeed() {
        ApplicationBootstrapFacade.isDemo = true
        val eurItem = OfferTestFactory.makeMarketPriceItem(eurMarket, 90_000_00L)
        val coordinator = coordinator(mapOf(eurMarket to eurItem))

        assertTrue(coordinator.commitMarket(eurMarket))

        assertEquals(eurItem.priceQuote, coordinator.createOfferModel.priceQuote)
    }

    @Test
    fun commitMarket_inDemo_marketWithoutPrice_isRejected() {
        ApplicationBootstrapFacade.isDemo = true
        val coordinator = coordinator(OfferTestFactory.usdPrices())

        assertFalse(coordinator.commitMarket(MarketVO("BTC", "COP")))
    }

    @Test
    fun getMostRecentPriceQuote_priceGoneAfterCommit_keepsTheCommittedMarketQuote() {
        val prices = OfferTestFactory.usdPrices().toMutableMap()
        val coordinator = coordinator(prices)
        coordinator.commitMarket(MarketVOFactory.USD)
        val committedQuote = coordinator.createOfferModel.originalPriceQuote

        prices.clear()

        assertEquals(committedQuote, coordinator.getMostRecentPriceQuote(MarketVOFactory.USD))
    }
}
