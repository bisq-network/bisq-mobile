package network.bisq.mobile.data.model.offerbook

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.utils.setDefaultLocale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MarketListItemTest {
    @BeforeTest
    fun setup() {
        setDefaultLocale("en")
    }

    @Test
    fun `withLocaleFiatCurrencyName returns same item when already localized for language`() {
        val marketItem =
            MarketListItem(
                market = testMarket("USD", "US Dollar"),
                numOffers = 1,
                localeFiatCurrencyName = "US Dollar",
                localizedCurrencyLocaleTag = "en",
            )

        val result = marketItem.withLocaleFiatCurrencyName("en")

        assertSame(marketItem, result)
    }

    @Test
    fun `withLocaleFiatCurrencyName relocalizes when language tag differs`() {
        val marketItem = MarketListItem.from(testMarket("EUR", "Euro"), numOffers = 2)

        val result = marketItem.withLocaleFiatCurrencyName("en")

        assertNotSame(marketItem, result)
        assertEquals("en", result.localizedCurrencyLocaleTag)
        assertTrue(result.localeFiatCurrencyName.isNotBlank())
        assertEquals(marketItem.market, result.market)
        assertEquals(marketItem.numOffers, result.numOffers)
    }

    @Test
    fun `withLocaleFiatCurrencyName refreshes blank names even with same language tag`() {
        val marketItem =
            MarketListItem(
                market = testMarket("GBP", "British Pound"),
                numOffers = 3,
                localeFiatCurrencyName = "",
                localizedCurrencyLocaleTag = "en",
            )

        val result = marketItem.withLocaleFiatCurrencyName("en")

        assertNotSame(marketItem, result)
        assertEquals("en", result.localizedCurrencyLocaleTag)
        assertTrue(result.localeFiatCurrencyName.isNotBlank())
    }

    @Test
    fun `withLocaleFiatCurrencyNames returns original list when all items are already localized`() {
        val items =
            listOf(
                MarketListItem(
                    market = testMarket("USD", "US Dollar"),
                    numOffers = 1,
                    localeFiatCurrencyName = "US Dollar",
                    localizedCurrencyLocaleTag = "en",
                ),
                MarketListItem(
                    market = testMarket("EUR", "Euro"),
                    numOffers = 2,
                    localeFiatCurrencyName = "Euro",
                    localizedCurrencyLocaleTag = "en",
                ),
            )

        val result = items.withLocaleFiatCurrencyNames("en")

        assertSame(items, result)
    }

    @Test
    fun `withLocaleFiatCurrencyNames creates new list only after first changed item`() {
        val unchangedItem =
            MarketListItem(
                market = testMarket("USD", "US Dollar"),
                numOffers = 1,
                localeFiatCurrencyName = "US Dollar",
                localizedCurrencyLocaleTag = "en",
            )
        val changedItem = MarketListItem.from(testMarket("CAD", "Canadian Dollar"), numOffers = 2)
        val items = listOf(unchangedItem, changedItem)

        val result = items.withLocaleFiatCurrencyNames("en")

        assertNotSame(items, result)
        assertSame(unchangedItem, result[0])
        assertNotSame(changedItem, result[1])
        assertEquals("en", result[1].localizedCurrencyLocaleTag)
        assertTrue(result[1].localeFiatCurrencyName.isNotBlank())
    }

    private fun testMarket(
        quoteCurrencyCode: String,
        quoteCurrencyName: String,
    ): MarketVO =
        MarketVO(
            baseCurrencyCode = "BTC",
            quoteCurrencyCode = quoteCurrencyCode,
            baseCurrencyName = "Bitcoin",
            quoteCurrencyName = quoteCurrencyName,
        )
}
