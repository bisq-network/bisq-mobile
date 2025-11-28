package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MarketPriceServiceFacadeTest : KoinTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var testMarketPriceServiceFacade: TestMarketPriceServiceFacade
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(testModule)
        }
        settingsRepository = SettingsRepositoryMock()
        testMarketPriceServiceFacade = TestMarketPriceServiceFacade(settingsRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun teardown() {
        runBlocking {
            settingsRepository.clear()
        }
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun testPersistSelectedMarket() = runBlocking {
        // Create a test market
        val marketVO = MarketVO("BTC", "USD")
        val marketListItem = MarketListItem.from(marketVO)
        
        // Select the market
        testMarketPriceServiceFacade.selectMarket(marketListItem)
        
        // Verify the market was persisted
        val settings = settingsRepository.fetch()
        assertNotNull(settings)
        assertEquals("BTC/USD", settings.selectedMarketCode)
    }

    @Test
    fun testRestoreSelectedMarket() = runBlocking {
        // Create and save settings with a selected market
        settingsRepository.setSelectedMarketCode("BTC/EUR")
        
        // Activate the service to trigger market restoration
        testMarketPriceServiceFacade.activate()

        delay(250L)
        
        // Verify the market was restored
        val restoredMarket = testMarketPriceServiceFacade.restoredMarket
        assertNotNull(restoredMarket)
        assertEquals("BTC", restoredMarket.baseCurrencyCode)
        assertEquals("EUR", restoredMarket.quoteCurrencyCode)
    }

    @Test
    fun testRestoreSelectedMarketWithInvalidCode() = runBlocking {
        // Create and save settings with an invalid market code
        settingsRepository.setSelectedMarketCode("INVALID")
        
        // Activate the service to trigger market restoration
        testMarketPriceServiceFacade.activate()
        
        // Verify no market was restored
        assertNull(testMarketPriceServiceFacade.restoredMarket)
    }

    // Test implementation of MarketPriceServiceFacade
    private class TestMarketPriceServiceFacade(
        private val settingsRepository: SettingsRepository
    ) : MarketPriceServiceFacade(settingsRepository) {
        
        var restoredMarket: MarketVO? = null
        private val testMarketPriceItem = MutableStateFlow<MarketPriceItem?>(null)
        
        override suspend fun activate() {
            super.activate()
            restoreSelectedMarketFromSettings { marketVO ->
                restoredMarket = marketVO
            }
        }
        
        override fun selectMarket(marketListItem: MarketListItem) {
            persistSelectedMarketToSettings(marketListItem)
        }
        
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
            return testMarketPriceItem.value
        }
        
        override fun findUSDMarketPriceItem(): MarketPriceItem? {
            return testMarketPriceItem.value
        }
        
        override fun refreshSelectedFormattedMarketPrice() {
            // No-op for test
        }
    }
}
