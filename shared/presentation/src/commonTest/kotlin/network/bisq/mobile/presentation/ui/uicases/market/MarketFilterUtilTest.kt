package network.bisq.mobile.presentation.ui.uicases.market

import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketFilterUtilTest {

    private fun createTestMarket(quoteCurrency: String, quoteCurrencyName: String = "${quoteCurrency} Name"): MarketVO {
        return MarketVO(
            baseCurrencyCode = "BTC",
            quoteCurrencyCode = quoteCurrency,
            baseCurrencyName = "Bitcoin",
            quoteCurrencyName = quoteCurrencyName
        )
    }

    private fun createTestMarketListItem(quoteCurrency: String, numOffers: Int = 0, quoteCurrencyName: String = "${quoteCurrency} Name"): MarketListItem {
        return MarketListItem(
            market = createTestMarket(quoteCurrency, quoteCurrencyName),
            numOffers = numOffers,
            ""
        )
    }

    // Note: Tests for filterMarketsWithPriceData are skipped because they require
    // MarketPriceServiceFacade which has complex dependencies. The core filtering
    // logic is tested in the domain module's GlobalPriceUpdateTest.

    @Test
    fun `filterMarketsBySearch should return all markets when search is blank`() {
        // Given
        val markets = listOf(
            createTestMarketListItem("USD"),
            createTestMarketListItem("EUR"),
            createTestMarketListItem("GBP")
        )

        // When
        val result = MarketFilterUtil.filterMarketsBySearch(markets, "")

        // Then
        assertEquals(markets, result)
    }

    @Test
    fun `filterMarketsBySearch should filter by currency code case insensitive`() {
        // Given
        val markets = listOf(
            createTestMarketListItem("USD"),
            createTestMarketListItem("EUR"),
            createTestMarketListItem("GBP")
        )

        // When
        val result = MarketFilterUtil.filterMarketsBySearch(markets, "usd")

        // Then
        assertEquals(1, result.size)
        assertEquals("USD", result[0].market.quoteCurrencyCode)
    }

    @Test
    fun `filterMarketsBySearch should filter by currency name case insensitive`() {
        // Given
        val markets = listOf(
            createTestMarketListItem("USD", quoteCurrencyName = "US Dollar"),
            createTestMarketListItem("EUR", quoteCurrencyName = "Euro"),
            createTestMarketListItem("GBP", quoteCurrencyName = "British Pound")
        )

        // When
        val result = MarketFilterUtil.filterMarketsBySearch(markets, "dollar")

        // Then
        assertEquals(1, result.size)
        assertEquals("USD", result[0].market.quoteCurrencyCode)
    }

    @Test
    fun `filterMarketsBySearch should return multiple matches`() {
        // Given
        val markets = listOf(
            createTestMarketListItem("USD", quoteCurrencyName = "US Dollar"),
            createTestMarketListItem("CAD", quoteCurrencyName = "Canadian Dollar"),
            createTestMarketListItem("EUR", quoteCurrencyName = "Euro")
        )

        // When
        val result = MarketFilterUtil.filterMarketsBySearch(markets, "dollar")

        // Then
        assertEquals(2, result.size)
        assertEquals(setOf("USD", "CAD"), result.map { it.market.quoteCurrencyCode }.toSet())
    }

    // Note: sortMarketsStandard is internal, so we can't test it directly.
    // The sorting logic is tested indirectly through the integration with the actual presenters.
}
