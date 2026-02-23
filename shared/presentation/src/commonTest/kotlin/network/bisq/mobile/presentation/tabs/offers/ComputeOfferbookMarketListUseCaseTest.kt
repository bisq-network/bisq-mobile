package network.bisq.mobile.presentation.tabs.offers

import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ComputeOfferbookMarketListUseCaseTest {
    @BeforeTest
    fun setup() {
        // Stabilize currency display names across different JVM default locales
        setDefaultLocale("en")
    }

    @Test
    fun `filters out markets without price data`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("USD", "US Dollar", numOffers = 1),
                marketItem("EUR", "Euro", numOffers = 1),
                marketItem("JPY", "Japanese Yen", numOffers = 1),
            )

        val result = useCase(MarketFilter.All, "", MarketSortBy.MostOffers, items)
        assertEquals(listOf("EUR", "USD"), result.map { it.market.quoteCurrencyCode }.sorted())
    }

    @Test
    fun `filters by WithOffers`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("USD", "US Dollar", numOffers = 0),
                marketItem("EUR", "Euro", numOffers = 2),
            )

        val result = useCase(MarketFilter.WithOffers, "", MarketSortBy.MostOffers, items)
        assertEquals(listOf("EUR"), result.map { it.market.quoteCurrencyCode })
    }

    @Test
    fun `filters by search text`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR", "CAD"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("USD", "US Dollar", numOffers = 1),
                marketItem("EUR", "Euro", numOffers = 1),
                marketItem("CAD", "Canadian Dollar", numOffers = 1),
            )

        val result = useCase(MarketFilter.All, "can", MarketSortBy.NameAZ, items)
        assertEquals(listOf("CAD"), result.map { it.market.quoteCurrencyCode })
    }

    @Test
    fun `sorts by MostOffers descending`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR", "CAD", "BRL", "AAA", "CCC", "BBB"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("CAD", "Canadian Dollar", numOffers = 6),
                marketItem("EUR", "Euro", numOffers = 10),
                marketItem("USD", "US Dollar", numOffers = 8),
                marketItem("BRL", "Brazilian Real", numOffers = 4),
                marketItem("AAA", "AAA", numOffers = 0),
                marketItem("CCC", "CCC", numOffers = 0),
                marketItem("BBB", "BBB", numOffers = 0),
            )

        val result = useCase(MarketFilter.All, "", MarketSortBy.MostOffers, items)
        assertEquals(listOf("EUR", "USD", "CAD", "BRL", "AAA", "BBB", "CCC"), result.map { it.market.quoteCurrencyCode })
    }

    @Test
    fun `sorts by NameAZ of locale fiat name`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR", "CAD", "BRL"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("EUR", "Euro", numOffers = 0),
                marketItem("CAD", "Canadian Dollar", numOffers = 0),
                marketItem("USD", "US Dollar", numOffers = 0),
                marketItem("BRL", "Brazilian Real", numOffers = 0),
            )

        val result = useCase(MarketFilter.All, "", MarketSortBy.NameAZ, items)
        assertEquals(listOf("BRL", "CAD", "EUR", "USD"), result.map { it.market.quoteCurrencyCode })
    }

    @Test
    fun `sorts by NameZA reversing entire comparator`() {
        val settingsRepository: SettingsRepository = SettingsRepositoryMock()
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(
                settingsRepository,
                marketsWithPrice = setOf("USD", "EUR", "CAD", "BRL"),
            )

        val useCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade)
        val items =
            listOf(
                marketItem("EUR", "Euro", numOffers = 0),
                marketItem("USD", "US Dollar", numOffers = 0),
                marketItem("BRL", "Brazilian Real", numOffers = 0),
                marketItem("CAD", "Canadian Dollar", numOffers = 0),
            )

        val result = useCase(MarketFilter.All, "", MarketSortBy.NameZA, items)
        assertEquals(listOf("USD", "EUR", "CAD", "BRL"), result.map { it.market.quoteCurrencyCode })
    }

    private fun marketItem(
        quoteCode: String,
        quoteName: String,
        numOffers: Int,
    ): MarketListItem {
        val market =
            MarketVO(
                baseCurrencyCode = "BTC",
                quoteCurrencyCode = quoteCode,
                baseCurrencyName = "Bitcoin",
                quoteCurrencyName = quoteName,
            )
        return MarketListItem.from(market = market, numOffers = numOffers)
    }

    private class FakeMarketPriceServiceFacade(
        settingsRepository: SettingsRepository,
        private val marketsWithPrice: Set<String>,
    ) : MarketPriceServiceFacade(settingsRepository) {
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
            if (!marketsWithPrice.contains(marketVO.quoteCurrencyCode)) return null
            val quote = PriceQuoteVOFactory.fromPrice(priceValue = 100_00L, market = marketVO)
            return MarketPriceItem(marketVO, quote, formattedPrice = "100")
        }

        override fun findUSDMarketPriceItem(): MarketPriceItem? = findMarketPriceItem(MarketVO("BTC", "USD"))

        override fun refreshSelectedFormattedMarketPrice() {
            // no-op
        }

        override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
    }
}
