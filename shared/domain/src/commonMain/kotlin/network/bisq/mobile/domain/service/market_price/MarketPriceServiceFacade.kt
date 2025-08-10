package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.ServiceFacade

abstract class MarketPriceServiceFacade(private val settingsRepository: SettingsRepository) : ServiceFacade() {
    protected val _selectedMarketPriceItem: MutableStateFlow<MarketPriceItem?> = MutableStateFlow(null)
    val selectedMarketPriceItem: StateFlow<MarketPriceItem?> get() = _selectedMarketPriceItem

    protected val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    val selectedFormattedMarketPrice: StateFlow<String> get() = _selectedFormattedMarketPrice.asStateFlow()

    // Global price update trigger - emits when any market price changes
    private val _globalPriceUpdate = MutableStateFlow(0L)
    val globalPriceUpdate: StateFlow<Long> get() = _globalPriceUpdate.asStateFlow()

    // Abstract methods that must be implemented by concrete classes
    abstract fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem?
    abstract fun findUSDMarketPriceItem(): MarketPriceItem?
    abstract fun refreshSelectedFormattedMarketPrice()
    abstract fun selectMarket(marketListItem: MarketListItem)

    /**
     * Triggers a global price update notification
     * Should be called whenever any market price data changes
     */
    protected fun triggerGlobalPriceUpdate() {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        _globalPriceUpdate.value = timestamp
        log.d { "Global price update triggered at timestamp: $timestamp" }
    }
    
    protected fun persistSelectedMarketToSettings(marketListItem: MarketListItem) {
        launchIO {
            try {
                val settings = settingsRepository.fetch() ?: Settings()
                val baseCurrencyCode = marketListItem.market.baseCurrencyCode
                val quoteCurrencyCode = marketListItem.market.quoteCurrencyCode
                if (baseCurrencyCode.isBlank() || quoteCurrencyCode.isBlank()) {
                    log.w("Invalid currency codes: base='$baseCurrencyCode', quote='$quoteCurrencyCode'")
                    return@launchIO
                }
                val marketCode = "$baseCurrencyCode/$quoteCurrencyCode"
                val updatedSettings = settings.copy(selectedMarketCode = marketCode)
                settingsRepository.update(updatedSettings)
            } catch (e: Exception) {
                log.e("Failed to save selected market", e)
            }
        }
    }
    
    protected fun restoreSelectedMarketFromSettings(onMarketRestored: (MarketVO) -> Unit) {
        launchIO {
            try {
                val settings = settingsRepository.fetch()
                settings?.selectedMarketCode?.let { marketCode ->
                    // Parse the market code to get base and quote currency
                    val parts = marketCode.split("/")
                    if (parts.size == 2) {
                        val baseCurrency = parts[0]
                        val quoteCurrency = parts[1]
                        val marketVO = MarketVO(baseCurrency, quoteCurrency)
                        runCatching { onMarketRestored(marketVO) }.onFailure {
                            log.e(it) { "Failed callback on restore selected market" }
                        }

                    }
                }
            } catch (e: Exception) {
                log.e("Failed to restore selected market", e)
            }
        }
    }
}