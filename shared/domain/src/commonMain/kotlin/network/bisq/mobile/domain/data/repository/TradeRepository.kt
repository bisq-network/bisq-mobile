package network.bisq.mobile.domain.data.repository

import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.model.Trade
import network.bisq.mobile.domain.data.persistance.PersistenceSource

/**
 * Repository for managing multiple Trade objects.
 *
 * This implementation was designed for open trades tracking and therefore doesn't
 * support large amounts of trades. To bring support filtering mechanisms need to be added
 * on the find methods
 * 
 * @param persistenceSource Optional persistence mechanism to save/load trades
 * @param prototype Optional prototype instance of Trade
 */
open class TradeRepository(
    persistenceSource: PersistenceSource<Trade>? = null,
    prototype: Trade? = null
) : MultiObjectRepository<Trade>(persistenceSource, prototype) {
    
    /**
     * Finds trades by status.
     * 
     * @param status The status to filter by
     * @return List of trades with the specified status
     */
    suspend fun findByStatus(status: String): List<Trade> {
        return fetchAll().filter { it.status == status }
    }
    
    /**
     * Finds trades by currency.
     * 
     * @param currency The currency to filter by
     * @return List of trades with the specified currency
     */
    suspend fun findByCurrency(currency: String): List<Trade> {
        return fetchAll().filter { it.offerCurrency == currency }
    }
    
    /**
     * Updates the status of a trade.
     * 
     * @param tradeId The ID of the trade to update
     * @param newStatus The new status
     * @return The updated trade, or null if not found
     */
    suspend fun updateStatus(tradeId: String, newStatus: String): Trade? {
        val trade = fetchById(tradeId) ?: return null
        val updatedTrade = trade.apply { 
            status = newStatus
            updatedAt = Clock.System.now().toEpochMilliseconds()
        }
        update(updatedTrade)
        return updatedTrade
    }
}
