package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MarketPriceServiceFacadeTest : KoinTest {

    private lateinit var settingsRepository: SettingsRepository
    private val persistenceSource: PersistenceSource<Settings> by inject(qualifier = named("settingsStorage"))
    private lateinit var testMarketPriceServiceFacade: TestMarketPriceServiceFacade

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
        settingsRepository = SettingsRepository(persistenceSource as KeyValueStorage<Settings>)
        runBlocking { settingsRepository.create(Settings()) }
        testMarketPriceServiceFacade = TestMarketPriceServiceFacade(settingsRepository)
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            settingsRepository.clear()
        }
        stopKoin()
    }

    @Test
    fun testPersistSelectedMarket() = runBlocking {
        // Create a test market
        val marketVO = MarketVO("BTC", "USD")
        val marketListItem = MarketListItem(marketVO, 0)
        
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
        val settings = Settings(selectedMarketCode = "BTC/EUR")
        settingsRepository.create(settings)
        
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
        val settings = Settings(selectedMarketCode = "INVALID")
        settingsRepository.create(settings)
        
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
        
        override fun activate() {
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
