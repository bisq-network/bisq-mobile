package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.persistance.KeyValueStorage

// this way of defining supports both platforms
// add your repositories here and then in your DI module call this classes for instanciation
open class SettingsRepository(keyValueStorage: KeyValueStorage<Settings>) : SingleObjectRepository<Settings>(keyValueStorage, Settings())
open class TradeReadStateRepository(keyValueStorage: KeyValueStorage<TradeReadState>) : SingleObjectRepository<TradeReadState>(keyValueStorage, TradeReadState()) {

    private val updateMutex = Mutex()

    /**
     * Thread-safe method to get the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @return The current read count for the trade, or 0 if not found
     */
    suspend fun getReadCount(tradeId: String): Int {
        return updateMutex.withLock {
            val current = fetch() ?: TradeReadState()
            current.map[tradeId] ?: 0
        }
    }

    /**
     * Thread-safe method to set the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @param count The new read count
     */
    suspend fun setReadCount(tradeId: String, count: Int) {
        updateMutex.withLock {
            val current = fetch() ?: TradeReadState()
            val updatedMap = current.map.toMutableMap()
            updatedMap[tradeId] = count
            val updated = TradeReadState().apply { map = updatedMap }
            update(updated)
        }
    }

    /**
     * Thread-safe method to increment the read count for a specific trade.
     * @param tradeId The ID of the trade
     * @return The new read count after incrementing
     */
    suspend fun incrementReadCount(tradeId: String): Int {
        return updateMutex.withLock {
            val current = fetch() ?: TradeReadState()
            val currentCount = current.map[tradeId] ?: 0
            val newCount = currentCount + 1
            val updatedMap = current.map.toMutableMap()
            updatedMap[tradeId] = newCount
            val updated = TradeReadState().apply { map = updatedMap }
            update(updated)
            newCount
        }
    }

    /**
     * Thread-safe method to update read count only if the new value is greater than current.
     * This prevents race conditions where an older update overwrites a newer one.
     * @param tradeId The ID of the trade
     * @param newCount The new read count
     * @return True if the update was applied, false if the current count was already >= newCount
     */
    suspend fun updateReadCountIfGreater(tradeId: String, newCount: Int): Boolean {
        return updateMutex.withLock {
            val current = fetch() ?: TradeReadState()
            val currentCount = current.map[tradeId] ?: 0
            if (newCount > currentCount) {
                val updatedMap = current.map.toMutableMap()
                updatedMap[tradeId] = newCount
                val updated = TradeReadState().apply { map = updatedMap }
                update(updated)
                true
            } else {
                false
            }
        }
    }

    /**
     * Thread-safe method to clear the read state for a specific trade.
     * @param tradeId The ID of the trade
     */
    suspend fun clearReadState(tradeId: String) {
        updateMutex.withLock {
            val current = fetch() ?: TradeReadState()
            val updatedMap = current.map.toMutableMap()
            updatedMap.remove(tradeId)
            val updated = TradeReadState().apply { map = updatedMap }
            update(updated)
        }
    }
}

open class UserRepository(keyValueStorage: KeyValueStorage<User>) : SingleObjectRepository<User>(keyValueStorage, User()) {
    suspend fun updateLastActivity(): User? {
        return withContext(IODispatcher) {
            val user = fetch()
            if (user != null) {
                update(user.apply {
                    lastActivity = Clock.System.now().toEpochMilliseconds()
                })
            }
            user
        }
    }
}